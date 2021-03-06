package org.hypertrace.gradle.publishing;

import org.gradle.api.Named;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class PomLicense implements Named {
  public final String name;
  public final Property<String> url;
  public final Property<String> distribution;
  public final Property<String> comments;

  @Inject
  public PomLicense(String name, ObjectFactory objectFactory) {
    this.name = name;
    this.url = objectFactory.property(String.class);
    this.distribution = objectFactory.property(String.class);
    this.comments = objectFactory.property(String.class);
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }

  public Property<String> getUrl() {
    return url;
  }

  public Property<String> getDistribution() {
    return distribution;
  }

  public Property<String> getComments() {
    return comments;
  }
}
