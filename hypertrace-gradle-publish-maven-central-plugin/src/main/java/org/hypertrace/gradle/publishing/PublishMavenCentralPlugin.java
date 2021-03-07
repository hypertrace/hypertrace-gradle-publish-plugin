package org.hypertrace.gradle.publishing;

import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;

import javax.annotation.Nonnull;

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
    this.extension =
      project.getExtensions().create(EXTENSION_NAME, HypertracePublishMavenCentralExtension.class);
    this.applyMavenPublish();
    this.applySigning();
    this.applyWithJavadocJar();
    this.applyWithSourcesJar();
    this.addPublishRepository();
    this.addPublications();
  }

  private void applySigning() {
    project.getPluginManager()
      .apply(SigningPlugin.class);
  }

  private void applyMavenPublish() {
    project.getPluginManager()
      .apply(MavenPublishPlugin.class);
  }

  private void applyWithJavadocJar() {
    getJavaPluginExtension().withJavadocJar();
    project.getTasks().withType(Javadoc.class).configureEach(javadoc -> {
      StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) javadoc.getOptions();
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
    String user = getPropertyOrThrow(project, PROPERTY_OSSRH_USERNAME);
    String password = getPropertyOrThrow(project, PROPERTY_OSSRH_PASSWORD);

    if (project.getVersion().toString().endsWith("SNAPSHOT")) {
      url = "https://s01.oss.sonatype.org/content/repositories/snapshots/";
    } else {
      url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/";
    }

    getPublishingExtension().repositories(artifactRepositories ->
      artifactRepositories.maven(mavenArtifactRepository -> {
        mavenArtifactRepository.setUrl(url);
        mavenArtifactRepository.credentials(passwordCredentials -> {
          passwordCredentials.setUsername(user);
          passwordCredentials.setPassword(password);
        });
      })
    );
  }

  private void addPublications() {
    project.afterEvaluate(unused -> {
      getPublishingExtension().publications(this::addJavaLibraryPublicationWhenApplied);

      // sign after publication is added
      addSigning();
    });
  }

  private void addJavaLibraryPublicationWhenApplied(PublicationContainer publications) {
    project.getPluginManager()
      .withPlugin("java-library", appliedPlugin -> {
        publications.create("javaLibrary", MavenPublication.class, publication -> {
          // from
          publication.from(project.getComponents().getByName("java"));

          // versionMapping
          publication.versionMapping(versionMappingStrategy -> {
            versionMappingStrategy.usage("java-api", variantVersionMappingStrategy -> {
              variantVersionMappingStrategy.fromResolutionOf("runtimeClasspath");
            });
            versionMappingStrategy.usage("java-runtime", variantVersionMappingStrategy -> {
              variantVersionMappingStrategy.fromResolutionResult();
            });
          });

          // pom
          publication.pom(mavenPom -> {
            // url
            mavenPom.getUrl().set(this.extension.url.get());

            // scm
            if (this.extension.repoName.isPresent()) {
              String repoName = this.extension.repoName.get();
              String scmConnection = String.format("scm:git:git://github.com/hypertrace/%s.git", repoName);
              String scmDeveloperConnection = String.format("scm:git:ssh://github.com:hypertrace/%s.git", repoName);
              String scmUrl = String.format("https://github.com/hypertrace/%s/tree/main", repoName);
              mavenPom.scm(mavenPomScm -> {
                mavenPomScm.getConnection().set(scmConnection);
                mavenPomScm.getDeveloperConnection().set(scmDeveloperConnection);
                mavenPomScm.getUrl().set(scmUrl);
              });
            }

            // developers
            this.extension.developers.all(developer -> {
              mavenPom.developers(mavenPomDeveloperSpec -> {
                mavenPomDeveloperSpec.developer(mavenPomDeveloper -> {
                  mavenPomDeveloper.getId().set(developer.getId());
                  mavenPomDeveloper.getName().set(developer.getName());
                  mavenPomDeveloper.getEmail().set(developer.getEmail());
                  mavenPomDeveloper.getOrganization().set(developer.getOrganization());
                  mavenPomDeveloper.getOrganizationUrl().set(developer.getOrganizationUrl());
                });
              });
            });

            // licenses
            this.extension.licenses.all(license -> {
              mavenPom.licenses(mavenPomLicenseSpec -> {
                mavenPomLicenseSpec.license(mavenPomLicense -> {
                  mavenPomLicense.getName().set(license.getName());
                  mavenPomLicense.getUrl().set(license.getUrl());
                  mavenPomLicense.getDistribution().set(license.getDistribution());
                  mavenPomLicense.getComments().set(license.getComments());
                });
              });
            });

          });
        });
      });
  }

  private void addSigning() {
    if (project.hasProperty(PROPERTY_SIGNING_KEY) && project.hasProperty(PROPERTY_SIGNING_PASSWORD)) {
      String signingKey = (String) project.property(PROPERTY_SIGNING_KEY);
      String signingPassword = (String) project.property(PROPERTY_SIGNING_PASSWORD);
      if (project.hasProperty(PROPERTY_SIGNING_KEY_ID)) {
        String signingKeyId = (String) project.property(PROPERTY_SIGNING_KEY_ID);
        getSigningExtension().useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword);
      } else {
        getSigningExtension().useInMemoryPgpKeys(signingKey, signingPassword);
      }
    }
    getSigningExtension()
      .sign(getPublishingExtension().getPublications().getByName("javaLibrary"));
  }

  private PublishingExtension getPublishingExtension() {
    return project.getExtensions()
      .getByType(PublishingExtension.class);
  }

  private JavaPluginExtension getJavaPluginExtension() {
    return project.getExtensions()
      .getByType(JavaPluginExtension.class);
  }

  private SigningExtension getSigningExtension() {
    return project.getExtensions()
      .getByType(SigningExtension.class);
  }

  private String getPropertyOrThrow(Project project, String propertyName) {
    if (!project.hasProperty(propertyName)) {
      throw new GradleException("Missing expected gradle property: " + propertyName + ". It should be added " +
        "in your ~/.gradle/gradle.properties file, or in a an environment variable of the form " +
        "ORG_GRADLE_PROJECT_" + propertyName);
    }
    return (String) project.property(propertyName);
  }
}
