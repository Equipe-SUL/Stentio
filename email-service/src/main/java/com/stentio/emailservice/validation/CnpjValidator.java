package com.stentio.emailservice.validation;

import java.util.Locale;

public final class CnpjValidator {

    private CnpjValidator() {
    }

    public static boolean valido(String cnpj) {

        if (cnpj == null || cnpj.isBlank()) {
            return false;
        }

        String normalizado = normalizar(cnpj);

        if (normalizado.length() != 14) {
            return false;
        }

        // Os 12 primeiros caracteres podem ser alfanuméricos.
        // Os dois últimos continuam sendo dígitos verificadores.
        String base = normalizado.substring(0, 12);
        String dvInformado = normalizado.substring(12);

        if (!dvInformado.matches("\\d{2}")) {
            return false;
        }

        int primeiroDigito = calcularDigito(base);
        int segundoDigito = calcularDigito(base + primeiroDigito);

        String dvCalculado = String.valueOf(primeiroDigito) + segundoDigito;

        return dvCalculado.equals(dvInformado);
    }

    private static int calcularDigito(String valor) {

        int[] pesos;

        if (valor.length() == 12) {
            pesos = new int[]{
                    5, 4, 3, 2,
                    9, 8, 7, 6,
                    5, 4, 3, 2
            };
        } else {
            pesos = new int[]{
                    6, 5, 4, 3, 2,
                    9, 8, 7, 6,
                    5, 4, 3, 2
            };
        }

        int soma = 0;

        for (int i = 0; i < valor.length(); i++) {

            char caractere = valor.charAt(i);

            int valorNumerico;

            if (Character.isDigit(caractere)) {
                valorNumerico = caractere - '0';
            } else if (Character.isLetter(caractere)) {
                valorNumerico = caractere - '0';
            } else {
                return -1;
            }

            soma += valorNumerico * pesos[i];
        }

        int resto = soma % 11;

        return resto < 2
                ? 0
                : 11 - resto;
    }

    private static String normalizar(String cnpj) {

        return cnpj
                .strip()
                .replace(".", "")
                .replace("/", "")
                .replace("-", "")
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);
    }
}
