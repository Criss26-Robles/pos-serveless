package com.salesapi.ventas;

import com.salesapi.ventas.dto.VentaRequest;
import com.salesapi.ventas.dto.VentaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para VentasDynamoRepo.
 * Verifica que guardar() construye el PutItemRequest con los atributos correctos.
 *
 * Requerimientos: 3.1, 5.1, 5.2
 */
@ExtendWith(MockitoExtension.class)
class VentasDynamoRepoTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    private VentasDynamoRepo repo;

    private static final String TABLA_NOMBRE = "TablaVentas";

    @BeforeEach
    void setUp() {
        repo = new VentasDynamoRepo(dynamoDbClient, TABLA_NOMBRE);
    }

    private VentaResponse ventaEjemplo() {
        VentaRequest.ProductoVenta p = new VentaRequest.ProductoVenta();
        p.setProductoId("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        p.setNombre("Producto Test");
        p.setCantidad(2);
        p.setPrecioUnitario(new BigDecimal("24.99"));

        return new VentaResponse(
                "f7e8d9c0-b1a2-3456-7890-abcdef123456",
                List.of(p),
                new BigDecimal("49.98"),
                "efectivo",
                "2025-01-15T14:30:00Z"
        );
    }

    @Test
    void guardar_construyePutItemRequestConAtributosId_yDetalle() {
        // El PutItemRequest debe contener los atributos "id" y "detalle"
        VentaResponse venta = ventaEjemplo();
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        repo.guardar(venta);

        verify(dynamoDbClient, times(1)).putItem(captor.capture());
        PutItemRequest captured = captor.getValue();

        Map<String, AttributeValue> item = captured.item();
        assertTrue(item.containsKey("id"), "Debe contener el atributo id");
        assertTrue(item.containsKey("detalle"), "Debe contener el atributo detalle");
    }

    @Test
    void guardar_usaTableNameCorrecta() {
        VentaResponse venta = ventaEjemplo();
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        repo.guardar(venta);

        verify(dynamoDbClient).putItem(captor.capture());
        assertEquals(TABLA_NOMBRE, captor.getValue().tableName());
    }

    @Test
    void guardar_ventaId_seAlmacenaComoAttributeValueFromS() {
        String ventaId = "f7e8d9c0-b1a2-3456-7890-abcdef123456";
        VentaResponse venta = ventaEjemplo();
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        repo.guardar(venta);

        verify(dynamoDbClient).putItem(captor.capture());
        AttributeValue atributo = captor.getValue().item().get("id");
        assertNotNull(atributo, "El atributo id no debe ser nulo");
        assertEquals(ventaId, atributo.s(),
                "id debe almacenarse como AttributeValue.fromS()");
        assertNull(atributo.n(),
                "id NO debe almacenarse como AttributeValue.fromN()");
    }

    @Test
    void guardar_detalleContieneTotalYFecha() {
        VentaResponse venta = ventaEjemplo();
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        repo.guardar(venta);

        verify(dynamoDbClient).putItem(captor.capture());
        AttributeValue detalleAttr = captor.getValue().item().get("detalle");
        assertNotNull(detalleAttr, "El atributo detalle no debe ser nulo");

        Map<String, AttributeValue> detalle = detalleAttr.m();
        assertTrue(detalle.containsKey("total"), "detalle debe contener total");
        assertTrue(detalle.containsKey("fecha"), "detalle debe contener fecha");
        assertTrue(detalle.containsKey("metodoPago"), "detalle debe contener metodoPago");
        assertTrue(detalle.containsKey("productos"), "detalle debe contener productos");
    }

    @Test
    void guardar_total_seAlmacenaComoAttributeValueFromN() {
        BigDecimal total = new BigDecimal("49.98");
        VentaResponse venta = ventaEjemplo();
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        repo.guardar(venta);

        verify(dynamoDbClient).putItem(captor.capture());
        Map<String, AttributeValue> detalle = captor.getValue().item().get("detalle").m();
        AttributeValue atributo = detalle.get("total");
        assertNotNull(atributo, "El atributo total no debe ser nulo");
        assertEquals(total.toPlainString(), atributo.n(),
                "total debe almacenarse como AttributeValue.fromN()");
        assertNull(atributo.s(),
                "total NO debe almacenarse como AttributeValue.fromS()");
    }

    @Test
    void guardar_fecha_seAlmacenaComoAttributeValueFromS() {
        String fecha = "2025-01-15T14:30:00Z";
        VentaResponse venta = ventaEjemplo();
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        repo.guardar(venta);

        verify(dynamoDbClient).putItem(captor.capture());
        Map<String, AttributeValue> detalle = captor.getValue().item().get("detalle").m();
        AttributeValue atributo = detalle.get("fecha");
        assertNotNull(atributo, "El atributo fecha no debe ser nulo");
        assertEquals(fecha, atributo.s(),
                "fecha debe almacenarse como AttributeValue.fromS()");
        assertNull(atributo.n(),
                "fecha NO debe almacenarse como AttributeValue.fromS()");
    }

    @Test
    void guardar_invocaDynamoDbPutItemExactamenteUnaVez() {
        VentaResponse venta = ventaEjemplo();

        repo.guardar(venta);

        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }
}
