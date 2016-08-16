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
package org.sosy_lab.cpachecker.cfa;

import java.util.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SortedSetMultimap;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

public class MutableCFA implements CFA {

  private final MachineModel machineModel;
  private final SortedMap<String, FunctionEntryNode> functions;
  private final SortedSetMultimap<String, CFANode> allNodes;
  private final FunctionEntryNode mainFunction;
  private final Language language;
  private Optional<LoopStructure> loopStructure = Optional.empty();
  private Optional<LiveVariables> liveVariables = Optional.empty();

  public MutableCFA(
      MachineModel pMachineModel,
      SortedMap<String, FunctionEntryNode> pFunctions,
      SortedSetMultimap<String, CFANode> pAllNodes,
      FunctionEntryNode pMainFunction,
      Language pLanguage) {

    machineModel = pMachineModel;
    functions = pFunctions;
    allNodes = pAllNodes;
    mainFunction = pMainFunction;
    language = pLanguage;

    assert functions.keySet().equals(allNodes.keySet());
    assert functions.get(mainFunction.getFunctionName()) == mainFunction;
  }

  public void addNode(CFANode pNode) {
    assert functions.containsKey(pNode.getFunctionName());
    allNodes.put(pNode.getFunctionName(), pNode);
  }

  public void clear() {
    functions.clear();
    allNodes.clear();
  }

  public void removeNode(CFANode pNode) {
    SortedSet<CFANode> functionNodes = allNodes.get(pNode.getFunctionName());
    assert functionNodes.contains(pNode);
    functionNodes.remove(pNode);

    if (functionNodes.isEmpty()) {
      functions.remove(pNode.getFunctionName());
    }
  }

  @Override
  public MachineModel getMachineModel() {
    return machineModel;
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
  public Set<String> getAllFunctionNames() {
    return Collections.unmodifiableSet(functions.keySet());
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
  public Map<String, FunctionEntryNode> getAllFunctions() {
    return Collections.unmodifiableMap(functions);
  }

  public SortedSet<CFANode> getFunctionNodes(String pName) {
    return Collections.unmodifiableSortedSet(allNodes.get(pName));
  }

  @Override
  public Collection<CFANode> getAllNodes() {
    return Collections.unmodifiableCollection(allNodes.values());
  }

  @Override
  public FunctionEntryNode getMainFunction() {
    return mainFunction;
  }

  @Override
  public Optional<LoopStructure> getLoopStructure() {
    return loopStructure;
  }

  public void setLoopStructure(LoopStructure pLoopStructure) {
    loopStructure = Optional.of(pLoopStructure);
  }

  @Override
  public Optional<ImmutableSet<CFANode>> getAllLoopHeads() {
    if (loopStructure.isPresent()) {
      return Optional.of(loopStructure.get().getAllLoopHeads());
    }
    return Optional.empty();
  }

  public ImmutableCFA makeImmutableCFA(Optional<VariableClassification> pVarClassification) {
    return new ImmutableCFA(machineModel, functions, allNodes, mainFunction,
        loopStructure, pVarClassification, liveVariables, language);
  }

  @Override
  public Optional<VariableClassification> getVarClassification() {
    return Optional.empty();
  }

  @Override
  public Optional<LiveVariables> getLiveVariables() {
    return liveVariables;
  }

  public void setLiveVariables(LiveVariables pLiveVariables) {
    liveVariables = Optional.of(pLiveVariables);
  }

  @Override
  public Language getLanguage() {
      return language;
  }

}