package com.example.sigebom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.sigebom.model.MembroEquipe;

@Repository
public interface MembroEquipeRepository extends JpaRepository<MembroEquipe, Long> {

    Optional<MembroEquipe> findByBombeiroId(Long bombeiroId);

    List<MembroEquipe> findByViaturaId(Long viaturaId);

    long countByViaturaId(Long viaturaId);
}