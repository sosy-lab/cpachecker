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
public class BlockMetadata {

  private final String id;
  private final List<String> predecessors;
  private final List<String> successors;
  private final BlockDescription description;

  public BlockMetadata(
      String pId,
      List<String> pPredecessors,
      List<String> pSuccessors,
      BlockDescription pDescription) {
    id = pId;
    predecessors = pPredecessors;
    successors = pSuccessors;
    description = pDescription;
  }

  public String getId() {
    return id;
  }

  public BlockDescription getDescription() {
    return description;
  }

  public List<String> getPredecessors() {
    return predecessors;
  }

  public List<String> getSuccessors() {
    return successors;
  }
}
