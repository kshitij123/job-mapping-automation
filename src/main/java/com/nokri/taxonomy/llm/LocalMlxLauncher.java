package com.nokri.taxonomy.llm;

import com.nokri.taxonomy.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts {@code mlx_lm.server} if needed (venv + model download happen in the shell script)
 * and stops it when the JVM exits so the agent and the local LLM share one lifecycle.
 */
public final class LocalMlxLauncher {
    private static final Logger log = LoggerFactory.getLogger(LocalMlxLauncher.class);
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);
    private static final AtomicBoolean HOOK_REGISTERED = new AtomicBoolean(false);
    private static volatile Process mlxProcess;

    private LocalMlxLauncher() {
    }

    public static boolean ensureRunning(AppConfig config) {
        registerShutdownHook(config);
        if (LocalLlmProbe.isReachableQuiet(config.localLlmBaseUrl())) {
            log.info("Local LLM already running at {}", config.localLlmBaseUrl());
            return true;
        }
        Optional<Path> script = findStartScript();
        if (script.isEmpty()) {
            log.warn("Could not find scripts/start-mlx-llm.sh");
            return false;
        }
        URI uri = URI.create(config.localLlmBaseUrl());
        String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 8080;
        log.info("Starting local MLX LLM via {} (model {})", script.get(), config.localLlmModel());
        try {
            ProcessBuilder builder = new ProcessBuilder(script.get().toAbsolutePath().toString());
            builder.environment().put("MLX_MODEL", config.localLlmModel());
            builder.environment().put("MLX_HOST", host);
            builder.environment().put("MLX_PORT", Integer.toString(port));
            builder.redirectErrorStream(true);
            builder.inheritIO();
            mlxProcess = builder.start();
            writePid(repoRoot().resolve(".run/mlx.pid"), mlxProcess.pid());
        } catch (Exception e) {
            log.error("Failed to start MLX server: {}", e.getMessage());
            return false;
        }
        Instant deadline = Instant.now().plus(STARTUP_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            if (mlxProcess != null && !mlxProcess.isAlive()) {
                log.warn("MLX process exited before becoming ready (code {})", mlxProcess.exitValue());
                return false;
            }
            if (LocalLlmProbe.isReachableQuiet(config.localLlmBaseUrl())) {
                log.info("Local LLM is ready at {}", config.localLlmBaseUrl());
                return true;
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        log.warn("Local LLM did not become ready within {}", STARTUP_TIMEOUT);
        return false;
    }

    public static void stop(AppConfig config) {
        Process process = mlxProcess;
        mlxProcess = null;
        if (process != null && process.isAlive()) {
            log.info("Stopping local MLX LLM (pid {})", process.pid());
            process.destroy();
            try {
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
        int port = 8080;
        try {
            URI uri = URI.create(config.localLlmBaseUrl());
            if (uri.getPort() > 0) {
                port = uri.getPort();
            }
        } catch (Exception ignored) {
            // keep 8080
        }
        killListenersOnPort(port);
        Path pidFile = repoRoot().resolve(".run/mlx.pid");
        try {
            Files.deleteIfExists(pidFile);
        } catch (Exception ignored) {
            // best-effort
        }
    }

    private static void registerShutdownHook(AppConfig config) {
        if (HOOK_REGISTERED.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(config), "mlx-shutdown"));
        }
    }

    private static void killListenersOnPort(int port) {
        try {
            Process lsof = new ProcessBuilder("lsof", "-ti", "tcp:" + port)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(lsof.getInputStream().readAllBytes());
            lsof.waitFor();
            for (String line : output.split("\\s+")) {
                if (line.isBlank()) {
                    continue;
                }
                long pid = Long.parseLong(line.trim());
                if (ProcessHandle.current().pid() == pid) {
                    continue;
                }
                ProcessHandle.of(pid).ifPresent(handle -> {
                    log.info("Stopping process on port {} (pid {})", port, pid);
                    handle.destroy();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (handle.isAlive()) {
                        handle.destroyForcibly();
                    }
                });
            }
        } catch (Exception e) {
            log.debug("Could not scan port {}: {}", port, e.getMessage());
        }
    }

    private static void writePid(Path file, long pid) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, Long.toString(pid));
        } catch (Exception e) {
            log.debug("Could not write pid file {}: {}", file, e.getMessage());
        }
    }

    private static Path repoRoot() {
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }

    private static Optional<Path> findStartScript() {
        Path cwd = repoRoot();
        Path candidate = cwd.resolve("scripts/start-mlx-llm.sh");
        if (Files.isRegularFile(candidate)) {
            return Optional.of(candidate);
        }
        Path parent = cwd.getParent();
        if (parent != null) {
            candidate = parent.resolve("scripts/start-mlx-llm.sh");
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
