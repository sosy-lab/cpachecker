// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.util;

import com.google.common.collect.FluentIterable;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

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
  public FluentIterable<CFAEdge> getEnteringEdges() {
    return CFAUtils.enteringEdges(function);
  }

  @Override
  public FluentIterable<CFAEdge> getLeavingEdges() {
    return CFAUtils.leavingEdges(function);
  }

  @Override
  public Set<CFANode> getContainedNodes() {
    CFATraversal traversal = CFATraversal.dfs();
    traversal = traversal.ignoreFunctionCalls();
    return traversal.collectNodesReachableFromTo(function, function.getExitNode());
  }
}
