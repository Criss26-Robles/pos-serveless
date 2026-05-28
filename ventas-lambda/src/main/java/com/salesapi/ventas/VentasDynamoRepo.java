package com.salesapi.ventas;

import com.salesapi.ventas.dto.VentaRequest;
import com.salesapi.ventas.dto.VentaResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VentasDynamoRepo {
    private final DynamoDbClient dynamoDb;
    private final String tablaNombre;
    private final String tablaProductos;

    public VentasDynamoRepo(DynamoDbClient dynamoDb, String tablaNombre) {
        this.dynamoDb = dynamoDb;
        this.tablaNombre = tablaNombre;
        this.tablaProductos = System.getenv("TABLA_PRODUCTOS") != null ? System.getenv("TABLA_PRODUCTOS") : "Productos";
    }

    public void guardar(VentaResponse venta) {
        try {
            List<AttributeValue> productosAttr = new ArrayList<>();
            if (venta.getProductos() != null) {
                for (VentaRequest.ProductoVenta p : venta.getProductos()) {
                    Map<String, AttributeValue> productoMap = new HashMap<>();
                    productoMap.put("productId", AttributeValue.fromS(p.getProductoId()));
                    productoMap.put("productName", AttributeValue.fromS(p.getNombre()));
                    productoMap.put("productPrice", AttributeValue.fromN(String.valueOf(p.getPrecioUnitario())));
                    productoMap.put("cantidad", AttributeValue.fromN(String.valueOf(p.getCantidad())));
                    productosAttr.add(AttributeValue.fromM(productoMap));
                }
            }

            Map<String, AttributeValue> detalle = new HashMap<>();
            detalle.put("productos", AttributeValue.fromL(productosAttr));
            detalle.put("subtotal", AttributeValue.fromN(venta.getTotal().toPlainString()));
            detalle.put("total", AttributeValue.fromN(venta.getTotal().toPlainString()));
            detalle.put("metodoPago", AttributeValue.fromS(venta.getMetodoPago() != null ? venta.getMetodoPago() : "efectivo"));
            detalle.put("fecha", AttributeValue.fromS(venta.getFecha()));

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.fromS(venta.getVentaId()));
            item.put("detalle", AttributeValue.fromM(detalle));

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tablaNombre)
                    .item(item)
                    .build();

            dynamoDb.putItem(request);
        } catch (Exception e) {
            throw new RuntimeException("Error guardando venta: " + e.getMessage(), e);
        }
    }

    public void descontarStock(String productoId, int cantidad) {
        try {
            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tablaProductos)
                    .key(Map.of("id", AttributeValue.fromS(productoId)))
                    .updateExpression("SET detalle.stock = detalle.stock - :cantidad")
                    .conditionExpression("detalle.stock >= :cantidad")
                    .expressionAttributeValues(Map.of(
                        ":cantidad", AttributeValue.fromN(String.valueOf(cantidad))
                    ))
                    .build();

            dynamoDb.updateItem(request);
            System.out.println("Stock descontado: producto " + productoId + " - " + cantidad + " unidades");
        } catch (ConditionalCheckFailedException e) {
            System.out.println("Stock insuficiente para producto: " + productoId);
        } catch (Exception e) {
            System.out.println("Error descontando stock: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> obtenerTodas() {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tablaNombre)
                .build();

        ScanResponse scanResponse = dynamoDb.scan(scanRequest);
        List<Map<String, Object>> ventas = new ArrayList<>();

        for (Map<String, AttributeValue> item : scanResponse.items()) {
            Map<String, Object> venta = new HashMap<>();
            venta.put("id", item.get("id").s());

            if (item.containsKey("detalle")) {
                Map<String, AttributeValue> detalle = item.get("detalle").m();
                venta.put("total", detalle.containsKey("total") ? detalle.get("total").n() : "0");
                venta.put("fecha", detalle.containsKey("fecha") ? detalle.get("fecha").s() : "");
                venta.put("metodoPago", detalle.containsKey("metodoPago") ? detalle.get("metodoPago").s() : "efectivo");

                if (detalle.containsKey("productos")) {
                    List<Map<String, Object>> productos = new ArrayList<>();
                    for (AttributeValue pAttr : detalle.get("productos").l()) {
                        Map<String, AttributeValue> pMap = pAttr.m();
                        Map<String, Object> producto = new HashMap<>();
                        producto.put("productId", pMap.get("productId").s());
                        producto.put("productName", pMap.get("productName").s());
                        producto.put("productPrice", pMap.get("productPrice").n());
                        producto.put("cantidad", pMap.get("cantidad").n());
                        productos.add(producto);
                    }
                    venta.put("productos", productos);
                }
            }

            ventas.add(venta);
        }

        return ventas;
    }
}