package assinatura.entidade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Padroniza textos usados como chave (categorias e preferências). */
final class Normalizacao {

    private Normalizacao() { }

    static String texto(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }

    static List<String> lista(Collection<String> valores) {
        List<String> resultado = new ArrayList<>();
        if (valores == null) {
            return resultado;
        }
        for (String valor : valores) {
            String normalizado = texto(valor);
            if (!normalizado.isEmpty() && !resultado.contains(normalizado)) {
                resultado.add(normalizado);
            }
        }
        return resultado;
    }
}
