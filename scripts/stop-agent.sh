#!/usr/bin/env bash
# Stop the mapping agent and the local MLX LLM it owns.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="${ROOT}/.run"
PORT="${MLX_PORT:-8080}"

kill_pidfile() {
  local file="$1"
  if [[ -f "${file}" ]]; then
    local pid
    pid="$(cat "${file}" 2>/dev/null || true)"
    if [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null; then
      echo "Stopping pid ${pid} (${file})"
      kill "${pid}" 2>/dev/null || true
      sleep 0.4
      kill -9 "${pid}" 2>/dev/null || true
    fi
    rm -f "${file}"
  fi
}

kill_pidfile "${RUN}/mlx.pid"

# Java mapping process (Maven exec or java -jar)
if pgrep -f 'com.nokri.taxonomy.CompanyMappingApplication' >/dev/null 2>&1; then
  echo "Stopping CompanyMappingApplication"
  pkill -f 'com.nokri.taxonomy.CompanyMappingApplication' 2>/dev/null || true
fi

# MLX server on the product port
if command -v lsof >/dev/null 2>&1; then
  ports="$(lsof -ti tcp:"${PORT}" 2>/dev/null || true)"
  if [[ -n "${ports}" ]]; then
    echo "Stopping listeners on port ${PORT}"
    # shellcheck disable=SC2086
    kill ${ports} 2>/dev/null || true
    sleep 0.3
    # shellcheck disable=SC2086
    kill -9 ${ports} 2>/dev/null || true
  fi
fi

if pgrep -f 'mlx_lm.server' >/dev/null 2>&1; then
  echo "Stopping mlx_lm.server"
  pkill -f 'mlx_lm.server' 2>/dev/null || true
  sleep 0.3
  pkill -9 -f 'mlx_lm.server' 2>/dev/null || true
fi

rm -f "${RUN}/agent.pid" "${RUN}/mlx.pid"
echo "Agent and local LLM are down."
