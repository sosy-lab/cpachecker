// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.conditions.global;

import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.IdentityTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonAbstractState;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopAlwaysOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class GlobalConditionsCPA
    implements ConfigurableProgramAnalysisWithBAM, AdjustableConditionCPA, ProofChecker {

  private final PrecisionAdjustment precisionAdjustment;
  private final GlobalConditionsThresholds thresholds;

  private final AbstractDomain domain;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(GlobalConditionsCPA.class);
  }

  private GlobalConditionsCPA(Configuration config, LogManager logger)
      throws InvalidConfigurationException {
    thresholds = new GlobalConditionsThresholds(config, logger);

    if (thresholds.isLimitEnabled()) {
      logger.log(Level.INFO, "Analyzing with the following", thresholds);
      GlobalConditionsSimplePrecisionAdjustment prec =
          new GlobalConditionsSimplePrecisionAdjustment(logger, thresholds);

      if (thresholds.getReachedSetSizeThreshold() >= 0) {
        precisionAdjustment = new GlobalConditionsPrecisionAdjustment(logger, thresholds, prec);
      } else {
        precisionAdjustment = prec;
      }

    } else {
      logger.log(
          Level.WARNING,
          "GlobalConditionsCPA used without any limits, you can remove it from the list of CPAs.");
      precisionAdjustment = StaticPrecisionAdjustment.getInstance();
    }

    domain = new FlatLatticeDomain(SingletonAbstractState.INSTANCE);
  }

  @Override
  public boolean adjustPrecision() {
    return thresholds.adjustPrecision();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonAbstractState.INSTANCE;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return StopAlwaysOperator.getInstance();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return IdentityTransferRelation.INSTANCE;
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    return pSuccessors.size() == 1 && pSuccessors.contains(pElement);
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement)
      throws CPAException {
    return pElement == pOtherElement;
  }
}
