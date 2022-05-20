// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.global;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.FunctionCallCollector;
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
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix = "cfa.functionCalls")
public class FunctionCallUnwinder {

  @Option(secure=true, description = "how often can a function appear in the callstack as a clone of the original function?")
  private int recursionDepth = 5;

  // TODO find better name, it is not always recursive
  private final static String RECURSION_SEPARATOR = "__recursive_call__";

  private final MutableCFA cfa;

  public FunctionCallUnwinder(final MutableCFA pCfa, final Configuration config)
          throws InvalidConfigurationException {
    config.inject(this);
    this.cfa = pCfa;

    if (cfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "Function-call unwinding is only supported for C code.");
    }
  }

  public MutableCFA unwindRecursion() {
    assert cfa.getLanguage() == Language.C;

    // copy content of old CFAs
    final NavigableMap<String, FunctionEntryNode> functions = new TreeMap<>(cfa.getAllFunctions());
    final TreeMultimap<String, CFANode> nodes = TreeMultimap.create();
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
      Preconditions.checkArgument(functions.containsKey(functionname), "function %s not available", functionname);
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

    return new MutableCFA(
        cfa.getMachineModel(),
        functions,
        nodes,
        cfa.getMainFunction(),
        cfa.getFileNames(),
        cfa.getLanguage());
  }

  static void replaceFunctionCall(final AStatementEdge functionCallEdge, final String newFunctionName) {
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
  private static void cloneFunction(final String oldFunctionname, final String newFunctionname,
      final Map<String, FunctionEntryNode> functions, final SortedSetMultimap<String, CFANode> nodes) {
    Preconditions.checkArgument(!functions.containsKey(newFunctionname), "function exists, cloning is not allowed.");

    // clone
    final FunctionEntryNode entryNode = functions.get(oldFunctionname);
    final Pair<FunctionEntryNode, Collection<CFANode>> newFunction = FunctionCloner.cloneCFA(entryNode, newFunctionname);

    // add new function to CFA
    functions.put(newFunctionname, newFunction.getFirst());
    nodes.putAll(newFunctionname, newFunction.getSecond());
  }

  static @Nullable String getNameOfFunction(final AStatementEdge edge) {
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
  static boolean isFunctionCall(final AStatementEdge edge, final Collection<String> cfaFunctions) {
    // declaration == null -> functionPointer
    // functionName exists in CFA -> functioncall with CFA for called function
    //          -> we assume an original CFA, not a clone
    // otherwise: call of non-existent function, example: nondet_int()
    final String functionname = getNameOfFunction(edge);
    return functionname != null && cfaFunctions.contains(functionname);
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
      index = Integer.parseInt(function.substring(i + RECURSION_SEPARATOR.length())) + 1;
      function = function.substring(0, i);
    }
    return function + RECURSION_SEPARATOR + index;
  }
}
