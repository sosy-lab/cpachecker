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
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ParallelCFAS;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.ComparatorWaitlist.EnvAppMinTopMin2;
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

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

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

  @Option(name="traversal.candidateComparator",
      description="Comparator that specifies which env. candidates should be explored first")
  private RGEnvCandidateComparator candCmp = RGEnvCandidateComparator.ARTID_MAX;

  @Option(name="traversal.etComparator",
      description="Comparator that specifies which env. transitions should be explored first")
  private RGEnvTransitionComparator etCmp = RGEnvTransitionComparator.ARTID_MAX;

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

  private final Map<Triple<RGEnvCandidate, Set<AbstractionPredicate>, RGLocationMapping>, RGEnvTransition> candidateToTransitionCache;


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
    this.candManager = new RGEnvCandidateManager(fManager, pfManager, absManager, ssaManager, tProver, rManager, pcfa, threadNo, config, logger);

    if (cacheGeneratingEnvTransition){
      candidateToTransitionCache = new HashMap<Triple<RGEnvCandidate, Set<AbstractionPredicate>, RGLocationMapping>, RGEnvTransition>();
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
    printCandidates("Enviornmental transitions generated:", candidates);
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
   * @param pImmutableMap
   * @param oldMGCandidates
   * @param newCandidates
   * @return
   */
  public List<RGEnvCandidate> findMostGeneralCandidates(List<RGEnvCandidate> toProcess,
      ARTElement element, ARTPrecision prec){

    if (toProcess.isEmpty()){
      return Collections.emptyList();
    }

    stats.candidateTimer.start();
    RGLocationMapping lm = prec.getLocationMapping();
    Vector<RGEnvCandidate> candidates = new Vector<RGEnvCandidate>(toProcess);

    /* Remove candidates whose application is pointless */
    List<RGEnvCandidate> toRemove = new Vector<RGEnvCandidate>(candidates.size());

    for (RGEnvCandidate cand : candidates){

      if (candManager.isBottom(cand, element, prec)){
        toRemove.add(cand);
      }
    }
    candidates.removeAll(toRemove);


    /* Find equivalence class for candidates defined by partial ordering of the manager;
     * sorted each class by the comparator. */
    TreeMultimap<RGEnvCandidate, RGEnvCandidate> eqClass =
        TreeMultimap.create(candCmp, candCmp);
    Set<RGEnvCandidate> dontprocess = new HashSet<RGEnvCandidate>();

    for (int i=0; i < candidates.size(); i++){
      RGEnvCandidate candI = candidates.get(i);

      if (dontprocess.contains(candI)){
        continue;
      }

      eqClass.put(candI, candI);

      for (int j=i+1; j < candidates.size(); j++){
        RGEnvCandidate candJ = candidates.get(j);

        if (dontprocess.contains(candJ)){
          continue;
        }


        boolean leq = candManager.isLessOrEqual(candI, candJ, lm);
        boolean geq = candManager.isLessOrEqual(candJ, candI, lm);

        if (leq && geq){
          // candI = candJ
          eqClass.put(candI, candJ);
          dontprocess.add(candJ);
        }
        else if (leq && !geq){
          // candI < candJ
          eqClass.removeAll(candI);
          break;
        }
        else if (!leq && geq){
          // candI > candJ
          dontprocess.add(candJ);
        }
      }
    }

    /* Pick the best candidate from every equivalence class */
    List<RGEnvCandidate> mostGeneral = new Vector<RGEnvCandidate>();

    for (RGEnvCandidate candClass : eqClass.keySet()){
      SortedSet<RGEnvCandidate> classMembers = eqClass.get(candClass);
      mostGeneral.add(classMembers.last());
    }



    /* sanity check on request */
    if (debug){
      checkMostGeneralCandidates(toProcess, mostGeneral, element, prec);
    }

    stats.candidateTimer.stop();
    return  mostGeneral;
  }

  /**
   * Checks result of {@link #getMostGeneralCandidates()}.
   * @param oldMGCandidates
   * @param newCandidates
   * @param newMGCandidates
   * @param lm
   */
  private void checkMostGeneralCandidates(Collection<RGEnvCandidate> newCandidates, Collection<RGEnvCandidate> newMGCandidates, ARTElement elem, ARTPrecision prec){
    stats.checkingTimer.start();

    RGLocationMapping lm = prec.getLocationMapping();

    // new m.g. candidates are incomperable
    for (RGEnvCandidate cand1 : newMGCandidates){
      for (RGEnvCandidate cand2 : newMGCandidates){
        if (cand1 != cand2){
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

      assert existsGeq || candManager.isBottom(nCand, elem, prec);
    }
    stats.checkingTimer.stop();
  }


  /**
   * Returns the most general env. transitions generated from the candidates using the precision.
   * @param candidates
   * @param preds
   * @return
   */
  private List<RGEnvTransition> findMostGeneralEnvTransitions(List<RGEnvCandidate> candidates, Set<AbstractionPredicate> preds, RGLocationMapping lm){

    /* abstract the candidates */
    List<RGEnvTransition> newEt   = new Vector<RGEnvTransition>(candidates.size());
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
      Triple<RGEnvCandidate, Set<AbstractionPredicate>, RGLocationMapping> key = Triple.of(cand, preds, lm);
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
  private void printEnvTransitions(String string, List<RGEnvTransition> rgEdges) {
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
  private void printCandidates(String string, Collection<RGEnvCandidate> pUnprocessedTransitions) {
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
  private List<RGEnvTransition> findMostGeneralTransitions(Collection<RGEnvTransition> transitions) {

    if (transitions.isEmpty()){
      return Collections.emptyList();
    }

    stats.etComparing.start();
    // candidates to be inspected
    Vector<RGEnvTransition> etToProcess = new Vector<RGEnvTransition>(transitions);
    // candidate that are covered or false
    Vector<RGEnvTransition> etCovered = new Vector<RGEnvTransition>();

    /* remove candidates whose application wouldn't make sense */
    for (RGEnvTransition et : etToProcess){
      if (etManager.isBottom(et)){
        etCovered.add(et);
      }
    }

    etToProcess.removeAll(etCovered);


    /* Find most general equivalence classes
     * and order elements using the environmental comparator. */
    // et -> equivalent environmental transitions
    TreeMultimap<RGEnvTransition, RGEnvTransition> eqClass =
        TreeMultimap.create(etCmp, etCmp);
    Set<RGEnvTransition> dontprocess = new HashSet<RGEnvTransition>();

    for (int i=0; i < etToProcess.size(); i++){
      RGEnvTransition eti = etToProcess.get(i);

      if (dontprocess.contains(eti)){
        continue;
      }

      eqClass.put(eti, eti);

      for (int j=i+1; j < etToProcess.size(); j++){
        RGEnvTransition etj = etToProcess.get(j);

        if (dontprocess.contains(etj)){
          continue;
        }

        boolean leq = etManager.isLessOrEqual(eti, etj);
        boolean geq = etManager.isLessOrEqual(etj, eti);

        if (leq && geq){
          // eti = etj
          eqClass.put(eti, etj);
          dontprocess.add(etj);
        }
        else if (leq && !geq){
          // eti < etj
          eqClass.removeAll(eti);
          break;
        }
        else if (!leq && geq){
          // eti > etj
          dontprocess.add(etj);
        }
      }
    }

    if (debug){
      // TODO remove this check if it never fails
      secondMethoCheck(eqClass, etToProcess);
    }

    /* use a comparator to pick one e.t. from every equivalence class */
    List<RGEnvTransition> mostGeneral = new Vector<RGEnvTransition>();

    for (RGEnvTransition etClass : eqClass.keySet()){
      SortedSet<RGEnvTransition> classMembers = eqClass.get(etClass);
      mostGeneral.add(classMembers.last());
    }

    /* sanity check on request */
    if (debug){
      checkMostGeneralTransitions(transitions, mostGeneral);
    }

    stats.etComparing.stop();
    return  mostGeneral;
  }



  private void secondMethoCheck(Multimap<RGEnvTransition, RGEnvTransition> eqClass,
      Vector<RGEnvTransition> etToProcess) {

    Vector<RGEnvTransition> etCovered = new Vector<RGEnvTransition>();

    /* find the most general candidates by comparing them */
    for (RGEnvTransition et1 : etToProcess){
      if (!etCovered.contains(et1)){
        for (RGEnvTransition et2 : etToProcess){
          if (et1 !=et2 && !etCovered.contains(et2)){
            if (etManager.isLessOrEqual(et1, et2)){
              // edge1 => edge2
              etCovered.add(et1);
              break;
            }
          }
        }
      }
    }

    List<RGEnvTransition> secondMethodMG = new Vector<RGEnvTransition>(etToProcess);
    secondMethodMG.removeAll(etCovered);

    if (debug){
      // remove this check
      for (RGEnvTransition et : eqClass.keySet()){
        boolean existsEq = false;

        for (RGEnvTransition secEt : secondMethodMG){
          boolean leq = etManager.isLessOrEqual(et, secEt);
          boolean geq = etManager.isLessOrEqual(secEt, et);
          assert !leq || geq;
          if (geq){
            existsEq = true;
            break;
          }
        }

        assert existsEq;
      }

      for (RGEnvTransition secEt : secondMethodMG){
        boolean existsEq = false;

        for (RGEnvTransition et : eqClass.keySet()){
          boolean geq = etManager.isLessOrEqual(et, secEt);
          boolean leq = etManager.isLessOrEqual(secEt, et);
          assert !leq || geq;
          if (geq){
            existsEq = true;
            break;
          }
        }

        assert existsEq;
      }
    }
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
    List<RGEnvCandidate> candidates = findMostGeneralCandidates(allCandidates, elem, prec);

    if (debug){
      printCandidates("Most general candidates:", candidates);
      System.out.println();
    }

    CFANode loc = elem.retrieveLocationElement().getLocationNode();
    RGPrecision rgPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
    Set<AbstractionPredicate> preds = new LinkedHashSet<AbstractionPredicate>(rgPrec.getEnvGlobalPredicates());
    preds.addAll(rgPrec.getEnvPredicateMap().get(loc));

    List<RGEnvTransition> newTransitions = findMostGeneralEnvTransitions(candidates, preds, lm);

    if (debug){
      this.printEnvTransitions("Env. transitions to apply", newTransitions);
      System.out.println();
    }

    List<RGEnvTransition> oldTransitions = elem.getEnvTransitionsApplied();
    List<RGEnvTransition> newToApply = getDifference(newTransitions, oldTransitions);

    if (debug){
      this.printEnvTransitions("Env. transitions to apply", newToApply);
      System.out.println();
    }

    stats.etToApplyTimer.stop();
    return newToApply;
  }

  /**
   * Returns new transitions that are not less or equal to any old transition.
   * @param newEt
   * @param oldEt
   * @return
   */
  private List<RGEnvTransition> getDifference(List<RGEnvTransition> newEt, List<RGEnvTransition> oldEt) {
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
   * Comparator for environmental transition. Maximizes topological number of their source.
   */
  public static class MaxTop implements Comparator<RGEnvTransition> {

    @Override
    public int compare(RGEnvTransition et1, RGEnvTransition et2) {
      Integer top1 = et1.getSourceARTElement().retrieveLocationElement().getLocationNode().getTopologicalSortId();
      Integer top2 = et2.getSourceARTElement().retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 > top2){
        return 1;
      }

      return -1;
    }
  }


  public static class EnvComparator implements Comparator<RGEnvTransition> {

    private Comparator<AbstractElement> elementComparator;

    public EnvComparator(){
      elementComparator = new EnvAppMinTopMin2();
    }


    @Override
    public int compare(RGEnvTransition et1, RGEnvTransition et2) {

      if (et1.equals(et2)){
        return 0;
      }

      ARTElement s1 = et1.getSourceARTElement();
      ARTElement s2 = et2.getSourceARTElement();

      int b1 = s1.getRefinementBranches();
      int b2 = s2.getRefinementBranches();

      if (b1 < b2){
        return 1;
      }

      if (b1 > b2){
        return -1;
      }

      int top1 = s1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      int top2 = s2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 > top2){
        return 1;
      }

      return -1;
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



