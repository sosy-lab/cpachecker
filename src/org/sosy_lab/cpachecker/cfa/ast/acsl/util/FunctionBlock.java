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

public class FunctionBlock implements SyntacticBlock {

  private final FunctionEntryNode function;

  public FunctionBlock(FunctionEntryNode pFunction) {
    function = pFunction;
  }

  @Override
  public boolean isFunction() {
    return true;
  }

  @Override
  public boolean isLoop() {
    return false;
  }

  @Override
  public int getStartOffset() {
    return function.getFileLocation().getNodeOffset();
  }

  @Override
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

  @Override
  public Set<CFANode> getContainedNodes() {
    CFATraversal traversal = CFATraversal.dfs();
    traversal = traversal.ignoreFunctionCalls();
    return traversal.collectNodesReachableFromTo(function, function.getExitNode());
  }
}
