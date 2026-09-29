package com.example.sigebom.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.sigebom.model.Viatura;
import com.example.sigebom.repository.ViaturaRepository;

@Controller
@RequestMapping("/viaturas")
@PreAuthorize("hasAuthority('ADMINISTRADOR')")
public class ViaturaController {

    private final ViaturaRepository repository;

    public ViaturaController(ViaturaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("viaturas", repository.findAll());
        return "viaturas";
    }

    @GetMapping("/nova")
    public String novaForm(Model model) {
        // Envia uma viatura vazia para o formulário no cadastro novo
        model.addAttribute("viatura", new Viatura());
        return "cadastro-viatura";
    }

    @GetMapping("/{id}")
    public String editarForm(@PathVariable("id") Long id, Model model) {
        // Envia a viatura preenchida para o formulário na edição
        Viatura viatura = repository.findById(id).orElseThrow();
        model.addAttribute("viatura", viatura);
        return "cadastro-viatura";
    }

    @PostMapping
    public String salvar(@ModelAttribute Viatura viatura) {
        if (viatura != null) {
            repository.save(viatura);
        }
        return "redirect:/viaturas";
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable("id") Long id) {
        if (id != null) {
            Viatura viatura = repository.findById(id).orElseThrow();
            viatura.setAtivo(false);
            repository.save(viatura);
        }
        return "redirect:/viaturas";
    }

    @PostMapping("/{id}/reativar")
    public String reativar(@PathVariable("id") Long id) {
        if (id != null) {
            Viatura viatura = repository.findById(id).orElseThrow();
            viatura.setAtivo(true);
            repository.save(viatura);
        }
        return "redirect:/viaturas";
    }
}