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
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.RGCFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGVariables;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvTransitionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.rg")
public class RGAlgorithm implements ConcurrentAlgorithm, StatisticsProvider{

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(description="Combine all valid environmental edges for thread in two one edge?")
  boolean combineEnvEdges = false;

  public final Stats stats;

  // TODO option for CFA export
  private int threadNo;
  private RGCFA[] cfas;
  private CFAFunctionDefinitionNode[] mainFunctions;
  private ConfigurableProgramAnalysis[] cpas;
  private LogManager logger;


  // CPAAlgorithm for each thread
  private RGThreadCPAAlgorithm[] threadCPA;
  // data structure for deciding whether a variable is global

  // stores information about environmental transitions
  private RGEnvironmentManager environment;
  // data on variables
  public RGVariables variables;


  public RGAlgorithm(RGCFA[] cfas, CFAFunctionDefinitionNode[] pMainFunctions, ConfigurableProgramAnalysis[] pCpas, RGEnvironmentManager environment, RGVariables vars, Configuration config, LogManager logger) {
    this.cfas = cfas;
    this.threadNo = cfas.length;
    this.mainFunctions = pMainFunctions;
    this.variables = vars;
    this.cpas = pCpas;
    this.logger = logger;
    this.stats = new Stats();

    threadCPA = new RGThreadCPAAlgorithm[threadNo];

    this.environment = environment;
    try {
      //environment = new RGEnvironmentManager(threadNo, vars, config, logger);
      config.inject(this, RGAlgorithm.class);
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }

    for (int i=0; i< this.threadNo; i++){
      threadCPA[i] = new RGThreadCPAAlgorithm(cpas[i],cfas[i], environment,config, logger, i);
    }

    // create DOT file for the original CFA
    for (int i=0; i< this.threadNo; i++){
      dumpDot(i, "test/output/oldCFA"+i+".dot");
    }
  }

  /**
   * Returns -1 if no error is found or the thread no with an error
   */
  @Override
  public int run(ReachedSet[] reached, int startThread) {
    assert environment.getUnprocessedTransitions().isEmpty();


    for (int i=0; i<reached.length; i++){
      ReachedSet rs = reached[i];
      ARTElement aElement = (ARTElement) rs.getFirstElement();
      aElement.setHasLocalChild(false);
    }

    boolean error = false;

    try{
      int i = startThread;
      while(i != -1 && !error) {

        addEnvTransitions(i, reached);

        error = runThread(i, reached[i]);
        if (error) {
          return i;
        }

        processEnvironment(i);

        i = pickThread(reached, i);
      }
    } catch(Exception e){
      e.printStackTrace();
    }

    return -1;
  }

  /**
   * Generate environmental transitions from the part of ART that was constructed.
   * @param i
   */
  private void processEnvironment(int i) {
    System.out.println();
    System.out.println("\t\t\t----- Processing Env Transitions -----");
    if (debug){
      environment.printUnprocessedTransitions();
      environment.resetProcessStats();
    }

    environment.processCandidates(i);
    environment.clearCandidates();

    if (debug){
      Statistics prStats = environment.getProcessStats();
      System.out.println();
      System.out.println("\t\t\t----- Environment processing stats -----");
      prStats.printStatistics(System.out, null, null);
    }

  }

  /**
   * Add env. transitions to CFA and read relevant elements to the waitlist.
   * @param i
   * @param reached
   */
  private void addEnvTransitions(int i, ReachedSet[] reached) {
    stats.applyEnvTimer.start();
    ListMultimap<CFANode, CFAEdge> envEdgesMap = addEnvTransitionsToCFA(i);
    // add relevant states to the wait list
    setWaitlist(reached[i], envEdgesMap);
    stats.applyEnvTimer.stop();
  }

  /**
   * Put relevant states into the waitlist
   * @param pEnvEdgesMap
   */
  private void setWaitlist(ReachedSet reachedSet, ListMultimap<CFANode, CFAEdge> pEnvEdgesMap) {
    for (CFANode node : pEnvEdgesMap.keySet()){
      Set<AbstractElement> relevant = reachedSet.getReached(node);
      for (AbstractElement ae : relevant){
        if (AbstractElements.extractLocation(ae).equals(node)){
          if (!reachedSet.getWaitlist().contains(relevant)){
            // set the env. edges as the only ones to be applied at this element
            // if there are remaining unnaplied edges, then replace them
            ARTElement artElement = (ARTElement) ae;
            List<CFAEdge> envEdges = artElement.getEnvEdgesToBeApplied();
            if (envEdges == null){
              envEdges = new Vector<CFAEdge>();
              artElement.setEnvEdgesToBeApplied(envEdges);
            }

            envEdges.addAll(pEnvEdgesMap.get(node));
            reachedSet.reAddToWaitlist(ae);
          }
        }
      }
    }
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
    assert environment.getUnprocessedTransitions().isEmpty();

    if (debug){
      threadCPA[i].restRunStats();
    }

    boolean sound = threadCPA[i].run(reached);

    stats.errorCheckTimer.start();
    /* The old way
     * if (reached.hasWaitingElement()){
      stats.errorCheckTimer.stop();
      return true;
    }*/

    // TODO this error check somehow slows down the analysis
    if (debug){
      // print run stats
      Statistics runStats = threadCPA[i].getRunStats();
      System.out.println();
      System.out.println("\t\t\t----- CPA statistics -----");
      runStats.printStatistics(System.out, null, null);
    }


    if (sound) {
      // clear the set of  unapplied env. edges

      environment.clearUnappliedEnvEdgesForThread(i);
      // analyse error found only if the analysis is marked as sound
      ARTElement last = (ARTElement) reached.getLastElement();
      boolean isTarget = last.isTarget();
      stats.errorCheckTimer.stop();
      return isTarget;
    }
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
    while(j < threadNo && reached[j].getWaitlistSize() == 0 && environment.getUnappliedEnvEdgesForThread(j).isEmpty()){
      j++;
    }

    if (j < threadNo){
      return j;
    }

    // check threads in [0, i]
    j=0;
    while(j <= i && reached[j].getWaitlistSize() == 0 && environment.getUnappliedEnvEdgesForThread(j).isEmpty()){
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
      BufferedWriter out = new BufferedWriter(fstream);;
      String s = DOTBuilder.generateDOT(cfas[pI].getFunctions().values(), mainFunctions[pI]);
      out.write(s);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Applies environmental edges to CFA i. Returns a map with env. edges that haven't
   * been applied before. All env. edges :
   * 1) are applied before global reads,
   * 2) are applied before global writes,
   * 3) are not applied where node.isEnvAllowed() is false.
   *
   * @param   i CFA number
   * @return  multimap CFA nodes -> CFA edges
   */
  private ListMultimap<CFANode, CFAEdge> addEnvTransitionsToCFA(int i) {


    ListMultimap<CFANode, CFAEdge> envEdgesMap = null;
    RGCFA cfa = cfas[i];

    // remove old environmental edges
    removeRGEdges(i);

    if (debug){
      dumpDot(i, "test/output/revertedCFA"+i+".dot");
    }

    // sum up all valid edges from other threads
    List<RGEnvTransition> valid = new Vector<RGEnvTransition>();
    for (int j=0; j<threadNo; j++){
      if (j!=i){
        valid.addAll(environment.getValidEnvEdgesFromThread(j));
      }
    }

    if (valid.isEmpty()){
      return ArrayListMultimap.create();
    }

    // check where to apply env. edges
    Set<CFANode> toApply = new HashSet<CFANode>();

    // apply all env transitions to CFA nodes that have an outgoing edge that reads from or writes to a global variable
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
          toApply.add(node);
          break;
        }
      }
    }

    // list of env. edges that haven't been applied before
    List<RGEnvTransition> unapplied = environment.getUnappliedEnvEdgesForThread(i);

    if(combineEnvEdges){
      // valid env. edges are merged into one edge
      envEdgesMap = null;
          //addCombinedEnvTransitionsToCFA(i, toApply, valid, unapplied);
    } else {
      // valid env. edges are applied separately
      envEdgesMap = addSeparateEnvTransitionsToCFA(i, toApply, valid, unapplied);
    }

    if (debug){
      dumpDot(i, "test/output/newCFA"+i+".dot");
    }

    return envEdgesMap;
  }


  /**
   * Combine environmental edges from valid and apply them to the nodes of CFA i, which belong to toApply.
   * Returns Returns a map with env. edges that haven't been applied before.
   *
   * @param i
   * @param toApply
   * @param pValid
   * @param pUnapplied
   * @return
   */
 /* private ListMultimap<CFANode, CFAEdge> addCombinedEnvTransitionsToCFA(int i, Set<CFANode> toApply, List<RGEnvTransition> pValid, List<RGEnvTransition> pUnapplied) {
    // TODO finish
    RelyGuaranteeCFA cfa = cfas[i];
    ListMultimap<CFANode, CFAEdge> envEdgesMap = ArrayListMultimap.create();

    // someUnapplied  iff some env. edges hasn't been applied before
    boolean someUnapplied = !pUnapplied.isEmpty();

    if (debug){
      System.out.println();
      System.out.println("Env edges applied at CFA "+i+" :");
    }

    // apply env. edges
    for(Entry<String, CFANode> entry :   cfa.getCFANodes().entries()){
      CFANode node = entry.getValue();

      if (toApply.contains(node)){
        // combine all valid edges into one
        RGCombinedCFAEdge combined = null;
            //new RGCFAEdge(pValid, node, node);
        addEnvTransitionToNode(combined);

        if (debug){
          System.out.println("\t-node "+node+" applied "+combined);
        }

        if (someUnapplied){
          envEdgesMap.put(node, combined);
        }
      }
    }

    return envEdgesMap;
  }*/


  /**
   * Apply valid environmental edges to the nodes of CFA i, which belong to toApply.
   * Returns Returns a map with env. edges that haven't been applied before.
   *
   * @param i
   * @param toApply
   * @param pValid
   * @param pUnapplied
   * @return
   */
  private ListMultimap<CFANode, CFAEdge> addSeparateEnvTransitionsToCFA(int i, Set<CFANode> toApply, List<RGEnvTransition> pValid, List<RGEnvTransition> pUnapplied) {

    RGCFA cfa = cfas[i];
    ListMultimap<CFANode, CFAEdge> envEdgesMap = ArrayListMultimap.create();

    // change unapplied to set, so its easier to decided membership
    Set<RGEnvTransition> unappliedSet = new HashSet<RGEnvTransition>(pUnapplied);

    if (debug){
      System.out.println();
      System.out.println("Env edges applied at CFA "+i+" :");
    }

    // apply env. edges
    for(Entry<String, CFANode> entry :   cfa.getCFANodes().entries()){
      CFANode node = entry.getValue();

      if (toApply.contains(node)){
        // apply edges

        for (RGEnvTransition edge : pValid){
          RGCFAEdge rgEdge = new RGCFAEdge(edge, node, node);
          addEnvTransitionToNode(rgEdge);

          if (debug){
            System.out.println("\t-node "+node+" applied "+rgEdge);
          }

          if (unappliedSet.contains(edge)){
            envEdgesMap.put(node, rgEdge);
          }
        }
      }
    }

    return envEdgesMap;
  }


  /**
   * Remove old environmental edges.
   */
  private void removeRGEdges(int i) {
    RGCFA cfa = this.cfas[i];
    List<CFAEdge> toRemove = new Vector<CFAEdge>();
    // remove old env edges from the CFA
    for (CFANode node : cfa.getCFANodes().values()){
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

    private final Timer applyEnvTimer = new Timer();
    private final Timer errorCheckTimer = new Timer();

    @Override
    public String getName() {
      return "RGAlgorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      out.println("time for error check:            " + errorCheckTimer);
      out.println("time for setting env. trans.:    " + applyEnvTimer);
    }
  }

}
