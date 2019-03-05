package com.jenkins.plugins.sparknotify.enums;

public enum SparkMessageType {
	TEXT("text"),
	MARKDOWN("markdown"),
	HTML("html");

	private final String text;

	SparkMessageType(final String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}
};
