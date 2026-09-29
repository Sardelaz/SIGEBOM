package com.example.sigebom.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.sigebom.repository.BombeiroRepository;

@Service
public class AutenticacaoService implements UserDetailsService {

    private final BombeiroRepository repository;

    public AutenticacaoService(BombeiroRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByIdentificador(username)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais não encontradas para: " + username));
    }
}