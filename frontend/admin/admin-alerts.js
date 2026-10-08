(() => {
    if (!localStorage.getItem("smartchargeAdmin")) return;

    const API = "http://localhost:8080/api/admin/emergencies/pending";
    const STORAGE_KEY = "smartchargeSeenEmergencyAlerts";
    const QUEUE_KEY = "smartchargePendingEmergencyAlerts";
    let seenIds;
    try { seenIds = new Set(JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]").map(String)); }
    catch { seenIds = new Set(); }
    let queue = [];
    try { queue = JSON.parse(localStorage.getItem(QUEUE_KEY) || "[]"); }
    catch { queue = []; }
    localStorage.removeItem(QUEUE_KEY);
    let isOpen = false;

    const style = document.createElement("style");
    style.textContent = `
        .sc-alert-backdrop{position:fixed;inset:0;z-index:99999;display:none;align-items:center;justify-content:center;padding:20px;background:rgba(15,23,42,.68)}
        .sc-alert-backdrop.open{display:flex}
        .sc-alert-card{width:min(640px,100%);padding:38px;border-radius:20px;background:#fff;box-shadow:0 24px 80px rgba(15,23,42,.36);text-align:center}
        .sc-alert-icon{font-size:54px;margin-bottom:12px}
        .sc-alert-card h2{font-size:30px;color:#991b1b;margin:0 0 12px}
        .sc-alert-card p{font-size:16px;line-height:1.6;color:#475569;margin:0 0 12px}
        .sc-alert-reason{padding:13px;border-radius:9px;background:#fff7ed;color:#7c2d12;margin:18px 0 24px;text-align:left;line-height:1.5;overflow-wrap:anywhere}
        .sc-alert-actions{display:flex;gap:12px}
        .sc-alert-actions button{flex:1;border:0;border-radius:9px;padding:14px;font-size:15px;font-weight:700;cursor:pointer}
        .sc-alert-show{background:#7c3aed;color:#fff}.sc-alert-dismiss{background:#e2e8f0;color:#334155}
        @media(max-width:520px){.sc-alert-card{padding:28px 22px}.sc-alert-actions{flex-direction:column}}
    `;
    document.head.appendChild(style);

    const backdrop = document.createElement("div");
    backdrop.className = "sc-alert-backdrop";
    backdrop.setAttribute("role", "dialog");
    backdrop.setAttribute("aria-modal", "true");
    backdrop.innerHTML = `<section class="sc-alert-card"><div class="sc-alert-icon">🚨</div><h2>New priority request</h2><p class="sc-alert-summary"></p><div class="sc-alert-reason"></div><div class="sc-alert-actions"><button class="sc-alert-show">Show emergency</button><button class="sc-alert-dismiss">OK</button></div></section>`;
    document.body.appendChild(backdrop);

    const summary = backdrop.querySelector(".sc-alert-summary");
    const reason = backdrop.querySelector(".sc-alert-reason");
    const showButton = backdrop.querySelector(".sc-alert-show");
    const dismissButton = backdrop.querySelector(".sc-alert-dismiss");
    let activeRequest = null;

    function persistSeen() {
        localStorage.setItem(STORAGE_KEY, JSON.stringify([...seenIds]));
    }

    function displayNext() {
        if (isOpen || !queue.length) return;
        activeRequest = queue.shift();
        seenIds.add(String(activeRequest.id));
        persistSeen();
        isOpen = true;
        summary.textContent = `Emergency request #${activeRequest.id} is waiting for Admin review.`;
        reason.textContent = activeRequest.emergencyReason || "No reason was provided.";
        backdrop.classList.add("open");
    }

    function dismiss() {
        backdrop.classList.remove("open");
        isOpen = false;
        activeRequest = null;
        window.setTimeout(displayNext, 120);
    }

    showButton.addEventListener("click", () => {
        if (activeRequest) {
            localStorage.setItem(QUEUE_KEY, JSON.stringify(queue));
            window.location.href = `emergencies.html?requestId=${encodeURIComponent(activeRequest.id)}`;
        }
    });
    dismissButton.addEventListener("click", dismiss);

    async function poll() {
        try {
            const response = await fetch(API, { cache: "no-store" });
            if (!response.ok) return;
            const requests = await response.json();
            for (const request of requests) {
                const id = String(request.id);
                if (seenIds.has(id) || queue.some(item => String(item.id) === id)
                        || (activeRequest && String(activeRequest.id) === id)) continue;
                queue.push(request);
            }
            displayNext();
        } catch (_) {
            // Keep the admin workspace usable if the backend is temporarily offline.
        }
    }

    poll();
    window.setInterval(poll, 3000);
})();
