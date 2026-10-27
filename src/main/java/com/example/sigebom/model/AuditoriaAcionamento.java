package com.example.sigebom.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// US21: Registro interno de auditoria para edições manuais do timestamp de acionamento
@Entity
@Table(name = "auditoria_acionamento")
public class AuditoriaAcionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ocorrenciaId;

    @Column(nullable = false)
    private LocalDateTime horarioOriginalMaquina;

    @Column(nullable = false)
    private LocalDateTime horarioAnterior;

    @Column(nullable = false)
    private LocalDateTime horarioNovo;

    @Column(nullable = false)
    private LocalDateTime dataModificacao;

    private String usuarioResponsavel;

    public AuditoriaAcionamento() {
    }

    public AuditoriaAcionamento(Long ocorrenciaId,
                                LocalDateTime horarioOriginalMaquina,
                                LocalDateTime horarioAnterior,
                                LocalDateTime horarioNovo,
                                LocalDateTime dataModificacao,
                                String usuarioResponsavel) {
        this.ocorrenciaId = ocorrenciaId;
        this.horarioOriginalMaquina = horarioOriginalMaquina;
        this.horarioAnterior = horarioAnterior;
        this.horarioNovo = horarioNovo;
        this.dataModificacao = dataModificacao;
        this.usuarioResponsavel = usuarioResponsavel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOcorrenciaId() {
        return ocorrenciaId;
    }

    public void setOcorrenciaId(Long ocorrenciaId) {
        this.ocorrenciaId = ocorrenciaId;
    }

    public LocalDateTime getHorarioOriginalMaquina() {
        return horarioOriginalMaquina;
    }

    public void setHorarioOriginalMaquina(LocalDateTime horarioOriginalMaquina) {
        this.horarioOriginalMaquina = horarioOriginalMaquina;
    }

    public LocalDateTime getHorarioAnterior() {
        return horarioAnterior;
    }

    public void setHorarioAnterior(LocalDateTime horarioAnterior) {
        this.horarioAnterior = horarioAnterior;
    }

    public LocalDateTime getHorarioNovo() {
        return horarioNovo;
    }

    public void setHorarioNovo(LocalDateTime horarioNovo) {
        this.horarioNovo = horarioNovo;
    }

    public LocalDateTime getDataModificacao() {
        return dataModificacao;
    }

    public void setDataModificacao(LocalDateTime dataModificacao) {
        this.dataModificacao = dataModificacao;
    }

    public String getUsuarioResponsavel() {
        return usuarioResponsavel;
    }

    public void setUsuarioResponsavel(String usuarioResponsavel) {
        this.usuarioResponsavel = usuarioResponsavel;
    }
}