package io.github.joicepassos.billing.shared.taxid;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A Brazilian tax id: CPF (11 digits) or CNPJ (14 characters, numeric or the alphanumeric format in use since
 * July 2026). Stored normalized (no punctuation, upper case). Personal data: {@link #toString()} is masked.
 */
public record TaxId(Type type, String value) {

    public enum Type { CPF, CNPJ }

    private static final Pattern CPF = Pattern.compile("\\d{11}");
    private static final Pattern CNPJ = Pattern.compile("[0-9A-Z]{12}\\d{2}");
    private static final int[] CNPJ_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public TaxId {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
    }

    /** Parses a CPF or CNPJ with or without punctuation; empty when the format or check digits are wrong. */
    public static Optional<TaxId> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.replaceAll("[.\\-/\\s]", "").toUpperCase(java.util.Locale.ROOT);
        if (CPF.matcher(normalized).matches() && validCpf(normalized)) {
            return Optional.of(new TaxId(Type.CPF, normalized));
        }
        if (CNPJ.matcher(normalized).matches() && validCnpj(normalized)) {
            return Optional.of(new TaxId(Type.CNPJ, normalized));
        }
        return Optional.empty();
    }

    public static boolean isValid(String raw) {
        return parse(raw).isPresent();
    }

    /** Keeps only enough to recognize the document: {@code ***.982.247-**} or {@code **.ABC.345/****-**}. */
    public String masked() {
        return switch (type) {
            case CPF -> "***." + value.substring(3, 6) + "." + value.substring(6, 9) + "-**";
            case CNPJ -> "**." + value.substring(2, 5) + "." + value.substring(5, 8) + "/****-**";
        };
    }

    @Override
    public String toString() {
        return type + " " + masked();
    }

    private static boolean validCpf(String digits) {
        if (allSame(digits)) {
            return false;
        }
        return checkDigit(digits, 9, 10) == digits.charAt(9) - '0'
                && checkDigit(digits, 10, 11) == digits.charAt(10) - '0';
    }

    private static int checkDigit(String digits, int length, int firstWeight) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (firstWeight - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static boolean validCnpj(String chars) {
        if (allSame(chars)) {
            return false;
        }
        return cnpjDigit(chars, 12) == chars.charAt(12) - '0' && cnpjDigit(chars, 13) == chars.charAt(13) - '0';
    }

    /** Each character counts as its ASCII code minus 48, so digits keep their value and A = 17 (Receita rule). */
    private static int cnpjDigit(String chars, int length) {
        int offset = CNPJ_WEIGHTS.length - length;
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (chars.charAt(i) - '0') * CNPJ_WEIGHTS[offset + i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static boolean allSame(String s) {
        return s.chars().allMatch(c -> c == s.charAt(0));
    }
}
