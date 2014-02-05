/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;


/** This class allows to dump functioncalls in a tree-like structure.
 * If the sourcefile contains recursion, there are loops.
 * Normally there is only one root-node. However in special cases,
 * we declare functions only in the "init-global-vars-block" and never use them.
 * Then they appear as additional roots in the graph. */
public class FunctionCallDumper {

  /** This method iterates over the CFA, searches for functioncalls
   * and after that, dumps them in a dot-format. */
  public static void dump(final Appendable pAppender, final CFA pCfa) throws IOException {

    final FunctionEntryNode main = pCfa.getMainFunction();
    final String mainFunction = main.getFunctionName();

    // get all function calls
    final CFAFunctionCallFinder finder = new CFAFunctionCallFinder();
    final FunctionEntryNode functionEntry = pCfa.getFunctionHead(mainFunction);
    CFATraversal.dfs().traverseOnce(functionEntry, finder);

    // build dot-file
    pAppender.append("digraph functioncalls {\n");
    pAppender.append("rankdir=LR;\n\n"); // node-order from Left to Right is nicer

    // write each caller-callee pair only once
    final Set<Pair<String, String>> callerCalleePair = new HashSet<>();

    // external functions are not part of functionNames
    final Set<String> functionNames = pCfa.getAllFunctionNames();

    callerCalleePair.add(Pair.of("init", mainFunction));
    pAppender.append(mainFunction + " [shape=\"box\", color=blue];\n");

    for (final String callerFunctionName : finder.functionCalls.keys()) {
      for (final String calleeFunctionName : finder.functionCalls.get(callerFunctionName)) {
        if (callerCalleePair.add(Pair.of(callerFunctionName, calleeFunctionName))) {
          // call to external function
          if (!functionNames.contains(calleeFunctionName)) {
            pAppender.append(calleeFunctionName + " [shape=\"box\", color=grey];\n");
          }

          pAppender.append(callerFunctionName + " -> " + calleeFunctionName + ";\n");
        }
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

      case StatementEdge: {
        final AStatementEdge edge = (AStatementEdge) pEdge;
        if (edge.getStatement() instanceof AFunctionCall) {
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
        final String functionName = pEdge.getPredecessor().getFunctionName();
        final String calledFunction = pEdge.getSuccessor().getFunctionName();
        functionCalls.put(functionName, calledFunction);
        break;
      }

      }
      return TraversalProcess.CONTINUE;
    }
  }
}
