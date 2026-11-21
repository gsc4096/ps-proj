package assinatura.colecao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import assinatura.entidade.Plano;

public class ColecaoPlanos {

    private final List<Plano> planos;

    public ColecaoPlanos(List<Plano> planos) {
        this.planos = new ArrayList<>(planos);
    }

    /** Lista só os planos ativos, que são os que podem ser contratados. */
    public List<Plano> listar() {
        return Collections.unmodifiableList(planos.stream().filter(Plano::isAtivo).toList());
    }
}
