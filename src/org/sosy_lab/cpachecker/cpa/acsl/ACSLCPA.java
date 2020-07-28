// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFAWithACSLAnnotationLocations;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLTermToCExpressionVisitor;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * This CPA is for deriving invariants from ACSL annotations.
 */
public class ACSLCPA extends AbstractCPA implements ConfigurableProgramAnalysis {

  private final CFAWithACSLAnnotationLocations cfa;
  private final ACSLTermToCExpressionVisitor visitor;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ACSLCPA.class);
  }

  private ACSLCPA(CFA pCFA, LogManager pLogManager) {
    super("sep", "sep", null);
    if (pCFA instanceof CFAWithACSLAnnotationLocations) {
      cfa = (CFAWithACSLAnnotationLocations) pCFA;
    } else {
      throw new AssertionError("No annotations in CFA");
    }
    visitor = new ACSLTermToCExpressionVisitor(cfa, pLogManager);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new ACSLTransferRelation(cfa, visitor);
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    Set<ACSLAnnotation> annotations = new HashSet<>();
    for (int i = 0; i < node.getNumEnteringEdges(); i++) {
      CFAEdge edge = node.getEnteringEdge(i);
      annotations.addAll(cfa.getEdgesToAnnotations().get(edge));
    }
    return new ACSLState(annotations, visitor);
  }
}
