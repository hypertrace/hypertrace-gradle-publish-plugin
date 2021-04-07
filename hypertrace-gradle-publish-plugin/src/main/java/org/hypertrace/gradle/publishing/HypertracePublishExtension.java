package org.hypertrace.gradle.publishing;

import javax.inject.Inject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class HypertracePublishExtension {
  public final Property<License> license;
  public final Property<String> url;

  @Inject
  public HypertracePublishExtension(ObjectFactory objectFactory) {
    this.license = objectFactory.property(License.class);
    this.url = objectFactory.property(String.class);
  }
}
