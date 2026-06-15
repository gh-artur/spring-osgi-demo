# springosgi

Spring Boot REST API com integração OSGi via Apache Felix embarcado.  
O endpoint `/products` consulta o registry OSGi por um `ProductProvider`; se nenhum bundle estiver instalado, retorna uma lista padrão hardcoded.

## Stack

- Java 17
- Spring Boot 4.1.0
- Spring MVC (`spring-boot-starter-webmvc`)
- Apache Felix 7.0.5 (framework OSGi embarcado)
- Maven (wrapper incluído)

## Running

```powershell
.\mvnw.cmd spring-boot:run
```

Servidor sobe em `http://localhost:8080`.  
O Felix inicia junto e carrega automaticamente todos os JARs presentes na pasta `bundles/`.

## Building

```powershell
.\mvnw.cmd package
```

## Testing

```powershell
.\mvnw.cmd test
```

## Endpoints

| Method | Path         | Descrição                                                        |
|--------|--------------|------------------------------------------------------------------|
| GET    | `/products`  | Retorna produtos via bundle OSGi, ou lista padrão se não houver bundle |
| GET    | `/customers` | Retorna lista fixa de clientes                                   |

## Estrutura do projeto

```
springosgi/
├── bundles/                              # Pasta monitorada: coloque JARs de bundles aqui
├── product-bundle/                       # Projeto Maven do bundle de exemplo
│   └── src/main/java/com/ghartur/springosgi/bundle/
│       ├── Activator.java                # Registra ProductProvider no registry OSGi
│       └── ProductProviderImpl.java      # Implementação alternativa com outros produtos
└── src/main/java/com/ghartur/springosgi/
    ├── SpringosgiApplication.java
    ├── osgi/
    │   └── OsgiFrameworkManager.java     # Sobe o Felix e carrega bundles de bundles/
    ├── product/
    │   ├── Product.java                  # record: id, name, description, price
    │   ├── ProductProvider.java          # Interface OSGi de serviço (contrato)
    │   ├── ProductService.java           # Lookup no registry OSGi + fallback padrão
    │   └── ProductController.java        # GET /products
    └── customer/
        ├── Customer.java                 # record: id, name, email
        └── CustomerController.java       # GET /customers
```

## Como usar o bundle OSGi

### 1. Instalar o app no repositório local Maven

O bundle depende das interfaces do app principal para compilar:

```powershell
.\mvnw.cmd install -DskipTests
```

### 2. Compilar o bundle

```powershell
cd product-bundle
mvn package
```

O JAR gerado fica em `product-bundle/target/springosgi-product-bundle-1.0.0.jar`.

### 3. Ativar o bundle

Copie o JAR para a pasta `bundles/` na raiz do projeto:

```powershell
Copy-Item product-bundle\target\springosgi-product-bundle-1.0.0.jar bundles\
```

### 4. Subir o app

```powershell
.\mvnw.cmd spring-boot:run
```

O Felix detecta o bundle na pasta `bundles/`, instala e inicia. O endpoint `/products` passa a retornar os dados do bundle em vez da lista padrão.

## Convenções

- O pacote `com.ghartur.springosgi.product` é exportado pelo Felix como system package — bundles importam esse pacote, não o empacotam.
- `ProductService` usa `ungetService` em bloco `finally` para liberar corretamente a referência OSGi.
- Bundles implementam `BundleActivator` para registrar/desregistrar o serviço no ciclo de vida OSGi.
