/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.constraints;

import java.util.Collection;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.constraints.domain.*;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.ConstraintsPrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.FullConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/**
 * Configurable Program Analysis that tracks constraints for analysis.
 */
@Options(prefix = "cpa.constraints")
public class ConstraintsCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  public enum ComparisonType { SUBSET, ALIASED_SUBSET, IMPLICATION }

  public enum MergeType { SEP, JOIN_FITTING_CONSTRAINT }

  @Option(description = "Type of less-or-equal operator to use", toUppercase = true)
  private ComparisonType lessOrEqualType = ComparisonType.SUBSET;

  @Option(description = "Type of merge operator to use", toUppercase =  true)
  private MergeType mergeType = MergeType.SEP;

  private final LogManager logger;

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private ConstraintsPrecisionAdjustment precisionAdjustment;
  private ConstraintsPrecision precision;

  private Solver solver;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ConstraintsCPA.class);
  }

  private ConstraintsCPA(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      CFA pCfa) throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = pLogger;
    solver = Solver.create(pConfig, pLogger, pShutdownNotifier);

    SymbolicValues.initialize(lessOrEqualType);
    abstractDomain = initializeAbstractDomain();
    mergeOperator = initializeMergeOperator();
    stopOperator = initializeStopOperator();
    transferRelation =
        new ConstraintsTransferRelation(solver, pCfa.getMachineModel(), logger, pConfig, pShutdownNotifier);
    precisionAdjustment = new ConstraintsPrecisionAdjustment();
    precision = FullConstraintsPrecision.getInstance();
  }

  private MergeOperator initializeMergeOperator() {
    switch (mergeType) {
      case SEP:
        return MergeSepOperator.getInstance();
      case JOIN_FITTING_CONSTRAINT:
        return new ConstraintsMergeOperator();
      default:
        throw new AssertionError("Unhandled merge type " + mergeType);
    }
  }

  private StopOperator initializeStopOperator() {
    return new StopSepOperator(abstractDomain);
  }

  private AbstractDomain initializeAbstractDomain() {
    switch (lessOrEqualType) {
      case SUBSET:
        abstractDomain = SubsetLessOrEqualOperator.getInstance();
        break;

      case ALIASED_SUBSET:
        abstractDomain = AliasedSubsetLessOrEqualOperator.getInstance();
        break;

      case IMPLICATION:
        abstractDomain = new ImplicationLessOrEqualOperator(solver);
        break;

      default:
        throw new AssertionError("Unhandled type for less-or-equal operator: " + lessOrEqualType);
    }

    return abstractDomain;
  }

  public Solver getSolver() { return solver; }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return new ConstraintsState();
  }

  @Override
  public Precision getInitialPrecision(CFANode node, StateSpacePartition partition) {
    return precision;
  }

  public void injectRefinablePrecision(final ConstraintsPrecision pNewPrecision) {
    precision = pNewPrecision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    precisionAdjustment.collectStatistics(statsCollection);

    if (mergeOperator instanceof Statistics) {
      statsCollection.add((Statistics) mergeOperator);
    }
  }
}
