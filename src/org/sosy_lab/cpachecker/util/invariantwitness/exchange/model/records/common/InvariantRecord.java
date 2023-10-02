// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;

public class InvariantRecord extends InformationRecord {

  @JsonProperty("location")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final LocationRecord location;

  public InvariantRecord(
      @JsonProperty("value") String pString,
      @JsonProperty("type") String pType,
      @JsonProperty("format") String pFormat,
      @JsonProperty("location") LocationRecord pLocation) {
    super(pString, pType, pFormat);
    location = pLocation;
  }

  public LocationRecord getLocation() {
    return location;
  }

  public enum InvariantRecordType {
    LOOP_INVARIANT("loop_invariant"),
    LOCATION_INVARIANT("location_invariant"),
    UNKNOWN("unknown");

    private static final Map<String, InvariantRecordType> map;
    private final String keyword;

    static {
      map = new HashMap<>();
      for (InvariantRecordType type : InvariantRecordType.values()) {
        map.put(type.getKeyword(), type);
      }
    }

    InvariantRecordType(String pKeyword) {
      keyword = pKeyword;
    }

    @JsonCreator
    public static InvariantRecordType fromKeyword(String keyword) {
      return map.getOrDefault(keyword, UNKNOWN);
    }

    @JsonValue
    String getKeyword() {
      return keyword;
    }
  }
}
