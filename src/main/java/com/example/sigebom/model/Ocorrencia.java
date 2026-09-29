package com.example.sigebom.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.sigebom.model.enums.Gravidade;
import com.example.sigebom.model.enums.StatusOcorrencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "ocorrencias")
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo;
    private String descricao;
    private String endereco;
    private String bairro;

    @Enumerated(EnumType.STRING)
    private Gravidade gravidade;

    private LocalDateTime horarioAcionamento;
    private String solicitante;
    private String telefoneSolicitante;

    @Enumerated(EnumType.STRING)
    private StatusOcorrencia status;

    @Column(columnDefinition = "TEXT")
    private String relatorio;

    @ManyToMany
    @JoinTable(name = "ocorrencia_viatura", joinColumns = @JoinColumn(name = "ocorrencia_id"), inverseJoinColumns = @JoinColumn(name = "viatura_id"))
    private List<Viatura> viaturas = new ArrayList<>();

    public Ocorrencia() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public Gravidade getGravidade() {
        return gravidade;
    }

    public void setGravidade(Gravidade gravidade) {
        this.gravidade = gravidade;
    }

    public LocalDateTime getHorarioAcionamento() {
        return horarioAcionamento;
    }

    public void setHorarioAcionamento(LocalDateTime horarioAcionamento) {
        this.horarioAcionamento = horarioAcionamento;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante;
    }

    public String getTelefoneSolicitante() {
        return telefoneSolicitante;
    }

    public void setTelefoneSolicitante(String telefoneSolicitante) {
        this.telefoneSolicitante = telefoneSolicitante;
    }

    public StatusOcorrencia getStatus() {
        return status;
    }

    public void setStatus(StatusOcorrencia status) {
        this.status = status;
    }

    public String getRelatorio() {
        return relatorio;
    }

    public void setRelatorio(String relatorio) {
        this.relatorio = relatorio;
    }

    public List<Viatura> getViaturas() {
        return viaturas;
    }

    public void setViaturas(List<Viatura> viaturas) {
        this.viaturas = viaturas;
    }
}