package com.example.ticket.repository.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ticket.entity.order.OrderItemEntity;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"order", "order.event"})
    @Query("""
        SELECT oi FROM OrderItemEntity oi
        WHERE oi.order.user.id = :userId
          AND oi.order.status = com.example.ticket.enums.OrderStatus.PAID
          AND (:eventId IS NULL OR oi.order.event.id = :eventId)
        ORDER BY oi.id DESC
    """)
    Page<OrderItemEntity> findPaidTickets(
        @Param("userId") Long userId,
        @Param("eventId") Long eventId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"order", "order.event"})
    @Query("""
        SELECT oi FROM OrderItemEntity oi
        WHERE oi.id = :id
          AND oi.order.user.id = :userId
          AND oi.order.status = com.example.ticket.enums.OrderStatus.PAID
    """)
    Optional<OrderItemEntity> findPaidTicketById(
        @Param("id") Long id,
        @Param("userId") Long userId
    );

    @EntityGraph(attributePaths = {"order", "order.event"})
    @Query("""
        SELECT oi FROM OrderItemEntity oi
        WHERE oi.id = :id
          AND oi.order.status = com.example.ticket.enums.OrderStatus.PAID
    """)
    Optional<OrderItemEntity> findPaidTicketByIdAdmin(
        @Param("id") Long id
    );
}
