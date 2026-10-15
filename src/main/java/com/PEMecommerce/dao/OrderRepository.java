package com.PEMecommerce.dao;

import com.PEMecommerce.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "orderItems")
    List<Order> findByCustomer_EmailIgnoreCaseOrderByDateCreatedDesc(String email);

    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findByOrderTrackingNumber(String orderTrackingNumber);
}
