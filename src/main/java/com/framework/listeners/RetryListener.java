package com.framework.listeners;

import com.framework.utils.RetryAnalyzer;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * {@link IAnnotationTransformer} that automatically attaches
 * {@link RetryAnalyzer} to every {@code @Test} method at runtime.
 * <p>
 * Retry behavior (enabled/disabled, count) is controlled entirely via the
 * {@code retry.count} config property, so tests do not need annotations.
 */
public class RetryListener implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        if (annotation.getRetryAnalyzerClass() == null) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
