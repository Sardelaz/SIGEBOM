package com.example.sigebom.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.sigebom.model.Ocorrencia;
import com.example.sigebom.model.enums.Gravidade;
import com.example.sigebom.repository.OcorrenciaRepository;

import jakarta.servlet.http.HttpServletRequest;

// US08 e US30 - Relatório de Tempo de Resposta
@Controller
public class RelatorioController {

    private final OcorrenciaRepository ocorrenciaRepository;

    public RelatorioController(OcorrenciaRepository ocorrenciaRepository) {
        this.ocorrenciaRepository = ocorrenciaRepository;
    }

    @GetMapping("/relatorios")
    public String relatorios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Gravidade gravidade,
            HttpServletRequest request,
            Model model) {

        // Sem filtro explícito: últimos 30 dias
        boolean isFiltro = request.getParameterMap().containsKey("de");
        LocalDate inicio = isFiltro ? de : LocalDate.now().minusDays(29);
        LocalDate fim = isFiltro ? ate : LocalDate.now();

        LocalDateTime dataInicio = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime dataFim = fim != null ? fim.atTime(23, 59, 59) : null;

        List<Ocorrencia> ocorrencias = ocorrenciaRepository
                .filtrarOcorrencias(dataInicio, dataFim, null, gravidade, Pageable.unpaged())
                .getContent();

        List<Ocorrencia> ocorrenciasOrdenadas = ocorrencias.stream()
                .filter(o -> o.getHorarioAcionamento() != null)
                .sorted(Comparator.comparing((Ocorrencia o) -> o.getHorarioAcionamento()).reversed())
                .toList();

        List<Ocorrencia> atendidas = ocorrenciasOrdenadas.stream()
                .filter(o -> o.calcularTempoResposta() != null)
                .toList();

        LongSummaryStatistics tempos = atendidas.stream()
                .mapToLong(o -> o.calcularTempoResposta())
                .summaryStatistics();

        List<LinhaGravidade> porGravidade = new ArrayList<>();
        for (Gravidade g : Gravidade.values()) {
            long total = ocorrencias.stream().filter(o -> o.getGravidade() == g).count();
            LongSummaryStatistics t = atendidas.stream()
                    .filter(o -> o.getGravidade() == g)
                    .mapToLong(o -> o.calcularTempoResposta())
                    .summaryStatistics();
            porGravidade.add(new LinhaGravidade(
                    g,
                    total,
                    t.getCount(),
                    t.getCount() > 0 ? Math.round(t.getAverage()) : null
            ));
        }

        model.addAttribute("totalOcorrencias", ocorrencias.size());
        model.addAttribute("totalFinalizadas", atendidas.size());
        model.addAttribute("tempoMedio", atendidas.isEmpty() ? 0 : Math.round(tempos.getAverage()));
        model.addAttribute("menorTempo", atendidas.isEmpty() ? 0 : tempos.getMin());
        model.addAttribute("maiorTempo", atendidas.isEmpty() ? 0 : tempos.getMax());
        model.addAttribute("porGravidade", porGravidade);
        model.addAttribute("atendidas", atendidas);
        model.addAttribute("ocorrenciasPeriodo", ocorrenciasOrdenadas);
        model.addAttribute("dataPadraoInicio", inicio);
        model.addAttribute("dataPadraoFim", fim);

        return "relatorios";
    }

    public static class LinhaGravidade {
        private final Gravidade gravidade;
        private final long total;
        private final long finalizadas;
        private final Long tempoMedio;

        public LinhaGravidade(Gravidade gravidade, long total, long finalizadas, Long tempoMedio) {
            this.gravidade = gravidade;
            this.total = total;
            this.finalizadas = finalizadas;
            this.tempoMedio = tempoMedio;
        }

        public Gravidade getGravidade() {
            return gravidade;
        }

        public long getTotal() {
            return total;
        }

        public long getFinalizadas() {
            return finalizadas;
        }

        public Long getTempoMedio() {
            return tempoMedio;
        }
    }
}