package org.hypertrace.gradle.publishing;

public enum License {
  AGPL_V3("AGPL-V3"),
  APACHE_2_0("Apache-2.0");

  public final String bintrayString;

  License(String bintrayString) {
    this.bintrayString = bintrayString;
  }
}
