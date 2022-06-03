// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import static org.sosy_lab.cpachecker.cfa.export.DOTBuilder.escapeGraphvizLabel;
import static org.sosy_lab.cpachecker.cfa.postprocessing.global.CFACloner.SEPARATOR;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_START;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * This class allows to dump functioncalls in a tree-like structure. For most cases the structure is
 * a tree, but for special cases the graph contains loops (-> recursion) or several root-nodes (->
 * one for each unused function).
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
    final CFAFunctionCallFinder finder = new CFAFunctionCallFinder(pCfa);
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
        filterCalls(finder.functionCalls, finder.threadCreations, mainFunction, usedFromMainOnly)) {
      for (final String calleeFunctionName : finder.functionCalls.get(callerFunctionName)) {
        final String caller = escape(escapedNames, callerFunctionName);
        final String callee = escape(escapedNames, calleeFunctionName);
        if (writtenNames.add(calleeFunctionName)) {
          pAppender.append(formatFunctionNode(finder, functionNames, calleeFunctionName, callee));
        }
        pAppender.append(String.format("%s -> %s;%n", caller, callee));
      }

      for (final String calleeFunctionName : finder.threadCreations.get(callerFunctionName)) {
        final String caller = escape(escapedNames, callerFunctionName);
        final String callee = escape(escapedNames, calleeFunctionName);
        if (writtenNames.add(calleeFunctionName)) {
          pAppender.append(formatFunctionNode(finder, functionNames, calleeFunctionName, callee));
        }
        pAppender.append(
            String.format(
                "%s -> %s [style=\"dashed\" label=\"%s\"];%n", caller, callee, THREAD_START));
      }
    }

    pAppender.append("}\n");
  }

  private static String formatFunctionNode(
      final CFAFunctionCallFinder finder,
      final Set<String> functionNames,
      final String calleeFunctionName,
      final String callee) {
    String label = calleeFunctionName;
    Collection<String> origNames = finder.originalNames.get(calleeFunctionName);
    origNames.remove(calleeFunctionName);
    if (!origNames.isEmpty()) {
      label += "\\n(" + Joiner.on(", ").join(origNames) + ")";
    }
    // different format for call to external function
    final String format =
        functionNames.contains(calleeFunctionName) ? "" : "shape=\"box\", color=grey";
    return String.format(
        "%s [label=\"%s\", %s];%n", callee, escapeGraphvizLabel(label, " "), format);
  }

  /**
   * return the set of functions to be exported.
   *
   * @param usedFromMainOnly whether to return only the "reachable" functions or "all" functions.
   */
  private static Set<String> filterCalls(
      final Multimap<String, String> functionCalls,
      final Multimap<String, String> threadCreations,
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
        worklist.addAll(threadCreations.get(nextFunction));
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

    /** contains the original names for functions */
    // TODO it would be nicer to avoid the intermediate step of collecting Strings
    // completely and directly use functionDeclarations.
    final Multimap<String, String> originalNames = LinkedHashMultimap.create();

    /** contains in which function we create a new thread starting with another function. */
    final Multimap<String, String> threadCreations = LinkedHashMultimap.create();

    private final CFA cfa;

    private CFAFunctionCallFinder(CFA pCfa) {
      cfa = pCfa;
    }

    @Override
    public TraversalProcess visitEdge(final CFAEdge pEdge) {
      switch (pEdge.getEdgeType()) {
        case CallToReturnEdge:
          {
            // the normal case of functioncall, both functions have their complete CFA
            final FunctionSummaryEdge function = (FunctionSummaryEdge) pEdge;
            final String functionName = function.getPredecessor().getFunctionName();
            final AFunctionDeclaration calledFunctionDecl =
                CFAUtils.leavingEdges(function.getPredecessor())
                    .filter(FunctionCallEdge.class)
                    .first()
                    .toJavaUtil()
                    .orElseThrow(() -> new IllegalStateException("internal function without body"))
                    .getSuccessor()
                    .getFunctionDefinition();
            functionCalls.put(functionName, calledFunctionDecl.getName());
            originalNames.put(calledFunctionDecl.getName(), calledFunctionDecl.getOrigName());
            break;
          }

        case StatementEdge:
          {
            final AStatementEdge edge = (AStatementEdge) pEdge;
            if (edge.getStatement() instanceof AFunctionCall) {
              // called function has no body, only declaration available, external function
              final AFunctionCall functionCall = (AFunctionCall) edge.getStatement();
              final AFunctionCallExpression functionCallExpression =
                  functionCall.getFunctionCallExpression();
              final AFunctionDeclaration declaration = functionCallExpression.getDeclaration();
              if (declaration != null) {
                final String functionName = pEdge.getPredecessor().getFunctionName();
                final String calledFunction = declaration.getName();
                functionCalls.put(functionName, calledFunction);
                originalNames.put(declaration.getName(), declaration.getOrigName());

                // for threads, we also collect function called via pthread_create
                AExpression functionNameExp = functionCallExpression.getFunctionNameExpression();
                List<? extends AExpression> params =
                    functionCall.getFunctionCallExpression().getParameterExpressions();
                if (functionNameExp instanceof AIdExpression
                    && THREAD_START.equals(((AIdExpression) functionNameExp).getName())
                    && params.get(2) instanceof CUnaryExpression) {
                  CExpression expr2 = ((CUnaryExpression) params.get(2)).getOperand();
                  if (expr2 instanceof CIdExpression) {
                    AFunctionDeclaration functionDecl =
                        (AFunctionDeclaration) ((CIdExpression) expr2).getDeclaration();
                    String calledThreadFunction = functionDecl.getName();
                    threadCreations.put(functionName, calledThreadFunction);
                    originalNames.put(functionDecl.getName(), functionDecl.getOrigName());
                    for (String name : cfa.getAllFunctions().keySet()) {
                      if (name.startsWith(calledThreadFunction + SEPARATOR)) {
                        threadCreations.put(functionName, name);
                        originalNames.put(name, calledThreadFunction);
                      }
                    }
                  }
                }
              }
            }
            break;
          }

        case FunctionCallEdge:
          {
            throw new AssertionError("traversal-strategy should ignore functioncalls");
          }

        default:
          // nothing to do

      }
      return TraversalProcess.CONTINUE;
    }
  }
}
