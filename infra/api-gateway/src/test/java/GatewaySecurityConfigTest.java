
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.ApiGateway;

@SpringBootTest(classes = ApiGateway.class)
@AutoConfigureWebTestClient
class GatewaySecurityConfigTest {

    @Autowired
    WebTestClient webTestClient;

    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = webTestClient.mutate().baseUrl("http://localhost").build();
    }

    private WebTestClient.ResponseSpec call(HttpMethod method, String uri, String user) {
        WebTestClient.RequestBodySpec req = client.method(method).uri(uri);
        if (user != null) {
            req.headers(h -> h.setBasicAuth(user, user));
        }
        return req.exchange();
    }

    private void assertPassed(HttpMethod m, String uri, String user) {
        call(m, uri, user).expectStatus().value(s -> {
            if (s == 401 || s == 403) {
                throw new AssertionError("Ожидали, что Security пропустит, но статус " + s);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/products", "/api/products/1", "/api/categories", "/api/inventory/5"})
    void publicGetRoutes_noAuth_passed(String uri) {
        assertPassed(HttpMethod.GET, uri, null);
    }

    @Test
    void createOrder_noAuth_401() {
        call(HttpMethod.POST, "/api/orders", null).expectStatus().isUnauthorized();
    }

    @Test
    void createOrder_ivan_passed() {
        assertPassed(HttpMethod.POST, "/api/orders", "ivan");
    }

    @Test
    void orderById_noAuth_401() {
        call(HttpMethod.GET, "/api/orders/1", null).expectStatus().isUnauthorized();
    }

    @Test
    void orderById_ivan_passed() {
        assertPassed(HttpMethod.GET, "/api/orders/1", "ivan");
    }

    @Test
    void ordersByEmail_ivan_passed() {
        assertPassed(HttpMethod.GET, "/api/orders/by-email?email=a@b.c", "ivan");
    }

    @Test
    void allOrders_ivan_403() {
        call(HttpMethod.GET, "/api/orders", "ivan").expectStatus().isForbidden();
    }

    @Test
    void allOrders_anna_passed() {
        assertPassed(HttpMethod.GET, "/api/orders", "anna");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/products", "/api/categories", "/api/inventory"})
    void mutatingRoutes_ivan_403(String uri) {
        for (HttpMethod m : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT,
                HttpMethod.PATCH, HttpMethod.DELETE}) {
            call(m, uri + "/1", "ivan").expectStatus().isForbidden();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/products", "/api/categories", "/api/inventory"})
    void mutatingRoutes_anna_passed(String uri) {
        assertPassed(HttpMethod.POST, uri, "anna");
        assertPassed(HttpMethod.DELETE, uri + "/1", "anna");
    }

    @Test
    void unknownRoute_anna_denied() {
        call(HttpMethod.GET, "/api/unknown", "anna").expectStatus().isForbidden();
    }

    @Test
    void unknownRoute_noAuth_401() {
        call(HttpMethod.GET, "/api/unknown", null).expectStatus().isUnauthorized();
    }

    @Test
    void wrongPassword_401() {
        client.post().uri("/api/orders")
                .headers(h -> h.setBasicAuth("ivan", "wrong"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void preflight_noAuth_passed_withCorsHeaders() {
        client.options().uri("/api/orders")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000");

    }

    @Test
    void unauthorized_response_hasCorsHeaders() {
        client.post().uri("/api/orders")
                .header("Origin", "http://localhost:3000")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000");

    }

    @Test
    void userRoutes_anna_passed() {
        assertPassed(HttpMethod.POST, "/api/orders", "anna");
        assertPassed(HttpMethod.GET, "/api/orders/1", "anna");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH", "DELETE"})
    void unlistedOrderMethods_anna_forbidden(String method) {
        call(HttpMethod.valueOf(method), "/api/orders/1", "anna")
                .expectStatus().isForbidden();
    }

    @Test
    void actuator_isClosed() {
        call(HttpMethod.GET, "/actuator/health", null).expectStatus().isUnauthorized();
        call(HttpMethod.GET, "/actuator/health", "anna").expectStatus().isForbidden();
    }

    @Test
    void unknownUser_401() {
        client.post().uri("/api/orders")
                .headers(h -> h.setBasicAuth("nobody", "nobody"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void preflight_onUnknownRoute_ok() {
        client.options().uri("/api/unknown")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void foreignOrigin_403() {
        client.get().uri("/api/products")
                .header("Origin", "http://evil.example")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void ordersByEmail_noAuth_401() {
        call(HttpMethod.GET, "/api/orders/by-email?email=a@b.c", null)
                .expectStatus().isUnauthorized();
    }

}
