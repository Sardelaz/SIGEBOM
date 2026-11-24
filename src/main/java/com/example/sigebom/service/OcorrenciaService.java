package com.example.sigebom.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sigebom.model.AuditoriaAcionamento;
import com.example.sigebom.model.Ocorrencia;
import com.example.sigebom.model.Viatura;
import com.example.sigebom.model.enums.StatusOcorrencia;
import com.example.sigebom.model.enums.StatusViatura;
import com.example.sigebom.repository.AuditoriaAcionamentoRepository;
import com.example.sigebom.repository.OcorrenciaRepository;
import com.example.sigebom.repository.ViaturaRepository;

@Service
public class OcorrenciaService {

    private static final Logger logger = LoggerFactory.getLogger(OcorrenciaService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final OcorrenciaRepository ocorrenciaRepository;
    private final ViaturaRepository viaturaRepository;
    private final AuditoriaAcionamentoRepository auditoriaRepository;

    public OcorrenciaService(OcorrenciaRepository ocorrenciaRepository,
                             ViaturaRepository viaturaRepository,
                             AuditoriaAcionamentoRepository auditoriaRepository) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.viaturaRepository = viaturaRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    public boolean usuarioEhAtendenteOuAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ATENDENTE") || a.getAuthority().equals("ADMINISTRADOR"));
    }

    @Transactional(rollbackFor = Exception.class)
    public Ocorrencia salvarOuAtualizarOcorrencia(Ocorrencia ocorrenciaForm, List<Long> viaturaIds) {
        Long ocorrenciaId = ocorrenciaForm.getId();
        Ocorrencia existente = ocorrenciaId != null
                ? ocorrenciaRepository.findById(ocorrenciaId)
                    .orElseThrow(() -> new IllegalArgumentException("Ocorrência #" + ocorrenciaId + " não encontrada."))
                : null;

        boolean existenteFinalizada = existente != null && existente.getStatus() == StatusOcorrencia.FINALIZADA;

        if (existenteFinalizada && !usuarioEhAtendenteOuAdmin()) {
            throw new IllegalStateException("Apenas Atendentes ou Administradores podem editar ocorrências já finalizadas.");
        }

        LocalDateTime agoraMaquina = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        Set<Long> viaturasJaVinculadas = new HashSet<>();

        if (existente != null) {
            LocalDateTime originalMaquina = existente.getHorarioAcionamentoOriginal() != null
                    ? existente.getHorarioAcionamentoOriginal()
                    : existente.getHorarioAcionamento();

            ocorrenciaForm.setHorarioAcionamentoOriginal(originalMaquina);
            ocorrenciaForm.setLogAuditoriaInterno(existente.getLogAuditoriaInterno());
            ocorrenciaForm.setAcionamentoEditado(existente.isAcionamentoEditado());
            ocorrenciaForm.setHorarioEmpenho(existente.getHorarioEmpenho());
            ocorrenciaForm.setHorarioDeslocamento(existente.getHorarioDeslocamento());
            ocorrenciaForm.setHorarioNoLocal(existente.getHorarioNoLocal());
            ocorrenciaForm.setHorarioEncerramento(existente.getHorarioEncerramento());

            if (existenteFinalizada && ocorrenciaForm.getRelatorio() != null) {
                ocorrenciaForm.setRelatorio(ocorrenciaForm.getRelatorio().trim());
            } else {
                ocorrenciaForm.setRelatorio(existente.getRelatorio());
            }

            ocorrenciaForm.setStatus(existente.getStatus());

            LocalDateTime horarioAnterior = existente.getHorarioAcionamento();
            LocalDateTime horarioInformado = ocorrenciaForm.getHorarioAcionamento();

            if (horarioInformado != null && usuarioEhAtendenteOuAdmin()) {
                LocalDateTime antTrunc = horarioAnterior != null ? horarioAnterior.truncatedTo(ChronoUnit.MINUTES) : null;
                LocalDateTime novoTrunc = horarioInformado.truncatedTo(ChronoUnit.MINUTES);

                if (antTrunc == null || !antTrunc.equals(novoTrunc)) {
                    ocorrenciaForm.setHorarioAcionamento(novoTrunc);
                    ocorrenciaForm.setAcionamentoEditado(true);
                    registrarLogAuditoriaInterno(ocorrenciaForm, originalMaquina, horarioAnterior, novoTrunc);
                } else {
                    ocorrenciaForm.setHorarioAcionamento(horarioAnterior);
                }
            } else {
                ocorrenciaForm.setHorarioAcionamento(horarioAnterior);
            }

            if (existenteFinalizada) {
                ocorrenciaForm.setViaturas(new ArrayList<>(existente.getViaturas()));
                return ocorrenciaRepository.save(ocorrenciaForm);
            }

            for (Viatura vAntiga : existente.getViaturas()) {
                viaturasJaVinculadas.add(vAntiga.getId());
            }

            if ((existente.getStatus() == StatusOcorrencia.EM_DESLOCAMENTO || existente.getStatus() == StatusOcorrencia.NO_LOCAL)
                    && (viaturaIds == null || viaturaIds.isEmpty())) {
                throw new IllegalStateException("Não é possível remover todas as viaturas de uma ocorrência em deslocamento ou no local.");
            }

            if (existente.isEmAberto()) {
                for (Viatura vAntiga : existente.getViaturas()) {
                    if (viaturaIds == null || !viaturaIds.contains(vAntiga.getId())) {
                        Viatura viaturaTravada = viaturaRepository.findByIdForUpdate(vAntiga.getId())
                                .orElseThrow(() -> new IllegalStateException("Viatura #" + vAntiga.getId() + " não encontrada."));
                        viaturaTravada.setStatus(StatusViatura.DISPONIVEL);
                        viaturaRepository.save(viaturaTravada);
                    }
                }
            }
        } else {
            LocalDateTime originalAbertura = ocorrenciaForm.getHorarioAcionamentoOriginal() != null
                    ? ocorrenciaForm.getHorarioAcionamentoOriginal().truncatedTo(ChronoUnit.MINUTES)
                    : agoraMaquina;
            ocorrenciaForm.setHorarioAcionamentoOriginal(originalAbertura);

            if (ocorrenciaForm.getHorarioAcionamento() == null || !usuarioEhAtendenteOuAdmin()) {
                ocorrenciaForm.setHorarioAcionamento(originalAbertura);
                ocorrenciaForm.setAcionamentoEditado(false);
            } else {
                LocalDateTime informadoTrunc = ocorrenciaForm.getHorarioAcionamento().truncatedTo(ChronoUnit.MINUTES);
                if (!informadoTrunc.equals(originalAbertura)) {
                    ocorrenciaForm.setHorarioAcionamento(informadoTrunc);
                    ocorrenciaForm.setAcionamentoEditado(true);
                    registrarLogAuditoriaInterno(ocorrenciaForm, originalAbertura, originalAbertura, informadoTrunc);
                } else {
                    ocorrenciaForm.setHorarioAcionamento(originalAbertura);
                    ocorrenciaForm.setAcionamentoEditado(false);
                }
            }

            ocorrenciaForm.registrarMudancaStatus(StatusOcorrencia.REGISTRADA);
        }

        List<Viatura> novasViaturas = new ArrayList<>();
        if (viaturaIds != null && !viaturaIds.isEmpty()) {
            Set<Long> idsUnicos = new HashSet<>(viaturaIds);
            for (Long vId : idsUnicos) {
                if (vId == null) {
                    continue;
                }
                Viatura v = viaturaRepository.findByIdForUpdate(vId)
                        .orElseThrow(() -> new IllegalStateException("Viatura #" + vId + " não encontrada. Empenho cancelado."));

                boolean jaEraDestaOcorrencia = viaturasJaVinculadas.contains(v.getId());

                if (ocorrenciaForm.isEmAberto()) {
                    if (!jaEraDestaOcorrencia) {
                        if (!v.isAtivo()) {
                            throw new IllegalStateException("A viatura " + v.getPrefixo() + " está inativa e não pode ser empenhada.");
                        }
                        if (v.getStatus() != StatusViatura.DISPONIVEL) {
                            throw new IllegalStateException("A viatura " + v.getPrefixo() + " já está ocupada ou indisponível.");
                        }
                    }
                    v.setStatus(StatusViatura.EM_ATENDIMENTO);
                    viaturaRepository.save(v);
                }

                novasViaturas.add(v);
            }

            ocorrenciaForm.setViaturas(novasViaturas);
            if (ocorrenciaForm.getStatus() == StatusOcorrencia.REGISTRADA) {
                ocorrenciaForm.registrarMudancaStatus(StatusOcorrencia.EMPENHADA);
            }
        } else {
            ocorrenciaForm.setViaturas(new ArrayList<>());
            if (ocorrenciaForm.getStatus() == StatusOcorrencia.EMPENHADA) {
                ocorrenciaForm.setStatus(StatusOcorrencia.REGISTRADA);
                ocorrenciaForm.setHorarioEmpenho(null);
            }
        }

        Ocorrencia salva = ocorrenciaRepository.save(ocorrenciaForm);

        if (existente == null && salva.isAcionamentoEditado()) {
            salvarEntidadeAuditoria(
                    salva.getId(),
                    salva.getHorarioAcionamentoOriginal(),
                    salva.getHorarioAcionamentoOriginal(),
                    salva.getHorarioAcionamento()
            );
        }

        return salva;
    }

    @Transactional(rollbackFor = Exception.class)
    public void despachoRapido(Long ocorrenciaId, Long viaturaId) {
        if (ocorrenciaId == null || viaturaId == null) {
            throw new IllegalArgumentException("Ocorrência e viatura devem ser informadas para o despacho.");
        }

        Ocorrencia oc = ocorrenciaRepository.findById(ocorrenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Ocorrência #" + ocorrenciaId + " não encontrada."));

        if (!oc.isEmAberto()) {
            throw new IllegalStateException("Não é possível empenhar viatura em uma ocorrência já encerrada ou cancelada.");
        }

        Viatura v = viaturaRepository.findByIdForUpdate(viaturaId)
                .orElseThrow(() -> new IllegalArgumentException("Viatura #" + viaturaId + " não encontrada."));

        if (!v.isAtivo() || v.getStatus() != StatusViatura.DISPONIVEL) {
            throw new IllegalStateException("A viatura " + v.getPrefixo() + " não está disponível para empenho.");
        }

        boolean jaVinculada = oc.getViaturas().stream().anyMatch(x -> x.getId().equals(v.getId()));
        if (jaVinculada) {
            throw new IllegalStateException("A viatura " + v.getPrefixo() + " já está empenhada nesta ocorrência.");
        }

        v.setStatus(StatusViatura.EM_ATENDIMENTO);
        viaturaRepository.save(v);

        oc.getViaturas().add(v);
        if (oc.getStatus() == StatusOcorrencia.REGISTRADA) {
            oc.registrarMudancaStatus(StatusOcorrencia.EMPENHADA);
        }
        ocorrenciaRepository.save(oc);
    }

    @Transactional(rollbackFor = Exception.class)
    public Ocorrencia avancarStatusSequencial(Long ocorrenciaId, StatusOcorrencia destino) {
        if (ocorrenciaId == null || destino == null) {
            throw new IllegalArgumentException("Ocorrência e próximo status são obrigatórios.");
        }

        Ocorrencia oc = ocorrenciaRepository.findById(ocorrenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Ocorrência #" + ocorrenciaId + " não encontrada."));

        StatusOcorrencia atual = oc.getStatus();
        if (atual == null) {
            throw new IllegalStateException("A ocorrência não possui um status inicial válido.");
        }

        if (destino == StatusOcorrencia.FINALIZADA) {
            throw new IllegalStateException("Para finalizar a ocorrência, preencha o Boletim de Ocorrência.");
        }

        if (!atual.podeAvancarSequencialmentePara(destino)) {
            StatusOcorrencia esperado = atual.getProximoSequencial();
            String detalhe = esperado != null
                    ? "O próximo passo obrigatório é: " + esperado.getRotulo() + "."
                    : "Esta ocorrência não permite mais avanço sequencial a partir de " + atual.getRotulo() + ".";
            throw new IllegalStateException("Transição fora da sequência obrigatória (" + atual.getRotulo() + " -> " + destino.getRotulo() + "). " + detalhe);
        }

        if (oc.getViaturas() == null || oc.getViaturas().isEmpty()) {
            throw new IllegalStateException("É obrigatório haver ao menos uma viatura empenhada antes de iniciar o deslocamento.");
        }

        oc.registrarMudancaStatus(destino);
        return ocorrenciaRepository.save(oc);
    }

    @Transactional(rollbackFor = Exception.class)
    public Ocorrencia finalizarOcorrencia(Long ocorrenciaId, String relatorio) {
        if (ocorrenciaId == null) {
            throw new IllegalArgumentException("Identificador da ocorrência inválido.");
        }
        if (relatorio == null || relatorio.isBlank()) {
            throw new IllegalArgumentException("O preenchimento do relatório do boletim é obrigatório para encerrar a ocorrência.");
        }

        Ocorrencia oc = ocorrenciaRepository.findById(ocorrenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Ocorrência #" + ocorrenciaId + " não encontrada."));

        if (oc.getStatus() != StatusOcorrencia.NO_LOCAL) {
            throw new IllegalStateException(
                    "Fluxo obrigatório não cumprido: a ocorrência precisa passar por Em Deslocamento e No Local antes de ser finalizada (status atual: "
                            + (oc.getStatus() != null ? oc.getStatus().getRotulo() : "Indefinido") + ").");
        }

        oc.setRelatorio(relatorio.trim());
        oc.setHorarioEncerramento(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        oc.registrarMudancaStatus(StatusOcorrencia.FINALIZADA);

        for (Viatura v : oc.getViaturas()) {
            Viatura viaturaTravada = viaturaRepository.findByIdForUpdate(v.getId())
                    .orElseThrow(() -> new IllegalStateException("Viatura #" + v.getId() + " não encontrada durante a liberação."));
            viaturaTravada.setStatus(StatusViatura.DISPONIVEL);
            viaturaRepository.save(viaturaTravada);
        }

        return ocorrenciaRepository.save(oc);
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