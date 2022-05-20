// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.chc;

import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
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
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

@Options(prefix = "cpa.chc")
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
  @Option(
      secure = true,
      name = "firingRelation",
      values = {"Always", "Maxcoeff", "Sumcoeff", "Homeocoeff"},
      description = "firing relation to be used in the precision adjustment operator")
  private String firingRelation = "Always";

  /*
   * Set the generalization operator.
   * "Generalization Strategies for the Verification of Infinite State Systems"
   * by F.Fioravanti, A.Pettorossi, M.Proietti and V.Senni
   * Theory and Practice of Logic Programming, Vol. 13, Special Issue 02, 2013, pp. 175-199
   * DOI: http://dx.doi.org/10.1017/S1471068411000627
   */
  @Option(
      secure = true,
      name = "generalizationOperator",
      values = {"Top", "Widen", "WidenMax", "WidenSum"},
      description = "generalization operator to be used in the precision adjustment operator")
  private String generalizationOperator = "Widen";

  private final AbstractDomain abstractDomain;
  private final Precision precision;
  private final PrecisionAdjustment precisionAdjustment;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;

  private CHCCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {

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
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    CHCState initialState = new CHCState();
    initialState.setNodeNumber(1);
    return initialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }

  public Precision getPrecision() {
    return precision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(
        new Statistics() {

          @Override
          public void printStatistics(
              PrintStream out, Result result, UnmodifiableReachedSet reached) {
            // TODO
            // transferRelation.printStatistics(out);
          }

          @Override
          public String getName() {
            return "CLPCPA";
          }
        });
  }
}
