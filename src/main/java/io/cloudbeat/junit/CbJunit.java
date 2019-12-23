package io.cloudbeat.junit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudbeat.common.CbTest;
import io.cloudbeat.common.StepModel;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;

public class CbJunit extends CbTest implements AfterTestExecutionCallback, BeforeTestExecutionCallback {
    public static String currentTestName;

    @Override
    public String getCurrentTestName() {
        return currentTestName;
    }

    private static String getTestName(ExtensionContext testInfo) {
        return testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName();
    }
    
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) {
        extensionContext.publishReportEntry(serializeSteps(getStepsForMethod(getTestName(extensionContext),
                                                                             extensionContext.getExecutionException().isPresent())));
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
        currentTestName = getTestName(extensionContext);
    }
}