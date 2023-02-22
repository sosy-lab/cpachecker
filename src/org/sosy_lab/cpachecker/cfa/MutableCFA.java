// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.TreeMultimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.graph.MutableCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public class MutableCFA implements CFA {

  // all entries in `functions` must also be in `allNodes`
  private final NavigableMap<String, FunctionEntryNode> functions;
  private final TreeMultimap<String, CFANode> allNodes;

  private CfaMetadata metadata;

  /**
   * Creates a new {@link MutableCFA}.
   *
   * @param pFunctions a mapping of function names to corresponding function entry nodes
   * @param pAllNodes a mapping of function names to nodes of the corresponding function
   * @param pCfaMetadata the metadata of the CFA
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if {@code pFunction} and {@code pAllNodes} do not contain the
   *     same functions
   * @throws IllegalArgumentException if {@code pAllNodes} doesn't contain all entries in {@code
   *     pFunctions}
   * @throws IllegalArgumentException if not all parameters have the same main function entry node
   */
  public MutableCFA(
      NavigableMap<String, FunctionEntryNode> pFunctions,
      TreeMultimap<String, CFANode> pAllNodes,
      CfaMetadata pCfaMetadata) {
    checkArgument(pFunctions.keySet().equals(pAllNodes.keySet()));
    checkArgument(pAllNodes.entries().containsAll(pFunctions.entrySet()));
    FunctionEntryNode mainFunctionEntry = pCfaMetadata.getMainFunctionEntry();
    checkArgument(mainFunctionEntry.equals(pFunctions.get(mainFunctionEntry.getFunctionName())));

    functions = pFunctions;
    allNodes = pAllNodes;

    metadata = pCfaMetadata;
  }

  /**
   * Adds the specified node to this CFA, if it is not already present.
   *
   * @param pNode the node to add to this CFA
   * @return {@code true} if this CFA was modified as a result of the call
   * @throws NullPointerException if {@code pNode == null}
   * @throws IllegalStateException if adding the new node would lead to multiple function entry
   *     nodes for the same function
   */
  @CanIgnoreReturnValue
  public boolean addNode(CFANode pNode) {
    String functionName = pNode.getFunctionName();
    if (pNode instanceof FunctionEntryNode entryNode) {
      checkState(
          !functions.containsKey(functionName),
          "Cannot add multiple function entry nodes for function '%s'",
          functionName);
      functions.put(functionName, entryNode);
    }
    return allNodes.put(functionName, pNode);
  }

  public void clear() {
    functions.clear();
    allNodes.clear();
  }

  /**
   * Removes the specified node from this CFA, if it is present.
   *
   * @param pNode the node to remove from this CFA
   * @return {@code true} if this CFA was modified as a result of the call
   * @throws NullPointerException if {@code pNode == null}
   */
  @CanIgnoreReturnValue
  public boolean removeNode(CFANode pNode) {
    String functionName = pNode.getFunctionName();
    if (pNode instanceof FunctionEntryNode && pNode.equals(functions.get(functionName))) {
      functions.remove(functionName);
    }
    return allNodes.remove(functionName, pNode);
  }

  @Override
  public boolean isEmpty() {
    return functions.isEmpty();
  }

  @Override
  public int getNumberOfFunctions() {
    return functions.size();
  }

  @Override
  public NavigableSet<String> getAllFunctionNames() {
    return Collections.unmodifiableNavigableSet(functions.navigableKeySet());
  }

  @Override
  public Collection<FunctionEntryNode> getAllFunctionHeads() {
    return Collections.unmodifiableCollection(functions.values());
  }

  @Override
  public FunctionEntryNode getFunctionHead(String pName) {
    return functions.get(pName);
  }

  @Override
  public NavigableMap<String, FunctionEntryNode> getAllFunctions() {
    return Collections.unmodifiableNavigableMap(functions);
  }

  public NavigableSet<CFANode> getFunctionNodes(String pName) {
    return Collections.unmodifiableNavigableSet(allNodes.get(pName));
  }

  @Override
  public Collection<CFANode> getAllNodes() {
    return Collections.unmodifiableCollection(allNodes.values());
  }

  public void setLoopStructure(LoopStructure pLoopStructure) {
    metadata = metadata.withLoopStructure(pLoopStructure);
  }

  public ImmutableCFA makeImmutableCFA(Optional<VariableClassification> pVarClassification) {
    return new ImmutableCFA(
        functions, allNodes, metadata.withVariableClassification(pVarClassification.orElse(null)));
  }

  public void setVariableClassification(@Nullable VariableClassification pVariableClassification) {
    metadata = metadata.withVariableClassification(pVariableClassification);
  }

  public void setLiveVariables(LiveVariables pLiveVariables) {
    metadata = metadata.withLiveVariables(pLiveVariables);
  }

  @Override
  public CfaMetadata getMetadata() {
    return metadata;
  }

  /**
   * Sets the metadata associated with this CFA.
   *
   * @param pCfaMetadata the metadata for this CFA
   * @throws NullPointerException if {@code pCfaMetadata == null}
   */
  public void setMetadata(CfaMetadata pCfaMetadata) {
    metadata = checkNotNull(pCfaMetadata);
  }

  /**
   * Returns a {@link MutableCfaNetwork} for this {@link MutableCFA}.
   *
   * <p>All modifying operations on the returned {@link MutableCfaNetwork} change this {@link
   * MutableCFA}.
   *
   * <p>All changes to this CFA are reflected in the returned {@link MutableCfaNetwork}. The CFA
   * represented by the returned {@link MutableCfaNetwork} always matches the CFA represented by its
   * elements (e.g., {@link CFAEdge#getSuccessor()} and {@link MutableCfaNetwork#successor(CFAEdge)}
   * always return the same value). Endpoints of a CFA edge and endpoints given as arguments to an
   * {@code addEdge} method must match.
   *
   * <p>IMPORTANT: The specified CFA must not contain any parallel edges (i.e., edges that connect
   * the same nodes in the same order) and never add them in the future. Additionally, the
   * collections returned by {@link CFA#getAllNodes()} and {@link CFA#getAllFunctionHeads()} must
   * not contain any duplicates and never add them in the future. Be aware that these requirements
   * are not enforced if Java assertions are disabled.
   *
   * @return a {@link MutableCfaNetwork} for this {@link MutableCFA}
   */
  @Override
  public MutableCfaNetwork asNetwork() {
    return MutableCfaNetwork.wrap(this);
  }
}
