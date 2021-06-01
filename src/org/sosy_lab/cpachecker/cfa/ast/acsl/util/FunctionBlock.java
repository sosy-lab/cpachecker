// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.util;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;

class FunctionBlock implements Block {
  // All the necessary information can be derived from the FunctionEntryNode
  private final FunctionEntryNode function;

  FunctionBlock(FunctionEntryNode pFunction) {
    function = pFunction;
  }

  public boolean isFunction() {
    return true;
  }

  public boolean isLoop() {
    return false;
  }

  public int getStartOffset() {
    return function.getFileLocation().getNodeOffset();
  }

  public int getEndOffset() {
    return function.getFileLocation().getNodeOffset() + function.getFileLocation().getNodeLength();
  }

  @Override
  public Set<CFAEdge> getEnteringEdges() {
    Set<CFAEdge> enteringEdges = new HashSet<>();
    for (int i = 0; i < function.getNumEnteringEdges(); i++) {
      enteringEdges.add(function.getEnteringEdge(i));
    }
    return enteringEdges;
  }

  @Override
  public Set<CFAEdge> getLeavingEdges() {
    Set<CFAEdge> leavingEdges = new HashSet<>();
    for (int i = 0; i < function.getNumLeavingEdges(); i++) {
      leavingEdges.add(function.getLeavingEdge(i));
    }
    return leavingEdges;
  }

  public Set<CFANode> getContainedNodes() {
    CFATraversal traversal = CFATraversal.dfs();
    traversal = traversal.ignoreFunctionCalls();
    return traversal.collectNodesReachableFromTo(function, function.getExitNode());
  }

  public void addEnteringEdge(CFAEdge edge) {
    throw new UnsupportedOperationException();
  }

  public void addLeavingEdge(CFAEdge edge) {
    throw new UnsupportedOperationException();
  }
}
