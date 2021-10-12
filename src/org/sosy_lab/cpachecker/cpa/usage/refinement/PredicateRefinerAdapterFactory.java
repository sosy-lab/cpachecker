// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import com.google.common.base.Function;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.cpa.predicate.BAMBlockFormulaStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefinerFactory;
import org.sosy_lab.cpachecker.cpa.predicate.RecomputeBlockFormulaStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.ThreadModularCPARefiner;
import org.sosy_lab.cpachecker.cpa.usage.refinement.PredicateRefinerAdapter.UsageStatisticsRefinementStrategy;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;

public class PredicateRefinerAdapterFactory {

  private final LogManager logger;
  private final ConfigurableProgramAnalysis cpa;

  PredicateRefinerAdapterFactory(ConfigurableProgramAnalysis pCpa, LogManager pLogger)
      throws InvalidConfigurationException {
    cpa = pCpa;
    logger = pLogger;

    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException("PredicateRefiner could not find the PredicateCPA");
    }
  }

  public PredicateRefinerAdapter createPlainRefiner(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> wrapper)
      throws InvalidConfigurationException {

    return createCommonRefiner(wrapper, false, false);
  }

  public PredicateRefinerAdapter createThreadModularRefiner(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> wrapper)
      throws InvalidConfigurationException {

    return createCommonRefiner(wrapper, false, true);
  }

  public PredicateRefinerAdapter createImpreciseRefiner(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> wrapper)
      throws InvalidConfigurationException {

    return createCommonRefiner(wrapper, true, false);
  }

  private PredicateRefinerAdapter createCommonRefiner(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> wrapper,
      boolean useHavoc,
      boolean useTM)
      throws InvalidConfigurationException {

    @SuppressWarnings("resource")
    BAMPredicateCPA bamPredicateCpa = ((WrapperCPA) cpa).retrieveWrappedCpa(BAMPredicateCPA.class);
    PredicateCPA predicateCpa = ((WrapperCPA) cpa).retrieveWrappedCpa(PredicateCPA.class);

    boolean withBAM = bamPredicateCpa != null;
    predicateCpa = withBAM ? bamPredicateCpa : predicateCpa;
    assert predicateCpa != null;

    BlockFormulaStrategy blockFormulaStrategy;
    Function<ARGState, ARGState> transformer;

    PathFormulaManager pfmgr;

    if (useHavoc) {
      ConfigurationBuilder configBuilder = Configuration.builder();
      configBuilder = configBuilder.copyFrom(predicateCpa.getConfiguration());
      configBuilder.setOption("cpa.predicate.useHavocAbstraction", "true");
      Configuration newConfig = configBuilder.build();
      pfmgr = predicateCpa.createPathFormulaManager(newConfig);
    } else {
      pfmgr = predicateCpa.getPathFormulaManager();
    }

    if (withBAM) {
      transformer = s -> ((BackwardARGState) s).getARGState();
      blockFormulaStrategy = new BAMBlockFormulaStrategy(pfmgr);
    } else {
      transformer = s -> s;
      // Anyway we need to recompute the formulas to avoid problems with the last nonabstraction
      // state
      blockFormulaStrategy = new RecomputeBlockFormulaStrategy(pfmgr);
    }

    UsageStatisticsRefinementStrategy pStrategy =
        new UsageStatisticsRefinementStrategy(
            predicateCpa.getConfiguration(),
            logger,
            predicateCpa.getSolver(),
            predicateCpa.getPredicateManager(),
            transformer);

    ARGBasedRefiner delegate =
        new PredicateCPARefinerFactory(cpa).setBlockFormulaStrategy(blockFormulaStrategy)
            .create(pStrategy);

    if (useTM) {
      delegate =
        new ThreadModularCPARefiner(logger, pStrategy, predicateCpa.getConfiguration(), delegate);
    }

    return new PredicateRefinerAdapter(wrapper, logger, pStrategy, delegate);
  }
}
