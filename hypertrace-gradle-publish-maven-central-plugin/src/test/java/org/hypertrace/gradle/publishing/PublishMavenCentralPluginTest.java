package org.hypertrace.gradle.publishing;

import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PublishMavenCentralPluginTest {
  private static Project project;

  @BeforeAll
  public static void setup() {
    project = ProjectBuilder.builder().build();
    project.getExtensions().getExtraProperties().set("ossrhUsername", "ossrhUsername");
    project.getExtensions().getExtraProperties().set("ossrhPassword", "ossrhPassword");
    project.getPluginManager().apply("java-library");
    project.getPluginManager().apply("org.hypertrace.publish-maven-central-plugin");
    project.getExtensions().getByType(HypertracePublishMavenCentralExtension.class).license.set(License.APACHE_2_0);
    project.getExtensions().getByType(HypertracePublishMavenCentralExtension.class).repoName.set("test");
    project.getTasksByName("tasks", false);
  }

  @Test
  public void testAppliedPlugins() {
    Assertions.assertTrue(project.getPluginManager().hasPlugin("java-library"));
    Assertions.assertTrue(project.getPluginManager().hasPlugin("org.hypertrace.publish-maven-central-plugin"));
    Assertions.assertTrue(project.getPluginManager().hasPlugin("maven-publish"));
    Assertions.assertTrue(project.getPluginManager().hasPlugin("signing"));
  }

  @Test
  public void testPomUrl() {
    MavenPublication publication = (MavenPublication) project.getExtensions()
      .getByType(PublishingExtension.class).getPublications().getByName("javaLibrary");
    Assertions.assertNotNull(publication);
    Assertions.assertNotNull(publication.getPom().getUrl().getOrNull());
    Assertions.assertEquals(publication.getPom().getUrl().get(), "https://www.hypertrace.org");
  }

  @Test
  public void testWithJavadocJar() {
    Assertions.assertNotNull(project.getTasks().getByName("javadocJar"));
  }

  @Test
  public void testWithSourcesJar() {
    Assertions.assertNotNull(project.getTasks().getByName("sourcesJar"));
  }

  @Test
  public void testExtensionDefaults() {
    HypertracePublishMavenCentralExtension extension = project.getExtensions()
      .getByType(HypertracePublishMavenCentralExtension.class);
    Assertions.assertNotNull(extension);
    Assertions.assertEquals(extension.url.get(), "https://www.hypertrace.org");
    Assertions.assertEquals(extension.developerId.get(), "hypertrace");
    Assertions.assertEquals(extension.developerName.get(), "Hypertrace Community");
    Assertions.assertEquals(extension.developerEmail.get(), "community@hypertrace.org");
    Assertions.assertEquals(extension.developerOrganization.get(), "Hypertrace");
    Assertions.assertEquals(extension.developerOrganizationUrl.get(), "https://www.hypertrace.org");
  }
}
