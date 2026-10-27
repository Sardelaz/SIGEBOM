package com.example.sigebom.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "equipe_membros")
public class MembroEquipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "bombeiro_id", unique = true, nullable = false)
    private Bombeiro bombeiro;

    @ManyToOne
    @JoinColumn(name = "viatura_id", nullable = false)
    private Viatura viatura;

    public MembroEquipe() {
    }

    public MembroEquipe(Bombeiro bombeiro, Viatura viatura) {
        this.bombeiro = bombeiro;
        this.viatura = viatura;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bombeiro getBombeiro() {
        return bombeiro;
    }

    public void setBombeiro(Bombeiro bombeiro) {
        this.bombeiro = bombeiro;
    }

    public Viatura getViatura() {
        return viatura;
    }

    public void setViatura(Viatura viatura) {
        this.viatura = viatura;
    }
}