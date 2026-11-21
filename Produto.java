package assinatura.entidade;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Produto {

    private final String nome;
    private final String categoria;
    private final boolean disponibilidade;
    private final List<String> preferenciasAtendidas;

    public Produto(String nome, String categoria, boolean disponibilidade, Collection<String> preferenciasAtendidas) {
        this.nome = nome;
        this.categoria = Normalizacao.texto(categoria);
        this.disponibilidade = disponibilidade;
        this.preferenciasAtendidas = Normalizacao.lista(preferenciasAtendidas);
    }

    /** Filtro por preferência: está disponível, é da categoria e atende todas as preferências pedidas. */
    public boolean atendeSelecao(String categoria, Collection<String> preferencias) {
        return disponibilidade
                && this.categoria.equals(Normalizacao.texto(categoria))
                && preferenciasAtendidas.containsAll(Normalizacao.lista(preferencias));
    }

    public String getNome() { return nome; }
    public String getCategoria() { return categoria; }
    public boolean isDisponibilidade() { return disponibilidade; }
    public List<String> getPreferenciasAtendidas() { return Collections.unmodifiableList(preferenciasAtendidas); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Produto p && nome.equals(p.nome) && categoria.equals(p.categoria);
    }

    @Override
    public int hashCode() { return Objects.hash(nome, categoria); }

    @Override
    public String toString() { return nome; }
}
