# Design — Sistema POS Serverless

## Arquitectura General

Navegador/Frontend (Node.js + Express)
  |
  | HTTP/JSON
  v
API Gateway (AWS)
https://zd536se6l8.execute-api.us-east-1.amazonaws.com/Prod
  |
  |---> GET  /productos --> ProductosLambda -------> DynamoDB (Tabla: Productos)
  |---> POST /productos --> CrearProductoLambda ---> DynamoDB (Tabla: Productos)
  |---> POST /ventas    --> VentasLambda ----------> DynamoDB (Tabla: Ventas + Productos)
  |---> GET  /ventas    --> ConsultaVentasLambda --> DynamoDB (Tabla: Ventas)

---

## Servicios AWS Utilizados

| Servicio | Uso |
|---|---|
| API Gateway | Expone endpoints HTTP publicos, enruta requests a Lambdas |
| AWS Lambda (Java 21) | Logica de negocio serverless, una funcion por Lambda |
| DynamoDB | Base de datos NoSQL administrada |
| IAM | Permisos minimos por rol de Lambda |
| AWS SAM | Despliegue de infraestructura como codigo |

---

## Estructura de Tablas DynamoDB

### Tabla: Productos
Solo 2 columnas (NoSQL correcto)

| Atributo | Tipo | Descripcion |
|---|---|---|
| id | String (PK) | Identificador unico del producto |
| detalle | Map | JSON con nombre, codigoBarras, precio, stock |

Ejemplo:
  id: 1
  detalle:
    nombre: Leche Entera 1L
    codigoBarras: 750123456789
    precio: 25.5
    stock: 10

### Tabla: Ventas
Solo 2 columnas (NoSQL correcto)

| Atributo | Tipo | Descripcion |
|---|---|---|
| id | String (PK) | UUID de la venta |
| detalle | Map | JSON con productos (List), total, metodoPago, fecha |

Ejemplo:
  id: uuid-venta
  detalle:
    productos:
      - productId: 1
        productName: Leche Entera 1L
        productPrice: 25.5
        cantidad: 2
    total: 51.0
    metodoPago: efectivo
    fecha: 2026-05-27T22:25:30Z

---

## Contratos de Endpoints

### GET /productos
- Query param: q (opcional)
- Si q es numerico: busca por codigoBarras exacto
- Si q tiene letras: busca por nombre parcial (case insensitive)
- Sin q: retorna todos los productos
- Response 200: [{id, nombre, codigoBarras, precio, stock}]

### POST /productos
- Body: {nombre, codigo_barras, precio, stock}
- Response 201: {id, detalle: {nombre, codigoBarras, precio, stock}}
- Response 400: {error: Los campos nombre, codigo_barras y precio son obligatorios}

### POST /ventas
- Body: {productos: [{productoId, nombre, cantidad, precioUnitario}], total, metodoPago, fecha}
- Response 200: {ventaId, productos, total, metodoPago, fecha}
- Response 400: {error: No hay productos en la venta}
- Response 400: {error: Cuerpo de la peticion invalido}

### GET /ventas
- Response 200: [{id, total, fecha, metodoPago, productos}]

---

## Estructura del Proyecto Backend

sales-api-serverless/
  template.yaml
  productos-lambda/
    src/main/java/com/salesapi/productos/
      ProductosHandler.java
      CrearProductoHandler.java
      BuscadorProductos.java
      ProductosDynamoRepo.java
      dto/ProductoItem.java
  ventas-lambda/
    src/main/java/com/salesapi/ventas/
      VentasHandler.java
      ConsultaVentasHandler.java
      RegistradorVentas.java
      VentasDynamoRepo.java
      dto/VentaRequest.java
      dto/VentaResponse.java
