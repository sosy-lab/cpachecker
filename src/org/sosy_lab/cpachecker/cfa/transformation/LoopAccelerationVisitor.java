// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

public class LoopAccelerationVisitor implements CFAVisitor {

  private final CFANode loopHead;
  private ImmutableList.Builder<CExpressionAssignmentStatement> statements;
  private Optional<CFAEdge> lastEdge;
  private Optional<CFANode> lastNode;
  private boolean success;

  public LoopAccelerationVisitor(CFANode pLoopHead) {
    loopHead = pLoopHead;
    statements = ImmutableList.builder();
    lastEdge = Optional.empty();
    lastNode = Optional.empty();
    success = false;
  }

  public Builder<CExpressionAssignmentStatement> getStatements() {
    return statements;
  }

  public Optional<CFAEdge> getLastEdge() {
    return lastEdge;
  }

  public Optional<CFANode> getLastNode() {
    return lastNode;
  }

  public boolean wasSuccesful() {
    return success;
  }

  @Override
  public TraversalProcess visitEdge(CFAEdge edge) {
    lastEdge = Optional.of(edge);
    lastNode = Optional.empty();
    switch (edge.getEdgeType()) {
      // todo safe to ignore blankedges??
      case BlankEdge:
        return TraversalProcess.CONTINUE;
      case AssumeEdge:
        return TraversalProcess.CONTINUE;
      case StatementEdge:
        CStatement statement = ((CStatementEdge) edge).getStatement();
        if (statement instanceof CExpressionAssignmentStatement assignmentStatement) {
          statements.add(assignmentStatement);
          return TraversalProcess.CONTINUE;
        }
        return TraversalProcess.ABORT;
      default:
        return TraversalProcess.ABORT;
    }
  }

  @Override
  public TraversalProcess visitNode(CFANode node) {
    lastNode = Optional.of(node);
    lastEdge = Optional.empty();
    if (node == loopHead) {
      success = true;
      return TraversalProcess.ABORT;
    }
    if (node.getLeavingEdges().size() != 1) {
      return TraversalProcess.ABORT;
    }
    return TraversalProcess.CONTINUE;
  }
}
