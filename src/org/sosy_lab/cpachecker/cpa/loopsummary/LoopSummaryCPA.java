// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@Options(prefix = "cpa.loopsummary")
public class LoopSummaryCPA extends AbstractLoopSummaryCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LoopSummaryCPA.class);
  }

  private final LoopSummaryTransferRelation transfer;

  @Option(
      name = "lookaheadamntnodes",
      secure = true,
      description =
          "Lookahead a certain amount of nodes in order to see if one can summarize some nodes inside to summarize the current node"
              + "This must be done in order to summarize loops inside loops")
  private int lookaheadamntnodes = 10;

  @Option(
      name = "lookaheaditerations",
      secure = true,
      description =
          "The amount of iterations one wants to summarize the ahead lookep CFA nodes in order to summarize loops inside loops")
  private int lookaheaditerations = 10;

  private LoopSummaryCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pCpa, config, pLogger, pShutdownNotifier, pSpecification, pCfa);
    config.inject(this);

    AlgorithmFactory factory = new CPAAlgorithmFactory(this, logger, config, pShutdownNotifier);

    transfer =
        new LoopSummaryTransferRelation(
            this,
            pShutdownNotifier,
            factory,
            super.getStrategies(),
            lookaheadamntnodes,
            lookaheaditerations,
            pCfa);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new LoopSummaryPrecision(getWrappedCpa().getInitialPrecision(pNode, pPartition));
  }

  @Override
  public LoopSummaryPrecisionAdjustment getPrecisionAdjustment() {
    if (getWrappedCpa() instanceof ARGCPA) {
      return new LoopSummaryPrecisionAdjustment(
          ((ARGCPA) getWrappedCpa()).getWrappedCpa().getPrecisionAdjustment(),
          ((ARGCPA) getWrappedCpa()).getInCPAEnabledAnalysis(),
          ((ARGCPA) getWrappedCpa()).getARGExporter());
    } else {
      logger.log(Level.SEVERE, "Unknown inner CPA: " + getWrappedCpa().getClass());
      logger.log(Level.SEVERE, "The inner CPA must be an ARGCPA, else Loopsummary does not work.");
      return null;
    }
  }

  @Override
  public MergeOperator getMergeOperator() {
    if (getWrappedCpa() instanceof ARGCPA) {
      if (!((ARGCPA) getWrappedCpa()).getInCPAEnabledAnalysis()) {
        return new LoopSummaryMergeJoin(
            ((ARGCPA) getWrappedCpa()).getWrappedCpa().getMergeOperator(),
            ((ARGCPA) getWrappedCpa()).getWrappedCpa().getAbstractDomain(),
            ((ARGCPA) getWrappedCpa()).getMergeOnWrappedSubsumption());
      } else {
        logger.log(Level.SEVERE, "Unknown merge operator for option: getInCPAEnabledAnalysis");
        return null;
      }
    } else {
      logger.log(Level.SEVERE, "Unknown inner CPA: " + getWrappedCpa().getClass());
      logger.log(Level.SEVERE, "The inner CPA must be an ARGCPA, else Loopsummary does not work.");
      return null;
    }
  }

  @Override
  public ForcedCoveringStopOperator getStopOperator() {
    if (getWrappedCpa() instanceof ARGCPA) {
      return new LoopSummaryStopSep(
          ((ARGCPA) getWrappedCpa()).getWrappedCpa().getStopOperator(),
          logger,
          ((ARGCPA) getWrappedCpa()).getInCPAEnabledAnalysis(),
          ((ARGCPA) getWrappedCpa()).getKeepCoveredStatesInReached(),
          ((ARGCPA) getWrappedCpa()).getCoverTargetStates());
    } else {
      logger.log(Level.SEVERE, "Unknown inner CPA: " + getWrappedCpa().getClass());
      logger.log(Level.SEVERE, "The inner CPA must be an ARGCPA, else Loopsummary does not work.");
      return null;
    }
  }

  @Override
  public LoopSummaryTransferRelation getTransferRelation() {
    return transfer;
  }

  public Configuration getConfiguration() {
    // TODO Auto-generated method stub
    return null;
  }

  public Solver getSolver() {
    // TODO Auto-generated method stub
    return null;
  }

  public PathFormulaManager getPathFormulaManager() {
    // TODO Auto-generated method stub
    return null;
  }

  public PredicateAbstractionManager getPredicateManager() {
    // TODO Auto-generated method stub
    return null;
  }
}
