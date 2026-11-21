package assinatura.entidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import assinatura.persistencia.RepositorioCsv;

public class Assinatura {

    public static final String CRIADA = "CRIADA";
    public static final String AGUARDANDO_PAGAMENTO = "AGUARDANDO_PAGAMENTO";
    public static final String ATIVA = "ATIVA";
    public static final String CANCELADA = "CANCELADA";

    private String protocolo;
    private BigDecimal valor;
    private String status;
    private Assinante assinante;
    private Plano plano;
    private Endereco endereco;
    private final List<Pedido> pedidos = new ArrayList<>();
    private final List<Pagamento> pagamentos = new ArrayList<>();

    public void criar(Assinante assinante, Plano plano) {
        this.assinante = assinante;
        this.plano = plano;
        this.status = CRIADA;
        this.protocolo = "ASS-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        this.valor = calcularValorTotal();
        assinante.adicionarAssinatura(this);
    }

    /** A assinatura cria o pedido do plano contratado. */
    public Pedido gerarPedido() {
        Pedido pedido = new Pedido();
        pedido.criar(this);
        return pedido;
    }

    /** Cria o endereço de entrega; devolve false (sem alterar nada) se os dados forem inválidos. */
    public boolean aguardarPagamento(String logradouro, String numero, String complemento,
                                     String cep, String cidade, String uf) {
        if (ATIVA.equals(status) || CANCELADA.equals(status)) {
            throw new IllegalStateException("Assinatura " + status + " não aceita novo pagamento.");
        }
        Endereco novo = new Endereco(logradouro, numero, complemento, cep, cidade, uf);
        if (!novo.isValido()) {
            return false;
        }
        this.endereco = novo;
        this.valor = calcularValorTotal();
        this.status = AGUARDANDO_PAGAMENTO;
        return true;
    }

    public BigDecimal calcularValorTotal() {
        return plano.getValor();
    }

    public void salvar() {
        RepositorioCsv.padrao().salvar(this);
    }

    /** Cria o pagamento aprovado, ativa a assinatura, aprova o pedido e devolve o protocolo. */
    public String confirmar(String codigoAutorizacao, Pedido pedido, LocalDateTime instante) {
        if (!AGUARDANDO_PAGAMENTO.equals(status)) {
            throw new IllegalStateException("Só é possível confirmar uma assinatura aguardando pagamento.");
        }
        Pagamento pagamento = new Pagamento();
        pagamento.criar(valor, codigoAutorizacao, instante);
        pagamentos.add(pagamento);
        status = ATIVA;
        pedido.aprovar(plano.preverPrimeiraEntrega(instante));
        salvar();
        return protocolo;
    }

    /** Encerra sem ativar (desistência). Uma assinatura ativa não é cancelada por aqui. */
    public void cancelar() {
        if (ATIVA.equals(status)) {
            throw new IllegalStateException("Assinatura ativa não pode ser cancelada neste fluxo.");
        }
        status = CANCELADA;
        pedidos.forEach(Pedido::cancelar);
    }

    void adicionarPedido(Pedido pedido) {
        pedidos.add(pedido);
    }

    public String getProtocolo() { return protocolo; }
    public BigDecimal getValor() { return valor; }
    public String getStatus() { return status; }
    public Assinante getAssinante() { return assinante; }
    public Plano getPlano() { return plano; }
    public Endereco getEndereco() { return endereco; }
    public List<Pedido> getPedidos() { return Collections.unmodifiableList(pedidos); }
    public List<Pagamento> getPagamentos() { return Collections.unmodifiableList(pagamentos); }
}
