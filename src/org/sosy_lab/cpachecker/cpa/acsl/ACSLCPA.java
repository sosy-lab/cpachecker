// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

/**
 * This CPA is for deriving invariants from ACSL annotations.
 */
@Options(prefix = "cpa.acsl")
public class ACSLCPA extends AbstractCPA implements ConfigurableProgramAnalysis {

  @Option(
      secure = true,
      description = "only store pure C expressions without ACSL-specific constructs")
  private boolean usePureExpressionsOnly = true;

  private final CFAWithACSLAnnotationLocations cfa;
  private final ACSLTermToCExpressionVisitor acslVisitor;
  private final ToCExpressionVisitor expressionTreeVisitor;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ACSLCPA.class);
  }

  private ACSLCPA(CFA pCFA, LogManager pLogManager, Configuration pConfig)
      throws InvalidConfigurationException {
    super("sep", "sep", null);
    if (pCFA instanceof CFAWithACSLAnnotationLocations) {
      cfa = (CFAWithACSLAnnotationLocations) pCFA;
    } else {
      cfa = new CFAWithACSLAnnotationLocations(pCFA, ImmutableMap.of());
      pLogManager.log(Level.WARNING, "No ACSL annotations in CFA, ACSLCPA is useless.");
    }
    acslVisitor = new ACSLTermToCExpressionVisitor(cfa, pLogManager);
    expressionTreeVisitor = new ToCExpressionVisitor(cfa.getMachineModel(), pLogManager);
    pConfig.inject(this);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new ACSLTransferRelation(
        cfa, acslVisitor, expressionTreeVisitor, usePureExpressionsOnly);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    ImmutableSet.Builder<ACSLAnnotation> annotations = ImmutableSet.builder();
    for (int i = 0; i < node.getNumEnteringEdges(); i++) {
      CFAEdge edge = node.getEnteringEdge(i);
      Collection<ACSLAnnotation> annotationsForEdge = cfa.getEdgesToAnnotations().get(edge);
      if (usePureExpressionsOnly) {
        annotationsForEdge =
            FluentIterable.from(annotationsForEdge)
                .filter(x -> x.getPredicateRepresentation().getUsedBuiltins().isEmpty())
                .toSet();
      }
      annotations.addAll(annotationsForEdge);
    }
    return new ACSLState(annotations.build(), acslVisitor, expressionTreeVisitor);
  }
}
