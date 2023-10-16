// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Objects;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InformationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.LocationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;

@Immutable
public abstract class InvariantEntry extends AbstractEntry {

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("location")
  private final LocationRecord location;

  @JsonProperty("invariant")
  private final InformationRecord invariant;

  public InvariantEntry(
      MetadataRecord metadata,
      LocationRecord location,
      InformationRecord invariant,
      String entryTypeIdentifier) {
    super(entryTypeIdentifier);
    this.metadata = metadata;
    this.location = location;
    this.invariant = invariant;
  }

  public MetadataRecord getMetadata() {
    return metadata;
  }

  public LocationRecord getLocation() {
    return location;
  }

  public InformationRecord getInvariant() {
    return invariant;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((location == null) ? 0 : location.hashCode());
    result = prime * result + ((invariant == null) ? 0 : invariant.hashCode());
    result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return super.equals(obj)
        && obj instanceof InvariantEntry other
        && Objects.equals(location, other.location)
        && Objects.equals(invariant, other.invariant)
        && Objects.equals(metadata, other.metadata);
  }

  @Override
  public String toString() {
    return "InvariantStoreEntry{"
        + " entry_type='"
        + getEntryType()
        + "'"
        + ", metadata='"
        + getMetadata()
        + "'"
        + ", location='"
        + getLocation()
        + "'"
        + ", loopInvariant='"
        + getInvariant()
        + "'"
        + "}";
  }
}
