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
public class LoopInvariantEntry extends AbstractEntry {

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("location")
  private final LocationRecord location;

  @JsonProperty("loop_invariant")
  private final InformationRecord loopInvariant;

  public LoopInvariantEntry(
      @JsonProperty("entry_type") String entryType,
      @JsonProperty("metadata") MetadataRecord metadata,
      @JsonProperty("location") LocationRecord location,
      @JsonProperty("loop_invariant") InformationRecord loopInvariant) {
    super(entryType);
    this.metadata = metadata;
    this.location = location;
    this.loopInvariant = loopInvariant;
  }

  public MetadataRecord getMetadata() {
    return metadata;
  }

  public LocationRecord getLocation() {
    return location;
  }

  public InformationRecord getLoopInvariant() {
    return loopInvariant;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((location == null) ? 0 : location.hashCode());
    result = prime * result + ((loopInvariant == null) ? 0 : loopInvariant.hashCode());
    result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof LoopInvariantEntry)) {
      return false;
    }
    LoopInvariantEntry other = (LoopInvariantEntry) obj;
    if (!Objects.equals(location, other.location)) {
      return false;
    }
    if (!Objects.equals(loopInvariant, other.loopInvariant)) {
      return false;
    }
    if (!Objects.equals(metadata, other.metadata)) {
      return false;
    }
    return true;
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
        + getLoopInvariant()
        + "'"
        + "}";
  }
}
