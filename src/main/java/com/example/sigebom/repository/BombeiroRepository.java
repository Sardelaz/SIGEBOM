package com.example.sigebom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.sigebom.model.Bombeiro;

@Repository
public interface BombeiroRepository extends JpaRepository<Bombeiro, Long> {

    Optional<Bombeiro> findByEmail(String email);

    @Query("SELECT b FROM Bombeiro b WHERE b.email = :identificador OR b.cpf = :identificador")
    Optional<Bombeiro> findByIdentificador(@Param("identificador") String identificador);

    List<Bombeiro> findByViaturaIsNullOrderByNome();

    long countByViaturaId(Long viaturaId);
}