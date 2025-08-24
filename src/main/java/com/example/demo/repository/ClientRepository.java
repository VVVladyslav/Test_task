package com.example.demo.repository;

import com.example.demo.model.Client;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query(
            "select c from Client c " +
                    "where lower(c.name) like lower(concat('%', :q, '%')) " +
                    "   or lower(c.email) like lower(concat('%', :q, '%')) " +
                    "   or lower(coalesce(c.address, '')) like lower(concat('%', :q, '%'))"
    )
    List<Client> searchByKeyword(String q);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Client c where c.id = :id")
    Optional<Client> findByIdForUpdate(Long id);
}