// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;

/**
 * This Visitor collects all functioncalls. It should visit the CFA of each functions BEFORE
 * creating super-edges (functioncall- and return-edges).
 */
public class FunctionCallCollector extends CFATraversal.DefaultCFAVisitor {
  // TODO this class is copied from CFASecondPassBuilder, can we merge this class with the other
  // visitor?
  // TODO in FunctionCallDumper there exists a similiar class, should we merge?

  private final List<AStatementEdge> functionCalls = new ArrayList<>();

  public Collection<AStatementEdge> getFunctionCalls() {
    return Collections.unmodifiableCollection(functionCalls);
  }

  @Override
  public CFATraversal.TraversalProcess visitEdge(final CFAEdge pEdge) {
    switch (pEdge.getEdgeType()) {
      case StatementEdge:
        {
          final AStatementEdge edge = (AStatementEdge) pEdge;
          if (edge.getStatement() instanceof AFunctionCall) {
            functionCalls.add(edge);
          }
          break;
        }

      case FunctionCallEdge:
      case FunctionReturnEdge:
      case CallToReturnEdge:
        throw new AssertionError("functioncall- and return-edges should not exist at this time.");
      default:
        // nothing to do
    }
    return CFATraversal.TraversalProcess.CONTINUE;
  }
}
