$ErrorActionPreference = "Stop"
$ModelId = "MoritzLaurer/deberta-v3-base-zeroshot-v1.1-all-33"
$Revision = "2a8a9dd6777d61f3b6331fca71436bf8f75b50d6"
$ModelDirectory = Join-Path $PSScriptRoot "model"
$Python = Join-Path $PSScriptRoot ".venv\Scripts\python.exe"

if (-not (Test-Path $Python)) { throw "Run setup-nlp.ps1 first to install the local Python environment." }

& $Python -c "from huggingface_hub import snapshot_download; snapshot_download(repo_id='$ModelId', revision='$Revision', local_dir=r'$ModelDirectory', allow_patterns=['config.json','model.safetensors','added_tokens.json','special_tokens_map.json','spm.model','tokenizer.json','tokenizer_config.json'])"
if ($LASTEXITCODE -ne 0) { throw "Model download failed. Check Python and network access, then retry." }
Write-Host "Model downloaded to $ModelDirectory"
