package com.salesapi.ventas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesapi.ventas.dto.VentaRequest;
import com.salesapi.ventas.dto.VentaResponse;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class VentasHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final RegistradorVentas registrador;
    private final ObjectMapper mapper;

    public VentasHandler() {
        DynamoDbClient dynamoDb = DynamoDbClient.create();
        String tablaNombre = System.getenv("TABLA_VENTAS");
        VentasDynamoRepo repositorio = new VentasDynamoRepo(dynamoDb, tablaNombre);
        this.registrador = new RegistradorVentas(repositorio);
        this.mapper = new ObjectMapper();
    }

    public VentasHandler(RegistradorVentas registrador, ObjectMapper mapper) {
        this.registrador = registrador;
        this.mapper = mapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        String body = event.getBody();
        context.getLogger().log("Body recibido: " + body);

        if (body == null || body.trim().isEmpty()) {
            return buildResponse(400, "{\"error\":\"Cuerpo de la peticion invalido o ausente\"}");
        }

        VentaRequest request;
        try {
            request = mapper.readValue(body, VentaRequest.class);
            context.getLogger().log("Request parseado: productos=" + (request.getProductos() != null ? request.getProductos().size() : "null"));
        } catch (JsonProcessingException e) {
            context.getLogger().log("Error parsing JSON: " + e.getMessage());
            return buildResponse(400, "{\"error\":\"Cuerpo de la peticion invalido o ausente\"}");
        }

        if (request.getProductos() == null || request.getProductos().isEmpty()) {
            return buildResponse(400, "{\"error\":\"La lista de productos es obligatoria\"}");
        }

        if (request.getTotal() == null) {
            return buildResponse(400, "{\"error\":\"El campo total es obligatorio\"}");
        }

        for (VentaRequest.ProductoVenta p : request.getProductos()) {
            if (p.getCantidad() == null || p.getCantidad() <= 0) {
                return buildResponse(400, "{\"error\":\"La cantidad debe ser mayor a cero\"}");
            }
        }

        try {
            VentaResponse ventaResponse = registrador.registrar(request);
            String responseBody = mapper.writeValueAsString(ventaResponse);
            context.getLogger().log("Venta registrada: " + ventaResponse.getVentaId());
            return buildResponse(201, responseBody);
        } catch (SdkException e) {
            context.getLogger().log("Error DynamoDB: " + e.getMessage());
            return buildResponse(500, "{\"error\":\"Error interno del servidor: " + e.getMessage() + "\"}");
        } catch (Exception e) {
            context.getLogger().log("Error general: " + e.getMessage());
            return buildResponse(500, "{\"error\":\"Error interno del servidor: " + e.getMessage() + "\"}");
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }
}