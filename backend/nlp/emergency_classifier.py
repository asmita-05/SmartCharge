"""Local zero-shot classifier service used by the SmartCharge Spring backend."""

import os
import logging
import re
import threading
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, HTTPException, Request
from transformers import pipeline


MODEL_ID = "MoritzLaurer/deberta-v3-base-zeroshot-v1.1-all-33"
PROTOCOL_VERSION = "smartcharge-priority-policy-v2"
MODEL_DIR = Path(os.environ.get("SMARTCHARGE_NLP_MODEL_DIR", Path(__file__).parent / "model"))
MAX_REASON_LENGTH = 500

SERIOUS_LABEL = (
    "a genuine emergency involving an accident or injury, an urgent medical condition, "
    "an immediate threat to someone's safety, fire, smoke, a dangerous vehicle or battery fault, "
    "or being stranded in an unsafe place"
)
ROUTINE_LABEL = (
    "a non-emergency request for faster charging based only on convenience, personal preference, "
    "VIP or other status, work, a meeting, appointment, deadline, or being in a hurry, "
    "without an injury, medical emergency, immediate danger, or safety risk"
)
PRIORITY_STATUS_PATTERN = re.compile(
    r"\b(vip|important person|celebrity|executive|special status|special treatment)\b",
    re.IGNORECASE,
)
SCHEDULING_URGENCY_PATTERN = re.compile(
    r"\b(work|business|meeting|appointment|deadline|interview|event|schedule|client|customer)\b",
    re.IGNORECASE,
)
URGENCY_CLAIM_PATTERN = re.compile(
    r"\b(urgent|urgently|urgency|important|priority|priorit\w*|asap|in a hurry|running late|rush)\b",
    re.IGNORECASE,
)
EMERGENCY_EVIDENCE_PATTERN = re.compile(
    r"\b(accident|crash|injur\w*|hurt|bleed\w*|unconscious|heart attack|chest pain|"
    r"medical emergency|medical help|urgent medical|trouble breathing|seizure|fire|smoke|"
    r"overheat\w*|explod\w*|battery fault|unsafe|danger\w*|life[- ]threatening|ambulance|"
    r"emergency services|stranded in (?:an )?unsafe|threatened|assault|robbery)\b",
    re.IGNORECASE,
)

classifier = None
classifier_lock = threading.Lock()
logger = logging.getLogger("uvicorn.error")


def load_model() -> None:
    global classifier
    if not (MODEL_DIR / "config.json").is_file() or not (MODEL_DIR / "model.safetensors").is_file():
        raise RuntimeError(
            f"Model files are missing from {MODEL_DIR}. Run download-model.ps1 first."
        )
    classifier = pipeline(
        task="zero-shot-classification",
        model=str(MODEL_DIR),
        tokenizer=str(MODEL_DIR),
        device=-1,
        local_files_only=True,
        model_kwargs={"use_safetensors": True},
    )


def is_priority_only_claim(text: str) -> bool:
    """Reject status/scheduling urgency as an emergency unless harm or danger is stated."""
    claims_priority = bool(PRIORITY_STATUS_PATTERN.search(text)) or (
        bool(SCHEDULING_URGENCY_PATTERN.search(text))
        and bool(URGENCY_CLAIM_PATTERN.search(text))
    )
    return claims_priority and not EMERGENCY_EVIDENCE_PATTERN.search(text)


@asynccontextmanager
async def lifespan(_app: FastAPI):
    load_model()
    yield


app = FastAPI(
    title="SmartCharge Local Emergency NLP",
    docs_url=None,
    redoc_url=None,
    lifespan=lifespan,
)


@app.get("/health")
def health() -> dict[str, str]:
    if classifier is None:
        raise HTTPException(status_code=503, detail="Model is not loaded")
    return {"status": "ready", "model": MODEL_ID, "protocol": PROTOCOL_VERSION}


@app.post("/classify")
async def classify(request: Request) -> dict[str, object]:
    if classifier is None:
        raise HTTPException(status_code=503, detail="Model is not loaded")
    raw_body = await request.body()
    logger.info(
        "Received classify payload: content_type=%s bytes=%d",
        request.headers.get("content-type", "(missing)"),
        len(raw_body),
    )
    try:
        payload = await request.json()
    except Exception:
        # Support plain UTF-8 request bodies too. This keeps classification
        # working if a local Java HTTP client sends the reason as text rather
        # than wrapping it in JSON.
        try:
            payload = raw_body.decode("utf-8-sig").strip()
        except UnicodeDecodeError as error:
            raise HTTPException(status_code=400, detail="Request body must be UTF-8 text or JSON") from error

    # Accept the canonical Java field and common alternatives so a field-name
    # mismatch cannot silently turn every emergency into NLP_ERROR.
    text = None
    if isinstance(payload, str):
        text = payload
    elif isinstance(payload, dict):
        for key in ("text", "reason", "emergencyReason", "emergency_reason"):
            value = payload.get(key)
            if isinstance(value, str):
                text = value
                break
        if text is None:
            string_values = [value for value in payload.values() if isinstance(value, str)]
            if len(string_values) == 1:
                text = string_values[0]

    if not isinstance(text, str):
        keys = ", ".join(payload.keys()) if isinstance(payload, dict) else type(payload).__name__
        raise HTTPException(
            status_code=422,
            detail=f"Expected a string in JSON field 'text'; received payload shape: {keys or 'empty object'}",
        )

    text = text.strip()
    logger.info("Parsed classify text length=%d", len(text))
    if not text:
        raise HTTPException(status_code=422, detail="Emergency reason cannot be blank")
    if len(text) > MAX_REASON_LENGTH:
        raise HTTPException(status_code=422, detail=f"Emergency reason must be at most {MAX_REASON_LENGTH} characters")

    try:
        # Serialize CPU inference to keep memory use predictable on a student laptop.
        with classifier_lock:
            result = classifier(
                text,
                candidate_labels=[SERIOUS_LABEL, ROUTINE_LABEL],
                hypothesis_template="The stated reason for this charging request indicates {}.",
                multi_label=False,
            )
    except Exception as error:
        raise HTTPException(status_code=503, detail="Classification failed") from error

    winning_label = result["labels"][0]
    classification = "SERIOUS" if winning_label == SERIOUS_LABEL else "ROUTINE"
    model_name = MODEL_ID
    confidence = float(result["scores"][0])
    if is_priority_only_claim(text):
        classification = "ROUTINE"
        routine_index = result["labels"].index(ROUTINE_LABEL)
        confidence = float(result["scores"][routine_index])
        model_name = f"{MODEL_ID}+priority-policy"
    return {
        "classification": classification,
        "confidence": confidence,
        "model": model_name,
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=8090, log_level="info")
