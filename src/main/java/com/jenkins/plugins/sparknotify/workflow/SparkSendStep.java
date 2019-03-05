package com.jenkins.plugins.sparknotify.workflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.jenkins.plugins.sparknotify.Messages;
import com.jenkins.plugins.sparknotify.beans.SparkMessage;
import com.jenkins.plugins.sparknotify.beans.SparkSpace;
import com.jenkins.plugins.sparknotify.enums.SparkMessageType;
import com.jenkins.plugins.sparknotify.services.SparkNotifier;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;

public class SparkSendStep extends AbstractStepImpl {

	private final String message;
	private String messageType;
	private final String credentialsId;
	private List<SparkSpace> spaceList;
	private boolean disable;
	private boolean failOnError;

	public String getMessage() {
		return message;
	}

	public List<SparkSpace> getSpaceList() {
		if (spaceList == null) {
			spaceList = new ArrayList<>();
		}
		return spaceList;
	}

	public String getCredentialsId() {
		return credentialsId;
	}

	public String getMessageType() {
		return messageType;
	}

	@DataBoundSetter
	public void setMessageType(final String messageType) {
		this.messageType = messageType;
	}

	public boolean isDisable() {
		return disable;
	}

	@DataBoundSetter
	public void setDisable(final boolean disable) {
		this.disable = disable;
	}

	public boolean isFailOnError() {
		return failOnError;
	}

	@DataBoundSetter
	public void setFailOnError(final boolean failOnError) {
		this.failOnError = failOnError;
	}

	@DataBoundConstructor
	public SparkSendStep(final String message, final List<SparkSpace> spaceList, final String credentialsId) {
		this.message = message;
		this.spaceList = spaceList;
		this.credentialsId = credentialsId;
	}

	public static class SparkSendStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

		private static final long serialVersionUID = 1L;

		@Inject
		transient SparkSendStep step;

		@StepContextParameter
		transient EnvVars envVars;

		@StepContextParameter
		transient TaskListener listener;

		@Override
		protected Void run() throws IOException, InterruptedException {
			if (step.disable) {
				listener.getLogger().println(Messages.Disabled());
				return null;
			}

			if (!SparkMessage.isMessageValid(step.getMessage())) {
				if (step.failOnError) {
					throw new AbortException(Messages.MessageNotNull());
				}
				listener.getLogger().println(Messages.SkipNoMessage());
				return null;
			}
			if (CollectionUtils.isEmpty(step.spaceList)) {
				if (step.failOnError) {
					throw new AbortException(Messages.SpacesNotEmpty());
				}
				listener.getLogger().println(Messages.SkipNoSpaces());
				return null;
			}
			if (StringUtils.isEmpty(step.messageType)) {
				step.messageType = "text";
			}

			SparkMessageType sparkMessageType = SparkMessageType.valueOf(step.messageType.toUpperCase());

			SparkNotifier notifier = new SparkNotifier(getCredentials(step.credentialsId, getContext().get(Run.class)), envVars);

			for (SparkSpace space : step.spaceList) {
				listener.getLogger().println(Messages.SendingMessage(space.getSpaceId()));
				try {
					Response response = notifier.sendMessage(space.getSpaceId(), step.getMessage(), sparkMessageType);
					if (response.getStatus() != Status.OK.getStatusCode()) {
						String error = Messages.ErrorHttpResponse(response.getStatus(), response.readEntity(String.class));
						if (step.failOnError) {
							throw new AbortException(error);
						}
						listener.error(error);
					} else {
						listener.getLogger().println(Messages.MessageSent());
					}
				} catch (IOException | RuntimeException e) {
					if (step.failOnError) {
						throw new AbortException(Messages.ErrorException(e));
					}
					listener.error(Messages.ErrorException(e));
				}
			}

			return null;
		}

		private Credentials getCredentials(final String credentialsId, final Run<?, ?> run) {
			return CredentialsProvider.findCredentialById(credentialsId, StringCredentials.class, run);
		}
	}

	@Extension
	public static class DescriptorImpl extends AbstractStepDescriptorImpl {

		public DescriptorImpl() {
			super(SparkSendStepExecution.class);
		}

		@Override
		public String getFunctionName() {
			return Messages.StepName();
		}

		@Override
		public String getDisplayName() {
			return Messages.DisplayName();
		}

		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Item owner) {
			if (owner == null || !owner.hasPermission(Item.CONFIGURE)) {
				return new ListBoxModel();
			}
			return new StandardListBoxModel().withEmptySelection().withMatching(
					CredentialsMatchers.instanceOf(StringCredentials.class), CredentialsProvider.lookupCredentials(
							StringCredentials.class, owner, ACL.SYSTEM, Collections.<DomainRequirement> emptyList()));
		}

		public ListBoxModel doFillMessageTypeItems(@QueryParameter final String messageType) {
			return new ListBoxModel(new Option("text", "text", messageType.matches("text")),
					new Option("markdown", "markdown", messageType.matches("markdown")),
					new Option("html", "html", messageType.matches("html")));
		}

		public FormValidation doMessageCheck(@QueryParameter final String message) {
			if (SparkMessage.isMessageValid(message)) {
				return FormValidation.ok();
			} else {
				return FormValidation.error(Messages.InvalidMessage());
			}
		}

		public FormValidation doSpaceIdCheck(@QueryParameter final String spaceId) {
			if (SparkMessage.isRoomIdValid(spaceId)) {
				return FormValidation.ok();
			} else {
				return FormValidation.error(Messages.InvalidSpaceId());
			}
		}
	}
}
