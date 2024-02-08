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
import java.util.Collections;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.CheckingCfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.ConsistentCfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.ForwardingCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public class MutableCFA extends ForwardingCfaNetwork implements CFA {

  // all entries in `functions` must also be in `allNodes`
  private final NavigableMap<String, FunctionEntryNode> functions;
  private final TreeMultimap<String, CFANode> allNodes;

  private CfaMetadata metadata;

  private final CfaNetwork network;

  /**
   * Creates a new {@link MutableCFA}.
   *
   * @param pFunctions a mapping of function names to corresponding function entry nodes
   * @param pAllNodes a mapping of function names to nodes of the corresponding function
   * @param pCfaMetadata the metadata of the CFA
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if {@code pFunctions} and {@code pAllNodes} do not contain the
   *     same functions
   * @throws IllegalArgumentException if {@code pAllNodes} doesn't contain all entries in {@code
   *     pFunctions}
   * @throws IllegalArgumentException if not all parameters have the same main function entry node
   */
  public MutableCFA(
      NavigableMap<String, FunctionEntryNode> pFunctions,
      TreeMultimap<String, CFANode> pAllNodes,
      CfaMetadata pCfaMetadata) {
    FunctionEntryNode mainFunctionEntry = pCfaMetadata.getMainFunctionEntry();
    checkArgument(pFunctions.keySet().equals(pAllNodes.keySet()));
    checkArgument(mainFunctionEntry.equals(pFunctions.get(mainFunctionEntry.getFunctionName())));

    functions = pFunctions;
    allNodes = pAllNodes;

    metadata = pCfaMetadata;

    network = CheckingCfaNetwork.wrapIfAssertionsEnabled(new DelegateCfaNetwork());
  }

  @Override
  public ImmutableCFA immutableCopy() {
    return new ImmutableCFA(functions, allNodes, metadata);
  }

  @Override
  protected CfaNetwork delegate() {
    return network;
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
  public int getNumberOfFunctions() {
    return functions.size();
  }

  @Override
  public NavigableSet<String> getAllFunctionNames() {
    return Collections.unmodifiableNavigableSet(functions.navigableKeySet());
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

  public void setASTStructure(ASTStructure pASTStructure) {
    metadata = metadata.withASTStructure(pASTStructure);
  }

  public void setLoopStructure(LoopStructure pLoopStructure) {
    metadata = metadata.withLoopStructure(pLoopStructure);
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

  private final class DelegateCfaNetwork extends ConsistentCfaNetwork {

    @Override
    public Set<CFANode> nodes() {
      return duplicateFreeNodeCollectionToSet(allNodes.values());
    }

    @Override
    public Set<FunctionEntryNode> entryNodes() {
      return duplicateFreeNodeCollectionToSet(functions.values());
    }
  }
}
