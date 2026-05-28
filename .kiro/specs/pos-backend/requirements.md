# Requirements — Sistema POS Serverless

## Descripcion General
Sistema de Punto de Venta (POS) serverless construido sobre AWS. Permite buscar productos por codigo de barras o nombre, gestionar un carrito de compras y registrar ventas con descuento automatico de stock.

---

## Requisitos Funcionales

### RF-01: Buscar productos
- El sistema debe permitir buscar productos por codigo de barras (valor numerico exacto)
- El sistema debe permitir buscar productos por nombre (busqueda parcial, insensible a mayusculas)
- Si el input es completamente numerico, se asume codigo de barras
- Si el input contiene letras, se asume nombre parcial
- Response exitoso: lista de productos con id, nombre, codigoBarras, precio, stock

### RF-02: Registrar venta
- El sistema debe registrar una venta con uno o mas productos
- Cada producto en la venta debe incluir: productId, productName, productPrice, cantidad
- La venta debe incluir: total, metodoPago, fecha
- Al registrar la venta, el stock de cada producto debe descontarse automaticamente
- Response exitoso: ventaId, productos, total, metodoPago, fecha

### RF-03: Estructura NoSQL correcta
- La tabla Productos debe tener solo dos atributos: id (PK) y detalle (Map)
- La tabla Ventas debe tener solo dos atributos: id (PK) y detalle (Map)
- El campo detalle en Ventas debe contener un array nativo de productos (List), no un string JSON

### RF-04: Crear producto
- El sistema debe permitir crear nuevos productos
- Campos requeridos: nombre, codigo_barras, precio, stock
- El producto se guarda con estructura id + detalle en DynamoDB

---

## Requisitos No Funcionales

### RNF-01: Serverless
- Toda la logica de negocio debe ejecutarse en AWS Lambda
- No se permite administrar servidores
- Cada Lambda ejecuta una sola funcion

### RNF-02: Escalabilidad
- La infraestructura debe escalar automaticamente segun la demanda
- DynamoDB y Lambda escalan sin configuracion adicional

### RNF-03: Latencia
- El tiempo de respuesta de cada Lambda no debe superar 30 segundos (timeout configurado)
- En condiciones normales la respuesta debe ser menor a 3 segundos

### RNF-04: Seguridad
- Cada Lambda debe tener permisos IAM minimos necesarios (principio de minimo privilegio)
- No se deben exponer credenciales AWS en el codigo ni en el repositorio

---

## Criterios de Aceptacion (Casos de Prueba)

### CA-01: GET /productos con codigo de barras
- Input: q=750123456789 (numerico)
- Expected: array con el producto cuyo codigoBarras sea 750123456789
- HTTP: 200

### CA-02: GET /productos con nombre parcial
- Input: q=leche
- Expected: array con todos los productos cuyo nombre contenga "leche" (case insensitive)
- HTTP: 200

### CA-03: GET /productos tabla vacia
- Input: q=productoinexistente
- Expected: array vacio []
- HTTP: 200

### CA-04: GET /productos sin parametro
- Input: sin q
- Expected: todos los productos de la tabla
- HTTP: 200

### CA-05: POST /ventas exitoso
- Input: { productos: [{productId, productName, productPrice, cantidad}], total, metodoPago }
- Expected: { ventaId, productos, total, metodoPago, fecha }
- HTTP: 200
- Stock descontado en DynamoDB

### CA-06: POST /ventas sin productos
- Input: { productos: [], total: 0 }
- Expected: { error: "No hay productos en la venta" }
- HTTP: 400

### CA-07: POST /ventas body invalido
- Input: body vacio o malformado
- Expected: { error: "Cuerpo de la peticion invalido" }
- HTTP: 400

### CA-08: Error de conexion DynamoDB
- Simulado con mock que lanza DynamoDbException
- Expected: { error: "Error interno del servidor" }
- HTTP: 500