package com.salesapi.productos.dto;

public class ProductoItem {

    private String id;
    private String codigoBarras;
    private String nombre;
    private double precio;
    private int stock;

    public ProductoItem() {}

    public ProductoItem(String id, String codigoBarras, String nombre, double precio, int stock) {
        this.id = id;
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}