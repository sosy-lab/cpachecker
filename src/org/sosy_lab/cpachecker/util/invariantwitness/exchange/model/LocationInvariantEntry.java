// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
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
public class LocationInvariantEntry extends AbstractEntry {

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("location")
  private final LocationRecord location;

  @JsonProperty("location_invariant")
  private final InformationRecord locationInvariant;

  public LocationInvariantEntry(
      @JsonProperty("entry_type") String pEntryType,
      @JsonProperty("metadata") MetadataRecord pMetadata,
      @JsonProperty("location") LocationRecord pLocation,
      @JsonProperty("location_invariant") InformationRecord pLocationInvariant) {
    super(pEntryType);
    metadata = pMetadata;
    location = pLocation;
    locationInvariant = pLocationInvariant;
  }

  public MetadataRecord getMetadata() {
    return metadata;
  }

  public LocationRecord getLocation() {
    return location;
  }

  public InformationRecord getLocationInvariant() {
    return locationInvariant;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof LocationInvariantEntry that)) {
      return false;
    }
    if (!super.equals(pO)) {
      return false;
    }
    return Objects.equals(metadata, that.metadata)
        && Objects.equals(location, that.location)
        && Objects.equals(locationInvariant, that.locationInvariant);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), metadata, location, locationInvariant);
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
        + ", locationInvariant='"
        + getLocationInvariant()
        + "'"
        + "}";
  }
}
