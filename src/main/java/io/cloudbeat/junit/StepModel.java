package io.cloudbeat.junit;

import java.util.ArrayList;
import java.util.Dictionary;

public class StepModel extends TestResultBase {
    public String location;
    public Dictionary<String, String> stats;
    public FailureModel failure;
    public String screenShot;
    public ArrayList<StepModel> steps;
}
