package assinatura.entidade;

public class Item {

    private final Produto produto;
    private final int qtde;

    public Item(Produto produto, int qtde) {
        this.produto = produto;
        this.qtde = qtde;
    }

    public Produto getProduto() { return produto; }
    public int getQtde() { return qtde; }
}
