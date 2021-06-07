package io.cloudbeat.junit;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.reporter.CbTestReporter;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

public class JunitReporterUtils {
    public static  void startInstance() {
        CbTestReporter reporter = CbTestContext.getReporter();
        if (!reporter.getInstance().isPresent())
            reporter.startInstance();
    }

    public static  void endInstance() {
        CbTestReporter reporter = CbTestContext.getReporter();
        if (reporter.getInstance().isPresent())
            reporter.endInstance();
    }

    public static void startSuite(ExtensionContext context) {
        final String classFqn = context.getTestClass().get().getName();
        final String classDisplayName = context.getTestClass().get().getSimpleName();
        CbTestReporter reporter = CbTestContext.getReporter();
        reporter.startSuite(classDisplayName, classFqn);
        System.out.println("startSuite: " + context.getTestClass().get().getName());
    }

    public static void endSuite(ExtensionContext context) {
        final String classFqn = context.getTestClass().get().getName();
        CbTestReporter reporter = CbTestContext.getReporter();
        reporter.endSuite(classFqn);
        /*if (context.getStore(NAMESPACE).get(FAILED) == null) {
            finishTestItem(context);
        } else {
            finishTestItem(context, FAILED);
            context.getParent().ifPresent(p -> p.getStore(NAMESPACE).put(FAILED, Boolean.TRUE));
        }*/
    }

    public static void startCase(ExtensionContext context) throws Exception {
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format("%s.%s", classFqn, methodName);
        CbTestReporter reporter = CbTestContext.getReporter();
        reporter.startCase(methodName, methodFqn);
    }

    public static void endCase(ExtensionContext context) throws Exception {
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format("%s.%s", classFqn, methodName);
        CbTestReporter reporter = CbTestContext.getReporter();
        if (context.getExecutionException().isPresent())
            reporter.failCase(methodFqn, classFqn, context.getExecutionException().get());
        else
            reporter.passCase(methodFqn, classFqn);
    }

    public static void disabledCase(ExtensionContext context, Optional<String> reason) throws Exception {
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format("%s.%s", classFqn, methodName);
        CbTestReporter reporter = CbTestContext.getReporter();
        reporter.startCase(methodName, methodFqn);
        reporter.skipCase(methodFqn, classFqn);
    }
}
