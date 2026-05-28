package com.salesapi.productos;

import com.salesapi.productos.dto.ProductoItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.*;
import java.util.stream.Collectors;

public class ProductosDynamoRepo {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public ProductosDynamoRepo(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public List<ProductoItem> buscarPorCodigoBarras(String codigoBarras) {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
        return scanResponse.items().stream()
                .filter(item -> {
                    if (!item.containsKey("detalle")) return false;
                    Map<String, AttributeValue> detalle = item.get("detalle").m();
                    return detalle.containsKey("codigoBarras") &&
                           detalle.get("codigoBarras").s().equals(codigoBarras);
                })
                .map(this::toProductoItem)
                .collect(Collectors.toList());
    }

    public List<ProductoItem> buscarPorNombreParcial(String nombre) {
        String nombreLower = nombre.toLowerCase();

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
        return scanResponse.items().stream()
                .filter(item -> {
                    if (!item.containsKey("detalle")) return false;
                    Map<String, AttributeValue> detalle = item.get("detalle").m();
                    return detalle.containsKey("nombre") &&
                           detalle.get("nombre").s().toLowerCase().contains(nombreLower);
                })
                .map(this::toProductoItem)
                .collect(Collectors.toList());
    }

    public List<ProductoItem> obtenerTodos() {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
        return scanResponse.items().stream()
                .filter(item -> item.containsKey("detalle"))
                .map(this::toProductoItem)
                .collect(Collectors.toList());
    }

    public void crearProducto(String id, String codigoBarras, String nombre, double precio, int stock) {
        Map<String, AttributeValue> detalle = new HashMap<>();
        detalle.put("nombre", AttributeValue.fromS(nombre));
        detalle.put("codigoBarras", AttributeValue.fromS(codigoBarras));
        detalle.put("precio", AttributeValue.fromN(String.valueOf(precio)));
        detalle.put("stock", AttributeValue.fromN(String.valueOf(stock)));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS(id));
        item.put("detalle", AttributeValue.fromM(detalle));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }

    public void actualizarStock(String id, int cantidad) {
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("id", AttributeValue.fromS(id)))
                .updateExpression("SET detalle.stock = detalle.stock - :cantidad")
                .conditionExpression("detalle.stock >= :cantidad")
                .expressionAttributeValues(Map.of(
                    ":cantidad", AttributeValue.fromN(String.valueOf(cantidad))
                ))
                .build();

        dynamoDbClient.updateItem(request);
    }

    private ProductoItem toProductoItem(Map<String, AttributeValue> item) {
        ProductoItem producto = new ProductoItem();
        producto.setId(item.get("id").s());

        if (item.containsKey("detalle")) {
            Map<String, AttributeValue> detalle = item.get("detalle").m();
            producto.setNombre(detalle.containsKey("nombre") ? detalle.get("nombre").s() : "");
            producto.setCodigoBarras(detalle.containsKey("codigoBarras") ? detalle.get("codigoBarras").s() : "");
            producto.setPrecio(detalle.containsKey("precio") ? Double.parseDouble(detalle.get("precio").n()) : 0);
            producto.setStock(detalle.containsKey("stock") ? Integer.parseInt(detalle.get("stock").n()) : 0);
        } else {
            // compatibilidad con productos viejos sin detalle
            producto.setNombre(item.containsKey("nombre") ? item.get("nombre").s() : "");
            producto.setCodigoBarras(item.containsKey("codigo_barras") ? item.get("codigo_barras").s() : "");
            producto.setPrecio(item.containsKey("precio") ? Double.parseDouble(item.get("precio").n()) : 0);
            producto.setStock(item.containsKey("stock") ? Integer.parseInt(item.get("stock").n()) : 0);
        }

        return producto;
    }
}