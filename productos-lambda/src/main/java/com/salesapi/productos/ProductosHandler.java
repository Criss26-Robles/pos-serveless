package com.salesapi.productos;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.salesapi.productos.dto.ProductoItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class ProductosHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private final ProductosDynamoRepo repositorio;
    private final BuscadorProductos buscador;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductosHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String tableName = System.getenv("TABLA_PRODUCTOS");
        if (tableName == null) tableName = "Productos";
        this.repositorio = new ProductosDynamoRepo(dynamoDbClient, tableName);
        this.buscador = new BuscadorProductos(repositorio);
    }

    // Constructor para pruebas unitarias
    ProductosHandler(BuscadorProductos buscador) {
        this.repositorio = null;
        this.buscador = buscador;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            Map<String, String> queryParams = event.getQueryStringParameters();
            String query = queryParams != null ? queryParams.get("q") : null;

            List<ProductoItem> productos = buscador.buscar(query);

            String responseBody = objectMapper.writeValueAsString(productos);

            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withBody(responseBody)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .build();
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(500)
                    .withBody("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}