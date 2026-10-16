package br.opussistem.exception;

/** Erro de acesso ao banco de dados (encapsula SQLException). */
public class DaoException extends RuntimeException {

    public DaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
