package com.salesapi.ventas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Map;

public class ConsultaVentasHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final VentasDynamoRepo repositorio;
    private final ObjectMapper mapper;

    public ConsultaVentasHandler() {
        DynamoDbClient dynamoDb = DynamoDbClient.create();
        String tablaNombre = System.getenv("TABLA_VENTAS");
        this.repositorio = new VentasDynamoRepo(dynamoDb, tablaNombre);
        this.mapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        try {
            List<Map<String, Object>> ventas = repositorio.obtenerTodas();
            String responseBody = mapper.writeValueAsString(ventas);
            context.getLogger().log("Ventas encontradas: " + ventas.size());
            return buildResponse(200, responseBody);
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return buildResponse(500, "{\"error\":\"Error interno del servidor\"}");
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }
}