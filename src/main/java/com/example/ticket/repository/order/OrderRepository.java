package com.example.ticket.repository.order;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ticket.entity.order.OrderEntity;
import com.example.ticket.enums.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.id = :id AND o.user.id = :userId")
    Optional<OrderEntity> findByIdAndUserIdWithItems(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<OrderEntity> findByIdWithItems(@Param("id") Long id);

    Optional<OrderEntity> findByIdAndUserId(Long id, Long userId);

    Optional<OrderEntity> findByOrderCode(String orderCode);

    Page<OrderEntity> findByUserId(Long userId, Pageable pageable);

    Page<OrderEntity> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    Page<OrderEntity> findByEventId(Long eventId, Pageable pageable);

    Page<OrderEntity> findByEventIdAndStatus(Long eventId, OrderStatus status, Pageable pageable);

    boolean existsByHoldIdAndStatus(Long holdId, OrderStatus status);

    Optional<OrderEntity> findByHoldId(Long holdId);
}
