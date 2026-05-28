package com.salesapi.productos;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import java.util.Map;

public class CrearProductoHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final ProductosDynamoRepo repositorio;
    private final ObjectMapper mapper = new ObjectMapper();

    public CrearProductoHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String tableName = System.getenv("TABLA_PRODUCTOS");
        if (tableName == null) tableName = "Productos";
        this.repositorio = new ProductosDynamoRepo(dynamoDbClient, tableName);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            String body = event.getBody();
            if (body == null || body.trim().isEmpty()) {
                return buildResponse(400, "{\"error\":\"Cuerpo de la peticion invalido\"}");
            }

            Map<String, Object> data = mapper.readValue(body, Map.class);

            String nombre = (String) data.get("nombre");
            String codigoBarras = (String) data.get("codigo_barras");
            Object precioObj = data.get("precio");
            Object stockObj = data.get("stock");

            if (nombre == null || codigoBarras == null || precioObj == null) {
                return buildResponse(400, "{\"error\":\"Los campos nombre, codigo_barras y precio son obligatorios\"}");
            }

            double precio = Double.parseDouble(precioObj.toString());
            int stock = stockObj != null ? Integer.parseInt(stockObj.toString()) : 0;
            String id = String.valueOf(System.currentTimeMillis());

            repositorio.crearProducto(id, codigoBarras, nombre, precio, stock);

            String responseBody = mapper.writeValueAsString(Map.of(
                "id", id,
                "detalle", Map.of(
                    "nombre", nombre,
                    "codigoBarras", codigoBarras,
                    "precio", precio,
                    "stock", stock
                )
            ));

            context.getLogger().log("Producto creado: " + nombre);
            return buildResponse(201, responseBody);

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return buildResponse(500, "{\"error\":\"Error interno del servidor\"}");
        }
    }

    private APIGatewayV2HTTPResponse buildResponse(int statusCode, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withBody(body)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .build();
    }
}