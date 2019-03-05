package com.jenkins.plugins.sparknotify.services;

import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.jenkins.plugins.sparknotify.beans.TestResult;
import com.jenkins.plugins.sparknotify.utils.MessageConst;

import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;

public class TextTestResultService implements TestResultService {
	private static final Logger LOG = Logger.getLogger(TextTestResultService.class.getName());

	private static final String RESULTS_URL = MessageConst.OPEN_PARENS + MessageConst.TEST_RESULT_URL + MessageConst.CLOSE_PARENS;

	private final TestResult testResult;
	private final int maxMessageCharLimit;

	public TextTestResultService(final TestResult testResult, final int maxMessageCharLimit) {
		this.testResult = testResult;
		this.maxMessageCharLimit = maxMessageCharLimit;
	}

	@Override
	public String appendMessageWithTestInformation(final String message) {
		if (testResult.getTestResultAction() == null) {
			return StringUtils.EMPTY;
		}

		String preMessage = StringUtils.isNotEmpty(message) ? message + MessageConst.LINE_BREAK : StringUtils.EMPTY;

		TestResultAction testResults = testResult.getTestResultAction();
		StringBuilder titleAndSummary = new StringBuilder(90);

		titleAndSummary.append(MessageConst.TEST_RESULT_TITLE);

		if (testResult.isTestSummary()) {
			titleAndSummary.append(" Failed: ").append(testResults.getFailCount());
			titleAndSummary.append(", Skipped: ").append(testResults.getSkipCount());
			titleAndSummary.append(", Total: ").append(testResults.getTotalCount());
			titleAndSummary.append(MessageConst.SPACE).append(RESULTS_URL);
		}

		StringBuilder results = new StringBuilder();
		if (testResult.isTestDetails()) {
			if (CollectionUtils.isNotEmpty(testResults.getFailedTests())) {
				for (CaseResult testResult : testResults.getFailedTests()) {
					results.append(MessageConst.LINE_BREAK);
					results.append(testResult.getFullDisplayName());
					results.append(MessageConst.LINE_BREAK);
					results.append(testResult.getErrorDetails());
				}
			}
		}

		String messageWithTestInfo = preMessage + titleAndSummary + results;
		if (maxMessageCharLimit != 0 && messageWithTestInfo.length() > maxMessageCharLimit) {
			LOG.warning("Message including test details exceeds character limit; size=" + messageWithTestInfo.length() + ", limit=" + maxMessageCharLimit);

			String errorMessage = preMessage + titleAndSummary + MessageConst.LINE_BREAK + MessageConst.MESSAGE_TOO_BIG;
			if (errorMessage.length() > maxMessageCharLimit) {
				LOG.warning("Message including test summary exceeds character limit; size=" + errorMessage.length() + ", limit=" + maxMessageCharLimit);
				return message;
			}

			return errorMessage;
		}

		return messageWithTestInfo;
	}
}
