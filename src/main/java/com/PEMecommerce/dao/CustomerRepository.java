package com.PEMecommerce.dao;

import com.PEMecommerce.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findFirstByEmailIgnoreCase(String email);
}
