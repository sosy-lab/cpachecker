// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public class CFAWithACSLAnnotations implements CFA {

  private CFA delegate;

  private Multimap<CFAEdge, ACSLAnnotation> edgesToAnnotations = LinkedHashMultimap.create();

  public CFAWithACSLAnnotations(CFA pCFA) {
    delegate = pCFA;
  }

  public Multimap<CFAEdge, ACSLAnnotation> getEdgesToAnnotations() {
    return edgesToAnnotations;
  }

  @Override
  public MachineModel getMachineModel() {
    return delegate.getMachineModel();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public int getNumberOfFunctions() {
    return delegate.getNumberOfFunctions();
  }

  @Override
  public NavigableSet<String> getAllFunctionNames() {
    return delegate.getAllFunctionNames();
  }

  @Override
  public Collection<FunctionEntryNode> getAllFunctionHeads() {
    return delegate.getAllFunctionHeads();
  }

  @Override
  public FunctionEntryNode getFunctionHead(String name) {
    return delegate.getFunctionHead(name);
  }

  @Override
  public NavigableMap<String, FunctionEntryNode> getAllFunctions() {
    return delegate.getAllFunctions();
  }

  @Override
  public Collection<CFANode> getAllNodes() {
    return delegate.getAllNodes();
  }

  @Override
  public FunctionEntryNode getMainFunction() {
    return delegate.getMainFunction();
  }

  @Override
  public Optional<LoopStructure> getLoopStructure() {
    return delegate.getLoopStructure();
  }

  @Override
  public Optional<ImmutableSet<CFANode>> getAllLoopHeads() {
    return delegate.getAllLoopHeads();
  }

  @Override
  public Optional<VariableClassification> getVarClassification() {
    return delegate.getVarClassification();
  }

  @Override
  public Optional<LiveVariables> getLiveVariables() {
    return delegate.getLiveVariables();
  }

  @Override
  public Language getLanguage() {
    return delegate.getLanguage();
  }

  @Override
  public List<Path> getFileNames() {
    return delegate.getFileNames();
  }
}
