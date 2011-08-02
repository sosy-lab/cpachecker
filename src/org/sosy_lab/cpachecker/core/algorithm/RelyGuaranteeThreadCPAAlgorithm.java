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
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeEnvironment;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeEnvironmentalTransition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;

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
  private final ConfigurableProgramAnalysis       cpa;
  private final LogManager                  logger;
  private RelyGuaranteeEnvironment environment;
  private int tid;

  private MathsatFormulaManager fManager;

  public RelyGuaranteeThreadCPAAlgorithm(ConfigurableProgramAnalysis  cpa, RelyGuaranteeEnvironment environment, Configuration config, LogManager logger,  int tid) {
    this.cpa = cpa;
    this.environment = environment;
    this.logger = logger;
    this.tid = tid;

    try {
      fManager = MathsatFormulaManager.getInstance(config, logger);
    } catch (InvalidConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

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

      stats.chooseTimer.stop();

      logger.log(Level.FINER, "Retrieved element from waitlist");
      logger.log(Level.ALL, "Current element is", element, "with precision",
          precision);

      stats.transferTimer.start();
      // pretty printing of predecessors
      ARTElement aElement = (ARTElement) element;

      RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(element, RelyGuaranteeAbstractElement.class);
      System.out.println();
      int atomNo = fManager.countAtoms(rgElement.getPathFormula().getFormula());
      System.out.println("@ Successor of '"+rgElement.getAbstractionFormula()+"','"+rgElement.getPathFormula()+
          "' with SSAMap "+rgElement.getPathFormula().getSsa()+" atomNo="+atomNo+" id:"+aElement.getElementId());


      Collection<? extends AbstractElement> successors =
        transferRelation.getAbstractSuccessors(element, precision, null);
      stats.transferTimer.stop();
      // TODO stats...
      // create and environmental edge and add it the global storage
      Vector<RelyGuaranteeEnvironmentalTransition> newEnvTransitions = createEnvTransitions(element);
      environment.addEnvTransitions(aElement, newEnvTransitions);



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
        AbstractElement mergedElement = null;
        boolean successorMerged = false;

        if (mergeOperator != MergeSepOperator.getInstance() && !reached.isEmpty()) {
          stats.mergeTimer.start();

          List<AbstractElement> toRemove = new ArrayList<AbstractElement>();
          List<Pair<AbstractElement, Precision>> toAdd =
            new ArrayList<Pair<AbstractElement, Precision>>();

          // TODO debugging
          if (AbstractElements.extractLocation(successor).toString().contains("24")){
            System.out.println();
          }
          /*int succ = ((ARTElement)successor).getElementId();
          if (succ == 647 || succ == 642){
            System.out.println();
          }*/


          logger.log(Level.FINER, "Considering", reached.size(),
          "elements from reached set for merge");
          for (AbstractElement reachedElement : reached) {

            RelyGuaranteeAbstractElement rsucc = AbstractElements.extractElementByType(successor, RelyGuaranteeAbstractElement.class);
            RelyGuaranteeAbstractElement rreach = AbstractElements.extractElementByType(reachedElement, RelyGuaranteeAbstractElement.class);
            if (!(rsucc instanceof RelyGuaranteeAbstractElement.AbstractionElement) && !(rreach instanceof RelyGuaranteeAbstractElement.AbstractionElement)){
              RelyGuaranteeAbstractElement rfake = AbstractElements.extractElementByType(mergedElement, RelyGuaranteeAbstractElement.class);
              if (rsucc.getAbstractionFormula().toString().contains("cs1@2 = 0") && rreach.getAbstractionFormula().toString().contains("cs1@2 = 0") && rreach.getPathFormula().toString().contains("(g@3 = 1)")){
                  System.out.println();
              }
            }

            mergedElement = mergeOperator.merge(successor, reachedElement,successorPrecision);


            if (!mergedElement.equals(reachedElement)) {

              logger.log(Level.FINER,
                  "Successor was merged with element from reached set");
              logger.log(Level.ALL, "Merged", successor, "\nand",
                  reachedElement, "\n-->", mergedElement);
              stats.countMerge++;
              // adjust precision after merging
         /*     precAdjustmentResult = precisionAdjustment.prec(mergedElement, successorPrecision, reachedSet);
              mergedElement = precAdjustmentResult.getFirst();
              Precision mergedPrecision = precAdjustmentResult.getSecond();
              toRemove.add(reachedElement);
              // TODO add hoc solution - the case of predicate abstraction,
              // the sucessor is subsummed by the merged element, but after abstraction of the merged element, this may not be detected
              successorMerged = true;
              // check if the merged element is covered

              rsucc = AbstractElements.extractElementByType(successor, RelyGuaranteeAbstractElement.class);
              rreach = AbstractElements.extractElementByType(reachedElement, RelyGuaranteeAbstractElement.class);
              if (!(rsucc instanceof RelyGuaranteeAbstractElement.AbstractionElement) && !(rreach instanceof RelyGuaranteeAbstractElement.AbstractionElement)){
                RelyGuaranteeAbstractElement rfake = AbstractElements.extractElementByType(mergedElement, RelyGuaranteeAbstractElement.class);
                if (rsucc.getAbstractionFormula().toString().contains("cs1@2 = 0") && rreach.getAbstractionFormula().toString().contains("cs1@2 = 0")){
                  if (!rfake.getAbstractionFormula().toString().contains("cs1@2 = 0")){
                    System.out.println();
                  }
                }
              }




              // check
              RelyGuaranteeAbstractElement rSucc = AbstractElements.extractElementByType(successor, RelyGuaranteeAbstractElement.class);
              RelyGuaranteeAbstractElement rReached = AbstractElements.extractElementByType(successor, RelyGuaranteeAbstractElement.class);
              RelyGuaranteeAbstractElement rMerged = AbstractElements.extractElementByType(successor, RelyGuaranteeAbstractElement.class);
              if (rSucc.getAbstractionFormula().equals(rReached.getAbstractionFormula()) && !rSucc.getAbstractionFormula().equals(rMerged.getAbstractionFormula())){
                System.out.println();
              }*/


              printMerge(successor, reachedElement, precAdjustmentResult.getFirst());
            /*  boolean stop =  stopOperator.stop(mergedElement, reached, mergedPrecision);

              if (stop) {
                printCovered(mergedElement);
              } else {
                toAdd.add(Pair.of(mergedElement, mergedPrecision));
              }*/
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

        if (stop || successorMerged) {
          logger.log(Level.FINER,
              "Successor is covered or unreachable, not adding to waitlist");
          printCovered(successor);
          stats.countStop++;

        } else {
          logger.log(Level.FINER,
          "No need to stop, adding successor to waitlist");

          //RelyGuaranteeAbstractElement rgSuccessor = (RelyGuaranteeAbstractElement) successor;
          // System.out.println("@ Adding to reached '"+rgSuccessor.getAbstractionFormula()+"','"+rgSuccessor.getPathFormula()+"'");
          reachedSet.add(successor, successorPrecision);
        }
      }
      System.out.println();
    }
    stats.totalTimer.stop();
    return true;
  }

  private void printCovered(AbstractElement pSuccessor) {
    ARTElement aSuccessor = (ARTElement) pSuccessor;
    RelyGuaranteeAbstractElement rSuccessor = AbstractElements.extractElementByType(pSuccessor, RelyGuaranteeAbstractElement.class);
    System.out.println("^ Covered  '"+rSuccessor.getAbstractionFormula()+"','"+rSuccessor.getPathFormula()+"' with SSA "+rSuccessor.getPathFormula().getSsa()+" id:"+aSuccessor.getElementId());

  }

  private void printMerge(AbstractElement pSuccessor, AbstractElement pReachedElement, AbstractElement pMergedElement) {
    ARTElement aSuccessor = (ARTElement) pSuccessor;
    ARTElement aReachedElement = (ARTElement) pReachedElement;
    ARTElement aMergedElement = (ARTElement) pMergedElement;
    RelyGuaranteeAbstractElement rSuccessor = AbstractElements.extractElementByType(pSuccessor, RelyGuaranteeAbstractElement.class);
    RelyGuaranteeAbstractElement rReached   = AbstractElements.extractElementByType(pReachedElement, RelyGuaranteeAbstractElement.class);
    RelyGuaranteeAbstractElement rMerged  = AbstractElements.extractElementByType(pMergedElement, RelyGuaranteeAbstractElement.class);
    int atomNo = fManager.countAtoms(rMerged.getPathFormula().getFormula());
    System.out.println("+ merged '"+rSuccessor.getAbstractionFormula()+"','"+rSuccessor.getPathFormula()+"' with SSA "+rSuccessor.getPathFormula().getSsa()+" id:"+aSuccessor.getElementId());
    System.out.println("\twith '"+rReached.getAbstractionFormula()+"','"+rReached.getPathFormula()+"' with SSA "+rReached.getPathFormula().getSsa()+" id:"+aReachedElement.getElementId());
    System.out.println("\t= '"+rMerged.getAbstractionFormula()+"','"+rMerged.getPathFormula()+"' with SSA "+rMerged.getPathFormula().getSsa()+" atomNo="+atomNo+" id:"+aMergedElement.getElementId());
  }

  // pretty-printing of successors
  private void printRelyGuaranteeAbstractElement(AbstractElement pSuccessor) {
    ARTElement aElement = (ARTElement) pSuccessor;
    RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(pSuccessor, RelyGuaranteeAbstractElement.class);
    if (rgElement.getParentEdge() == null){
      System.out.println("- by local edge UNKNOWN");
    }
    else if (rgElement.getParentEdge().getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
      RelyGuaranteeCFAEdge rgEdge = (RelyGuaranteeCFAEdge) rgElement.getParentEdge();
      int atomNo = fManager.countAtoms(rgEdge.getPathFormula().getFormula());
      System.out.println("- by env. edge '"+rgEdge.getLocalEdge().getRawStatement()+"','"+rgEdge.getPathFormula()+"' SSA "+rgEdge.getPathFormula().getSsa()+" atomNo="+atomNo+" id:"+aElement.getElementId());
    } else {
      System.out.println("- by local edge "+rgElement.getParentEdge().getRawStatement());
    }
    int atomNo2 = fManager.countAtoms(rgElement.getPathFormula().getFormula());
    System.out.println("\t is '"+rgElement.getAbstractionFormula()+"','"+rgElement.getPathFormula()+"' with SSA "+rgElement.getPathFormula().getSsa()+" atomNo="+atomNo2+" id:"+aElement.getElementId());
    //System.out.println();

  }

  // generate  environmental edges for every outgoing
  private Vector<RelyGuaranteeEnvironmentalTransition> createEnvTransitions(AbstractElement pElement) {
    // get the underlying PredicateAbstractElement
    Vector<RelyGuaranteeEnvironmentalTransition> envTransitions = new  Vector<RelyGuaranteeEnvironmentalTransition>();
    ARTElement aElement  = (ARTElement) pElement;
    CompositeElement cElement = (CompositeElement)  aElement.getWrappedElement();
    CFANode node = cElement.retrieveLocationElement().getLocationNode();
    // find the predicate CPA
    RelyGuaranteeAbstractElement predElement = AbstractElements.extractElementByType(cElement, RelyGuaranteeAbstractElement.class);

    // create an environmental edge for every outgoing assignment
    CFAEdge edge;
    for (int i=0; i<node.getNumLeavingEdges(); i++){
      edge = node.getLeavingEdge(i);
      if (this.createsEnvTransition(edge)) {
        //RelyGuaranteeEnvironmentalTransition newEnvTransition = new RelyGuaranteeEnvironmentalTransition(predElement.getAbstractionFormula().asFormula(), predElement.getPathFormula(), edge,  this.tid);
        RelyGuaranteeEnvironmentalTransition newEnvTransition = new RelyGuaranteeEnvironmentalTransition(aElement, edge,  this.tid);
        envTransitions.add(newEnvTransition);
      }
    }

    return envTransitions;
  }



  // returns true if an environmental transition should be created by the edge
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
