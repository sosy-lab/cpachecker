/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.AllRelevantPredicatesComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.AuxiliaryComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.CachingRelevantPredicatesComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.RefineableOccurrenceComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.RelevantPredicatesComputer;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;


/**
 * Implements an BAM-based predicate CPA.
 */
@Options(prefix="cpa.predicate.bam")
public class BAMPredicateCPA extends PredicateCPA implements ConfigurableProgramAnalysisWithBAM {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BAMPredicateCPA.class).withOptions(BAMBlockOperator.class);
  }

  private final BAMBlockOperator blk;
  private RelevantPredicatesComputer relevantPredicatesComputer;

  @Option(
    description =
        "which strategy/heuristic should be used to compute relevant predicates for a block-reduction?"
            + "\nAUXILIARY: dependencies between variables."
            + "\nOCCURENCE: occurence of variables in the block."
            + "\nALL: all variables are relevant.",
    secure = true,
    values = {"AUXILIARY", "OCCURRENCE", "ALL"},
    toUppercase = true
  )
  private String predicateComputer = "AUXILIARY";

  @Option(description = "should we use precision reduction at the BAM block entry",
      secure = true)
  private boolean usePrecisionReduction = true;

  @Option(description = "should we use abstraction reduction at the BAM block entry",
      secure = true)
  private boolean reduceIrrelevantPrecision = true;

  @Option(description = "should we use precision reduction using relevantComputer",
      secure = true)
  private boolean useAbstractionReduction = true;

  @Option(description = "should we fail in case of repeated counterexample." +
                        "There are cases, when repeated counterexamples occurs " +
                        "if precision was cut after another refinement with refinement root" +
                        "higher than the first one. In BAM analysis we can not merge all precision from subgraph" +
                        "and we may lost it",
      secure = true)
  private boolean failAfterRepeatedCounterexample = true;

  private BAMPredicateCPA(
      Configuration config,
      LogManager logger,
      BAMBlockOperator pBlk,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException {
    super(config, logger, pBlk, pCfa, pShutdownNotifier, pSpecification, pAggregatedReachedSets);

    config.inject(this, BAMPredicateCPA.class);

    FormulaManagerView fmgr = getSolver().getFormulaManager();
    switch (predicateComputer) {
      case "AUXILIARY":
        relevantPredicatesComputer =
            new CachingRelevantPredicatesComputer(new AuxiliaryComputer(fmgr));
        break;
      case "OCCURRENCE":
        relevantPredicatesComputer =
            new CachingRelevantPredicatesComputer(new RefineableOccurrenceComputer(fmgr));
        break;
      case "ALL":
        relevantPredicatesComputer = AllRelevantPredicatesComputer.INSTANCE;
        break;
      default:
        throw new AssertionError("unhandled case");
    }

    blk = pBlk;
  }

  RelevantPredicatesComputer getRelevantPredicatesComputer() {
    return relevantPredicatesComputer;
  }

  void setRelevantPredicatesComputer(RelevantPredicatesComputer pRelevantPredicatesComputer) {
    relevantPredicatesComputer = pRelevantPredicatesComputer;
  }

  BlockPartitioning getPartitioning() {
    return blk.getPartitioning();
  }

  boolean usePrecisionReduction() {
    return usePrecisionReduction;
  }

  boolean useAbstractionReduction() {
    return useAbstractionReduction;
  }

  public boolean reduceIrrelevantPrecision() {
    return reduceIrrelevantPrecision;
  }

  public boolean failAfterRepeatedCounterexample() {
    return failAfterRepeatedCounterexample;
  }

  @Override
  public BAMPredicateReducer getReducer() {
    return new BAMPredicateReducer(
        getSolver().getFormulaManager().getBooleanFormulaManager(), this);
  }

  @Override
  public void setPartitioning(BlockPartitioning partitioning) {
    blk.setPartitioning(partitioning);
  }
}
