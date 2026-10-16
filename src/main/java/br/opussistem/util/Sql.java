package br.opussistem.util;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Conversões null-safe entre JDBC e java.time. */
public final class Sql {

    private Sql() {
    }

    public static LocalDateTime timestamp(ResultSet rs, String coluna) throws SQLException {
        Timestamp t = rs.getTimestamp(coluna);
        return t == null ? null : t.toLocalDateTime();
    }

    public static LocalDate data(ResultSet rs, String coluna) throws SQLException {
        java.sql.Date d = rs.getDate(coluna);
        return d == null ? null : d.toLocalDate();
    }

    public static void setTimestamp(PreparedStatement ps, int i, LocalDateTime v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(i, Timestamp.valueOf(v));
        }
    }

    public static void setData(PreparedStatement ps, int i, LocalDate v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.DATE);
        } else {
            ps.setDate(i, java.sql.Date.valueOf(v));
        }
    }

    /** Texto vazio vira NULL. */
    public static void setTexto(PreparedStatement ps, int i, String v) throws SQLException {
        if (v == null || v.isBlank()) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            ps.setString(i, v);
        }
    }

    public static void setDecimal(PreparedStatement ps, int i, BigDecimal v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.DECIMAL);
        } else {
            ps.setBigDecimal(i, v);
        }
    }
}
