package com.ghartur.springosgi.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/osgi/bundles")
public class OsgiController {

    private final OsgiFrameworkManager osgiManager;

    public OsgiController(OsgiFrameworkManager osgiManager) {
        this.osgiManager = osgiManager;
    }

    @GetMapping
    public List<BundleInfo> listBundles() {
        return Arrays.stream(osgiManager.getBundleContext().getBundles())
                .map(b -> new BundleInfo(
                        b.getBundleId(),
                        b.getSymbolicName(),
                        b.getVersion().toString(),
                        BundleInfo.stateLabel(b.getState())))
                .toList();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<BundleInfo> startBundle(@PathVariable long id) throws BundleException {
        Bundle bundle = getBundle(id);
        if (bundle == null) return ResponseEntity.notFound().build();
        bundle.start();
        return ResponseEntity.ok(toInfo(bundle));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<BundleInfo> stopBundle(@PathVariable long id) throws BundleException {
        Bundle bundle = getBundle(id);
        if (bundle == null) return ResponseEntity.notFound().build();
        bundle.stop();
        return ResponseEntity.ok(toInfo(bundle));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> uninstallBundle(@PathVariable long id) throws BundleException {
        Bundle bundle = getBundle(id);
        if (bundle == null) return ResponseEntity.notFound().build();
        bundle.uninstall();
        return ResponseEntity.noContent().build();
    }

    private Bundle getBundle(long id) {
        BundleContext ctx = osgiManager.getBundleContext();
        return Arrays.stream(ctx.getBundles())
                .filter(b -> b.getBundleId() == id)
                .findFirst()
                .orElse(null);
    }

    private BundleInfo toInfo(Bundle b) {
        return new BundleInfo(b.getBundleId(), b.getSymbolicName(),
                b.getVersion().toString(), BundleInfo.stateLabel(b.getState()));
    }
}
