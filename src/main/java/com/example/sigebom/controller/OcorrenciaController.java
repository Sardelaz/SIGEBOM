package com.example.sigebom.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sigebom.model.Ocorrencia;
import com.example.sigebom.model.Viatura;
import com.example.sigebom.model.enums.Gravidade;
import com.example.sigebom.model.enums.StatusOcorrencia;
import com.example.sigebom.model.enums.StatusViatura;
import com.example.sigebom.repository.OcorrenciaRepository;
import com.example.sigebom.repository.ViaturaRepository;
import com.example.sigebom.service.OcorrenciaService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaRepository repository;
    private final ViaturaRepository viaturaRepository;
    private final OcorrenciaService ocorrenciaService;

    @Autowired
    public OcorrenciaController(OcorrenciaRepository repository,
                                ViaturaRepository viaturaRepository,
                                OcorrenciaService ocorrenciaService) {
        this.repository = repository;
        this.viaturaRepository = viaturaRepository;
        this.ocorrenciaService = ocorrenciaService;
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

        boolean isFiltro = request.getParameterMap().containsKey("de");

        if (!isFiltro) {
            dataInicio = LocalDate.now().atStartOfDay();
            dataFim = LocalDate.now().atTime(23, 59, 59);
        } else {
            if (de != null) {
                dataInicio = de.atStartOfDay();
            }
            if (ate != null) {
                dataFim = ate.atTime(23, 59, 59);
            }
        }

        Page<Ocorrencia> pagina = repository.filtrarOcorrencias(dataInicio, dataFim, status, gravidade, pageable);

        model.addAttribute("ocorrencias", pagina);
        model.addAttribute("statusValues", StatusOcorrencia.values());
        model.addAttribute("gravidadeValues", Gravidade.values());
        model.addAttribute("viaturasDisponiveis", viaturaRepository.findByAtivoTrueAndStatus(StatusViatura.DISPONIVEL));
        model.addAttribute("podeEditarFinalizada", ocorrenciaService.usuarioEhAtendenteOuAdmin());

        model.addAttribute("dataPadraoInicio", dataInicio != null ? dataInicio.toLocalDate() : null);
        model.addAttribute("dataPadraoFim", dataFim != null ? dataFim.toLocalDate() : null);

        return "ocorrencias";
    }

    @GetMapping("/nova")
    public String novaForm(Model model) {
        Ocorrencia nova = new Ocorrencia();
        LocalDateTime agoraMaquina = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        nova.setHorarioAcionamento(agoraMaquina);
        nova.setHorarioAcionamentoOriginal(agoraMaquina);

        model.addAttribute("ocorrencia", nova);
        model.addAttribute("statusValues", StatusOcorrencia.values());
        model.addAttribute("viaturasOpcoes", viaturaRepository.findByAtivoTrueAndStatus(StatusViatura.DISPONIVEL));
        model.addAttribute("empenhadasIds", new ArrayList<Long>());
        model.addAttribute("somenteLeitura", false);
        model.addAttribute("ocorrenciaFinalizada", false);
        return "cadastro-ocorrencia";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        if (id == null) {
            return "redirect:/ocorrencias";
        }
        Ocorrencia ocorrencia = repository.findById(id).orElse(null);
        if (ocorrencia == null) {
            ra.addFlashAttribute("erro", "Ocorrência #" + id + " não encontrada.");
            return "redirect:/ocorrencias";
        }

        boolean ocorrenciaFinalizada = ocorrencia.getStatus() == StatusOcorrencia.FINALIZADA;

        boolean somenteLeitura = ocorrenciaFinalizada && !ocorrenciaService.usuarioEhAtendenteOuAdmin();

        model.addAttribute("ocorrencia", ocorrencia);
        model.addAttribute("statusValues", StatusOcorrencia.values());
        model.addAttribute("somenteLeitura", somenteLeitura);
        model.addAttribute("ocorrenciaFinalizada", ocorrenciaFinalizada);

        List<Viatura> viaturasOpcoes;
        if (somenteLeitura || ocorrenciaFinalizada) {
            viaturasOpcoes = new ArrayList<>(ocorrencia.getViaturas());
        } else {
            viaturasOpcoes = new ArrayList<>(viaturaRepository.findByAtivoTrueAndStatus(StatusViatura.DISPONIVEL));
            for (Viatura v : ocorrencia.getViaturas()) {
                if (!viaturasOpcoes.contains(v)) {
                    viaturasOpcoes.add(v);
                }
            }
        }
        model.addAttribute("viaturasOpcoes", viaturasOpcoes);

        List<Long> empenhadasIds = ocorrencia.getViaturas().stream()
                .map(v -> v.getId())
                .toList();
        model.addAttribute("empenhadasIds", empenhadasIds);

        return "cadastro-ocorrencia";
    }

    @PostMapping
    public String registrarOcorrencia(@ModelAttribute Ocorrencia ocorrencia,
                                      @RequestParam(value = "viaturaIds", required = false) List<Long> viaturaIds,
                                      RedirectAttributes ra) {
        try {
            Ocorrencia salva = ocorrenciaService.salvarOuAtualizarOcorrencia(ocorrencia, viaturaIds);
            ra.addFlashAttribute("sucesso", "Ocorrência #" + salva.getId() + " salva com sucesso.");
            return "redirect:/ocorrencias";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            if (ocorrencia.getId() != null) {
                return "redirect:/ocorrencias/" + ocorrencia.getId() + "/editar";
            }
            return "redirect:/ocorrencias/nova";
        }
    }

    @PostMapping("/despacho")
    public String despachoRapido(@RequestParam("id") Long id,
                                 @RequestParam("viaturaId") Long viaturaId,
                                 RedirectAttributes ra) {
        try {
            ocorrenciaService.despachoRapido(id, viaturaId);
            ra.addFlashAttribute("sucesso", "Viatura empenhada na ocorrência #" + id + " com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/ocorrencias";
    }

    @PostMapping("/{id}/avancar-status")
    public String avancarStatusSequencial(@PathVariable("id") Long id,
                                          @RequestParam("novoStatus") StatusOcorrencia novoStatus,
                                          @RequestParam(value = "origem", required = false) String origem,
                                          RedirectAttributes ra) {
        try {
            Ocorrencia atualizada = ocorrenciaService.avancarStatusSequencial(id, novoStatus);
            ra.addFlashAttribute("sucesso",
                    "Ocorrência #" + id + " atualizada para \"" + atualizada.getStatus().getRotulo() + "\" com horário registrado.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }

        if ("editar".equals(origem)) {
            return "redirect:/ocorrencias/" + id + "/editar";
        }
        return "redirect:/ocorrencias";
    }

    @GetMapping("/{id}/boletim")
    public String boletimForm(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        if (id == null) {
            return "redirect:/ocorrencias";
        }
        Ocorrencia ocorrencia = repository.findById(id).orElse(null);
        if (ocorrencia == null) {
            ra.addFlashAttribute("erro", "Ocorrência #" + id + " não encontrada.");
            return "redirect:/ocorrencias";
        }

        // US25: Bloqueia acesso ao encerramento se o fluxo obrigatório não chegou em NO_LOCAL
        if (!ocorrencia.isPodeFinalizar()) {
            ra.addFlashAttribute("erro",
                    "A ocorrência #" + id + " precisa avançar sequencialmente por Em Deslocamento e No Local antes de ser finalizada.");
            return "redirect:/ocorrencias";
        }

        model.addAttribute("ocorrencia", ocorrencia);
        return "boletim";
    }

    @PostMapping("/{id}/finalizar")
    public String finalizarOcorrencia(@PathVariable("id") Long id,
                                      @RequestParam("relatorio") String relatorio,
                                      RedirectAttributes ra) {
        try {
            ocorrenciaService.finalizarOcorrencia(id, relatorio);
            ra.addFlashAttribute("sucesso", "Ocorrência #" + id + " finalizada e viaturas liberadas com sucesso.");
            return "redirect:/ocorrencias";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/ocorrencias/" + id + "/boletim";
        }
    }
}