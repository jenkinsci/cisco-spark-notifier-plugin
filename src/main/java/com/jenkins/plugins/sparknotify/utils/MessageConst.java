package com.jenkins.plugins.sparknotify.utils;

public class MessageConst {

	public static final String BUILD_RESULT = "${BUILD_RESULT}";
	public static final String BUILD_URL = "${BUILD_URL}";

	public static final String TEST_RESULT_TITLE = "Test Results:";
	public static final String TEST_RESULT_URL = BUILD_URL + "/testReport";
	public static final String MESSAGE_TOO_BIG = "Details exceeded maximum message size; please check jenkins";

	public static final String OPEN_BRACKET = "[";
	public static final String CLOSE_BRACKET = "]";
	public static final String OPEN_PARENS = "(";
	public static final String CLOSE_PARENS = ")";
	public static final String GREATER_THAN = ">";
	public static final String SPACE = " ";
	public static final String LINE_BREAK = "\n";

	public static final String HTML_BLOCKQUOTE_DANGER_START = "<blockquote class=\"danger\">";
	public static final String HTML_BLOCKQUOTE_END = "</blockquote>";
	public static final String HTML_ITALIC_START = "<i>";
	public static final String HTML_ITALIC_END = "</i>";
	public static final String HTML_BOLD_START = "<b>";
	public static final String HTML_BOLD_END = "</b>";
	public static final String HTML_HYPERLINK_START = "<a href=";
	public static final String HTML_HYPERLINK_END = "</a>";
	public static final String HTML_LINE_BREAK = "<br>";

	public static final String MARKDOWN_BOLD = "**";
	public static final String MARKDOWN_LINE_BREAK = "  \n";

	public static final String HTML_X_FACE_DEC = "&#128565;";
	public static final String MESSAGE_TOO_BIG_WITH_FACE = HTML_X_FACE_DEC + SPACE + MESSAGE_TOO_BIG;

}
