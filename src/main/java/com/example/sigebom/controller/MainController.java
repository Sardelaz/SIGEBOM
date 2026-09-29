package com.example.sigebom.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.sigebom.model.Bombeiro;
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

    // Construtor nativo para injeção das dependências do Banco de Dados
    public MainController(BombeiroRepository bombeiroRepository,
                           ViaturaRepository viaturaRepository,
                           OcorrenciaRepository ocorrenciaRepository) {
        this.bombeiroRepository = bombeiroRepository;
        this.viaturaRepository = viaturaRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
    }

    // Entrega a página index.html quando o usuário acessar /login
    @GetMapping("/login")
    public String login() {
        return "index";
    }

    // Se o usuário acessar a raiz do site, joga direto para o painel
    @GetMapping("/")
    public String raiz() {
        return "redirect:/painel";
    }

    // Entrega a página painel.html já com os dados populados
    @GetMapping("/painel")
    public String painel(Model model) {
        
        // 1. OCORRÊNCIAS ABERTAS (Tudo que não for Finalizado ou Cancelado)
        List<Ocorrencia> todasOcorrencias = ocorrenciaRepository.findAll();
        List<Ocorrencia> ocorrenciasAbertas = todasOcorrencias.stream()
                .filter(o -> o.getStatus() != null && 
                             o.getStatus() != StatusOcorrencia.FINALIZADA && 
                             o.getStatus() != StatusOcorrencia.CANCELADA)
                .collect(Collectors.toList());

        // 2. VIATURAS (Lista de frota ativa e contagem das disponíveis)
        List<Viatura> todasViaturas = viaturaRepository.findAll();
        List<Viatura> viaturasAtivas = todasViaturas.stream()
                .filter(Viatura::isAtivo)
                .collect(Collectors.toList());
        
        long viaturasDisponiveis = viaturasAtivas.stream()
                .filter(v -> v.getStatus() != null && v.getStatus() == StatusViatura.DISPONIVEL)
                .count();

        // 3. EFETIVO DISPONÍVEL (Contagem de bombeiros ativos na corporação)
        long efetivoDisponivel = bombeiroRepository.findAll().stream()
                .filter(Bombeiro::isAtivo)
                .count();

        // Injeta os dados no Thymeleaf para a tela exibir
        model.addAttribute("totalAbertas", ocorrenciasAbertas.size());
        model.addAttribute("viaturasDisponiveis", viaturasDisponiveis);
        model.addAttribute("efetivoDisponivel", efetivoDisponivel);
        model.addAttribute("tempoMedio", 0); // Fica 0 até o módulo de relatórios ficar pronto
        model.addAttribute("ocorrenciasAbertas", ocorrenciasAbertas);
        model.addAttribute("viaturasAtivas", viaturasAtivas);

        return "painel";
    }
}