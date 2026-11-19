// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockDescription {

  private final BlockLocation start;
  private final List<BlockSegment> segments;

  public BlockDescription(BlockLocation pStart, List<BlockSegment> pSegments) {
    start = pStart;
    segments = pSegments;
  }

  public BlockLocation getStart() {
    return start;
  }

  public List<BlockSegment> getSegments() {
    return segments;
  }
}
