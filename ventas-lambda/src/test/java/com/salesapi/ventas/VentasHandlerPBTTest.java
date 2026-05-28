package com.salesapi.ventas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pruebas basadas en propiedades para VentasHandler.
 * Valida: Requerimientos 3.5
 */
class VentasHandlerPBTTest {

    private Context buildContext() {
        Context ctx = Mockito.mock(Context.class);
        LambdaLogger logger = Mockito.mock(LambdaLogger.class);
        Mockito.when(ctx.getLogger()).thenReturn(logger);
        Mockito.doNothing().when(logger).log(Mockito.anyString());
        return ctx;
    }

    // Property 4: Rechazo de cantidad inválida — Req 3.5
    /**
     * Para cualquier valor de cantidad <= 0, el handler debe rechazar con HTTP 400
     * y el mensaje exacto "La cantidad debe ser mayor a cero", sin persistir datos.
     *
     * Validates: Requirement 3.5
     */
    @Property(tries = 100)
    void rechazoDeCanidadInvalida(
            @ForAll @IntRange(min = -2147483648, max = 0) int cantidad) {

        // Arrange
        VentasDynamoRepo repoMock = Mockito.mock(VentasDynamoRepo.class);
        RegistradorVentas registrador = new RegistradorVentas(repoMock);
        VentasHandler handler = new VentasHandler(registrador, new ObjectMapper());
        Context context = buildContext();

        String body = String.format(
                "{\"productos\":[{\"productoId\":\"prod-test-uuid\",\"nombre\":\"Prod\","
                + "\"cantidad\":%d,\"precioUnitario\":24.99}],\"total\":49.98}",
                cantidad
        );
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(body);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // Assert
        assertEquals(400, response.getStatusCode(),
                "Debe retornar HTTP 400 para cantidad=" + cantidad);
        assertEquals("{\"error\":\"La cantidad debe ser mayor a cero\"}", response.getBody(),
                "El mensaje de error debe ser exacto para cantidad=" + cantidad);

        // repositorio.guardar() nunca debe ser invocado
        verify(repoMock, never()).guardar(Mockito.any());
    }
}
