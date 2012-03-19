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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ThreadCFA;
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
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTPrecision;
import org.sosy_lab.cpachecker.cpa.art.ARTTransferRelation;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationClass;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGPrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGTransferRelation;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

@Options(prefix="cpa.rg")
public class RGThreadCPAAlgorithm implements Algorithm, StatisticsProvider {

  @Option(description="Print debugging info?")
  private boolean debug = false;

  @Option(description="If true, then change treads after a number of operations.")
  private boolean changeThread = false;

  public final Stats stats;
  private RunStats runStats;
  private final ConfigurableProgramAnalysis cpa;
  private final ThreadCFA            cfa;
  private final LogManager                  logger;
  private RGEnvironmentManager environment;
  private final int tid;
  private final int threadNo;
  private ImmutableSet<CFANode> applyEnv;

  private List<Pair<AbstractElement, Precision>> forcedStop = new Vector<Pair<AbstractElement, Precision>>();

  /** Unprocessed candidates for env transitions. */
  private final List<RGEnvCandidate> candidates;
  /** Candidates for env. transitions from all threads */
  private List<RGEnvCandidate>[] candidatesFromThread;



  public RGThreadCPAAlgorithm(ConfigurableProgramAnalysis  cpa, ThreadCFA cfa, RGEnvironmentManager environment, ImmutableSet<CFANode> applyEnv, List<RGEnvCandidate>[] candidatesFromThread, Configuration config, LogManager logger,  int tid, int threadNo) {
    this.cpa = cpa;
    this.cfa = cfa;
    this.environment = environment;
    this.logger = logger;
    this.tid = tid;
    this.threadNo = threadNo;


    this.stats = new Stats((ARTCPA) cpa, tid);

    try {
      config.inject(this, RGThreadCPAAlgorithm.class);
    } catch (InvalidConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // check where to apply env. edges
    this.applyEnv = applyEnv;

    this.candidates = new Vector<RGEnvCandidate>();

    this.candidatesFromThread = candidatesFromThread;

  }

  @Override
  public boolean run(final ReachedSet reachedSet) throws CPAException, InterruptedException {

    if (runStats == null){
      runStats = new RunStats();
    }
    stats.totalTimer.start();
    runStats.totalTimer.start();

    //assert candidates.isEmpty();

    final TransferRelation transferRelation = cpa.getTransferRelation();
    final MergeOperator mergeOperator = cpa.getMergeOperator();
    final StopOperator stopOperator = cpa.getStopOperator();
    final PrecisionAdjustment precisionAdjustment = cpa.getPrecisionAdjustment();

    int stepCount = 0;

    //List<RGEnvTransition> newEnvEdges = environment.getUnappliedEnvEdgesForThread(tid);


    assert forcedStop.isEmpty();

    while (reachedSet.hasWaitingElement()) {
      CPAchecker.stopIfNecessary();

      stats.countIterations++;
      runStats.countIterations++;
      stepCount++;

      // Pick next element using strategy
      // BFS, DFS or top sort according to the configuration
      int size = reachedSet.getWaitlistSize();
      stats.maxWaitlistSize = Math.max(stats.maxWaitlistSize, size);
      stats.countWaitlistSize += size;

      final AbstractElement element =  reachedSet.popFromWaitlist();
      final Precision precision = reachedSet.getPrecision(element);

      ARTElement aElement = (ARTElement) element;
      if (!(precision instanceof ARTPrecision)){
        System.out.println();
      }
      ARTPrecision artPrec = (ARTPrecision) precision;
      RGPrecision rgPrec = Precisions.extractPrecisionByType(precision, RGPrecision.class);
      CFANode loc = aElement.retrieveLocationElement().getLocationNode();

      if (debug){
        System.out.println("Successors of "+aElement+", "+rgPrec);
      }

      /*if (aElement.getElementId() == 128){
        System.out.println(this.getClass());
      }*/



      Collection<CFAEdge> edges = getEdgesForElement(aElement, artPrec);

      if (debug && edges.isEmpty()){
        System.out.println();
      }


      stats.transferTimer.start();
      runStats.transferTimer.start();
      Collection<Pair<AbstractElement,CFAEdge>> successors = new LinkedHashSet<Pair<AbstractElement,CFAEdge>>(edges.size());

      for (CFAEdge edge : edges){
        Collection<? extends AbstractElement> newSucc = transferRelation.getAbstractSuccessors(element, precision, edge);

        if (newSucc.isEmpty()){
          this.printSuccessor(null, edge, null);
        }

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
          byEnvEdge = true;// TODO Auto-generated method stub
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

        Triple<AbstractElement, Precision, Action> precAdjustmentResult = precisionAdjustment.prec(successor, precision, reachedSet);
        successor = precAdjustmentResult.getFirst();
        Precision successorPrecision = precAdjustmentResult.getSecond();
        Action action = precAdjustmentResult.getThird();

        stats.precisionTimer.stop();
        runStats.precisionTimer.stop();
        stats.envPrecisionTimer.stop();

        if (debug){
          printSuccessor(successor, edge, successorPrecision);
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

        /* Remember the transition element--edge-->successor as candidate for an env. transition. */
        stats.envGenTimer.start();
        if (createsEnvTransition(edge)){
          RGEnvCandidate candidate = generateCandidate(element, successor, precision, edge);
          candidates.add(candidate);
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
            printCovered(successor, reached);
          }
          stats.countStop++;
          if (byEnvEdge){
            stats.countEnvStop++;
          }

        } else {
          logger.log(Level.FINER,
          "No need to stop, adding successor to waitlist");

          if (debug){
            System.out.println();
          }

          if (((ARTElement) successor).getElementId() == 117){
            System.out.println(this.getClass());
          }

          reachedSet.add(successor, successorPrecision);
          // if changeThread is true, then we stop expanding local successor

        }

      }

      if (changeThread && stepCount > 5){
        return false;
      }
    }

    stats.totalTimer.stop();
    runStats.totalTimer.stop();
    return true;
    /*if (changeThread && !forcedStop.isEmpty()){
      return false;
    } else {
      return true;
    }*/

  }

  private RGEnvCandidate generateCandidate(AbstractElement pElement,
      AbstractElement successor, Precision pPrecision, CFAEdge edge) {

    ARTElement element = (ARTElement) pElement;
    ImmutableMap<Integer, RGLocationClass> locClasses = element.getLocationClasses();
    SetMultimap<Integer, CFANode> concreateLocs = LinkedHashMultimap.create();

    for (int i=0; i<threadNo; i++){

      if (i == tid){
        CFANode loc = element.retrieveLocationElement().getLocationNode();
        concreateLocs.put(i, loc);
      } else {
        RGLocationClass locClass = locClasses.get(i);
        assert locClass != null;
        concreateLocs.putAll(i, locClass.getClassNodes());
      }
    }

    RGEnvCandidate candidate = new RGEnvCandidate(element, (ARTElement)successor, edge, concreateLocs, tid);
    return candidate;
  }

  /**
   * Returns local and environmental edges to be applied at the element.
   * @param aElem
   * @param artPrec
   * @return
   */
  private List<CFAEdge> getEdgesForElement(ARTElement aElem, ARTPrecision artPrec) {
    assert aElem != null;
    assert artPrec != null;

    List<CFAEdge> edges = new Vector<CFAEdge>();
    CFANode loc = aElem.retrieveLocationElement().getLocationNode();

    /* get environmental edges */
    if (applyEnv.contains(loc)){

      // sum up candidates from other threads
      List<RGEnvCandidate> sum = new Vector<RGEnvCandidate>();

      for (int j=0; j<threadNo; j++){

        if (j != tid){
          sum.addAll(this.candidatesFromThread[j]);
        }
      }

      List<RGEnvTransition> envTransitionToApply = environment.getEnvironmentalTransitionsToApply(aElem, sum, artPrec);

      for (RGEnvTransition et : envTransitionToApply){
          RGCFAEdge rgEdge = new RGCFAEdge(et, loc, loc);
          edges.add(rgEdge);
      }
    }

    /* get local edges */
    if (!aElem.hasLocalChild()){

      for (int i=0; i<loc.getNumLeavingEdges(); i++){
        CFAEdge edge = loc.getLeavingEdge(i);
        if (edge.getEdgeType() != CFAEdgeType.RelyGuaranteeCFAEdge){
          edges.add(edge);
        }
      }
    }


    return edges;
  }


  private void printCovered(AbstractElement pSuccessor, Collection<AbstractElement> reached) {
    System.out.println("\t covered");
    System.out.println("");
  }

  private void printMerge(AbstractElement successor, AbstractElement reachedElement, AbstractElement mergedElement) {
    System.out.println("\t merged with "+reachedElement);
    System.out.println("\t to "+mergedElement);
  }

  // pretty-printing of successors
  private void printSuccessor(AbstractElement pSuccessor, CFAEdge edge, Precision prec) {
    System.out.println("\t-by edge "+edge.getRawStatement()+":");

    if (pSuccessor == null){
      System.out.println("\t none");
      System.out.println();
    } else {
      RGPrecision rgPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
      System.out.println("\t "+pSuccessor);
    }
  }


  public List<Pair<AbstractElement, Precision>> getForcedStop() {
    return forcedStop;
  }

  // returns true if an environmental transition should be created by the edge
  private boolean createsEnvTransition(CFAEdge edge){
    if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
      return false;
    }

    CFANode node = edge.getPredecessor();

    if (cfa.getExecNodes().contains(node)){
      return true;
    } else {
      return false;
    }

  }



  public List<RGEnvCandidate> getCandidates() {
    return candidates;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return cpa;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    ARTCPA artCPA = (ARTCPA) cpa;
    pStatsCollection.add(stats);
    ARTTransferRelation artTr = (ARTTransferRelation) cpa.getTransferRelation();
    artTr.collectStatistics(pStatsCollection);
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

      out.println("number of iterations "+tid+" :         " + formatInt(countIterations));
      out.println("max size of waitlist "+tid+" :         " + formatInt(maxWaitlistSize));
      out.println("average size of waitlist "+tid+" :     " + formatInt(countWaitlistSize/ countIterations));
      out.println();
      out.println("no of environmental succ "+tid+" :     " + formatInt(countEnvSuccessors));
      out.println("no of all successors "+tid+" :         " + formatInt(countSuccessors));
      out.println("max successors for one element:  " + formatInt(maxSuccessors));
      out.println("number of environmetal merges "+tid+" :" + formatInt(countEnvMerge));
      out.println("number of all merges "+tid+" :         " + formatInt(countMerge));
      out.println("number of environmetal stops "+tid+" : " + formatInt(countEnvStop));
      out.println("number of all stops "+tid+" :          " + formatInt(countStop));
      out.println("time for generating env. trans. "+tid    + envGenTimer);
      out.println("time for transfer relation "+tid+" :   " + transferTimer);
      out.println("time for transfer form. constr.: " + tr.pfConstructionTimer);
      out.println("time for env. transfer relation: " + envTransferTimer);
      out.println("time for precision adjustment "+tid+" :" + precisionTimer);
      out.println("time for env. prec. adjustment:  " + envPrecisionTimer);
      out.println("time for merge operator "+tid+" :      " + mergeTimer);
      out.println("time for env. merge operator "+tid+" : " + envMergeTimer);
      out.println("time for stop operator "+tid+" :       " + stopTimer);
      out.println("time for env. stop operator "+tid+" :  " + envStopTimer);
      out.println("total time for CPA algorithm "+tid+" : " + totalTimer);
    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }
  }


}
