package com.example.sigebom.model;

import java.util.ArrayList;
import java.util.List;

import com.example.sigebom.model.enums.StatusViatura;
import com.example.sigebom.model.enums.TipoViatura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "viatura")
public class Viatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String prefixo;

    @Column(unique = true)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoViatura tipo = TipoViatura.CAMINHAO;

    @Enumerated(EnumType.STRING)
    private StatusViatura status = StatusViatura.DISPONIVEL;

    private boolean ativo = true;

    @OneToMany(mappedBy = "viatura")
    private List<Bombeiro> bombeiros = new ArrayList<>();

    public Viatura() {
    }

    public Viatura(String prefixo, String placa, TipoViatura tipo) {
        this.prefixo = prefixo;
        this.placa = placa;
        this.tipo = tipo;
        this.status = StatusViatura.DISPONIVEL;
        this.ativo = true;
    }

    public int getCapacidade() {
        return tipo != null ? tipo.getCapacidade() : 0;
    }

    public int getVagasLivres() {
        int ocupadas = bombeiros != null ? bombeiros.size() : 0;
        return Math.max(0, getCapacidade() - ocupadas);
    }

    public boolean isCheia() {
        int ocupadas = bombeiros != null ? bombeiros.size() : 0;
        return getCapacidade() > 0 && ocupadas >= getCapacidade();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrefixo() {
        return prefixo;
    }

    public void setPrefixo(String prefixo) {
        this.prefixo = prefixo;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public TipoViatura getTipo() {
        return tipo;
    }

    public void setTipo(TipoViatura tipo) {
        this.tipo = tipo;
    }

    public StatusViatura getStatus() {
        return status;
    }

    public void setStatus(StatusViatura status) {
        this.status = status;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public List<Bombeiro> getBombeiros() {
        return bombeiros;
    }

    public void setBombeiros(List<Bombeiro> bombeiros) {
        this.bombeiros = bombeiros;
    }
}