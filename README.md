# springboot-encrypt-starter

面向 Spring Boot 2.7 和 JDK 17 的接口请求解密、响应加密 starter。它基于 Servlet Filter 工作，适合普通 JSON API 在进入 Spring MVC 参数绑定前解密请求体，并在响应写出前统一加密响应体。

## 版本定位

- Java: 17
- Spring Boot: 2.7.18
- Servlet API: `javax.servlet`
- Maven 坐标: `com.unknown:springboot-encrypt-starter:0.1-SNAPSHOT`

Spring Boot 2.7 仍使用 `javax.servlet`。不要在 Boot 2.7 项目里切到 `jakarta.servlet`，那是 Spring Boot 3.x 的 API 体系。

## 是否应该用 Filter

当前实现使用 `FilterRegistrationBean<EncryptionFilter>` 注册 Servlet Filter。这不是所有场景下的唯一最佳方案，但对“请求体整体是密文，Controller 仍按普通 `@RequestBody` 接收明文对象”这种需求是直接有效的方案。

它的优点：

- 在 Spring MVC 读取 body 前完成解密，不需要改 Controller 参数绑定方式。
- 在 Controller 写出响应后统一加密，业务代码不需要关心响应加密。
- 能覆盖注解和配置指定的接口，也能通过 ignore 列表排除接口。

它的边界：

- 响应会被缓冲到内存后再加密，不适合大文件下载、流式响应、SSE、长连接响应。
- 不应该处理二进制响应、文件上传下载、Actuator、Swagger、静态资源等端点。
- 对只处理 JSON body 的 Spring MVC 应用，可以考虑用 `RequestBodyAdvice` 和 `ResponseBodyAdvice`，它们更贴近 Spring MVC 扩展点；但它们不如 Filter 适合处理“整个 HTTP body 是密文”的通用场景。

这个 starter 的定位是普通 JSON API 加解密，不是网关加密、传输层安全或文件流加密方案。

## 安装

本地安装：

```bash
mvn -DskipTests install
```

业务项目引用：

```xml
<dependency>
    <groupId>com.unknown</groupId>
    <artifactId>springboot-encrypt-starter</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

Spring Boot 2.7 会通过 `spring.factories` 自动加载配置。`@EnableEncrypt` 仍保留兼容，但新项目不需要显式添加。

## 基础配置

```yaml
spring:
  encrypt:
    enabled: true
    default-mode: explicit
    key: d7b85f6e214abcdad7b85f6e214abcda
    debug: false
    response-charset: UTF-8
    url-patterns:
      - /*
    order: 1
    encrypt-error-responses: false
    skip-attachment-responses: true
    skip-response-content-types:
      - application/octet-stream
      - text/event-stream
```

配置说明：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.encrypt.enabled` | `true` | 是否注册加解密过滤器 |
| `spring.encrypt.default-mode` | `explicit` | 空规则时的默认行为，支持 `explicit`、`all` |
| `spring.encrypt.key` | `d7b85f6e214abcdad7b85f6e214abcda` | AES key，当前默认算法按 Hex 字符串处理 |
| `spring.encrypt.debug` | `false` | 调试模式，开启后不执行加解密 |
| `spring.encrypt.response-charset` | `UTF-8` | 响应加密前后的字符集 |
| `spring.encrypt.url-patterns` | `/*` | Filter 拦截路径 |
| `spring.encrypt.order` | `1` | Filter 顺序 |
| `spring.encrypt.encrypt-error-responses` | `false` | 是否加密 HTTP 4xx/5xx 响应 |
| `spring.encrypt.skip-attachment-responses` | `true` | 是否跳过文件下载响应 |
| `spring.encrypt.skip-response-content-types` | `application/octet-stream`, `text/event-stream` | 不加密的响应类型前缀 |

生产环境不要把密钥写死在仓库配置里，应该通过外部配置或环境变量注入。

## 默认行为

默认行为必须明确：`default-mode` 默认为 `explicit`。没有配置任何 `request-decrypt-uri-list` 和 `response-encrypt-uri-list`，也没有通过注解收集到任何加解密 URI 时，starter 不会处理请求或响应。

如果整个 API 服务都走加密协议，可以改成 `all`：

```yaml
spring:
  encrypt:
    default-mode: all
```

`all` 模式会在没有任何显式加解密 URI 时默认请求解密、响应加密。混合场景建议继续使用 `explicit`，或者至少配置 ignore 列表排除这些端点：

```yaml
spring:
  encrypt:
    response-encrypt-uri-ignore-list:
      - /actuator/**
      - /swagger-ui/**
      - /v3/api-docs/**
      - /static/**
    request-decrypt-uri-ignore-list:
      - /actuator/**
      - /swagger-ui/**
      - /v3/api-docs/**
      - /static/**
```

匹配决策顺序：

1. 先判断接口是否命中请求解密列表或响应加密列表。
2. 如果两个列表都为空，并且 `default-mode=all`，则默认请求解密和响应加密都开启。
3. 再判断 ignore 列表，ignore 命中后会关闭对应方向的处理。
4. 最终只有需要处理的方向才会包装 request 或 response。

## 注解用法

响应加密：

```java
@Encrypt
@GetMapping("/api/user")
public UserDto user() {
    return userService.current();
}
```

请求解密：

```java
@Decrypt
@PostMapping("/api/user/update")
public Result update(@RequestBody UserDto request) {
    return userService.update(request);
}
```

忽略加密或解密：

```java
@EncryptIgnore
@DecryptIgnore
@GetMapping("/api/ping")
public String ping() {
    return "ok";
}
```

也可以直接指定 URI。注解里显式写 URI 时，显式值优先于 Controller mapping 推导：

```java
@Encrypt("get:/api/user/{id}")
@GetMapping("/api/user/{id}")
public UserDto user(@PathVariable Long id) {
    return userService.get(id);
}
```

## GET 和 POST 规则

注解不按“所有 GET 默认加密、所有 POST 默认解密”这种方式工作。规则是：

- `@Encrypt` 表示当前接口响应需要加密，和 GET/POST 本身无关。
- `@Decrypt` 表示当前接口请求需要解密，和 GET/POST 本身无关。
- 对非 GET 请求，`@Decrypt` 会尝试解密 request body。
- 对 GET 请求，`@Decrypt` 不会解密 body，只会在配置了 query 参数解密时处理指定参数。
- `@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping` 会被推导成 `get:`、`post:`、`put:`、`delete:` 前缀。
- 普通 `@RequestMapping` 如果没有声明 method，当前实现按 `GET` 推导。

示例：

```java
@Encrypt
@GetMapping("/api/user")
```

推导结果是响应加密 URI：`get:/api/user`。

```java
@Decrypt
@PostMapping("/api/order")
```

推导结果是请求解密 URI：`post:/api/order`，并解密 body。

```java
@Decrypt(decryptParam = "keyword")
@GetMapping("/api/search")
```

推导结果是请求解密 URI：`get:/api/search`，只解密 query 参数 `keyword`，不处理 body。

## URI 配置

不使用注解时，可以直接配置 URI：

```yaml
spring:
  encrypt:
    response-encrypt-uri-list:
      - get:/api/user/*
      - post:/api/order/create
    request-decrypt-uri-list:
      - post:/api/order/create
    response-encrypt-uri-ignore-list:
      - /actuator/**
      - /swagger-ui/**
    request-decrypt-uri-ignore-list:
      - /actuator/**
      - /swagger-ui/**
```

URI 规则支持：

- 原始路径：`/api/user`
- 方法前缀：`get:/api/user`
- Ant 风格路径：`/api/user/**`
- 带方法前缀的 Ant 风格路径：`post:/api/order/*`
- Controller 注解推导出的路径

推荐使用带方法前缀的规则，避免同一路径不同 HTTP 方法之间互相影响。

## Query 参数解密

`@Decrypt` 支持指定需要解密的 query 参数：

```java
@Decrypt(decryptParam = "name,phone")
@GetMapping("/api/search")
public Result search(String name, String phone, Integer page) {
    return service.search(name, phone, page);
}
```

也可以通过配置指定。推荐使用 `request-decrypt-params`：

```yaml
spring:
  encrypt:
    request-decrypt-params:
      get$/api/search: name,phone
```

配置 key 中用 `$` 表示 `:`，最终会解析成 `get:/api/search`。

只会覆盖被配置为需要解密的参数；未配置的普通 query 参数会保留原始值。

为兼容旧配置，`request-decrypt-param`、`requestDecryptParam`、`requestDecyptParam` 仍会绑定到同一份参数配置。

## 自定义算法

默认使用 AES。业务项目可以提供自己的 `EncryptAlgorithm` Bean：

```java
@Bean
public EncryptAlgorithm encryptAlgorithm() {
    return new CustomEncryptAlgorithm();
}
```

接口定义：

```java
public interface EncryptAlgorithm {
    String encrypt(String content, String encryptKey) throws Exception;
    String decrypt(String encryptStr, String decryptKey) throws Exception;
}
```

## Actuator

如果业务项目引入了 Spring Boot Actuator，会注册 `encrypt-config` endpoint，用于查看当前加解密 URI 配置。是否暴露 endpoint 仍由业务项目自己的 actuator 配置控制。

```yaml
management:
  endpoints:
    web:
      exposure:
        include: encrypt-config
```

## 构建验证

```bash
mvn -DskipTests clean package
```
