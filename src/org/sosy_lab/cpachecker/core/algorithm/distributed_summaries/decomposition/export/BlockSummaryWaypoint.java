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
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockSummaryWaypoint extends BlockWaypoint {

  @JsonProperty(value = "follow_branch")
  private final Optional<String> followBranch;

  @JsonProperty(value = "summary_blocks")
  private final List<String> summaryBlocks;

  public BlockSummaryWaypoint(
      String pType,
      String pAction,
      BlockLocation pLocation,
      Optional<String> pFollowBranch,
      List<String> pSummaryBlocks) {
    super(pType, pAction, pLocation);
    followBranch = pFollowBranch;
    summaryBlocks = pSummaryBlocks;
  }

  public BlockSummaryWaypoint(
      boolean pIsEntry,
      BlockLocation pLocation,
      Optional<String> pFollowBranch,
      List<String> pSummaryBlocks) {
    super("follow", pIsEntry ? "enter_summary" : "exit_summary", pLocation);
    followBranch = pFollowBranch;
    summaryBlocks = pSummaryBlocks;
  }

  public List<String> getSummaryBlocks() {
    return summaryBlocks;
  }

  public String getFollowBranch() {
    return followBranch.orElse(null);
  }
}
