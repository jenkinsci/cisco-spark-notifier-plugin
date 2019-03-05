package com.jenkins.plugins.sparknotify.beans;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

import com.jenkins.plugins.sparknotify.SparkNotifyException;
import com.jenkins.plugins.sparknotify.enums.SparkMessageType;

public class SparkMessage {
	private final static String ROOM_ID_PREFIX = "ciscospark://us/ROOM/";

	private String roomId;
	private String text;
	private String markdown;
	private String html;

	public String getRoomId() {
		return roomId;
	}

	public String getText() {
		return text;
	}

	public String getHtml() {
		return html;
	}

	public String getMarkdown() {
		return markdown;
	}

	private void setRoomId(final String roomId) {
		this.roomId = roomId;
	}

	private void setMessage(final String message, final SparkMessageType messageType) throws SparkNotifyException {
		switch (messageType) {
		case TEXT:
			text = message;
			break;
		case MARKDOWN:
			markdown = message;
			break;
		case HTML:
			html = message;
			break;
		default:
			throw new SparkNotifyException("Could not find message type. This shouldn't happen.");
		}
	}

	public static boolean isMessageValid(final String message) {
		return message != null && !message.isEmpty();
	}

	public static boolean isRoomIdValid(final String roomId) {
		try {
			UUID.fromString(roomId);
			return true;
		} catch (IllegalArgumentException e1) {
			String roomIdDecodedFull = null;
			try {
				Base64 base64 = new Base64();
				roomIdDecodedFull = new String(base64.decode(roomId), StandardCharsets.UTF_8);
				if (!roomIdDecodedFull.startsWith(ROOM_ID_PREFIX)) {
					return false;
				}
				UUID.fromString(roomIdDecodedFull.substring(ROOM_ID_PREFIX.length() + 1));
				return true;
			} catch (IllegalArgumentException e2) {
				return false;
			}
		}
	}

	public static class SparkMessageBuilder {

		private String roomId;
		private String message;
		private SparkMessageType messageType;

		public SparkMessageBuilder() {}

		public SparkMessageBuilder roomId(final String roomId) {
			this.roomId = roomId;
			return this;
		}

		public SparkMessageBuilder message(final String message) {
			this.message = message;
			return this;
		}

		public SparkMessageBuilder messageType(final SparkMessageType messageType) {
			this.messageType = messageType;
			return this;
		}

		public SparkMessage build() throws SparkNotifyException {
			SparkMessage sparkMessage = new SparkMessage();
			sparkMessage.setRoomId(roomId);
			sparkMessage.setMessage(message, messageType);
			return sparkMessage;
		}
	}
}
