package com.salesapi.productos;

import com.salesapi.productos.dto.ProductoItem;
import java.util.*;

public class BuscadorProductos {

    private final ProductosDynamoRepo repositorio;

    public BuscadorProductos(ProductosDynamoRepo repositorio) {
        this.repositorio = repositorio;
    }

    public boolean esNumerico(String q) {
        return q != null && q.matches("\\d+");
    }

    public List<ProductoItem> buscar(String q) {
        if (q == null || q.trim().isEmpty()) {
            return repositorio.obtenerTodos();
        }
        if (esNumerico(q)) {
            return repositorio.buscarPorCodigoBarras(q);
        } else {
            return repositorio.buscarPorNombreParcial(q);
        }
    }
}