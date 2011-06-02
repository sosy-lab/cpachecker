/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.AuxiliaryComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.CachingRelevantPredicatesComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.OccurrenceComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.RelevantPredicatesComputer;


/**
 * Implements an ABM-based predicate CPA.
 * @author dwonisch
 *
 */
@Options(prefix="cpa.predicate.abm")
public class ABMPredicateCPA extends PredicateCPA implements ConfigurableProgramAnalysisWithABM {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ABMPredicateCPA.class);
  }

  private final RelevantPredicatesComputer relevantPredicatesComputer;
  private final ABMPredicateReducer reducer;
  private final ABMPredicateTransferRelation transfer;

  @Option(description="whether to use auxiliary predidates for reduction")
  private boolean auxiliaryPredicateComputer = true;

  private ABMPredicateCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    super(config, logger);

    config.inject(this, ABMPredicateCPA.class);

    if (auxiliaryPredicateComputer) {
      relevantPredicatesComputer = new CachingRelevantPredicatesComputer(new AuxiliaryComputer());
    } else {
      relevantPredicatesComputer = new CachingRelevantPredicatesComputer(new OccurrenceComputer());
    }

    reducer = new ABMPredicateReducer(this);
    transfer = new ABMPredicateTransferRelation(this);
  }

  @Override
  protected PredicateCPAStatistics createStatistics() throws InvalidConfigurationException {
    return new ABMPredicateCPAStatistics(this);
  }

  @Override
  protected Configuration getConfiguration() {
    return super.getConfiguration();
  }

  @Override
  public LogManager getLogger() {
    return super.getLogger();
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  public RelevantPredicatesComputer getRelevantPredicatesComputer() {
    return relevantPredicatesComputer;
  }

  @Override
  public ABMPredicateTransferRelation getTransferRelation() {
    return transfer;
  }
}
