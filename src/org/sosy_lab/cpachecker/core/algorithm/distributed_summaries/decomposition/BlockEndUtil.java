// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.graph.FlexCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class BlockEndUtil {

  public static final String UNIQUE_DESCRIPTION = "<<distributed-block-summary-block-end>>";

  public static BlankEdge getBlockEndBlankEdge(FileLocation pFileLocation, CFANode pPredecessor) {
    return new BlankEdge(
        "",
        pFileLocation,
        pPredecessor,
        new CFANode(pPredecessor.getFunction()),
        UNIQUE_DESCRIPTION);
  }
}
