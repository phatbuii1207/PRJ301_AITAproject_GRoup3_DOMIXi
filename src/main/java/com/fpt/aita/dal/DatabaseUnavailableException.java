package com.fpt.aita.dal;

import java.sql.SQLException;

/** Keeps infrastructure failures distinct from invalid login credentials. */
public final class DatabaseUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public DatabaseUnavailableException(SQLException cause) {
        super("Database temporarily unavailable", cause);
    }
}
