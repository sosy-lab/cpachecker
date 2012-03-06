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
import org.sosy_lab.cpachecker.cpa.art.ARTPrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGPrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

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

  /*
  @Option(description="Use exact locations instead of abstracted location classes.")
  private boolean preciseLocations = false;*/

  /** Number of threads. */
  private int threadNo;

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

  private final Map<Pair<RGEnvCandidate, Set<AbstractionPredicate>>, RGEnvTransition> candidateToTransitionCache;


  public RGEnvironmentManager(ParallelCFAS pcfa, Configuration config, LogManager logger) throws InvalidConfigurationException{
    config.inject(this, RGEnvironmentManager.class);

    this.threadNo = pcfa.getThreadNo();
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

    if (cacheGeneratingEnvTransition){
      candidateToTransitionCache = new HashMap<Pair<RGEnvCandidate, Set<AbstractionPredicate>>, RGEnvTransition>();
    } else {
      candidateToTransitionCache = null;
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
  public List<RGEnvCandidate> findMostGeneralCandidates(Collection<RGEnvCandidate> candidates, RGLocationMapping lm){
    stats.candidateTimer.start();

    Vector<RGEnvCandidate> newToProcess = new Vector<RGEnvCandidate>(candidates);
    Vector<RGEnvCandidate> newCovered = new Vector<RGEnvCandidate>(candidates.size());

    /* remove candidates whose application wouldn't make sense */
    for (RGEnvCandidate cnd : newToProcess){
      if (candManager.isBottom(cnd, lm)){
        newCovered.add(cnd);

        if (debug && false){
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
            if (candManager.isLessOrEqual(cnd1, cnd2, lm)){
              // edge1 => edge2
              if (debug && false){
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


    /* sanity check on request */
    if (debug){
      checkMostGeneralCandidates(candidates, newToProcess, lm);
    }

    stats.candidateTimer.stop();
    return  newToProcess;
  }

  /**
   * Checks result of {@link #getMostGeneralCandidates()}.
   * @param oldMGCandidates
   * @param newCandidates
   * @param newMGCandidates
   * @param lm
   */
  private void checkMostGeneralCandidates(Collection<RGEnvCandidate> newCandidates, Collection<RGEnvCandidate> newMGCandidates, RGLocationMapping lm){
    stats.checkingTimer.start();

    // new m.g. candidates are incomperable
    for (RGEnvCandidate cand1 : newMGCandidates){
      for (RGEnvCandidate cand2 : newMGCandidates){
        if (cand1 != cand2){
          if (candManager.isLessOrEqual(cand1, cand2, lm)){
            System.out.println(this.getClass());
          }
          if (candManager.isLessOrEqual(cand2, cand1, lm)){
            System.out.println();
          }
          assert !candManager.isLessOrEqual(cand2, cand1, lm);
          assert !candManager.isLessOrEqual(cand1, cand2, lm);
        }
      }
    }

    // for every candidate in newCandidates there exist greater of equal candidate in newMGCandidates
    for (RGEnvCandidate nCand : newCandidates){
      boolean existsGeq = false;
      for (RGEnvCandidate mgCand : newMGCandidates){
        if (candManager.isLessOrEqual(nCand, mgCand, lm)){
          existsGeq = true;
          break;
        }
      }

      assert existsGeq || candManager.isBottom(nCand, lm);
    }
    stats.checkingTimer.stop();
  }


  /**
   * Returns the most general env. transitions generated from the candidates using the precision.
   * @param candidates
   * @param preds
   * @return
   */
  private List<RGEnvTransition> findMostGeneralEnvTransitions(Collection<RGEnvCandidate> candidates, Set<AbstractionPredicate> preds, RGLocationMapping lm){

    /* abstract the candidates */
    Vector<RGEnvTransition> newEt   = new Vector<RGEnvTransition>(candidates.size());
    for (RGEnvCandidate cand : candidates){
      RGEnvTransition et = generateEnvTransition(cand, preds, lm);
      //RGEnvTransition et = etManager.generateEnvTransition(cand, preds);
      newEt.add(et);
    }

    newEt = findMostGeneralTransitions(newEt);

    //stats.maxValid = Math.max(stats.maxValid, newEt.size());
    stats.maxApplied = stats.maxApplied >= newEt.size() ? stats.maxApplied : newEt.size();
    return newEt;
  }


  private RGEnvTransition generateEnvTransition(RGEnvCandidate cand, Set<AbstractionPredicate> preds, RGLocationMapping lm) {
    stats.etGenerated++;
    RGEnvTransition et;

    if (cacheGeneratingEnvTransition){
      Pair<RGEnvCandidate, Set<AbstractionPredicate>> key = Pair.of(cand, preds);
      et = candidateToTransitionCache.get(key);

      if (et == null){
        et = etManager.generateEnvTransition(cand, preds, lm);
        candidateToTransitionCache.put(key, et);
      } else {
        stats.cand2EtCacheHits++;
      }
    }
    else {
      et = etManager.generateEnvTransition(cand, preds, lm);
    }

    return et;
  }


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

      for (ARTElement parent : element.getLocalParents()){
        if (!visisted.contains(parent)){
          toProcess.addLast(parent);
        }
      }
    }

    return laARTElement;
  }


  /**
   * Find new env. transitions that should be applied.
   * @param elem env. transitions previously applied
   * @param allCandidates all candidates from source thread
   * @param preds predicates for env. transitions
   * @return
   */
  public List<RGEnvTransition> getEnvironmentalTransitionsToApply(ARTElement elem, List<RGEnvCandidate> allCandidates, ARTPrecision prec) {
    stats.etToApplyTimer.start();

    RGLocationMapping lm = prec.getLocationMapping();

    // find most general candidates w.r.t to the location mapping
    List<RGEnvCandidate> candidates = findMostGeneralCandidates(allCandidates, lm);

    // find concreate locatino that the element may belong to
    SetMultimap<Integer, CFANode> cLocsElem = LinkedHashMultimap.create();
    ImmutableMap<Integer, Integer> locCl = elem.getLocationClasses();
    CFANode loc = elem.retrieveLocationElement().getLocationNode();

    for (int i=0; i<threadNo; i++){
      if (i == elem.getTid()){
        cLocsElem.put(i, loc);
      } else {
        Integer classNo = locCl.get(i);
        Collection<CFANode> nodes = lm.classToNodes(classNo);
        assert !nodes.isEmpty();
        cLocsElem.putAll(i, nodes);
      }
    }


    // filter out candidates with mistmatching location classes
    Collection<RGEnvCandidate> covered = new Vector<RGEnvCandidate>();

    for (RGEnvCandidate cand : candidates){
      SetMultimap<Integer, CFANode> cLocsCand = cand.getConcreateLocations();

      for (int i=0; i<threadNo; i++){
        Set<CFANode> s1 = cLocsElem.get(i);
        Set<CFANode> s2 = cLocsCand.get(i);
        SetView<CFANode> inter = Sets.intersection(s1, s2);
        if (inter.isEmpty()){
          covered.add(cand);
          break;
        }
      }
    }

    candidates.removeAll(covered);

    RGPrecision rgPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
    Set<AbstractionPredicate> preds = new HashSet<AbstractionPredicate>(rgPrec.getEnvGlobalPredicates());
    preds.addAll(rgPrec.getEnvPredicateMap().get(loc));

    List<RGEnvTransition> newTransitions = findMostGeneralEnvTransitions(candidates, preds, lm);
    Set<RGEnvTransition> oldTransitions = elem.getEnvTransitionsApplied();
    List<RGEnvTransition> newToApply = getDifference(newTransitions, oldTransitions);

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



