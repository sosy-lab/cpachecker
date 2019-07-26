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
package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.AllTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.DependencyPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.ImplicitDependencyPrecision;

/**
 * CPA for tracking the Active Control Dependencies
 */
@Options(prefix = "cpa.ifcsecurity")
public class ControlDependencyTrackerCPA implements ConfigurableProgramAnalysis, ProofCheckerCPA {

  @SuppressWarnings("unused")
  private LogManager logger;

  private AbstractDomain domain;
  private ControlDependencyTrackerRelation transfer;
  private DependencyPrecision precision;


  @SuppressWarnings("unused")
  private CFA cfa;

  /**
   * Internal Variable: Control Dependencies
   */
  private Map<CFANode, TreeSet<CFANode>> rcd;

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = { "SEP", "JOIN" },
      description = "which merge operator to use for ControlDependencyTrackerCPA")
  private String mergeType = "JOIN";

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = { "SEP", "JOIN" },
      description = "which stop operator to use for ControlDependencyTrackerCPA")
  private String stopType = "SEP";

  @Option(
      secure = true,
      name = "precisionType",
      values = { "pol-indep","pol-dep"},
      description = "which stop operator to use for DependencyTrackerCPA")
  private String precisionType = "pol-indep";

  private StopOperator stop;
  private MergeOperator merge;


  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ControlDependencyTrackerCPA.class);
  }

  private ControlDependencyTrackerCPA(LogManager pLogger, Configuration pConfig,
      ShutdownNotifier pShutdownNotifier, CFA pCfa) throws InvalidConfigurationException {
    pConfig.inject(this);
    this.logger = pLogger;
    this.cfa = pCfa;

    Dominators postdom = new Dominators(pCfa, 1);
    postdom.execute();
    Map<CFANode, CFANode> postdominators = postdom.getDom();
    pLogger.log(Level.FINE, "Postdominators");
    pLogger.log(Level.FINE, postdominators);

    LHOreduc lhoreduc = new LHOreduc(pLogger, pConfig, pCfa, 1, postdominators);
    lhoreduc.execute();
    ArrayList<CFANode> lhoOrder = lhoreduc.getLHO();
    Map<CFANode, List<CFANode>> rDom = lhoreduc.getRDom();
    pLogger.log(Level.FINE, "LHO-Order:");
    pLogger.log(Level.FINE, lhoOrder);




    DominanceFrontier domfron = new DominanceFrontier(pCfa, postdominators);
    domfron.execute();
    Map<CFANode, TreeSet<CFANode>> df = domfron.getDominanceFrontier();
    pLogger.log(Level.FINE, "Dominance Frontier");
    pLogger.log(Level.FINE, df);

    ControlDependenceComputer cdcom = new ControlDependenceComputer(pCfa, df);
    cdcom.execute();
    Map<CFANode, TreeSet<CFANode>> cd = cdcom.getControlDependency();
    pLogger.log(Level.FINE, "Control Dependency");
    pLogger.log(Level.FINE, cd);

    Map<CFANode, TreeSet<CFANode>> recd = cdcom.getReversedControlDependency();
    pLogger.log(Level.FINE, "Reversed Control Dependency");
    pLogger.log(Level.FINE, recd);
    this.rcd = recd;

    pLogger.log(Level.FINE, "Dom=:" + postdominators);
    pLogger.log(Level.FINE, "RDom=:" + rDom);
    pLogger.log(Level.FINE, "CD=:" + cd);
    pLogger.log(Level.FINE, "RCD=:" + recd);
    pLogger.log(Level.FINE, "PDF=:" + df);

    lhoreduc.writeLHOOrder();

    domain = DelegateAbstractDomain.<ControlDependencyTrackerState> getInstance();
    transfer = new ControlDependencyTrackerRelation(pLogger, pShutdownNotifier, rcd);
    precision=choosePrecision(pConfig, pLogger);


    if (stopType.equals("SEP")) {
      stop = new StopSepOperator(domain);
    } else if (stopType.equals("JOIN")) {
      stop = new StopJoinOperator(domain);
    }
    if (mergeType.equals("SEP")) {
      merge = MergeSepOperator.getInstance();
    } else if (mergeType.equals("JOIN")) {
      merge = new MergeJoinOperator(domain);
    }
  }

  private DependencyPrecision choosePrecision(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    //TODO other choices by option
    if(precisionType.equals("pol-dep")){
      return new ImplicitDependencyPrecision(pConfig, pLogger);
    }
    if(precisionType.equals("pol-indep")){
      return new AllTrackingPrecision(pConfig, pLogger);
    }
    return null;
  }

  /**
   * Internal Class for Representing a 2-Pair
   * @param <T> Type of the first element
   * @param <E> Type of the second element
   */
  static class Pair<T, E> {

    /**
    * first element
    */
    private T first;
    /**
    * second element
    */
    private E second;

    /**
    * Generates a new 2-Pair
    * @param first The first element of the 2-Pair.
    * @param second The second element of the 2-Pair.
    */
    public Pair(T first, E second) {
      this.first = first;
      this.second = second;
    }

    /**
    * Returns the first element of the 2-Pair.
    * @return The first element of the 2-Pair.
    */
    public T getFirst() {
      return first;
    }

    /**
    * Returns the second element of the 2-Pair.
    * @return The second element of the 2-Pair.
    */
    public E getSecond() {
      return second;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) { return false; }
      if (!(obj instanceof Pair)) { return false; }
      @SuppressWarnings("unchecked")
      Pair<T, E> other = (Pair<T, E>) obj;
      return (first.equals(other.first) && second.equals(other.second));
    }

    @Override
    public String toString() {
      return "["
          + ((first == null) ? "Null"
              : ((first instanceof CExpression) ? ((CExpression) first).toASTString()
                  : first.toString()))
          + "," + ((second == null) ? "Null" : ((second instanceof CExpression)
              ? ((CExpression) second).toASTString() : (second.toString())))
          + "]";
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    ControlDependencyTrackerState initialstate = new ControlDependencyTrackerState();
    return initialstate;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }
}
