package io.cloudbeat.junit;

import java.util.ArrayList;
import java.util.Dictionary;

public class CaseModel extends TestResultEntityWithId {
    public int iterationNum;
    public ArrayList<LogResult> logs;
    public Dictionary<String, Object> сontext;
    public ArrayList<StepModel> steps;
    public short order;
}
