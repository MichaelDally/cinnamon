package io.magentys.cinnamon.webdriver;

import io.magentys.cinnamon.webdriver.actions.synthetic.DomEvent;
import io.magentys.cinnamon.webdriver.actions.synthetic.SyntheticEvent;
import io.magentys.cinnamon.webdriver.conditions.Condition;
import io.magentys.cinnamon.webdriver.support.ui.CinnamonExpectedConditions;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import static io.magentys.cinnamon.webdriver.Timeouts.defaultTimeout;
import static io.magentys.cinnamon.webdriver.WebDriverContainer.webDriver;
import static io.magentys.cinnamon.webdriver.WebDriverContainer.windowTracker;

public final class Browser {

    /**
     * Opens a new web page in the current browser window.
     *
     * @param url The URL to load
     */
    public static void open(String url) {
        webDriver().get(url);
    }

    /**
     * Gets the title of the current page.
     *
     * @return The title of the current page
     */
    public static String title() {
        return webDriver().getTitle();
    }

    public static void back() {
        webDriver().navigate().back();
    }

    public static void forward() {
        webDriver().navigate().forward();
    }

    public static void refresh() {
        webDriver().navigate().refresh();
    }

    /**
     * Closes the browser window that has focus.
     */
    public static void close() {
        webDriver().close();
    }

    public static CinnamonTargetLocator switchTo() {
        return new CinnamonTargetLocator(webDriver(), windowTracker());
    }

    public static Alert alert() {
        return alert(defaultTimeout());
    }

    public static Alert alert(final Timeout timeout) {
        return switchTo().alert(timeout);
    }

    public static void fireEvent(final WebElement element, final DomEvent event) {
        syntheticEvent().fireEvent(element, event);
    }

    private static SyntheticEvent syntheticEvent() {
        return new SyntheticEvent(webDriver());
    }

    /**
     * Gets the Options interface
     * <p>
     * Binds to an instance of CinnamonWebDriverOptions. Uses Javascript to manage cookies if the browser
     * is either InternetExplorer or Safari.
     *
     * @return WebDriver.Options
     * @see org.openqa.selenium.WebDriver.Options
     */
    public static WebDriver.Options manage() {
        return new CinnamonWebDriverOptions(webDriver());
    }

    public static void waitUntil(final Condition<WebDriver> condition) {
        waitUntil(condition, defaultTimeout());
    }

    public static void waitUntil(final Condition<WebDriver> condition, final Timeout timeout) {
        wait(timeout).until(CinnamonExpectedConditions.conditionToBe(condition));
    }

    private static FluentWait<WebDriver> wait(final Timeout timeout) {
        return new WebDriverWait(webDriver(), timeout.getSeconds());
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease execution) for the specified number of
     * milliseconds.
     * <p>
     * This method is not recommended. Instead, you should identify a more robust synchronisation solution.
     *
     * @param millis The time to sleep in milliseconds
     * @see Thread#sleep(long)
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
