package com.example.sigebom.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.sigebom.model.Ocorrencia;
import com.example.sigebom.model.enums.Gravidade;
import com.example.sigebom.model.enums.StatusOcorrencia;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    @Query("SELECT o FROM Ocorrencia o WHERE " +
           "(:dataInicio IS NULL OR o.horarioAcionamento >= :dataInicio) AND " +
           "(:dataFim IS NULL OR o.horarioAcionamento <= :dataFim) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:gravidade IS NULL OR o.gravidade = :gravidade)")
    Page<Ocorrencia> filtrarOcorrencias(
            @Param("dataInicio") LocalDateTime dataInicio, 
            @Param("dataFim") LocalDateTime dataFim, 
            @Param("status") StatusOcorrencia status, 
            @Param("gravidade") Gravidade gravidade, 
            Pageable pageable);
}