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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.RGAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGPrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSetMultimap.Builder;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;


@Options(prefix="cpa.rg")
public class RGRefiner implements StatisticsProvider{

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(name="refinement.addPredicatesGlobally",
      description="refinement will add all discovered predicates "
          + "to all the locations in the abstract trace")
  private boolean addPredicatesGlobally = true;

  @Option(name="refinement.addEnvPredicatesGlobally",
      description="refinement will add all discovered predicates "
          + "to all the locations in the abstract trace")
  private boolean addEnvPredicatesGlobally = true;

  @Option(name="refinement.lazy",
      description="Use lazy refinement rather than restart the analysis.")
  private boolean lazy = false;


  private final Stats stats;
  private final RGRefinementManager<?, ?> refManager;
  private final RGLocationRefinementManager locrefManager;
  private final ARTCPA[] artCpas;

  private final  RGEnvironmentManager environment;
  private final int threadNo;

  // TODO remove
  private RGAlgorithm algorithm;
  private static RGRefiner singleton;

  /**
   * Singleton instance of RelyGuaranteeRefiner.
   * @param algorithm
   * @param cpas
   * @param rgEnvironment
   * @param pConfig
   * @return
   * @throws InvalidConfigurationException
   */
  public static RGRefiner getInstance(RGAlgorithm algorithm, final ConfigurableProgramAnalysis[] cpas, RGEnvironmentManager rgEnvironment, Configuration pConfig) throws InvalidConfigurationException {
    if (singleton == null){
      singleton = new RGRefiner(algorithm, cpas, rgEnvironment, pConfig);
    }
    return singleton;
  }

  public RGRefiner(RGAlgorithm pAlgorithm, final ConfigurableProgramAnalysis[] cpas, RGEnvironmentManager rgEnvironment, Configuration pConfig) throws InvalidConfigurationException{
    pConfig.inject(this, RGRefiner.class);
    artCpas = new ARTCPA[cpas.length];
    for (int i=0; i<cpas.length; i++){
      if (cpas[i] instanceof ARTCPA) {
        artCpas[i] = (ARTCPA) cpas[i];
      } else {
        throw new InvalidConfigurationException("ART CPA needed for refinement");
      }
    }

    this.threadNo = cpas.length;
    this.algorithm = pAlgorithm;
    this.environment = rgEnvironment;
    this.stats = new Stats();

    RGCPA rgCPA = artCpas[0].retrieveWrappedCpa(RGCPA.class);
    if (rgCPA != null){
      refManager = rgCPA.getRelyGuaranteeManager();
      locrefManager = rgCPA.getLocrefManager();
      //rgCPA.getConfiguration().inject(this, RelyGuaranteeRefiner.class);
    } else {
      throw new InvalidConfigurationException("RelyGuaranteeCPA needed for refinement");
    }

  }


  protected Path computePath(ARTElement pLastElement, ReachedSet pReached) throws InterruptedException, CPAException {
    return ARTUtils.getOnePathTo(pLastElement);
  }

  protected List<Triple<ARTElement, CFANode, RGAbstractElement>> transformPath(Path pPath) throws CPATransferException {
    List<Triple<ARTElement, CFANode, RGAbstractElement>> result = Lists.newArrayList();

    for (ARTElement ae : skip(transform(pPath, Pair.<ARTElement>getProjectionToFirst()), 1)) {
      RGAbstractElement pe = extractElementByType(ae, RGAbstractElement.class);
      if (pe instanceof AbstractionElement) {
        CFANode loc = AbstractElements.extractLocation(ae);
        result.add(Triple.of(ae, loc, pe));
      }
    }

    //assert pPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  /**
   *
   * @param pRelyGuaranteeEnvironment
   * @param pReachedSets
   * @param pErrorThr
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean performRefinment(ReachedSet[] reachedSets, RGEnvironmentManager environment, int errorThr) throws InterruptedException, CPAException {
    stats.totalTimer.start();

    int threadNo = reachedSets.length;


    //assert checkART(reachedSets[errorThr]);
    assert reachedSets[errorThr].getLastElement() instanceof ARTElement;
    ARTElement targetElement = (ARTElement) reachedSets[errorThr].getLastElement();
    assert targetElement.isTarget();

    ARTReachedSet[] artReachedSets = new ARTReachedSet[threadNo];
    for (int i=0; i<threadNo; i++){
      artReachedSets[i] = new ARTReachedSet(reachedSets[i], artCpas[i]);
    }
    System.out.println();
    System.out.println("\t\t\t ----- Interpolation -----");
    InterpolationTreeResult counterexampleInfo = refManager.refine(targetElement, reachedSets, errorThr);
    counterexampleInfo = locrefManager.refine(counterexampleInfo);

    // if error is spurious refine
    if (counterexampleInfo.isSpurious()) {

      if (lazy){
        performLazyRefinement(artReachedSets, counterexampleInfo, algorithm);
      } else {
        performRestartingRefinement(artReachedSets, counterexampleInfo, algorithm);


        for (int i=0; i<reachedSets.length;i++){
          assert reachedSets[i].getReached().size()==1;
        }
      }

      stats.totalTimer.stop();
      return true;
    } else {
      // a real error
      stats.totalTimer.stop();
      return false;
    }
  }


  private void performLazyRefinement(ARTReachedSet[] reachedSets, InterpolationTreeResult cexample, RGAlgorithm algorithm) {
    System.out.println();
    System.out.println("\t\t\t ----- Lazy refinement -----");
    Multimap<Integer, Pair<ARTElement, RGPrecision>> pivots = getLazyRefinement(reachedSets, cexample);
    dropPivots(reachedSets, pivots);
    algorithm.removeDestroyedCandidates();


  }

  private void performRestartingRefinement(ARTReachedSet[] reachedSets, InterpolationTreeResult cexample, RGAlgorithm algorithm) {
    // TODO Auto-generated method stub
    System.out.println();
    System.out.println("\t\t\t ----- Restarting refinement -----");

    Multimap<Integer, Pair<ARTElement, RGPrecision>> pivots = getRestartingPrecision(reachedSets, cexample);
    dropPivots(reachedSets, pivots);
    algorithm.resetEnvironment();
  }

  private void dropPivots(ARTReachedSet[] reachedSets, Multimap<Integer, Pair<ARTElement, RGPrecision>> pivots) {
    // drop subtrees and change precision
    for(int tid : pivots.keySet()){
      for(Pair<ARTElement, RGPrecision> pair : pivots.get(tid)){
        ARTElement root = pair.getFirst();
        // drop cut-off node in every thread
        RGPrecision precision = pair.getSecond();
        Set<ARTElement> parents = new HashSet<ARTElement>(root.getLocalParents());


        // TODO why does it take so long?
        reachedSets[tid].removeSubtree(root, precision);

        if (debug){
          for (ARTElement parent : parents){
            RGPrecision prec = Precisions.extractPrecisionByType(reachedSets[tid].getPrecision(parent), RGPrecision.class);
            System.out.println("Precision for thread "+tid+":");
            System.out.println("\t-"+prec);
          }
        }
      }
    }
  }

  /**
   * Finds cut-off points and new precision for them.
   * @param pReachedSets
   * @param pCounterexampleInfo
   * @return
   */
  private Multimap<Integer, Pair<ARTElement, RGPrecision>> getLazyRefinement(ARTReachedSet[] reachedSets, InterpolationTreeResult info) {

    //boolean newPred = false;
    //boolean newLM = false;

    // set location mapping
    RGLocationMapping lm = info.getRefinedLocationMapping();
    if (lm != null){
      //newLM = true;

      if (debug){
        System.out.println("New "+lm);
      }

      environment.setLocationMapping(lm);

      for (int i=0; i<artCpas.length; i++){
        ARTCPA artCpa = artCpas[i];
        artCpa.setLocationMapping(lm);
      }
    }


    Pivots pivots = getNewPrecisionElements(reachedSets, info);
    if (debug){
      Multimap<Integer, Integer> idMap = pivots.getPivotIds();
      for (int tid : pivots.getTids()){
        System.out.println("Thread "+tid+":");
        System.out.println("\t-new precision pivots: "+idMap.get(tid));
      }
      System.out.println();
    }

    pivots = getInterthreadImpact(pivots);

    if (debug){
      Multimap<Integer, Integer> idMap = pivots.getPivotIds();
      for (int tid : pivots.getTids()){
        System.out.println("Thread "+tid+":");
        System.out.println("\t-all pivots: "+idMap.get(tid));
      }
      System.out.println();
    }

    Map<Integer, SetMultimap<ARTElement, ARTElement>> topElems = findTopElements(pivots);

    if (debug){
      for (int tid : pivots.getTids()){
        System.out.println("Thread "+tid+":");
        System.out.print("\t-top pivots: ");

        Set<Integer> topIds = new HashSet<Integer>();
        for (ARTElement elem : topElems.get(tid).keySet()){
          topIds.add(elem.getElementId());
        }

        System.out.println(topIds);
      }
      System.out.println();
    }

    Multimap<Integer, Pair<ARTElement, RGPrecision>> result = gatherPrecision(topElems, pivots, reachedSets);

    if (debug){
      for (int tid : pivots.getTids()){
        System.out.println("Thread "+tid+":");

        for (Pair<ARTElement, RGPrecision>  pair : result.get(tid)){
          System.out.println("\t-"+pair.getFirst().getElementId()+" : "+pair.getSecond());
        }
      }
      System.out.println();
    }

    return result;
  }



  /**
   * Compute new precision for the top elements. This precision includes the current precision and all new precision of the
   * covered elements.
   * @param topElems
   * @param pivots
   * @param reachedSets
   * @return
   */
  private Multimap<Integer, Pair<ARTElement, RGPrecision>> gatherPrecision(Map<Integer, SetMultimap<ARTElement, ARTElement>> topElems,
      Pivots pivots, ARTReachedSet[] reachedSets) {

    Multimap<Integer, Pair<ARTElement, RGPrecision>> cutoff = HashMultimap.create();

    for (int tid : topElems.keySet()){
      SetMultimap<ARTElement, ARTElement> coverMap = topElems.get(tid);

      for (ARTElement topElem : coverMap.keySet()){
        RGPrecision prec = gatherPrecisionForElement(topElem, coverMap.get(topElem), pivots, reachedSets[tid]);
        cutoff.put(tid, Pair.of(topElem, prec));
      }
    }

    return cutoff;
  }

  /**
   * Compute new precision for the element. This precision includes the current precision and all new precision of the
   * covered elements.
   * @param topElem
   * @param covered
   * @param predicates
   * @param reachedSets
   * @return
   */
  private RGPrecision gatherPrecisionForElement(ARTElement topElem, Set<ARTElement> covered, Pivots pivots, ARTReachedSet reachedSets) {
    Builder<CFANode, AbstractionPredicate> artMapBldr = ImmutableSetMultimap.<CFANode, AbstractionPredicate>builder();
    com.google.common.collect.ImmutableSet.Builder<AbstractionPredicate> artGlobalBldr =
        ImmutableSet.<AbstractionPredicate>builder();
    Builder<CFANode, AbstractionPredicate> envMapBldr = ImmutableSetMultimap.<CFANode, AbstractionPredicate>builder();
    com.google.common.collect.ImmutableSet.Builder<AbstractionPredicate> envGlobalBldr =
        ImmutableSet.<AbstractionPredicate>builder();

    // add exists precision of the top element
    Precision topPrec = reachedSets.getPrecision(topElem);
    assert topPrec != null;
    RGPrecision topRgPrec = Precisions.extractPrecisionByType(topPrec, RGPrecision.class);

    artMapBldr = artMapBldr.putAll(topRgPrec.getARTPredicateMap());
    artGlobalBldr = artGlobalBldr.addAll(topRgPrec.getARTGlobalPredicates());
    envMapBldr = envMapBldr.putAll(topRgPrec.getEnvPredicateMap());
    envGlobalBldr = envGlobalBldr.addAll(topRgPrec.getEnvGlobalPredicates());

    Set<ARTElement> toInclude = new HashSet<ARTElement>(covered);
    toInclude.add(topElem);

    for (ARTElement elem : toInclude){
      if (elem == null){
        continue;
      }
      CFANode loc = elem.retrieveLocationElement().getLocationNode();
      Set<AbstractionPredicate> artPreds = pivots.getARTPredicateForPivot(elem);
      if (artPreds == null){
        continue;
      }

      if (addPredicatesGlobally){
        artGlobalBldr = artGlobalBldr.addAll(artPreds);
      } else {
        artMapBldr = artMapBldr.putAll(loc, artPreds);
      }

      Set<AbstractionPredicate> envPreds = pivots.getEnvPredicateForPivot(elem);
      if (envPreds == null){
        continue;
      }

      if (addEnvPredicatesGlobally){
        envGlobalBldr = envGlobalBldr.addAll(envPreds);
      } else {
        envMapBldr = envMapBldr.putAll(loc, envPreds);
      }
    }

    return new RGPrecision(artMapBldr.build(), artGlobalBldr.build(), envMapBldr.build(), envGlobalBldr.build());
  }


  /**
   * Returns for each tread a map from a uncovered, top element to the element it covers.
   * @param pivots
   * @return
   */
  private Map<Integer, SetMultimap<ARTElement, ARTElement>> findTopElements(Pivots pivots) {

    // tid -> element -> covered elements
    Map<Integer, SetMultimap<ARTElement, ARTElement>> covered = new HashMap<Integer, SetMultimap<ARTElement, ARTElement>>();

    for (int tid : pivots.getTids()){
      Set<ARTElement> threadPivots = pivots.getPivotsForThread(tid);

      SetMultimap<ARTElement, ARTElement> map = covered.get(tid);
      if (map == null){
        map = LinkedHashMultimap.create();
        covered.put(tid, map);
      }

      for (ARTElement pivot : threadPivots){
        if (map.containsValue(pivot) || pivot.isCovered()){
          // pivot is covered
          continue;
        }

        map.put(pivot, null);

        Set<ARTElement> subtree = pivot.getLocalSubtree();

        for (ARTElement otherPivot : threadPivots){
          if (otherPivot.equals(pivot)){
            continue;
          }

          if (subtree.contains(otherPivot)){
            // pivot coveres otherPivot
            map.put(pivot, otherPivot);
            map.removeAll(otherPivot);
          }
        }

      }
    }

    if (debug){
      for (int tid : pivots.getTids()){
        for (ARTElement pivot : pivots.getPivotsForThread(tid)){
          boolean existsTop = false;

          for (ARTElement top : covered.get(tid).keySet()){
            if (top.equals(pivot)){
              existsTop = true;
              break;
            }

            if (covered.get(tid).get(top).contains(pivot)){
              assert top.getLocalSubtree().contains(pivot);
              existsTop = true;
              break;
            }
          }

          assert existsTop || pivot.isCovered();
        }
      }
    }

    return covered;
  }

  /**
   * If the subtree of some pivot generated an env. transition, then the abstraction point for its application
   * also becomes a pivot. This function return the least fixed point of this induction.
   * @param pivots
   * @return
   */
  private Pivots getInterthreadImpact(Pivots pivots) {
    Pivots unprocessed = pivots;
    Pivots allPivots = new Pivots();
    allPivots.putAll(unprocessed);

    while (!unprocessed.isEmpty()){

      Pivots newUnprocessed = new Pivots();

      for (int tid : unprocessed.getTids()){

        Set<ARTElement> elems = unprocessed.getPivotsForThread(tid);
        for (ARTElement abs : elems){
          Set<ARTElement> absElems = abs.getLocalSubtree();

          for (ARTElement aElem : absElems){
            ImmutableSet<ARTElement> envChildren = aElem.getEnvChildMap().keySet();

            for (ARTElement child : envChildren){
              int cTid = child.getTid();
              ARTElement la = RGCPA.findLastAbstractionARTElement(child);


              if (!allPivots.contains(la)){
                newUnprocessed.addPivotWithNoPredicates(cTid, la);
                allPivots.addPivotWithNoPredicates(cTid, la);
              }
            }
          }
        }
      }
      unprocessed = newUnprocessed;
      newUnprocessed = new Pivots();
    }

    return allPivots;
  }



  /**
   * Get pivots for which new precision has been discovered.
   * @param reachedSets
   * @param info
   * @return
   */
  private Pivots getNewPrecisionElements(ARTReachedSet[] reachedSets, InterpolationTreeResult info) {

    Pivots pivots = new Pivots();

    SetMultimap<InterpolationTreeNode, AbstractionPredicate> artMap = info.getArtMap();

    for (InterpolationTreeNode node : artMap.keySet()){
      ARTElement aelem = node.getArtElement();
      Set<AbstractionPredicate> itps = artMap.get(node);
      CFANode loc = aelem.retrieveLocationElement().getLocationNode();
      int tid = aelem.getTid();

      Precision prec = reachedSets[tid].getPrecision(aelem);
      RGPrecision rgPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
      HashSet<AbstractionPredicate> artPredicates = new HashSet<AbstractionPredicate>(rgPrec.getARTGlobalPredicates());
      artPredicates.addAll(rgPrec.getARTPredicateMap().get(loc));

      // check if the new precision is contained in the old one
      if (!artPredicates.containsAll(itps)){
        ARTElement laElem = RGCPA.findLastAbstractionARTElement(aelem);
        pivots.addPivotWithARTPredicates(tid, laElem, itps);
      }
    }

    SetMultimap<InterpolationTreeNode, AbstractionPredicate> envMap = info.getEnvMap();

    for (InterpolationTreeNode node : envMap.keySet()){
      ARTElement aelem = node.getArtElement();
      Set<AbstractionPredicate> itps = envMap.get(node);
      CFANode loc = aelem.retrieveLocationElement().getLocationNode();
      int tid = aelem.getTid();

      Precision prec = reachedSets[tid].getPrecision(aelem);
      RGPrecision rgPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
      HashSet<AbstractionPredicate> envPredicates = new HashSet<AbstractionPredicate>(rgPrec.getEnvGlobalPredicates());
      envPredicates.addAll(rgPrec.getEnvPredicateMap().get(loc));

      // check if the new precision is contained in the old one
      if (!envPredicates.containsAll(itps)){
        ARTElement laElem = RGCPA.findLastAbstractionARTElement(aelem);
        pivots.addPivotWithEnvPredicates(tid, laElem, itps);
      }
    }

    return pivots;
  }


  /**
   * Computes new precision for the inital elements.
   * @param reachedSets
   * @param info
   * @return
   */
  private Multimap<Integer, Pair<ARTElement, RGPrecision>> getRestartingPrecision(ARTReachedSet[] reachedSets, InterpolationTreeResult info){
    stats.restartingTimer.start();

    boolean newPred = false;
    boolean newLM = false;

    // set location mapping
    RGLocationMapping lm = info.getRefinedLocationMapping();
    if (lm != null){
      newLM = true;

      if (debug){
        System.out.println("New "+lm);
      }

      environment.setLocationMapping(lm);

      for (int i=0; i<artCpas.length; i++){
        ARTCPA artCpa = artCpas[i];
        artCpa.setLocationMapping(lm);
      }
     }


    Multimap<Integer, Pair<ARTElement, RGPrecision>> refMap = LinkedHashMultimap.create();

    // for every thread gather all new predicates in a single precision, which be added to the root
    for (int i=0; i<threadNo; i++){
      ARTElement initial = reachedSets[i].getFirstElement();
      Precision prec = reachedSets[i].getPrecision(initial);
      RGPrecision oldPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
      Pair<RGPrecision, Boolean> pair = gatherAllPredicatesForThread(info, oldPrec, i);
      RGPrecision newPrec = pair.getFirst();

      newPred = newPred || pair.getSecond();

      for (ARTElement child : initial.getLocalChildren()){
        refMap.put(i, Pair.of(child, newPrec));
      }
    }

    if (debug){
      assert newPred || newLM : "No new predicates nor location mapping found.";
    }

    stats.restartingTimer.stop();
    return refMap;
  }




  /**
   * Combine all predicates for thread i into a single precision. Return a pair of new precision
   * and a boolean value that is true if some new predicate was found (this is checked only in debugging mode).
   * @param pInfo
   * @param oldPrec
   * @param pI
   * @return
   */
  private Pair<RGPrecision, Boolean> gatherAllPredicatesForThread(InterpolationTreeResult itpResult, RGPrecision oldPrec, int i) {

    ImmutableSetMultimap<CFANode, AbstractionPredicate> artPredicateMap;
    ImmutableSet<AbstractionPredicate> artGlobalPredicates;
    ImmutableSetMultimap<CFANode, AbstractionPredicate> envPredicateMap;
    ImmutableSet<AbstractionPredicate> envGlobalPredicates;

    /* predicates that didn't appear before, useful for debugging */
    Multimap<CFANode, AbstractionPredicate> dARTPred = null;
    Set<AbstractionPredicate> dARTGlobal = null;
    Multimap<CFANode, AbstractionPredicate> dEnvPred = null;
    Set<AbstractionPredicate> dEnvGlobal = null;

    /* gather ART predicates */
    Multimap<CFANode, AbstractionPredicate> newArtPredicates = gatherARTPredicates(itpResult, i);

    if (addPredicatesGlobally){
      artPredicateMap = oldPrec.getARTPredicateMap();

      com.google.common.collect.ImmutableSet.Builder<AbstractionPredicate> artGlobalBldr = ImmutableSet.builder();
      artGlobalBldr = artGlobalBldr.addAll(oldPrec.getARTGlobalPredicates());
      artGlobalBldr = artGlobalBldr.addAll(newArtPredicates.values());
      artGlobalPredicates = artGlobalBldr.build();

      if (debug){
        dARTGlobal = new HashSet<AbstractionPredicate>(newArtPredicates.values());
        dARTGlobal.removeAll(oldPrec.getARTGlobalPredicates());
      }

    } else {
      artGlobalPredicates = oldPrec.getARTGlobalPredicates();

      Builder<CFANode, AbstractionPredicate> artPredicateBldr = ImmutableSetMultimap.builder();
      artPredicateBldr = artPredicateBldr.putAll(oldPrec.getARTPredicateMap());
      artPredicateBldr = artPredicateBldr.putAll(newArtPredicates);
      artPredicateMap = artPredicateBldr.build();

      if (debug){
        newArtPredicates.removeAll(oldPrec.getARTPredicateMap());
        dARTPred = newArtPredicates;
      }
    }

    /* gather env. predicates */
    Multimap<CFANode, AbstractionPredicate> newEnvPredicates = gatherEnvPredicates(itpResult, i);

    if (addEnvPredicatesGlobally){
      envPredicateMap = oldPrec.getEnvPredicateMap();

      com.google.common.collect.ImmutableSet.Builder<AbstractionPredicate> envGlobalBldr = ImmutableSet.builder();
      envGlobalBldr = envGlobalBldr.addAll(oldPrec.getEnvGlobalPredicates());
      envGlobalBldr = envGlobalBldr.addAll(newEnvPredicates.values());
      envGlobalPredicates = envGlobalBldr.build();

      environment.addPredicatesToEnvPrecision(i, null, newEnvPredicates.values());

      if (debug){
        dEnvGlobal = new HashSet<AbstractionPredicate>(newEnvPredicates.values());
        dEnvGlobal.removeAll(oldPrec.getEnvGlobalPredicates());
      }
    } else {
      envGlobalPredicates = oldPrec.getEnvGlobalPredicates();

      Builder<CFANode, AbstractionPredicate> envPredicateBldr = ImmutableSetMultimap.builder();
      envPredicateBldr = envPredicateBldr.putAll(oldPrec.getEnvPredicateMap());
      envPredicateBldr = envPredicateBldr.putAll(newEnvPredicates);
      envPredicateMap = envPredicateBldr.build();

      if (debug){
        newEnvPredicates.removeAll(oldPrec.getEnvPredicateMap());
        dEnvPred = newEnvPredicates;
      }
    }

    boolean newPred = false;

    if (debug){
      System.out.println("new predicates for thread "+i+":");

      if (dARTGlobal != null){
        for (AbstractionPredicate pred : dARTGlobal){
          System.out.println("\t- global ART: "+pred);
        }

        newPred = newPred || !dARTGlobal.isEmpty();
      }

      if (dARTPred != null){
        for (Entry<CFANode, AbstractionPredicate> entry : dARTPred.entries()){
          System.out.println("\t- local ART: "+entry.getKey() + "->" + entry.getValue());
        }

        newPred = newPred || !dARTPred.isEmpty();
      }

      if (dEnvGlobal != null){
        for (AbstractionPredicate pred : dEnvGlobal){
          System.out.println("\t- global env: "+pred);
        }

        newPred = newPred || !dEnvGlobal.isEmpty();
      }

      if (dEnvPred != null){
        for (Entry<CFANode, AbstractionPredicate> entry : dEnvPred.entries()){
          System.out.println("\t- local env: "+entry.getKey() + "->" + entry.getValue());
        }

        newPred = newPred || !dEnvPred.isEmpty();
      }

      System.out.println();
    }

    RGPrecision newPrec = new RGPrecision(artPredicateMap, artGlobalPredicates, envPredicateMap, envGlobalPredicates);

    return Pair.of(newPrec, newPred);
  }



  private Multimap<CFANode, AbstractionPredicate> gatherEnvPredicates(InterpolationTreeResult result, int i) {
    Multimap<CFANode, AbstractionPredicate> map = LinkedHashMultimap.create();

    SetMultimap<InterpolationTreeNode, AbstractionPredicate> envMap = result.getEnvMap();
    for (InterpolationTreeNode node : envMap.keySet()){
      ARTElement aElement = node.getArtElement();
      int tid = aElement.getTid();

      if (i == tid){
        CFANode loc = aElement.retrieveLocationElement().getLocationNode();
        map.putAll(loc, envMap.get(node));
      }

    }

    return map;
  }

  private Multimap<CFANode, AbstractionPredicate> gatherARTPredicates(InterpolationTreeResult result, int i) {
    Multimap<CFANode, AbstractionPredicate> map = LinkedHashMultimap.create();

    SetMultimap<InterpolationTreeNode, AbstractionPredicate> artMap = result.getArtMap();

    for (InterpolationTreeNode node : artMap.keySet()){
      ARTElement aElement = node.getArtElement();
      int tid = aElement.getTid();

      if (i == tid){
        CFANode loc = aElement.retrieveLocationElement().getLocationNode();
        map.putAll(loc, artMap.get(node));
      }

    }

    return map;
  }


  @Override
  public void collectStatistics(Collection<Statistics> scoll) {
    scoll.add(stats);
    refManager.collectStatistics(scoll);
    locrefManager.collectStatistics(scoll);
  }

  /**
   * Represents pivots - elements in the ART to be removed - and their new precision.
   */
  public static class Pivots  {

    // map : tid -> pivot state -> (new art predicates, new env predicates)
    private final Map<Integer, SetMultimap<ARTElement, AbstractionPredicate>> artPivots;
    private final Map<Integer, SetMultimap<ARTElement, AbstractionPredicate>> envPivots;
    private final SetMultimap<Integer, ARTElement> noPreds;

    public Pivots(){
      artPivots = new HashMap<Integer, SetMultimap<ARTElement, AbstractionPredicate>>();
      envPivots = new HashMap<Integer, SetMultimap<ARTElement, AbstractionPredicate>>();
      noPreds   = LinkedHashMultimap.create();
    }


    public Set<AbstractionPredicate> getARTPredicateForPivot(ARTElement elem) {
      int tid = elem.getTid();

      if (noPreds.containsEntry(tid, elem)){
        return Collections.emptySet();
      }

      SetMultimap<ARTElement, AbstractionPredicate> map = artPivots.get(tid);
      if (map == null){
        return Collections.emptySet();
      }
      Set<AbstractionPredicate> preds = new HashSet<AbstractionPredicate>(map.get(elem));
      preds.remove(null);

      return preds;
    }

    public Set<AbstractionPredicate> getEnvPredicateForPivot(ARTElement elem) {
      int tid = elem.getTid();

      if (noPreds.containsEntry(tid, elem)){
        return Collections.emptySet();
      }

      SetMultimap<ARTElement, AbstractionPredicate> map = envPivots.get(tid);
      if (map == null){
        return Collections.emptySet();
      }
      return map.get(elem);
    }


    public boolean contains(ARTElement elem) {
      int tid = elem.getTid();

      if (artPivots.get(tid) != null && artPivots.get(tid).containsKey(elem)){
        return true;
      }

      if (envPivots.get(tid) != null && envPivots.get(tid).containsKey(elem)){
        return true;
      }

      return false;
    }


    public Set<Integer> getTids() {
      Set<Integer> tids = new HashSet<Integer>(artPivots.keySet());
      tids.addAll(envPivots.keySet());
      tids.addAll(noPreds.keySet());

      return tids;
    }


    public boolean isEmpty() {
      return artPivots.isEmpty() && envPivots.isEmpty() && noPreds.isEmpty();
    }


    public void putAll(Pivots other) {
      // put art pivots
      for (int tid : other.artPivots.keySet()){
        addPivotsWithARTPredicates(tid, other.artPivots.get(tid));
      }

      // put env pivots
      for (int tid : other.envPivots.keySet()){
        addPivotsWithEnvPredicates(tid, other.envPivots.get(tid));
      }

    }

    public boolean addPivotsWithARTPredicates(int tid, SetMultimap<ARTElement, AbstractionPredicate> pSetMultimap) {
      SetMultimap<ARTElement, AbstractionPredicate> map = artPivots.get(tid);
      if (map == null){
        map = LinkedHashMultimap.create();
        artPivots.put(tid, map);
      }

      return map.putAll(pSetMultimap);
    }


    public boolean addPivotsWithEnvPredicates(int tid, SetMultimap<ARTElement, AbstractionPredicate> pSetMultimap) {
      assert pSetMultimap != null;

      SetMultimap<ARTElement, AbstractionPredicate> map = envPivots.get(tid);
      if (map == null){
        map = LinkedHashMultimap.create();
        envPivots.put(tid, map);
      }

      return map.putAll(pSetMultimap);
    }


    public boolean addPivotWithARTPredicates(int tid, ARTElement aelem, Collection<AbstractionPredicate> preds){

      SetMultimap<ARTElement, AbstractionPredicate> map = artPivots.get(tid);
      if (map == null){
        map = LinkedHashMultimap.create();
        artPivots.put(tid, map);
      }

      return map.putAll(aelem, preds);
    }

    public boolean addPivotWithNoPredicates(int tid, ARTElement aelem){
      assert artPivots.get(tid) == null || !artPivots.get(tid).containsKey(aelem);
      assert envPivots.get(tid) == null || !envPivots.get(tid).containsKey(aelem);
      return noPreds.put(tid, aelem);
    }

    public boolean addPivotWithEnvPredicates(int tid, ARTElement aelem, Collection<AbstractionPredicate> preds){
      SetMultimap<ARTElement, AbstractionPredicate> map = envPivots.get(tid);
      if (map == null){
        map = LinkedHashMultimap.create();
        envPivots.put(tid, map);
      }
      return map.putAll(aelem, preds);
    }

    public Set<ARTElement> getPivotsForThread(int tid){
      Set<ARTElement> pivots = new HashSet<ARTElement>();

      SetMultimap<ARTElement, AbstractionPredicate> artSet = artPivots.get(tid);
      if (artSet != null){
        pivots.addAll(artSet.keySet());
      }

     SetMultimap<ARTElement, AbstractionPredicate> envSet = envPivots.get(tid);
     if (envSet != null){
       pivots.addAll(envSet.keySet());
     }

     pivots.addAll(noPreds.get(tid));

     return pivots;
    }

    public Multimap<Integer, Integer> getPivotIds(){
      Multimap<Integer, Integer> mmap = LinkedHashMultimap.create();

      for (int tid : this.artPivots.keySet()){
        for (ARTElement elem : artPivots.get(tid).keySet()){
          mmap.put(tid, elem.getElementId());
        }
      }

      for (int tid : this.envPivots.keySet()){
        for (ARTElement elem : envPivots.get(tid).keySet()){
          mmap.put(tid, elem.getElementId());
        }
      }

      for (int tid : this.noPreds.keys()){
        for (ARTElement elem : noPreds.get(tid)){
          mmap.put(tid, elem.getElementId());
        }
      }

      return mmap;
    }

    @Override
    public String toString(){

      return this.getPivotIds().toString();
    }


  }


  public static class  Stats implements Statistics {

    public final Timer totalTimer          = new Timer();
    public final Timer restartingTimer     = new Timer();

    public int maxPredicatesPerLoc      = 0;

    @Override
    public String getName() {
      return "RGRefiners";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached){
      out.println("max. predicates per location     " + formatInt(maxPredicatesPerLoc));
      out.println("time on restarting analysis:     " + restartingTimer);
      out.println("total time on refinement:        " + totalTimer);
    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }
  }



}
