// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.timedautomata;

import com.google.common.base.Preconditions;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopEqualsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TAFormulaEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TAFormulaEncodingProvider;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@Options(prefix = "cpa.timedautomata")
public class TAUnrollingCPA
    implements ConfigurableProgramAnalysis,
        AutoCloseable,
        AdjustableConditionCPA,
        ReachedSetAdjustingCPA {
  @Option(
      secure = true,
      description =
          "Initial maximum unrolling steps that will be performed. A value less than zero means infinity")
  private int initialMaximumStepCount = 50;

  @Option(
      secure = true,
      description = "Amount by which to increase the maximum unrolling step bound.")
  private int stepCountAdujstmentStep = 0;

  @Option(secure = true, description = "Maximum value that the unrolling step bound will reach.")
  private int stepCountUpperBound = 50;

  private int maximumStepCount;

  private final TAFormulaEncoding encoding;
  private final TAUnrollingTransferRelation transferRelation;
  private final AbstractDomain abstractDomain = new FlatLatticeDomain();

  private final Solver solver;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TAUnrollingCPA.class);
  }

  public TAUnrollingCPA(
      Configuration config, LogManager logger, CFA pCfa, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    config.inject(this, TAUnrollingCPA.class);
    Preconditions.checkState(
        stepCountAdujstmentStep >= 0,
        "Step count adjustment step must be greater or equal to zero");
    Preconditions.checkState(
        stepCountUpperBound >= initialMaximumStepCount,
        "Step count upper bound must be greater or equal to initial maximum step count");
    maximumStepCount = initialMaximumStepCount;

    solver = Solver.create(config, logger, pShutdownNotifier);
    var formulaManager = solver.getFormulaManager();

    encoding = new TAFormulaEncodingProvider(config).createConfiguredEncoding(pCfa, formulaManager);
    transferRelation = new TAUnrollingTransferRelation(encoding, initialMaximumStepCount);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    var initialFormula = encoding.getInitialFormula(pNode);
    return new TAUnrollingState(initialFormula, 0, maximumStepCount <= 0);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopEqualsOperator();
  }

  @Override
  public void close() {
    solver.close();
  }

  @Override
  public boolean adjustPrecision() {
    if (stepCountAdujstmentStep == 0) {
      return false;
    }
    if (maximumStepCount + stepCountAdujstmentStep <= stepCountUpperBound) {
      maximumStepCount += stepCountAdujstmentStep;
      transferRelation.setStepCountBound(maximumStepCount);
      return true;
    }
    return false;
  }

  @Override
  public void adjustReachedSet(ReachedSet pReachedSet) {
    Set<AbstractState> blockedByBound = new LinkedHashSet<>();
    for (AbstractState s : pReachedSet) {
      var unrollingState = AbstractStates.extractStateByType(s, TAUnrollingState.class);
      if (unrollingState != null && unrollingState.didReachBound()) {
        blockedByBound.add(s);
        if (unrollingState.getStepCount() < maximumStepCount) {
          unrollingState.setDidReachedBoundFalse();
        }
      }
    }

    blockedByBound.forEach(pReachedSet::reAddToWaitlist);
  }
}
