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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ParallelCFAS;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.SSAMapManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * Stores information about environmental edges.
 */
@Options(prefix="cpa.rg")
public class RGEnvironmentManager implements StatisticsProvider{

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(toUppercase=true, values={"FA", "SA", "ST"},
          description="How to abstract environmental transitions:"
          + "ST - no abstraction, SA - precondition abstracted only, FA - precondition and operation abstracted")
  private String abstractEnvTransitions = "FA";

  @Option(description="Use caching for generating environmental transitions.")
  private boolean cacheGeneratingEnvTransition = true;

  @Option(description="Use exact locations instead of abstracted location classes.")
  private boolean preciseLocations = false;

  /** Number of threads. */
  private int threadNo;
  /** envPrecision[i] are predicates for generating env. edges from thread i. */
  private final SetMultimap<CFANode, AbstractionPredicate>[] envPrecision;
  /** envGlobalPrecision[i] contains global env. predicates for thread i */
  private final Set<AbstractionPredicate>[] envGlobalPrecision;
  private RGLocationMapping locationMapping;

  private final Map<Pair<RGEnvCandidate, Set<AbstractionPredicate>>, RGEnvTransition> candidateToTransitionCache;

  /* Managers */
  private final PathFormulaManager pfManager;
  private final MathsatFormulaManager fManager;
  private final MathsatTheoremProver tProver;
  private final RGAbstractionManager absManager;
  private final RegionManager rManager;
  private final AbstractionManagerImpl amManager;
  private final SSAMapManager ssaManager;
  private final RGEnvTransitionManager etManager;
  private final RGEnvCandidateManager candManager;

  /** Statitics of processing env. transitions. */
  private RelyGuaranteeEnvironmentProcessStatistics processStats;
  /** General stats */
  public final Stats stats;



  public RGEnvironmentManager(ParallelCFAS pcfa, Configuration config, LogManager logger) throws InvalidConfigurationException{
    // TODO add option for caching
    config.inject(this, RGEnvironmentManager.class);

    this.threadNo = pcfa.getThreadNo();

    this.envPrecision = new SetMultimap[threadNo];
    this.envGlobalPrecision = new HashSet[threadNo];
    this.stats = new Stats();

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
    this.absManager = RGAbstractionManager.getInstance(rManager, fManager, pfMgr, tProver, config, logger);
    this.amManager = AbstractionManagerImpl.getInstance(rManager, msatFormulaManager, pfManager, config, logger);
    this.ssaManager = SSAMapManagerImpl.getInstance(fManager, config, logger);
    this.etManager  = RGEnvTransitionManagerFactory.getInstance(abstractEnvTransitions, fManager, pfManager, absManager, ssaManager, tProver, rManager, pcfa, config, logger);
    this.candManager = new RGEnvCandidateManager(fManager, pfManager, absManager, ssaManager, tProver, rManager, pcfa, config, logger);

    for (int i=0; i< threadNo; i++){
      this.envPrecision[i] = HashMultimap.create();
      this.envGlobalPrecision[i] = new HashSet<AbstractionPredicate>();
    }

    if (cacheGeneratingEnvTransition){
      candidateToTransitionCache = new HashMap<Pair<RGEnvCandidate, Set<AbstractionPredicate>>, RGEnvTransition>();
    } else {
      candidateToTransitionCache = null;
    }


    if (preciseLocations){
      this.locationMapping = RGLocationMapping.getIndentity(pcfa);
    } else {
      this.locationMapping = RGLocationMapping.getEmpty(pcfa);
    }
  }

  public int getThreadNo() {
    return threadNo;
  }

  public void setThreadNo(int pThreadNo) {
    threadNo = pThreadNo;
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

  public RegionManager getrManager() {
    return rManager;
  }

  public AbstractionManagerImpl getAbsManager() {
    return amManager;
  }


  public void printUnprocessedTransitions(Collection<RGEnvCandidate> candidates) {
    printTransitions("Enviornmental transitions generated:", candidates);
  }

  /**
   * Return the stats of {@link processEnvTransitions} since the last {@link resetProcessStats}.
   * @return
   */
  public RelyGuaranteeEnvironmentProcessStatistics getProcessStats() {
    return processStats;
  }

  /**
   * Resets the stats of {@link processEnvTransitions}.
   */
  public void resetProcessStats(){
    processStats = null;
  }


  /**
   * Returns most general candidates from the previous m.g. candidates and some new ones.
   * @param oldMGCandidates
   * @param newCandidates
   * @return
   */
  public List<RGEnvCandidate> findMostGeneralCandidates(Collection<RGEnvCandidate> oldMGCandidates, Collection<RGEnvCandidate> newCandidates){
    stats.candidateTimer.start();

    Vector<RGEnvCandidate> newToProcess = new Vector<RGEnvCandidate>(newCandidates);
    Vector<RGEnvCandidate> newCovered = new Vector<RGEnvCandidate>(newCandidates.size());

    /* remove candidates whose application wouldn't make sense */
    for (RGEnvCandidate cnd : newToProcess){
      if (candManager.isBottom(cnd)){
        newCovered.add(cnd);

        if (debug){
          System.out.println("\t-bottom: "+cnd);
        }
      }
    }

    newToProcess.removeAll(newCovered);
    newCovered.clear();

    /* find the most general candidates among the remaning new */
    for (RGEnvCandidate cnd1 : newToProcess){
      if (!newCovered.contains(cnd1)){
        for (RGEnvCandidate cnd2 : newToProcess){
          if (cnd1 !=cnd2 && !newCovered.contains(cnd2)){
            if (candManager.isLessOrEqual(cnd1, cnd2)){
              // edge1 => edge2
              if (debug){
                System.out.println("\t-covered: "+cnd1+" => "+cnd2);
              }
              newCovered.add(cnd1);
              break;
            }
          }
        }
      }
    }

    newToProcess.removeAll(newCovered);
    newCovered.clear();

    /* find the most general candidates among old m.g. and the remaning new */
    List<RGEnvCandidate> oldCovered = new Vector<RGEnvCandidate>();

    for (RGEnvCandidate nCnd : newToProcess){
      for (RGEnvCandidate oCnd : oldMGCandidates){

        if (oldCovered.contains(oCnd)){
          continue;
        }

        if (candManager.isLessOrEqual(nCnd, oCnd)){
          // new <= old
          newCovered.add(nCnd);

          if (debug){
            System.out.println("\t- new covered: "+nCnd+ " => "+oCnd);
          }
          break;
        }
        else if (candManager.isLessOrEqual(oCnd, nCnd)){
          // new > old
          oldCovered.add(oCnd);

          if (debug){
            System.out.println("\t- old covered: "+oCnd+ " => "+nCnd);
          }
        }
      }
    }

    List<RGEnvCandidate> newMGCandidates = new Vector<RGEnvCandidate>(newToProcess);
    newMGCandidates.removeAll(newCovered);
    newMGCandidates.addAll(oldMGCandidates);
    newMGCandidates.removeAll(oldCovered);

    /* sanity check on request */
    if (debug){
      checkMostGeneralCandidates(oldMGCandidates, newCandidates, newMGCandidates);
    }

    stats.candidateTimer.stop();
    return  newMGCandidates;
  }

  /**
   * Checks result of {@link #getMostGeneralCandidates()}.
   * @param oldMGCandidates
   * @param newCandidates
   * @param newMGCandidates
   */
  private void checkMostGeneralCandidates(Collection<RGEnvCandidate> oldMGCandidates, Collection<RGEnvCandidate> newCandidates, Collection<RGEnvCandidate> newMGCandidates){
    stats.checkingTimer.start();

    // new m.g. candidates are incomperable
    for (RGEnvCandidate cand1 : newMGCandidates){
      for (RGEnvCandidate cand2 : newMGCandidates){
        if (cand1 != cand2){
          if (candManager.isLessOrEqual(cand1, cand2)){
            System.out.println(this.getClass());
          }
          assert !candManager.isLessOrEqual(cand2, cand1);
          assert !candManager.isLessOrEqual(cand1, cand2);
        }
      }
    }

    // for every candidate in oldMGCandidates there exist greater of equal candidate in newMGCandidates
    for (RGEnvCandidate oCand : oldMGCandidates){
      boolean existsGeq = false;
      for (RGEnvCandidate mgCand : newMGCandidates){
        if (candManager.isLessOrEqual(oCand, mgCand)){
          existsGeq = true;
          break;
        }
      }
      assert existsGeq || candManager.isBottom(oCand);
    }

    // for every candidate in newCandidates there exist greater of equal candidate in newMGCandidates
    for (RGEnvCandidate nCand : newCandidates){
      boolean existsGeq = false;
      for (RGEnvCandidate mgCand : newMGCandidates){
        if (candManager.isLessOrEqual(nCand, mgCand)){
          existsGeq = true;
          break;
        }
      }

      assert existsGeq || candManager.isBottom(nCand);
    }
    stats.checkingTimer.stop();
  }


  /**
   * Returns the most general env. transitions generated from the candidates using the precision.
   * @param candidates
   * @param preds
   * @return
   */
  private List<RGEnvTransition> findMostGeneralEnvTransitions(Collection<RGEnvCandidate> candidates, Set<AbstractionPredicate> preds){

    /* abstract the candidates */
    Vector<RGEnvTransition> newEt   = new Vector<RGEnvTransition>(candidates.size());
    for (RGEnvCandidate cand : candidates){
      RGEnvTransition et = generateEnvTransition(cand, preds);
      //RGEnvTransition et = etManager.generateEnvTransition(cand, preds);
      newEt.add(et);
    }

    newEt = findMostGeneralTransitions(newEt);

    //stats.maxValid = Math.max(stats.maxValid, newEt.size());
    stats.maxApplied = stats.maxApplied >= newEt.size() ? stats.maxApplied : newEt.size();
    return newEt;
  }


  private RGEnvTransition generateEnvTransition(RGEnvCandidate cand, Set<AbstractionPredicate> preds) {
    stats.etGenerated++;
    RGEnvTransition et;

    if (cacheGeneratingEnvTransition){
      Pair<RGEnvCandidate, Set<AbstractionPredicate>> key = Pair.of(cand, preds);
      et = candidateToTransitionCache.get(key);

      if (et == null){
        et = etManager.generateEnvTransition(cand, preds);
        candidateToTransitionCache.put(key, et);
      } else {
        stats.cand2EtCacheHits++;
      }
    }
    else {
      et = etManager.generateEnvTransition(cand, preds);
    }

    return et;
  }



  /*
   * Converts  unprocessed candidates from thread i into environmental transitions.
   * The most general transitions are remebered as valid and unapplied for other threads.
   * @param i   thread that generated the transitions
   *public List<RGEnvTransition> processCandidates(int i, Collection<RGEnvCandidate> candidates, List<RGEnvTransition> oldValid) {
    if (processStats == null){
      processStats = new RelyGuaranteeEnvironmentProcessStatistics();
    }
    processStats.totalTimer.start();
    stats.totalTimer.start();
    processStats.candidates = candidates.size();
    stats.allCandidates += candidates.size();



    Vector<RGEnvCandidate> gCandidates = findMostGeneralCandidates(candidates);

    // abstract the candidates
    Vector<RGEnvTransition> newEnv   = new Vector<RGEnvTransition>(gCandidates.size());
    for (RGEnvCandidate cand : gCandidates){
      RGEnvTransition et = etManager.generateEnvTransition(cand, envGlobalPrecision[i], envPrecision[i]);
      newEnv.add(et);
    }

    newEnv = findMostGeneralTransitions(newEnv);

    // compare new and old transitions
    Pair<Vector<RGEnvTransition>, Vector<RGEnvTransition>> pair = semanticCoverageCheck(newEnv, oldValid, i);

    // env. edges that are not properly covered by any valid edge or another edge in newValid
    Vector<RGEnvTransition> newValid = pair.getFirst();
    // valid env. edges that are properly covered by some edge in newValid
    Vector<RGEnvTransition> oldCovered = pair.getSecond();
    processStats.newValid = newValid.size();
    stats.allNew += newValid.size();

    if (debug){
      printEdges("New env. edges that are not covered:",newValid);
      printEdges("Old env. edges that are covered by some new ones:",oldCovered);
    }

    Vector<RGEnvTransition> validPrime = new Vector<RGEnvTransition>(oldValid);

    // remove valid edges that have been covered
    validPrime.removeAll(oldCovered);

    // add the edges after filtering to the set of valid edges by thread i
    // add them to the set of unapplied edges for other threads
    validPrime.addAll(newValid);
    processStats.allValid = validPrime.size();
    stats.maxValid = Math.max(stats.maxValid, validPrime.size());

    if (debug){
      printEdges("All valid env. edges from thread "+i+" after filtering", validPrime);
    }

    stats.totalTimer.stop();
    processStats.totalTimer.stop();

    return validPrime;
  }*/



  /**
   * Print env. edges with a title.
   * @param string
   * @param rgEdges
   */
  private void printEdges(String string, List<RGEnvTransition> rgEdges) {
    //System.out.println();
    if (rgEdges.isEmpty()){
      System.out.println(string+"\tnone");
    } else {
      System.out.println(string);
    }
    for (RGEnvTransition edge : rgEdges){
      System.out.println("\t-"+edge);
    }
  }

  /**
   * Print env. transitinos with a title.
   * @param string
   */
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
   * Find the most general elements from the set of candidates using the comparator.
   * @param candidates
   * @return
   */
  private Vector<RGEnvCandidate> findMostGeneralCandidates(Collection<RGEnvCandidate> candidates) {

    // candidates to be inspected
    Vector<RGEnvCandidate> cndToProcess = new Vector<RGEnvCandidate>(candidates);
    // candidate that are covered or false
    Vector<RGEnvCandidate> cndCovered = new Vector<RGEnvCandidate>();

    /* remove candidates whose application wouldn't make sense */
    for (RGEnvCandidate cnd : cndToProcess){
      if (candManager.isBottom(cnd)){
        cndCovered.add(cnd);

        if (debug){
          System.out.println("\t-bottom: "+cnd);
        }
      }
    }

    cndToProcess.removeAll(cndCovered);

    /* find the most general candidates by comparing them */
    for (RGEnvCandidate cnd1 : cndToProcess){
      if (!cndCovered.contains(cnd1)){
        for (RGEnvCandidate cnd2 : cndToProcess){
          if (cnd1 !=cnd2 && !cndCovered.contains(cnd2)){
            if (candManager.isLessOrEqual(cnd1, cnd2)){
              // edge1 => edge2
              if (debug){
                System.out.println("\t-covered: "+cnd1+" => "+cnd2);
              }
              cndCovered.add(cnd1);
              break;
            }
          }
        }
      }
    }

    cndToProcess.removeAll(cndCovered);


    /* sanity check on request */
    if (debug){
      // for every input candidate there exist an candidate in cndToProcess that is greater or equal,
      // unless the candidate is bottom
      for (RGEnvCandidate cand1 : candidates){
        boolean existsGeq = false;
        for (RGEnvCandidate cand2 : cndToProcess){
          if (candManager.isLessOrEqual(cand1, cand2)){
            existsGeq = true;
            break;
          }
        }

        assert existsGeq || candManager.isBottom(cand1);
      }

      // among the candidates in cndToProcess none is less or equal than other
      for (RGEnvCandidate cand1 : cndToProcess){
        for (RGEnvCandidate cand2 : cndToProcess){
          assert cand1 == cand2 || !candManager.isLessOrEqual(cand1, cand2);
        }
      }
    }

    return  cndToProcess;
  }




  /**
   * Find the most general environmental transitions from the set.
   * @param transitions
   * @return
   */
  private Vector<RGEnvTransition> findMostGeneralTransitions(Collection<RGEnvTransition> transitions) {
    stats.etComparing.start();

    // candidates to be inspected
    Vector<RGEnvTransition> etToProcess = new Vector<RGEnvTransition>(transitions);
    // candidate that are covered or false
    Vector<RGEnvTransition> etCovered = new Vector<RGEnvTransition>();

    /* remove candidates whose application wouldn't make sense */
    for (RGEnvTransition et : etToProcess){
      if (etManager.isBottom(et)){
        etCovered.add(et);

        if (debug){
          System.out.println("\t-bottom: "+et);
        }
      }
    }

    etToProcess.removeAll(etCovered);

    /* find the most general candidates by comparing them */
    for (RGEnvTransition et1 : etToProcess){
      if (!etCovered.contains(et1)){
        for (RGEnvTransition et2 : etToProcess){
          if (et1 !=et2 && !etCovered.contains(et2)){
            if (etManager.isLessOrEqual(et1, et2)){
              // edge1 => edge2
              if (debug){
                System.out.println("\t-covered: "+et1+" => "+et2);
              }
              etCovered.add(et1);
              break;
            }
          }
        }
      }
    }

    etToProcess.removeAll(etCovered);

    /* sanity check on request */
    if (debug){
      checkMostGeneralTransitions(transitions, etToProcess);
    }

    stats.etComparing.stop();
    return  etToProcess;
  }

  /**
   * Checks result of {@link #findMostGeneralTransitions()}.
   * @param allTransitions
   * @param mostGeneral
   */
  private void checkMostGeneralTransitions(Collection<RGEnvTransition> allTransitions, Collection<RGEnvTransition> mostGeneral){
    stats.checkingTimer.start();
    for (RGEnvTransition edge1 : allTransitions){
      for (RGEnvTransition edge2 : mostGeneral){
        assert edge1 == edge2 || !etManager.isLessOrEqual(edge2,edge1)  ||  etManager.isLessOrEqual(edge2,edge1);
      }
    }
    for (RGEnvTransition edge1 : mostGeneral){
      for (RGEnvTransition edge2 : mostGeneral){
        assert edge1 == edge2 || !etManager.isLessOrEqual(edge2,edge1);
      }
    }
    stats.checkingTimer.stop();
  }



  /**
   * Return a pair of list.
   * The first list contains env. transitions from newTransitions that aren't properly covered by any transition
   * in validEnvEdgesFromThread[i]. The second list contains env. edges from validEnvEdgesFromThread[i] that are
   * properly covered by some edge in newEdges.
   * @param rgEdges   set of new env edges generated by thread i
   * @param i         source thread
   */
  private Pair<Vector<RGEnvTransition>, Vector<RGEnvTransition>> semanticCoverageCheck(List<RGEnvTransition> newEnv, List<RGEnvTransition> oldValid, int i) {
    Vector<RGEnvTransition> toProcess  = new Vector<RGEnvTransition>(newEnv);
    Vector<RGEnvTransition> newValid   = new Vector<RGEnvTransition>(newEnv);
    Vector<RGEnvTransition> oldCovered = new Vector<RGEnvTransition>();

    // TODO improve
    for (RGEnvTransition newEdge : toProcess){
      for (RGEnvTransition oldEdge : oldValid){
        if (oldCovered.contains(oldEdge)){
          continue;
        }
        assert newEdge != oldEdge;
        if (etManager.isLessOrEqual(newEdge, oldEdge)){
          // newEdge => oldEdge
          if (debug){
            System.out.println("Covered :\t"+newEdge+" => "+oldEdge);
          }
          newValid.remove(newEdge);
          break;
        } else if (etManager.isLessOrEqual(oldEdge, newEdge)){
          // oldEdge => newEdge, but not oldEdge <= newEdge
          if (debug){
            System.out.println("Covered :\t"+oldEdge+" => "+newEdge);
          }
          oldCovered.add(oldEdge);
        }
      }
    }

    // sanity check on request
    if (debug){
      for (RGEnvTransition edge1  : newValid){
        for (RGEnvTransition edge2  : oldValid){
          assert  !etManager.isLessOrEqual(edge1,edge2);
        }
      }
      for (RGEnvTransition edge1  : oldCovered){
        boolean covered = false;
        assert oldValid.contains(edge1);
        for (RGEnvTransition edge2  : newValid){
          covered = covered || etManager.isLessOrEqual(edge1, edge2);
        }
        assert covered;
      }
    }
    return Pair.of(newValid, oldCovered);
  }


  /**
   * Finds the last rely-guarantee abstraction element that is an ancestor of the argument.
   * @param target
   * @return
   */
  public static ARTElement findLastAbstractionARTElement(ARTElement target) {
    ARTElement laARTElement = null;
    Deque<ARTElement> toProcess = new LinkedList<ARTElement>();
    Set<ARTElement> visisted = new HashSet<ARTElement>();

    if (target.isDestroyed()){
      target = target.getMergedWith();
    }

    toProcess.add(target);

    while (!toProcess.isEmpty()){
      ARTElement element = toProcess.poll();
      assert !element.isDestroyed();
      visisted.add(element);

      AbstractionElement aElement = AbstractElements.extractElementByType(element, AbstractionElement.class);
      if (aElement != null){
        laARTElement = element;
        break;
      }

      for (ARTElement parent : element.getParentARTs()){
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

  public RGLocationMapping getLocationMapping() {
    return locationMapping;
  }

  public void setLocationMapping(RGLocationMapping pLocationMapping) {
    locationMapping = pLocationMapping;
  }



  /**
   * Add predicates to environmental precision of some thread.
   * @param tid
   * @param loc
   * @param preds
   */
  public void addPredicatesToEnvPrecision(Integer tid, CFANode loc, Collection<AbstractionPredicate> preds){
    //this.envPrecision[tid].putAll(loc, preds);
    this.envGlobalPrecision[tid].addAll(preds);
  }


  /**
   * Find new env. transitions that should be applied.
   * @param appliedBefore env. transitions previously applied
   * @param candidates all candidates from source thread
   * @param preds predicates for env. transitions
   * @return
   */
  public List<RGEnvTransition> getEnvironmentalTransitionsToApply(Set<RGEnvTransition> appliedBefore, List<RGEnvCandidate> candidates, Set<AbstractionPredicate> preds) {
    stats.etToApplyTimer.start();

    List<RGEnvTransition> newTransitions = findMostGeneralEnvTransitions(candidates, preds);
    List<RGEnvTransition> newToApply = getDifference(newTransitions, appliedBefore);

    stats.etToApplyTimer.stop();
    return newToApply;
  }

  /**
   * Returns new transitions that are not less or equal to any old transition.
   * @param newEt
   * @param oldEt
   * @return
   */
  private List<RGEnvTransition> getDifference(Collection<RGEnvTransition> newEt, Collection<RGEnvTransition> oldEt) {
    List<RGEnvTransition> diff = new Vector<RGEnvTransition>(newEt);

    for (RGEnvTransition net : newEt){
      for (RGEnvTransition oet : oldEt){

        if (this.etManager.isLessOrEqual(net, oet)){
          diff.remove(net);
          break;
        }
      }
    }

    return diff;
  }


  public Set<AbstractionPredicate>[] getEnvGlobalPrecision() {
    return envGlobalPrecision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  public static class Stats implements Statistics {


    //private final Timer totalTimer       = new Timer();
    private final Timer candidateTimer    = new Timer();
    private final Timer etToApplyTimer = new Timer();
    private final Timer checkingTimer     = new Timer();
    private final Timer etComparing       = new Timer();
    private int maxApplied        = 0;
    private int etGenerated       = 0;
    private int cand2EtCacheHits  = 0;

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("time on most general candidates:   " + candidateTimer);
      out.println("time on finding e.t. to apply.:    " + etToApplyTimer);
      out.println("time on correctness checks:        " + checkingTimer);
      out.println("time on comparing e.t.:            " + etComparing);
      out.println("env. tr. generation cache hits:    " + cand2EtCacheHits+"/"+etGenerated+" ("+toPercent(cand2EtCacheHits, etGenerated)+")");
      out.println("max env. transitions applied:      " + maxApplied);
    }

    private String toPercent(double val, double full) {
      return String.format("%1.0f", val/full*100) + "%";
    }

    @Override
    public String getName() {
      return "RGEnvironmentalManager";
    }

  }

  /**
   * Statistics for processing env. transitions.
   */
  public static class RelyGuaranteeEnvironmentProcessStatistics implements Statistics {

    private final Timer totalTimer    = new Timer();
    private int candidates      = 0;
    private int newValid        = 0;
    private int allValid        = 0;

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {

      out.println("time:"+totalTimer+" candidates:" +candidates + " new valid:"+newValid + " all valid:"+allValid );
    }

    @Override
    public String getName() {
      return "RGEnvironmentalManager.processEnvTransitions";
    }

  }


}



