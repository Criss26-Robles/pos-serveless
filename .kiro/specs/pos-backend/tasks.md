# Tasks — Sistema POS Serverless

## Descripcion
Lista de tareas de implementacion derivadas del design.md, en orden de ejecucion.

---

## Fase 1: Infraestructura

### TASK-01: Crear template.yaml con AWS SAM
- Definir API Gateway con 4 endpoints
- Definir 4 funciones Lambda (ProductosLambda, CrearProductoLambda, VentasLambda, ConsultaVentasLambda)
- Definir 2 tablas DynamoDB (Productos, Ventas) con id como PK
- Configurar variables de entorno TABLA_PRODUCTOS y TABLA_VENTAS
- Estado: COMPLETADO

### TASK-02: Desplegar infraestructura con sam deploy
- Ejecutar sam build
- Ejecutar sam deploy --guided
- Verificar en consola AWS que existen las Lambdas, API Gateway y tablas DynamoDB
- Estado: COMPLETADO

---

## Fase 2: Backend — Lambda Productos

### TASK-03: Implementar ProductosDynamoRepo
- Metodo buscarPorCodigoBarras: busqueda exacta por campo detalle.codigoBarras
- Metodo buscarPorNombreParcial: busqueda contains en detalle.nombre (case insensitive)
- Metodo obtenerTodos: scan completo de la tabla
- Metodo crearProducto: PutItem con estructura id + detalle (Map nativo)
- Metodo actualizarStock: UpdateItem en detalle.stock
- Estado: COMPLETADO

### TASK-04: Implementar BuscadorProductos
- Detectar si el query es numerico (codigo de barras) o texto (nombre)
- Delegar a ProductosDynamoRepo segun el tipo
- Estado: COMPLETADO

### TASK-05: Implementar ProductosHandler
- Recibir evento de API Gateway
- Extraer query param q
- Llamar a BuscadorProductos
- Retornar respuesta JSON con CORS headers
- Estado: COMPLETADO

### TASK-06: Implementar CrearProductoHandler
- Recibir body con nombre, codigo_barras, precio, stock
- Validar campos obligatorios
- Guardar en DynamoDB con estructura id + detalle
- Estado: COMPLETADO

---

## Fase 3: Backend — Lambda Ventas

### TASK-07: Implementar VentasDynamoRepo
- Metodo guardar: PutItem con estructura id + detalle (Map nativo)
- detalle contiene: productos (List nativa), total, metodoPago, fecha
- Metodo descontarStock: UpdateItem en detalle.stock de tabla Productos
- Metodo obtenerTodas: Scan de tabla Ventas
- Estado: COMPLETADO

### TASK-08: Implementar RegistradorVentas
- Generar UUID para la venta
- Guardar venta en DynamoDB
- Descontar stock de cada producto vendido
- Estado: COMPLETADO

### TASK-09: Implementar VentasHandler
- Recibir body con productos, total, metodoPago
- Validar que haya productos
- Llamar a RegistradorVentas
- Retornar respuesta JSON
- Estado: COMPLETADO

### TASK-10: Implementar ConsultaVentasHandler
- Llamar a VentasDynamoRepo.obtenerTodas()
- Retornar lista de ventas como JSON
- Estado: COMPLETADO

---

## Fase 4: Migracion NoSQL

### TASK-11: Migrar tabla Productos a estructura id + detalle
- Reescribir ProductosDynamoRepo para usar Map nativo en DynamoDB
- Migrar productos existentes con put-item usando nueva estructura
- Estado: COMPLETADO

### TASK-12: Migrar tabla Ventas a estructura id + detalle
- Reescribir VentasDynamoRepo para usar Map y List nativos de DynamoDB
- Estado: COMPLETADO

---

## Fase 5: Permisos IAM

### TASK-13: Configurar permisos minimos por Lambda
- ProductosLambda: dynamodb:Scan, dynamodb:GetItem en tabla Productos
- CrearProductoLambda: dynamodb:PutItem en tabla Productos
- VentasLambda: dynamodb:PutItem en Ventas, dynamodb:UpdateItem en Productos
- ConsultaVentasLambda: dynamodb:Scan en tabla Ventas
- Estado: COMPLETADO

---

## Fase 6: Pruebas

### TASK-14: Escribir pruebas unitarias para ProductosHandler
- [x] Caso exitoso: busqueda por codigo de barras
- [x] Caso exitoso: busqueda por nombre
- [x] Caso: tabla vacia
- [x] Caso: error de conexion DynamoDB (mock)

### TASK-15: Escribir pruebas unitarias para VentasHandler
- [x] Caso exitoso: venta con productos validos
- [x] Caso: body vacio
- [x] Caso: lista de productos vacia
- [ ] Caso: error de conexion DynamoDB (mock)

---

## Fase 7: Documentacion y GitHub

### TASK-16: Crear README.md
- [x] Descripcion de arquitectura
- [x] Instrucciones de despliegue (sam build, sam deploy)
- [x] URL del API Gateway
- [x] Capturas de Postman
- [x] Seccion SDD

### TASK-17: Subir repositorio a GitHub
- [x] Verificar .gitignore (no subir .env ni credenciales)
- [x] Subir codigo con specs en .kiro/specs/pos-backend/
