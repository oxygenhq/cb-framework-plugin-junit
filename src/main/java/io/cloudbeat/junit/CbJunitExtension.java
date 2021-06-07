package io.cloudbeat.junit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import io.cloudbeat.common.CbTestContext;

import java.io.Console;
import java.util.*;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.launcher.TestExecutionListener;

import java.lang.reflect.Method;

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

    @Override
    public synchronized void beforeAll(ExtensionContext context) {
        if (!started) {
            started = true;
            // The following line registers a callback hook when the root test context is shut down
            context.getRoot().getStore(GLOBAL).put("CB-JUNIT-EXT", this);
        }
        if (!CbTestContext.getReporter().isStarted())
            setup(context);

        JunitReporterUtils.startSuite(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (context.getRoot() == null)
            return;
        JunitReporterUtils.endSuite(context);
        System.out.println("afterAll: " + context.toString());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        //context.getParent().ifPresent(this::startTemplate);
        System.out.println("beforeEach: " + context.toString());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        System.out.println("afterEach: " + context.toString());
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        //context.getTestMethod().get().getParameters()
        System.out.println("beforeTestExecution: " + context.toString());
        try {
            JunitReporterUtils.startCase(context);
        }
        catch (Exception e) {
            System.err.println("Error in beforeTestExecution: " + e.toString());
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        System.out.println("afterTestExecution: " + context.toString());
        try {
            JunitReporterUtils.endCase(context);
        }
        catch (Exception e) {
            System.err.println("Error in afterTestExecution: " + e.toString());
        }
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        try {
            JunitReporterUtils.disabledCase(context, reason);
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


    private String serialize(Object object) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            return "";
        }
    }

    private void setup(final ExtensionContext context) {
        JunitReporterUtils.startInstance();
    }

    @Override
    public void close() throws Throwable {
        JunitReporterUtils.endInstance();
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
}