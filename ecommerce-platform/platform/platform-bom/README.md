# platform-bom

BOM (Bill of Materials) Maven da plataforma. Centraliza toda versão de dependência — nenhum serviço declara a versão de nada que já esteja gerenciado aqui.

## Papel na arquitetura

Módulo `packaging=pom`, sem código-fonte (nenhum `src/main`, nenhum `src/test`) — puro `<dependencyManagement>`. Todo `pom.xml` de serviço importa este BOM logo no início e passa a poder declarar qualquer dependência gerenciada aqui **sem** `<version>`.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.ecommerce</groupId>
            <artifactId>platform-bom</artifactId>
            <version>${project.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## O que gerencia

- **`spring-boot-dependencies`** (importado primeiro, de propósito — todo o resto herda versões de driver Postgres, Flyway, Jackson, JUnit, Mockito, Micrometer, Lombok etc. já testadas em conjunto pelo time do Spring Boot).
- **`spring-cloud-dependencies`** (release train Northfields — ver nota de compatibilidade abaixo).
- **`spring-cloud-aws-dependencies`**.
- **`testcontainers-bom`**.
- **`resilience4j-bom`**.
- **`opentelemetry-bom`**.
- MapStruct (`mapstruct`, `mapstruct-processor`).
- `springdoc-openapi-starter-webmvc-ui`.
- JJWT (`jjwt-api` compile, `jjwt-impl`/`jjwt-jackson` runtime).
- `logstash-logback-encoder`.
- Todos os 7 módulos `platform-*` internos, na versão `${project.version}`.

## ⚠️ Nota de compatibilidade (aprendida por uma falha real)

O release train do Spring Cloud precisa casar com a versão do Spring Boot. Este projeto usa **Spring Cloud 2025.0.x ("Northfields")** com **Spring Boot 3.5.x** — a versão seguinte, **2025.1.x ("Oakwood")**, é para **Spring Boot 4.x**.

Isso não é teórico: durante o desenvolvimento, uma tentativa de usar 2025.1.x com Boot 3.5.x quebrou a subida de todo serviço com `NoClassDefFoundError: ConfigurableBootstrapContext` (a classe mudou de pacote no Boot 4). **Antes de subir `spring-cloud.version`, confira a [matriz de compatibilidade oficial](https://spring.io/projects/spring-cloud) contra a versão de `spring-boot.version` já fixada aqui.**

## Como usar

Nenhum serviço declara `<version>` para nada que este BOM gerencie — só `groupId`/`artifactId`/`scope`. Ver qualquer `pom.xml` em [`services/`](../../services/) como exemplo.

## Testes

Nenhum — módulo sem código, nada a testar.
