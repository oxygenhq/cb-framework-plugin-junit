package io.cloudbeat.junit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudbeat.common.CloudBeatTest;
import io.cloudbeat.common.model.LogResult;
import io.cloudbeat.common.model.StepModel;
import io.cloudbeat.common.model.FailureModel;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;

public class CbExtension implements AfterTestExecutionCallback, BeforeTestExecutionCallback {
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        CbJunit test = (CbJunit) extensionContext.getTestInstance().orElse(null);
        FailureModel failureModel = null;
        if(test == null) {
            return;
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
    }


    private String serialize(Object object) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(object);
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
