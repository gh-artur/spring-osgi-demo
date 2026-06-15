# springosgi

Spring Boot REST API com integração OSGi via Apache Felix embarcado.  
O endpoint `/products` consulta o registry OSGi por um `ProductProvider`; se nenhum bundle estiver instalado, retorna uma lista padrão hardcoded. O Felix monitora a pasta `bundles/` em runtime via `WatchService` — dropar ou remover um JAR instala/desinstala o bundle sem reiniciar o app.

## Stack

- Java 17
- Spring Boot 4.1.0
- Spring MVC (`spring-boot-starter-webmvc`)
- Apache Felix 7.0.5 (framework OSGi embarcado)
- Maven (wrapper incluído)
- `maven-bundle-plugin` 5.1.9 (usado no `product-bundle` para gerar OSGi-compliant JAR)

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

O fat JAR executável fica em `target/springosgi-0.0.1-SNAPSHOT-exec.jar` (classifier `exec`).  
O thin JAR `target/springosgi-0.0.1-SNAPSHOT.jar` permanece como artefato Maven para que bundles possam depender dele com `<scope>provided</scope>`.

## Testing

```powershell
.\mvnw.cmd test
```

## Endpoints

### Negócio

| Method | Path         | Descrição                                                              |
|--------|--------------|------------------------------------------------------------------------|
| GET    | `/products`  | Retorna produtos via bundle OSGi ativo, ou lista padrão se não houver bundle |
| GET    | `/customers` | Retorna lista fixa de 3 clientes                                       |

### Gerenciamento OSGi

| Method | Path                        | Descrição                                              |
|--------|-----------------------------|--------------------------------------------------------|
| GET    | `/osgi/bundles`             | Lista todos os bundles instalados (id, nome, versão, estado) |
| POST   | `/osgi/bundles/{id}/start`  | Inicia um bundle pelo id                               |
| POST   | `/osgi/bundles/{id}/stop`   | Para um bundle pelo id                                 |
| DELETE | `/osgi/bundles/{id}`        | Desinstala um bundle pelo id                           |

## Estrutura do projeto

```
springosgi/
├── bundles/                              # Pasta monitorada: coloque JARs de bundles aqui
├── product-bundle/                       # Projeto Maven do bundle de exemplo (packaging=bundle)
│   ├── pom.xml                           # Usa maven-bundle-plugin; dependência do app é provided
│   └── src/main/java/com/ghartur/springosgi/bundle/
│       ├── Activator.java                # Registra/desregistra ProductProvider no ciclo de vida OSGi
│       └── ProductProviderImpl.java      # Retorna 6 produtos alternativos (Monitor, Headset, etc.)
└── src/main/java/com/ghartur/springosgi/
    ├── SpringosgiApplication.java
    ├── osgi/
    │   ├── OsgiFrameworkManager.java     # Sobe o Felix, carrega bundles/, inicia WatchService em thread daemon
    │   ├── OsgiController.java           # REST para gerenciar bundles em runtime
    │   └── BundleInfo.java               # record DTO: id, symbolicName, version, state (label textual)
    ├── product/
    │   ├── Product.java                  # record: id, name, description, price
    │   ├── ProductProvider.java          # Interface OSGi de serviço (contrato que bundles implementam)
    │   ├── ProductService.java           # Lookup no registry + fallback para DEFAULT_PRODUCTS
    │   └── ProductController.java        # GET /products
    └── customer/
        ├── Customer.java                 # record: id, name, email
        └── CustomerController.java       # GET /customers (lista hardcoded)
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

### 3. Ativar o bundle (hot-deploy)

Copie o JAR para a pasta `bundles/` na raiz do projeto:

```powershell
Copy-Item product-bundle\target\springosgi-product-bundle-1.0.0.jar bundles\
```

Se o app já estiver rodando, o Felix detecta o arquivo novo e instala/inicia o bundle automaticamente (~200 ms de debounce). A partir disso, `/products` retorna os dados do bundle.  
Para remover, basta apagar o JAR de `bundles/`.

### 4. Subir o app (se não estiver rodando)

```powershell
.\mvnw.cmd spring-boot:run
```

## Decisões técnicas relevantes

- **`classifier=exec` no fat JAR**: evita que o fat JAR (que contém todas as deps embarcadas) seja resolvido como dependência por outros projetos Maven. O artefato primário é o thin JAR.
- **System package export**: `com.ghartur.springosgi.product` é exportado pelo Felix via `FRAMEWORK_SYSTEMPACKAGES_EXTRA` — bundles importam esse pacote, não o empacotam. Isso garante que `Product` e `ProductProvider` são as mesmas classes em ambos os classloaders.
- **`ungetService` em `finally`**: `ProductService` sempre libera a referência OSGi mesmo em caso de exceção, evitando memory leak no registry.
- **WatchService em thread daemon**: a thread de monitoramento é daemon para não bloquear o shutdown do JVM. Interrupção limpa é tratada no `@PreDestroy`.
- **Felix cache em `target/`**: `FRAMEWORK_STORAGE=target/felix-cache` com `FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT` — cache limpo a cada primeiro start, dentro do diretório de build (ignorado pelo git).

## Convenções

- Bundles implementam `BundleActivator` para registrar/desregistrar o serviço no ciclo de vida OSGi.
- `OsgiController` usa o `BundleContext` diretamente via `OsgiFrameworkManager.getBundleContext()`.
- `BundleInfo` converte o estado inteiro do OSGi (`Bundle.getState()`) para label textual via `switch`.
