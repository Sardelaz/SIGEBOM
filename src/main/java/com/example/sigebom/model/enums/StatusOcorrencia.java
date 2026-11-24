package com.example.sigebom.model.enums;

public enum StatusOcorrencia {
    REGISTRADA("Registrada"),
    EMPENHADA("Empenhada"),
    EM_DESLOCAMENTO("Em Deslocamento"),
    NO_LOCAL("No Local"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada");

    private final String rotulo;

    StatusOcorrencia(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public StatusOcorrencia getProximoSequencial() {
        return switch (this) {
            case EMPENHADA -> EM_DESLOCAMENTO;
            case EM_DESLOCAMENTO -> NO_LOCAL;
            case NO_LOCAL -> FINALIZADA;
            default -> null;
        };
    }

    public String getRotuloAcaoProxima() {
        return switch (this) {
            case EMPENHADA -> "Iniciar Deslocamento";
            case EM_DESLOCAMENTO -> "Chegada no Local";
            case NO_LOCAL -> "Finalizar";
            default -> null;
        };
    }

    public boolean podeAvancarSequencialmentePara(StatusOcorrencia destino) {
        return destino != null && destino == getProximoSequencial();
    }
}