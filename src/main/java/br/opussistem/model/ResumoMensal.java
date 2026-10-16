package br.opussistem.model;

import java.time.YearMonth;

public record ResumoMensal(YearMonth mes, int abertas, int fechadas) {
}
