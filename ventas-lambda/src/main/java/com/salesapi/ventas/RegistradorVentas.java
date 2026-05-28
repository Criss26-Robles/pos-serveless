package com.salesapi.ventas;

import com.salesapi.ventas.dto.VentaRequest;
import com.salesapi.ventas.dto.VentaResponse;
import java.time.Instant;
import java.util.UUID;

public class RegistradorVentas {
    private final VentasDynamoRepo repositorio;

    public RegistradorVentas(VentasDynamoRepo repositorio) {
        this.repositorio = repositorio;
    }

    public VentaResponse registrar(VentaRequest request) {
        String ventaId = UUID.randomUUID().toString();
        String fecha = Instant.now().toString();

        VentaResponse ventaResponse = new VentaResponse(
                ventaId,
                request.getProductos(),
                request.getTotal(),
                request.getMetodoPago() != null ? request.getMetodoPago() : "efectivo",
                fecha
        );

        repositorio.guardar(ventaResponse);

        if (request.getProductos() != null) {
            for (VentaRequest.ProductoVenta producto : request.getProductos()) {
                repositorio.descontarStock(producto.getProductoId(), producto.getCantidad());
            }
        }

        return ventaResponse;
    }
}