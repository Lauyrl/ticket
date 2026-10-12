package com.example.ticket.repository.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ticket.entity.user.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
