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
package org.sosy_lab.cpachecker.core.algorithm;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment.Action;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeEnvEdge;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;

public class RelyGuaranteeThreadCPAAlgorithm implements Algorithm, StatisticsProvider {

  private static class CPAStatistics implements Statistics {

    private Timer totalTimer         = new Timer();
    private Timer chooseTimer        = new Timer();
    private Timer precisionTimer     = new Timer();
    private Timer transferTimer      = new Timer();
    private Timer mergeTimer         = new Timer();
    private Timer stopTimer          = new Timer();

    private int   countIterations   = 0;
    private int   maxWaitlistSize   = 0;
    private int   countWaitlistSize = 0;
    private int   countSuccessors   = 0;
    private int   maxSuccessors     = 0;
    private int   countMerge        = 0;
    private int   countStop         = 0;
    private int   countBreak        = 0;

    @Override
    public String getName() {
      return "Single thread rely-guarantee CPA algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {
      out.println("Number of iterations:            " + countIterations);
      out.println("Max size of waitlist:            " + maxWaitlistSize);
      out.println("Average size of waitlist:        " + countWaitlistSize
          / countIterations);
      out.println("Number of computed successors:   " + countSuccessors);
      out.println("Max successors for one element:  " + maxSuccessors);
      out.println("Number of times merged:          " + countMerge);
      out.println("Number of times stopped:         " + countStop);
      out.println("Number of times breaked:         " + countBreak);
      out.println();
      out.println("Total time for CPA algorithm:   " + totalTimer + " (Max: " + totalTimer.printMaxTime() + ")");
      out.println("Time for choose from waitlist:  " + chooseTimer);
      out.println("Time for precision adjustment:  " + precisionTimer);
      out.println("Time for transfer relation:     " + transferTimer);
      out.println("Time for merge operator:        " + mergeTimer);
      out.println("Time for stop operator:         " + stopTimer);
    }
  }

  private final CPAStatistics               stats = new CPAStatistics();
  private final ConfigurableProgramAnalysis           cpa;
  private final LogManager                  logger;
  private Vector<RelyGuaranteeEnvEdge> envTransitions;
  private int tid;

  public RelyGuaranteeThreadCPAAlgorithm(ConfigurableProgramAnalysis cpa, Vector<RelyGuaranteeEnvEdge> envTransitions, LogManager logger, Set<String> pGlobalVariables, int tid) {
    this.cpa = cpa;
    this.envTransitions = envTransitions;
    this.logger = logger;
    this.tid = tid;
    //this.cpa.setGlobalVariables(pGlobalVariables);
    //this.cpa.setThreadId(tid);
  }

  @Override
  public boolean run(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    stats.totalTimer.start();
    System.out.println();
    System.out.println("## Running thread "+this.tid+" ##");
    final TransferRelation transferRelation = cpa.getTransferRelation();
    final MergeOperator mergeOperator = cpa.getMergeOperator();
    final StopOperator stopOperator = cpa.getStopOperator();
    final PrecisionAdjustment precisionAdjustment =
        cpa.getPrecisionAdjustment();

    while (reachedSet.hasWaitingElement()) {
      CPAchecker.stopIfNecessary();

      stats.countIterations++;

      // Pick next element using strategy
      // BFS, DFS or top sort according to the configuration
      int size = reachedSet.getWaitlistSize();
      if (size >= stats.maxWaitlistSize) {
        stats.maxWaitlistSize = size;
      }
      stats.countWaitlistSize += size;

      stats.chooseTimer.start();
      final AbstractElement element =  reachedSet.popFromWaitlist();
      final Precision precision = reachedSet.getPrecision(element);
      //fina Precision precision = this.cpa.

      stats.chooseTimer.stop();

      logger.log(Level.FINER, "Retrieved element from waitlist");
      logger.log(Level.ALL, "Current element is", element, "with precision",
          precision);

      stats.transferTimer.start();
      // pretty printing of predecessors
      RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(element, RelyGuaranteeAbstractElement.class);
      System.out.println();
      System.out.println("@ Successor of '"+rgElement.getAbstractionFormula()+"','"+rgElement.getPathFormula()+
          "' with SSAMap "+rgElement.getPathFormula().getSsa());

      Collection<? extends AbstractElement> successors =
          transferRelation.getAbstractSuccessors(element, precision, null);
      stats.transferTimer.stop();
          // TODO stats...
          // create and environmental edge and add it the global storage
          Vector<RelyGuaranteeEnvEdge> newEnvTransitions = createEnvTransitions(element);
          if (!newEnvTransitions.isEmpty()){
            envTransitions.addAll(newEnvTransitions);
          }


      // TODO When we have a nice way to mark the analysis result as incomplete,
      // we could continue analysis on a CPATransferException with the next element from waitlist.

      int numSuccessors = successors.size();
      logger.log(Level.FINER, "Current element has", numSuccessors,
          "successors");
      stats.countSuccessors += numSuccessors;
      stats.maxSuccessors = Math.max(numSuccessors, stats.maxSuccessors);

      for (AbstractElement successor : successors) {
        logger.log(Level.FINER, "Considering successor of current element");
        logger.log(Level.ALL, "Successor of", element, "\nis", successor);

        stats.precisionTimer.start();
        Triple<AbstractElement, Precision, Action> precAdjustmentResult =
            precisionAdjustment.prec(successor, precision, reachedSet);
        stats.precisionTimer.stop();

        successor = precAdjustmentResult.getFirst();
        Precision successorPrecision = precAdjustmentResult.getSecond();
        Action action = precAdjustmentResult.getThird();

        printRelyGuaranteeAbstractElement(successor);


        if (action == Action.BREAK) {
          stats.countBreak++;
          // re-add the old element to the waitlist, there may be unhandled
          // successors left that otherwise would be forgotten
          reachedSet.reAddToWaitlist(element);
          reachedSet.add(successor, successorPrecision);

          stats.totalTimer.stop();
          return true;
        }
        assert action == Action.CONTINUE : "Enum Action has unhandled values!";

        Collection<AbstractElement> reached = reachedSet.getReached(successor);

        // An optimization, we don't bother merging if we know that the
        // merge operator won't do anything (i.e., it is merge-sep).
        if (mergeOperator != MergeSepOperator.getInstance() && !reached.isEmpty()) {
          stats.mergeTimer.start();

          List<AbstractElement> toRemove = new ArrayList<AbstractElement>();
          List<Pair<AbstractElement, Precision>> toAdd =
              new ArrayList<Pair<AbstractElement, Precision>>();

          logger.log(Level.FINER, "Considering", reached.size(),
              "elements from reached set for merge");
          for (AbstractElement reachedElement : reached) {
            AbstractElement mergedElement =
                mergeOperator.merge(successor, reachedElement,
                    successorPrecision);

            if (!mergedElement.equals(reachedElement)) {
              logger.log(Level.FINER,
                  "Successor was merged with element from reached set");
              logger.log(Level.ALL, "Merged", successor, "\nand",
                  reachedElement, "\n-->", mergedElement);
              stats.countMerge++;

              toRemove.add(reachedElement);
              toAdd.add(Pair.of(mergedElement, successorPrecision));
            }
          }
          reachedSet.removeAll(toRemove);
          reachedSet.addAll(toAdd);

          stats.mergeTimer.stop();
        }

        stats.stopTimer.start();
        boolean stop =
            stopOperator.stop(successor, reached, successorPrecision);
        stats.stopTimer.stop();

        if (stop) {
          logger.log(Level.FINER,
              "Successor is covered or unreachable, not adding to waitlist");
          stats.countStop++;

        } else {
          logger.log(Level.FINER,
              "No need to stop, adding successor to waitlist");

          //RelyGuaranteeAbstractElement rgSuccessor = (RelyGuaranteeAbstractElement) successor;
          // System.out.println("@ Adding to reached '"+rgSuccessor.getAbstractionFormula()+"','"+rgSuccessor.getPathFormula()+"'");
          reachedSet.add(successor, successorPrecision);
        }
      }
    }
    stats.totalTimer.stop();
    return true;
  }

 // pretty-printing of successors
  private void printRelyGuaranteeAbstractElement(AbstractElement pSuccessor) {
    //ARTElement aElement = (ARTElement) pSuccessor;
    RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(pSuccessor, RelyGuaranteeAbstractElement.class);
    if (rgElement.getParentEdge() == null){
      System.out.println("- by local edge UNKNOWN");
    }
    else if (rgElement.getParentEdge().getEdgeType() == CFAEdgeType.EnvironmentalEdge){
      RelyGuaranteeEnvEdge rgEdge = (RelyGuaranteeEnvEdge) rgElement.getParentEdge();
      System.out.println("- by env. edge '"+rgEdge.getAbstractionFormula()+"','"+rgEdge.getPathFormula()+"','"+rgEdge.getLocalEdge().getRawStatement()+"'");
    } else {
      System.out.println("- by local edge "+rgElement.getParentEdge().getRawStatement());
    }
    System.out.println("\t is '"+rgElement.getAbstractionFormula()+"','"+rgElement.getPathFormula()+"' with SSA "+rgElement.getPathFormula().getSsa());
    //System.out.println();

  }

  // generate and environmental edge
  private Vector<RelyGuaranteeEnvEdge> createEnvTransitions(AbstractElement pElement) {
    // get the underlying PredicateAbstractElement
    Vector<RelyGuaranteeEnvEdge> envTransitions = new Vector<RelyGuaranteeEnvEdge>();
    ARTElement aElement  = (ARTElement) pElement;
    CompositeElement cElement = (CompositeElement)  aElement.getWrappedElement();
    CFANode node = cElement.retrieveLocationElement().getLocationNode();
    // find the predicate CPA
    RelyGuaranteeAbstractElement predElement = AbstractElements.extractElementByType(cElement, RelyGuaranteeAbstractElement.class);

    // create an environmental edge for every outgoing assigment
    CFAEdge edge;
    for (int i=0; i<node.getNumLeavingEdges(); i++){
      edge = node.getLeavingEdge(i);
      if (this.createsEnvTransition(edge)) {
        RelyGuaranteeEnvEdge newEnvTransition = new RelyGuaranteeEnvEdge(edge, predElement.getAbstractionFormula(), predElement.getPathFormula(), this.tid);
        logger.log(Level.ALL, "Created",newEnvTransition);
        envTransitions.add(newEnvTransition);
      }
    }

    return envTransitions;
  }

  // returns true if an enviornmental transition is created from the edge
  private boolean createsEnvTransition(CFAEdge edge){
    if (edge.getRawAST() instanceof IASTFunctionCallStatement) {
      return false;
    }
    if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      return true;
    }
    if (edge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      return false;
    }
    return false;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return cpa;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
