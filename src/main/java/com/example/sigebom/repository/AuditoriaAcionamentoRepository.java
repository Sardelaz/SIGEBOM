package com.example.sigebom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.sigebom.model.AuditoriaAcionamento;

@Repository
public interface AuditoriaAcionamentoRepository extends JpaRepository<AuditoriaAcionamento, Long> {

    List<AuditoriaAcionamento> findByOcorrenciaIdOrderByDataModificacaoDesc(Long ocorrenciaId);
}