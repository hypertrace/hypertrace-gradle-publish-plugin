package org.hypertrace.gradle.publishing;

import org.gradle.api.Named;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class PomDeveloper implements Named {
  public final String name;
  public final Property<String> id;
  public final Property<String> email;
  public final Property<String> organization;
  public final Property<String> organizationUrl;


  @Inject
  public PomDeveloper(String name, ObjectFactory objectFactory) {
    this.name = name;
    this.id = objectFactory.property(String.class);
    this.email = objectFactory.property(String.class);
    this.organization = objectFactory.property(String.class);
    this.organizationUrl = objectFactory.property(String.class);
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }

  public Property<String> getId() {
    return id;
  }

  public Property<String> getEmail() {
    return email;
  }

  public Property<String> getOrganization() {
    return organization;
  }

  public Property<String> getOrganizationUrl() {
    return organizationUrl;
  }
}


