package org.hypertrace.gradle.publishing;

import io.codearte.gradle.nexus.BaseStagingTask;
import io.codearte.gradle.nexus.NexusStagingExtension;
import io.codearte.gradle.nexus.NexusStagingPlugin;
import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;

import javax.annotation.Nonnull;
import java.util.Optional;

public class PublishMavenCentralPlugin implements Plugin<Project> {

  private static final String EXTENSION_NAME = "hypertracePublishMavenCentral";

  private static final String PROPERTY_SIGNING_KEY_ID = "signingKeyId";
  private static final String PROPERTY_SIGNING_KEY = "signingKey";
  private static final String PROPERTY_SIGNING_PASSWORD = "signingPassword";
  private static final String PROPERTY_OSSRH_USERNAME = "ossrhUsername";
  private static final String PROPERTY_OSSRH_PASSWORD = "ossrhPassword";

  private Project project;
  private HypertracePublishMavenCentralExtension extension;

  @Override
  public void apply(@Nonnull Project target) {
    project = target;
    // Root projects only should configure nexus staging
    if (project.equals(project.getRootProject())) {
      project.getPluginManager().apply(NexusStagingPlugin.class);
    }
    // Library projects only (whether root or not) should set up publishing
    project
        .getPluginManager()
        .withPlugin(
            "java-library",
            unused -> {
              this.extension =
                  project
                      .getExtensions()
                      .create(EXTENSION_NAME, HypertracePublishMavenCentralExtension.class);
              this.applyMavenPublish();
              this.applySigning();
              this.applyWithJavadocJar();
              this.applyWithSourcesJar();
              this.addPublishRepository();
              this.addPublications();
            });
  }

  private void applySigning() {
    project.getPluginManager().apply(SigningPlugin.class);
  }

  private void configureNexusStagingPlugin() {
    Optional<String> user = getProperty(PROPERTY_OSSRH_USERNAME);
    Optional<String> password = getProperty(PROPERTY_OSSRH_PASSWORD);

    getNexusStagingExtension().setServerUrl("https://s01.oss.sonatype.org/service/local/");
    if (user.isPresent() && password.isPresent()) {
      getNexusStagingExtension().setUsername(user.get());
      getNexusStagingExtension().setPassword(password.get());
    }

    // packageGroup
    getNexusStagingExtension().setPackageGroup(this.extension.packageGroup.get());
  }

  private void applyMavenPublish() {
    project.getPluginManager().apply(MavenPublishPlugin.class);
  }

  private void applyWithJavadocJar() {
    getJavaPluginExtension().withJavadocJar();
    project
        .getTasks()
        .withType(Javadoc.class)
        .configureEach(
            javadoc -> {
              StandardJavadocDocletOptions options =
                  (StandardJavadocDocletOptions) javadoc.getOptions();
              if (JavaVersion.current().isJava9Compatible()) {
                options.addBooleanOption("html5", true);
              }
            });
  }

  private void applyWithSourcesJar() {
    getJavaPluginExtension().withSourcesJar();
  }

  private void addPublishRepository() {
    String url;
    Optional<String> user = getProperty(PROPERTY_OSSRH_USERNAME);
    Optional<String> password = getProperty(PROPERTY_OSSRH_PASSWORD);

    if (project.getVersion().toString().endsWith("SNAPSHOT")) {
      url = "https://s01.oss.sonatype.org/content/repositories/snapshots/";
    } else {
      url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/";
    }

    if (user.isPresent() && password.isPresent()) {
      getPublishingExtension()
          .repositories(
              artifactRepositories ->
                  artifactRepositories.maven(
                      mavenArtifactRepository -> {
                        mavenArtifactRepository.setName("mavenCentral");
                        mavenArtifactRepository.setUrl(url);
                        mavenArtifactRepository.credentials(
                            passwordCredentials -> {
                              passwordCredentials.setUsername(user.get());
                              passwordCredentials.setPassword(password.get());
                            });
                      }));
    }
  }

  private void addPublications() {
    project.afterEvaluate(
        unused -> {
          validateExtensionAtConfigurationTime();
          getPublishingExtension().publications(this::addJavaLibraryPublicationWhenApplied);
          validateGradlePropertiesBeforePublishTask();
          // sign after publication is added
          addSigning();
          // configure staging plugin
          configureNexusStagingPlugin();
          validateGradlePropertiesBeforeStagingTasks();
        });
  }

  private void addJavaLibraryPublicationWhenApplied(PublicationContainer publications) {
    project
        .getPluginManager()
        .withPlugin(
            "java-library",
            appliedPlugin -> {
              MavenPublication publication;

              try {
                // use existing publication from org.hypertrace.publish-plugin plugin
                publication = (MavenPublication) publications.getByName("javaLibrary");
              } catch (UnknownDomainObjectException e) {
                // create new publication
                publication = publications.create("javaLibrary", MavenPublication.class);
                publication.from(project.getComponents().getByName("java"));
              }

              // configure pom
              publication.pom(
                  mavenPom -> {
                    // name
                    mavenPom
                        .getName()
                        .set(
                            project.provider(
                                () -> {
                                  return String.format(
                                      "%s:%s", project.getGroup(), project.getName());
                                }));

                    // url
                    mavenPom.getUrl().set(this.extension.url);

                    // description
                    mavenPom.getDescription().set(project.getDescription());

                    // scm
                    Provider<String> qualifiedRepoProvider =
                        this.extension.scmOrganization.flatMap(
                            orgName ->
                                this.extension.repoName.map(
                                    repoName -> String.format("%s/%s", orgName, repoName)));
                    Provider<String> scmConnectionProvider =
                        qualifiedRepoProvider.map(
                            qualifiedRepo ->
                                String.format("scm:git:git://github.com/%s.git", qualifiedRepo));
                    Provider<String> scmDeveloperConnectionProvider =
                        qualifiedRepoProvider.map(
                            qualifiedRepo ->
                                String.format("scm:git:ssh://github.com:%s.git", qualifiedRepo));
                    Provider<String> scmUrlProvider =
                        qualifiedRepoProvider.map(
                            qualifiedRepo ->
                                String.format("https://github.com/%s/tree/main", qualifiedRepo));
                    mavenPom.scm(
                        mavenPomScm -> {
                          mavenPomScm.getConnection().set(scmConnectionProvider);
                          mavenPomScm.getDeveloperConnection().set(scmDeveloperConnectionProvider);
                          mavenPomScm.getUrl().set(scmUrlProvider);
                        });

                    // developers
                    mavenPom.developers(
                        mavenPomDeveloperSpec -> {
                          mavenPomDeveloperSpec.developer(
                              mavenPomDeveloper -> {
                                mavenPomDeveloper.getId().set(this.extension.developerId);
                                mavenPomDeveloper.getName().set(this.extension.developerName);
                                mavenPomDeveloper.getEmail().set(this.extension.developerEmail);
                                mavenPomDeveloper
                                    .getOrganization()
                                    .set(this.extension.developerOrganization);
                                mavenPomDeveloper
                                    .getOrganizationUrl()
                                    .set(this.extension.developerOrganizationUrl);
                              });
                        });

                    // licenses
                    mavenPom.licenses(
                        mavenPomLicenseSpec -> {
                          mavenPomLicenseSpec.license(
                              mavenPomLicense -> {
                                mavenPomLicense
                                    .getName()
                                    .set(this.extension.license.get().toString());
                              });
                        });
                  });
            });
  }

  private void addSigning() {
    if (project.hasProperty(PROPERTY_SIGNING_KEY)
        && project.hasProperty(PROPERTY_SIGNING_PASSWORD)) {
      String signingKey = (String) project.property(PROPERTY_SIGNING_KEY);
      String signingPassword = (String) project.property(PROPERTY_SIGNING_PASSWORD);
      if (project.hasProperty(PROPERTY_SIGNING_KEY_ID)) {
        String signingKeyId = (String) project.property(PROPERTY_SIGNING_KEY_ID);
        getSigningExtension().useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword);
      } else {
        getSigningExtension().useInMemoryPgpKeys(signingKey, signingPassword);
      }
    }
    getSigningExtension().sign(getPublishingExtension().getPublications().getByName("javaLibrary"));
  }

  private PublishingExtension getPublishingExtension() {
    return project.getExtensions().getByType(PublishingExtension.class);
  }

  private JavaPluginExtension getJavaPluginExtension() {
    return project.getExtensions().getByType(JavaPluginExtension.class);
  }

  private SigningExtension getSigningExtension() {
    return project.getExtensions().getByType(SigningExtension.class);
  }

  private NexusStagingExtension getNexusStagingExtension() {
    return project.getRootProject().getExtensions().getByType(NexusStagingExtension.class);
  }

  private Optional<String> getProperty(String propertyName) {
    return Optional.ofNullable(project.findProperty(propertyName)).map(String::valueOf);
  }

  private void validateExtensionAtConfigurationTime() {
    if (!this.extension.license.isPresent()) {
      throw new GradleException(
          "A license type must be specified in the build DSL to use the Hypertrace maven central publish plugin");
    }
    if (!this.extension.repoName.isPresent()) {
      throw new GradleException(
          "Repository Name must be specified in the build DSL to use the Hypertrace maven central publish plugin");
    }
  }

  private void validateGradlePropertiesBeforePublishTask() {
    project
        .getTasks()
        .named("publish")
        .configure(
            task -> {
              task.doFirst(
                  unused -> {
                    validateGradleProperty(PROPERTY_OSSRH_USERNAME);
                    validateGradleProperty(PROPERTY_OSSRH_PASSWORD);
                  });
            });
  }

  private void validateGradlePropertiesBeforeStagingTasks() {
    project.getRootProject().getTasks().withType(BaseStagingTask.class).stream()
        .forEach(
            task -> {
              task.doFirst(
                  unused -> {
                    validateGradleProperty(PROPERTY_OSSRH_USERNAME);
                    validateGradleProperty(PROPERTY_OSSRH_PASSWORD);
                  });
            });
  }

  private void validateGradleProperty(String propertyName) {
    if (!project.hasProperty(propertyName)) {
      throw new GradleException(
          "Missing expected gradle property: "
              + propertyName
              + ". It should be added "
              + "in your ~/.gradle/gradle.properties file, or in a an environment variable of the form "
              + "ORG_GRADLE_PROJECT_"
              + propertyName);
    }
  }
}
