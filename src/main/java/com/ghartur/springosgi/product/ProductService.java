package com.ghartur.springosgi.product;

import com.ghartur.springosgi.osgi.OsgiFrameworkManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private static final List<Product> DEFAULT_PRODUCTS = List.of(
            new Product(1L, "Notebook", "Notebook Dell 16GB RAM", 4500.00),
            new Product(2L, "Mouse", "Mouse sem fio Logitech", 150.00),
            new Product(3L, "Teclado", "Teclado mecânico RGB", 350.00)
    );

    private final OsgiFrameworkManager osgiManager;

    public ProductService(OsgiFrameworkManager osgiManager) {
        this.osgiManager = osgiManager;
    }

    public List<Product> getProducts() {
        BundleContext ctx = osgiManager.getBundleContext();
        ServiceReference<ProductProvider> ref = ctx.getServiceReference(ProductProvider.class);

        if (ref == null) {
            return DEFAULT_PRODUCTS;
        }

        ProductProvider provider = ctx.getService(ref);
        try {
            return provider.getProducts();
        } finally {
            ctx.ungetService(ref);
        }
    }
}
