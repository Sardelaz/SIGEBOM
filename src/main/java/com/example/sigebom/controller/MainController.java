package com.example.sigebom.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.sigebom.model.Ocorrencia;
import com.example.sigebom.model.Viatura;
import com.example.sigebom.model.enums.StatusOcorrencia;
import com.example.sigebom.model.enums.StatusViatura;
import com.example.sigebom.repository.BombeiroRepository;
import com.example.sigebom.repository.OcorrenciaRepository;
import com.example.sigebom.repository.ViaturaRepository;

@Controller
public class MainController {

    private final BombeiroRepository bombeiroRepository;
    private final ViaturaRepository viaturaRepository;
    private final OcorrenciaRepository ocorrenciaRepository;

    public MainController(BombeiroRepository bombeiroRepository,
                          ViaturaRepository viaturaRepository,
                          OcorrenciaRepository ocorrenciaRepository) {
        this.bombeiroRepository = bombeiroRepository;
        this.viaturaRepository = viaturaRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "index";
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/painel";
    }

    @GetMapping("/painel")
    public String painel(Model model) {

        List<Ocorrencia> todasOcorrencias = ocorrenciaRepository.findAll();
        List<Ocorrencia> ocorrenciasAbertas = todasOcorrencias.stream()
                .filter(o -> o.getStatus() != null &&
                             o.getStatus() != StatusOcorrencia.FINALIZADA &&
                             o.getStatus() != StatusOcorrencia.CANCELADA)
                .collect(Collectors.toList());

        List<Viatura> todasViaturas = viaturaRepository.findAll();
        List<Viatura> viaturasAtivas = todasViaturas.stream()
                .filter(v -> v.isAtivo())
                .collect(Collectors.toList());

        long viaturasDisponiveis = viaturasAtivas.stream()
                .filter(v -> v.getStatus() != null && v.getStatus() == StatusViatura.DISPONIVEL)
                .count();

        long efetivoDisponivel = bombeiroRepository.findAll().stream()
                .filter(b -> b.isAtivo())
                .count();

        model.addAttribute("totalAbertas", ocorrenciasAbertas.size());
        model.addAttribute("viaturasDisponiveis", viaturasDisponiveis);
        model.addAttribute("efetivoDisponivel", efetivoDisponivel);

        LocalDateTime inicioPeriodo = LocalDate.now().minusDays(29).atStartOfDay();
        double tempoMedio = todasOcorrencias.stream()
                .filter(o -> o.calcularTempoResposta() != null &&
                             o.getHorarioAcionamento() != null &&
                             !o.getHorarioAcionamento().isBefore(inicioPeriodo))
                .mapToLong(o -> o.calcularTempoResposta())
                .average()
                .orElse(0);

        model.addAttribute("tempoMedio", Math.round(tempoMedio));
        model.addAttribute("ocorrenciasAbertas", ocorrenciasAbertas);
        model.addAttribute("viaturasAtivas", viaturasAtivas);

        return "painel";
    }
}