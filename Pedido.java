package assinatura.entidade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Pedido {

    public static final String EM_MONTAGEM = "EM_MONTAGEM";
    public static final String APROVADO = "APROVADO";
    public static final String CANCELADO = "CANCELADO";

    private String id;
    private String status;
    private Assinatura assinatura;
    private final List<Item> itens = new ArrayList<>();
    private final List<Entrega> entregas = new ArrayList<>();

    public void criar(Assinatura assinatura) {
        this.assinatura = Objects.requireNonNull(assinatura);
        this.status = EM_MONTAGEM;
        this.id = assinatura.getProtocolo() + "-P" + (assinatura.getPedidos().size() + 1);
        assinatura.adicionarPedido(this);
    }

    /**
     * Valida a escolha de uma categoria: cada produto precisa estar disponível e atender as
     * preferências do assinante, e a soma das quantidades respeita o limite do plano.
     * Escolha vazia significa "pular a categoria" e é válida.
     */
    public boolean validarSelecao(String categoria, Map<Produto, Integer> escolhas, Plano plano) {
        if (escolhas == null || escolhas.isEmpty()) {
            return true;
        }
        List<String> preferencias = assinatura.getAssinante().getPreferencias();
        int total = 0;
        for (Map.Entry<Produto, Integer> escolha : escolhas.entrySet()) {
            Produto produto = escolha.getKey();
            Integer qtde = escolha.getValue();
            if (produto == null || qtde == null || qtde <= 0) {
                return false;
            }
            if (!produto.atendeSelecao(categoria, preferencias)) {
                return false;
            }
            total += qtde;
        }
        return plano.permiteQuantidade(total);
    }

    /** Troca todos os itens da categoria pelos novos (quantidades zero são ignoradas). */
    public void substituirItens(String categoria, Map<Produto, Integer> escolhas) {
        String cat = categoria == null ? "" : categoria.trim().toUpperCase();
        itens.removeIf(item -> item.getProduto().getCategoria().equals(cat));
        if (escolhas == null) {
            return;
        }
        escolhas.forEach((produto, qtde) -> {
            if (qtde != null && qtde > 0) {
                itens.add(new Item(produto, qtde));
            }
        });
    }

    /** Aprova o pedido e programa a primeira entrega (o pedido cria a Entrega). */
    public void aprovar(LocalDate previsao) {
        this.status = APROVADO;
        Entrega primeira = new Entrega();
        primeira.criar(previsao);
        entregas.add(primeira);
    }

    void cancelar() {
        this.status = CANCELADO;
    }

    public String getId() { return id; }
    public String getStatus() { return status; }
    public LocalDate getPrevisaoPrimeiraEntrega() { return entregas.isEmpty() ? null : entregas.get(0).getDataPrevista(); }
    public List<Entrega> getEntregas() { return Collections.unmodifiableList(entregas); }
    public Assinatura getAssinatura() { return assinatura; }
    public List<Item> getItens() { return Collections.unmodifiableList(itens); }
}
