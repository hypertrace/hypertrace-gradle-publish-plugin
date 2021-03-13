package org.hypertrace.gradle.publishing;

import org.hypertrace.gradle.publishing.License;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class HypertracePublishMavenCentralExtension implements ExtensionAware {
  private static final String DEFAULT_URL = "https://www.hypertrace.org";
  private static final String DEFAULT_DEVELOPER_ID = "hypertrace";
  private static final String DEFAULT_DEVELOPER_NAME = "Hypertrace Community";
  private static final String DEFAULT_DEVELOPER_EMAIL = "community@hypertrace.org";
  private static final String DEFAULT_DEVELOPER_ORG = "Hypertrace";
  private static final String DEFAULT_DEVELOPER_ORG_URL = "https://www.hypertrace.org";
  private static final String DEFAULT_PACKAGE_GROUP = "org.hypertrace";

  public final Property<String> url;
  public final Property<String> repoName;
  public final Property<String> developerId;
  public final Property<String> developerName;
  public final Property<String> developerEmail;
  public final Property<String> developerOrganization;
  public final Property<String> developerOrganizationUrl;
  public final Property<License> license;
  public final Property<String> packageGroup;
  public final Property<String> stagingProfileId;
  public final Property<Integer> numberOfRetries;
  public final Property<Integer> delayBetweenRetriesInMillis;

  @Inject
  public HypertracePublishMavenCentralExtension(ObjectFactory objectFactory) {
    this.url = objectFactory.property(String.class).convention(DEFAULT_URL);
    this.repoName = objectFactory.property(String.class);
    this.developerId = objectFactory.property(String.class).convention(DEFAULT_DEVELOPER_ID);
    this.developerName = objectFactory.property(String.class).convention(DEFAULT_DEVELOPER_NAME);
    this.developerEmail = objectFactory.property(String.class).convention(DEFAULT_DEVELOPER_EMAIL);
    this.developerOrganization = objectFactory.property(String.class).convention(DEFAULT_DEVELOPER_ORG);
    this.developerOrganizationUrl = objectFactory.property(String.class).convention(DEFAULT_DEVELOPER_ORG_URL);
    this.license = objectFactory.property(License.class);
    this.packageGroup = objectFactory.property(String.class).convention(DEFAULT_PACKAGE_GROUP);
    this.stagingProfileId = objectFactory.property(String.class);
    this.numberOfRetries = objectFactory.property(Integer.class);
    this.delayBetweenRetriesInMillis = objectFactory.property(Integer.class);
  }
}
