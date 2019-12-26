package io.cloudbeat.junit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudbeat.common.CbTest;
import io.cloudbeat.common.StepModel;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;

public class CbExtension implements AfterTestExecutionCallback, BeforeTestExecutionCallback {
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        CbJunit test = (CbJunit) extensionContext.getTestInstance().orElse(null);

        if(test == null) {
            return;
        }

        test.driver.quit();

        String testName = getTestName(extensionContext);
        extensionContext.publishReportEntry(testName, serializeSteps(test.getStepsForMethod(getTestName(extensionContext),
                extensionContext.getExecutionException().isPresent())));
    }

    private static String getTestName(ExtensionContext testInfo) {
        return testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName();
    }


    private String serializeSteps(ArrayList<StepModel> steps) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(steps);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        CbJunit test = (CbJunit) extensionContext.getTestInstance().orElse(null);

        if(test == null) {
            return;
        }

        test.currentTestName = getTestName(extensionContext);
    }
}
