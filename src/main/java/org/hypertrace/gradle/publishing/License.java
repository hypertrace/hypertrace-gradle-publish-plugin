package org.hypertrace.gradle.publishing;

public enum License {
  AGPL_V3("AGPL-V3");

  public final String bintrayString;

  License(String bintrayString) {
    this.bintrayString = bintrayString;
  }
}
