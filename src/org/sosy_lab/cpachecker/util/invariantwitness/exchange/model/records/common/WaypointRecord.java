// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.errorprone.annotations.Immutable;
import java.util.HashMap;
import java.util.Map;

@Immutable
public class WaypointRecord {
  @JsonProperty("type")
  private final WaypointType type;

  @JsonProperty("action")
  private final WaypointAction action;

  @JsonProperty("constraint")
  private final InformationRecord constraint;

  @JsonProperty("location")
  private final LocationRecord location;

  public WaypointRecord(
      @JsonProperty("type") WaypointType type,
      @JsonProperty("action") WaypointAction action,
      @JsonProperty("constraint") InformationRecord constraint,
      @JsonProperty("location") LocationRecord location) {
    // if not mentioned in json the type is assumed to be VISIT:
    this.type = type == null ? WaypointType.VISIT : type;
    // if not mentioned in json the action is assumed to be FOLLOW
    this.action = action == null ? WaypointAction.FOLLOW : action;
    this.constraint = constraint;
    this.location = location;
  }

  public WaypointRecord withAction(WaypointAction pAction) {
    return new WaypointRecord(this.getType(), pAction, this.getConstraint(), this.getLocation());
  }

  public WaypointRecord withType(WaypointType pType) {
    return new WaypointRecord(pType, this.getAction(), this.getConstraint(), this.getLocation());
  }

  public WaypointRecord withConstraint(InformationRecord pConstraint) {
    return new WaypointRecord(this.getType(), this.getAction(), pConstraint, this.getLocation());
  }

  public WaypointType getType() {
    return type;
  }

  public WaypointAction getAction() {
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

  public enum WaypointType {
    VISIT("visit"),
    BRANCHING("branching"),
    ASSUMPTION("assumption"),
    TARGET("target"),
    UNKNOWN("unknown");

    private static final Map<String, WaypointType> map;
    private final String keyword;

    static {
      map = new HashMap<>();
      for (WaypointType type : WaypointType.values()) {
        map.put(type.getKeyword(), type);
      }
    }

    WaypointType(String pKeyword) {
      keyword = pKeyword;
    }

    @JsonCreator
    public static WaypointType fromKeyword(String keyword) {
      if (keyword == null) {
        return VISIT;
      }
      return map.getOrDefault(keyword, UNKNOWN);
    }

    @JsonValue
    String getKeyword() {
      return keyword;
    }
  }

  public enum WaypointAction {
    FOLLOW("follow"),
    AVOID("avoid"),
    UNKNOWN("unknown");

    private static final Map<String, WaypointAction> map;
    private final String keyword;

    static {
      map = new HashMap<>();
      for (WaypointAction type : WaypointAction.values()) {
        map.put(type.getKeyword(), type);
      }
    }

    WaypointAction(String pKeyword) {
      keyword = pKeyword;
    }

    @JsonCreator
    public static WaypointAction fromKeyword(String keyword) {
      return map.getOrDefault(keyword, UNKNOWN);
    }

    @JsonValue
    String getKeyword() {
      return keyword;
    }
  }
}
