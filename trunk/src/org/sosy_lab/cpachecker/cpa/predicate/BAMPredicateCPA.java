// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** Implements an BAM-based predicate CPA. */
@Options(prefix = "cpa.predicate.bam")
public class BAMPredicateCPA extends PredicateCPA implements ConfigurableProgramAnalysisWithBAM {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BAMPredicateCPA.class).withOptions(BAMBlockOperator.class);
  }

  private final BAMBlockOperator blk;

  private BAMPredicateCPA(
      Configuration config,
      LogManager logger,
      BAMBlockOperator pBlk,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(config, logger, pBlk, pCfa, pShutdownNotifier, pSpecification, pAggregatedReachedSets);
    config.inject(this, BAMPredicateCPA.class);
    blk = pBlk; // keep reference to later inject the BlockPartitioning
  }

  @Override
  public BAMPredicateReducer getReducer() throws InvalidConfigurationException {
    return new BAMPredicateReducer(this, config);
  }

  @Override
  public void setPartitioning(BlockPartitioning partitioning) {
    blk.setPartitioning(partitioning);
  }

  public void clearAllCaches() {
    getPredicateManager().clear();
    getPathFormulaManager().clearCaches();
  }
}
