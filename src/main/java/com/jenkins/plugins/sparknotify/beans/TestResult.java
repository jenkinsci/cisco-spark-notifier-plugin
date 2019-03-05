package com.jenkins.plugins.sparknotify.beans;

import hudson.tasks.junit.TestResultAction;

public class TestResult {

	private TestResultAction testResultAction;
	private boolean testSummary;
	private boolean testDetails;

	public TestResult(final TestResultAction testResultAction, final boolean testSummary, final boolean testDetails) {
		super();
		this.testResultAction = testResultAction;
		this.testSummary = testSummary;
		this.testDetails = testDetails;
	}

	public TestResultAction getTestResultAction() {
		return testResultAction;
	}

	public void setTestResultAction(final TestResultAction testResultAction) {
		this.testResultAction = testResultAction;
	}

	public boolean isTestSummary() {
		return testSummary;
	}

	public void setTestSummary(final boolean testSummary) {
		this.testSummary = testSummary;
	}

	public boolean isTestDetails() {
		return testDetails;
	}

	public void setTestDetails(final boolean testDetails) {
		this.testDetails = testDetails;
	}

}
