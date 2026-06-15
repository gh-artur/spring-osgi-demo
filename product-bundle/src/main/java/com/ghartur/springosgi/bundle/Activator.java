package com.ghartur.springosgi.bundle;

import com.ghartur.springosgi.product.ProductProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private ServiceRegistration<ProductProvider> registration;

    @Override
    public void start(BundleContext context) {
        registration = context.registerService(ProductProvider.class, new ProductProviderImpl(), null);
    }

    @Override
    public void stop(BundleContext context) {
        if (registration != null) {
            registration.unregister();
        }
    }
}
