// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.overflow;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.ArithmeticOverflowAssumptionBuilder;

/** CPA for detecting overflows in C programs. */
public class OverflowCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithBAM {

  private final CBinaryExpressionBuilder expressionBuilder;
  private final ArithmeticOverflowAssumptionBuilder noOverflowAssumptionBuilder;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(OverflowCPA.class);
  }

  private OverflowCPA(CFA pCfa, LogManager pLogger, Configuration pConfiguration)
      throws InvalidConfigurationException {
    super("sep", "sep", null);
    expressionBuilder = new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger);
    noOverflowAssumptionBuilder =
        new ArithmeticOverflowAssumptionBuilder(pCfa, pLogger, pConfiguration);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new OverflowTransferRelation(noOverflowAssumptionBuilder, expressionBuilder);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new OverflowState(ImmutableSet.of(), false);
  }
}
