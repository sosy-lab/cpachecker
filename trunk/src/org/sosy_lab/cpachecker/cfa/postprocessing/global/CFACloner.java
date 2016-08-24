/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.postprocessing.global;

import com.google.common.base.Preconditions;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.FunctionCallCollector;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

@Options(prefix = "cfa.cfaCloner")
public class CFACloner {

  private final static String SEPARATOR = "__cloned_function__";

  private final CFA cfa;

  @Option(secure = true, description = "how often do we clone a function?")
  private int numberOfCopies = 5;

  public CFACloner(final CFA pCfa, Configuration pConfig)
      throws InvalidConfigurationException {
    this.cfa = pCfa;
    pConfig.inject(this);
  }

  public MutableCFA execute() {
    assert cfa.getLanguage() == Language.C;

    // copy content of old CFAs
    final SortedMap<String, FunctionEntryNode> functions = new TreeMap<>(cfa.getAllFunctions());
    final SortedSetMultimap<String, CFANode> nodes = TreeMultimap.create();
    for (final String function : cfa.getAllFunctionNames()) {
      if (cfa instanceof MutableCFA) {
        // it is more efficient to directly copy the nodes
        nodes.putAll(function, ((MutableCFA)cfa).getFunctionNodes(function));
      } else {
        nodes.putAll(function, CFATraversal.dfs().collectNodesReachableFrom(cfa.getFunctionHead(function)));
      }
    }

    for (String functionName : cfa.getAllFunctionNames()) {
      if (cfa.getMainFunction().getFunctionName().equals(functionName)) {
        continue; // ignore main function
      }

      final FunctionEntryNode entryNode = cfa.getFunctionHead(functionName);
      for (int i = 1; i <= numberOfCopies; i++) {
        final String newFunctionName = getFunctionName(functionName, i);

        Preconditions.checkArgument(!cfa.getAllFunctionNames().contains(newFunctionName));
        final Pair<FunctionEntryNode, Collection<CFANode>> newFunction = FunctionCloner.cloneCFA(entryNode, newFunctionName);
        functions.put(newFunctionName, newFunction.getFirst());
        nodes.putAll(newFunctionName, newFunction.getSecond());

        // get functioncalls from the CFA
        Preconditions.checkArgument(functions.containsKey(newFunctionName), "function %s not available", newFunctionName);
        final FunctionCallCollector visitor = new FunctionCallCollector();
        CFATraversal.dfs().traverseOnce(functions.get(newFunctionName), visitor);
        final Collection<AStatementEdge> functionCalls = visitor.getFunctionCalls();

        // redirect from caller to new (cloned) called function,
        // but only if the calling and the called function have equal clone-indices
        for (AStatementEdge statementEdge : functionCalls) {
          if (FunctionCallUnwinder.isFunctionCall(statementEdge, functions.keySet())) {
            final String calledFunctionName = FunctionCallUnwinder.getNameOfFunction(statementEdge);
            final String newCalledFunctionName = getFunctionName(calledFunctionName, i);
            FunctionCallUnwinder.replaceFunctionCall(statementEdge, newCalledFunctionName);
          }
        }
      }
    }

    return new MutableCFA(cfa.getMachineModel(), functions, nodes, cfa.getMainFunction(), cfa.getLanguage());
  }

  /** build a new name consisting of a function-name and an index. */
  public static String getFunctionName(String function, int index) {
    return function + SEPARATOR + index;
  }

  /** remove the index from a function-name if possible. */
  public static String extractFunctionName(String function) {
    final int index = function.indexOf(SEPARATOR);
    return index == -1 ? function : function.substring(0, index);
  }
}
