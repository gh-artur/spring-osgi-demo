# springosgi

Spring Boot REST API with embedded OSGi integration via Apache Felix.

The `/products` endpoint queries the OSGi service registry for a `ProductProvider`. If no bundle is installed, it falls back to a hardcoded default list. The Felix framework watches the `bundles/` directory at runtime — dropping or removing a JAR there installs or uninstalls the bundle automatically without restarting the app.

## Stack

| Component | Version |
|-----------|---------|
| Java | 17 |
| Spring Boot | 4.1.0 |
| Spring MVC | `spring-boot-starter-webmvc` |
| Apache Felix (embedded OSGi) | 7.0.5 |
| Build tool | Maven (wrapper included) |

## Running

```powershell
.\mvnw.cmd spring-boot:run
```

Server starts at `http://localhost:8080`. Felix starts alongside Spring and auto-loads any JARs present in the `bundles/` folder.

## Building

```powershell
.\mvnw.cmd package
```

The executable fat JAR is produced as `target/springosgi-0.0.1-SNAPSHOT-exec.jar`. The thin JAR (`springosgi-0.0.1-SNAPSHOT.jar`) stays as the Maven artifact so bundles can declare it as a compile dependency.

## Testing

```powershell
.\mvnw.cmd test
```

## API Endpoints

### Business endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/products` | Returns products from the active OSGi bundle, or a default list if no bundle is installed |
| `GET` | `/customers` | Returns a fixed list of customers |

### OSGi management endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/osgi/bundles` | Lists all installed bundles with their id, symbolic name, version, and state |
| `POST` | `/osgi/bundles/{id}/start` | Starts a bundle by id |
| `POST` | `/osgi/bundles/{id}/stop` | Stops a bundle by id |
| `DELETE` | `/osgi/bundles/{id}` | Uninstalls a bundle by id |

## Project Structure

```
springosgi/
├── bundles/                              # Hot-deploy folder — drop bundle JARs here
├── product-bundle/                       # Example bundle (separate Maven project)
│   └── src/main/java/com/ghartur/springosgi/bundle/
│       ├── Activator.java                # Registers/unregisters ProductProvider on bundle lifecycle
│       └── ProductProviderImpl.java      # Alternative product list provided by the bundle
└── src/main/java/com/ghartur/springosgi/
    ├── SpringosgiApplication.java
    ├── osgi/
    │   ├── OsgiFrameworkManager.java     # Starts Felix, watches bundles/ for hot-deploy
    │   ├── OsgiController.java           # REST endpoints for bundle management
    │   └── BundleInfo.java               # DTO for bundle state
    ├── product/
    │   ├── Product.java                  # record: id, name, description, price
    │   ├── ProductProvider.java          # OSGi service interface (the contract bundles implement)
    │   ├── ProductService.java           # Looks up ProductProvider from registry; falls back to defaults
    │   └── ProductController.java        # GET /products
    └── customer/
        ├── Customer.java                 # record: id, name, email
        └── CustomerController.java       # GET /customers
```

## Using the Example Bundle

### 1. Install the main app to the local Maven repository

The bundle depends on the main app's interfaces at compile time:

```powershell
.\mvnw.cmd install -DskipTests
```

### 2. Build the bundle

```powershell
cd product-bundle
mvn package
```

The JAR is generated at `product-bundle/target/springosgi-product-bundle-1.0.0.jar`.

### 3. Deploy the bundle

Copy the JAR into the `bundles/` folder at the project root:

```powershell
Copy-Item product-bundle\target\springosgi-product-bundle-1.0.0.jar bundles\
```

If the app is already running, Felix detects the new file automatically and installs and starts the bundle within milliseconds — no restart required. From that point on, `GET /products` returns the bundle's product list instead of the default one.

To undeploy, delete the JAR from `bundles/` and Felix uninstalls the bundle automatically.

### 4. Start the app (if not already running)

```powershell
.\mvnw.cmd spring-boot:run
```

## How it Works

```
HTTP Request
     │
     ▼
ProductController
     │  calls
     ▼
ProductService
     │  looks up ProductProvider in OSGi registry
     ├─ bundle installed? → delegate to ProductProvider.getProducts()
     └─ no bundle?        → return default hardcoded list
```

`OsgiFrameworkManager` boots Felix as an embedded framework within the Spring context (`@PostConstruct` / `@PreDestroy`). It exports `com.ghartur.springosgi.product` as a system package so bundles can import the `ProductProvider` interface without bundling it themselves. A background `WatchService` thread monitors `bundles/` for JAR additions and deletions and installs/uninstalls bundles on the fly.

## Conventions

- The package `com.ghartur.springosgi.product` is exported by Felix as a system package — bundles import it, they do not embed it.
- `ProductService` calls `ungetService` in a `finally` block to correctly release the OSGi service reference.
- Bundles implement `BundleActivator` to register and unregister their service on OSGi lifecycle events.
