package org.hypertrace.gradle.publishing;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class HypertracePublishMavenCentralExtension implements ExtensionAware {
  private static final String DEFAULT_URL = "https://www.hypertrace.org";
  public final Property<String> url;
  public final Property<String> scmConnection;
  public final Property<String> scmDeveloperConnection;
  public final Property<String> scmUrl;
    public final NamedDomainObjectContainer<PomLicense> licenses;
  public final NamedDomainObjectContainer<PomDeveloper> developers;

  @Inject
  public HypertracePublishMavenCentralExtension(ObjectFactory objectFactory) {
    this.url = objectFactory.property(String.class).convention(DEFAULT_URL);
    this.scmConnection = objectFactory.property(String.class);
    this.scmDeveloperConnection = objectFactory.property(String.class);
    this.scmUrl = objectFactory.property(String.class);
    this.licenses = objectFactory.domainObjectContainer(PomLicense.class);
    this.developers = objectFactory.domainObjectContainer(PomDeveloper.class);
  }

  public PomLicense license(String licenseName) {
    return license(licenseName, null);
  }

  public PomLicense license(String licenseName, Action<PomLicense> licenseAction) {
    PomLicense license = this.licenses.maybeCreate(licenseName);
    if (licenseAction != null) {
      licenseAction.execute(license);
    }
    return license;
  }

  public PomDeveloper developer(String developerName) {
    return developer(developerName, null);
  }

  public PomDeveloper developer(String developerName, Action<PomDeveloper> developerAction) {
    PomDeveloper developer = this.developers.maybeCreate(developerName);
    if (developerAction != null) {
      developerAction.execute(developer);
    }
    return developer;
  }
}
