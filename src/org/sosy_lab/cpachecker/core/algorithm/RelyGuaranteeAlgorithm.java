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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.RelyGuaranteeCFA;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
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
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeEnvironment;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.Multimap;

@Options(prefix="cpa.relyguarantee")
public class RelyGuaranteeAlgorithm implements ConcurrentAlgorithm, StatisticsProvider{

  @Option(name="symbolcCoverageCheck",description="Use a theorem prover to remove covered environemtal transitions" +
                      " if false perform only a syntatic check for equivalence")
  private boolean checkEnvTransitionCoverage = true;

  @Option(description="List of variables global to multiple threads")
  protected String[] globalVariables = {};

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
  private PathFormulaManager pfManager;
  private FormulaManager     fManager;
  private TheoremProver      tProver;
  private RegionManager rManager = BDDRegionManager.getInstance();

  // stores information about environmental transitions
  private RelyGuaranteeEnvironment environment;

  public RelyGuaranteeAlgorithm(CFA[] pCfas, CFAFunctionDefinitionNode[] pMainFunctions, ConfigurableProgramAnalysis[] pCpas, Configuration config, LogManager logger) {
    this.threadNo = pCfas.length;
    this.mainFunctions = pMainFunctions;
    this.cpas = pCpas;
    this.logger = logger;

    environment = new RelyGuaranteeEnvironment(threadNo, config, logger);
    threadCPA = new RelyGuaranteeThreadCPAAlgorithm[threadNo];
    cfas = new RelyGuaranteeCFA[threadNo];

    try {
      config.inject(this, RelyGuaranteeAlgorithm.class);
      for (int i=0; i<threadNo; i++) {
        cfas[i] = new RelyGuaranteeCFA(pCfas[i]);
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

    for (int i=0; i< this.threadNo; i++){
      threadCPA[i] = new RelyGuaranteeThreadCPAAlgorithm(cpas[i],environment,config, logger, i);
    }

    // create DOT file for the original CFA
    for (int i=0; i< this.threadNo; i++){
      this.dumpDot(i, "test/output/oldCFA"+i+".dot");
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    // TODO Auto-generated method stub
  }

  /**
   * Returns -1 if no error is found or the thread no with an error
   */
  public int run(ReachedSet[] reached, int startThread) {
    assert environment.getUnprocessedTransitions().isEmpty();
    boolean error = false;
    try{
      // run each tread at least once until no new env can be applied to any thread
      int i = startThread;
      while(i != -1 && !error) {

        // apply all valid env. edges to CFA
        Map<CFANode, RelyGuaranteeCFAEdge> envEdgesMap = addEnvTransitionsToCFA(i);
        // add relevant states to the wait list
        setWaitlist(reached[i], envEdgesMap);
        // run the thread
        error = runThread(i, reached[i], true);
        // clear the set of  unapplied env. edges
        environment.clearUnappliedEnvEdgesForThread(i);
        if (error) {
          // error state has been reached
          return i;
        }
        environment.printUnprocessedTransitions();
        // process new env. transitions
        environment.processEnvTransitions(i);
        // chose a new thread to run
        i = pickThread(reached);
      }

    } catch(Exception e){
      e.printStackTrace();
    }

    return -1;
  }

  /**
   * Put relevant states into the waitlist
   * @param envEdgesMap
   */
  private void setWaitlist(ReachedSet reachedSet, Map<CFANode, RelyGuaranteeCFAEdge> envEdgesMap) {
    for (CFANode node : envEdgesMap.keySet()){
      Set<AbstractElement> relevant = reachedSet.getReached(node);
      for (AbstractElement ae : relevant){
        if (AbstractElements.extractLocation(ae).equals(node)){
          ARTElement artElement = (ARTElement) ae;
          Set<RelyGuaranteeCFAEdge> envEdges = artElement.getEnvEdgesToBeApplied();
          if (envEdges == null){
            envEdges = new HashSet<RelyGuaranteeCFAEdge>();
          }
          envEdges.add(envEdgesMap.get(node));
          artElement.setEnvEdgesToBeApplied(envEdges);
          reachedSet.reAddToWaitlist(ae);
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
    while(i<this.threadNo && reached[i].getWaitlistSize() == 0 && environment.getUnappliedEnvEdgesForThread(i).isEmpty()){
      i++;
    }
    if (i==this.threadNo){
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
      String s = DOTBuilder.generateDOT(this.cfas[pI].getFunctions().values(), this.mainFunctions[pI]);
      out.write(s);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Apply env transitions to CFA no. i and return true if at least one node has been applied
   */
  private Map<CFANode, RelyGuaranteeCFAEdge> addEnvTransitionsToCFA(int i) {
    Map<CFANode, RelyGuaranteeCFAEdge> envEdgesMap = new HashMap<CFANode, RelyGuaranteeCFAEdge>();
    RelyGuaranteeCFA cfa = cfas[i];
    Multimap<CFANode, String> map = cfa.getRhsVariables();
    // remove old environmental edges
    removeRGEdges(i);
    this.dumpDot(i, "test/output/revertedCFA"+i+".dot");
    // iterate over both new and old env edges
    // TODO extend to mutliple threds
    int j = (i==1 ? 0 : 1);
     List<RelyGuaranteeCFAEdgeTemplate> valid = environment.getValidEnvEdgesFromThread(j);


    for (RelyGuaranteeCFAEdgeTemplate envTransition : valid){
      String var = getLhsVariable(envTransition.getLocalEdge());
      for(Entry<String, CFANode> entry :   cfa.getCFANodes().entries()){
        CFANode node = entry.getValue();
        // check if the rhs of any edge leaving the node reads the variable assigned by 'envTransition'
        if (map.get(node).contains(var)){
          RelyGuaranteeCFAEdge edge = envTransition.instantiate();
          addEnvTransitionToNode(node, edge);
          envEdgesMap.put(node, edge);
        }
      }
    }
    this.dumpDot(i, "test/output/newCFA"+i+".dot");

    return envEdgesMap;
  }

  /**
   * Get the variable in the lhs of an expression or return null
   */
  private String getLhsVariable(CFAEdge edge){
    IASTNode node = edge.getRawAST();
    if (node instanceof IASTExpressionAssignmentStatement) {
      IASTExpressionAssignmentStatement stmNode = (IASTExpressionAssignmentStatement) node;
      if (stmNode.getLeftHandSide() instanceof IASTIdExpression) {
        IASTIdExpression idExp = (IASTIdExpression) stmNode.getLeftHandSide();
        return new String(idExp.getName());
      }
    }
    return null;
  }

  /**
   * Remove old environmental edges.
   */
  private void removeRGEdges(int i) {
    RelyGuaranteeCFA cfa = this.cfas[i];
    Multimap<CFANode, String> map = cfa.getRhsVariables();
    List<CFAEdge> toRemove = new Vector<CFAEdge>();
    // remove old env edges from the CFA
    for (CFANode node : cfa.getCFANodes().values()){
      for (int j=0; j<node.getNumLeavingEdges(); j++) {
        CFAEdge edge = node.getLeavingEdge(j);
        if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
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
  private void addEnvTransitionToNode(CFANode pNode, RelyGuaranteeCFAEdge pEnvTransition) {
    pEnvTransition.setPredecessor(pNode);
    pEnvTransition.setSuccessor(pNode);
    pNode.addLeavingEdge(pEnvTransition);
    pNode.addEnteringEdge(pEnvTransition);
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
