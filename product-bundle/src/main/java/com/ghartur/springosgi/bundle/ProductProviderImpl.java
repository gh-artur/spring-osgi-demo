package com.ghartur.springosgi.bundle;

import com.ghartur.springosgi.product.Product;
import com.ghartur.springosgi.product.ProductProvider;

import java.util.List;

public class ProductProviderImpl implements ProductProvider {

    @Override
    public List<Product> getProducts() {
        return List.of(
                new Product(10L, "Monitor 4K", "Monitor LG 27 polegadas 4K", 3200.00),
                new Product(11L, "Headset", "Headset Sony WH-1000XM5", 1800.00),
                new Product(12L, "Webcam HD", "Webcam Logitech 1080p", 450.00),
                new Product(13L, "Teclado Mecânico", "Teclado Razer Chroma", 600.00),
                new Product(14L, "Mouse Gamer", "Mouse Razer DeathAdder", 300.00),
                new Product(15L, "SSD Externo", "SSD Samsung T7 1TB", 900.00)
        );
    }
}
