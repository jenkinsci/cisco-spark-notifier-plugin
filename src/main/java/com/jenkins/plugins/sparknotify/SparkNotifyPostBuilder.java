package com.jenkins.plugins.sparknotify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.jenkins.plugins.sparknotify.beans.SparkMessage;
import com.jenkins.plugins.sparknotify.beans.TestResult;
import com.jenkins.plugins.sparknotify.enums.SparkMessageType;
import com.jenkins.plugins.sparknotify.services.HtmlTestResultService;
import com.jenkins.plugins.sparknotify.services.MarkdownTestResultService;
import com.jenkins.plugins.sparknotify.services.SparkNotifier;
import com.jenkins.plugins.sparknotify.services.TestResultService;
import com.jenkins.plugins.sparknotify.services.TextTestResultService;
import com.jenkins.plugins.sparknotify.utils.MessageConst;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.junit.TestResultAction;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import net.sf.json.JSONObject;

public class SparkNotifyPostBuilder extends Notifier {
	private static final Logger LOG = Logger.getLogger(SparkNotifyPostBuilder.class.getName());

	// the maximum spark message size is 7439 characters before encrypted, 10000 characters after encrypted
	private static final int MAXIUMUM_TEST_RESULT_SIZE = 7250;

	private List<SparkRoom> roomList;
	private final boolean disable;
	private final boolean skipOnFailure;
	private final boolean skipOnSuccess;
	private final boolean skipOnAborted;
	private final boolean skipOnUnstable;
	private String message;
	private String messageType;
	private String messageContent;
	private String credentialsId;
	private boolean skipOnEmptyTest;
	private boolean testSummary;
	private boolean testDetails;

	/**
	 * @deprecated Backwards compatibility; please use SparkSpace
	 */
	@Deprecated
	public static final class SparkRoom extends AbstractDescribableImpl<SparkRoom> {
		private final String rName;
		private final String rId;

		public String getRName() {
			return rName;
		}

		public String getRId() {
			return rId;
		}

		@DataBoundConstructor
		public SparkRoom(final String rName, final String rId) {
			this.rName = rName;
			this.rId = rId;
		}

		@Extension
		public static class DescriptorImpl extends Descriptor<SparkRoom> {
			@Override
			public String getDisplayName() {
				return "";
			}
		}
	}

	@DataBoundConstructor
	public SparkNotifyPostBuilder(final boolean disable, final boolean skipOnFailure, final boolean skipOnSuccess,
			final boolean skipOnAborted, final boolean skipOnUnstable, final String messageContent,
			final String messageType, final List<SparkRoom> roomList, final String credentialsId) {
		this.disable = disable;
		this.skipOnFailure = skipOnFailure;
		this.skipOnSuccess = skipOnSuccess;
		this.skipOnAborted = skipOnAborted;
		this.skipOnUnstable = skipOnUnstable;
		this.messageContent = messageContent;
		this.messageType = messageType;
		this.roomList = roomList;
		this.credentialsId = credentialsId;
	}

	public String getMessageContent() {
		return messageContent;
	}

	@DataBoundSetter
	public void setMessageContent(final String messageContent) {
		this.messageContent = messageContent;
	}

	public String getMessageType() {
		return messageType;
	}

	public boolean isDisable() {
		return disable;
	}

	public boolean isSkipOnFailure() {
		return skipOnFailure;
	}

	public boolean isSkipOnSuccess() {
		return skipOnSuccess;
	}

	public boolean isSkipOnAborted() {
		return skipOnAborted;
	}

	public boolean isSkipOnUnstable() {
		return skipOnUnstable;
	}

	public List<SparkRoom> getRoomList() {
		if (roomList == null) {
			roomList = new ArrayList<>();
		}
		return roomList;
	}

	public String getCredentialsId() {
		return credentialsId;
	}

	@DataBoundSetter
	public void setCredentialsId(final String credentialsId) {
		this.credentialsId = Util.fixEmpty(credentialsId);
	}

	public boolean isSkipOnEmptyTest() {
		return skipOnEmptyTest;
	}

	@DataBoundSetter
	public void setSkipOnEmptyTest(final boolean skipOnEmptyTest) {
		this.skipOnEmptyTest = skipOnEmptyTest;
	}

	public boolean isTestSummary() {
		return testSummary;
	}

	@DataBoundSetter
	public void setTestSummary(final boolean testSummary) {
		this.testSummary = testSummary;
	}

	public boolean isTestDetails() {
		return testDetails;
	}

	@DataBoundSetter
	public void setTestDetails(final boolean testDetails) {
		this.testDetails = testDetails;
	}

	/**
	 * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild,
	 *      hudson.Launcher, hudson.model.BuildListener)
	 *
	 */
	@Override
	public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
			throws InterruptedException, IOException {
		if (disable) {
			listener.getLogger().println("Spark Notify Plugin Disabled!");
			return true;
		}

		EnvVars envVars = build.getEnvironment(listener);

		message = getMessageContent();
		if (!SparkMessage.isMessageValid(message)) {
			listener.getLogger().println("Skipping spark notifications because no message was defined");
			return true;
		}

		String result = build.getResult().toString();
		if (StringUtils.isNotEmpty(result)) {
			message = message.replace(MessageConst.BUILD_RESULT, result);
		} else {
			listener.error(Messages.NoBuildResult());
			result = StringUtils.EMPTY;
		}

		if (skipOnSuccess && result.equals(Result.SUCCESS.toString())) {
			listener.getLogger().println(Messages.SkipSuccess());
			return true;
		}
		if (skipOnFailure && result.equals(Result.FAILURE.toString())) {
			listener.getLogger().println(Messages.SkipFailed());
			return true;
		}
		if (skipOnAborted && result.equals(Result.ABORTED.toString())) {
			listener.getLogger().println(Messages.SkipAborted());
			return true;
		}
		if (skipOnUnstable && result.equals(Result.UNSTABLE.toString())) {
			listener.getLogger().println(Messages.SkipUnstable());
			return true;
		}

		if (StringUtils.isEmpty(messageType)) {
			messageType = "text";
		}

		if (CollectionUtils.isEmpty(roomList)) {
			listener.getLogger().println(Messages.SkipNoSpaces());
			return true;
		}

		SparkMessageType sparkMessageType = SparkMessageType.valueOf(messageType.toUpperCase());

		if (testSummary || testDetails || skipOnEmptyTest) {
			TestResultAction testResultAction = build.getAction(TestResultAction.class);
			if (testResultAction == null) {
				LOG.warning("Test results not found");
				if (skipOnEmptyTest) {
					listener.getLogger().println(Messages.SkipNoTestResults());
					return true;
				}
			} else if (testSummary || testDetails) {
				TestResult testResult = new TestResult(testResultAction, testSummary, testDetails);
				TestResultService testResultService = getTestResultService(testResult, sparkMessageType);
				message = testResultService.appendMessageWithTestInformation(message);
			}
		}

		SparkNotifier notifier = new SparkNotifier(getCredentials(credentialsId, build), envVars);

		for (SparkRoom room : roomList) {
			listener.getLogger().println(Messages.SendingMessage(StringUtils.isNotEmpty(room.getRName()) ? room.getRName() : room.getRId()));
			try {
				Response response = notifier.sendMessage(room.getRId(), message, sparkMessageType);
				if (response.getStatus() != Status.OK.getStatusCode()) {
					listener.error(Messages.ErrorHttpResponse(response.getStatus(), response.readEntity(String.class)));
				} else {
					listener.getLogger().println(Messages.MessageSent());
				}
			} catch (IOException | RuntimeException e) {
				listener.error(Messages.ErrorException(e));
			}
		}

		return true;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	@Override
	public SparkNotifyPostBuilderDescriptor getDescriptor() {
		return (SparkNotifyPostBuilderDescriptor) super.getDescriptor();
	}

	@Extension
	public static final class SparkNotifyPostBuilderDescriptor extends BuildStepDescriptor<Publisher> {
		public SparkNotifyPostBuilderDescriptor() {
			super(SparkNotifyPostBuilder.class);
			load();
		}

		/**
		 * @see hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest,
		 *      net.sf.json.JSONObject)
		 */
		@Override
		public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
			save();
			return true;
		}

		public FormValidation doMessageCheck(@QueryParameter final String message) {
			if (SparkMessage.isMessageValid(message)) {
				return FormValidation.ok();
			} else {
				return FormValidation.error(Messages.MessageNotNull());
			}
		}

		public FormValidation doRoomIdCheck(@QueryParameter final String roomId) {
			if (SparkMessage.isRoomIdValid(roomId)) {
				return FormValidation.ok();
			} else {
				return FormValidation.error(Messages.InvalidSpaceId());
			}
		}

		/**
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
			return true;
		}

		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Job<?, ?> project,
				@QueryParameter final String serverURI) {
			return new StandardListBoxModel().withEmptySelection().withMatching(
					CredentialsMatchers.instanceOf(StringCredentials.class),
					CredentialsProvider.lookupCredentials(StringCredentials.class, project, ACL.SYSTEM,
							URIRequirementBuilder.fromUri(serverURI).build()));
		}

		public ListBoxModel doFillMessageTypeItems(@QueryParameter final String messageType) {
			return new ListBoxModel(new Option("text", "text", messageType.matches("text")),
					new Option("markdown", "markdown", messageType.matches("markdown")),
					new Option("html", "html", messageType.matches("html")));
		}

		/**
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return Messages.DisplayName();
		}
	}

	private Credentials getCredentials(final String credentialsId, final Run<?, ?> build) {
		return CredentialsProvider.findCredentialById(credentialsId, StringCredentials.class, build);
	}

	private TestResultService getTestResultService(final TestResult testResult, final SparkMessageType messageType) {
		TestResultService testResultService;
		switch (messageType) {
		case MARKDOWN:
			testResultService = new MarkdownTestResultService(testResult, MAXIUMUM_TEST_RESULT_SIZE);
			break;
		case HTML:
			testResultService = new HtmlTestResultService(testResult, MAXIUMUM_TEST_RESULT_SIZE);
			break;
		case TEXT:
		default:
			testResultService = new TextTestResultService(testResult, MAXIUMUM_TEST_RESULT_SIZE);
			break;
		}
		return testResultService;
	}
}
