package com.example.sigebom.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/esqueci-senha")
public class SenhaController {

    // Carrega a página HTML quando o usuário clica no botão "Esqueci minha senha"
    @GetMapping
    public String paginaRecuperacao() {
        return "esqueci-senha";
    }

    // Processa os dados quando o usuário preenche o CPF/E-mail e aperta "Enviar"
    @PostMapping
    public String processarRecuperacao(@RequestParam("identificador") String identificador) {
        
        // Lógica simulada para cumprir a US2 sem precisar de servidor SMTP configurado
        System.out.println("=====================================================");
        System.out.println("Enviando link de recuperação de senha para: " + identificador);
        System.out.println("=====================================================");
        
        // Redireciona o usuário de volta para o login, ativando a mensagem verde de sucesso
        return "redirect:/login?recuperacaoEnviada=true";
    }
}