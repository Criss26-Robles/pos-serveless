# sales-api-serverless

API REST serverless para un sistema de Punto de Venta (POS), construida sobre AWS Lambda, API Gateway y DynamoDB, desplegada con AWS SAM.

---

## Descripción de Arquitectura

```
Frontend (Node.js + Express)
        |
        | HTTP/JSON
        v
API Gateway (AWS)
https://zd536se6l8.execute-api.us-east-1.amazonaws.com/Prod
        |
        |---> GET  /productos --> ProductosLambda      --> DynamoDB (TablaProductos)
        |---> POST /productos --> CrearProductoLambda  --> DynamoDB (TablaProductos)
        |---> POST /ventas    --> VentasLambda         --> DynamoDB (TablaVentas + TablaProductos)
        |---> GET  /ventas    --> ConsultaVentasLambda --> DynamoDB (TablaVentas)
```

### Servicios AWS utilizados

| Servicio | Rol |
|---|---|
| **API Gateway** | Expone los endpoints HTTP públicos y enruta las peticiones a las Lambdas correspondientes |
| **AWS Lambda (Java 21)** | Ejecuta la lógica de negocio de forma serverless; una función por operación |
| **DynamoDB** | Base de datos NoSQL administrada con escalado automático |
| **IAM** | Permisos mínimos por rol de Lambda (principio de mínimo privilegio) |
| **AWS SAM** | Infraestructura como código para definir y desplegar todos los recursos |

### Funciones Lambda

| Lambda | Método | Ruta | Descripción |
|---|---|---|---|
| `ProductosLambda` | GET | `/productos` | Busca productos por código de barras, nombre parcial, o retorna todos |
| `CrearProductoLambda` | POST | `/productos` | Crea un nuevo producto en el catálogo |
| `VentasLambda` | POST | `/ventas` | Registra una venta y descuenta el stock automáticamente |
| `ConsultaVentasLambda` | GET | `/ventas` | Lista todas las ventas registradas |

### Tablas DynamoDB

Ambas tablas siguen una estructura NoSQL de dos columnas: clave primaria + mapa de atributos.

**TablaProductos**

| Atributo | Tipo | Descripción |
|---|---|---|
| `id` | String (PK) | Identificador único del producto |
| `detalle` | Map | `{ nombre, codigoBarras, precio, stock }` |

**TablaVentas**

| Atributo | Tipo | Descripción |
|---|---|---|
| `ventaId` | String (PK) | UUID de la venta |
| `detalle` | Map | `{ productos (List), total, metodoPago, fecha }` |

### Estructura del proyecto

```
sales-api-serverless/
├── template.yaml                  # Definición SAM (Lambdas, API Gateway, DynamoDB)
├── samconfig.toml                 # Configuración de despliegue (región, stack name)
├── productos-lambda/              # Módulo Maven — Lambdas de productos
│   └── src/main/java/com/salesapi/productos/
│       ├── ProductosHandler.java
│       ├── CrearProductoHandler.java
│       ├── BuscadorProductos.java
│       ├── ProductosDynamoRepo.java
│       └── dto/ProductoItem.java
└── ventas-lambda/                 # Módulo Maven — Lambdas de ventas
    └── src/main/java/com/salesapi/ventas/
        ├── VentasHandler.java
        ├── ConsultaVentasHandler.java
        ├── RegistradorVentas.java
        ├── VentasDynamoRepo.java
        └── dto/
            ├── VentaRequest.java
            └── VentaResponse.java
```

---

## Instrucciones de Despliegue

### Prerrequisitos

- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html) configurado con credenciales válidas
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html) instalado
- Java 21 y Maven instalados

### 1. Compilar el proyecto

```bash
cd sales-api-serverless
sam build
```

Este comando compila los módulos Maven de `productos-lambda` y `ventas-lambda`, y empaqueta los artefactos para su despliegue.

### 2. Desplegar en AWS

**Primera vez (modo guiado):**

```bash
sam deploy --guided
```

SAM solicitará los parámetros de configuración. Los valores recomendados:

```
Stack Name:        sales-api-serverless
AWS Region:        us-east-1
Confirm changes:   y
Allow SAM CLI IAM role creation: y
Disable rollback:  n
Save arguments to samconfig.toml: y
```

**Despliegues posteriores** (usa la configuración guardada en `samconfig.toml`):

```bash
sam deploy
```

### 3. Verificar el despliegue

Al finalizar, SAM muestra los outputs del stack. La URL del API Gateway aparece en la sección `Outputs`:

```
Key   ApiUrl
Value https://zd536se6l8.execute-api.us-east-1.amazonaws.com/Prod
```

También puedes verificar en la consola AWS que existen:
- 4 funciones Lambda: `ProductosLambda`, `CrearProductoLambda`, `VentasLambda`, `ConsultaVentasLambda`
- 2 tablas DynamoDB: `TablaProductos`, `TablaVentas`
- 1 API Gateway con las rutas configuradas

### 4. Eliminar el stack (opcional)

```bash
sam delete
```

---

## URL del API Gateway

```
https://zd536se6l8.execute-api.us-east-1.amazonaws.com/Prod
```

---

## Ejemplos de Endpoints (curl)

### GET /productos — Obtener todos los productos

```bash
curl -X GET \
  "https://zd536se6l8.execute-api.us-east-1.amazonaws.com/Prod/productos"
```

**Respuesta 200:**
```json
[
  {
    "id": "1",
    "nombre": "Leche Entera 1L",
    "codigoBarras": "750123456789",
    "precio": 25.5,
    "stock": 10
  },
  {
    "id": "2",
    "nombre": "Pan Integral",
    "codigoBarras": "750987654321",
    "precio": 18.0,
    "stock": 25
  }
]
```

---

### GET /productos?q={codigo_barras} — Buscar por código de barras

Si el parámetro `q` es completamente numérico, se realiza una búsqueda exacta por código de barras.

```bash
curl -X GET \
  "https://zd536se6l8.execute-api.us-east-1.amazonaws.com/Prod/productos?q=750123456789"
```

**Respuesta 200:**
```json
[
  {
    "id": "1",
    "nombre": "Leche Entera 1L",
    "codigoBarras": "750123456789",
    "precio": 25.5,
    "stock": 10
  }
]
```

---

### GET /productos?q={nombre} — Buscar por nombre parcial

Si el parámetro `q` contiene letras, se realiza una búsqueda parcial e insensible a mayúsculas en el nombre.

```bash
curl -X GET \
  "https://zd536se6l8.execute-api.us-east-1.amazonaws.com/Prod/productos?q=leche"
```

**Respuesta 200:**
```json
[
  {
    "id": "1",
    "nombre": "Leche Entera 1L",
    "codigoBarras": "750123456789",
    "precio": 25.5,
    "stock": 10
  }
]
```

---

### POST /productos — Crear un producto

```bash
curl -X POST \
  "https://zd536se6l8.execute-api.us-east-1.amazonaws.com/Prod/productos" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Agua Mineral 500ml",
    "codigo_barras": "750111222333",
    "precio": 12.0,
    "stock": 50
  }'
```

**Respuesta 201:**
```json
{
  "id": "3",
  "detalle": {
    "nombre": "Agua Mineral 500ml",
    "codigoBarras": "750111222333",
    "precio": 12.0,
    "stock": 50
  }
}
```

**Respuesta 400 (campos faltantes):**
```json
{
  "error": "Los campos nombre, codigo_barras y precio son obligatorios"
}
```

---

### POST /ventas — Registrar una venta

Al registrar la venta, el stock de cada producto se descuenta automáticamente en DynamoDB.

```bash
curl -X POST \
  "https://zd536se6l8.execute-api.us-east-1.amazonaws.com/Prod/ventas" \
  -H "Content-Type: application/json" \
  -d '{
    "productos": [
      {
        "productoId": "1",
        "nombre": "Leche Entera 1L",
        "cantidad": 2,
        "precioUnitario": 25.5
      },
      {
        "productoId": "2",
        "nombre": "Pan Integral",
        "cantidad": 1,
        "precioUnitario": 18.0
      }
    ],
    "total": 69.0,
    "metodoPago": "efectivo"
  }'
```

**Respuesta 200:**
```json
{
  "ventaId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "productos": [
    {
      "productoId": "1",
      "nombre": "Leche Entera 1L",
      "cantidad": 2,
      "precioUnitario": 25.5
    },
    {
      "productoId": "2",
      "nombre": "Pan Integral",
      "cantidad": 1,
      "precioUnitario": 18.0
    }
  ],
  "total": 69.0,
  "metodoPago": "efectivo",
  "fecha": "2026-05-27T22:25:30Z"
}
```

**Respuesta 400 (lista de productos vacía):**
```json
{
  "error": "No hay productos en la venta"
}
```

**Respuesta 400 (body inválido):**
```json
{
  "error": "Cuerpo de la peticion invalido"
}
```

---

### GET /ventas — Listar todas las ventas

```bash
curl -X GET \
  "https://zd536se6l8.execute-api.us-east-1.amazonaws.com/Prod/ventas"
```

**Respuesta 200:**
```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "total": 69.0,
    "fecha": "2026-05-27T22:25:30Z",
    "metodoPago": "efectivo",
    "productos": [
      {
        "productoId": "1",
        "nombre": "Leche Entera 1L",
        "cantidad": 2,
        "precioUnitario": 25.5
      }
    ]
  }
]
```

---

## Spec Driven Development (SDD)

Este proyecto fue construido siguiendo la metodología **Spec Driven Development**: primero se definen los requisitos y el diseño, luego se generan las tareas de implementación, y finalmente se validan las propiedades de corrección mediante pruebas basadas en propiedades.

### Archivos de especificación

Los archivos de spec se encuentran en `.kiro/specs/pos-backend/`:

| Archivo | Contenido |
|---|---|
| `requirements.md` | Requisitos funcionales (RF-01 a RF-04) y no funcionales (RNF-01 a RNF-04), con criterios de aceptación (CA-01 a CA-08) |
| `design.md` | Arquitectura del sistema, contratos de endpoints, estructura de tablas DynamoDB y organización del código |
| `tasks.md` | Lista de tareas de implementación derivadas del diseño, organizadas en fases |

### Pruebas basadas en propiedades (Property-Based Testing)

Las pruebas de corrección se implementan con **[jqwik](https://jqwik.net/)**, un framework de property-based testing para Java.

Las propiedades validadas cubren los requisitos críticos del sistema:

| Propiedad | Requisito | Descripción |
|---|---|---|
| **Enrutamiento de búsqueda** | RF-01 | Un input completamente numérico siempre se enruta como búsqueda por código de barras; un input con letras siempre se enruta como búsqueda por nombre parcial |
| **Registro de venta con descuento de stock** | RF-02 | Al registrar una venta, el stock de cada producto disminuye exactamente en la cantidad vendida |
| **Reglas de validación** | RF-02 | Una venta con lista de productos vacía siempre retorna error 400; un body malformado siempre retorna error 400 |

Estas pruebas generan cientos de casos de entrada aleatorios para verificar que las propiedades se cumplen universalmente, complementando los tests unitarios con ejemplos específicos.

---

## Tecnologías

- **Java 21** — Runtime de las funciones Lambda
- **AWS SAM** — Infraestructura como código y despliegue
- **AWS Lambda** — Cómputo serverless
- **Amazon API Gateway** — Exposición de endpoints HTTP
- **Amazon DynamoDB** — Base de datos NoSQL
- **Maven** — Gestión de dependencias y build
- **jqwik** — Property-based testing
- **JUnit 5** — Pruebas unitarias
