/*
 * Copyright (c) 2016, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025 JetBrains s.r.o. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.plugin.jtreg.runtime;

import com.sun.javatest.Harness;
import com.sun.javatest.Parameters;
import com.sun.javatest.Status;
import com.sun.javatest.TestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestListener implements Harness.Observer {

    private static final int PASSED = 0;
    private static final int FAILED = 1;
    private static final int ERROR = 2;
    private static final int NOT_RUN = 3;

    @Override
    public void startingTestRun(Parameters parameters) {
        System.out.println("##teamcity[testSuiteStarted name='jtreg']");
    }

    @Override
    public void startingTest(TestResult testResult) {
        final String presentationName = getPresentationName(testResult);
        String location = "";
        try {
            location = "locationHint='file://" + testResult.getDescription().getFile().getCanonicalPath() + "'";
        } catch (TestResult.Fault | IOException e) {
            //do nothing (leave location empty)
        }

        final String repeatMode = getProperty(testResult, "repeatMode", "once");
        final int executionNumber = getIntProperty(testResult, "executionNumber", 0);
        if (!repeatMode.equalsIgnoreCase("once") && executionNumber == 1) {
            final String baseTestName = escapeName(testResult.getTestName());
            System.out.println("##teamcity[testSuiteStarted name='" + baseTestName + "']");
        }

        System.out.println("##teamcity[testStarted name='" + presentationName + "' " + location + "]");
    }

    @Override
    public void finishedTest(TestResult testResult) {
        Status status = testResult.getStatus();
        int statusType = status.getType();
        File file = testResult.getFile();
        String statusReason = status.getReason();
        String elapsed = getProperty(testResult, "elapsed", "0");
        String presentationName = getPresentationName(testResult);
        if (statusType == FAILED || statusType == ERROR) {
            if (file.isFile()) {
                final String output = loadText(file);
                if (!output.isEmpty()) {
                    System.out.println("##teamcity[testStdOut name='" + presentationName + "' " +
                            "out='" + escapeName(output) + "']");
                }
            }
            System.out.println("##teamcity[testFailed name='" + presentationName + "' " +
                    "message='" + escapeName(statusReason) + "']");
        } else if (statusType == NOT_RUN) {
            System.out.println("##teamcity[testIgnored name='" + presentationName + "']");
        }

        String duration = elapsed.split(" ")[0];
        System.out.println("##teamcity[testFinished name='" + presentationName + "' " +
                (!duration.equals("0") ? "duration='" + duration : "") + "'" +
                (statusType != FAILED ? "outputFile='" + escapeName(file.getAbsolutePath()) + "'" : "") +
                " ]");

        final String repeatMode = getProperty(testResult, "repeatMode", "once");
        boolean passCondition = repeatMode.equalsIgnoreCase("until_success") && statusType == PASSED;
        boolean failCondition = repeatMode.equalsIgnoreCase("until_failure") && statusType == FAILED;
        int maxRepeatCount = getIntProperty(testResult, "maxRepeatCount", -1);
        int executionNumber = getIntProperty(testResult, "executionNumber", 0);
        boolean isLastNRun = repeatMode.equalsIgnoreCase("n") && (maxRepeatCount == executionNumber);
        if (passCondition || failCondition || isLastNRun) {
            final String baseTestName = escapeName(testResult.getTestName());
            System.out.println("##teamcity[testSuiteFinished name='" + baseTestName + "']");
        }
    }

    @Override
    public void stoppingTestRun() {
        //do nothing
    }

    @Override
    public void finishedTestRun(boolean b) {
        System.out.println("##teamcity[testSuiteFinished name='jtreg']");
    }

    @Override
    public void error(String s) {
        System.out.println(s);
    }

    private static String escapeName(String str) {
        return MapSerializerUtil.escapeStr(str);
    }

    private static String loadText(File file) {
        try {
            return String.join("\n", Files.readAllLines(file.toPath()));
        } catch (IOException e) {
            return "Failed to load test results.";
        }
    }

    private static String getProperty(TestResult testResult, String name, String defaultValue) {
        try {
            String value = testResult.getProperty(name);
            return value == null || value.isEmpty() ? defaultValue : value;
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    private static int getIntProperty(TestResult testResult, String name, int defaultValue) {
        try {
            return Integer.parseInt(testResult.getProperty(name));
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    private static String getPresentationName(TestResult testResult) {
        String executionNumber = getProperty(testResult, "executionNumber", null);
        if (executionNumber != null && !executionNumber.equals("0")) {
            return escapeName(testResult.getTestName() + " run #" + executionNumber);
        } else {
            return escapeName(testResult.getTestName());
        }
    }

}
