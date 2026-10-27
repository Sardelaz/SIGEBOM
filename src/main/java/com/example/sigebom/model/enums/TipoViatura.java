package com.example.sigebom.model.enums;

public enum TipoViatura {
    CARRO("Carro", 5),
    MOTO("Moto", 2),
    CAMINHAO("Caminhão", 7);

    private final String rotulo;
    private final int capacidade;

    TipoViatura(String rotulo, int capacidade) {
        this.rotulo = rotulo;
        this.capacidade = capacidade;
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getCapacidade() {
        return capacidade;
    }
}