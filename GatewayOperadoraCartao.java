package assinatura.externo;

import java.math.BigDecimal;

import assinatura.entidade.Cartao;

/** Fronteira com a operadora: qualquer falha vira recusa (código vazio). */
public class GatewayOperadoraCartao {

    private final OperadoraCartao operadora;

    public GatewayOperadoraCartao(OperadoraCartao operadora) {
        this.operadora = operadora;
    }

    public String autorizar(Cartao cartao, BigDecimal valor) {
        try {
            String codigo = operadora.autorizar(cartao, valor);
            return codigo == null ? "" : codigo;
        } catch (RuntimeException e) {
            return "";
        }
    }
}
