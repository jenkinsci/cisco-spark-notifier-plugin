package com.jenkins.plugins.sparknotify.utils;

import hudson.tasks.junit.CaseResult;

public class MessageFormatUtils {

	public static String convertToMarkdownBold(final String s) {
		return MessageConst.MARKDOWN_BOLD + s + MessageConst.MARKDOWN_BOLD;
	}

	public static String convertToHtmlBold(final String s) {
		return MessageConst.HTML_BOLD_START + s + MessageConst.HTML_BOLD_END;
	}

	public static String convertToMarkdownLink(final String url, final String title) {
		return MessageConst.OPEN_BRACKET + title + MessageConst.CLOSE_BRACKET + MessageConst.OPEN_PARENS + url + MessageConst.CLOSE_PARENS;
	}

	public static String convertToHtmlLink(final String url, final String title) {
		return MessageConst.HTML_HYPERLINK_START + url + MessageConst.GREATER_THAN + title + MessageConst.HTML_HYPERLINK_END;
	}

	public static String convertToHtmlBlockquoteDanger(final String s) {
		return MessageConst.HTML_BLOCKQUOTE_DANGER_START + s + MessageConst.HTML_BLOCKQUOTE_END;
	}

	public static String convertToHtmlItalic(final String s) {
		return MessageConst.HTML_ITALIC_START + s + MessageConst.HTML_ITALIC_END;
	}

	public static String formatTestResultToHtml(final CaseResult testResult) {
		return convertToHtmlBlockquoteDanger(convertToHtmlItalic(testResult.getFullDisplayName()) + MessageConst.HTML_LINE_BREAK + testResult.getErrorDetails());
	}

}
