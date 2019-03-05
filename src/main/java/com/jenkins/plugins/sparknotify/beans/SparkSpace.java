package com.jenkins.plugins.sparknotify.beans;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public final class SparkSpace extends AbstractDescribableImpl<SparkSpace> {
	private final String spaceName;
	private final String spaceId;

	public String getSpaceName() {
		return spaceName;
	}

	public String getSpaceId() {
		return spaceId;
	}

	@DataBoundConstructor
	public SparkSpace(final String spaceName, final String spaceId) {
		this.spaceName = spaceName;
		this.spaceId = spaceId;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<SparkSpace> {
		@Override
		public String getDisplayName() {
			return "";
		}
	}
}
