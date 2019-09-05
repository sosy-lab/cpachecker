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

import static org.sosy_lab.cpachecker.cfa.export.DOTBuilder.escapeGraphvizLabel;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** This class allows to dump functioncalls in a tree-like structure.
 * For most cases the structure is a tree, but for special cases the graph contains
 * loops (-> recursion) or several root-nodes (-> one for each unused function).
 */
public class FunctionCallDumper {

  /**
   * This method iterates over the CFA, searches for function calls and after that, dumps them in a
   * dot-format.
   *
   * @param usedFromMainOnly dump only function calls starting from main-function.
   */
  public static void dump(
      final Appendable pAppender, final CFA pCfa, final boolean usedFromMainOnly)
      throws IOException {

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

    final Map<String, String> escapedNames = new HashMap<>();
    final Set<String> writtenNames = new HashSet<>();

    for (final String callerFunctionName :
        filterCalls(finder.functionCalls, mainFunction, usedFromMainOnly)) {
      for (final String calleeFunctionName : finder.functionCalls.get(callerFunctionName)) {
        final String caller = escape(escapedNames, callerFunctionName);
        final String callee = escape(escapedNames, calleeFunctionName);
        if (writtenNames.add(calleeFunctionName)) {
          final String label = escapeGraphvizLabel(calleeFunctionName, " ");
          // different format for call to external function
          final String format =
              functionNames.contains(calleeFunctionName) ? "" : "shape=\"box\", color=grey";
          pAppender.append(String.format("%s [label=\"%s\", %s];%n", callee, label, format));
        }

        pAppender.append(caller + " -> " + callee + ";\n");
      }
    }

    pAppender.append("}\n");
  }

  /** return the set of functions to be exported. */
  private static Set<String> filterCalls(
      final Multimap<String, String> functionCalls,
      final String mainFunction,
      final boolean usedFromMainOnly) {
    if (!usedFromMainOnly) {
      return functionCalls.keySet();
    }

    Set<String> calls = new LinkedHashSet<>();
    Deque<String> worklist = new ArrayDeque<>();
    worklist.push(mainFunction);
    while (!worklist.isEmpty()) {
      String nextFunction = worklist.pop();
      if (calls.add(nextFunction)) {
        worklist.addAll(functionCalls.get(nextFunction));
      }
    }
    return calls;
  }

  /** escape non-graphviz-conform identifiers for nodes */
  private static String escape(Map<String, String> escapedNames, String functionName) {
    return escapedNames.computeIfAbsent(
        functionName,
        str -> str.matches("[a-zA-Z0-9_]*") ? str : ("escapedFunctionName_" + escapedNames.size()));
  }

  /** visitor for collecting dependencies, i.e., which function calls which function. */
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
            final Optional<FunctionCallEdge> calledFunction =
                CFAUtils.leavingEdges(function.getPredecessor())
                    .filter(FunctionCallEdge.class)
                    .first();
            Preconditions.checkState(calledFunction.isPresent(), "internal function without body");
            functionCalls.put(functionName, calledFunction.get().getSuccessor().getFunctionName());
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
