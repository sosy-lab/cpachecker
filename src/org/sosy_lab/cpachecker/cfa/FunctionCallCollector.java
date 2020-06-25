/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;

/** This Visitor collects all functioncalls.
 *  It should visit the CFA of each functions BEFORE creating super-edges (functioncall- and return-edges). */
public class FunctionCallCollector extends CFATraversal.DefaultCFAVisitor {
  // TODO this class is copied from CFASecondPassBuilder, can we merge this class with the other visitor?
  // TODO in FunctionCallDumper there exists a similiar class, should we merge?

  private final List<AStatementEdge> functionCalls = new ArrayList<>();

  public Collection<AStatementEdge> getFunctionCalls() {
    return Collections.unmodifiableCollection(functionCalls);
  }

  @Override
  public CFATraversal.TraversalProcess visitEdge(final CFAEdge pEdge) {
    switch (pEdge.getEdgeType()) {
      case StatementEdge: {
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
