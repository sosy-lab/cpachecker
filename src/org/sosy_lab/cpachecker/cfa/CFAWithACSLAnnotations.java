// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.NavigableMap;
import java.util.NavigableSet;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.ForwardingCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

public class CFAWithACSLAnnotations extends ForwardingCfaNetwork implements CFA {

  private CFA delegate;

  private Multimap<CFAEdge, ACSLAnnotation> edgesToAnnotations = LinkedHashMultimap.create();

  public CFAWithACSLAnnotations(CFA pCFA) {
    delegate = pCFA;
  }

  @Override
  public CFA immutableCopy() {
    throw new UnsupportedOperationException(
        "Cannot create immutable copy of `CFAWithACSLAnnotations`!");
  }

  @Override
  protected CfaNetwork delegate() {
    return delegate;
  }

  public Multimap<CFAEdge, ACSLAnnotation> getEdgesToAnnotations() {
    return edgesToAnnotations;
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
  public FunctionEntryNode getFunctionHead(String name) {
    return delegate.getFunctionHead(name);
  }

  @Override
  public NavigableMap<String, FunctionEntryNode> getAllFunctions() {
    return delegate.getAllFunctions();
  }

  @Override
  public CfaMetadata getMetadata() {
    return delegate.getMetadata();
  }
}
