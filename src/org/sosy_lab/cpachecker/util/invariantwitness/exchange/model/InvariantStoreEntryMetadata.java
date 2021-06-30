// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

@Immutable
public class InvariantStoreEntryMetadata {
  @JsonProperty("format_version")
  private final String formatVersion;

  public InvariantStoreEntryMetadata(@JsonProperty("format_version") String pFormatVersion) {
    formatVersion = pFormatVersion;
  }

  public InvariantStoreEntryMetadata() {
    this.formatVersion = "0.1";
  }

  public String getFormatVersion() {
    return formatVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof InvariantStoreEntryMetadata)) {
      return false;
    }
    InvariantStoreEntryMetadata other = (InvariantStoreEntryMetadata) o;
    return formatVersion.equals(other.formatVersion);
  }

  @Override
  public int hashCode() {
    return formatVersion.hashCode();
  }
}
