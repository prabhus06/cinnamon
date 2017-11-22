package io.magentys.cinnamon.cucumber;

import cucumber.runtime.junit.ExecutionUnitRunner;
import cucumber.runtime.junit.FeatureRunner;

import gherkin.formatter.Reporter;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Step;
import io.magentys.cinnamon.cucumber.events.*;
import io.magentys.cinnamon.eventbus.EventBusContainer;
import io.magentys.cinnamon.reportium.ReportiumLogger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Aspect
public class CucumberAspect {

    private static final ThreadLocal<String> featureName = new ThreadLocal<>();
    private static final ThreadLocal<String> scenarioName = new ThreadLocal<>();
    private static final ThreadLocal<Reporter> reporter = new ThreadLocal<>();
    private static final ThreadLocal<List<Result>> results = new ThreadLocal<>();
    private static final ThreadLocal<Step> stepName = new ThreadLocal<>();

    /**
     * Pointcut for <code>cucumber.api.junit.Cucumber.run</code> method.
     */
    @Pointcut("execution(public * cucumber.api.junit.Cucumber.run(..))")
    public void runCucumber() {
        // pointcut body must be empty
    }

    /**
     * Pointcut for <code>cucumber.runtime.junit.FeatureRunner.FeatureRunner.run</code> method.
     */
    @Pointcut("execution(public * cucumber.runtime.junit.FeatureRunner.run(..))")
    public void runFeature() {
        // pointcut body must be empty
    }

    /**
     * Pointcut for <code>cucumber.runtime.junit.ExecutionUnitRunner.run</code> method.
     */
    @Pointcut("execution(public * cucumber.runtime.junit.ExecutionUnitRunner.run(..))")
    public void runScenario() {
        // pointcut body must be empty
    }

    /**
     * Pointcut for <code>cucumber.runtime.Runtime.buildBackendWorlds</code> method.
     */
    @Pointcut("execution(public void cucumber.runtime.Runtime.buildBackendWorlds(..))")
    public void buildBackendWorlds() {
        // pointcut body must be empty
    }

    /**
     * Pointcut for <code>cucumber.runtime.Runtime.runBeforeHooks</code> method.
     */
    @Pointcut("execution(public void cucumber.runtime.Runtime.runBeforeHooks(..))")
    public void runBeforeHooks() {
        // pointcut body must be empty
    }

    @Pointcut("execution(* cucumber.runtime.Runtime.addHookToCounterAndResult(..))")
    public void addHookToCounterAndResult() {
        // pointcut body must be empty
    }

    /**
     * Pointcut for <code>cucumber.runtime.Runtime.runStep</code> method.
     */
    @Pointcut("execution(public void cucumber.runtime.Runtime.runStep(..))")
    public void runStep() {
        // pointcut body must be empty
    }

    /**
     * Pointcut for <code>cucumber.runtime.Runtime.addStepToCounterAndResult</code> method.
     */
    @Pointcut("execution(* cucumber.runtime.Runtime.addStepToCounterAndResult(..))")
    public void addStepToCounterAndResult() {
        // pointcut body must be empty
    }

    /**
     * Pointcut for <code>cucumber.runtime.Runtime.runAfterHooks</code> method.
     */
    @Pointcut("execution(public void cucumber.runtime.Runtime.runAfterHooks(..))")
    public void runAfterHooks() {
        // pointcut body must be empty
    }

    /**
     * Pointcut for <code>cucumber.runtime.Runtime.disposeBackendWorlds</code> method.
     */
    @Pointcut("execution(public void cucumber.runtime.Runtime.disposeBackendWorlds(..))")
    public void disposeBackendWorlds() {
        // pointcut body must be empty
    }

    @Before("runFeature()")
    public void beforeRunFeature(JoinPoint joinPoint) {
        FeatureRunner featureRunner = (FeatureRunner) joinPoint.getTarget();
        CucumberAspect.featureName.set(featureRunner.getName());
        EventBusContainer.getEventBus().post(new BeforeFeatureScenario(featureRunner.getName()));
    }

    @Before("runScenario()")
    public void beforeRunScenario(JoinPoint joinPoint) {
        ExecutionUnitRunner executionUnitRunner = (ExecutionUnitRunner) joinPoint.getTarget();
        CucumberAspect.scenarioName.set(executionUnitRunner.getName());
        EventBusContainer.getEventBus().post(new BeforeScenarioEvent(executionUnitRunner.getName(), executionUnitRunner.getDescription().toString()));
    }

    @Before("buildBackendWorlds() && args(reporter,..)")
    public void beforeBuildBackendWorlds(Reporter reporter) {
        CucumberAspect.reporter.set(reporter);
    }

    @After("buildBackendWorlds()")
    public void afterBuildBackendWorlds() {
        CucumberAspect.results.set(new ArrayList<>());
    }

    @After("addStepToCounterAndResult() && args(result,..)")
    public void afterAddStepToCounterAndResult(Result result) {
        CucumberAspect.results.get().add(result);
        EventBusContainer.getEventBus().post(new StepFinishedEvent(result, reporter.get(), result.getErrorMessage(), result.getError()));
    }

    @After("addHookToCounterAndResult() && args(result,..)")
    public void afterAddHookToCounterAndResult(Result result) {
        EventBusContainer.getEventBus().post(new BeforeHooksFinishedEvent(result, result.getErrorMessage(), result.getError()));
    }

    @After("runAfterHooks()")
    public void afterRunAfterHooks() {
        EventBusContainer.getEventBus().post(new AfterHooksFinishedEvent());
    }

    @After("disposeBackendWorlds()")
    public void afterDisposeBackendWorlds() {
        EventBusContainer.getEventBus().post(new ScenarioFinishedEvent(CucumberAspect.results.get()));
    }

    //TODO this would need to move to reportium module
    @Before("runCucumber()")
    public void beforeRunCucumber(JoinPoint joinPoint) {
//        EventBusContainer.getEventBus().register(new ReportiumLogger());
    }

    @Before("runStep()")
    public void beforeRunStep(JoinPoint joinPoint) {
        EventBusContainer.getEventBus().post(new AfterStepEvent(((Step)joinPoint.getArgs()[1]).getName()));
    }

    @After("runCucumber()")
    public void afterRunCucumber() {
        try {
            EventBusContainer.getEventBus().post(new CucumberFinishedEvent());
        } finally {
            reporter.remove();
            results.remove();
            scenarioName.remove();
            featureName.remove();
        }
    }
}
