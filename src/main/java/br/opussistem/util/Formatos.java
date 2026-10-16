package br.opussistem.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Formatação e leitura de datas e valores no padrão brasileiro. */
public final class Formatos {

    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final Pattern DATA_BR = Pattern.compile("^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{2,4})");
    private static final Pattern DATA_ISO = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})");
    private static final Pattern DATA_COMPACTA = Pattern.compile("^(\\d{4})(\\d{2})(\\d{2})$");

    private Formatos() {
    }

    public static String data(LocalDate d) {
        return d == null ? "" : DATA.format(d);
    }

    public static String data(LocalDateTime d) {
        return d == null ? "" : DATA.format(d);
    }

    public static String dataHora(LocalDateTime d) {
        return d == null ? "" : DATA_HORA.format(d);
    }

    public static String moeda(BigDecimal v) {
        return v == null ? "" : NumberFormat.getCurrencyInstance(PT_BR).format(v);
    }

    /** Número com vírgula decimal, sem símbolo (para CSV). */
    public static String decimal(BigDecimal v) {
        return v == null ? "" : v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** Lê "1.234,56", "1234,56" ou "1234.56". Retorna null se vazio. */
    public static BigDecimal lerDecimal(String texto) {
        if (texto == null) {
            return null;
        }
        String s = texto.replaceAll("[^\\d,.-]", "");
        if (s.isEmpty() || s.equals("-")) {
            return null;
        }
        if (s.contains(",")) {
            s = s.replace(".", "").replace(',', '.');
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Lê datas em dd/MM/yyyy, dd-MM-yy, yyyy-MM-dd (com hora opcional) ou yyyyMMdd. Retorna null se inválida. */
    public static LocalDate lerData(String texto) {
        if (texto == null) {
            return null;
        }
        String s = texto.trim();
        try {
            Matcher m = DATA_ISO.matcher(s);
            if (m.find()) {
                return LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            }
            m = DATA_BR.matcher(s);
            if (m.find()) {
                int ano = Integer.parseInt(m.group(3));
                if (m.group(3).length() == 2) {
                    ano += 2000;
                }
                return LocalDate.of(ano, Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
            }
            m = DATA_COMPACTA.matcher(s);
            if (m.find()) {
                return LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            }
        } catch (java.time.DateTimeException | NumberFormatException e) {
            return null;
        }
        return null;
    }

    /** "hoje", "há 1 dia", "há 5 dias" (como na lista de OS da versão web). */
    public static String ha(LocalDateTime d) {
        if (d == null) {
            return "";
        }
        long dias = ChronoUnit.DAYS.between(d.toLocalDate(), LocalDate.now());
        if (dias <= 0) {
            return "hoje";
        }
        return dias == 1 ? "há 1 dia" : "há " + dias + " dias";
    }

    public static String texto(String s) {
        return s == null ? "" : s;
    }
}
