package br.opussistem.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvTest {

    @Test
    void leCamposComAspasQuebrasDeLinhaESeparadorPontoEVirgula() {
        String texto = "\uFEFFNome;Obs\r\n\"Silva; João\";\"linha1\nlinha2\"\r\nMaria;\"diz \"\"oi\"\"\"\r\n";
        List<String[]> linhas = Csv.ler(texto);
        assertEquals(3, linhas.size());
        assertEquals("Silva; João", linhas.get(1)[0]);
        assertEquals("linha1\nlinha2", linhas.get(1)[1]);
        assertEquals("diz \"oi\"", linhas.get(2)[1]);
    }

    @Test
    void detectaVirgulaComoSeparador() {
        List<String[]> linhas = Csv.ler("a,b,c\n1,2,3\n");
        assertEquals(2, linhas.size());
        assertEquals("3", linhas.get(1)[2]);
    }

    @Test
    void escreveEscapandoCamposEspeciais() {
        String csv = Csv.escrever(List.<String[]>of(new String[]{"a;b", "c\"d", "ok"}));
        assertEquals("\uFEFF\"a;b\";\"c\"\"d\";ok\r\n", csv);
    }
}
