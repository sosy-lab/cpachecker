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
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
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

  private final BAMPredicateReducer reducer;
  private final BAMBlockOperator blk;
  private RelevantPredicatesComputer relevantPredicatesComputer;

  @Option(secure=true, description="whether to use auxiliary predidates for reduction")
  private boolean auxiliaryPredicateComputer = true;


  private BAMPredicateCPA(Configuration config, LogManager logger,
      BAMBlockOperator pBlk, CFA pCfa, ShutdownNotifier pShutdownNotifier)
          throws InvalidConfigurationException, CPAException {
    super(config, logger, pBlk, pCfa, pShutdownNotifier);

    config.inject(this, BAMPredicateCPA.class);

    FormulaManagerView fmgr = getSolver().getFormulaManager();
    RelevantPredicatesComputer relevantPredicatesComputer;
    if (auxiliaryPredicateComputer) {
      relevantPredicatesComputer = new AuxiliaryComputer(fmgr);
    } else {
      relevantPredicatesComputer = new RefineableOccurrenceComputer(fmgr);
    }
    this.relevantPredicatesComputer = new CachingRelevantPredicatesComputer(relevantPredicatesComputer);

    reducer = new BAMPredicateReducer(fmgr.getBooleanFormulaManager(), this);
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

  @Override
  public BAMPredicateReducer getReducer() {
    return reducer;
  }

  public void setPartitioning(BlockPartitioning partitioning) {
    blk.setPartitioning(partitioning);
  }
}
