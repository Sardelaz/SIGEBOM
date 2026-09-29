package com.example.sigebom.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.sigebom.model.Ocorrencia;
import com.example.sigebom.model.Viatura;
import com.example.sigebom.model.enums.Gravidade;
import com.example.sigebom.model.enums.StatusOcorrencia;
import com.example.sigebom.model.enums.StatusViatura;
import com.example.sigebom.repository.OcorrenciaRepository;
import com.example.sigebom.repository.ViaturaRepository;

@Controller
@RequestMapping("/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaRepository repository;
    private final ViaturaRepository viaturaRepository;

    @Autowired
    public OcorrenciaController(OcorrenciaRepository repository, ViaturaRepository viaturaRepository) {
        this.repository = repository;
        this.viaturaRepository = viaturaRepository;
    }

    @GetMapping
    public String listarOcorrencias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) StatusOcorrencia status,
            @RequestParam(required = false) Gravidade gravidade,
            @PageableDefault(size = 15) Pageable pageable,
            HttpServletRequest request,
            Model model) {
        
        LocalDateTime dataInicio = null;
        LocalDateTime dataFim = null;
        
        // Verifica se a requisição veio do menu (sem parâmetros) ou do botão "Filtrar"
        boolean isFiltro = request.getParameterMap().containsKey("de");

        if (!isFiltro) {
            // Comportamento padrão da US: focado no dia atual
            dataInicio = LocalDate.now().atStartOfDay();
            dataFim = LocalDate.now().atTime(23, 59, 59);
        } else {
            // O usuário utilizou o filtro. Se ele limpar as datas na tela, 
            // as variáveis ficarão nulas, forçando a listagem de todo o histórico.
            if (de != null) dataInicio = de.atStartOfDay();
            if (ate != null) dataFim = ate.atTime(23, 59, 59);
        }

        Page<Ocorrencia> pagina = repository.filtrarOcorrencias(dataInicio, dataFim, status, gravidade, pageable);

        model.addAttribute("ocorrencias", pagina);
        model.addAttribute("statusValues", StatusOcorrencia.values());
        model.addAttribute("gravidadeValues", Gravidade.values());
        model.addAttribute("viaturasDisponiveis", viaturaRepository.findByAtivoTrueAndStatus(StatusViatura.DISPONIVEL));
        
        // Passa as datas usadas para o Front-end refletir visualmente o filtro aplicado
        model.addAttribute("dataPadraoInicio", dataInicio != null ? dataInicio.toLocalDate() : null);
        model.addAttribute("dataPadraoFim", dataFim != null ? dataFim.toLocalDate() : null);

        return "ocorrencias";
    }

    @GetMapping("/nova")
    public String novaForm(Model model) {
        model.addAttribute("ocorrencia", new Ocorrencia());
        model.addAttribute("viaturasOpcoes", viaturaRepository.findByAtivoTrueAndStatus(StatusViatura.DISPONIVEL));
        model.addAttribute("empenhadasIds", new ArrayList<Long>());
        return "cadastro-ocorrencia";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable("id") Long id, Model model) {
        if (id == null) return "redirect:/ocorrencias";
        Ocorrencia ocorrencia = repository.findById(id).orElseThrow();
        model.addAttribute("ocorrencia", ocorrencia);
        
        List<Viatura> viaturasOpcoes = viaturaRepository.findByAtivoTrueAndStatus(StatusViatura.DISPONIVEL);
        viaturasOpcoes.addAll(ocorrencia.getViaturas());
        model.addAttribute("viaturasOpcoes", viaturasOpcoes);
        
        List<Long> empenhadasIds = ocorrencia.getViaturas().stream().map(Viatura::getId).toList();
        model.addAttribute("empenhadasIds", empenhadasIds);
        
        return "cadastro-ocorrencia";
    }

    @PostMapping
    public String registrarOcorrencia(@ModelAttribute Ocorrencia ocorrencia, @RequestParam(value = "viaturaIds", required = false) List<Long> viaturaIds) {
        Ocorrencia existente = ocorrencia.getId() != null ? repository.findById(ocorrencia.getId()).orElse(null) : null;
        
        if (existente != null) {
            ocorrencia.setHorarioAcionamento(existente.getHorarioAcionamento());
            ocorrencia.setStatus(existente.getStatus());
            ocorrencia.setRelatorio(existente.getRelatorio());
            
            for (Viatura vAntiga : existente.getViaturas()) {
                if (viaturaIds == null || !viaturaIds.contains(vAntiga.getId())) {
                    vAntiga.setStatus(StatusViatura.DISPONIVEL);
                    viaturaRepository.save(vAntiga);
                }
            }
        } else {
            ocorrencia.setHorarioAcionamento(LocalDateTime.now());
            ocorrencia.setStatus(StatusOcorrencia.REGISTRADA);
        }
        
        List<Viatura> novasViaturas = new ArrayList<>();
        if (viaturaIds != null && !viaturaIds.isEmpty()) {
            for (Long vId : viaturaIds) {
                if (vId == null) continue;
                Viatura v = viaturaRepository.findById(vId).orElse(null);
                if (v != null) {
                    v.setStatus(StatusViatura.EM_ATENDIMENTO);
                    viaturaRepository.save(v);
                    novasViaturas.add(v);
                }
            }
            ocorrencia.setViaturas(novasViaturas);
            if (ocorrencia.getStatus() == StatusOcorrencia.REGISTRADA) ocorrencia.setStatus(StatusOcorrencia.EMPENHADA);
        } else {
            ocorrencia.setViaturas(new ArrayList<>());
            if (ocorrencia.getStatus() == StatusOcorrencia.EMPENHADA) ocorrencia.setStatus(StatusOcorrencia.REGISTRADA);
        }
        
        repository.save(ocorrencia);
        return "redirect:/ocorrencias";
    }

    @PostMapping("/despacho")
    public String despachoRapido(@RequestParam("id") Long id, @RequestParam("viaturaId") Long viaturaId) {
        if (id == null || viaturaId == null) return "redirect:/ocorrencias";
        Ocorrencia oc = repository.findById(id).orElseThrow();
        Viatura v = viaturaRepository.findById(viaturaId).orElseThrow();
        
        v.setStatus(StatusViatura.EM_ATENDIMENTO);
        viaturaRepository.save(v);
        
        oc.getViaturas().add(v);
        oc.setStatus(StatusOcorrencia.EMPENHADA);
        repository.save(oc);
        
        return "redirect:/ocorrencias";
    }

    @GetMapping("/{id}/boletim")
    public String boletimForm(@PathVariable("id") Long id, Model model) {
        if (id == null) return "redirect:/ocorrencias";
        Ocorrencia ocorrencia = repository.findById(id).orElseThrow();
        model.addAttribute("ocorrencia", ocorrencia);
        return "boletim";
    }

    @PostMapping("/{id}/finalizar")
    public String finalizarOcorrencia(@PathVariable("id") Long id, @RequestParam("relatorio") String relatorio) {
        if (id == null) return "redirect:/ocorrencias";
        Ocorrencia oc = repository.findById(id).orElseThrow();
        
        oc.setRelatorio(relatorio);
        oc.setStatus(StatusOcorrencia.FINALIZADA);
        
        for (Viatura v : oc.getViaturas()) {
            v.setStatus(StatusViatura.DISPONIVEL);
            viaturaRepository.save(v);
        }
        
        repository.save(oc);
        return "redirect:/ocorrencias";
    }
}