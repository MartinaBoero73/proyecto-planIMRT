package com.planimrt.repository;

import com.planimrt.model.User;
import com.planimrt.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByRole(UserRole role);

    List<User> findByActiveTrue();

    boolean existsByUsername(String username);
}