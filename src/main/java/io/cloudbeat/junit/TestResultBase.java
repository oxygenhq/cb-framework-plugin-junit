package io.cloudbeat.junit;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.cloudbeat.junit.serializers.DateSerializer;

import java.util.Date;

public abstract class TestResultBase {
    public String name;
    public ResultStatus status;
    @JsonSerialize(using = DateSerializer.class)
    public Date startTime;
    @JsonSerialize(using = DateSerializer.class)
    public Date endTime;
    public long duration;
}
