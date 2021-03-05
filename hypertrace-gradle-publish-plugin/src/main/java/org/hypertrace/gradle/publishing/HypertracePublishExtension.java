package org.hypertrace.gradle.publishing;

import java.util.Optional;
import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class HypertracePublishExtension {

  static final String BINTRAY_REPO_NAME = "maven";
  static final String BINTRAY_ORG_NAME = "hypertrace";
  static final String CIRCLECI_REPO_URL_ENV_VAR = "CIRCLE_REPOSITORY_URL";
  static final String PUBLISH_USER_PROPERTY = "publishUser";
  static final String PUBLISH_API_KEY_PROPERTY = "publishApiKey";
  static final String DEFAULT_BINTRAY_API_URL = "https://api.bintray.com";
  static final String UNSET = "unset";

  public final Property<License> license;
  public final Property<String> vcsUrl;
  public final Property<String> user;
  public final Property<String> apiKey;
  public final Property<String> repo;
  public final Property<String> organization;
  public final Property<String> name;
  public final Property<String> apiUrl;

  @Inject
  public HypertracePublishExtension(Project project, ObjectFactory objectFactory) {
    this.license = objectFactory.property(License.class);
    this.vcsUrl =
        objectFactory
            .property(String.class)
            .convention(this.getEnvironmentVariable(CIRCLECI_REPO_URL_ENV_VAR).orElse(UNSET));
    this.user = objectFactory.property(String.class);
    this.getProperty(project, PUBLISH_USER_PROPERTY).ifPresent(this.user::convention);
    this.apiKey = objectFactory.property(String.class);
    this.getProperty(project, PUBLISH_API_KEY_PROPERTY).ifPresent(this.apiKey::convention);
    this.repo = objectFactory.property(String.class).convention(BINTRAY_REPO_NAME);
    this.organization = objectFactory.property(String.class).convention(BINTRAY_ORG_NAME);
    this.name = objectFactory.property(String.class).convention(project.getName());
    this.apiUrl = objectFactory.property(String.class).convention(DEFAULT_BINTRAY_API_URL);
  }

  private Optional<String> getProperty(Project project, String propertyName) {
    return Optional.ofNullable(project.findProperty(propertyName)).map(String::valueOf);
  }

  private Optional<String> getEnvironmentVariable(String variableName) {
    return Optional.ofNullable(System.getenv(variableName));
  }
}
