package org.hypertrace.gradle.publishing;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME;
import static org.hypertrace.gradle.publishing.HypertracePublishExtension.PUBLISH_API_KEY_PROPERTY;
import static org.hypertrace.gradle.publishing.HypertracePublishExtension.PUBLISH_USER_PROPERTY;

import com.jfrog.bintray.gradle.tasks.BintrayPublishTask;
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.Publication;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.TaskProvider;

public class PublishPlugin implements Plugin<Project> {

  private static final String EXTENSION_NAME = "hypertracePublish";
  // can't be changed, hardcoded into publish task.
  private static final String UPLOAD_TASK_NAME = "bintrayUpload";
  private static final String PUBLISH_TASK_NAME = "bintrayPublish";
  private Project project;
  private HypertracePublishExtension extension;

  @Override
  public void apply(@Nonnull Project target) {
    project = target;
    this.extension =
        project.getExtensions().create(EXTENSION_NAME, HypertracePublishExtension.class, project);
    this.applyMavenPublish();
    this.addKnownPublications();
    this.addUploadTask(this.setupRootForPublishingIfNeeded());
  }

  private void applyMavenPublish() {
    project.getPluginManager().apply(MavenPublishPlugin.class);
  }

  private void addKnownPublications() {
    getPublishingExtension().publications(this::addJavaLibraryPublicationWhenApplied);
  }

  private void addUploadTask(TaskProvider<?> publishTask) {
    TaskProvider<BintrayUploadTask> uploadTask =
        this.project.getTasks().register(UPLOAD_TASK_NAME, BintrayUploadTask.class);
    publishTask.configure(task -> task.dependsOn(uploadTask));
    project.afterEvaluate(unused -> uploadTask.configure(this::configureUploadTask));

    this.getPublishingExtension()
        .getPublications()
        .all(
            publication ->
                uploadTask.configure(
                    task -> {
                      task.dependsOn(this.getDependenciesForPublication(publication));
                      task.setPublications(
                          this.getPublishingExtension().getPublications().toArray());
                    }));
  }

  private TaskProvider<?> setupRootForPublishingIfNeeded() {
    Project root = this.project.getRootProject();
    root.getPluginManager().apply(PublishingPlugin.class);
    return this.getOrCreateRootPublishTask();
  }

  private void addJavaLibraryPublicationWhenApplied(PublicationContainer publications) {
    project
        .getPluginManager()
        .withPlugin(
            "java-library",
            appliedPlugin -> {
              if (this.isJavaGradlePluginPluginApplied()) {
                return; // This already creates publications, we don't want to duplicate
              }

              publications.create(
                  "javaLibrary",
                  MavenPublication.class,
                  publication -> publication.from(project.getComponents().getByName("java")));
            });
  }

  private PublishingExtension getPublishingExtension() {
    return project.getExtensions().getByType(PublishingExtension.class);
  }

  private boolean isJavaGradlePluginPluginApplied() {
    return project.getPluginManager().hasPlugin("java-gradle-plugin");
  }

  private void configureUploadTask(BintrayUploadTask task) {
    this.validateExtensionAtConfigurationTime();
    task.doFirst(unused -> this.validateExtensionAtExecutionTime());
    Project project = task.getProject();
    task.project = project; // Why do they have their own var?
    task.setUser(this.extension.user.getOrNull());
    task.setApiKey(this.extension.apiKey.getOrNull());
    task.setApiUrl(this.extension.apiUrl.get());
    task.setPublish(true);
    task.setOverride(false);
    task.setRepoName(this.extension.repo.get());
    task.setPackageName(this.extension.name.get());
    task.setPackageVcsUrl(this.extension.vcsUrl.get());
    task.setUserOrg(this.extension.organization.get());
    task.setPackageLicenses(this.extension.license.get().bintrayString);
    task.setVersionName(String.valueOf(project.getVersion()));
  }

  private Collection<Object> getDependenciesForPublication(Publication publication) {
    if (!(publication instanceof MavenPublication)) {
      return Set.of();
    }
    return Set.of(this.getPublishLocalTaskNameForPublication(publication));
  }

  private String getPublishLocalTaskNameForPublication(Publication publication) {
    return String.format("publish%sPublicationToMavenLocal", capitalize(publication.getName()));
  }

  private void validateExtensionAtConfigurationTime() {
    if (!this.extension.license.isPresent()) {
      throw new GradleException(
          "A license type must be specified in the build DSL to use the Hypertrace publish plugin");
    }
  }

  private void validateExtensionAtExecutionTime() {
    if (!this.extension.user.isPresent()) {
      throw new GradleException(
          String.format(
              "A bintray user must be provided to run %s. Please provide one through the DSL as %s.user or the gradle property %s",
              UPLOAD_TASK_NAME, EXTENSION_NAME, PUBLISH_USER_PROPERTY));
    }
    if (!this.extension.apiKey.isPresent()) {
      throw new GradleException(
          String.format(
              "A bintray API Key must be provided to run %s. Please provide one through the DSL as %s.apiKey or the gradle property %s",
              UPLOAD_TASK_NAME, EXTENSION_NAME, PUBLISH_API_KEY_PROPERTY));
    }
  }

  private TaskProvider<?> getOrCreateRootPublishTask() {
    Project root = this.project.getRootProject();
    try {
      return root.getTasks().named(PUBLISH_TASK_NAME);
    } catch (Exception ignored) {
      TaskProvider<?> rootPublish =
          root.getTasks().register(PUBLISH_TASK_NAME, BintrayPublishTask.class);
      root.getTasks()
          .named(PUBLISH_LIFECYCLE_TASK_NAME)
          .configure(task -> task.dependsOn(rootPublish));
      return rootPublish;
    }
  }
}
