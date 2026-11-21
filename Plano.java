package assinatura.entidade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Plano {

    /** Dias de antecedência necessários para preparar a primeira entrega. */
    public static final int DIAS_PREPARO = 2;

    private final String nome;
    private final int qtdRefeicoes;
    private final BigDecimal valor;
    private final String periodicidade;
    private final boolean ativo;

    public Plano(String nome, int qtdRefeicoes, BigDecimal valor, String periodicidade, boolean ativo) {
        this.nome = nome;
        this.qtdRefeicoes = qtdRefeicoes;
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
        this.periodicidade = periodicidade;
        this.ativo = ativo;
    }

    /** Limite do plano: a quantidade deve ser positiva e não pode passar do total de refeições. */
    public boolean permiteQuantidade(int quantidade) {
        return quantidade > 0 && quantidade <= qtdRefeicoes;
    }

    /** Data do pedido + {@link #DIAS_PREPARO} dias; se cair no domingo, passa para segunda. */
    public LocalDate preverPrimeiraEntrega(LocalDateTime instante) {
        LocalDate data = instante.toLocalDate().plusDays(DIAS_PREPARO);
        return data.getDayOfWeek() == DayOfWeek.SUNDAY ? data.plusDays(1) : data;
    }

    public String getNome() { return nome; }
    public int getQtdRefeicoes() { return qtdRefeicoes; }
    public BigDecimal getValor() { return valor; }
    public String getPeriodicidade() { return periodicidade; }
    public boolean isAtivo() { return ativo; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Plano p && nome.equals(p.nome);
    }

    @Override
    public int hashCode() { return Objects.hash(nome); }

    @Override
    public String toString() { return nome; }
}
