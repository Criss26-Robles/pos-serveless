package com.salesapi.ventas.dto;

import java.math.BigDecimal;
import java.util.List;

public class VentaRequest {

    private List<ProductoVenta> productos;
    private BigDecimal total;
    private String metodoPago;
    private String fecha;

    public VentaRequest() {}

    public List<ProductoVenta> getProductos() { return productos; }
    public void setProductos(List<ProductoVenta> productos) { this.productos = productos; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public static class ProductoVenta {
        private String productoId;
        private String nombre;
        private Integer cantidad;
        private BigDecimal precioUnitario;

        public ProductoVenta() {}

        public String getProductoId() { return productoId; }
        public void setProductoId(String productoId) { this.productoId = productoId; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

        public BigDecimal getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    }
}