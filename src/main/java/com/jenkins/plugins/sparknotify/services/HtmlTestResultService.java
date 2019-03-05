package com.jenkins.plugins.sparknotify.services;

import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.jenkins.plugins.sparknotify.beans.TestResult;
import com.jenkins.plugins.sparknotify.utils.MessageConst;
import com.jenkins.plugins.sparknotify.utils.MessageFormatUtils;

import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;

public class HtmlTestResultService implements TestResultService {
	private static final Logger LOG = Logger.getLogger(HtmlTestResultService.class.getName());

	private final TestResult testResult;
	private final int maxMessageCharLimit;

	public HtmlTestResultService(final TestResult testResult, final int maxMessageCharLimit) {
		this.testResult = testResult;
		this.maxMessageCharLimit = maxMessageCharLimit;
	}

	@Override
	public String appendMessageWithTestInformation(final String message) {
		if (testResult.getTestResultAction() == null) {
			return StringUtils.EMPTY;
		}

		String preMessage = StringUtils.isNotEmpty(message) ? message + MessageConst.HTML_LINE_BREAK : StringUtils.EMPTY;

		TestResultAction testResults = testResult.getTestResultAction();
		StringBuilder titleAndSummary = new StringBuilder(90);

		titleAndSummary.append(MessageFormatUtils.convertToHtmlLink(MessageConst.TEST_RESULT_URL, MessageFormatUtils.convertToHtmlBold(MessageConst.TEST_RESULT_TITLE)));

		if (testResult.isTestSummary()) {
			titleAndSummary.append(" Failed: ").append(testResults.getFailCount());
			titleAndSummary.append(", Skipped: ").append(testResults.getSkipCount());
			titleAndSummary.append(", Total: ").append(testResults.getTotalCount());
		}

		StringBuilder results = new StringBuilder();
		if (testResult.isTestDetails()) {
			if (CollectionUtils.isNotEmpty(testResults.getFailedTests())) {
				for (CaseResult testResult : testResults.getFailedTests()) {
					results.append(MessageFormatUtils.formatTestResultToHtml(testResult));
				}
			}
		}

		String messageWithTestInfo = preMessage + titleAndSummary + results;
		if (maxMessageCharLimit != 0 && messageWithTestInfo.length() > maxMessageCharLimit) {
			LOG.warning("Message including test details exceeds character limit; size=" + messageWithTestInfo.length() + ", limit=" + maxMessageCharLimit);

			String errorMessage = preMessage + titleAndSummary + MessageConst.HTML_LINE_BREAK + MessageConst.MESSAGE_TOO_BIG_WITH_FACE;
			if (errorMessage.length() > maxMessageCharLimit) {
				LOG.warning("Message including test summary exceeds character limit; size=" + errorMessage.length() + ", limit=" + maxMessageCharLimit);
				return message;
			}

			return errorMessage;
		}

		return messageWithTestInfo;
	}
}
