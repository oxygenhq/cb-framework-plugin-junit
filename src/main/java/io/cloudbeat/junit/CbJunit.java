package io.cloudbeat.junit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudbeat.common.CloudBeatTest;
import io.cloudbeat.common.model.StepModel;

import org.junit.jupiter.api.extension.*;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;

public class CbJunit extends CloudBeatTest {
    public String currentTestName;

    @Override
    public String getCurrentTestName() {
        return currentTestName;
    }
}