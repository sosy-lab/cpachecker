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
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.CheckingCfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.ConsistentCfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.ForwardingCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * This class represents a CFA after it has been fully created (parsing, linking of functions,
 * etc.).
 */
class ImmutableCFA extends ForwardingCfaNetwork implements CFA, Serializable {

  private static final long serialVersionUID = 5399965350156780812L;

  private final ImmutableSortedMap<String, FunctionEntryNode> functions;
  private final ImmutableSortedSet<CFANode> allNodes;

  private final CfaMetadata metadata;

  // `network` isn't `final` due to serialization, but shouldn't be reassigned anywhere else
  private transient CfaNetwork network;

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
  }

  @Override
  public CFA immutableCopy() {
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

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {

    // write default stuff
    s.defaultWriteObject();

    // we have to keep the order of edges 'AS IS'
    final List<CFAEdge> enteringEdges = new ArrayList<>();
    for (CFANode node : allNodes) {
      Iterables.addAll(enteringEdges, CFAUtils.enteringEdges(node));
    }
    s.writeObject(enteringEdges);

    // we have to keep the order of edges 'AS IS'
    final List<CFAEdge> leavingEdges = new ArrayList<>();
    for (CFANode node : allNodes) {
      Iterables.addAll(leavingEdges, CFAUtils.leavingEdges(node));
    }
    s.writeObject(leavingEdges);
  }

  @SuppressWarnings("unchecked")
  private void readObject(java.io.ObjectInputStream s)
      throws java.io.IOException, ClassNotFoundException {

    // read default stuff
    s.defaultReadObject();

    // read entering edges, we have to keep the order of edges 'AS IS'
    for (CFAEdge edge : (List<CFAEdge>) s.readObject()) {
      edge.getSuccessor().addEnteringEdge(edge);
    }

    // read leaving edges, we have to keep the order of edges 'AS IS'
    for (CFAEdge edge : (List<CFAEdge>) s.readObject()) {
      edge.getPredecessor().addLeavingEdge(edge);
    }

    network =
        CheckingCfaNetwork.wrapIfAssertionsEnabled(
            new DelegateCfaNetwork(allNodes, ImmutableSet.copyOf(functions.values())));
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
