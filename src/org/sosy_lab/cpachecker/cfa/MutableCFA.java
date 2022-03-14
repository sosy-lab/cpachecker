// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public class MutableCFA implements CFA {

  private final MachineModel machineModel;
  private final NavigableMap<String, FunctionEntryNode> functions;
  private final TreeMultimap<String, CFANode> allNodes;
  private final FunctionEntryNode mainFunction;
  private final List<Path> fileNames;
  private final Language language;
  private Optional<LoopStructure> loopStructure = Optional.empty();
  private Optional<LiveVariables> liveVariables = Optional.empty();

  public MutableCFA(
      MachineModel pMachineModel,
      NavigableMap<String, FunctionEntryNode> pFunctions,
      TreeMultimap<String, CFANode> pAllNodes,
      FunctionEntryNode pMainFunction,
      List<Path> pFileNames,
      Language pLanguage) {

    machineModel = pMachineModel;
    functions = pFunctions;
    allNodes = pAllNodes;
    mainFunction = Preconditions.checkNotNull(pMainFunction);
    fileNames = ImmutableList.copyOf(pFileNames);
    language = pLanguage;

    assert functions.keySet().equals(allNodes.keySet());
    assert mainFunction.equals(functions.get(mainFunction.getFunctionName()));
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
    NavigableSet<CFANode> functionNodes = allNodes.get(pNode.getFunctionName());
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
      return Optional.of(loopStructure.orElseThrow().getAllLoopHeads());
    }
    return Optional.empty();
  }

  public ImmutableCFA makeImmutableCFA(Optional<VariableClassification> pVarClassification) {
    return new ImmutableCFA(
        machineModel,
        functions,
        allNodes,
        mainFunction,
        loopStructure,
        pVarClassification,
        liveVariables,
        fileNames,
        language);
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

  @Override
  public List<Path> getFileNames() {
    return fileNames;
  }
}
