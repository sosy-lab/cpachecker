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
public class InvariantStoreEntry {
  @JsonProperty("entry_type")
  private final String entryType;

  @JsonProperty("metadata")
  private final InvariantStoreEntryMetadata metadata;

  @JsonProperty("location")
  private final InvariantStoreEntryLocation location;

  @JsonProperty("loop_invariant")
  private final InvariantStoreEntryLoopInvariant loopInvariant;

  public InvariantStoreEntry(
      @JsonProperty("entry_tpe") String entryType,
      @JsonProperty("metadata") InvariantStoreEntryMetadata metadata,
      @JsonProperty("location") InvariantStoreEntryLocation location,
      @JsonProperty("loop_invariant") InvariantStoreEntryLoopInvariant loopInvariant) {
    this.entryType = entryType;
    this.metadata = metadata;
    this.location = location;
    this.loopInvariant = loopInvariant;
  }

  public String getEntryType() {
    return entryType;
  }

  public InvariantStoreEntryMetadata getMetadata() {
    return metadata;
  }

  public InvariantStoreEntryLocation getLocation() {
    return location;
  }

  public InvariantStoreEntryLoopInvariant getLoopInvariant() {
    return loopInvariant;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof InvariantStoreEntry)) {
      return false;
    }
    InvariantStoreEntry other = (InvariantStoreEntry) o;
    return entryType.equals(other.entryType)
        && metadata.equals(other.metadata)
        && location.equals(other.location)
        && loopInvariant.equals(other.loopInvariant);
  }

  @Override
  public int hashCode() {
    int hashCode = entryType.hashCode();
    hashCode = 31 * hashCode + metadata.hashCode();
    hashCode = 31 * hashCode + location.hashCode();
    hashCode = 31 * hashCode + loopInvariant.hashCode();
    return hashCode;
  }

  @Override
  public String toString() {
    return "InvariantStoreEntry{"
        + " entryType='"
        + getEntryType()
        + "'"
        + ", metadata='"
        + getMetadata()
        + "'"
        + ", location='"
        + getLocation()
        + "'"
        + ", loopInvariant='"
        + getLoopInvariant()
        + "'"
        + "}";
  }
}
