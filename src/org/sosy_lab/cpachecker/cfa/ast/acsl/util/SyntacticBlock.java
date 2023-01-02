// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.util;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public interface SyntacticBlock {

  boolean isFunction();

  boolean isLoop();

  int getStartOffset();

  int getEndOffset();

  /**
   * Returns the entering edges of the block. An edge is an entering edge if it is not inside the
   * block but the next concrete edge is.
   */
  Iterable<CFAEdge> getEnteringEdges();

  /**
   * Returns the leaving edges of the block. An edge is a leaving edge if it is inside the block but
   * the next concrete edge is not.
   */
  Iterable<CFAEdge> getLeavingEdges();

  Set<CFANode> getContainedNodes();

  default boolean contains(SyntacticBlock other) {
    return getStartOffset() < other.getStartOffset() && getEndOffset() > other.getEndOffset();
  }
}
