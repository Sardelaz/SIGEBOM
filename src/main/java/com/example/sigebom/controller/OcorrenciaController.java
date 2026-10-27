package com.example.sigebom.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.sigebom.model.AuditoriaAcionamento;
import com.example.sigebom.model.Ocorrencia;
import com.example.sigebom.model.Viatura;
import com.example.sigebom.model.enums.Gravidade;
import com.example.sigebom.model.enums.StatusOcorrencia;
import com.example.sigebom.model.enums.StatusViatura;
import com.example.sigebom.repository.AuditoriaAcionamentoRepository;
import com.example.sigebom.repository.OcorrenciaRepository;
import com.example.sigebom.repository.ViaturaRepository;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/ocorrencias")
public class OcorrenciaController {

    private static final Logger logger = LoggerFactory.getLogger(OcorrenciaController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final OcorrenciaRepository repository;
    private final ViaturaRepository viaturaRepository;
    private final AuditoriaAcionamentoRepository auditoriaRepository;

    @Autowired
    public OcorrenciaController(OcorrenciaRepository repository,
                                ViaturaRepository viaturaRepository,
                                AuditoriaAcionamentoRepository auditoriaRepository) {
        this.repository = repository;
        this.viaturaRepository = viaturaRepository;
        this.auditoriaRepository = auditoriaRepository;
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

        model.addAttribute("dataPadraoInicio", dataInicio != null ? dataInicio.toLocalDate() : null);
        model.addAttribute("dataPadraoFim", dataFim != null ? dataFim.toLocalDate() : null);

        return "ocorrencias";
    }

    @GetMapping("/nova")
    public String novaForm(Model model) {
        Ocorrencia nova = new Ocorrencia();
        // US21: Horário automático gerado pela máquina na abertura
        LocalDateTime agoraMaquina = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        nova.setHorarioAcionamento(agoraMaquina);
        nova.setHorarioAcionamentoOriginal(agoraMaquina);

        model.addAttribute("ocorrencia", nova);
        model.addAttribute("statusValues", StatusOcorrencia.values());
        model.addAttribute("viaturasOpcoes", viaturaRepository.findByAtivoTrueAndStatus(StatusViatura.DISPONIVEL));
        model.addAttribute("empenhadasIds", new ArrayList<Long>());
        return "cadastro-ocorrencia";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable("id") Long id, Model model) {
        if (id == null) {
            return "redirect:/ocorrencias";
        }
        Ocorrencia ocorrencia = repository.findById(id).orElseThrow();
        model.addAttribute("ocorrencia", ocorrencia);
        model.addAttribute("statusValues", StatusOcorrencia.values());

        List<Viatura> viaturasOpcoes = new ArrayList<>(viaturaRepository.findByAtivoTrueAndStatus(StatusViatura.DISPONIVEL));
        for (Viatura v : ocorrencia.getViaturas()) {
            if (!viaturasOpcoes.contains(v)) {
                viaturasOpcoes.add(v);
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
                                      @RequestParam(value = "viaturaIds", required = false) List<Long> viaturaIds) {
        Long ocorrenciaId = ocorrencia.getId();
        Ocorrencia existente = ocorrenciaId != null ? repository.findById(ocorrenciaId).orElse(null) : null;
        LocalDateTime agoraMaquina = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        if (existente != null) {
            // Preserva o horário original gerado pela máquina na abertura
            LocalDateTime originalMaquina = existente.getHorarioAcionamentoOriginal() != null
                    ? existente.getHorarioAcionamentoOriginal()
                    : existente.getHorarioAcionamento();
            ocorrencia.setHorarioAcionamentoOriginal(originalMaquina);
            ocorrencia.setLogAuditoriaInterno(existente.getLogAuditoriaInterno());
            ocorrencia.setAcionamentoEditado(existente.isAcionamentoEditado());

            // Preserva os timestamps de mudança de status anteriores
            ocorrencia.setHorarioEmpenho(existente.getHorarioEmpenho());
            ocorrencia.setHorarioDeslocamento(existente.getHorarioDeslocamento());
            ocorrencia.setHorarioNoLocal(existente.getHorarioNoLocal());
            ocorrencia.setHorarioEncerramento(existente.getHorarioEncerramento());
            ocorrencia.setRelatorio(existente.getRelatorio());

            if (ocorrencia.getStatus() == null) {
                ocorrencia.setStatus(existente.getStatus());
            }

            // US21: Verifica se o Atendente/Admin editou manualmente o horário de acionamento
            LocalDateTime horarioAnterior = existente.getHorarioAcionamento();
            LocalDateTime horarioInformado = ocorrencia.getHorarioAcionamento();

            if (horarioInformado != null && usuarioPodeEditarHorario()) {
                LocalDateTime antTrunc = horarioAnterior != null ? horarioAnterior.truncatedTo(ChronoUnit.MINUTES) : null;
                LocalDateTime novoTrunc = horarioInformado.truncatedTo(ChronoUnit.MINUTES);

                if (antTrunc == null || !antTrunc.equals(novoTrunc)) {
                    ocorrencia.setHorarioAcionamento(novoTrunc);
                    ocorrencia.setAcionamentoEditado(true);
                    registrarLogAuditoriaInterno(ocorrencia, originalMaquina, horarioAnterior, novoTrunc);
                } else {
                    ocorrencia.setHorarioAcionamento(horarioAnterior);
                }
            } else {
                ocorrencia.setHorarioAcionamento(horarioAnterior);
            }

            // Libera viaturas que foram desmarcadas na edição
            for (Viatura vAntiga : existente.getViaturas()) {
                if (viaturaIds == null || !viaturaIds.contains(vAntiga.getId())) {
                    vAntiga.setStatus(StatusViatura.DISPONIVEL);
                    viaturaRepository.save(vAntiga);
                }
            }
        } else {
            // US21: Abertura de nova ocorrência — grava horário automático gerado pela máquina
            LocalDateTime originalAbertura = ocorrencia.getHorarioAcionamentoOriginal() != null
                    ? ocorrencia.getHorarioAcionamentoOriginal().truncatedTo(ChronoUnit.MINUTES)
                    : agoraMaquina;
            ocorrencia.setHorarioAcionamentoOriginal(originalAbertura);

            if (ocorrencia.getHorarioAcionamento() == null || !usuarioPodeEditarHorario()) {
                ocorrencia.setHorarioAcionamento(originalAbertura);
                ocorrencia.setAcionamentoEditado(false);
            } else {
                LocalDateTime informadoTrunc = ocorrencia.getHorarioAcionamento().truncatedTo(ChronoUnit.MINUTES);
                if (!informadoTrunc.equals(originalAbertura)) {
                    ocorrencia.setHorarioAcionamento(informadoTrunc);
                    ocorrencia.setAcionamentoEditado(true);
                    registrarLogAuditoriaInterno(ocorrencia, originalAbertura, originalAbertura, informadoTrunc);
                } else {
                    ocorrencia.setHorarioAcionamento(originalAbertura);
                    ocorrencia.setAcionamentoEditado(false);
                }
            }

            if (ocorrencia.getStatus() == null) {
                ocorrencia.setStatus(StatusOcorrencia.REGISTRADA);
            }
        }

        // Gerenciamento das viaturas empenhadas
        List<Viatura> novasViaturas = new ArrayList<>();
        if (viaturaIds != null && !viaturaIds.isEmpty()) {
            for (Long vId : viaturaIds) {
                if (vId == null) {
                    continue;
                }
                Viatura v = viaturaRepository.findById(vId).orElse(null);
                if (v != null) {
                    if (ocorrencia.getStatus() != StatusOcorrencia.FINALIZADA &&
                        ocorrencia.getStatus() != StatusOcorrencia.CANCELADA) {
                        v.setStatus(StatusViatura.EM_ATENDIMENTO);
                    } else {
                        v.setStatus(StatusViatura.DISPONIVEL);
                    }
                    viaturaRepository.save(v);
                    novasViaturas.add(v);
                }
            }
            ocorrencia.setViaturas(novasViaturas);
            if (ocorrencia.getStatus() == StatusOcorrencia.REGISTRADA) {
                ocorrencia.setStatus(StatusOcorrencia.EMPENHADA);
            }
        } else {
            ocorrencia.setViaturas(new ArrayList<>());
            if (ocorrencia.getStatus() == StatusOcorrencia.EMPENHADA) {
                ocorrencia.setStatus(StatusOcorrencia.REGISTRADA);
            }
        }

        // US08: Registra o timestamp da mudança de status correspondente
        ocorrencia.registrarMudancaStatus(ocorrencia.getStatus());

        // Se foi alterada para FINALIZADA ou CANCELADA na edição, libera viaturas
        if (ocorrencia.getStatus() == StatusOcorrencia.FINALIZADA ||
            ocorrencia.getStatus() == StatusOcorrencia.CANCELADA) {
            for (Viatura v : ocorrencia.getViaturas()) {
                v.setStatus(StatusViatura.DISPONIVEL);
                viaturaRepository.save(v);
            }
        }

        Ocorrencia salva = repository.save(ocorrencia);

        // Caso tenha sido editada já na criação, vincula o ID na tabela de auditoria
        if (existente == null && salva.isAcionamentoEditado()) {
            salvarEntidadeAuditoria(salva.getId(),
                    salva.getHorarioAcionamentoOriginal(),
                    salva.getHorarioAcionamentoOriginal(),
                    salva.getHorarioAcionamento());
        }

        return "redirect:/ocorrencias";
    }

    @PostMapping("/despacho")
    public String despachoRapido(@RequestParam("id") Long id, @RequestParam("viaturaId") Long viaturaId) {
        if (id == null || viaturaId == null) {
            return "redirect:/ocorrencias";
        }
        Ocorrencia oc = repository.findById(id).orElseThrow();
        Viatura v = viaturaRepository.findById(viaturaId).orElseThrow();

        v.setStatus(StatusViatura.EM_ATENDIMENTO);
        viaturaRepository.save(v);

        oc.getViaturas().add(v);
        oc.registrarMudancaStatus(StatusOcorrencia.EMPENHADA);
        repository.save(oc);

        return "redirect:/ocorrencias";
    }

    @PostMapping("/{id}/status")
    public String atualizarStatusRapido(@PathVariable("id") Long id,
                                        @RequestParam("novoStatus") StatusOcorrencia novoStatus) {
        if (id == null || novoStatus == null) {
            return "redirect:/ocorrencias";
        }
        Ocorrencia oc = repository.findById(id).orElseThrow();
        oc.registrarMudancaStatus(novoStatus);

        if (novoStatus == StatusOcorrencia.FINALIZADA || novoStatus == StatusOcorrencia.CANCELADA) {
            for (Viatura v : oc.getViaturas()) {
                v.setStatus(StatusViatura.DISPONIVEL);
                viaturaRepository.save(v);
            }
        }
        repository.save(oc);
        return "redirect:/ocorrencias";
    }

    @GetMapping("/{id}/boletim")
    public String boletimForm(@PathVariable("id") Long id, Model model) {
        if (id == null) {
            return "redirect:/ocorrencias";
        }
        Ocorrencia ocorrencia = repository.findById(id).orElseThrow();
        model.addAttribute("ocorrencia", ocorrencia);
        return "boletim";
    }

    @PostMapping("/{id}/finalizar")
    public String finalizarOcorrencia(@PathVariable("id") Long id, @RequestParam("relatorio") String relatorio) {
        if (id == null) {
            return "redirect:/ocorrencias";
        }
        Ocorrencia oc = repository.findById(id).orElseThrow();

        oc.setRelatorio(relatorio);
        oc.setHorarioEncerramento(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        oc.registrarMudancaStatus(StatusOcorrencia.FINALIZADA);

        for (Viatura v : oc.getViaturas()) {
            v.setStatus(StatusViatura.DISPONIVEL);
            viaturaRepository.save(v);
        }

        repository.save(oc);
        return "redirect:/ocorrencias";
    }

    private boolean usuarioPodeEditarHorario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ATENDENTE") || a.getAuthority().equals("ADMINISTRADOR"));
    }

    private String obterUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "SISTEMA";
    }

    private void registrarLogAuditoriaInterno(Ocorrencia ocorrencia,
                                              LocalDateTime horarioOriginalMaquina,
                                              LocalDateTime horarioAnterior,
                                              LocalDateTime horarioNovo) {
        String usuario = obterUsuarioLogado();
        LocalDateTime momentoEdicao = LocalDateTime.now();

        String linhaLog = String.format(
                "[AUDITORIA INTERNA US21] Data Edicao: %s | Usuario: %s | Horario Original (Maquina): %s | Horario Anterior: %s | Novo Horario Manual: %s",
                momentoEdicao.format(FORMATTER),
                usuario,
                horarioOriginalMaquina != null ? horarioOriginalMaquina.format(FORMATTER) : "N/A",
                horarioAnterior != null ? horarioAnterior.format(FORMATTER) : "N/A",
                horarioNovo != null ? horarioNovo.format(FORMATTER) : "N/A"
        );

        String logAcumulado = ocorrencia.getLogAuditoriaInterno();
        if (logAcumulado == null || logAcumulado.isBlank()) {
            ocorrencia.setLogAuditoriaInterno(linhaLog);
        } else {
            ocorrencia.setLogAuditoriaInterno(logAcumulado + "\n" + linhaLog);
        }

        // Log invisível/interno no console do servidor
        logger.info(linhaLog);

        if (ocorrencia.getId() != null) {
            salvarEntidadeAuditoria(ocorrencia.getId(), horarioOriginalMaquina, horarioAnterior, horarioNovo);
        }
    }

    private void salvarEntidadeAuditoria(Long ocorrenciaId,
                                         LocalDateTime originalMaquina,
                                         LocalDateTime anterior,
                                         LocalDateTime novo) {
        if (ocorrenciaId == null) {
            return;
        }
        AuditoriaAcionamento auditoria = new AuditoriaAcionamento(
                ocorrenciaId,
                originalMaquina != null ? originalMaquina : LocalDateTime.now(),
                anterior != null ? anterior : LocalDateTime.now(),
                novo != null ? novo : LocalDateTime.now(),
                LocalDateTime.now(),
                obterUsuarioLogado()
        );
        auditoriaRepository.save(auditoria);
    }
}