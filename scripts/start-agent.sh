#!/usr/bin/env bash
# Bring up local MLX LLM (install + download if needed) and run the mapping agent.
# On Ctrl+C or normal exit, both the agent and the LLM are stopped.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "${ROOT}"
mkdir -p "${ROOT}/.run"

cleanup() {
  "${ROOT}/scripts/stop-agent.sh" || true
}
trap cleanup EXIT INT TERM

chmod +x "${ROOT}/scripts/start-mlx-llm.sh" "${ROOT}/scripts/stop-agent.sh"

echo "Starting company mapping agent (will start local LLM if needed)..."
mvn -q exec:java
