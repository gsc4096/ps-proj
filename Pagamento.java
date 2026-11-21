package assinatura.entidade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class Pagamento {

    private BigDecimal valor;
    private LocalDateTime data;
    private String codigoAutorizacao;

    public void criar(BigDecimal valor, String codigo, LocalDateTime instante) {
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
        this.codigoAutorizacao = codigo;
        this.data = instante;
    }

    public BigDecimal getValor() { return valor; }
    public LocalDateTime getData() { return data; }
    public String getCodigoAutorizacao() { return codigoAutorizacao; }
}
