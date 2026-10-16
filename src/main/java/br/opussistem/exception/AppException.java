package br.opussistem.exception;

/** Erro de regra de negócio ou de uso (a mensagem pode ser mostrada ao usuário). */
public class AppException extends RuntimeException {

    public AppException(String mensagem) {
        super(mensagem);
    }

    public AppException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
