package com.example.sigebom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.sigebom.model.Viatura;
import com.example.sigebom.model.enums.StatusViatura;

@Repository
public interface ViaturaRepository extends JpaRepository<Viatura, Long> {
    
    // Busca viaturas que não estão inativadas e que possuam um status exato (ex: DISPONIVEL)
    List<Viatura> findByAtivoTrueAndStatus(StatusViatura status);
}