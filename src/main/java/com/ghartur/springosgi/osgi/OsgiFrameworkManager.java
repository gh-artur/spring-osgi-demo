package com.ghartur.springosgi.osgi;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.felix.framework.FrameworkFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Component
public class OsgiFrameworkManager {

    private static final Logger log = Logger.getLogger(OsgiFrameworkManager.class.getName());

    private Framework framework;
    private Thread watcherThread;

    // Rastreia bundles instalados: caminho absoluto do JAR → Bundle
    private final Map<String, Bundle> installedBundles = new ConcurrentHashMap<>();

    @PostConstruct
    public void start() throws BundleException {
        Map<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_STORAGE, "target/felix-cache");
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
                "com.ghartur.springosgi.product;version=1.0.0");

        framework = new FrameworkFactory().newFramework(config);
        framework.start();
        log.info("OSGi framework iniciado");

        File bundlesDir = new File("bundles");
        bundlesDir.mkdirs();

        loadExistingBundles(bundlesDir);
        startDirectoryWatcher(bundlesDir);
    }

    private void loadExistingBundles(File dir) {
        File[] jars = dir.listFiles((d, n) -> n.endsWith(".jar"));
        if (jars == null) return;
        for (File jar : jars) {
            installAndStart(jar);
        }
    }

    private void installAndStart(File jar) {
        BundleContext ctx = framework.getBundleContext();
        String location = "file:" + jar.getAbsolutePath();
        try {
            Bundle bundle = ctx.installBundle(location);
            bundle.start();
            installedBundles.put(jar.getAbsolutePath(), bundle);
            log.info("Bundle instalado: " + bundle.getSymbolicName() + " [" + jar.getName() + "]");
        } catch (BundleException e) {
            log.warning("Falha ao instalar bundle " + jar.getName() + ": " + e.getMessage());
        }
    }

    private void uninstallBundle(File jar) {
        Bundle bundle = installedBundles.remove(jar.getAbsolutePath());
        if (bundle == null) return;
        try {
            bundle.stop();
            bundle.uninstall();
            log.info("Bundle removido: " + bundle.getSymbolicName() + " [" + jar.getName() + "]");
        } catch (BundleException e) {
            log.warning("Falha ao remover bundle " + jar.getName() + ": " + e.getMessage());
        }
    }

    private void startDirectoryWatcher(File dir) {
        watcherThread = new Thread(() -> {
            try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
                dir.toPath().register(watcher,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE);

                log.info("Monitorando bundles em: " + dir.getAbsolutePath());

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watcher.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = dir.toPath().resolve((Path) event.context());
                        if (!changed.toString().endsWith(".jar")) continue;

                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            // Pequena pausa para garantir que o arquivo foi totalmente copiado
                            Thread.sleep(200);
                            installAndStart(changed.toFile());
                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            uninstallBundle(changed.toFile());
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                log.warning("Erro no watcher de bundles: " + e.getMessage());
            }
        }, "osgi-bundle-watcher");

        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    public BundleContext getBundleContext() {
        return framework.getBundleContext();
    }

    @PreDestroy
    public void stop() throws BundleException, InterruptedException {
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
        if (framework != null) {
            framework.stop();
            framework.waitForStop(5000);
            log.info("OSGi framework parado");
        }
    }
}
