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
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * CPA for tracking the Active Control Dependencies
 */
@Options(prefix = "cpa.ifcsecurity")
public class ControlDependencyTrackerCPA implements ConfigurableProgramAnalysis {

  @SuppressWarnings("unused")
  private LogManager logger;
  private AbstractDomain domain;
  private ControlDependencyTrackerRelation transfer;
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

  private StopOperator stop;
  private MergeOperator merge;


  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ControlDependencyTrackerCPA.class);
  }

  private ControlDependencyTrackerCPA(LogManager logger, Configuration config,
      ShutdownNotifier shutdownNotifier, CFA cfa) throws InvalidConfigurationException {
    config.inject(this);
    this.logger = logger;
    this.cfa = cfa;

    Dominators postdom = new Dominators(cfa, 1);
    postdom.execute();
    Map<CFANode, CFANode> postdominators = postdom.getDom();
    logger.log(Level.FINE, "Postdominators");
    logger.log(Level.FINE, postdominators);

    DominanceFrontier domfron = new DominanceFrontier(cfa, postdominators);
    domfron.execute();
    Map<CFANode, TreeSet<CFANode>> df = domfron.getDominanceFrontier();
    logger.log(Level.FINE, "Dominance Frontier");
    logger.log(Level.FINE, df);

    ControlDependenceComputer cdcom = new ControlDependenceComputer(cfa, df);
    cdcom.execute();
    Map<CFANode, TreeSet<CFANode>> cd = cdcom.getControlDependency();
    logger.log(Level.FINE, "Control Dependency");
    logger.log(Level.FINE, cd);

    Map<CFANode, TreeSet<CFANode>> recd = cdcom.getReversedControlDependency();
    logger.log(Level.FINE, "Reversed Control Dependency");
    logger.log(Level.FINE, recd);
    this.rcd = recd;

    domain = DelegateAbstractDomain.<ControlDependencyTrackerState> getInstance();
    transfer = new ControlDependencyTrackerRelation(logger, shutdownNotifier, rcd);

    if (stopType.equals("SEP")) {
      stop = new StopSepOperator(domain);
    } else if (mergeType.equals("JOIN")) {
      stop = new StopJoinOperator(domain);
    }
    if (mergeType.equals("SEP")) {
      merge = MergeSepOperator.getInstance();
    } else if (mergeType.equals("JOIN")) {
      merge = new MergeJoinOperator(domain);
    }
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
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    ControlDependencyTrackerState initialstate = new ControlDependencyTrackerState();
    return initialstate;
  }
}
