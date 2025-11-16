package aletca.tests.api;

import aletca.core.api.APITestBase;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GitHubSearchInterceptionTest extends APITestBase {


    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        // Перехват запроса поиска
        context.route("**/search**", route -> {
            // Получаем оригинальный URL
            String originalUrl = route.request().url();

            // Декодируем и модифицируем параметры
            String modifiedUrl = originalUrl.contains("q=")
                    ? originalUrl.replaceAll("q=[^&]+", "q=stars%3A%3E10000")
                    : originalUrl + (originalUrl.contains("?") ? "&" : "?") + "q=stars%3A%3E10000";

            // Продолжаем запрос с модифицированным URL
            route.resume(new Route.ResumeOptions().setUrl(modifiedUrl));
        });
    }

    @Test
    void testSearchModification() {
        navigateTo("https://github.com/search?q=java");

        // Ожидаем результатов и проверяем всё в одном
        page.locator(".Box-sc-62in7e-0").first().waitFor();

        String currentUrl = page.url();
        String displayedQuery = page.locator("h1, h2, h3")
                .filter(new Locator.FilterOptions().setHasText("stars:>10000"))
                .textContent()
                .replace("repositories Search Results · ", "")
                .trim();

        assertAll("Search modification validation",
                () -> assertTrue(currentUrl.contains("q=stars%3A%3E10000"),
                        "URL should contain modified query. Actual: " + currentUrl),
                () -> assertEquals("stars:>10000", displayedQuery,
                        "UI should display modified query. Actual: " + displayedQuery)
        );
    }

    @AfterEach
    protected void tearDown() {
        super.tearDown();
    }
}
