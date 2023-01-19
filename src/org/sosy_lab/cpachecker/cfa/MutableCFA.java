// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.TreeMultimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public class MutableCFA implements CFA {

  private final NavigableMap<String, FunctionEntryNode> functions;
  private final TreeMultimap<String, CFANode> allNodes;

  private CfaMetadata metadata;

  public MutableCFA(
      NavigableMap<String, FunctionEntryNode> pFunctions,
      TreeMultimap<String, CFANode> pAllNodes,
      CfaMetadata pCfaMetadata) {

    functions = pFunctions;
    allNodes = pAllNodes;

    metadata = pCfaMetadata;

    assert functions.keySet().equals(allNodes.keySet());
    FunctionEntryNode mainFunctionEntry = pCfaMetadata.getMainFunctionEntry();
    assert mainFunctionEntry.equals(functions.get(mainFunctionEntry.getFunctionName()));
  }

  @CanIgnoreReturnValue
  public boolean addNode(CFANode pNode) {

    assert functions.containsKey(pNode.getFunctionName());
    boolean nodeAdded = allNodes.put(pNode.getFunctionName(), pNode);

    return nodeAdded;
  }

  public void clear() {
    functions.clear();
    allNodes.clear();
  }

  @CanIgnoreReturnValue
  public boolean removeNode(CFANode pNode) {
    NavigableSet<CFANode> functionNodes = allNodes.get(pNode.getFunctionName());
    assert functionNodes.contains(pNode);
    boolean nodeRemoved = functionNodes.remove(pNode);
    assert nodeRemoved;

    if (functionNodes.isEmpty()) {
      functions.remove(pNode.getFunctionName());
    }

    return nodeRemoved;
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
}
