/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.acsl;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFAWithACSLAnnotationLocations;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLToCExpressionVisitor;
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

  private CFAWithACSLAnnotationLocations cfa;
  private LogManager logger;
  private ACSLToCExpressionVisitor visitor;

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
    logger = pLogManager;
    visitor = new ACSLToCExpressionVisitor(cfa, logger);
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
