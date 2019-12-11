package io.cloudbeat.junit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CbTest implements AfterTestExecutionCallback {
    private static Map<String, ArrayList<StepModel>> _steps = new HashMap<>();


    public static void startStep(String name, TestInfo testInfo) {
        StepModel newStep = new StepModel();
        newStep.name = name;
        newStep.steps = new ArrayList<>();
        newStep.startTime = new Date();
        newStep.isFinished = false;

        String testName = getTestName(testInfo);
        if (_steps.containsKey(testName)) {
            ArrayList<StepModel> steps = _steps.get(testName);
            StepModel currentStep = getFirstNotFinishedStep(steps);

            while (currentStep != null) {
                steps = currentStep.steps;
                currentStep = getFirstNotFinishedStep(steps);
            }

            steps.add(newStep);
            return;
        }

        ArrayList steps = new ArrayList<StepModel>();
        steps.add(newStep);
        _steps.put(testName, steps);
    }

    public static void endStep(String name, TestInfo testInfo) {
        endStepInner(name, getTestName(testInfo), true);
    }

    private static void endStepInner(String name, String testName, boolean isSuccess) {
        if (!_steps.containsKey(testName)) {
            return;
        }

        ArrayList<StepModel> steps = _steps.get(testName);
        StepModel currentStep = getFirstNotFinishedStep(steps);

        if (currentStep == null) {
            return;
        }

        while (!currentStep.name.equalsIgnoreCase(name)) {
            steps = currentStep.steps;
            currentStep = getFirstNotFinishedStep(steps);

            if (currentStep == null) {
                return;
            }
        }

        finishStep(currentStep, isSuccess);


        while (currentStep != null) {
            finishStep(currentStep, isSuccess);
            steps = currentStep.steps;
            currentStep = getFirstNotFinishedStep(steps);
        }
    }

    private static void finishStep(StepModel currentStep, boolean isSuccess) {
        currentStep.status = isSuccess ? ResultStatus.Passed : ResultStatus.Failed;
        currentStep.isFinished = true;
        currentStep.duration = (new Date().toInstant().toEpochMilli() - currentStep.startTime.toInstant().toEpochMilli());
        if(!isSuccess) {
            WebDriver driver = JUnitRunner.getWebDriver();
            if (driver == null || !(driver instanceof TakesScreenshot)) {
                return;
            }

            currentStep.screenShot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.BASE64);
        }
    }

    private static StepModel getFirstNotFinishedStep(ArrayList<StepModel> steps) {
        return steps.stream()
                .filter((step) -> !step.isFinished)
                .findFirst()
                .orElse(null);
    }

    private static String getTestName(TestInfo testInfo) {

        return testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName();
    }

    private static String getTestName(ExtensionContext testInfo) {
        return testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName();
    }
    
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        if (_steps.containsKey(getTestName(extensionContext)))
        {
            ArrayList<StepModel> steps = _steps.get(getTestName(extensionContext));
            steps = new ArrayList<>(steps.stream().filter((stepModel -> !stepModel.isFinished)).collect(Collectors.toList()));
            for (StepModel step : steps)
            {
                endStepInner(step.name, getTestName(extensionContext), extensionContext.getExecutionException().isPresent());
            }

            extensionContext.publishReportEntry(serializeSteps(_steps.get(getTestName(extensionContext))));
        }
    }

    private String serializeSteps(ArrayList<StepModel> steps) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(steps);
        } catch (Exception e) {
            return "";
        }
    }
}