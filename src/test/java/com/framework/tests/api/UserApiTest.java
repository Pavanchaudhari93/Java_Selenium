package com.framework.tests.api;

import com.framework.api.ApiHelper;
import com.framework.api.ApiResponseValidator;
import com.framework.base.BaseTest;
import com.framework.config.ConfigReader;
import com.framework.dataproviders.DataProviderUtils;
import com.framework.model.User;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRUD tests against the free, no-auth jsonplaceholder.typicode.com API.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>GET/POST/PUT/DELETE via {@link ApiHelper}.</li>
 *   <li>Status code, JSON schema, field and response-time validation.</li>
 *   <li>JSON data provider mapping into {@link User} POJOs.</li>
 * </ul>
 * These tests are marked group {@code api} so BaseTest skips browser setup.
 */
@Feature("User API")
public class UserApiTest extends BaseTest {

    private static final String USERS_ENDPOINT = ConfigReader.get("api.usersEndpoint", "/users");
    private static final String POSTS_ENDPOINT = ConfigReader.get("api.postsEndpoint", "/posts");
    private static final long MAX_RESPONSE_TIME_MS = ConfigReader.getInt("api.maxResponseTimeMs", 5000);

    @Test(groups = {"api"})
    @Story("List users")
    public void testGetUsers() {
        Response response = ApiHelper.get(USERS_ENDPOINT);

        ApiResponseValidator.validateStatusCode(response, 200);
        ApiResponseValidator.validateResponseTimeLessThan(response, MAX_RESPONSE_TIME_MS);
        ApiResponseValidator.validateSchema(response, "testdata/schemas/users-schema.json");
        ApiResponseValidator.validateFieldNotNull(response, "[0].id");
        ApiResponseValidator.validateFieldNotNull(response, "[0].email");
    }

    @Test(dataProvider = "jsonData", dataProviderClass = DataProviderUtils.class, groups = {"api"})
    @Story("Create resource from JSON-driven data")
    public void testCreatePost(User user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", user.getFirstName() + " " + user.getLastName());
        body.put("body", "Automation payload");
        body.put("userId", user.getId());

        Response response = ApiHelper.post(POSTS_ENDPOINT, body);

        ApiResponseValidator.validateStatusCode(response, 201);
        ApiResponseValidator.validateResponseTimeLessThan(response, MAX_RESPONSE_TIME_MS);
        ApiResponseValidator.validateFieldNotNull(response, "id");
        ApiResponseValidator.validateField(response, "title", user.getFirstName() + " " + user.getLastName());
    }

    @Test(groups = {"api"})
    @Story("Update resource")
    public void testUpdatePost() {
        Map<String, Object> body = Map.of(
                "title", "updated title",
                "body", "updated body");

        Response response = ApiHelper.put(POSTS_ENDPOINT + "/1", body);

        ApiResponseValidator.validateStatusCode(response, 200);
        ApiResponseValidator.validateResponseTimeLessThan(response, MAX_RESPONSE_TIME_MS);
        ApiResponseValidator.validateField(response, "title", "updated title");
        ApiResponseValidator.validateFieldNotNull(response, "id");
    }

    @Test(groups = {"api"})
    @Story("Delete resource")
    public void testDeletePost() {
        Response response = ApiHelper.delete(POSTS_ENDPOINT + "/1");

        ApiResponseValidator.validateStatusCode(response, 200);
        ApiResponseValidator.validateResponseTimeLessThan(response, MAX_RESPONSE_TIME_MS);
    }
}
