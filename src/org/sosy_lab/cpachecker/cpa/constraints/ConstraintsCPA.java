// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints;

import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
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
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsMergeOperator;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.domain.SubsetLessOrEqualOperator;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.ConstraintsPrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.FullConstraintsPrecision;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/** Configurable Program Analysis that tracks constraints for analysis. */
@Options(prefix = "cpa.constraints")
public class ConstraintsCPA
    implements ConfigurableProgramAnalysis, StatisticsProvider, AutoCloseable {

  public enum ComparisonType {
    SUBSET,
  }

  public enum MergeType {
    SEP,
    JOIN_FITTING_CONSTRAINT
  }

  @Option(description = "Type of less-or-equal operator to use", toUppercase = true)
  private ComparisonType lessOrEqualType = ComparisonType.SUBSET;

  @Option(description = "Type of merge operator to use", toUppercase = true)
  private MergeType mergeType = MergeType.SEP;

  private final LogManager logger;

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private ConstraintsTransferRelation transferRelation;
  private ConstraintsPrecisionAdjustment precisionAdjustment;
  private ConstraintsPrecision precision;

  private final ConstraintsSolver constraintsSolver;
  private final Solver solver;

  private final ConstraintsStatistics stats = new ConstraintsStatistics();

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ConstraintsCPA.class);
  }

  private ConstraintsCPA(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = pLogger;
    solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    FormulaManagerView formulaManager = solver.getFormulaManager();
    CtoFormulaConverter converter =
        initializeCToFormulaConverter(
            formulaManager, pLogger, pConfig, pShutdownNotifier, pCfa.getMachineModel());
    constraintsSolver = new ConstraintsSolver(pConfig, solver, formulaManager, converter, stats);

    abstractDomain = initializeAbstractDomain();
    mergeOperator = initializeMergeOperator();
    stopOperator = initializeStopOperator();

    transferRelation =
        new ConstraintsTransferRelation(
            constraintsSolver, stats, pCfa.getMachineModel(), logger, pConfig);
    precisionAdjustment = new ConstraintsPrecisionAdjustment(stats);
    precision = FullConstraintsPrecision.getInstance();
  }

  // Can only be called after machineModel and formulaManager are set
  private CtoFormulaConverter initializeCToFormulaConverter(
      FormulaManagerView pFormulaManager,
      LogManager pLogger,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel)
      throws InvalidConfigurationException {

    FormulaEncodingWithPointerAliasingOptions options =
        new FormulaEncodingWithPointerAliasingOptions(pConfig);
    TypeHandlerWithPointerAliasing typeHandler =
        new TypeHandlerWithPointerAliasing(logger, pMachineModel, options);

    return new CToFormulaConverterWithPointerAliasing(
        options,
        pFormulaManager,
        pMachineModel,
        Optional.empty(),
        pLogger,
        pShutdownNotifier,
        typeHandler,
        AnalysisDirection.FORWARD);
  }

  private MergeOperator initializeMergeOperator() {
    switch (mergeType) {
      case SEP:
        return MergeSepOperator.getInstance();
      case JOIN_FITTING_CONSTRAINT:
        return new ConstraintsMergeOperator(stats);
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

      default:
        throw new AssertionError("Unhandled type for less-or-equal operator: " + lessOrEqualType);
    }

    return abstractDomain;
  }

  public ConstraintsSolver getSolver() {
    return constraintsSolver;
  }

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
    statsCollection.add(stats);
  }

  @Override
  public void close() {
    solver.close();
  }
}
