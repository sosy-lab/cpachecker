// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class SegmentRecord {
  @JsonProperty("segment")
  private final List<WaypointRecord> segment;

  public SegmentRecord(@JsonProperty("segment") List<WaypointRecord> pSegment) {
    segment = pSegment;
  }

  public List<WaypointRecord> getSegment() {
    return segment;
  }

  public static SegmentRecord ofOnlyElement(WaypointRecord waypoint) {
    return new SegmentRecord(ImmutableList.of(waypoint));
  }
}
