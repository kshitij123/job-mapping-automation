#!/usr/bin/env bash
# Local MLX LLM for company-mapping POC (Apple Silicon, no paid API).
# Default: Qwen2.5-3B-Instruct 4-bit — small, fast, instruct-tuned for short JSON.
# Larger (slower, may emit thinking text): MLX_MODEL=mlx-community/Qwen3-8B-4bit
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VENV="${ROOT}/.venv-mlx"
MODEL="${MLX_MODEL:-mlx-community/Qwen2.5-3B-Instruct-4bit}"
HOST="${MLX_HOST:-127.0.0.1}"
PORT="${MLX_PORT:-8080}"

if [[ ! -d "${VENV}" ]]; then
  echo "Creating Python venv and installing mlx-lm (one-time)..."
  python3 -m venv "${VENV}"
  "${VENV}/bin/pip" install --upgrade pip
  "${VENV}/bin/pip" install 'mlx-lm>=0.22.0'
elif [[ ! -x "${VENV}/bin/mlx_lm.server" ]]; then
  echo "Installing mlx-lm into existing venv..."
  "${VENV}/bin/pip" install --upgrade pip
  "${VENV}/bin/pip" install 'mlx-lm>=0.22.0'
fi

mkdir -p "${ROOT}/.run"
echo $$ > "${ROOT}/.run/mlx.pid"

echo "Starting MLX OpenAI-compatible server on http://${HOST}:${PORT}/v1"
echo "Model: ${MODEL} (first run downloads weights from Hugging Face if missing)"
exec "${VENV}/bin/mlx_lm.server" \
  --model "${MODEL}" \
  --host "${HOST}" \
  --port "${PORT}"
