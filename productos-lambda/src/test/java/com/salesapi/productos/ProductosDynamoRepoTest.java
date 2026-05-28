package com.salesapi.productos;

import com.salesapi.productos.dto.ProductoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ProductosDynamoRepo.
 * Verifica la búsqueda por código de barras, por nombre parcial y el mapeo de ítems.
 *
 * Requerimientos: 1.1, 2.1, 2.3
 */
@ExtendWith(MockitoExtension.class)
class ProductosDynamoRepoTest {

    @Mock
    private DynamoDbClient dynamoDb;

    private ProductosDynamoRepo repo;

    private static final String TABLA = "TablaProductos";

    @BeforeEach
    void setUp() {
        repo = new ProductosDynamoRepo(dynamoDb, TABLA);
    }

    // ─── buscarPorCodigoBarras ────────────────────────────────────────────────

    @Test
    void buscarPorCodigoBarras_retornaListaVaciaWhenNoHayItems() {
        when(dynamoDb.scan(any(ScanRequest.class)))
                .thenReturn(ScanResponse.builder().items(List.of()).build());

        List<ProductoItem> resultado = repo.buscarPorCodigoBarras("9999999999999");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void buscarPorCodigoBarras_filtraItemsQueCoinciden() {
        // Ítem con detalle que coincide con el código de barras buscado
        Map<String, AttributeValue> detalle = new HashMap<>();
        detalle.put("codigoBarras", AttributeValue.fromS("7501234567890"));
        detalle.put("nombre", AttributeValue.fromS("Leche Entera 1L"));
        detalle.put("precio", AttributeValue.fromN("24.50"));
        detalle.put("stock", AttributeValue.fromN("10"));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS("id-001"));
        item.put("detalle", AttributeValue.fromM(detalle));

        when(dynamoDb.scan(any(ScanRequest.class)))
                .thenReturn(ScanResponse.builder().items(List.of(item)).build());

        List<ProductoItem> resultado = repo.buscarPorCodigoBarras("7501234567890");

        assertEquals(1, resultado.size());
        assertEquals("id-001", resultado.get(0).getId());
        assertEquals("7501234567890", resultado.get(0).getCodigoBarras());
        assertEquals("Leche Entera 1L", resultado.get(0).getNombre());
        assertEquals(24.50, resultado.get(0).getPrecio(), 0.001);
        assertEquals(10, resultado.get(0).getStock());
    }

    @Test
    void buscarPorCodigoBarras_noRetornaItemsConCodigoDistinto() {
        // Ítem con código de barras diferente al buscado
        Map<String, AttributeValue> detalle = new HashMap<>();
        detalle.put("codigoBarras", AttributeValue.fromS("1111111111111"));
        detalle.put("nombre", AttributeValue.fromS("Otro Producto"));
        detalle.put("precio", AttributeValue.fromN("10.00"));
        detalle.put("stock", AttributeValue.fromN("5"));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS("id-002"));
        item.put("detalle", AttributeValue.fromM(detalle));

        when(dynamoDb.scan(any(ScanRequest.class)))
                .thenReturn(ScanResponse.builder().items(List.of(item)).build());

        List<ProductoItem> resultado = repo.buscarPorCodigoBarras("9999999999999");

        assertTrue(resultado.isEmpty());
    }

    // ─── buscarPorNombreParcial ───────────────────────────────────────────────

    @Test
    void buscarPorNombreParcial_retornaListaVaciaWhenNoHayItems() {
        when(dynamoDb.scan(any(ScanRequest.class)))
                .thenReturn(ScanResponse.builder().items(List.of()).build());

        List<ProductoItem> resultado = repo.buscarPorNombreParcial("inexistente");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void buscarPorNombreParcial_filtraItemsQueContienenNombre() {
        Map<String, AttributeValue> detalle = new HashMap<>();
        detalle.put("codigoBarras", AttributeValue.fromS("7509876543210"));
        detalle.put("nombre", AttributeValue.fromS("Leche Descremada 1L"));
        detalle.put("precio", AttributeValue.fromN("22.00"));
        detalle.put("stock", AttributeValue.fromN("5"));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS("id-003"));
        item.put("detalle", AttributeValue.fromM(detalle));

        when(dynamoDb.scan(any(ScanRequest.class)))
                .thenReturn(ScanResponse.builder().items(List.of(item)).build());

        List<ProductoItem> resultado = repo.buscarPorNombreParcial("leche");

        assertEquals(1, resultado.size());
        assertEquals("Leche Descremada 1L", resultado.get(0).getNombre());
    }

    @Test
    void buscarPorNombreParcial_esCaseInsensitive() {
        Map<String, AttributeValue> detalle = new HashMap<>();
        detalle.put("codigoBarras", AttributeValue.fromS("7509876543210"));
        detalle.put("nombre", AttributeValue.fromS("Leche Descremada 1L"));
        detalle.put("precio", AttributeValue.fromN("22.00"));
        detalle.put("stock", AttributeValue.fromN("5"));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS("id-004"));
        item.put("detalle", AttributeValue.fromM(detalle));

        when(dynamoDb.scan(any(ScanRequest.class)))
                .thenReturn(ScanResponse.builder().items(List.of(item)).build());

        // Búsqueda en mayúsculas debe encontrar el producto
        List<ProductoItem> resultado = repo.buscarPorNombreParcial("LECHE");

        assertEquals(1, resultado.size());
    }

    // ─── Mapeo de ítems DynamoDB a ProductoItem ───────────────────────────────

    @Test
    void buscarPorCodigoBarras_mapeaItemCorrectamente() {
        Map<String, AttributeValue> detalle = new HashMap<>();
        detalle.put("codigoBarras", AttributeValue.fromS("7501234567890"));
        detalle.put("nombre", AttributeValue.fromS("Leche Entera 1L"));
        detalle.put("precio", AttributeValue.fromN("24.50"));
        detalle.put("stock", AttributeValue.fromN("10"));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
        item.put("detalle", AttributeValue.fromM(detalle));

        when(dynamoDb.scan(any(ScanRequest.class)))
                .thenReturn(ScanResponse.builder().items(List.of(item)).build());

        List<ProductoItem> resultado = repo.buscarPorCodigoBarras("7501234567890");

        assertEquals(1, resultado.size());
        ProductoItem producto = resultado.get(0);

        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", producto.getId());
        assertEquals("Leche Entera 1L", producto.getNombre());
        assertEquals("7501234567890", producto.getCodigoBarras());
        assertEquals(24.50, producto.getPrecio(), 0.001);
        assertEquals(10, producto.getStock());
    }

    @Test
    void buscarPorNombreParcial_mapeaItemCorrectamente() {
        Map<String, AttributeValue> detalle = new HashMap<>();
        detalle.put("codigoBarras", AttributeValue.fromS("7509876543210"));
        detalle.put("nombre", AttributeValue.fromS("Leche Descremada 1L"));
        detalle.put("precio", AttributeValue.fromN("22.00"));
        detalle.put("stock", AttributeValue.fromN("3"));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS("b2c3d4e5-f6a7-8901-bcde-f12345678901"));
        item.put("detalle", AttributeValue.fromM(detalle));

        when(dynamoDb.scan(any(ScanRequest.class)))
                .thenReturn(ScanResponse.builder().items(List.of(item)).build());

        List<ProductoItem> resultado = repo.buscarPorNombreParcial("leche");

        assertEquals(1, resultado.size());
        ProductoItem producto = resultado.get(0);

        assertEquals("b2c3d4e5-f6a7-8901-bcde-f12345678901", producto.getId());
        assertEquals("Leche Descremada 1L", producto.getNombre());
        assertEquals("7509876543210", producto.getCodigoBarras());
        assertEquals(22.00, producto.getPrecio(), 0.001);
        assertEquals(3, producto.getStock());
    }
}
