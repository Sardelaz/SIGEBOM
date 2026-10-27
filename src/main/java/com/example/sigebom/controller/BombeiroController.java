package com.example.sigebom.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.sigebom.model.Bombeiro;
import com.example.sigebom.repository.BombeiroRepository;

@Controller
@RequestMapping("/bombeiros")
@PreAuthorize("hasAuthority('ADMINISTRADOR')")
public class BombeiroController {

    private final BombeiroRepository repository;
    private final PasswordEncoder passwordEncoder;

    public BombeiroController(BombeiroRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("bombeiros", repository.findAll());
        return "bombeiros";
    }

    @GetMapping("/novo")
    public String novoForm(Model model) {
        model.addAttribute("bombeiro", new Bombeiro());
        return "cadastro-bombeiro";
    }

    @GetMapping("/{id}")
    public String editarForm(@PathVariable("id") Long id, Model model) {
        if (id == null) {
            return "redirect:/bombeiros";
        }
        Bombeiro bombeiro = repository.findById(id).orElseThrow();
        model.addAttribute("bombeiro", bombeiro);
        return "cadastro-bombeiro";
    }

    @PostMapping
    public String salvar(@ModelAttribute Bombeiro bombeiro) {
        if (bombeiro == null) {
            return "redirect:/bombeiros";
        }

        Long id = bombeiro.getId();
        if (id != null) {
            Bombeiro existente = repository.findById(id).orElseThrow();
            if (bombeiro.getSenha() == null || bombeiro.getSenha().isBlank()) {
                bombeiro.setSenha(existente.getSenha());
            } else {
                bombeiro.setSenha(passwordEncoder.encode(bombeiro.getSenha()));
            }
            bombeiro.setViatura(existente.getViatura());
            bombeiro.setAtivo(existente.isAtivo());
        } else {
            if (bombeiro.getSenha() != null) {
                bombeiro.setSenha(passwordEncoder.encode(bombeiro.getSenha()));
            }
        }

        repository.save(bombeiro);
        return "redirect:/bombeiros";
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable("id") Long id) {
        if (id != null) {
            Bombeiro bombeiro = repository.findById(id).orElseThrow();
            bombeiro.setAtivo(false);
            repository.save(bombeiro);
        }
        return "redirect:/bombeiros";
    }

    @PostMapping("/{id}/reativar")
    public String reativar(@PathVariable("id") Long id) {
        if (id != null) {
            Bombeiro bombeiro = repository.findById(id).orElseThrow();
            bombeiro.setAtivo(true);
            repository.save(bombeiro);
        }
        return "redirect:/bombeiros";
    }
}