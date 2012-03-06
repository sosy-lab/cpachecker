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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.ParallelCFAS;
import org.sosy_lab.cpachecker.cfa.ThreadCFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvTransitionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;

@Options(prefix="cpa.rg")
public class RGAlgorithm implements ConcurrentAlgorithm, StatisticsProvider{

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(description="Combine all valid environmental edges for thread in two one edge?")
  boolean combineEnvEdges = false;

  @Option(toUppercase=true, values={"ALL", "GLOBAL"},
      description="Where to apply env. edges:\n"  +
      		"ALL - to every non-atomic location,\n" +
          "GLOBAL - to non-atomic locations that after global writes, reads or are the inital state.")
  private String applyEnvEdges =  "GLOBAL";


  private final int threadNo;
  private final ParallelCFAS pcfa;
  private final ConfigurableProgramAnalysis[] cpas;
  private final LogManager logger;

  /** manager for env. transitions */
  private final RGEnvironmentManager environment;

  /** CPAAlgorithm for each thread */
  private final RGThreadCPAAlgorithm[] threadCPA;
  /** Where to apply env. transitionas */
  private final ImmutableSet<CFANode>[] applyEnv;
  /** Candidates from ARTs */
  private final List<RGEnvCandidate>[] allCandidatesFrom;
  public final Stats stats;



  public RGAlgorithm(ParallelCFAS pcfa, ConfigurableProgramAnalysis[] pCpas, RGEnvironmentManager environment, Configuration config, LogManager logger) {
    this.pcfa = pcfa;
    this.threadNo = pcfa.getThreadNo();
    this.cpas = pCpas;
    this.logger = logger;
    this.stats = new Stats(threadNo);

    this.threadCPA = new RGThreadCPAAlgorithm[threadNo];
    this.applyEnv = new ImmutableSet[threadNo];

    this.environment = environment;
    try {
      config.inject(this, RGAlgorithm.class);
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }

    this.allCandidatesFrom = new Vector[threadNo];

    for (int i=0; i< this.threadNo; i++){
      this.applyEnv[i] = getNodesForEnvApplication(pcfa.getCfa(i));
      this.allCandidatesFrom[i] = new Vector<RGEnvCandidate>();
      this.threadCPA[i] = new RGThreadCPAAlgorithm(cpas[i], pcfa.getCfa(i), environment, applyEnv[i], allCandidatesFrom, config, logger, i, threadNo);
    }
  }


  /**
   * Returns -1 if no error is found or the thread no with an error
   */
  @Override
  public int run(ReachedSet[] reached, int startThread) {

    boolean error = false;

    try{
      int i = startThread;
      while(i != -1 && !error) {

        int otherTid = i == 0 ? 1 : 0;
        this.printTransitions("Transition from "+otherTid, allCandidatesFrom[otherTid]);

        // construct ART for thread i
        error = runThread(i, reached[i]);
        if (error) {
          return i;
        }

        /* find the most general candidates from the thread */
        List<RGEnvCandidate> newCandidates = threadCPA[i].getCandidates();
        boolean foundNewCand = !allCandidatesFrom[i].containsAll(newCandidates);
        allCandidatesFrom[i].addAll(newCandidates);

        stats.allCandidates += newCandidates.size();
        stats.maxValidCandidates[i] = Math.max(stats.maxValidCandidates[i], allCandidatesFrom[i].size());

        threadCPA[i].getCandidates().clear();


        /* find new candidates found, then re-add to waitlist elements in other threads */
        if (foundNewCand){
          for (int j=0; j<threadNo; j++){
            if (j!=i){
              setWaitlist(reached[j], applyEnv[j]);
            }
          }
        }


        i = pickThread(reached, i);
      }
    } catch(Exception e){
      e.printStackTrace();
    }

    return -1;
  }


  /*
   * Finds new list of most general candidates and returns true if the list has grown.
   * @param newCandidates
   * @param mgCandidates
   * @return

  private Pair<List<RGEnvCandidate>, Boolean> processCandidates(List<RGEnvCandidate> newCandidates, List<RGEnvCandidate> mgCandidates) {
    stats.allCandidates += newCandidates.size();

    List<RGEnvCandidate> newMG = environment.findMostGeneralCandidates(mgCandidates, newCandidates);
    boolean foundNew = !mgCandidates.containsAll(newMG);

    return Pair.of(newMG, foundNew);
  }*/


  /**
   * Generate environmental transitions from the part of ART that was constructed.
   * @param i
   */
 /* private boolean processEnvironment(int i) {
    System.out.println();
    System.out.println("\t\t\t----- Processing Env Transitions -----");
    if (debug){
      environment.printUnprocessedTransitions(threadCPA[i].getCandidates());
      environment.resetProcessStats();
    }


    List<RGEnvTransition> newValid = environment.processCandidates(i, threadCPA[i].getCandidates(), validEnvEdgesFromThread[i]);
    List<RGEnvTransition> deltaValid = new Vector<RGEnvTransition>(newValid);
    deltaValid.removeAll(validEnvEdgesFromThread[i]);
    validEnvEdgesFromThread[i] = newValid;

    threadCPA[i].getCandidates().clear();

    if (debug){
      Statistics prStats = environment.getProcessStats();
      System.out.println();
      System.out.println("\t\t\t----- Environment processing stats -----");
      prStats.printStatistics(System.out, null, null);
    }

    return !deltaValid.isEmpty();
  }*/

  /**
   * Add env. transitions to CFA and read relevant elements to the waitlist.
   * @param i
   * @param reached
   */
  /*private void addEnvTransitions(int i, ReachedSet[] reached) {
    stats.applyEnvTimer.start();
    ListMultimap<CFANode, CFAEdge> envEdgesMap = addEnvTransitionsToCFA(i);
    // add relevant states to the wait list
    setWaitlist(reached[i], envEdgesMap);
    stats.applyEnvTimer.stop();
  }*/

  /**
   * Put relevant states into the waitlist
   * @param pEnvEdgesMap
   */
  private void setWaitlist(ReachedSet reachedSet, Set<CFANode> toApply) {
    stats.waitlistTimer.start();
    for (CFANode node : toApply){
      Set<AbstractElement> relevant = reachedSet.getReached(node);

      for (AbstractElement ae : relevant){

        if (AbstractElements.extractLocation(ae).equals(node)){
          if (!reachedSet.getWaitlist().contains(relevant)){
            reachedSet.reAddToWaitlist(ae);
          }
        }
      }
    }
    stats.waitlistTimer.stop();
  }


  /**
   * Runs thread i. Returns true iff error was found.
   * @param i
   * @param reached
   * @param stats
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  private boolean runThread(int i, ReachedSet reached) throws CPAException, InterruptedException {

    System.out.println();
    System.out.println("\t\t\t----- Running thread "+i+" -----");

    //assert threadCPA[i].getCandidates().isEmpty();

    if (debug){
      threadCPA[i].restRunStats();
    }

    boolean sound = threadCPA[i].run(reached);

    stats.errorCheckTimer.start();
    // TODO this error check somehow slows down the analysis
    if (debug){
      // print run stats
      Statistics runStats = threadCPA[i].getRunStats();
      System.out.println();
      System.out.println("\t\t\t----- CPA statistics -----");
      runStats.printStatistics(System.out, null, null);
    }

    if (sound) {
      // analyse error found only if the analysis is marked as sound
      ARTElement last = (ARTElement) reached.getLastElement();
      // if error is found then last is not null
      boolean isTarget = last != null && last.isTarget();
      assert isTarget || threadCPA[i].getForcedStop().isEmpty();
      stats.errorCheckTimer.stop();
      threadCPA[i].getForcedStop().clear();
      return isTarget;
    }

    // readd to waitlist elements that were forced to stop
    for (Pair<AbstractElement, Precision> pair : threadCPA[i].getForcedStop()){
      AbstractElement element = pair.getFirst();
      Precision precision = pair.getSecond();
      reached.add(element, precision);
    }
    threadCPA[i].getForcedStop().clear();

    stats.errorCheckTimer.stop();
    return false;
  }

  /**
   * Choose the next thread for running.
   * Return -1 if there are no new env edges or waiting elements for any thread
   * @param reached
   * @return
   */
  private int pickThread(ReachedSet[] reached, int i) {

    // check threads in [i+1, threadNo-1]
    int j=i+1;
    while(j < threadNo && reached[j].getWaitlistSize() == 0){
      j++;
    }

    if (j < threadNo){
      return j;
    }

    // check threads in [0, i]
    j=0;
    while(j <= i && reached[j].getWaitlistSize() == 0){
      j++;
    }

    if (j <= i){
      return j;
    }

    return -1;
  }

  /**
   * Writes a DOT file for CFA no. pI.
   */
  private void dumpDot(int pI, String file) {
    FileWriter fstream;
    try {
      fstream = new FileWriter(file);
      BufferedWriter out = new BufferedWriter(fstream);
      ThreadCFA cfa = pcfa.getCfa(pI);
      String s = DOTBuilder.generateDOT(cfa.getFunctions().values(), cfa.getInitalNode());
      out.write(s);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private void printTransitions(String string, Collection<RGEnvCandidate> pUnprocessedTransitions) {
    System.out.println();
    if (pUnprocessedTransitions.isEmpty()){
      System.out.println(string+"\tnone");
    } else {
      System.out.println(string);
    }
    for (RGEnvCandidate tran : pUnprocessedTransitions){
      System.out.println("\t-"+tran);
    }
  }


  /**
   * Get set of nodes, where env. transitions can be applied.
   * @param cfa
   * @return
   */
  private ImmutableSet<CFANode> getNodesForEnvApplication(ThreadCFA cfa) {
    Set<CFANode> toApply = new HashSet<CFANode>();

    boolean global = applyEnvEdges.equals("GLOBAL");

    // apply all env transitions to CFA nodes that have an outgoing edge that reads from or writes to a global variable
    for (CFANode node : cfa.getExecNodes()){

      if (cfa.getAtomic().contains(node)){
        continue;
      }

      if (global){
        // apply at the exeuctution start and after global reads and writes
        if (cfa.getExecutionStart().equals(node)){
          toApply.add(node);
        }

        for (int j=0; j<node.getNumEnteringEdges(); j++){
          CFAEdge edge = node.getEnteringEdge(j);

          if (edge.isGlobalRead() || edge.isGlobalWrite()){
            toApply.add(node);
          }
        }
      }
      else {
        toApply.add(node);
      }
    }
    return ImmutableSet.copyOf(toApply);
  }

  /**
   * Removes all candidates.
   */
  public void resetEnvironment(){
    for (int i=0; i<threadNo; i++){
      this.allCandidatesFrom[i].clear();
      threadCPA[i].getCandidates().clear();
    }
  }

  /**
   * Removes destroyed candidates.
   */
  public void removeDestroyedCandidates() {
    for (int i=0; i<threadNo; i++){
      List<RGEnvCandidate> toRemove = new Vector<RGEnvCandidate>();

      for (RGEnvCandidate cand : allCandidatesFrom[i]){
        if (cand.getElement().isDestroyed()){
          toRemove.add(cand);
        }
      }
      allCandidatesFrom[i].removeAll(toRemove);

      toRemove.clear();
      for (RGEnvCandidate cand : threadCPA[i].getCandidates()){
        if (cand.getElement().isDestroyed()){
          toRemove.add(cand);
        }
      }
      threadCPA[i].getCandidates().removeAll(toRemove);
    }
  }


  /**
   * Apply valid environmental edges to the nodes of CFA i, which belong to toApply.
   * Returns a map with env. edges that haven't been applied before.
   *
   * @param i
   * @param toApply
   * @param pValid
   * @param pUnapplied
   * @return
   */
  private ListMultimap<CFANode, CFAEdge> addSeparateEnvTransitionsToCFA(int i, Set<CFANode> toApply, List<RGEnvTransition> pValid) {

    ThreadCFA cfa = pcfa.getCfa(i);
    ListMultimap<CFANode, CFAEdge> envEdgesMap = ArrayListMultimap.create();

    // apply env. edges
    for(CFANode node : toApply){
      for (RGEnvTransition edge : pValid){
        RGCFAEdge rgEdge = new RGCFAEdge(edge, node, node);
        //addEnvTransitionToNode(rgEdge);

        envEdgesMap.put(node, rgEdge);
      }
    }

    return envEdgesMap;
  }


  /**
   * Remove old environmental edges.
   * @param applyEnv
   */
  private void removeRGEdges(int i, ImmutableSet<CFANode> applyEnv) {
    ThreadCFA cfa = pcfa.getCfa(i);
    List<CFAEdge> toRemove = new Vector<CFAEdge>();
    // remove old env edges from the CFA

    for (CFANode node : applyEnv){
      for (int j=0; j<node.getNumLeavingEdges(); j++) {
        CFAEdge edge = node.getLeavingEdge(j);
        if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge || edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCombinedCFAEdge){
          toRemove.add(edge);
        }
      }
      for (CFAEdge edge : toRemove){
        node.removeLeavingEdge(edge);
        CFANode successor = edge.getSuccessor();
        successor.removeEnteringEdge(edge);
      }
      toRemove.clear();
    }

  }

  /**
   * Add the env transition to the CFA node
   */
  private void addEnvTransitionToNode(CFAEdge edge) {
    edge.getPredecessor().addLeavingEdge(edge);
    edge.getSuccessor().addEnteringEdge(edge);;
  }

  @Override
  public ConfigurableProgramAnalysis[] getCPAs() {
    return cpas;
  }

  @Override
  public Result getResult() {
    return null;
  }

  public RGEnvironmentManager getRelyGuaranteeEnvironment() {
    return environment;
  }

  @Override
  public void collectStatistics(Collection<Statistics> scoll) {
    ARTCPA artCPA = (ARTCPA) this.cpas[0];
    RGCPA rgCPA = artCPA.retrieveWrappedCpa(RGCPA.class);

    // RGThreadCPAAlgorithm
    for (int t=0; t<threadNo; t++){
      threadCPA[t].collectStatistics(scoll);
    }

    // RGEnvironmentManager
    environment.collectStatistics(scoll);

    // RGEnvTransitionManager
    RGEnvTransitionManager etManager = rgCPA.getEtManager();
    if (etManager instanceof StatisticsProvider){
      StatisticsProvider sp = (StatisticsProvider) etManager;
      sp.collectStatistics(scoll);
    }

    // PredicateAbstractionManager
    RGAbstractionManager absManager = rgCPA.getAbstractionManager();
    absManager.collectStatistics(scoll);

    // CachingPathFormulaManager
    PathFormulaManager pfManager = rgCPA.getPathFormulaManager();
    if (pfManager instanceof StatisticsProvider){
      StatisticsProvider sp = (StatisticsProvider) pfManager;
      sp.collectStatistics(scoll);
    }

    // MathsatFormulaManager
    FormulaManager fManager = rgCPA.getFormulaManager();
    if (fManager instanceof StatisticsProvider){
      StatisticsProvider sp = (StatisticsProvider) fManager;
      sp.collectStatistics(scoll);
    }

    // SSAMapManager
    SSAMapManager ssaManager = rgCPA.getSsaManager();
    ssaManager.collectStatistics(scoll);


    // RGAlgorithm
    scoll.add(stats);
  }


  public static class Stats implements Statistics {


    private final Timer waitlistTimer = new Timer();
    private final Timer errorCheckTimer = new Timer();
    private int allCandidates = 0;
    private final int[] maxValidCandidates;

    public Stats(int threadNo){
      maxValidCandidates = new int[threadNo];
    }

    @Override
    public String getName() {
      return "RGAlgorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      out.println("time on error check:             " + errorCheckTimer);
      out.println("time on re-adding to waitlist:   " + waitlistTimer);
      out.println("all candidates generated:        " + formatInt(allCandidates));
      for (int i=0; i<maxValidCandidates.length; i++){
        out.println("max valid candidates from "+i+":     " + formatInt(maxValidCandidates[i]));
      }
    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }
  }




}
