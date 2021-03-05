package org.hypertrace.gradle.publishing;

import javax.inject.Inject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public class HypertracePublishMavenCentralExtension {
  public final Property<String> url;
//  final public Property<String> scmConnection;
//  final public Property<String> scmDeveloperConnection;
//  final public Property<String> scmUrl;
//  final public ListProperty<String> licenses;
//  final public ListProperty<String> developers;

  @Inject
  public HypertracePublishMavenCentralExtension(ObjectFactory objectFactory) {
    this.url = objectFactory.property(String.class).convention("https://www.hypertrace.org");
//    this.scmConnection = objectFactory.property(String.class);
//    this.scmDeveloperConnection = objectFactory.property(String.class);
//    this.scmUrl = objectFactory.property(String.class);
//    this.licenses = objectFactory.listProperty(String.class);
//    this.developers = objectFactory.listProperty(String.class);
  }
}
