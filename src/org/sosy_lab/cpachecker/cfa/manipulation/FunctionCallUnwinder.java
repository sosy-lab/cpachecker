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
package org.sosy_lab.cpachecker.cfa.manipulation;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

@Options(prefix = "cfa.functionCalls")
public class FunctionCallUnwinder {

  @Option(description = "how often can a function appear in the callstack as a clone of the original function?")
  private int recursionDepth = 5;

  // TODO find better name, it is not always recursive
  private final static String RECURSION_SEPARATOR = "__recursive_call__";

  private final MutableCFA cfa;
  private final LogManager logger;

  public FunctionCallUnwinder(final MutableCFA pCfa, final Configuration config, final LogManager pLogger)
          throws InvalidConfigurationException {
    config.inject(this);
    this.cfa = pCfa;
    this.logger = pLogger;
  }

  public MutableCFA unwindRecursion() {
    if (cfa.getLanguage() != Language.C) {
      // TODO throw exception?
      logger.log(Level.INFO, "FunctionCallUnwinder does only support C.");
      return cfa;
    }

    // copy content of old CFAs
    final Map<String, FunctionEntryNode> functions = new LinkedHashMap<>(cfa.getAllFunctions());
    final SortedSetMultimap<String, CFANode> nodes = TreeMultimap.create();
    for (final String function : cfa.getAllFunctionNames()) {
      nodes.putAll(function, cfa.getFunctionNodes(function));
    }

    final Multimap<String, String> reverseCallGraph = HashMultimap.create();
    final Set<String> finished = new HashSet<>();
    final Deque<String> waitlist = new ArrayDeque<>();
    waitlist.add(cfa.getMainFunction().getFunctionName());

    while (!waitlist.isEmpty()) {
      final String functionname = waitlist.pop();

      if (!finished.add(functionname)) {
        continue;
      }

      // get CFA for functionname
      Preconditions.checkArgument(functions.containsKey(functionname), "function not available: " + functionname);
      FunctionEntryNode entryNode = functions.get(functionname);

      // get functioncalls from the CFA
      final FunctionCallCollector visitor = new FunctionCallCollector();
      CFATraversal.dfs().traverseOnce(entryNode, visitor);
      final Collection<AStatementEdge> functionCalls = visitor.getFunctionCalls();

      // unwind recursion
      for (AStatementEdge statementEdge : functionCalls) {

        if (!isFunctionCall(statementEdge, functions.keySet())) {
          continue;
        }

        final String calledFunction = getNameOfFunction(statementEdge);
        String newFunctionname = calledFunction;
        if (isCallStackSizeReached(calledFunction, reverseCallGraph)) {
          // ignore, we have "bounded" recursion unwinding

        } else { // further unwinding allowed
          while (isFatherOf(functionname, newFunctionname, reverseCallGraph)) {
            newFunctionname = incrementFunctionname(newFunctionname);
          }

          if (!calledFunction.equals(newFunctionname)) {
            // if we have found recursion and need a functioncall-replacement
            if (!functions.containsKey(newFunctionname)) {
              cloneFunction(calledFunction, newFunctionname, functions, nodes);
            }

            // redirect from caller to new (cloned) called function
            replaceFunctionCall(statementEdge, newFunctionname);
          }
        }

        waitlist.add(newFunctionname);
        reverseCallGraph.put(newFunctionname, functionname);
      }
    }

    return new MutableCFA(cfa.getMachineModel(), functions, nodes, cfa.getMainFunction(), cfa.getLanguage());
  }

  private void replaceFunctionCall(final AStatementEdge functionCallEdge, final String newFunctionName) {
    final CFANode pred = functionCallEdge.getPredecessor();
    final CFANode succ = functionCallEdge.getSuccessor();
    final AFunctionCall call = (AFunctionCall)functionCallEdge.getStatement();

    final AStatementEdge newEdge;
    if (call instanceof CFunctionCall) {

      // get old values
      final CDeclaration declaration = ((CFunctionCall) call).getFunctionCallExpression().getDeclaration();
      Preconditions.checkNotNull(declaration);
      final String oldFunctionName = declaration.getQualifiedName();

      // build new edge
      final FunctionCloner fc = new FunctionCloner(oldFunctionName, newFunctionName, true);
      newEdge = fc.cloneEdge(functionCallEdge, pred, succ);
    } else {
      // TODO support JAVA
      throw new AssertionError("unsupported edge: " + functionCallEdge);
    }

    // replace edge
    CFACreationUtils.removeEdgeFromNodes(functionCallEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);
  }

  /** clones a function and adds it to the maps. */
  private void cloneFunction(final String oldFunctionname, final String newFunctionname,
      final Map<String, FunctionEntryNode> functions, final SortedSetMultimap<String, CFANode> nodes) {
    Preconditions.checkArgument(!functions.containsKey(newFunctionname), "function exists, cloning is not allowed.");

    // clone
    final FunctionEntryNode entryNode = functions.get(oldFunctionname);
    final Pair<FunctionEntryNode, Collection<CFANode>> newFunction = FunctionCloner.cloneCFA(entryNode, newFunctionname);

    // add new function to CFA
    functions.put(newFunctionname, newFunction.getFirst());
    nodes.putAll(newFunctionname, newFunction.getSecond());
  }

  private static String getNameOfFunction(final AStatementEdge edge) {
    if (!(edge instanceof CStatementEdge)) {
      return null;
    }
    final CStatement statement = ((CStatementEdge) edge).getStatement();
    if (!(statement instanceof CFunctionCall)) {
      return null;
    }
    final CDeclaration declaration = ((CFunctionCall) statement).getFunctionCallExpression().getDeclaration();
    if (declaration == null) {
      return null;
    }
    return declaration.getQualifiedName();
  }

  /** returns, iff the edge contains a functioncall to another CFA. */
  private static boolean isFunctionCall(final AStatementEdge edge, final Collection<String> cfaFunctions) {
    // declaration == null -> functionPointer
    // functionName exists in CFA -> functioncall with CFA for called function
    //          -> we assume an original CFA, not a clone
    // otherwise: call of non-existent function, example: nondet_int()
    final String functionname = getNameOfFunction(edge);
    return functionname != null && (cfaFunctions.contains(functionname));
  }

  /** checks, iff there is an call-stack from father to child.
   * In the graph this would be a way from father to child.
   * It should be more efficient to search backwards,
   * because children have only one father in most cases. */
  private static boolean isFatherOf(final String child, final String possibleFather, final Multimap<String, String> reverseGraph) {
    final Set<String> finished = new HashSet<>();
    final Deque<String> waitlist = new ArrayDeque<>();
    waitlist.add(child);
    while (!waitlist.isEmpty()) {
      String current = waitlist.pop();
      if (!finished.add(current)) {
        continue;
      }
      if (current.equals(possibleFather)) {
        return true;
      }
      waitlist.addAll(reverseGraph.get(current));
    }
    return false;
  }

  /** checks maximum size of callstack.
   * TODO implement better user-defined limits */
  private boolean isCallStackSizeReached(final String calledFunction, final Multimap<String, String> reverseGraph) {
    final Collection<String> functions = reverseGraph.keySet();
    int maxDepth = 0;
    for (String function : functions) {
      if (function.startsWith(calledFunction)) {
        int index = function.indexOf(RECURSION_SEPARATOR);
        if (index != -1) {
          int depth = Integer.parseInt(function.substring(index + RECURSION_SEPARATOR.length()));
          maxDepth = Math.max(maxDepth,depth);
        }
      }
    }
    return maxDepth >= recursionDepth;
  }

  private String incrementFunctionname(String function) {
    int i = function.indexOf(RECURSION_SEPARATOR);
    int index = 1;
    if (i != -1) {
      index = Integer.parseInt(function.substring(i + RECURSION_SEPARATOR.length(), function.length())) + 1;
      function = function.substring(0, i);
    }
    return function + RECURSION_SEPARATOR + index;
  }
}
