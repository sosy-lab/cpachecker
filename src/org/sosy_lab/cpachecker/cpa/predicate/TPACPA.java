// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier.TrivialInvariantSupplier;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

@Options(prefix = "cpa.predicate.transitionpredicateabstraction")
public class TPACPA extends PredicateCPA
    implements ConfigurableProgramAnalysis, StatisticsProvider, ProofChecker, AutoCloseable {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TPACPA.class).withOptions(BlockOperator.class);
  }

  private final PredicateCPAInvariantsManager invariantsManager;

  protected TPACPA(
      Configuration config,
      LogManager logger,
      BlockOperator pBlk,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier,
      Specification specification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(config, logger, pBlk, pCfa, pShutdownNotifier, specification, pAggregatedReachedSets);
    FormulaManagerView.usingTPA();

    logger.log(Level.INFO, "TPA-CPA is initialized");
    config.inject(this, TPACPA.class);
    invariantsManager = super.getInvariantsManager();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new PredicateAbstractDomain(getPredicateManager(), isSymbolicCoverageCheck(), getStatistics());
  }

  @Override
  public PredicateTransferRelation getTransferRelation() {
    return new PredicateTransferRelation(
        logger,
        getDirection(),
        getFormulaManager(),
        getPathFormulaManager(),
        getBlk(),
        getPredicateManager(),
        getStatistics(),
        getOptions());
  }

  @Override
  public MergeOperator getMergeOperator() {
    return switch (getMergeType()) {
      case "SEP" -> MergeSepOperator.getInstance();
      case "ABE" ->
          new PredicateMergeOperator(
              logger,
              getPathFormulaManager(),
              getStatistics(),
              isMergeAbstractionStates(),
              getPredicateManager());
      default -> throw new AssertionError("Update list of allowed merge operators");
    };
  }

  @Override
  public StopOperator getStopOperator() {
    return switch (getStopType()) {
      case "SEP" -> new PredicateStopOperator(getAbstractDomain());
      case "SEPPCC" ->
          new PredicatePCCStopOperator(getPathFormulaManager(), getPredicateManager(), getSolver());
      case "SEPNAA" -> new PredicateNeverAtAbstractionStopOperator(getAbstractDomain());
      default -> throw new AssertionError("Update list of allowed stop operators");
    };
  }

  @Override
  public PredicateAbstractionManager getPredicateManager() {

    return new PredicateAbstractionManager(
        getAbstractionManager(),
        getPathFormulaManager(),
        getSolver(),
        getAbstractionOptions(),
        getWeakeningOptions(),
        getAbstractionStorage(),
        logger,
        shutdownNotifier,
        getAbstractionStats(),
        getInvariantsManager().appendToAbstractionFormula()
        ? invariantsManager
        : TrivialInvariantSupplier.INSTANCE);
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new TPAPrecisionAdjustment(
        logger,
        getFormulaManager(),
        getPathFormulaManager(),
        getBlk(),
        getPredicateManager(),
        invariantsManager,
        getPredicateProvider(),
        getStatistics());
  }
}