package assinatura.colecao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import assinatura.entidade.Produto;

public class ColecaoProdutos {

    private final List<Produto> produtos;

    public ColecaoProdutos(List<Produto> produtos) {
        this.produtos = new ArrayList<>(produtos);
    }

    public List<Produto> listar() {
        return Collections.unmodifiableList(produtos);
    }
}
