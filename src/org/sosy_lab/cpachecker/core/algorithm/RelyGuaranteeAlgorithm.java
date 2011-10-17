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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.RelyGuaranteeCFA;
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
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeCFAEdgeTemplate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeCombinedCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeEnvironment;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeVariables;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.relyguarantee")
public class RelyGuaranteeAlgorithm implements ConcurrentAlgorithm, StatisticsProvider{

  @Option(description="Print debugging info?")
  private boolean debug=true;

  @Option(description="List of variables global to multiple threads")
  protected String[] globalVariables = {};

  @Option(description="Combine all valid environmental edges for thread in two one edge?")
  boolean combineEnvEdges = true;

  public class RelyGuaranteeAlgorithmStatistics implements Statistics {
    private Timer totalTimer = new Timer();

    @Override
    public String getName() {
      return "Rely-guarantee";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      out.println("Total Time for algorithm:       " + totalTimer);
    }

  }

  private RelyGuaranteeAlgorithmStatistics stats;

  // TODO option for CFA export
  private int threadNo;
  private RelyGuaranteeCFA[] cfas;
  private CFAFunctionDefinitionNode[] mainFunctions;
  private ConfigurableProgramAnalysis[] cpas;
  private LogManager logger;




  // CPAAlgorithm for each thread
  private RelyGuaranteeThreadCPAAlgorithm[] threadCPA;
  // data structure for deciding whether a variable is global
  private Set<String> globalVarsSet;

  // managers
  private PathFormulaManager  pfManager;
  private FormulaManager      fManager;
  private TheoremProver       tProver;
  private RegionManager       rManager    =   BDDRegionManager.getInstance();

  // stores information about environmental transitions
  private RelyGuaranteeEnvironment environment;
  // data on variables
  public RelyGuaranteeVariables variables;


  public RelyGuaranteeAlgorithm(CFA[] pCfas, CFAFunctionDefinitionNode[] pMainFunctions, ConfigurableProgramAnalysis[] pCpas, RelyGuaranteeVariables vars, Configuration config, LogManager logger) {
    this.threadNo = pCfas.length;
    this.mainFunctions = pMainFunctions;
    this.variables = vars;
    this.cpas = pCpas;
    this.logger = logger;

    environment = new RelyGuaranteeEnvironment(threadNo, config, logger);
    threadCPA = new RelyGuaranteeThreadCPAAlgorithm[threadNo];
    cfas = new RelyGuaranteeCFA[threadNo];

    try {
      config.inject(this, RelyGuaranteeAlgorithm.class);
      for (int i=0; i<threadNo; i++) {
        cfas[i] = new RelyGuaranteeCFA(pCfas[i], i);
      }
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    } catch (UnrecognizedCFAEdgeException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // create a set of global variables
    globalVarsSet = new HashSet<String>();
    for (String var : globalVariables) {
      globalVarsSet.add(var);
    }

    assert vars.allVars.containsAll(globalVarsSet);
    assert globalVarsSet.containsAll(vars.allVars);

    for (int i=0; i< this.threadNo; i++){
      threadCPA[i] = new RelyGuaranteeThreadCPAAlgorithm(cpas[i],environment,config, logger, i);
    }

    // create DOT file for the original CFA
    for (int i=0; i< this.threadNo; i++){
      dumpDot(i, "test/output/oldCFA"+i+".dot");
    }
  }

  /**
   * Returns -1 if no error is found or the thread no with an error
   */
  public int run(ReachedSet[] reached, int startThread) {
    assert environment.getUnprocessedTransitions().isEmpty();

    stats = new RelyGuaranteeAlgorithmStatistics();
    stats.totalTimer.start();

    boolean error = false;
    try{
      // run each tread at least once until no new env can be applied to any thread
      int i = startThread;
      while(i != -1 && !error) {

        // apply all valid env. edges to CFA
        ListMultimap<CFANode, CFAEdge> envEdgesMap = addEnvTransitionsToCFA(i);
        // add relevant states to the wait list
        setWaitlist(reached[i], envEdgesMap);
        // run the thread
        System.out.println();
        System.out.println("\t\t\t----- Running thread "+i+" -----");
        assert environment.getUnprocessedTransitions().isEmpty();
        error = runThread(i, reached[i], true);
        // print stats
        threadCPA[i].printStatitics();
        // clear the set of  unapplied env. edges
        environment.clearUnappliedEnvEdgesForThread(i);
        if (error) {
          // error state has been reached
          stats.totalTimer.start();
          return i;
        }
        System.out.println();
        System.out.println("\t\t\t----- Processing Env Transitions -----");
        if (debug){
          environment.printUnprocessedTransitions();
        }

        // process new env. transitions
        environment.processEnvTransitions(i);
        environment.printProcessingStatistics();
        // chose a new thread to run
        i = pickThread(reached);
      }

    } catch(Exception e){
      e.printStackTrace();
    }

    stats.totalTimer.start();
    return -1;

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
            } else {
              envEdges.clear();
            }
            envEdges.addAll(pEnvEdgesMap.get(node));
            reachedSet.reAddToWaitlist(ae);
          }
        }
      }
    }
  }

  // TODO remove stopAfterError
  /**
   * Runs a thread.
   */
  private boolean runThread(int i, ReachedSet reached, boolean stopAfterError) throws CPAException, InterruptedException {
    boolean sound = true;
    do {
      sound &=  threadCPA[i].run(reached);
    } while (!stopAfterError && reached.hasWaitingElement());

    if (reached.hasWaitingElement()) {
      return true;
    }
    return false;
  }

  /**
   * Chose the next thread for running; return -1 if there are no new env edges or waiting elements for any thread
   * @param reached
   */
  private int pickThread(ReachedSet[] reached) {
    int i=0;
    while(i < threadNo && reached[i].getWaitlistSize() == 0 && environment.getUnappliedEnvEdgesForThread(i).isEmpty()){
      i++;
    }
    if (i == threadNo){
      return -1;
    }
    else {
      return i;
    }
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
    RelyGuaranteeCFA cfa = cfas[i];

    // remove old environmental edges
    removeRGEdges(i);

    if (debug){
      dumpDot(i, "test/output/revertedCFA"+i+".dot");
    }

    // sum up all valid edges from other threads
    List<RelyGuaranteeCFAEdgeTemplate> valid = new Vector<RelyGuaranteeCFAEdgeTemplate>();
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
        if (globalVarsSet.contains(var)){
          toApply.add(node);
          break;
        }
      }
    }

    // list of env. edges that haven't been applied before
    List<RelyGuaranteeCFAEdgeTemplate> unapplied = environment.getUnappliedEnvEdgesForThread(i);

    if(combineEnvEdges){
      // valid env. edges are merged into one edge
      envEdgesMap = addCombinedEnvTransitionsToCFA(i, toApply, valid, unapplied);
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
   * @param valid
   * @param unapplied
   * @return
   */
  private ListMultimap<CFANode, CFAEdge> addCombinedEnvTransitionsToCFA(int i, Set<CFANode> toApply, List<RelyGuaranteeCFAEdgeTemplate> valid, List<RelyGuaranteeCFAEdgeTemplate> unapplied) {

    RelyGuaranteeCFA cfa = cfas[i];
    ListMultimap<CFANode, CFAEdge> envEdgesMap = ArrayListMultimap.create();

    // someUnapplied  iff some env. edges hasn't been applied before
    boolean someUnapplied = !unapplied.isEmpty();

    if (debug){
      System.out.println();
      System.out.println("Env edges applied at CFA "+i+" :");
    }

    // apply env. edges
    for(Entry<String, CFANode> entry :   cfa.getCFANodes().entries()){
      CFANode node = entry.getValue();

      if (toApply.contains(node)){
        // combine all valid edges into one
        RelyGuaranteeCombinedCFAEdge combined = new RelyGuaranteeCombinedCFAEdge(valid, node, node);
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
  }


  /**
   * Apply valid environmental edges to the nodes of CFA i, which belong to toApply.
   * Returns Returns a map with env. edges that haven't been applied before.
   *
   * @param i
   * @param toApply
   * @param valid
   * @param unapplied
   * @return
   */
  private ListMultimap<CFANode, CFAEdge> addSeparateEnvTransitionsToCFA(int i, Set<CFANode> toApply, List<RelyGuaranteeCFAEdgeTemplate> valid, List<RelyGuaranteeCFAEdgeTemplate> unapplied) {

    RelyGuaranteeCFA cfa = cfas[i];
    ListMultimap<CFANode, CFAEdge> envEdgesMap = ArrayListMultimap.create();

    // change unapplied to set, so its easier to decided membership
    Set<RelyGuaranteeCFAEdgeTemplate> unappliedSet = new HashSet<RelyGuaranteeCFAEdgeTemplate>(unapplied);

    if (debug){
      System.out.println();
      System.out.println("Env edges applied at CFA "+i+" :");
    }

    // apply env. edges
    for(Entry<String, CFANode> entry :   cfa.getCFANodes().entries()){
      CFANode node = entry.getValue();

      if (toApply.contains(node)){
        // apply edges

        for (RelyGuaranteeCFAEdgeTemplate edge : valid){
          RelyGuaranteeCFAEdge rgEdge = edge.instantiate(node, node);
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
    RelyGuaranteeCFA cfa = this.cfas[i];
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
    // TODO Auto-generated method stub
    return this.cpas;
  }

  @Override
  public Result getResult() {
    // TODO Auto-generated method stub
    return null;
  }

  public RelyGuaranteeEnvironment getRelyGuaranteeEnvironment() {
    return environment;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  public void printStatitics(){
    System.out.println();
    System.out.println("RG algorithm statistics:");
    if (stats != null){
      stats.printStatistics(System.out, null, null);
    }
  }
  // for testing 'isCovered' method, commented cases are for completness
  /*private boolean isCoveredTest() {
    Formula g1 = fManager.makeVariable("g", 1);
    Formula g2 = fManager.makeVariable("g", 2);
    Formula g3 = fManager.makeVariable("g", 3);
    Formula t = fManager.makeTrue();
    Formula f = fManager.makeFalse();
    Formula r2 = fManager.makeVariable("r", 2);
    Formula n0 = fManager.makeNumber(0);
    Formula n1 = fManager.makeNumber(1);
    Formula n2 = fManager.makeNumber(2);
    Formula g2_1 = fManager.makeEqual(g1, n1);
    Formula g2_2 = fManager.makeEqual(g2, n2);
    Formula r2_1 = fManager.makeEqual(r2, n1);
    Formula g1geq0 = fManager.makeGeq(g1, n0);
    Formula g1lt0 = fManager.makeLt(g1, n0);
    Formula g2minus1 = fManager.makeMinus(g2, n1);
    Formula g3_g2minus1 = fManager.makeEqual(g3, g2minus1);
    Formula l2 = fManager.makeAnd(fManager.makeOr(g1geq0, g1lt0),r2_1);
    Formula l3 = fManager.makeAnd(r2_1, g1geq0);
    Formula l4 = fManager.makeAnd(g2_2, g3_g2minus1);
    // test I
    CFANode node = new CFANode(0, "f");
    CFAEdge edge = new BlankEdge("test",0,node,node);
    SSAMapBuilder ssa1_I = SSAMap.emptySSAMap().builder();
    ssa1_I.setIndex("g", 1);
    PathFormula pf1_I = new PathFormula(g2_1,ssa1_I.build(),0);
    PathFormula pf2_It = new PathFormula(t,SSAMap.emptySSAMap(),0);
    PathFormula pf2_If = new PathFormula(f,SSAMap.emptySSAMap(),0);
    RelyGuaranteeCFAEdge ef1_I = new RelyGuaranteeCFAEdge(edge, pf1_I, 0);
    RelyGuaranteeCFAEdge ef2_It = new RelyGuaranteeCFAEdge(edge, pf2_It, 0);
    RelyGuaranteeCFAEdge ef2_If = new RelyGuaranteeCFAEdge(edge, pf2_If, 0);
    if (!isCovered(ef1_I, ef2_It)){
      return false;
    }
    if (isCovered(ef1_I, ef2_If)){
      return false;
    }

    // test III
    RelyGuaranteeCFAEdge ef1_III = ef1_I;
    SSAMapBuilder ssa2_IIIt = SSAMap.emptySSAMap().builder();
    ssa2_IIIt.setIndex("g", 2);
    PathFormula pf2_IIIt = new PathFormula(l4, ssa2_IIIt.build(), 0);
    PathFormula pf2_IIIf = new PathFormula(g2_2, ssa1_I.build(), 0);
    RelyGuaranteeCFAEdge ef2_IIIt = new RelyGuaranteeCFAEdge(edge, pf2_IIIt, 0);
    RelyGuaranteeCFAEdge ef2_IIIf = new RelyGuaranteeCFAEdge(edge, pf2_IIIf, 0);
   /* if (!isCovered(ef1_III, ef2_IIIt)){
      return false;
    }*/
  /*   if (isCovered(ef1_III, ef2_IIIf)){
      return false;
    }
    // test II
    SSAMapBuilder ssa1_II = SSAMap.emptySSAMap().builder();
    ssa1_II.setIndex("r", 1);
    PathFormula pf1_II = new PathFormula(r2_1,ssa1_II.build(),0);
    SSAMapBuilder ssa2_IIt = SSAMap.emptySSAMap().builder();
    ssa2_IIt.setIndex("r", 1);
    ssa2_IIt.setIndex("g", 2);
    PathFormula pf2_IIt = new PathFormula(l2,ssa2_IIt.build(),0);
    PathFormula pf2_IIf = new PathFormula(l3,ssa2_IIt.build(),0);
    RelyGuaranteeCFAEdge ef1_II = new RelyGuaranteeCFAEdge(edge, pf1_II, 0);
    RelyGuaranteeCFAEdge ef2_IIt = new RelyGuaranteeCFAEdge(edge, pf2_IIt, 0);
    RelyGuaranteeCFAEdge ef2_IIf = new RelyGuaranteeCFAEdge(edge, pf2_IIf, 0);
   /* if (!isCovered(ef1_II, ef2_IIt)){
      return false;
    }*/
  /*   if (isCovered(ef1_II, ef2_IIf)){
      return false;
    }

    return true;
  }*/

}
