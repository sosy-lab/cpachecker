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
package org.sosy_lab.cpachecker.cpa.relyguarantee.environment;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
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
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractCFAEdgeTemplate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCFAEdgeTemplate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGVariables;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.SSAMapManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * Stores information about environmental edges.
 */
@Options(prefix="cpa.relyguarantee")
public class RGEnvironmentManager {

  @Option(description="Print debugging info?")
  private boolean debug=true;

  @Option(description="Abstract environmental transitions using their own predicates:"
      + "0 - don't abstract, 1 - abstract filter, 2 - abstract filter and operation.")
  private int abstractEnvTransitions = 2;

  /**
   * Statiscs for processing env transitions transitions.
   */
  public class RelyGuaranteeEnvironmentProcessStatistics implements Statistics {

    private Timer totalTimer    = new Timer();
    private Timer semanticTimer = new Timer();

    private int unprocessed       = 0;
    private int removedSyntactic  = 0;
    private int coverChecks       = 0;
    private int oldCovered        = 0;
    private int newCovered        = 0;
    private int newValid          = 0;
    private int allValid           = 0;

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {

      out.println("Transitions generated:                 " + unprocessed);
      out.println("Transitions removed by syntatic check: " + removedSyntactic);
      out.println("No of coverage checks (non-debug):     " + coverChecks);
      out.println("New transitions that are covered:      " + newCovered);
      out.println("Old transitions that become covered:   " + oldCovered);
      out.println("New valid transitions by thread:       " + newValid  );
      out.println("All valid transitions by thread:       " + allValid  );
      out.println();
      out.println("Time for coverage check:               " + semanticTimer);
      out.println("Total time for processing:             " + totalTimer);
    }

    @Override
    public String getName() {
      return "Enviornmetal transitions processing statistics";
    }

  }

  // Statitics about processing env. transitions
  private RelyGuaranteeEnvironmentProcessStatistics processStats;

  // number of threads
  private int threadNo;
  // unprocessed candidate for env transitions
  private final Vector<RGEnvCandidate> unprocessedTransitions;
  // all rely guarantee transitions  generated by thread i that do not have to be processed again
  // for thread i envTransProcessedBeforeFromThread[i] is a multimap : source ART element -> generated env transitions
  //private final Multimap<ARTElement, RelyGuaranteeEnvironmentalTransition>[] envTransProcessedBeforeFromThread;
  // valid rely env edges generated by thread i
  private final Vector<RGCFAEdgeTemplate>[] validEnvEdgesFromThread;
  // rely guarantee edges valid for thread i, that haven't been applied yet on the CFA
  // these edges should belong to some  validEnvEdgesFromThread[j], where j != i
  // this data structure is used for termination check
  private final Vector<RGCFAEdgeTemplate>[] unappliedEnvEdgesForThread;
  // envPrecision[i] are predicates for generating env. edges from thread i.
  private final SetMultimap<CFANode, AbstractionPredicate>[] envPrecision;
  // envGlobalPrecision[i] contains global env. predicates for thread i
  private final Set<AbstractionPredicate>[] envGlobalPrecision;
  // information about variables in threads
  private final RGVariables variables;

  // Managers
  private final PathFormulaManager pfManager;
  private final MathsatFormulaManager fManager;
  private final MathsatTheoremProver tProver;
  private final PredicateAbstractionManager paManager;
  private final RegionManager rManager;
  private final AbstractionManagerImpl absManager;
  private final SSAMapManager ssaManager;

  /* comparators */
  private final RGEnvCandidateComparator candComparator;
  private final RGEnvTransitionComparator etComparator;


  public RGEnvironmentManager(int threadNo, RGVariables vars, Configuration config, LogManager logger) throws InvalidConfigurationException{
    // TODO add option for caching
    config.inject(this, RGEnvironmentManager.class);

    this.threadNo = threadNo;
    this.unprocessedTransitions = new Vector<RGEnvCandidate>();
    this.validEnvEdgesFromThread = new Vector[threadNo];
    this.unappliedEnvEdgesForThread = new Vector[threadNo];
    this.envPrecision = new SetMultimap[threadNo];
    this.envGlobalPrecision = new HashSet[threadNo];
    this.variables = vars;

    // set up managers
    MathsatFormulaManager msatFormulaManager;
    this.rManager = BDDRegionManager.getInstance();
    msatFormulaManager = MathsatFormulaManager.getInstance(config, logger);
    this.fManager = msatFormulaManager;
    this.tProver = MathsatTheoremProver.getInstance(msatFormulaManager);
    PathFormulaManager pfMgr  = PathFormulaManagerImpl.getInstance(msatFormulaManager, config, logger);
    pfMgr = CachingPathFormulaManager.getInstance(pfMgr);
    this.pfManager = pfMgr;
    RegionManager rManager = BDDRegionManager.getInstance();
    this.paManager = PredicateAbstractionManager.getInstance(rManager, fManager, pfMgr, tProver, config, logger);
    this.absManager = AbstractionManagerImpl.getInstance(rManager, msatFormulaManager, pfManager, config, logger);
    this.ssaManager = SSAMapManagerImpl.getInstance(fManager, config, logger);

    this.candComparator = RGEnvCandidateComparator.getComparator(abstractEnvTransitions, tProver, fManager, rManager);
    this.etComparator   = RGEnvTransitionComparator.getComparator(abstractEnvTransitions, tProver, fManager, rManager);

    for (int i=0; i< threadNo; i++){
      //envTransProcessedBeforeFromThread[i] = HashMultimap.<ARTElement, RelyGuaranteeEnvironmentalTransition>create();
      this.validEnvEdgesFromThread[i] = new Vector<RGCFAEdgeTemplate>();
      this.unappliedEnvEdgesForThread[i] = new Vector<RGCFAEdgeTemplate>();
      this.envPrecision[i] = HashMultimap.create();
      this.envGlobalPrecision[i] = new HashSet<AbstractionPredicate>();
    }


  }



  public int getThreadNo() {
    return threadNo;
  }

  public void setThreadNo(int pThreadNo) {
    threadNo = pThreadNo;
  }

  public RGVariables getVariables() {
    return variables;
  }

  public PathFormulaManager getPfManager() {
    return pfManager;
  }

  public MathsatFormulaManager getfManager() {
    return fManager;
  }

  public MathsatTheoremProver gettProver() {
    return tProver;
  }

  public PredicateAbstractionManager getPaManager() {
    return paManager;
  }



  public RegionManager getrManager() {
    return rManager;
  }



  public AbstractionManagerImpl getAbsManager() {
    return absManager;
  }



  /**
   * Add new environmental transitions for processing.
   * @param pAElement
   * @param pNewEnvTransitions
   */
  public void addEnvTransitions(Collection<? extends RGEnvCandidate> newTransitions) {
    if (!newTransitions.isEmpty()){
      unprocessedTransitions.addAll(newTransitions);
    }
  }

  public void addEnvTransition(RGEnvCandidate newTransition) {
    unprocessedTransitions.add(newTransition);
  }

  public void clearUnappliedEnvEdgesForThread(int i) {
    unappliedEnvEdgesForThread[i].removeAllElements();
  }

  public Vector<RGEnvCandidate> getUnprocessedTransitions(){
    return unprocessedTransitions;
  }

  public void addNewEnvEgde(int j, RGCFAEdgeTemplate edge) {
    unappliedEnvEdgesForThread[j].add(edge);
  }


  public  List<RGCFAEdgeTemplate> getValidEnvEdgesFromThread(int i) {
    return Collections.unmodifiableList(validEnvEdgesFromThread[i]);
  }

  public  List<RGCFAEdgeTemplate> getUnappliedEnvEdgesForThread(int i) {
    return Collections.unmodifiableList(unappliedEnvEdgesForThread[i]);
  }

  public void printUnprocessedTransitions() {
    printTransitions("Enviornmental transitions generated:", unprocessedTransitions);
  }

  /**
   * Converts environmental unprocessed transitions from thread i into enviromental transitions.
   * The most general transitions are remebered as valid and unapplied for other threads.
   * @param i   thread that generate the transitions
   */
  public void processEnvTransitions(int i) {
    processStats = new RelyGuaranteeEnvironmentProcessStatistics();
    processStats.totalTimer.start();
    processStats.unprocessed = unprocessedTransitions.size();

    processStats.semanticTimer.start();
    Vector<RGEnvCandidate> gCandidates = findMostGeneralCandidates(unprocessedTransitions);
    unprocessedTransitions.clear();
    processStats.semanticTimer.stop();

    // abstract the candidates
    Vector<RGCFAEdgeTemplate> newEnv   = new Vector<RGCFAEdgeTemplate>(gCandidates.size());
    for (RGEnvCandidate cand : gCandidates){
      RGCFAEdgeTemplate et = generateEnvTransition(cand);
      newEnv.add(et);
    }

    newEnv = findMostGeneralTransitions(newEnv);

    // compare new and old transitions
    processStats.semanticTimer.start();
    Pair<Vector<RGCFAEdgeTemplate>, Vector<RGCFAEdgeTemplate>> pair = semanticCoverageCheck(newEnv, i);
    processStats.semanticTimer.stop();

    // env. edges that are not properly covered by any valid edge or another edge in newValid
    Vector<RGCFAEdgeTemplate> newValid = pair.getFirst();
    // valid env. edges that are properly covered by some edge in newValid
    Vector<RGCFAEdgeTemplate> oldCovered = pair.getSecond();
    processStats.newValid = newValid.size();
    processStats.oldCovered = oldCovered.size();

    if (debug){
      printEdges("New env. edges that are not covered:",newValid);
      printEdges("Old env. edges that are covered by some new ones:",oldCovered);
    }

    // remove valid edges that have been covered
    validEnvEdgesFromThread[i].removeAll(oldCovered);
    // unapplied edges may also become covered
    for (int j=0; j<threadNo; j++){
      if (j != i){
        unappliedEnvEdgesForThread[j].removeAll(oldCovered);
      }
    }

    // add the edges after filtering to the set of valid edges by thread i
    // add them to the set of unapplied edges for other threads
    validEnvEdgesFromThread[i].addAll(newValid);
    processStats.allValid = validEnvEdgesFromThread[i].size();
    distributeAsUnapplied(newValid, i);

    if (debug){
      printEdges("All valid env. edges from thread "+i+" after filtering", validEnvEdgesFromThread[i]);
    }

    processStats.totalTimer.stop();
  }

  /**
   * Generates a template for a env. transition. The transition may be abstracted.
   * @param pCand
   * @return
   */
  private RGCFAEdgeTemplate generateEnvTransition(RGEnvCandidate pCand) {
    ARTElement lastARTAbstractionElement = findLastAbstractionARTElement(pCand.getElement());
    assert lastARTAbstractionElement != null;

    if (this.abstractEnvTransitions == 2){
      // get the predicates for the transition
      int sourceTid = pCand.getTid();
      CFANode loc = pCand.getOperation().getPredecessor();
      SetMultimap<CFANode, AbstractionPredicate> prec = envPrecision[sourceTid];
      Set<AbstractionPredicate> preds = new HashSet<AbstractionPredicate>(prec.get(loc));
      preds.addAll(envGlobalPrecision[sourceTid]);


      PathFormula oldPf = pCand.getRgElement().getPathFormula();
      AbstractionFormula oldAbs = pCand.getRgElement().getAbstractionFormula();
      PathFormula newPf = null;

      // compute the sucessor's path formula
      try {
        newPf = pfManager.makeAnd(oldPf, pCand.getOperation());
      } catch (CPATransferException e) {
        e.printStackTrace();
      }

      // increment indexes of variables global and local to this thread by 1, mimimal index is 2
      Set<String> vars = new HashSet<String>(variables.globalVars);
      vars.addAll(variables.localVars.get(sourceTid));
      SSAMap oldSsa = oldPf.getSsa();
      SSAMap newSsa = ssaManager.incrementMap(oldSsa, vars, 1);

      // create a formula, where every index is increased - either by operation or by equivalence
      Pair<Pair<Formula, Formula>, SSAMap> equivs = ssaManager.mergeSSAMaps(newPf.getSsa(), newSsa);
      Formula newF = fManager.makeAnd(newPf.getFormula(), equivs.getFirst().getFirst());
      newPf = new PathFormula(newF, newSsa, newPf.getLength());



      // abstract
      AbstractionFormula newAbs = paManager.buildNextValAbstraction(oldAbs, oldPf, newPf, preds, sourceTid);
      assert lastARTAbstractionElement != null;

      return new RGAbstractCFAEdgeTemplate(newAbs, lastARTAbstractionElement, pCand);
    }
    /*else if (this.abstractEnvTransitions == 1){
      // abstract the conjuction of abstraction and path formula using set of predicates.
      int sourceTid = pCand.getSourceThread();
      CFANode loc = pCand.getEdge().getPredecessor();
      // preds is the set of env. predicates for the location plus the global predicates
      SetMultimap<CFANode, AbstractionPredicate> prec = envPrecision[sourceTid];
      Set<AbstractionPredicate> preds = new HashSet<AbstractionPredicate>(prec.get(loc));
      preds.addAll(envGlobalPrecision[sourceTid]);

      AbstractionFormula aFilter = paManager.buildAbstraction(pCand.getAbstractionFormula(), pCand.getPathFormula(), preds);
      return new RGAbstractCFAEdgeTemplate(aFilter, lastARTAbstractionElement, pCand);
    } else {
      // don't abstract - the filer is conjuction of abstraction and path formula of  the generating elements
      PathFormula filter = pfManager.makeAnd(pCand.getPathFormula(), pCand.getAbstractionPathFormula());
      return new RGCFAEdgeTemplate(filter, lastARTAbstractionElement, pCand);
    }*/
    return null;
  }

  /**
   * Print env. edges with a title.
   * @param string
   * @param rgEdges
   */
  private void printEdges(String string, List<RGCFAEdgeTemplate> rgEdges) {
    //System.out.println();
    if (rgEdges.isEmpty()){
      System.out.println(string+"\tnone");
    } else {
      System.out.println(string);
    }
    for (RGCFAEdgeTemplate edge : rgEdges){
      System.out.println("\t-"+edge);
    }
  }

  /**
   * Print env. transitinos with a title.
   * @param string
   */
  private void printTransitions(String string, Vector<RGEnvCandidate> pUnprocessedTransitions) {
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
   * Distributes env. edges as unapplied to threads other than i.
   * @param rgEdges
   * @param i
   */
  private void distributeAsUnapplied(List<RGCFAEdgeTemplate> rgEdges, int i) {
    for (int j=0; j<threadNo; j++){
      if (j!=i){
        unappliedEnvEdgesForThread[j].addAll(rgEdges);
      }
    }
  }

  /**
   * Find the most general elements from the set of candidates using the comparator.
   * @param candidates
   * @return
   */
  private Vector<RGEnvCandidate> findMostGeneralCandidates(List<RGEnvCandidate> candidates) {

    // candidates to be inspected
    Vector<RGEnvCandidate> cndToProcess = new Vector<RGEnvCandidate>(candidates);
    // candidate that are covered or false
    Vector<RGEnvCandidate> cndCovered = new Vector<RGEnvCandidate>();

    /* find the most general candidates by comparing them */
    for (RGEnvCandidate cnd1 : cndToProcess){
      if (!cndCovered.contains(cnd1)){
        for (RGEnvCandidate cnd2 : cndToProcess){
          if (cnd1 !=cnd2 && !cndCovered.contains(cnd2)){
            processStats.coverChecks++;
            if (candComparator.isLessOrEqual(cnd1, cnd2)){
              // edge1 => edge2
              if (debug){
                System.out.println("Covered 0:\t"+cnd1+" => "+cnd2);
              }
              cndCovered.add(cnd1);
              processStats.newCovered++;
              break;
            }
          }
        }
      }
    }

    cndToProcess.removeAll(cndCovered);

    /* sanity check on request */
    if (debug){
      for (RGEnvCandidate edge1 : candidates){
        for (RGEnvCandidate edge2 : cndToProcess){
          assert edge1 == edge2 || !candComparator.isLessOrEqual(edge2,edge1)  ||  candComparator.isLessOrEqual(edge2,edge1);
        }
      }
      for (RGEnvCandidate edge1 : cndToProcess){
        for (RGEnvCandidate edge2 : cndToProcess){
          assert edge1 == edge2 || !candComparator.isLessOrEqual(edge2,edge1);
        }
      }
    }

    return  cndToProcess;
  }

  /**
   * Find the most general elements from the set of candidates using the comparator.
   * @param transitions
   * @return
   */
  private Vector<RGCFAEdgeTemplate> findMostGeneralTransitions(List<RGCFAEdgeTemplate> transitions) {

    // candidates to be inspected
    Vector<RGCFAEdgeTemplate> etToProcess = new Vector<RGCFAEdgeTemplate>(transitions);
    // candidate that are covered or false
    Vector<RGCFAEdgeTemplate> etCovered = new Vector<RGCFAEdgeTemplate>();

    /* find the most general candidates by comparing them */
    for (RGCFAEdgeTemplate et1 : etToProcess){
      if (!etCovered.contains(et1)){
        for (RGCFAEdgeTemplate et2 : etToProcess){
          if (et1 !=et2 && !etCovered.contains(et2)){
            processStats.coverChecks++;
            if (isCovered(et1, et2)){
              // edge1 => edge2
              if (debug){
                System.out.println("Covered 0:\t"+et1+" => "+et2);
              }
              etCovered.add(et1);
              processStats.newCovered++;
              break;
            }
          }
        }
      }
    }

    etToProcess.removeAll(etCovered);

    /* sanity check on request */
    if (debug){
      for (RGCFAEdgeTemplate edge1 : transitions){
        for (RGCFAEdgeTemplate edge2 : etToProcess){
          assert edge1 == edge2 || !isCovered(edge2,edge1)  ||  isCovered(edge2,edge1);
        }
      }
      for (RGCFAEdgeTemplate edge1 : etToProcess){
        for (RGCFAEdgeTemplate edge2 : etToProcess){
          assert edge1 == edge2 || !isCovered(edge2,edge1);
        }
      }
    }

    return  etToProcess;
  }



  /**
   * Return a pair of list.
   * The first list contains env. transitions from newTransitions that aren't properly covered by any transition
   * in validEnvEdgesFromThread[i]. The second list contains env. edges from validEnvEdgesFromThread[i] that are
   * properly covered by some edge in newEdges.
   * @param rgEdges   set of new env edges generated by thread i
   * @param i         source thread
   */
  private Pair<Vector<RGCFAEdgeTemplate>, Vector<RGCFAEdgeTemplate>> semanticCoverageCheck(List<RGCFAEdgeTemplate> newTransitions, int i) {
    Vector<RGCFAEdgeTemplate> toProcess  = new Vector<RGCFAEdgeTemplate>(newTransitions);
    Vector<RGCFAEdgeTemplate> newValid   = new Vector<RGCFAEdgeTemplate>(newTransitions);
    Vector<RGCFAEdgeTemplate> oldCovered = new Vector<RGCFAEdgeTemplate>();

    // TODO improve
    for (RGCFAEdgeTemplate newEdge : toProcess){
      for (RGCFAEdgeTemplate oldEdge : validEnvEdgesFromThread[i]){
        if (oldCovered.contains(oldEdge)){
          continue;
        }
        assert newEdge != oldEdge;
        processStats.coverChecks++;
        if (isCovered(newEdge, oldEdge)){
          // newEdge => oldEdge
          if (debug){
            System.out.println("Covered 1:\t"+newEdge+" => "+oldEdge);
          }
          processStats.newCovered++;
          newValid.remove(newEdge);
          break;
        } else if (isCovered(oldEdge, newEdge)){
          // oldEdge => newEdge, but not oldEdge <= newEdge
          if (debug){
            System.out.println("Covered 2:\t"+oldEdge+" => "+newEdge);
          }
          processStats.oldCovered++;
          oldCovered.add(oldEdge);
        }
      }
    }

    // sanity check on request
    // TODO remove
    if (debug){
      for (RGCFAEdgeTemplate edge1  : newValid){
        for (RGCFAEdgeTemplate edge2  : validEnvEdgesFromThread[i]){
          assert  !isCovered(edge1,edge2);
        }
      }
      for (RGCFAEdgeTemplate edge1  : oldCovered){
        boolean covered = false;
        assert validEnvEdgesFromThread[i].contains(edge1);
        for (RGCFAEdgeTemplate edge2  : newValid){
          covered = covered || isCovered(edge1, edge2);
        }
        assert covered;
      }
    }
    return Pair.of(newValid, oldCovered);
  }


  /**
   * Returns true if env1 => env2 is valid, sound but not complete if operation is not abstracted.
   */
  public boolean isCovered(RGCFAEdgeTemplate env1, RGCFAEdgeTemplate env2) {
    if (env1.equals(env2)){
      return true;
    }

    // if operation was unabstracted, then they must match
    if (this.abstractEnvTransitions == 0 || this.abstractEnvTransitions == 1){
      if (!env1.getLocalEdge().equals(env2.getLocalEdge())){
        return false;
      }
    }

    Formula f1= env1.getFilter().getFormula();
    Formula f2 = env2.getFilter().getFormula();

    if (f1.isFalse() || f2.isTrue()) {
      return true;
    }

    if (this.abstractEnvTransitions == 0){
      // check coverage by thm. prover


      Formula nImpl = fManager.makeAnd(f1, fManager.makeNot(f2));
      tProver.init();
      try {
        return tProver.isUnsat(nImpl);
      } finally {
        tProver.reset();
      }
    } else {
      // check coverage by BDDs
      assert env1.getType() == RGCFAEdgeTemplate.RelyGuaranteeAbstractCFAEdgeTemplate;
      assert env2.getType() == RGCFAEdgeTemplate.RelyGuaranteeAbstractCFAEdgeTemplate;
      RGAbstractCFAEdgeTemplate aEnv1 = (RGAbstractCFAEdgeTemplate) env1;
      RGAbstractCFAEdgeTemplate aEnv2 = (RGAbstractCFAEdgeTemplate) env2;
      Region r1 = aEnv1.getAbstractFilter().asRegion();
      Region r2 = aEnv2.getAbstractFilter().asRegion();

      return rManager.entails(r1, r2);
    }
  }

  /**
   * Returns true iff edge is an assignment to a non-global variable
   */
  private boolean isLocalAssigment(CFAEdge edge) {
    String var = getLhsVariable(edge);
    if (var == null || !variables.globalVars.contains(var)){
      return true;
    }
    return false;
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
   * reachedElement elements has been merged into mergedElement, therefore adjust the source elements.
   * @param mergedElement
   * @param reachedElement
   */
  public void mergeSourceElements(ARTElement mergedElement, ARTElement reachedElement, int i) {
    assert reachedElement.isDestroyed();
    assert !mergedElement.isDestroyed();

    // valid
    for(RGCFAEdgeTemplate rgEdge : validEnvEdgesFromThread[i]){
      ARTElement sourceARTElement = rgEdge.getSourceARTElement();
      if (sourceARTElement == reachedElement){
       // rgEdge.setSourceARTElement(mergedElement);
        System.out.println("! Replaced id:"+reachedElement.getElementId()+" by id:"+mergedElement.getElementId()+" in a valid edge: "+rgEdge);
      }
    }


    // TODO
    /*
    for (RGEnvironmentalTransition et :  unprocessedTransitions){
      if (et.getSourceARTElement() == reachedElement){
        et.setSourceARTElement(mergedElement);
        System.out.println("! Replaced id:"+reachedElement.getElementId()+" by id:"+mergedElement.getElementId()+" in unprocesseds transition: "+et);
      }
    }*/


  }




  public void cleanEnvironment(int tid) {
    // TODO works only for two threads
    int other = tid==0 ? 1 : 0;
    unappliedEnvEdgesForThread[other].clear();
    validEnvEdgesFromThread[tid].clear();
  }

  public void resetEnvironment() {
    for (int i=0; i<threadNo; i++){
      unappliedEnvEdgesForThread[i].clear();
      validEnvEdgesFromThread[i].clear();
    }
    unprocessedTransitions.clear();
  }

  public void printProcessingStatistics(){
    System.out.println();
    System.out.println("Processing statistics:");
    if (processStats != null){
      processStats.printStatistics(System.out, null, null);
    }
  }

  /**
   * Finds the next descendant in the ART that is a rely-guarantee abstraction.
   * Returns the argument if it is an abstraction.
   * @param element
   * @return
   */
  private ARTElement findNextAbstractionARTElement(ARTElement element) {

    ARTElement naARTElement = null;
    Deque<ARTElement> toProcess = new LinkedList<ARTElement>();
    Set<ARTElement> visisted = new HashSet<ARTElement>();
    toProcess.add(element);

    while (!toProcess.isEmpty()){
      ARTElement elem = toProcess.poll();
      visisted.add(elem);

      AbstractionElement aElement = AbstractElements.extractElementByType(elem, AbstractionElement.class);
      if (aElement != null){
        naARTElement = elem;
        break;
      }

      for (ARTElement parent : elem.getChildren()){
        if (!visisted.contains(parent)){
          toProcess.addLast(parent);
        }
      }
    }

    return naARTElement;
  }


  /**
   * Finds the last rely-guarantee abstraction element that is an ancestor of the argument.
   * @param pElement
   * @return
   */
  public static ARTElement findLastAbstractionARTElement(ARTElement pElement) {
    ARTElement laARTElement = null;
    Deque<ARTElement> toProcess = new LinkedList<ARTElement>();
    Set<ARTElement> visisted = new HashSet<ARTElement>();
    toProcess.add(pElement);

    while (!toProcess.isEmpty()){
      ARTElement element = toProcess.poll();
      visisted.add(element);

      AbstractionElement aElement = AbstractElements.extractElementByType(element, AbstractionElement.class);
      if (aElement != null){
        laARTElement = element;
        break;
      }

      for (ARTElement parent : element.getParents()){
        if (!visisted.contains(parent)){
          toProcess.addLast(parent);
        }
      }
    }

    return laARTElement;
  }



  public SetMultimap<CFANode, AbstractionPredicate>[] getEnvPrecision() {
    return envPrecision;
  }

  /**
   * Add predicates to environmental precision of some thread.
   * @param tid
   * @param loc
   * @param preds
   */
  public void addPredicatesToEnvPrecision(Integer tid, CFANode loc, Collection<AbstractionPredicate> preds){
    this.envPrecision[tid].putAll(loc, preds);
  }

  public Set<AbstractionPredicate>[] getEnvGlobalPrecision() {
    return envGlobalPrecision;
  }







}



