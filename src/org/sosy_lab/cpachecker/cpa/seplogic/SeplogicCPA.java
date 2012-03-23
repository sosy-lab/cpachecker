/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.seplogic;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
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
import org.sosy_lab.cpachecker.cpa.seplogic.csif.CorestarInterface;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Empty;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Formula;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.SeparatingConjunction;

@Options(prefix="cpa.seplogic")
public class SeplogicCPA implements ConfigurableProgramAnalysis, StatisticsProvider {
  class SeplogicStatistics implements Statistics {

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
      CorestarInterface csInt = CorestarInterface.getInstance();
      pOut.println("Number of implication queries: " + csInt.getImpTimer().getNumberOfIntervals());
      pOut.println("Time for implication queries: " + csInt.getImpTimer());
      pOut.println();
      pOut.println("Number of frame queries: " + csInt.getFrameTimer().getNumberOfIntervals());
      pOut.println("Time for frame queries: " + csInt.getFrameTimer());
      pOut.println();
      pOut.println("Number of spec ass queries: " + csInt.getSpecAssTimer().getNumberOfIntervals());
      pOut.println("Time for spec ass queries: " + csInt.getSpecAssTimer());
      pOut.println();
      pOut.println("Number of abstraction queries: " + csInt.getAbstractionTimer().getNumberOfIntervals());
      pOut.println("Time for abstraction queries: " + csInt.getAbstractionTimer());
      pOut.println();
      pOut.println("Time for parsing: " + csInt.getParsingTimer());
    }

    @Override
    public String getName() {
      return "SeplogicCPA";
    }

  }
  private AbstractDomain abstractDomain = new SeplogicDomain();
  private TransferRelation transferRelation = new SeplogicTransferRelation();
  private MergeOperator mergeOperator = MergeSepOperator.getInstance();
  private StopOperator stopOperator = new StopSepOperator(abstractDomain);
  private Formula abductionState = new SeparatingConjunction(Formula.TRUE, new Empty());
  private Statistics stats = new SeplogicStatistics();


  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SeplogicCPA.class);
  }

  private SeplogicCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
    CorestarInterface.prepare(config);
    CorestarInterface.getInstance().setLogger(logger);
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
    return new SeplogicPrecisionAdjustment();
  }

  @Override
  public AbstractElement getInitialElement(CFANode pNode) {
    Deque<String> namespaces = new ArrayDeque<String>();
    namespaces.push("main");
    return new SeplogicElement(abductionState, namespaces);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }

  public void setAbductionState(Formula pMissing) {
    abductionState = pMissing;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

}
