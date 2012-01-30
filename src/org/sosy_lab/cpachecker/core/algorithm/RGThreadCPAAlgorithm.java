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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.RelyGuaranteeCFA;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGPrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGTransferRelation;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGVariables;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.relyguarantee")
public class RGThreadCPAAlgorithm implements Algorithm, StatisticsProvider {

  @Option(description="Print debugging info?")
  private boolean debug = true;

  @Option(description="If true, then change treads after successors for a state were computed.")
  private boolean changeThread = false;



  public final Stats stats;
  private RunStats runStats;
  private final ConfigurableProgramAnalysis cpa;
  private final RelyGuaranteeCFA            cfa;
  private final LogManager                  logger;
  private RGEnvironmentManager environment;
  private int tid;

  private MathsatFormulaManager fManager;

  private Set<CFANode> nodeForEnvApp;


  public RGThreadCPAAlgorithm(ConfigurableProgramAnalysis  cpa, RelyGuaranteeCFA cfa, RGEnvironmentManager environment, Configuration config, LogManager logger,  int tid) {
    this.cpa = cpa;
    this.cfa = cfa;
    this.environment = environment;
    this.logger = logger;
    this.tid = tid;

    this.stats = new Stats((ARTCPA) cpa, tid);

    try {
      config.inject(this, RGThreadCPAAlgorithm.class);
      fManager = MathsatFormulaManager.getInstance(config, logger);
    } catch (InvalidConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

 // check where to apply env. edges
    nodeForEnvApp = new HashSet<CFANode>();

    RGVariables variables = environment.getVariables();

    Multimap<CFANode, String> lhsVars = cfa.getLhsVariables();
    Multimap<CFANode, String> rhsVars = cfa.getRhsVariables();
    Multimap<CFANode, String> allVars = HashMultimap.create(lhsVars);
    allVars.putAll(rhsVars);

    for(Entry<String, CFANode> entry :   cfa.getCFANodes().entries()){
      CFANode node = entry.getValue();

      if (!node.isEnvAllowed()){
        continue;
      }

      for (String var : allVars.get(node)){
        if (variables.allVars.contains(var)){
          nodeForEnvApp.add(node);
          break;
        }
      }
    }
    System.out.println();

  }

  @Override
  public boolean run(final ReachedSet reachedSet) throws CPAException, InterruptedException {

    if (runStats == null){
      runStats = new RunStats();
    }
    stats.totalTimer.start();
    runStats.totalTimer.start();

    final TransferRelation transferRelation = cpa.getTransferRelation();
    final MergeOperator mergeOperator = cpa.getMergeOperator();
    final StopOperator stopOperator = cpa.getStopOperator();
    final PrecisionAdjustment precisionAdjustment = cpa.getPrecisionAdjustment();

    List<RGEnvTransition> envEdges = environment.getUnappliedEnvEdgesForThread(tid);

    while (reachedSet.hasWaitingElement()) {
      CPAchecker.stopIfNecessary();

      stats.countIterations++;
      runStats.countIterations++;

      // Pick next element using strategy
      // BFS, DFS or top sort according to the configuration
      int size = reachedSet.getWaitlistSize();
      stats.maxWaitlistSize = Math.max(stats.maxWaitlistSize, size);
      stats.countWaitlistSize += size;

      final AbstractElement element =  reachedSet.popFromWaitlist();
      final Precision precision = reachedSet.getPrecision(element);

      logger.log(Level.FINER, "Retrieved element from waitlist");
      logger.log(Level.ALL, "Current element is", element, "with precision",
          precision);



      ARTElement aElement = (ARTElement) element;
      AbstractElementWithLocation lElement = aElement.retrieveLocationElement();
      CFANode loc = lElement.getLocationNode();
      if (debug){
        // pretty printing
        RGAbstractElement rgElement = AbstractElements.extractElementByType(element, RGAbstractElement.class);
        Precision prec = reachedSet.getPrecision(element);
        RGPrecision rgPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
        System.out.println();
        System.out.println("@ Successor of '"+rgElement.getAbstractionFormula()+"','"+rgElement.getPathFormula()+" id:"+aElement.getElementId()+" at "+loc);
      }

      if (aElement.getElementId() == 313){
        System.out.println();
      }

      stats.transferTimer.start();
      runStats.transferTimer.start();
      // if local child was not expanded, then do it, otherwise only apply new env. transitions
      int edgesNo = 0;

      if (nodeForEnvApp.contains(loc)){
        edgesNo = envEdges.size();
      }

      if (!aElement.hasLocalChild()){
        edgesNo +=  loc.getNumLeavingEdges();
      }

      // edges to be applied
      Set<CFAEdge> edges = new LinkedHashSet<CFAEdge>(edgesNo);


      if (nodeForEnvApp.contains(loc)){
        for (RGEnvTransition template : envEdges){
          // find the edge matching the template
          RGCFAEdge rgEdge = null;
          for (int i=0; i<loc.getNumLeavingEdges(); i++){
            CFAEdge edge = loc.getLeavingEdge(i);
            if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
              rgEdge = (RGCFAEdge) edge;
              if (rgEdge.getRgEnvTransition() == template){
                break;
              }
            }
          }
          assert rgEdge != null;
          edges.add(rgEdge);
        }
      }


      if (!aElement.hasLocalChild()){
        for (int i=0; i<loc.getNumLeavingEdges(); i++){
          CFAEdge lEdge = loc.getLeavingEdge(i);
          edges.add(lEdge);
        }

        aElement.setHasLocalChild(true);
      }

      Collection<Pair<AbstractElement,CFAEdge>> successors = new LinkedHashSet<Pair<AbstractElement,CFAEdge>>(edgesNo);


      for (CFAEdge edge : edges){

        Collection<? extends AbstractElement> newSucc = transferRelation.getAbstractSuccessors(element, precision, edge);
        // generate env edge
        for (AbstractElement successor : newSucc){
          successors.add(Pair.of(successor, edge));
        }

      }
      stats.transferTimer.stop();
      runStats.transferTimer.stop();


      int numSuccessors = successors.size();
      logger.log(Level.FINER, "Current element has", numSuccessors,"successors");
      stats.countSuccessors += numSuccessors;
      runStats.countSuccessors += numSuccessors;
      stats.maxSuccessors = Math.max(numSuccessors, stats.maxSuccessors);

      for (Pair<AbstractElement,CFAEdge> pair : successors) {
        AbstractElement successor = pair.getFirst();
        CFAEdge edge = pair.getSecond();
        // TODO for statistics, could slower down the analysis
        boolean byEnvEdge = false;
        RGAbstractElement rgElement = AbstractElements.extractElementByType(successor, RGAbstractElement.class);


        if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge || edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCombinedCFAEdge){
          byEnvEdge = true;
          stats.countEnvSuccessors++;
          runStats.countEnvSuccessors++;
        }

        logger.log(Level.FINER, "Considering successor of current element");
        logger.log(Level.ALL, "Successor of", element, "\nis", successor);

        stats.precisionTimer.start();
        runStats.precisionTimer.start();
        if (byEnvEdge){
          stats.envPrecisionTimer.start();
        }

        Triple<AbstractElement, Precision, Action> precAdjustmentResult =precisionAdjustment.prec(successor, precision, reachedSet);
        successor = precAdjustmentResult.getFirst();
        Precision successorPrecision = precAdjustmentResult.getSecond();
        Action action = precAdjustmentResult.getThird();

        stats.precisionTimer.stop();
        runStats.precisionTimer.stop();
        stats.envPrecisionTimer.stop();

        if (debug){
          printRelyGuaranteeAbstractElement(successor);
        }

        if (action == Action.BREAK) {
          // re-add the old element to the waitlist, there may be unhandled
          // successors left that otherwise would be forgotten
          reachedSet.reAddToWaitlist(element);
          reachedSet.add(successor, successorPrecision);
          stats.totalTimer.stop();
          runStats.totalTimer.stop();
          return true;
        }
        assert action == Action.CONTINUE : "Enum Action has unhandled values!";

        stats.envGenTimer.start();

        if (this.createsEnvTransition(edge)){
          RGEnvCandidate candidate = new RGEnvCandidate((ARTElement)element, (ARTElement)successor, edge, tid);
          environment.addEnvTransition(candidate);
        }
        stats.envGenTimer.stop();



        Collection<AbstractElement> reached = reachedSet.getReached(successor);

        // An optimization, we don't bother merging if we know that the
        // merge operator won't do anything (i.e., it is merge-sep).

        if (mergeOperator != MergeSepOperator.getInstance() && !reached.isEmpty()) {
          stats.mergeTimer.start();
          runStats.mergeTimer.start();
          if (byEnvEdge){
            stats.envMergeTimer.start();
          }

          List<AbstractElement> toRemove = new ArrayList<AbstractElement>();
          List<Pair<AbstractElement, Precision>> toAdd =
            new ArrayList<Pair<AbstractElement, Precision>>();


          logger.log(Level.FINER, "Considering", reached.size(), "elements from reached set for merge");
          for (AbstractElement reachedElement : reached) {


            AbstractElement  mergedElement = mergeOperator.merge(successor, reachedElement,successorPrecision);

            if (!mergedElement.equals(reachedElement)) {
              environment.mergeSourceElements((ARTElement)mergedElement, (ARTElement)reachedElement, tid);

              logger.log(Level.FINER,
              "Successor was merged with element from reached set");
              logger.log(Level.ALL, "Merged", successor, "\nand",reachedElement, "\n-->", mergedElement);
              stats.countMerge++;

              if (byEnvEdge){
                stats.countEnvMerge++;
              }

              if (debug){
                printMerge(successor, reachedElement, mergedElement);
              }


              toRemove.add(reachedElement);
              toAdd.add(Pair.of(mergedElement, successorPrecision));
            }
          }
          reachedSet.removeAll(toRemove);
          reachedSet.addAll(toAdd);

          stats.mergeTimer.stop();
          stats.envMergeTimer.stop();
          runStats.mergeTimer.stop();
        }




        stats.stopTimer.start();
        runStats.stopTimer.start();
        if (byEnvEdge){
          stats.envStopTimer.start();
        }
        boolean stop = stopOperator.stop(successor, reached, successorPrecision);
        stats.stopTimer.stop();
        stats.envStopTimer.stop();
        runStats.stopTimer.stop();

        if (stop) {
          logger.log(Level.FINER,
          "Successor is covered or unreachable, not adding to waitlist");

          if (debug){
            printCovered(successor);
          }
          stats.countStop++;
          if (byEnvEdge){
            stats.countEnvStop++;
          }

        } else {
          logger.log(Level.FINER,
          "No need to stop, adding successor to waitlist");

          reachedSet.add(successor, successorPrecision);
        }
      }

      if (changeThread && reachedSet.hasWaitingElement()){
        // we switch to another state
        stats.totalTimer.stop();
        runStats.totalTimer.stop();
        return false;
      }
    }

    stats.totalTimer.stop();
    runStats.totalTimer.stop();
    return true;
  }

  private void printCovered(AbstractElement pSuccessor) {
    ARTElement aSuccessor = (ARTElement) pSuccessor;
    RGAbstractElement rSuccessor = AbstractElements.extractElementByType(pSuccessor, RGAbstractElement.class);
    System.out.println("^ Covered  '"+rSuccessor.getAbstractionFormula()+"','"+rSuccessor.getPathFormula()+"' with SSA "+rSuccessor.getPathFormula().getSsa()+" id:"+aSuccessor.getElementId());

  }

  private void printMerge(AbstractElement pSuccessor, AbstractElement pReachedElement, AbstractElement pMergedElement) {
    ARTElement aSuccessor = (ARTElement) pSuccessor;
    ARTElement aReachedElement = (ARTElement) pReachedElement;
    ARTElement aMergedElement = (ARTElement) pMergedElement;
    RGAbstractElement rSuccessor = AbstractElements.extractElementByType(pSuccessor, RGAbstractElement.class);
    RGAbstractElement rReached   = AbstractElements.extractElementByType(pReachedElement, RGAbstractElement.class);
    RGAbstractElement rMerged  = AbstractElements.extractElementByType(pMergedElement, RGAbstractElement.class);
    CFANode lSuccessor = AbstractElements.extractLocation(pSuccessor);
    CFANode lReached = AbstractElements.extractLocation(pSuccessor);
    CFANode lMerged = AbstractElements.extractLocation(pSuccessor);
    System.out.println("+ merged '"+rSuccessor.getAbstractionFormula()+"','"+rSuccessor.getPathFormula()+" id:"+aSuccessor.getElementId()+" at "+lSuccessor);
    System.out.println("\twith '"+rReached.getAbstractionFormula()+"','"+rReached.getPathFormula()+" id:"+aReachedElement.getElementId()+" at "+lReached);
    System.out.println("\t= '"+rMerged.getAbstractionFormula()+"','"+rMerged.getPathFormula()+" id:"+aMergedElement.getElementId()+" at "+lMerged);
  }

  // pretty-printing of successors
  private void printRelyGuaranteeAbstractElement(AbstractElement pSuccessor) {
    ARTElement aElement = (ARTElement) pSuccessor;
    RGAbstractElement rgElement = AbstractElements.extractElementByType(pSuccessor, RGAbstractElement.class);
    CFANode loc = AbstractElements.extractLocation(pSuccessor);
    if (rgElement.getParentEdge() == null){
      System.out.println("- by local edge UNKNOWN");
    }
    else if (rgElement.getParentEdge().getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
      RGCFAEdge rgEdge = (RGCFAEdge) rgElement.getParentEdge();
      System.out.println("- by env. edge '"+rgEdge);
    }
    else if (rgElement.getParentEdge().getEdgeType() == CFAEdgeType.RelyGuaranteeCombinedCFAEdge){
      //RGCombinedCFAEdge rgEdge = (RGCombinedCFAEdge) rgElement.getParentEdge();
      //System.out.println("- by combined env. edge '"+rgEdge);
    }
    else {
      System.out.println("- by local edge "+rgElement.getParentEdge().getRawStatement());
    }
    System.out.println("\t is '"+rgElement.getAbstractionFormula()+"','"+rgElement.getPathFormula()+" id:"+aElement.getElementId()+" at "+loc);

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

  /**
   * Get thread algorithm stats since the begining or {@link restRunStats}.
   * @return
   */
  public RunStats getRunStats() {
    return runStats;
  }

  /**
   * Reset thread algoritm stats.
   */
  public void restRunStats() {
    runStats = null;
  }



  /**
   * Simplified stats covering shorter period of time.
   */
  public static class RunStats implements Statistics {

    private Timer totalTimer         = new Timer();
    private Timer precisionTimer     = new Timer();
    private Timer transferTimer      = new Timer();
    private Timer mergeTimer         = new Timer();
    private Timer stopTimer          = new Timer();

    private int   countIterations     = 0;
    private int   countSuccessors     = 0;
    private int   countEnvSuccessors  = 0;

    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {
      out.println("time:"+totalTimer+" transfer:"+transferTimer+" prec.:"+precisionTimer+" merge:"+mergeTimer+" stop:"+stopTimer);
      out.println("iterations:"+countIterations + " successors:"+countSuccessors+" env. successors:"+countEnvSuccessors);

    }

    @Override
    public String getName() {
      return "RGThreadCPAAlgorithm.run";
    }

  }

  /**
   * Total states for the thread algorithm.
   */
  public static class Stats implements Statistics {

    private final RGCPA cpa;
    private final int tid;

    private Timer totalTimer         = new Timer();
    private Timer envPrecisionTimer  = new Timer();
    private Timer precisionTimer     = new Timer();
    private Timer envTransferTimer   = new Timer();
    private Timer transferTimer      = new Timer();
    private Timer envMergeTimer      = new Timer();
    private Timer mergeTimer         = new Timer();
    private Timer envStopTimer       = new Timer();
    private Timer stopTimer          = new Timer();
    private Timer envGenTimer        = new Timer();

    private int   countIterations     = 0;
    private int   maxWaitlistSize     = 0;
    private int   countWaitlistSize   = 0;
    private int   countSuccessors     = 0;
    private int   countEnvSuccessors  = 0;
    private int   maxSuccessors       = 0;
    private int   countMerge          = 0;
    private int   countEnvMerge       = 0;
    private int   countStop           = 0;
    private int   countEnvStop        = 0;

    public Stats(ARTCPA cpa, int tid){
      this.cpa = cpa.retrieveWrappedCpa(RGCPA.class);
      this.tid = tid;
    }

    @Override
    public String getName() {
      return "RGThreadCPAAlgorithm "+tid;
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {

      RGTransferRelation tr = (RGTransferRelation) cpa.getTransferRelation();

      out.println("number of iterations:            " + formatInt(countIterations));
      out.println("max size of waitlist:            " + formatInt(maxWaitlistSize));
      out.println("average size of waitlist:        " + formatInt(countWaitlistSize/ countIterations));
      out.println();
      out.println("no of environmental successors:  " + formatInt(countEnvSuccessors));
      out.println("no of all successors:            " + formatInt(countSuccessors));
      out.println("max successors for one element:  " + formatInt(maxSuccessors));
      out.println("number of environmetal merges:   " + formatInt(countEnvMerge));
      out.println("number of all merges:            " + formatInt(countMerge));
      out.println("number of environmetal stops:    " + formatInt(countEnvStop));
      out.println("number of all stops:             " + formatInt(countStop));
      out.println("time for generating env. trans.: " + envGenTimer);
      out.println("time for transfer relation:      " + transferTimer);
      out.println("time for transfer form. constr.: " + tr.pfConstructionTimer);
      out.println("time for env. transfer relation: " + envTransferTimer);
      out.println("time for precision adjustment:   " + precisionTimer);
      out.println("time for env. prec. adjustment:  " + envPrecisionTimer);
      out.println("time for merge operator:         " + mergeTimer);
      out.println("time for env. merge operator:    " + envMergeTimer);
      out.println("time for stop operator:          " + stopTimer);
      out.println("time for env. stop operator:     " + envStopTimer);
      out.println("total time for CPA algorithm:    " + totalTimer);
    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }
  }


}
