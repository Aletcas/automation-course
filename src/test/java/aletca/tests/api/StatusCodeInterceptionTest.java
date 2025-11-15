package aletca.tests.api;

import aletca.core.api.APITestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StatusCodeInterceptionTest extends APITestBase {

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        setupRouteInterceptor(
                "**/status_codes/404",
                200,
                "<h3>Mocked Success Response</h3>"
        );
    }

    @Test
    void testMockedStatusCode() {
        page.navigate("https://the-internet.herokuapp.com/status_codes");

        page.click("a[href='status_codes/404']");

        String responseText = page.textContent("h3");
        assertEquals("Mocked Success Response", responseText,
                "Должен отображаться текст из мок-ответа");

        assertTrue(page.content().contains("Mocked Success Response"),
                "Страница должна содержать мок-текст");
    }
}