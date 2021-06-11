package io.cloudbeat.junit;

import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.reporter.CbTestReporter;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

public class JunitReporterUtils {
    public static final String JAVA_METHOD_FQN_FORMAT = "%s#%s";

    public static  void startInstance(CbTestReporter reporter) {
        //CbTestReporter reporter = CbTestContext.getReporter();
        if (!reporter.getInstance().isPresent())
            reporter.startInstance();
    }

    public static  void endInstance(CbTestReporter reporter) {
        //CbTestReporter reporter = CbTestContext.getReporter();
        if (reporter.getInstance().isPresent())
            reporter.endInstance();
    }

    public static void startSuite(CbTestReporter reporter, ExtensionContext context) {
        final String classFqn = context.getTestClass().get().getName();
        final String classDisplayName = context.getTestClass().get().getSimpleName();
        //CbTestReporter reporter = CbTestContext.getReporter();
        reporter.startSuite(classDisplayName, classFqn);
        System.out.println("startSuite: " + context.getTestClass().get().getName());
    }

    public static void endSuite(CbTestReporter reporter, ExtensionContext context) {
        final String classFqn = context.getTestClass().get().getName();
        //CbTestReporter reporter = CbTestContext.getReporter();
        reporter.endSuite(classFqn);
        /*if (context.getStore(NAMESPACE).get(FAILED) == null) {
            finishTestItem(context);
        } else {
            finishTestItem(context, FAILED);
            context.getParent().ifPresent(p -> p.getStore(NAMESPACE).put(FAILED, Boolean.TRUE));
        }*/
    }

    public static void startCase(CbTestReporter reporter, ExtensionContext context) throws Exception {
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);
        //CbTestReporter reporter = CbTestContext.getReporter();
        reporter.startCase(methodName, methodFqn, classFqn);
    }

    public static void endCase(CbTestReporter reporter, ExtensionContext context) throws Exception {
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);
        //CbTestReporter reporter = CbTestContext.getReporter();
        if (context.getExecutionException().isPresent())
            reporter.failCase(methodFqn, context.getExecutionException().get());
        else
            reporter.passCase(methodFqn);
    }

    public static void disabledCase(CbTestReporter reporter, ExtensionContext context, Optional<String> reason) throws Exception {
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);
        //CbTestReporter reporter = CbTestContext.getReporter();
        reporter.startCase(methodName, methodFqn, classFqn);
        reporter.skipCase(methodFqn, classFqn);
    }
    public static void failedCase(
            CbTestReporter reporter,
            ExtensionContext context,
            Throwable throwable
    ) throws Exception{
        final String classFqn = context.getTestClass().get().getName();
        final String methodName = context.getTestMethod().get().getName();
        final String methodFqn = String.format(JAVA_METHOD_FQN_FORMAT, classFqn, methodName);

        // failed case can be either called be after method hook or by testFailed method
        // if it was called by "after method hook", then we should already have a case started
        if (reporter.getLastStep() != null
                && reporter.getLastStep().getFqn() != null
                && reporter.getLastStep().getFqn().equals(methodFqn)
        ) {
            reporter.failStep(methodFqn, throwable);
        }
        // otherwise, start and end case with failure
        else {
            reporter.startCase(methodName, methodFqn, classFqn);
            reporter.failCase(methodFqn, throwable);
        }
    }

    public static void startBeforeEachHook(CbTestReporter reporter, ExtensionContext context) {
    }

    public static void endBeforeEachHook(CbTestReporter reporter, ExtensionContext context) {
    }


}
