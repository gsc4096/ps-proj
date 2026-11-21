package assinatura.externo;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import assinatura.entidade.Cartao;

/**
 * Simula a operadora. Regras da simulação: número com 16 dígitos e dígito verificador (Luhn)
 * válido, CVV com 3 dígitos, validade MM/AA no futuro. Cartões terminados em 0002 sempre são
 * recusados (cartão de teste). Retorna o código de autorização, ou "" se recusado.
 */
public class OperadoraCartao {

    private static final Pattern VALIDADE = Pattern.compile("^(0[1-9]|1[0-2])/(\\d{2})$");

    public String autorizar(Cartao cartao, BigDecimal valor) {
        if (cartao == null || valor == null || valor.signum() <= 0) {
            return "";
        }
        String numero = cartao.getNumero();
        if (numero.length() != 16 || !luhnValido(numero) || numero.endsWith("0002")) {
            return "";
        }
        if (!cartao.getCvv().matches("\\d{3}") || !validadeOk(cartao.getValidade())) {
            return "";
        }
        return "AUT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static boolean validadeOk(String validade) {
        Matcher m = VALIDADE.matcher(validade);
        if (!m.matches()) {
            return false;
        }
        YearMonth vencimento = YearMonth.of(2000 + Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
        return !vencimento.isBefore(YearMonth.now());
    }

    private static boolean luhnValido(String numero) {
        int soma = 0;
        boolean dobrar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int d = numero.charAt(i) - '0';
            if (dobrar) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            soma += d;
            dobrar = !dobrar;
        }
        return soma % 10 == 0;
    }
}
