package com.salesapi.productos;

import com.salesapi.productos.dto.ProductoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para BuscadorProductos.
 * Valida: Requerimientos 1.1, 1.2, 1.4, 2.1, 2.2
 */
@ExtendWith(MockitoExtension.class)
class BuscadorProductosTest {

    @Mock
    private ProductosDynamoRepo repositorio;

    private BuscadorProductos buscador;

    @BeforeEach
    void setUp() {
        buscador = new BuscadorProductos(repositorio);
    }

    // ─── esNumerico ───────────────────────────────────────────────────────────

    @Test
    void esNumerico_conCodigoBarrasNumerico_retornaTrue() {
        // Req 1.4: todos los caracteres son dígitos
        assertTrue(buscador.esNumerico("7501234567890"));
    }

    @Test
    void esNumerico_conTextoAlfa_retornaFalse() {
        // Req 1.4: contiene caracteres no numéricos
        assertFalse(buscador.esNumerico("leche"));
    }

    @Test
    void esNumerico_conMezclaNumeroLetra_retornaFalse() {
        // Req 1.4: mezcla de dígitos y letras
        assertFalse(buscador.esNumerico("123abc"));
    }

    // ─── buscar — enrutamiento ────────────────────────────────────────────────

    @Test
    void buscar_conQNumerico_llamaBuscarPorCodigoBarrasYNoBuscarPorNombreParcial() {
        // Req 1.1, 2.1: q numérico → buscarPorCodigoBarras
        String q = "7501234567890";
        when(repositorio.buscarPorCodigoBarras(q)).thenReturn(Collections.emptyList());

        buscador.buscar(q);

        verify(repositorio, times(1)).buscarPorCodigoBarras(q);
        verify(repositorio, never()).buscarPorNombreParcial(anyString());
    }

    @Test
    void buscar_conQTexto_llamaBuscarPorNombreParcialYNoBuscarPorCodigoBarras() {
        // Req 2.1, 1.1: q con texto → buscarPorNombreParcial
        String q = "leche";
        when(repositorio.buscarPorNombreParcial(q)).thenReturn(Collections.emptyList());

        buscador.buscar(q);

        verify(repositorio, times(1)).buscarPorNombreParcial(q);
        verify(repositorio, never()).buscarPorCodigoBarras(anyString());
    }

    // ─── buscar — resultado vacío ─────────────────────────────────────────────

    @Test
    void buscar_cuandoRepositorioRetornaListaVacia_retornaListaVacia() {
        // Req 1.2, 2.2: sin resultados → lista vacía con HTTP 200
        String q = "productoInexistente";
        when(repositorio.buscarPorNombreParcial(q)).thenReturn(Collections.emptyList());

        List<ProductoItem> resultado = buscador.buscar(q);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }
}
