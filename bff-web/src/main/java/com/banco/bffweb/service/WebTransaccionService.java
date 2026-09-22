package com.banco.bffweb.service;

import com.banco.bffweb.client.CoreClient;
import com.banco.bffweb.dto.TransaccionDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WebTransaccionService {

    private final CoreClient coreClient;

    public WebTransaccionService(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    public List<TransaccionDTO> listar() {
        return coreClient.listarTransacciones();
    }

    public Optional<TransaccionDTO> obtener(Long id) {
        return coreClient.obtenerTransaccion(id);
    }
}
