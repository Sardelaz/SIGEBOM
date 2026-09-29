package com.example.sigebom.model;

import com.example.sigebom.model.enums.StatusViatura;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "viaturas")
public class Viatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String prefixo;
    private String tipo;
    private String placa;
    
    @Enumerated(EnumType.STRING)
    private StatusViatura status;
    
    private boolean ativo = true;

    public Viatura() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPrefixo() { return prefixo; }
    public void setPrefixo(String prefixo) { this.prefixo = prefixo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public StatusViatura getStatus() { return status; }
    public void setStatus(StatusViatura status) { this.status = status; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}