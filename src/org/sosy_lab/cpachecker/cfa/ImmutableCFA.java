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

import static com.google.common.base.Preconditions.*;

import java.util.Map;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;

import java.util.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.SetMultimap;

/**
 * This class represents a CFA after it has been fully created (parsing, linking
 * of functions, etc.).
 */
class ImmutableCFA implements CFA {

  private final MachineModel machineModel;
  private final ImmutableSortedMap<String, FunctionEntryNode> functions;
  private final ImmutableSortedSet<CFANode> allNodes;
  private final FunctionEntryNode mainFunction;
  private final Optional<LoopStructure> loopStructure;
  private final Optional<VariableClassification> varClassification;
  private final Optional<LiveVariables> liveVariables;
  private final Language language;

  ImmutableCFA(
      MachineModel pMachineModel,
      Map<String, FunctionEntryNode> pFunctions,
      SetMultimap<String, CFANode> pAllNodes,
      FunctionEntryNode pMainFunction,
      Optional<LoopStructure> pLoopStructure,
      Optional<VariableClassification> pVarClassification,
      Optional<LiveVariables> pLiveVariables,
      Language pLanguage) {

    machineModel = pMachineModel;
    functions = ImmutableSortedMap.copyOf(pFunctions);
    allNodes = ImmutableSortedSet.copyOf(pAllNodes.values());
    mainFunction = checkNotNull(pMainFunction);
    loopStructure = pLoopStructure;
    varClassification = pVarClassification;
    liveVariables = pLiveVariables;
    language = pLanguage;

    checkArgument(functions.get(mainFunction.getFunctionName()) == mainFunction);
  }

  private ImmutableCFA(MachineModel pMachineModel, Language pLanguage) {
    machineModel = pMachineModel;
    functions = ImmutableSortedMap.of();
    allNodes = ImmutableSortedSet.of();
    mainFunction = null;
    loopStructure = Optional.empty();
    varClassification = Optional.empty();
    liveVariables = Optional.empty();
    language = pLanguage;
  }

  static ImmutableCFA empty(MachineModel pMachineModel, Language pLanguage) {
    return new ImmutableCFA(pMachineModel, pLanguage);
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
  public ImmutableSortedSet<String> getAllFunctionNames() {
    return functions.keySet();
  }

  @Override
  public ImmutableCollection<FunctionEntryNode> getAllFunctionHeads() {
    return functions.values();
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
  public ImmutableSortedSet<CFANode> getAllNodes() {
    return allNodes;
  }

  @Override
  public FunctionEntryNode getMainFunction() {
    return mainFunction;
  }

  @Override
  public Optional<LoopStructure> getLoopStructure() {
    return loopStructure;
  }

  @Override
  public Optional<ImmutableSet<CFANode>> getAllLoopHeads() {
    if (loopStructure.isPresent()) {
      return Optional.of(loopStructure.get().getAllLoopHeads());
    }
    return Optional.empty();
  }

  @Override
  public Optional<VariableClassification> getVarClassification() {
    return varClassification;
  }

  @Override
  public Optional<LiveVariables> getLiveVariables() {
    return liveVariables;
  }

  @Override
  public Language getLanguage() {
    return language;
  }

}
