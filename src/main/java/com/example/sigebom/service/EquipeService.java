package com.example.sigebom.service;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.sigebom.model.Bombeiro;
import com.example.sigebom.model.Viatura;
import com.example.sigebom.repository.BombeiroRepository;
import com.example.sigebom.repository.ViaturaRepository;

import jakarta.transaction.Transactional;

@Service
public class EquipeService {

    @Autowired
    private BombeiroRepository bombeiroRepository;

    @Autowired
    private ViaturaRepository viaturaRepository;

    @Transactional
    public void acoplar(Long bombeiroId, Long viaturaId) {
        if (bombeiroId == null || viaturaId == null) {
            throw new IllegalArgumentException("Bombeiro e viatura devem ser informados.");
        }
        Bombeiro bombeiro = bombeiroRepository.findById(bombeiroId)
                .orElseThrow(() -> new IllegalArgumentException("Bombeiro não encontrado."));
        Viatura destino = viaturaRepository.findById(viaturaId)
                .orElseThrow(() -> new IllegalArgumentException("Viatura não encontrada."));

        if (bombeiro.getViatura() != null && Objects.equals(bombeiro.getViatura().getId(), viaturaId)) {
            throw new IllegalArgumentException(bombeiro.getNome() + " já está na viatura " + destino.getPrefixo() + ".");
        }

        long ocupadas = bombeiroRepository.countByViaturaId(viaturaId);
        if (ocupadas >= destino.getCapacidade()) {
            throw new IllegalArgumentException("A viatura " + destino.getPrefixo() + " está cheia ("
                    + destino.getCapacidade() + " vagas). Libere uma vaga ou use a troca entre bombeiros.");
        }

        bombeiro.setViatura(destino);
        bombeiroRepository.save(bombeiro);
    }

    @Transactional
    public void desacoplar(Long bombeiroId) {
        if (bombeiroId == null) {
            throw new IllegalArgumentException("Bombeiro inválido.");
        }
        Bombeiro bombeiro = bombeiroRepository.findById(bombeiroId)
                .orElseThrow(() -> new IllegalArgumentException("Bombeiro não encontrado."));
        bombeiro.setViatura(null);
        bombeiroRepository.save(bombeiro);
    }

    @Transactional
    public void trocar(Long bombeiroAId, Long bombeiroBId) {
        if (bombeiroAId == null || bombeiroBId == null || Objects.equals(bombeiroAId, bombeiroBId)) {
            throw new IllegalArgumentException("Escolha dois bombeiros diferentes.");
        }

        Bombeiro a = bombeiroRepository.findById(bombeiroAId)
                .orElseThrow(() -> new IllegalArgumentException("Bombeiro não encontrado."));
        Bombeiro b = bombeiroRepository.findById(bombeiroBId)
                .orElseThrow(() -> new IllegalArgumentException("Bombeiro não encontrado."));

        Viatura viaturaA = a.getViatura();
        a.setViatura(b.getViatura());
        b.setViatura(viaturaA);

        bombeiroRepository.save(a);
        bombeiroRepository.save(b);
    }
}