package main.util;

import org.camunda.bpm.engine.delegate.DelegateExecution;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class CamundaVars {

    private CamundaVars() {
    }

    public static String getString(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        return value == null ? null : value.toString();
    }

    public static Long getLong(DelegateExecution execution, String name) {
        return toLong(execution.getVariable(name));
    }

    public static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static LocalDate getLocalDate(DelegateExecution execution, String name) {
        return toLocalDate(execution.getVariable(name));
    }

    public static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        try {
            return LocalDate.parse(value.toString().trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}