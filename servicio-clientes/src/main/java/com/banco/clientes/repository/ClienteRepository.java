package com.banco.clientes.repository;

import com.banco.clientes.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByRut(String rut);
    boolean existsByRut(String rut);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}
