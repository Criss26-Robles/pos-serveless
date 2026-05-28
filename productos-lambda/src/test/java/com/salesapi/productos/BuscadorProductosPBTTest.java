package com.salesapi.productos;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas basadas en propiedades para BuscadorProductos.
 * Valida: Requerimientos 1.4, 1.1, 2.1
 */
class BuscadorProductosPBTTest {

    private final ProductosDynamoRepo repositorio = Mockito.mock(ProductosDynamoRepo.class);
    private final BuscadorProductos buscador = new BuscadorProductos(repositorio);

    // Property 1: Clasificación correcta del parámetro q — Reqs 1.4, 1.1, 2.1
    @Property(tries = 100)
    void clasificacionParametroQ(@ForAll @StringLength(min = 1, max = 50) String q) {
        boolean resultado = buscador.esNumerico(q);
        boolean esperado = q.chars().allMatch(Character::isDigit);
        assertEquals(esperado, resultado,
                "esNumerico(\"" + q + "\") debería ser " + esperado);
    }
}
