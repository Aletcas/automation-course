package aletca.core.api;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;

public abstract class APITestBase {
    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeEach
    protected void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void tearDown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    protected void setupRouteInterceptor(String urlPattern, int statusCode, String body) {
        context.route(urlPattern, route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(statusCode)
                    .setHeaders(Collections.singletonMap("Content-Type", "text/html"))
                    .setBody(body)
            );
        });
    }

    protected String getElementText(String selector) {
        return page.textContent(selector);
    }

    protected void navigateTo(String url) {
        page.navigate(url);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
}
