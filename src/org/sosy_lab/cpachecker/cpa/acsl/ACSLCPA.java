// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimaps;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

/** This CPA is for deriving invariants from ACSL annotations. */
@Options(prefix = "cpa.acsl")
public class ACSLCPA extends AbstractCPA implements ConfigurableProgramAnalysis {

  @Option(
      secure = true,
      description = "only store pure C expressions without ACSL-specific constructs")
  private boolean usePureExpressionsOnly = true;

  private final LogManager logger;
  private final AcslToExpressionTreeConverter converter;
  private final ToCExpressionVisitor expressionTreeVisitor;
  private final ImmutableSetMultimap<CFANode, AcslPredicate> invariants;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ACSLCPA.class);
  }

  private ACSLCPA(CFA pCFA, LogManager pLogManager, Configuration pConfig)
      throws InvalidConfigurationException {
    super("sep", "sep", null);
    pConfig.inject(this);
    logger = pLogManager;
    converter = new AcslToExpressionTreeConverter(pCFA.getMachineModel(), logger);
    expressionTreeVisitor = new ToCExpressionVisitor(pCFA.getMachineModel(), logger);
    if (pCFA.getAcslMetadata() == null) {
      logger.log(Level.WARNING, "No ACSL annotations in CFA, ACSLCPA is useless.");
    }
    ImmutableSetMultimap<CFANode, AcslPredicate> allInvariants = AcslInvariants.of(pCFA);
    invariants =
        usePureExpressionsOnly
            ? ImmutableSetMultimap.copyOf(
                Multimaps.filterValues(allInvariants, converter::canConvert))
            : allInvariants;
  }

  /** Returns the state for the predicates that hold at the given node. */
  ACSLState getStateForNode(CFANode pNode) {
    return new ACSLState(invariants.get(pNode), converter, expressionTreeVisitor, logger);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new ACSLTransferRelation(this);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return getStateForNode(node);
  }
}
