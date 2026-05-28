package com.salesapi.ventas;

import com.salesapi.ventas.dto.VentaRequest;
import com.salesapi.ventas.dto.VentaResponse;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas basadas en propiedades para RegistradorVentas.
 * Valida: Requerimientos 3.1, 3.2, 3.7
 */
class RegistradorVentasPBTTest {

    @Provide
    Arbitrary<UUID> productoIds() {
        return Arbitraries.create(UUID::randomUUID);
    }

    private VentasDynamoRepo crearRepoMock() {
        VentasDynamoRepo repo = Mockito.mock(VentasDynamoRepo.class);
        Mockito.doNothing().when(repo).guardar(Mockito.any(VentaResponse.class));
        return repo;
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

    // Property 3: Round-trip de datos en registro de venta — Reqs 3.1, 3.2, 3.7
    @Property(tries = 100)
    void registroVentaRoundTrip(
            @ForAll("productoIds") UUID productoId,
            @ForAll @IntRange(min = 1, max = 1000) int cantidad,
            @ForAll @BigRange(min = "0.01", max = "99999") BigDecimal total) {

        VentasDynamoRepo repo = crearRepoMock();
        RegistradorVentas registrador = new RegistradorVentas(repo);

        VentaRequest request = buildRequest(productoId.toString(), cantidad, total);
        VentaResponse response = registrador.registrar(request);

        // productos deben coincidir
        assertNotNull(response.getProductos(), "productos en response no debe ser nulo");
        assertEquals(1, response.getProductos().size(), "debe haber exactamente 1 producto");
        assertEquals(productoId.toString(), response.getProductos().get(0).getProductoId(),
                "productoId en response debe coincidir con el request");
        assertEquals(cantidad, response.getProductos().get(0).getCantidad(),
                "cantidad en response debe coincidir con el request");

        // total debe ser igual por compareTo (ignora escala)
        assertEquals(0, request.getTotal().compareTo(response.getTotal()),
                "total en response debe ser igual al del request");

        // ventaId debe ser un UUID v4 válido
        UUID ventaUUID = UUID.fromString(response.getVentaId());
        assertEquals(4, ventaUUID.version(),
                "ventaId debe ser un UUID versión 4");

        // fecha debe ser parseable como Instant ISO-8601
        assertDoesNotThrow(() -> Instant.parse(response.getFecha()),
                "fecha debe ser un Instant ISO-8601 válido");
    }

    // Property 6: Unicidad de ventaId generado — Req 3.7
    @Property(tries = 100)
    void unicidadVentaId(
            @ForAll @IntRange(min = 2, max = 10) int n) {

        VentasDynamoRepo repo = crearRepoMock();
        RegistradorVentas registrador = new RegistradorVentas(repo);

        List<String> ventaIds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            VentaRequest request = buildRequest(
                    UUID.randomUUID().toString(),
                    i + 1,
                    BigDecimal.valueOf(10.00 * (i + 1))
            );
            VentaResponse response = registrador.registrar(request);
            ventaIds.add(response.getVentaId());
        }

        // Todos los ventaId deben ser distintos entre sí
        Set<String> ventaIdsUnicos = new HashSet<>(ventaIds);
        assertEquals(n, ventaIdsUnicos.size(),
                "Todos los ventaId generados deben ser únicos");

        // Cada ventaId debe ser un UUID v4 válido
        for (String ventaId : ventaIds) {
            UUID uuid = UUID.fromString(ventaId);
            assertEquals(4, uuid.version(),
                    "Cada ventaId debe ser un UUID versión 4, pero fue: " + ventaId);
        }
    }
}
