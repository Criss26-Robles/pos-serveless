package com.salesapi.ventas;

import com.salesapi.ventas.dto.VentaRequest;
import com.salesapi.ventas.dto.VentaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pruebas unitarias para RegistradorVentas.
 * Valida: Requerimientos 3.1, 3.2, 3.7
 */
@ExtendWith(MockitoExtension.class)
class RegistradorVentasTest {

    @Mock
    private VentasDynamoRepo repositorio;

    private RegistradorVentas registrador;

    @BeforeEach
    void setUp() {
        registrador = new RegistradorVentas(repositorio);
    }

    private VentaRequest buildRequest(String productoId, int cantidad, BigDecimal total) {
        VentaRequest.ProductoVenta p = new VentaRequest.ProductoVenta();
        p.setProductoId(productoId);
        p.setNombre("Producto Test");
        p.setCantidad(cantidad);
        p.setPrecioUnitario(total.divide(BigDecimal.valueOf(cantidad), 2, java.math.RoundingMode.HALF_UP));

        VentaRequest request = new VentaRequest();
        request.setProductos(List.of(p));
        request.setTotal(total);
        request.setMetodoPago("efectivo");
        return request;
    }

    @Test
    void registrar_debeRetornarProductosDelRequest() {
        // Req 3.1: VentaResponse.productos coincide con VentaRequest.productos
        String productoId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        VentaRequest request = buildRequest(productoId, 2, new BigDecimal("49.98"));

        VentaResponse response = registrador.registrar(request);

        assertNotNull(response.getProductos());
        assertEquals(1, response.getProductos().size());
        assertEquals(productoId, response.getProductos().get(0).getProductoId());
    }

    @Test
    void registrar_debeRetornarCantidadDelRequest() {
        // Req 3.1: VentaResponse.productos[0].cantidad coincide con VentaRequest
        VentaRequest request = buildRequest("prod-123", 5, new BigDecimal("100.00"));

        VentaResponse response = registrador.registrar(request);

        assertEquals(5, response.getProductos().get(0).getCantidad());
    }

    @Test
    void registrar_debeRetornarTotalDelRequest() {
        // Req 3.1: VentaResponse.total coincide con VentaRequest.total
        BigDecimal total = new BigDecimal("249.99");
        VentaRequest request = buildRequest("prod-456", 3, total);

        VentaResponse response = registrador.registrar(request);

        assertEquals(0, total.compareTo(response.getTotal()));
    }

    @Test
    void registrar_debeGenerarVentaIdComoUUIDValido() {
        // Req 3.7: VentaResponse.ventaId es un UUID válido (parseable con UUID.fromString())
        VentaRequest request = buildRequest("prod-789", 1, new BigDecimal("24.50"));

        VentaResponse response = registrador.registrar(request);

        assertDoesNotThrow(() -> UUID.fromString(response.getVentaId()),
                "ventaId debe ser un UUID válido");
        assertNotNull(response.getVentaId());
    }

    @Test
    void registrar_debeGenerarFechaEnFormatoISO8601() {
        // Req 3.2: VentaResponse.fecha es parseable con Instant.parse()
        VentaRequest request = buildRequest("prod-abc", 2, new BigDecimal("50.00"));

        VentaResponse response = registrador.registrar(request);

        assertDoesNotThrow(() -> Instant.parse(response.getFecha()),
                "fecha debe ser parseable como Instant ISO-8601");
        assertNotNull(response.getFecha());
    }

    @Test
    void registrar_debeInvocarRepositorioGuardarExactamenteUnaVez() {
        // Req 3.1: repositorio.guardar() es invocado exactamente una vez
        VentaRequest request = buildRequest("prod-xyz", 4, new BigDecimal("199.96"));

        registrador.registrar(request);

        verify(repositorio, times(1)).guardar(any(VentaResponse.class));
    }
}
