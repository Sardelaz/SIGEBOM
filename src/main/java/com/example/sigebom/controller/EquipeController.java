package com.example.sigebom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sigebom.repository.BombeiroRepository;
import com.example.sigebom.repository.ViaturaRepository;
import com.example.sigebom.service.EquipeService;

@Controller
@RequestMapping("/equipes")
public class EquipeController {

    @Autowired
    private ViaturaRepository viaturaRepository;

    @Autowired
    private BombeiroRepository bombeiroRepository;

    @Autowired
    private EquipeService equipeService;

    @GetMapping
    public String tela(Model model) {
        model.addAttribute("viaturas", viaturaRepository.findAll());
        model.addAttribute("semViatura", bombeiroRepository.findByViaturaIsNullOrderByNome());
        model.addAttribute("todosBombeiros", bombeiroRepository.findAll());
        return "equipes";
    }

    @PostMapping("/acoplar")
    public String acoplar(@RequestParam("bombeiroId") Long bombeiroId,
                          @RequestParam("viaturaId") Long viaturaId,
                          RedirectAttributes ra) {
        executar(ra, () -> equipeService.acoplar(bombeiroId, viaturaId), "Bombeiro acoplado.");
        return "redirect:/equipes";
    }

    @PostMapping("/desacoplar")
    public String desacoplar(@RequestParam("bombeiroId") Long bombeiroId,
                             RedirectAttributes ra) {
        executar(ra, () -> equipeService.desacoplar(bombeiroId), "Bombeiro removido da viatura.");
        return "redirect:/equipes";
    }

    @PostMapping("/trocar")
    public String trocar(@RequestParam("bombeiroA") Long bombeiroA,
                         @RequestParam("bombeiroB") Long bombeiroB,
                         RedirectAttributes ra) {
        executar(ra, () -> equipeService.trocar(bombeiroA, bombeiroB), "Bombeiros trocados.");
        return "redirect:/equipes";
    }

    private void executar(RedirectAttributes ra, Runnable acao, String sucesso) {
        try {
            acao.run();
            ra.addFlashAttribute("sucesso", sucesso);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
    }
}