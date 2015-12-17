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
package org.sosy_lab.cpachecker.cfa.export;

import java.io.IOException;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;


/** This class allows to dump functioncalls in a tree-like structure.
 * For most cases the structure is a tree, but for special cases the graph contains
 * loops (-> recursion) or several root-nodes (-> one for each unused function).
 */
public class FunctionCallDumper {

  /** This method iterates over the CFA, searches for functioncalls
   * and after that, dumps them in a dot-format. */
  public static void dump(final Appendable pAppender, final CFA pCfa) throws IOException {

    // get all function calls
    final CFAFunctionCallFinder finder = new CFAFunctionCallFinder();
    for (final FunctionEntryNode entryNode : pCfa.getAllFunctionHeads()) {
      CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(entryNode, finder);
    }

    // build dot-file
    pAppender.append("digraph functioncalls {\n");
    pAppender.append("rankdir=LR;\n\n"); // node-order from Left to Right is nicer

    // external functions are not part of functionNames
    final Set<String> functionNames = pCfa.getAllFunctionNames();

    final String mainFunction = pCfa.getMainFunction().getFunctionName();
    pAppender.append(mainFunction + " [shape=\"box\", color=blue];\n");

    for (final String callerFunctionName : finder.functionCalls.keySet()) {
      for (final String calleeFunctionName : finder.functionCalls.get(callerFunctionName)) {
          // call to external function
          if (!functionNames.contains(calleeFunctionName)) {
            pAppender.append(calleeFunctionName + " [shape=\"box\", color=grey];\n");
          }

          pAppender.append(callerFunctionName + " -> " + calleeFunctionName + ";\n");
      }
    }

    pAppender.append("}\n");
  }

  private static class CFAFunctionCallFinder extends DefaultCFAVisitor {

    /** contains pairs of (functionname, calledFunction) */
    final Multimap<String, String> functionCalls = LinkedHashMultimap.create();

    @Override
    public TraversalProcess visitEdge(final CFAEdge pEdge) {
      switch (pEdge.getEdgeType()) {

      case CallToReturnEdge: {
        // the normal case of functioncall, both functions have their complete CFA
        final FunctionSummaryEdge function = (FunctionSummaryEdge) pEdge;
        final String functionName = function.getPredecessor().getFunctionName();
        final String calledFunction = function.getPredecessor().getLeavingEdge(0).getSuccessor().getFunctionName();
        functionCalls.put(functionName, calledFunction);
        break;
      }

      case StatementEdge: {
        final AStatementEdge edge = (AStatementEdge) pEdge;
        if (edge.getStatement() instanceof AFunctionCall) {
          // called function has no body, only declaration available, external function
          final AFunctionCall functionCall = (AFunctionCall) edge.getStatement();
          final AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
          final AFunctionDeclaration declaration = functionCallExpression.getDeclaration();
          if (declaration != null) {
            final String functionName = pEdge.getPredecessor().getFunctionName();
            final String calledFunction = declaration.getName();
            functionCalls.put(functionName, calledFunction);
          }
        }
        break;
      }

      case FunctionCallEdge: {
        throw new AssertionError("traversal-strategy should ignore functioncalls");
      }

      default:
         // nothing to do

      }
      return TraversalProcess.CONTINUE;
    }
  }
}
