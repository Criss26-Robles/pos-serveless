package com.salesapi.ventas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesapi.ventas.dto.VentaRequest;
import com.salesapi.ventas.dto.VentaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para VentasHandler.
 * Valida: Requerimientos 3.1, 3.2, 3.3, 3.4, 3.5, 8.2, 8.4
 */
@ExtendWith(MockitoExtension.class)
class VentasHandlerTest {

    @Mock
    private RegistradorVentas registrador;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    private VentasHandler handler;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        handler = new VentasHandler(registrador, mapper);
        when(context.getLogger()).thenReturn(logger);
        doNothing().when(logger).log(anyString());
    }

    // ─── Helper para construir un producto válido ─────────────────────────────

    private VentaRequest.ProductoVenta productoValido(String productoId, int cantidad, double precio) {
        VentaRequest.ProductoVenta p = new VentaRequest.ProductoVenta();
        p.setProductoId(productoId);
        p.setNombre("Producto Test");
        p.setCantidad(cantidad);
        p.setPrecioUnitario(BigDecimal.valueOf(precio));
        return p;
    }

    private VentaResponse ventaResponseEjemplo() {
        VentaRequest.ProductoVenta p = productoValido("prod-001", 2, 24.99);
        return new VentaResponse(
                "f7e8d9c0-b1a2-3456-7890-abcdef123456",
                List.of(p),
                new BigDecimal("49.98"),
                "efectivo",
                "2025-01-15T14:30:00Z"
        );
    }

    // ─── 1. Venta con productos válidos → 201 ────────────────────────────────

    @Test
    void ventaConProductosValidos_retorna201() throws Exception {
        // Req 3.1, 8.2: body válido con lista de productos → HTTP 201 con ventaId
        VentaResponse ventaResponse = ventaResponseEjemplo();
        when(registrador.registrar(any(VentaRequest.class))).thenReturn(ventaResponse);

        String body = "{\"productos\":[{\"productoId\":\"prod-001\",\"nombre\":\"Producto Test\","
                + "\"cantidad\":2,\"precioUnitario\":24.99}],\"total\":49.98,\"metodoPago\":\"efectivo\"}";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("ventaId"), "La respuesta debe contener ventaId");
        assertTrue(response.getBody().contains("f7e8d9c0-b1a2-3456-7890-abcdef123456"),
                "La respuesta debe contener el ventaId generado");
        verify(registrador, times(1)).registrar(any(VentaRequest.class));
    }

    @Test
    void ventaConProductosValidos_retorna201_conHeaderContentType() throws Exception {
        // Req 8.2: Content-Type: application/json en respuesta 201
        when(registrador.registrar(any(VentaRequest.class))).thenReturn(ventaResponseEjemplo());

        String body = "{\"productos\":[{\"productoId\":\"prod-001\",\"nombre\":\"Prod\","
                + "\"cantidad\":1,\"precioUnitario\":10.00}],\"total\":10.00}";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getHeaders());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    // ─── 2. Body vacío → 400 ─────────────────────────────────────────────────

    @Test
    void bodyVacio_retorna400() {
        // Req 3.3: body nulo → HTTP 400 con mensaje de error
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("error"));
        verifyNoInteractions(registrador);
    }

    @Test
    void bodyEspaciosEnBlanco_retorna400() {
        // Req 3.3: body con solo espacios → HTTP 400
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody("   ");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("error"));
        verifyNoInteractions(registrador);
    }

    // ─── 3. Lista de productos vacía → 400 ───────────────────────────────────

    @Test
    void listaProductosVacia_retorna400() {
        // Req 3.4: productos vacío → HTTP 400 con mensaje de lista obligatoria
        String body = "{\"productos\":[],\"total\":49.98}";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertEquals("{\"error\":\"La lista de productos es obligatoria\"}", response.getBody());
        verifyNoInteractions(registrador);
    }

    @Test
    void listaProductosNula_retorna400() {
        // Req 3.4: productos null → HTTP 400 con mensaje de lista obligatoria
        String body = "{\"total\":49.98}";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertEquals("{\"error\":\"La lista de productos es obligatoria\"}", response.getBody());
        verifyNoInteractions(registrador);
    }

    // ─── 4. Error de conexión DynamoDB (mock) → 500 ──────────────────────────

    @Test
    void errorDynamoDB_retorna500() {
        // Req 8.4: RegistradorVentas lanza RuntimeException → HTTP 500
        when(registrador.registrar(any(VentaRequest.class)))
                .thenThrow(new RuntimeException("Connection refused: DynamoDB endpoint unreachable"));

        String body = "{\"productos\":[{\"productoId\":\"prod-001\",\"nombre\":\"Prod\","
                + "\"cantidad\":2,\"precioUnitario\":24.99}],\"total\":49.98}";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(500, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("error"), "La respuesta debe contener campo error");
        verify(registrador, times(1)).registrar(any(VentaRequest.class));
    }

    @Test
    void errorDynamoDB_retorna500_conHeaderContentType() {
        // Req 8.4: Content-Type: application/json en respuesta 500
        when(registrador.registrar(any(VentaRequest.class)))
                .thenThrow(new RuntimeException("DynamoDB error"));

        String body = "{\"productos\":[{\"productoId\":\"prod-001\",\"nombre\":\"Prod\","
                + "\"cantidad\":1,\"precioUnitario\":10.00}],\"total\":10.00}";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(500, response.getStatusCode());
        assertNotNull(response.getHeaders());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    // ─── Validaciones adicionales ─────────────────────────────────────────────

    @Test
    void totalNulo_retorna400() {
        // Req 3.4: total ausente → HTTP 400
        String body = "{\"productos\":[{\"productoId\":\"prod-001\",\"nombre\":\"Prod\","
                + "\"cantidad\":2,\"precioUnitario\":24.99}]}";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertEquals("{\"error\":\"El campo total es obligatorio\"}", response.getBody());
        verifyNoInteractions(registrador);
    }

    @Test
    void cantidadCero_retorna400() {
        // Req 3.5: cantidad = 0 → HTTP 400
        String body = "{\"productos\":[{\"productoId\":\"prod-001\",\"nombre\":\"Prod\","
                + "\"cantidad\":0,\"precioUnitario\":24.99}],\"total\":0.00}";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertEquals("{\"error\":\"La cantidad debe ser mayor a cero\"}", response.getBody());
        verifyNoInteractions(registrador);
    }

    @Test
    void cantidadNegativa_retorna400() {
        // Req 3.5: cantidad < 0 → HTTP 400
        String body = "{\"productos\":[{\"productoId\":\"prod-001\",\"nombre\":\"Prod\","
                + "\"cantidad\":-3,\"precioUnitario\":24.99}],\"total\":49.98}";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertEquals("{\"error\":\"La cantidad debe ser mayor a cero\"}", response.getBody());
        verifyNoInteractions(registrador);
    }

    @Test
    void jsonMalformado_retorna400() {
        // Req 3.3: JSON inválido → HTTP 400
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody("{esto no es json valido");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("error"));
        verifyNoInteractions(registrador);
    }
}
