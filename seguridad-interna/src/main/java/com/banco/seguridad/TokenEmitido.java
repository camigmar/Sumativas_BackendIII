package com.banco.seguridad;

import java.time.Duration;

// Access token emitido por auth-server y su vigencia (expires_in).
public record TokenEmitido(String valor, Duration vigencia) {
}
