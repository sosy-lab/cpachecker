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
package org.sosy_lab.cpachecker.cpa.chc;

import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
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
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

@Options(prefix="cpa.chc")
public class CHCCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(CHCCPA.class);
  }

  /*
   * Set the firing relation.
   * "Generalization Strategies for the Verification of Infinite State Systems"
   * by F.Fioravanti, A.Pettorossi, M.Proietti and V.Senni
   * Theory and Practice of Logic Programming, Vol. 13, Special Issue 02, 2013, pp. 175-199
   * DOI: http://dx.doi.org/10.1017/S1471068411000627
   */
  @Option(name="firingRelation", values={"Always","Maxcoeff","Sumcoeff","Homeocoeff"},
      description="firing relation to be used in the precision adjustment operator")
  private String firingRelation = "Always";

  /*
   * Set the generalization operator.
   * "Generalization Strategies for the Verification of Infinite State Systems"
   * by F.Fioravanti, A.Pettorossi, M.Proietti and V.Senni
   * Theory and Practice of Logic Programming, Vol. 13, Special Issue 02, 2013, pp. 175-199
   * DOI: http://dx.doi.org/10.1017/S1471068411000627
   */
  @Option(name="generalizationOperator", values={"Top","Widen","WidenMax","WidenSum"},
      description="generalization operator to be used in the precision adjustment operator")
  private String generalizationOperator = "Widen";

  /*
   * SEP = identity
   * JOIN = convex hull
   */
  @Option(name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="merge operator to be used")
  private String mergeType = "SEP";


  private final AbstractDomain abstractDomain;
  private final Precision precision;
  private final PrecisionAdjustment precisionAdjustment;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;

  private CHCCPA(CFA cfa, Configuration config, LogManager logger)
    throws InvalidConfigurationException {

    config.inject(this);

    abstractDomain = new CHCDomain();
    mergeOperator = MergeSepOperator.getInstance();
    precision = new CHCPrecision();
    precisionAdjustment = new CHCPrecisionAdjustment(logger);
    stopOperator = new StopSepOperator(abstractDomain);
    transferRelation = new CHCTransferRelation(logger);

    if (!ConstraintManager.init(firingRelation, generalizationOperator, logger)) {
      logger.log(Level.WARNING, "CLP interpreter initialization failure.");
    }
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
  public AbstractState getInitialState(CFANode pNode) {
    CHCState initialState = new CHCState();
    initialState.setNodeNumber(1);
    return initialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return precision;
  }

  public Precision getPrecision() {
    return precision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(new Statistics() {

      @Override
      public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
        //TODO
        //transferRelation.printStatistics(out);
      }

      @Override
      public String getName() {
        return "CLPCPA";
      }
    });
  }
}