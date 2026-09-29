package com.example.sigebom.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.sigebom.model.Bombeiro;
import com.example.sigebom.model.enums.PerfilAcesso;
import com.example.sigebom.repository.BombeiroRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final BombeiroRepository repository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(BombeiroRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String emailAdmin = "admin@sigebom.com.br";
        
        // Puxa o admin existente ou cria um novo se não existir
        Bombeiro admin = repository.findByEmail(emailAdmin).orElse(new Bombeiro());
        
        admin.setNome("Administrador");
        admin.setSobrenome("Sistema");
        admin.setCpf("000.000.000-00");
        admin.setMatriculaRe("000000");
        admin.setPostoGraduacao("Coronel");
        admin.setTelefone("(00) 00000-0000");
        admin.setEmail(emailAdmin);
        
        // Regrava a senha com a criptografia correta garantida
        admin.setSenha(passwordEncoder.encode("admin123")); 
        
        admin.setPerfil(PerfilAcesso.ADMINISTRADOR);
        admin.setAtivo(true);

        repository.save(admin);
    }
}