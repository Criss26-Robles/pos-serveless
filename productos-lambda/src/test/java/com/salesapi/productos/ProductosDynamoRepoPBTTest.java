package com.salesapi.productos;

import com.salesapi.productos.dto.ProductoItem;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mockito;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas basadas en propiedades para ProductosDynamoRepo.
 * Valida: Requerimiento 2.3
 */
class ProductosDynamoRepoPBTTest {

    /**
     * Property 2: Estructura completa de la respuesta de productos — Req 2.3
     *
     * Para cualquier combinación válida de campos (id, nombre, codigoBarras, precio, stock),
     * el mapper de ProductosDynamoRepo debe producir un ProductoItem con todos los campos
     * correctamente mapeados sin pérdida de información.
     */
    @Property(tries = 100)
    void estructuraCompletaRespuestaProductos(
            @ForAll @StringLength(min = 1, max = 50) String id,
            @ForAll @StringLength(min = 1, max = 50) String nombre,
            @ForAll @StringLength(min = 1, max = 50) String codigoBarras,
            @ForAll @DoubleRange(min = 0.0, max = 99999.0) double precio) {

        // Arrange — construir el ítem DynamoDB con los campos presentes en el formato "detalle"
        Map<String, AttributeValue> detalle = new HashMap<>();
        detalle.put("nombre", AttributeValue.fromS(nombre));
        detalle.put("codigoBarras", AttributeValue.fromS(codigoBarras));
        detalle.put("precio", AttributeValue.fromN(String.valueOf(precio)));
        detalle.put("stock", AttributeValue.fromN("0"));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS(id));
        item.put("detalle", AttributeValue.fromM(detalle));

        DynamoDbClient dynamoDb = Mockito.mock(DynamoDbClient.class);
        when(dynamoDb.scan(any(ScanRequest.class)))
                .thenReturn(ScanResponse.builder().items(List.of(item)).build());

        ProductosDynamoRepo repo = new ProductosDynamoRepo(dynamoDb, "TablaProductos");

        // Act — llamar a obtenerTodos para ejercitar el mapper
        List<ProductoItem> resultado = repo.obtenerTodos();

        // Assert — el mapper produce un ProductoItem con todos los campos mapeados sin pérdida
        assertNotNull(resultado, "El resultado no debe ser nulo");
        assertEquals(1, resultado.size(), "Debe retornar exactamente un ítem");

        ProductoItem productoItem = resultado.get(0);

        assertNotNull(productoItem.getId(),           "id no debe ser nulo");
        assertNotNull(productoItem.getNombre(),       "nombre no debe ser nulo");
        assertNotNull(productoItem.getCodigoBarras(), "codigoBarras no debe ser nulo");

        assertEquals(id,           productoItem.getId(),
                "id debe coincidir con el valor del ítem DynamoDB");
        assertEquals(nombre,       productoItem.getNombre(),
                "nombre debe coincidir con el valor del ítem DynamoDB");
        assertEquals(codigoBarras, productoItem.getCodigoBarras(),
                "codigoBarras debe coincidir con el valor del ítem DynamoDB");
        assertEquals(precio, productoItem.getPrecio(), 0.001,
                "precio debe coincidir con el valor del ítem DynamoDB sin pérdida de información");
    }
}
