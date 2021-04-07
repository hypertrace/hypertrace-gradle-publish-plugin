package org.hypertrace.gradle.publishing;

public enum License {
  TRACEABLE_COMMUNITY("Traceable"),
  APACHE_2_0("Apache-2.0"),
  PROPRIETARY("Proprietary");

  private final String license;

  License(String license) {
    this.license = license;
  }

  @Override
  public String toString() {
    return license;
  }
}