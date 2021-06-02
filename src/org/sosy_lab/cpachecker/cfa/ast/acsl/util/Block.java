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

public interface Block {

  boolean isFunction();

  boolean isLoop();

  int getStartOffset();

  int getEndOffset();

  Set<CFAEdge> getEnteringEdges();

  Set<CFAEdge> getLeavingEdges();

  Set<CFANode> getContainedNodes();

  default boolean contains(Block other) {
    return getStartOffset() < other.getStartOffset() && getEndOffset() > other.getEndOffset();
  }
}
