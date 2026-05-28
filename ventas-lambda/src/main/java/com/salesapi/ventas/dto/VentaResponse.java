package com.salesapi.ventas.dto;

import java.math.BigDecimal;
import java.util.List;

public class VentaResponse {
    private String ventaId;
    private List<VentaRequest.ProductoVenta> productos;
    private BigDecimal total;
    private String metodoPago;
    private String fecha;

    public VentaResponse() {}

    public VentaResponse(String ventaId, List<VentaRequest.ProductoVenta> productos, BigDecimal total, String metodoPago, String fecha) {
        this.ventaId = ventaId;
        this.productos = productos;
        this.total = total;
        this.metodoPago = metodoPago;
        this.fecha = fecha;
    }

    public String getVentaId() { return ventaId; }
    public void setVentaId(String ventaId) { this.ventaId = ventaId; }

    public List<VentaRequest.ProductoVenta> getProductos() { return productos; }
    public void setProductos(List<VentaRequest.ProductoVenta> productos) { this.productos = productos; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
}