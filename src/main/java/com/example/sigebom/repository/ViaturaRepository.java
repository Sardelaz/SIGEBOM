package com.example.sigebom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.sigebom.model.Viatura;
import com.example.sigebom.model.enums.StatusViatura;

import jakarta.persistence.LockModeType;

@Repository
public interface ViaturaRepository extends JpaRepository<Viatura, Long> {

    List<Viatura> findByAtivoTrueAndStatus(StatusViatura status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Viatura v WHERE v.id = :id")
    Optional<Viatura> findByIdForUpdate(@Param("id") Long id);
}