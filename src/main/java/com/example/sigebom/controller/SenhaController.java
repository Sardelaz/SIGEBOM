package com.example.sigebom.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/esqueci-senha")
public class SenhaController {

    @GetMapping
    public String paginaRecuperacao() {
        return "esqueci-senha";
    }

    @PostMapping
    public String processarRecuperacao(@RequestParam("identificador") String identificador) {
        System.out.println("=====================================================");
        System.out.println("Enviando link de recuperação de senha para: " + identificador);
        System.out.println("=====================================================");

        return "redirect:/login?recuperacaoEnviada=true";
    }
}