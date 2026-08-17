---
name: add-api-test
description: Write or modify REST API tests in this framework using ApiHelper/ApiResponseValidator/RestAssured. Use when adding or changing API test coverage against a JSON HTTP service.
---

# Adding / editing an API test

Location: `src/test/java/com/framework/tests/api/`. See `UserApiTest.java` for the
canonical style.

## Rules

1. **Extend `BaseTest`** and **tag `@Test(groups = {"api"})`** - this is what makes
   `BaseTest.isUiTest()` return `false` and skip browser init. Forgetting the group
   launches a browser for nothing.

2. **Register the class in `testng.xml`** under `<test name="API Tests"><classes>`.

3. **Call the API only through `ApiHelper`** (`get`, `post`, `put`, `patch`, `delete`),
   passing **relative** endpoint paths (`"/users"`, not a full URL). Base URI, base
   path, content type and default headers all come from config via
   `ApiBase.getRequestSpec()` (`api.baseUri`, `api.basePath`, `api.contentType`,
   `api.defaultHeaders`) - never build a `RequestSpecification` or call `RestAssured`
   directly from a test.

4. **New endpoint config**: add `api.xxxEndpoint` to `config.properties` (and per-env
   overrides), then reference it as a `static final` field via `ConfigReader.get(...)`
   at the top of the test class - don't inline the path string in each test method
   (see `USERS_ENDPOINT`, `POSTS_ENDPOINT` in `UserApiTest`).

5. **Validate with `ApiResponseValidator`** (all hard assertions - they fail
   immediately):
   - `validateStatusCode(response, expected)`
   - `validateResponseTimeLessThan(response, maxMs)` - threshold from
     `api.maxResponseTimeMs`, read once as a class field
   - `validateSchema(response, "testdata/schemas/xxx-schema.json")` - schema file goes
     under `src/test/resources/testdata/schemas/`
   - `validateField(response, jsonPath, expected)` / `validateFieldNotNull(response, jsonPath)`
   Use `SoftAssertHelper` instead only for secondary, non-blocking checks.

6. **Request/response traffic is already logged and attached to Allure** - the
   `Log4j2RequestResponseFilter` and `AllureRestAssured` filter are wired into every
   spec by `ApiBase`. Don't add manual logging of request/response bodies in the test.

7. **JSON-driven payloads**: `dataProvider = "jsonData"` maps a JSON array file onto a
   POJO (Jackson, snake_case JSON via `@JsonProperty`, see `model.User`). Add a new
   POJO under `com.framework.model` for a new payload shape; add the JSON fixture
   under `src/test/resources/testdata/`.

## Template

```java
package com.framework.tests.api;

import com.framework.api.ApiHelper;
import com.framework.api.ApiResponseValidator;
import com.framework.base.BaseTest;
import com.framework.config.ConfigReader;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

@Feature("Orders API")
public class OrderApiTest extends BaseTest {

    private static final String ORDERS_ENDPOINT = ConfigReader.get("api.ordersEndpoint", "/orders");
    private static final long MAX_RESPONSE_TIME_MS = ConfigReader.getInt("api.maxResponseTimeMs", 5000);

    @Test(groups = {"api"})
    @Story("List orders")
    public void testGetOrders() {
        Response response = ApiHelper.get(ORDERS_ENDPOINT);

        ApiResponseValidator.validateStatusCode(response, 200);
        ApiResponseValidator.validateResponseTimeLessThan(response, MAX_RESPONSE_TIME_MS);
        ApiResponseValidator.validateSchema(response, "testdata/schemas/orders-schema.json");
        ApiResponseValidator.validateFieldNotNull(response, "[0].id");
    }
}
```

Then add `<class name="com.framework.tests.api.OrderApiTest"/>` under the `API Tests`
`<test>` block in `testng.xml`.

## Checklist

- [ ] Extends `BaseTest`; `@Test(groups = {"api"}, ...)`
- [ ] Class added to `testng.xml` -> `API Tests` -> `<classes>`
- [ ] Endpoint paths are relative, sourced from `ConfigReader` static fields, not literals
- [ ] All calls go through `ApiHelper`, never raw `RestAssured`/`RequestSpecification`
- [ ] Validation via `ApiResponseValidator`, schema files under `testdata/schemas/`
- [ ] New payload POJOs under `com.framework.model` with correct Jackson annotations
