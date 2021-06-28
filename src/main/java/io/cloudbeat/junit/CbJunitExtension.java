package io.cloudbeat.junit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import io.cloudbeat.common.CbTestContext;

import java.io.Console;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import io.cloudbeat.common.Helper;
import io.cloudbeat.common.config.CbConfig;
import io.cloudbeat.common.reporter.CbTestReporter;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.lang.reflect.Method;
import java.util.function.Function;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

//@AutoService(TestExecutionListener.class)
public class CbJunitExtension implements
        //Extension,
        BeforeAllCallback,
        BeforeEachCallback,
        //InvocationInterceptor,
        BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        AfterEachCallback,
        AfterAllCallback,
        ExtensionContext.Store.CloseableResource,
        //ExecutionCondition,
        TestWatcher
{
    static boolean started = false;
    static CbTestContext ctx = CbTestContext.getInstance();

    public CbJunitExtension() {

    }

    public static WebDriver createWebDriver() throws MalformedURLException {
        return createWebDriver(null);
    }
    public static WebDriver createWebDriver(DesiredCapabilities extraCapabilities) throws MalformedURLException {
        if (ctx == null || ctx.getReporter() == null) {
            // if user called createWebDriver method outside CloudBeat context and provided capabilities
            // then try to initialize a regular WebDriver with default webdriver URL
            if (extraCapabilities != null)
                return new RemoteWebDriver(new URL(CbConfig.DEFAULT_WEBDRIVER_URL), extraCapabilities);
            return null;
        }
        CbTestReporter reporter = ctx.getReporter();
        DesiredCapabilities capabilities = Helper.mergeUserAndCloudbeatCapabilities(extraCapabilities);
        CbConfig config = CbTestContext.getInstance().getConfig();
        final String webdriverUrl = config != null && config.getSeleniumUrl() != null ? config.getSeleniumUrl() : CbConfig.DEFAULT_WEBDRIVER_URL;
        RemoteWebDriver driver = new RemoteWebDriver(new URL(webdriverUrl), capabilities);
        return reporter.getWebDriverWrapper().wrap(driver);
    }

    @Override
    public synchronized void beforeAll(ExtensionContext context) {
        System.out.println("beforeAll - thread: " + Thread.currentThread().getName());
        System.out.println("beforeAll - class: " + context.getTestClass().get().getName());
        if (!started) {
            started = true;
            // The following line registers a callback hook when the root test context is shut down
            context.getRoot().getStore(GLOBAL).put("CB-JUNIT-EXT", this);
        }
        if (ctx.isActive() && !ctx.getReporter().isStarted())
            setup(context);

        if (ctx.isActive())
            JunitReporterUtils.startSuite(ctx.getReporter(), context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (ctx.isActive())
            JunitReporterUtils.endSuite(ctx.getReporter(), context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (ctx.isActive())
            JunitReporterUtils.startBeforeEachHook(ctx.getReporter(), context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (ctx.isActive())
            JunitReporterUtils.endBeforeEachHook(ctx.getReporter(), context);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        if (!ctx.isActive())
            return;
        try {
            JunitReporterUtils.startCase(ctx.getReporter(), context);
        }
        catch (Exception e) {
            System.err.println("Error in beforeTestExecution: " + e.toString());
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (!ctx.isActive())
            return;
        try {
            JunitReporterUtils.endCase(ctx.getReporter(), context);
        }
        catch (Exception e) {
            System.err.println("Error in afterTestExecution: " + e.toString());
        }
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        if (!ctx.isActive())
            return;
        try {
            JunitReporterUtils.disabledCase(ctx.getReporter(), context, reason);
        }
        catch (Exception e) {
            System.err.println("Error in testDisabled: " + e.toString());
        }
        //if (Boolean.parseBoolean(System.getProperty("reportDisabledTests"))) {
        //    String description = reason.orElse(createStepDescription(context));
        //    startTestItem(context, Collections.emptyList(), STEP, description, Calendar.getInstance().getTime());
        //    finishTestItem(context, SKIPPED);
        //}
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable throwable) {
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable throwable) {
        if (!ctx.isActive())
            return;
        try {
            JunitReporterUtils.failedCase(ctx.getReporter(), context, throwable);
        }
        catch (Throwable e) {
            System.err.println("Error in testFailed: " + e.toString());
        }
    }

    /*@Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        CbJunit test = (CbJunit) extensionContext.getTestInstance().orElse(null);
        FailureModel failureModel = null;
        if(test == null) {
            return;
        }

        if(test.driver != null) {
            test.driver.close();
        }

        Throwable ex = extensionContext.getExecutionException().orElse(null);
        boolean isTestSuccess = true;
        if(ex != null) {
            isTestSuccess = false;
            failureModel = new FailureModel(ex, test.getCurrentTestPackageName());
        }

        String testName = getTestName(extensionContext);

        ArrayList<StepModel> steps = test.getStepsForMethod(testName, isTestSuccess, failureModel);

        ArrayList<LogResult> logs = test.getLastLogEntries();

        extensionContext.publishReportEntry("steps", serialize(steps));
        extensionContext.publishReportEntry("logs", serialize(logs));
    }

    private static String getTestName(ExtensionContext testInfo) {
        return testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName();
    }*/

    private void setup(final ExtensionContext context) {
        if (!ctx.isActive())
            return;
        ctx.getReporter().setFramework("JUnit", "5");
        JunitReporterUtils.startInstance(ctx.getReporter());
    }

    @Override
    public void close() throws Throwable {
        if (!ctx.isActive())
            return;
        System.out.println("close - thread: " + Thread.currentThread().getName());
        JunitReporterUtils.endInstance(ctx.getReporter());
        started = false;
    }

    /*@Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        return null;
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        CbJunit test = (CbJunit) extensionContext.getTestInstance().orElse(null);

        if(test == null) {
            return;
        }

        test.currentTestName = getTestName(extensionContext);
    }*/
    public static String startStep(final String name) {
        CbTestReporter reporter = getReporter();
        if (reporter == null)
            return null;
        return reporter.startStep(name);
    }

    public static void endLastStep() {
        CbTestReporter reporter = getReporter();
        if (reporter == null)
            return;
        reporter.endLastStep();
    }

    public static void step(final String name, Runnable stepFunc) {
        CbTestReporter reporter = getReporter();
        if (reporter == null)
            return;
        reporter.step(name, stepFunc);
    }

    private static CbTestReporter getReporter() {
        if (CbJunitExtension.ctx == null)
            return null;
        return CbJunitExtension.ctx.getReporter();
    }
}
