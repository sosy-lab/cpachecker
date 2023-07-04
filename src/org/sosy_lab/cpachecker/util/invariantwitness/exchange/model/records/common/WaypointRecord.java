// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

@Immutable
public class WaypointRecord {
  @JsonProperty("type")
  private final String type;

  @JsonProperty("action")
  private final String action;

  @JsonProperty("constraint")
  private final InformationRecord constraint;

  @JsonProperty("location")
  private final LocationRecord location;

  public WaypointRecord(
      @JsonProperty("type") String type,
      @JsonProperty("action") String action,
      @JsonProperty("constraint") InformationRecord constraint,
      @JsonProperty("location") LocationRecord location) {
    this.type = type;
    this.action = action;
    this.constraint = constraint;
    this.location = location;
  }

  public String getType() {
    return type;
  }

  public String getAction() {
    return action;
  }

  public InformationRecord getConstraint() {
    return constraint;
  }

  public LocationRecord getLocation() {
    return location;
  }

  @Override
  @SuppressWarnings("EqualsGetClass")
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther == null || getClass() != pOther.getClass()) {
      return false;
    }
    WaypointRecord other = (WaypointRecord) pOther;
    return type.equals(other.type)
        && action.equals(other.action)
        && constraint.equals(other.constraint)
        && location.equals(other.location);
  }

  @Override
  public int hashCode() {
    int hashCode = type.hashCode();
    hashCode = 31 * hashCode + action.hashCode();
    hashCode = 31 * hashCode + constraint.hashCode();
    hashCode = 31 * hashCode + location.hashCode();
    return hashCode;
  }

  @Override
  public String toString() {
    return "WaypointRecord [type="
        + type
        + ", action="
        + action
        + ", constraint="
        + constraint
        + ", location="
        + location
        + "]";
  }
}
