package com.stentio.core.validation;

import java.util.regex.Pattern;

public final class CpfCnpjUtils {

    private static final Pattern APENAS_DIGITOS = Pattern.compile("\\D");
    private static final int[] PESOS_CNPJ_PRIMEIRO_DIGITO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_SEGUNDO_DIGITO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CpfCnpjUtils() {
    }

    public static String somenteDigitos(String valor) {
        if (valor == null) {
            return null;
        }
        return APENAS_DIGITOS.matcher(valor).replaceAll("");
    }

    public static boolean ehValido(String valor) {
        if (valor == null) {
            return false;
        }
        String digitos = somenteDigitos(valor);
        if (digitos.length() == 11) {
            return ehCpfValido(digitos);
        } else if (digitos.length() == 14) {
            return ehCnpjValido(digitos);
        }
        return false;
    }

    public static boolean ehCpfValido(String cpf) {
        String digitos = somenteDigitos(cpf);
        if (digitos == null || digitos.length() != 11) {
            return false;
        }

        // Rejeita sequências repetidas (ex.: 000.000.000-00, 111.111.111-11, etc.)
        if (todosCaracteresIguais(digitos)) {
            return false;
        }

        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += (digitos.charAt(i) - '0') * (10 - i);
        }
        int resto = 11 - (soma % 11);
        char digito1 = (resto >= 10) ? '0' : (char) (resto + '0');
        if (digito1 != digitos.charAt(9)) {
            return false;
        }

        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += (digitos.charAt(i) - '0') * (11 - i);
        }
        resto = 11 - (soma % 11);
        char digito2 = (resto >= 10) ? '0' : (char) (resto + '0');
        return digito2 == digitos.charAt(10);
    }

    public static boolean ehCnpjValido(String cnpj) {
        String digitos = somenteDigitos(cnpj);
        if (digitos == null || digitos.length() != 14) {
            return false;
        }

        // Rejeita sequências repetidas
        if (todosCaracteresIguais(digitos)) {
            return false;
        }

        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += (digitos.charAt(i) - '0') * PESOS_CNPJ_PRIMEIRO_DIGITO[i];
        }
        int resto = soma % 11;
        char digito1 = (resto < 2) ? '0' : (char) ((11 - resto) + '0');
        if (digito1 != digitos.charAt(12)) {
            return false;
        }

        soma = 0;
        for (int i = 0; i < 13; i++) {
            soma += (digitos.charAt(i) - '0') * PESOS_CNPJ_SEGUNDO_DIGITO[i];
        }
        resto = soma % 11;
        char digito2 = (resto < 2) ? '0' : (char) ((11 - resto) + '0');
        return digito2 == digitos.charAt(13);
    }

    private static boolean todosCaracteresIguais(String s) {
        char primeiro = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != primeiro) {
                return false;
            }
        }
        return true;
    }
}
