package com.ghartur.springosgi.customer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    @GetMapping
    public List<Customer> getCustomers() {
        return List.of(
            new Customer(1L, "Ana Silva", "ana.silva@email.com"),
            new Customer(2L, "Carlos Souza", "carlos.souza@email.com"),
            new Customer(3L, "Mariana Costa", "mariana.costa@email.com")
        );
    }
}
