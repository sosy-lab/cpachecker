// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockSegment {

  @JsonProperty(value = "waypoint")
  private final List<BlockWaypoint> waypoints;

  public BlockSegment(List<BlockWaypoint> pWaypoints) {
    waypoints = pWaypoints;
  }

  public List<BlockWaypoint> getWaypoints() {
    return waypoints;
  }
}
