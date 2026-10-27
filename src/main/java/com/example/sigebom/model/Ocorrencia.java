package com.example.sigebom.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

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

    // US21: Horário de acionamento (automático na abertura e editável por Atendente/Admin)
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime horarioAcionamento;

    // US21: Horário original gerado pela máquina na abertura (rastro de auditoria interno)
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime horarioAcionamentoOriginal;

    // US21: Indica se o horário de acionamento sofreu edição manual
    private boolean acionamentoEditado = false;

    // US21: Log invisível/interno mantendo o rastro completo das edições de horário
    @Column(columnDefinition = "TEXT")
    private String logAuditoriaInterno;

    // US08: Timestamps registrados nas mudanças de status
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime horarioEmpenho;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime horarioDeslocamento;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime horarioNoLocal;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime horarioEncerramento;

    private String solicitante;
    private String telefoneSolicitante;

    @Enumerated(EnumType.STRING)
    private StatusOcorrencia status;

    @Column(columnDefinition = "TEXT")
    private String relatorio;

    @ManyToMany
    @JoinTable(
        name = "ocorrencia_viatura",
        joinColumns = @JoinColumn(name = "ocorrencia_id"),
        inverseJoinColumns = @JoinColumn(name = "viatura_id")
    )
    private List<Viatura> viaturas = new ArrayList<>();

    public Ocorrencia() {
    }

    /**
     * US08: Registra automaticamente o timestamp correspondente à mudança de status.
     */
    public void registrarMudancaStatus(StatusOcorrencia novoStatus) {
        if (novoStatus == null) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now().withSecond(0).withNano(0);
        this.status = novoStatus;

        switch (novoStatus) {
            case REGISTRADA -> {
                if (this.horarioAcionamento == null) {
                    this.horarioAcionamento = agora;
                }
            }
            case EMPENHADA -> {
                if (this.horarioEmpenho == null) {
                    this.horarioEmpenho = agora;
                }
            }
            case EM_DESLOCAMENTO -> {
                if (this.horarioEmpenho == null) {
                    this.horarioEmpenho = agora;
                }
                if (this.horarioDeslocamento == null) {
                    this.horarioDeslocamento = agora;
                }
            }
            case NO_LOCAL -> {
                if (this.horarioEmpenho == null) {
                    this.horarioEmpenho = agora;
                }
                if (this.horarioDeslocamento == null) {
                    this.horarioDeslocamento = agora;
                }
                if (this.horarioNoLocal == null) {
                    this.horarioNoLocal = agora;
                }
            }
            case FINALIZADA -> {
                if (this.horarioEncerramento == null) {
                    this.horarioEncerramento = agora;
                }
            }
            case CANCELADA -> {
                if (this.horarioEncerramento == null) {
                    this.horarioEncerramento = agora;
                }
            }
        }
    }

    /**
     * US08: Calcula o tempo de resposta em minutos baseado nos timestamps registrados nas mudanças de status.
     */
    public Long calcularTempoResposta() {
        if (horarioAcionamento == null) {
            return null;
        }
        LocalDateTime fim = horarioEncerramento != null ? horarioEncerramento : horarioNoLocal;
        if (fim == null) {
            return null;
        }
        long minutos = Duration.between(horarioAcionamento, fim).toMinutes();
        return Math.max(0L, minutos);
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

    public LocalDateTime getHorarioAcionamentoOriginal() {
        return horarioAcionamentoOriginal;
    }

    public void setHorarioAcionamentoOriginal(LocalDateTime horarioAcionamentoOriginal) {
        this.horarioAcionamentoOriginal = horarioAcionamentoOriginal;
    }

    public boolean isAcionamentoEditado() {
        return acionamentoEditado;
    }

    public void setAcionamentoEditado(boolean acionamentoEditado) {
        this.acionamentoEditado = acionamentoEditado;
    }

    public String getLogAuditoriaInterno() {
        return logAuditoriaInterno;
    }

    public void setLogAuditoriaInterno(String logAuditoriaInterno) {
        this.logAuditoriaInterno = logAuditoriaInterno;
    }

    public LocalDateTime getHorarioEmpenho() {
        return horarioEmpenho;
    }

    public void setHorarioEmpenho(LocalDateTime horarioEmpenho) {
        this.horarioEmpenho = horarioEmpenho;
    }

    public LocalDateTime getHorarioDeslocamento() {
        return horarioDeslocamento;
    }

    public void setHorarioDeslocamento(LocalDateTime horarioDeslocamento) {
        this.horarioDeslocamento = horarioDeslocamento;
    }

    public LocalDateTime getHorarioNoLocal() {
        return horarioNoLocal;
    }

    public void setHorarioNoLocal(LocalDateTime horarioNoLocal) {
        this.horarioNoLocal = horarioNoLocal;
    }

    public LocalDateTime getHorarioEncerramento() {
        return horarioEncerramento;
    }

    public void setHorarioEncerramento(LocalDateTime horarioEncerramento) {
        this.horarioEncerramento = horarioEncerramento;
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