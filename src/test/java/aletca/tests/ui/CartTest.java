package aletca.tests.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.microsoft.playwright.options.WaitForSelectorState.ATTACHED;
import static com.microsoft.playwright.options.WaitForSelectorState.VISIBLE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CartTest {
    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;
    Path videoPath;

    // Директория для артефактов с timestamp
    private String artifactsDir;
    private Path screenshotsDir;
    private Path videosDir;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

    @BeforeEach
    void setup() {
        // Создаем директорию для артефактов с текущей датой/временем
        createArtifactsDirectory();

        // Создаем контекст с записью видео в timestamp директорию
        context = browser.newContext(new Browser.NewContextOptions()
                .setRecordVideoDir(videosDir)
                .setRecordVideoSize(1280, 720));
        page = context.newPage();
        page.setViewportSize(1920, 1080);
    }

    /**
     * Создает директорию для артефактов с текущей датой/временем
     */
    private void createArtifactsDirectory() {
        artifactsDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        screenshotsDir = Paths.get("artifacts", artifactsDir, "screenshots");
        videosDir = Paths.get("artifacts", artifactsDir, "videos");

        try {
            Files.createDirectories(screenshotsDir);
            Files.createDirectories(videosDir);
            System.out.println("Создана директория для артефактов: " + artifactsDir);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать директорию для артефактов: " + artifactsDir, e);
        }
    }

    @Test
    void testCartActions() {
        Allure.step("1. Открытие страницы логина", () -> {
            page.navigate("https://www.saucedemo.com/",
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            // Сохраняем скриншот в timestamp директорию и прикрепляем к Allure
            saveScreenshotToDirectory("01_login_page.png", "Страница логина");
        });

        Allure.step("2. Заполнение учетных данных", () -> {
            page.waitForSelector("#user-name", new Page.WaitForSelectorOptions().setState(VISIBLE));
            page.waitForSelector("#password", new Page.WaitForSelectorOptions().setState(VISIBLE));

            page.fill("#user-name", "standard_user");
            page.fill("#password", "secret_sauce");

            // Сохраняем скриншот с заполненными полями
            saveScreenshotToDirectory("02_filled_credentials.png", "Заполненные учетные данные");
        });

        Allure.step("3. Клик по кнопке входа", () -> {
            page.waitForSelector("#login-button", new Page.WaitForSelectorOptions()
                    .setState(ATTACHED)
                    .setTimeout(5000));

            // Скриншот перед кликом
            saveScreenshotToDirectory("03_before_login.png", "Перед входом");

            page.click("#login-button");

            page.waitForCondition(() ->
                            page.evaluate("() => document.readyState").equals("complete"),
                    new Page.WaitForConditionOptions().setTimeout(10000)
            );
        });

        Allure.step("4. Проверка успешного входа", () -> {
            page.waitForSelector(".app_logo",
                    new Page.WaitForSelectorOptions()
                            .setState(VISIBLE)
                            .setTimeout(10000));

            assertTrue(page.isVisible(".app_logo"),
                    "Регистрация не прошла");

            // Скриншот после успешного входа
            saveScreenshotToDirectory("04_after_login.png", "Успешный вход");
        });

        Allure.step("5. Добавление товара в корзину", () -> {
            // Ждем появления товаров
            page.waitForSelector("button[data-test^='add-to-cart']",
                    new Page.WaitForSelectorOptions().setState(VISIBLE));

            // Скриншот перед добавлением
            saveScreenshotToDirectory("05_before_adding_to_cart.png", "Товары до добавления");

            // Добавляем первый товар в корзину
            page.click("button[data-test^='add-to-cart-sauce-labs-backpack']");

            // Ждем обновления корзины
            page.waitForTimeout(1000);

            // Скриншот корзины после добавления
            saveLocatorScreenshotToDirectory("06_cart_after_add.png", ".shopping_cart_link", "Корзина после добавления товара");

            // Общий скриншот страницы
            saveScreenshotToDirectory("07_page_after_add.png", "Страница после добавления товара");
        });

        Allure.step("6. Удаление товара из корзины", () -> {
            // Скриншот перед удалением
            saveScreenshotToDirectory("08_before_remove.png", "Перед удалением товара");

            // Удаляем товар из корзины
            page.click("button[data-test^='remove-sauce-labs-backpack']");

            // Ждем обновления корзины
            page.waitForTimeout(1000);

            // Скриншот корзины после удаления
            saveLocatorScreenshotToDirectory("09_cart_after_remove.png", ".shopping_cart_link", "Корзина после удаления товара");

            // Общий скриншот страницы
            saveScreenshotToDirectory("10_page_after_remove.png", "Страница после удаления товара");
        });

        Allure.step("7. Переход в корзину", () -> {
            page.click(".shopping_cart_link");
            page.waitForSelector(".cart_list",
                    new Page.WaitForSelectorOptions().setState(VISIBLE));

            saveScreenshotToDirectory("11_cart_page.png", "Страница корзины");
        });
    }

    /**
     * Сохраняет скриншот всей страницы в директорию и прикрепляет к Allure
     */
    private void saveScreenshotToDirectory(String filename, String allureName) {
        try {
            byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotsDir.resolve(filename))
                    .setFullPage(true));

            // Прикрепляем к Allure отчету
            Allure.addAttachment(allureName, "image/png",
                    new ByteArrayInputStream(screenshotBytes), ".png");

            System.out.println("Скриншот сохранен: " + screenshotsDir.resolve(filename));
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении скриншота: " + e.getMessage());
        }
    }

    /**
     * Сохраняет скриншот конкретного локатора в директорию и прикрепляет к Allure
     */
    private void saveLocatorScreenshotToDirectory(String filename, String selector, String allureName) {
        try {
            byte[] screenshotBytes = page.locator(selector).screenshot(new Locator.ScreenshotOptions()
                    .setPath(screenshotsDir.resolve(filename)));

            // Прикрепляем к Allure отчету
            Allure.addAttachment(allureName, "image/png",
                    new ByteArrayInputStream(screenshotBytes), ".png");

            System.out.println("Скриншот локатора сохранен: " + screenshotsDir.resolve(filename));
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении скриншота локатора: " + e.getMessage());
        }
    }

    /**
     * Копирует видео файл в timestamp директорию и прикрепляет к Allure
     */
    private void saveVideoToDirectory() {
        try {
            if (videoPath != null && Files.exists(videoPath)) {
                // Создаем имя файла для видео
                String videoFilename = "test_execution.webm";
                Path targetVideoPath = videosDir.resolve(videoFilename);

                // Копируем видео файл в нашу timestamp директорию
                Files.copy(videoPath, targetVideoPath);

                // Прикрепляем видео к Allure отчету
                try (InputStream videoStream = Files.newInputStream(targetVideoPath)) {
                    Allure.addAttachment("Видео выполнения теста", "video/webm", videoStream, ".webm");
                }

                System.out.println("Видео сохранено: " + targetVideoPath);

                // Удаляем оригинальный временный файл
                Files.deleteIfExists(videoPath);
            }
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении видео: " + e.getMessage());
        }
    }

    @AfterEach
    void teardown() {
        try {
            // Получаем путь к видео ДО закрытия контекста
            if (page != null && page.video() != null) {
                videoPath = page.video().path();
            }

            // Закрываем контекст (это завершает запись видео)
            if (context != null) {
                context.close();
            }

            // Сохраняем видео в timestamp директорию
            saveVideoToDirectory();

        } catch (Exception e) {
            System.err.println("Ошибка в teardown: " + e.getMessage());
        }
    }

    @AfterAll
    static void closeBrowser() {
        try {
            if (browser != null) {
                browser.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}