// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFAWithACSLAnnotations;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLBuiltinCollectingVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLPredicateToExpressionTreeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLTermToCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

/** This CPA is for deriving invariants from ACSL annotations. */
@Options(prefix = "cpa.acsl")
public class ACSLCPA extends AbstractCPA implements ConfigurableProgramAnalysis {

  @Option(
      secure = true,
      description = "only store pure C expressions without ACSL-specific constructs")
  private boolean usePureExpressionsOnly = true;

  private final CFAWithACSLAnnotations cfa;
  private final LogManager logger;
  private final ACSLPredicateToExpressionTreeVisitor acslVisitor;
  private final ToCExpressionVisitor expressionTreeVisitor;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ACSLCPA.class);
  }

  private ACSLCPA(CFA pCFA, LogManager pLogManager, Configuration pConfig)
      throws InvalidConfigurationException {
    super("sep", "sep", null);
    logger = pLogManager;
    if (pCFA instanceof CFAWithACSLAnnotations) {
      cfa = (CFAWithACSLAnnotations) pCFA;
    } else {
      cfa = new CFAWithACSLAnnotations(pCFA);
      logger.log(Level.WARNING, "No ACSL annotations in CFA, ACSLCPA is useless.");
    }
    ACSLTermToCExpressionVisitor termVisitor = new ACSLTermToCExpressionVisitor(cfa, logger);
    acslVisitor = new ACSLPredicateToExpressionTreeVisitor(termVisitor);
    expressionTreeVisitor = new ToCExpressionVisitor(cfa.getMachineModel(), logger);
    pConfig.inject(this);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new ACSLTransferRelation(
        cfa, logger, acslVisitor, expressionTreeVisitor, usePureExpressionsOnly);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    ImmutableSet.Builder<ACSLAnnotation> annotations = ImmutableSet.builder();
    for (CFAEdge edge : CFAUtils.enteringEdges(node)) {
      Collection<ACSLAnnotation> annotationsForEdge = cfa.getEdgesToAnnotations().get(edge);
      if (usePureExpressionsOnly) {
        ACSLBuiltinCollectingVisitor visitor = new ACSLBuiltinCollectingVisitor();
        annotationsForEdge =
            FluentIterable.from(annotationsForEdge)
                .filter(x -> x.getPredicateRepresentation().accept(visitor).isEmpty())
                .toSet();
      }
      annotations.addAll(annotationsForEdge);
    }
    return new ACSLState(annotations.build(), acslVisitor, expressionTreeVisitor, logger);
  }
}
