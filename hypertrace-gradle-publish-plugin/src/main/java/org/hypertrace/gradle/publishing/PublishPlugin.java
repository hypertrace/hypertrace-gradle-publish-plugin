package org.hypertrace.gradle.publishing;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME;
import static org.hypertrace.gradle.publishing.HypertracePublishExtension.PUBLISH_API_KEY_PROPERTY;
import static org.hypertrace.gradle.publishing.HypertracePublishExtension.PUBLISH_USER_PROPERTY;
import static org.hypertrace.gradle.publishing.License.TRACEABLE_COMMUNITY;

import com.jfrog.bintray.gradle.tasks.BintrayPublishTask;
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.publish.Publication;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal;
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
      this.getOrCreateTask(this.project, UPLOAD_TASK_NAME, BintrayUploadTask.class);
    uploadTask.configure(task -> task.setEnabled(true));
    publishTask.configure(task -> task.dependsOn(uploadTask));
    project.afterEvaluate(unused -> uploadTask.configure(this::configureUploadTask));

    this.getPublishingExtension()
      .getPublications()
      .withType(MavenPublication.class)
      .all(
        publication -> {
          Collection<Task> dependencies =
            this.getDependenciesForPublication(project, publication);
          if (dependencies.stream().noneMatch(Task::getEnabled)) {
            return; // Ignore any publication that isn't actually publishing
          }
          this.addModuleMetadataPublication(publication);
          uploadTask.configure(
            task -> {
              task.dependsOn(dependencies);
              task.setPublications(this.getPublishingExtension().getPublications().toArray());
            });
        });
  }

  private TaskProvider<?> setupRootForPublishingIfNeeded() {
    Project root = this.project.getRootProject();
    root.getPluginManager().apply(PublishingPlugin.class);
    this.addRootUploadTaskIfNeeded();
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
    if (this.extension.license.get() != TRACEABLE_COMMUNITY) {
      // The bintray plugin doesn't support custom licenses. This is the default on all repos.
      task.setPackageLicenses(this.extension.license.get().bintrayString);
    }
    task.setVersionName(String.valueOf(project.getVersion()));
    System.out.println("apiUrl: " + this.extension.apiUrl.get());
  }

  private Collection<Task> getDependenciesForPublication(Project project, Publication publication) {
    if (!(publication instanceof MavenPublication)) {
      return Collections.emptySet();
    }
    return project.getTasksByName(this.getPublishLocalTaskNameForPublication(publication), false);
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
    TaskProvider<?> taskProvider =
      this.getOrCreateTask(project.getRootProject(), PUBLISH_TASK_NAME, BintrayPublishTask.class);
    project
      .getRootProject()
      .getTasks()
      .named(PUBLISH_LIFECYCLE_TASK_NAME)
      .configure(task -> task.dependsOn(taskProvider));
    return taskProvider;
  }

  private void addRootUploadTaskIfNeeded() {
    this.getOrCreateTask(project.getRootProject(), UPLOAD_TASK_NAME, BintrayUploadTask.class)
      .configure(
        task -> {
          task.setEnabled(false);
          task.project = project.getRootProject();
        });
  }

  private <T extends Task> TaskProvider<T> getOrCreateTask(
    Project project, String name, Class<T> taskClass) {
    try {
      return project.getTasks().withType(taskClass).named(name);
    } catch (Exception ignored) {
      return project.getTasks().register(name, taskClass);
    }
  }

  /*
   Bug in bintray plugin prevents the gradle metadata from being published normally. This is a hack
   to publish it explicitly. TODO: remove once bintray plugin handles this -
   https://github.com/bintray/gradle-bintray-plugin/pull/306
   Once the plugin handles this, this extra publication will duplicate the artifact and cause
   publish failures.
  */
  private void addModuleMetadataPublication(MavenPublication publication) {
    // This call will resolve a publication - we don't want to do that until other plugins have
    // finished setting up, so wait til after evaluate
    project.afterEvaluate(
      unused -> {
        if (publication instanceof MavenPublicationInternal) {
          // Extract the module metadata artifact, and re-register it as a first class artifact
          ((MavenPublicationInternal) publication)
            .getPublishableArtifacts().stream()
            .filter(mavenArtifact -> mavenArtifact.getExtension().equals("module"))
            .findFirst()
            .ifPresent(publication::artifact);
        }
      });
  }
}