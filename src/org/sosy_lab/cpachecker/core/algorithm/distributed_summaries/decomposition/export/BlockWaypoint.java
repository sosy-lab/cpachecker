// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.export;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockWaypoint {

  private final String type;
  private final String action;
  private final BlockLocation location;

  public BlockWaypoint(String pType, String pAction, BlockLocation pLocation) {
    type = pType;
    action = pAction;
    location = pLocation;
  }

  public BlockLocation getLocation() {
    return location;
  }

  public String getAction() {
    return action;
  }

  public String getType() {
    return type;
  }
}
