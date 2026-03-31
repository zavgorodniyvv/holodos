package com.holodos.common.infrastructure;

import com.holodos.common.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGoogleSubject(String googleSubject);
}
