// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.SetMultimap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.CheckingCfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.ConsistentCfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.ForwardingCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

/**
 * This class represents a CFA after it has been fully created (parsing, linking of functions,
 * etc.).
 */
public class ImmutableCFA extends ForwardingCfaNetwork implements CFA {

  private final ImmutableSortedMap<String, FunctionEntryNode> functions;
  private final ImmutableSortedSet<CFANode> allNodes;
  private final ImmutableSet<CFAEdge> allEdges;

  private final CfaMetadata metadata;

  private final CfaNetwork network;

  ImmutableCFA(
      Map<String, FunctionEntryNode> pFunctions,
      SetMultimap<String, CFANode> pAllNodes,
      CfaMetadata pCfaMetadata) {
    functions = ImmutableSortedMap.copyOf(pFunctions);
    allNodes = ImmutableSortedSet.copyOf(pAllNodes.values());

    metadata = pCfaMetadata;

    FunctionEntryNode mainFunctionEntry = pCfaMetadata.getMainFunctionEntry();
    checkArgument(mainFunctionEntry.equals(functions.get(mainFunctionEntry.getFunctionName())));

    network =
        CheckingCfaNetwork.wrapIfAssertionsEnabled(
            new DelegateCfaNetwork(allNodes, ImmutableSet.copyOf(functions.values())));
    allEdges = ImmutableSet.copyOf(super.edges());
  }

  public ImmutableCFA(
      Map<String, FunctionEntryNode> pFunctions, Set<CFANode> pAllNodes, CfaMetadata pCfaMetadata) {
    functions = ImmutableSortedMap.copyOf(pFunctions);
    allNodes = ImmutableSortedSet.copyOf(pAllNodes);

    metadata = pCfaMetadata;

    FunctionEntryNode mainFunctionEntry = pCfaMetadata.getMainFunctionEntry();
    checkArgument(mainFunctionEntry.equals(functions.get(mainFunctionEntry.getFunctionName())));

    network =
        CheckingCfaNetwork.wrapIfAssertionsEnabled(
            new DelegateCfaNetwork(allNodes, ImmutableSet.copyOf(functions.values())));
    // this must happen after 'network' is assigned. Network is the delegate of this class.
    allEdges = ImmutableSet.copyOf(super.edges());
  }

  @Override
  public Set<CFAEdge> edges() {
    return allEdges;
  }

  @Override
  public ImmutableCFA immutableCopy() {
    return this;
  }

  @Override
  protected CfaNetwork delegate() {
    return network;
  }

  @Override
  public ImmutableSortedSet<CFANode> nodes() {
    // we can directly return `allNodes`, no need to use the delegate `CfaNetwork`
    return allNodes;
  }

  @Override
  public ImmutableSet<FunctionEntryNode> entryNodes() {
    // we are sure that the delegate `CfaNetwork` always returns an `ImmutableSet`
    return (ImmutableSet<FunctionEntryNode>) network.entryNodes();
  }

  @Override
  public int getNumberOfFunctions() {
    return functions.size();
  }

  @Override
  public ImmutableSortedSet<String> getAllFunctionNames() {
    return functions.keySet();
  }

  @Override
  public FunctionEntryNode getFunctionHead(String name) {
    return functions.get(name);
  }

  @Override
  public ImmutableSortedMap<String, FunctionEntryNode> getAllFunctions() {
    return functions;
  }

  @Override
  public CfaMetadata getMetadata() {
    return metadata;
  }

  private static class DelegateCfaNetwork extends ConsistentCfaNetwork {

    private final ImmutableSet<CFANode> nodes;
    private final ImmutableSet<FunctionEntryNode> entryNodes;

    private DelegateCfaNetwork(
        ImmutableSet<CFANode> pNodes, ImmutableSet<FunctionEntryNode> pEntryNodes) {
      nodes = pNodes;
      entryNodes = pEntryNodes;
    }

    @Override
    public Set<CFANode> nodes() {
      return nodes;
    }

    @Override
    public Set<FunctionEntryNode> entryNodes() {
      return entryNodes;
    }
  }
}
