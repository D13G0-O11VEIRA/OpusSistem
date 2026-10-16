package br.opussistem.model;

import java.time.LocalDateTime;

public record PacoteAssinatura(String codigo, String cliente, String assinante, String email,
                               int quantidade, LocalDateTime assinadoEm) {
}
