package com.salesapi.productos;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.salesapi.productos.dto.ProductoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ProductosHandler.
 * Valida: Requerimientos 1.1, 1.2, 1.3, 8.1, 8.3
 */
@ExtendWith(MockitoExtension.class)
class ProductosHandlerTest {

    @Mock
    private BuscadorProductos buscadorProductos;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger lambdaLogger;

    private ProductosHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductosHandler(buscadorProductos);
    }

    // ─── Búsqueda por código de barras ────────────────────────────────────────

    @Test
    void buscarPorCodigoBarras_retorna200ConProducto() {
        // q es numérico → BuscadorProductos.buscar delega a buscarPorCodigoBarras
        ProductoItem producto = new ProductoItem("id-1", "7501234567890", "Leche Entera 1L", 24.50, 10);
        when(buscadorProductos.buscar("7501234567890")).thenReturn(List.of(producto));

        Map<String, String> params = new HashMap<>();
        params.put("q", "7501234567890");
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder()
                .withQueryStringParameters(params)
                .build();

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, context);

        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertTrue(response.getBody().contains("7501234567890"));
        assertTrue(response.getBody().contains("Leche Entera 1L"));
    }

    // ─── Búsqueda por nombre ──────────────────────────────────────────────────

    @Test
    void buscarPorNombre_retorna200ConProductos() {
        // q tiene letras → BuscadorProductos.buscar delega a buscarPorNombreParcial
        ProductoItem producto = new ProductoItem("id-2", "7509876543210", "Leche Descremada 1L", 22.00, 5);
        when(buscadorProductos.buscar("leche")).thenReturn(List.of(producto));

        Map<String, String> params = new HashMap<>();
        params.put("q", "leche");
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder()
                .withQueryStringParameters(params)
                .build();

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, context);

        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertTrue(response.getBody().contains("Leche Descremada 1L"));
    }

    // ─── Tabla vacía / sin resultados ─────────────────────────────────────────

    @Test
    void tablaVacia_retorna200ConListaVacia() {
        // q presente pero no hay resultados → 200 con []
        when(buscadorProductos.buscar("xyz")).thenReturn(Collections.emptyList());

        Map<String, String> params = new HashMap<>();
        params.put("q", "xyz");
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder()
                .withQueryStringParameters(params)
                .build();

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, context);

        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("[]", response.getBody());
    }

    // ─── Error de DynamoDB → 500 ──────────────────────────────────────────────

    @Test
    void errorDynamoDB_retorna500() {
        // BuscadorProductos lanza RuntimeException → handler devuelve 500
        when(buscadorProductos.buscar(anyString()))
                .thenThrow(new RuntimeException("Connection to DynamoDB failed"));
        when(context.getLogger()).thenReturn(lambdaLogger);

        Map<String, String> params = new HashMap<>();
        params.put("q", "leche");
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder()
                .withQueryStringParameters(params)
                .build();

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, context);

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Connection to DynamoDB failed"));
        verify(context.getLogger()).log("Error: Connection to DynamoDB failed");
    }
}
