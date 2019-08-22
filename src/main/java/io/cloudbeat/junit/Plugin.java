package io.cloudbeat.junit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import com.google.common.base.Stopwatch;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

@AutoService(TestExecutionListener.class)
public class Plugin implements TestExecutionListener {
    private Stopwatch testTimer;
    private Stopwatch suiteTimer;
    private PayloadModel payload;
    private ResultModel result;
    private String testMonitorStatusUrl;
    private String testMonitorToken;
    private int currentCaseIndex = 0;
    private ResultModel.Case currentCase;
    private ResultModel.SuiteIteration currentSuiteIteration;
    private final static String TEST_RESULTS_FILENAME = ".CB_TEST_RESULTS";
    private boolean isPluginDisabled;
    private boolean isSuiteSuccess = true;

    public Plugin() {
        isPluginDisabled = true;
        String payloadpath = System.getProperty("payloadpath");;
        String testmonitorUrl = System.getProperty("testmonitorurl");
        testMonitorToken = System.getProperty("testmonitortoken");

        if (payloadpath != null && testmonitorUrl != null && testMonitorToken != null) {
            testMonitorStatusUrl = testmonitorUrl + "/status";

            try {
                payload = PayloadModel.Load(payloadpath);
                result = new ResultModel();
                result.runId = payload.runId;
                result.instanceId = payload.instanceId;
                result.capabilities = payload.capabilities;
                result.metadata = payload.metadata;
                result.environmentVariables = payload.environmentVariables;
                result.iterations = new ArrayList();
                result.startTime = new Date();

                currentSuiteIteration = new ResultModel.SuiteIteration();
                currentSuiteIteration.cases = new ArrayList();

                suiteTimer = Stopwatch.createStarted();

                if (result.capabilities.containsKey("browserName")) {
                    // remove "technology" prefix from the browserName. old CB version uses technology.browser as browserName
                    // FIXME: this should be removed once CB backend is adapted to send only the browser name without technology prefix.
                    String browserName = result.capabilities.get("browserName");
                    int browserNameIdx = browserName.indexOf('.');
                    if (browserNameIdx > 0)
                        browserName = browserName.substring(browserNameIdx + 1);
                    System.setProperty("browserName", browserName);
                    isPluginDisabled = false;
                    return;
                }

                logError("Plugin will be disabled. browserName is not specified in capabilities.");
            } catch (Exception e) {
                logError("Plugin will be disabled. Unable to read/deserialize payload file.", e);
            }
        } else {
            logInfo("Plugin will be disabled. One of payloadpath, testmonitorurl, or testmonitortoken parameters is missing.");
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if(isPluginDisabled) {
            return;
        }

        currentSuiteIteration.isSuccess = isSuiteSuccess;
        result.iterations.add(currentSuiteIteration);
        result.endTime = new Date();
        result.isSuccess = isSuiteSuccess;
        result.iterationsTotal = 1;
        result.iterationsFailed = isSuiteSuccess ? 0 : 1;
        result.iterationsWarning = 0;
        result.iterationsPassed = isSuiteSuccess ? 1 : 0;

        suiteTimer.stop();
        long duration = suiteTimer.elapsed().getSeconds();
        result.duration = duration;

        ObjectMapper mapper = new ObjectMapper();
        String resultJson;
        try {
            resultJson = mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            logError("Failed to serialize results.", e);
            return;
        }

        try {
            PrintWriter writer = new PrintWriter(TEST_RESULTS_FILENAME, "UTF-8");
            writer.write(resultJson);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            logError("Failed to create " + TEST_RESULTS_FILENAME, e);
        }
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if(isPluginDisabled) {
            return;
        }

        testTimer = Stopwatch.createStarted();

        String testCaseName = testIdentifier.getDisplayName();
        testCaseName = testCaseName.substring(0, testCaseName.length() - 2);
        currentCaseIndex++;

        currentCase = new ResultModel.Case();

        PayloadModel.Case caze = payload.cases.get(testCaseName);
        if (caze != null) {
            currentCase.id = caze.id;
        }

        currentCase.name = testCaseName;
        currentCase.iterations = new ArrayList();
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if(isPluginDisabled) {
            return;
        }

        boolean isSuccess = testExecutionResult.getStatus() == TestExecutionResult.Status.SUCCESSFUL;

        if (!isSuccess) {
            onFailure(testIdentifier, testExecutionResult.getThrowable().get());
            return;
        }

        String testName = testIdentifier.getDisplayName();

        ResultModel.Step step = new ResultModel.Step();
        step.isSuccess = true;
        step.name = testName;

        testTimer.stop();
        step.duration = testTimer.elapsed().toMillis();

        ResultModel.CaseIteration caseIteration = new ResultModel.CaseIteration();
        caseIteration.iterationNum = currentCaseIndex;
        caseIteration.isSuccess = true;
        caseIteration.steps = new ArrayList();
        caseIteration.steps.add(step);

        currentCase.iterations.add(caseIteration);
        currentCase.isSuccess = true;

        currentSuiteIteration.cases.add(currentCase);

        StatusModel status = createBaseStatusModel(testName);
        status.caze.iterationsFailed = 0;
        status.caze.iterationsPassed = 1;

        if (report(testMonitorStatusUrl, status))
            logInfo("Status report for '" + testName + "' has been sent");
    }

    private void onFailure(TestIdentifier testIdentifier, Throwable error) {
        String failureReason = error.getLocalizedMessage();

        result.isSuccess = false;
        result.failure = failureReason;
        String testName = testIdentifier.getDisplayName();

        testName = testName.substring(0, testName.length() - 2);
        ResultModel.Step step = new ResultModel.Step();
        step.isSuccess = false;
        step.screenshot = takeWebDriverScreenshot();
        step.failure = failureReason;

        testTimer.stop();
        step.duration = testTimer.elapsed().toMillis();

        ResultModel.CaseIteration caseIteration = new ResultModel.CaseIteration();
        caseIteration.iterationNum = currentCaseIndex;
        caseIteration.isSuccess = false;
        caseIteration.steps = new ArrayList();
        caseIteration.steps.add(step);

        if (currentCase.iterations != null) {
            currentCase.iterations.add(caseIteration);
        }

        currentCase.isSuccess = false;
        currentSuiteIteration.cases.add(currentCase);

        StatusModel status = createBaseStatusModel(testName);
        status.caze.iterationsFailed = 1;
        status.caze.iterationsPassed = 0;

        if (report(testMonitorStatusUrl, status))
            logInfo("Status report for '" + testName + "' has been sent");
    }

    private StatusModel createBaseStatusModel(String testCaseName) {
        StatusModel status = new StatusModel();

        status.status = StatusModel.Statuses.Running.getValue();
        status.instanceId = payload.instanceId;
        status.runId = payload.runId;
        status.progress = (float)currentCaseIndex / payload.cases.size();

        status.caze = new StatusModel.CaseStatus();
        PayloadModel.Case caze = payload.cases.get(testCaseName);
        if (caze != null) {
            status.caze.id = caze.id;
        }

        status.caze.name = testCaseName;
        status.caze.order = currentCaseIndex;
        status.caze.progress = 1;

        return status;
    }

    private String takeWebDriverScreenshot() {
        WebDriver driver = JUnitRunner.getWebDriver();
        if (driver == null || !(driver instanceof TakesScreenshot))
            return null;
        return ((TakesScreenshot)driver).getScreenshotAs(OutputType.BASE64);
    }

    private boolean report(String endpointUrl, Object data) {
        HttpURLConnection http = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(data);
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            URL url = new URL(endpointUrl);
            URLConnection con = url.openConnection();
            http = (HttpURLConnection) con;
            http.setRequestMethod("POST");
            http.setRequestProperty("Authorization", "Bearer " + testMonitorToken);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            http.setRequestProperty("Connection", "Close");
            http.setDoOutput(true);
            http.setFixedLengthStreamingMode(length);
            http.connect();
            try (OutputStream os = http.getOutputStream()) {
                os.write(out);
                os.flush();
            }

            int responseCode = http.getResponseCode();
            if (responseCode < 200 || responseCode > 299) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()))) {
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    logError("Unable to report to " + endpointUrl + " : " + responseCode + " - " + response.toString());
                }
                return false;
            }


        } catch (Exception e) {
            logError("Unable to report to " + endpointUrl, e);
            return false;
        } finally {
            if (http != null)
                http.disconnect();
        }

        return true;
    }

    private void logError(String message) {
        System.err.println("[CloudBeat] " + message);
    }

    private void logError(String message, Exception e) {
        System.err.println("[CloudBeat] " + message);
        e.printStackTrace();
    }

    private void logInfo(String message) {
        System.out.println("[CloudBeat] " + message);
    }
}
