package com.jenkins.plugins.sparknotify.services;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import com.cloudbees.plugins.credentials.Credentials;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.jenkins.plugins.sparknotify.SparkNotifyException;
import com.jenkins.plugins.sparknotify.beans.SparkMessage;
import com.jenkins.plugins.sparknotify.beans.SparkMessage.SparkMessageBuilder;
import com.jenkins.plugins.sparknotify.enums.SparkMessageType;

import hudson.EnvVars;

public class SparkNotifier {
	private static final String SPARK_MSG_POST_URL = "https://api.ciscospark.com/v1/messages";
	private static final Pattern ENV_PATTERN_WORKFLOW = Pattern.compile("\\$\\{env\\.(.+?)\\}");
	private static final Client DEFAULT_CLIENT = ClientBuilder.newBuilder().register(JacksonJsonProvider.class).build();

	private final Credentials credentials;
	private final EnvVars env;

	public SparkNotifier(final Credentials credentials, final EnvVars env) {
		this.credentials = credentials;
		this.env = env;
	}

	public Response sendMessage(final String roomId, String message, final SparkMessageType messageType) throws IOException {
		if (env != null) {
			message = replaceEnvVars(message, env);
		}

		SparkMessage messageData = new SparkMessageBuilder().roomId(roomId)
				.message(message)
				.messageType(messageType)
				.build();

		return DEFAULT_CLIENT.target(SPARK_MSG_POST_URL)
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + getToken())
				.post(Entity.json(messageData));
	}

	private String getToken() throws SparkNotifyException {
		if (credentials == null) {
			throw new SparkNotifyException("No credentials found");
		}
		if (credentials instanceof StringCredentials) {
			StringCredentials tokenCredential = (StringCredentials) credentials;
			String token = tokenCredential.getSecret().getPlainText();
			if (token == null || token.isEmpty()) {
				throw new SparkNotifyException("Token cannot be null");
			}
			return token;
		} else {
			throw new SparkNotifyException("Invalid credential type; only use 'Secret text' (token)");
		}
	}

	private String replaceEnvVars(String message, final EnvVars env) {
		// Normal and ${env.VAR} matching for pipeline consistency

		Matcher workflowMatcher = ENV_PATTERN_WORKFLOW.matcher(message);
		while (workflowMatcher.find()) {
			String var = workflowMatcher.group(1);
			message = message.replace("${env." + var + "}", env.get(var, ""));
		}

		return env.expand(message);
	}
}
