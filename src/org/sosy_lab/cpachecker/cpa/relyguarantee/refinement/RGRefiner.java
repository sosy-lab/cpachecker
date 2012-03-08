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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import org.sosy_lab.cpachecker.cfa.ParallelCFAS;
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
import org.sosy_lab.cpachecker.cpa.art.ARTPrecision;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGPrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.pivots.Pivots;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.pivots.RGLazyAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSetMultimap.Builder;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;


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
  private boolean lazy = true;

  private final RGRefinementManager<?, ?> refManager;
  private final RGLocationRefinementManager locrefManager;
  private final RGLazyAbstractionManager lazyManager;

  private final ARTCPA[] artCpas;
  private ParallelCFAS pcfa;
  private final int threadNo;
  // TODO remove
  private RGAlgorithm algorithm;

  private final Stats stats;





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

    this.threadNo     = cpas.length;
    this.algorithm    = pAlgorithm;
    this.pcfa        = this.algorithm.getPcfa();

    this.stats        = new Stats();

    RGCPA rgCPA = artCpas[0].retrieveWrappedCpa(RGCPA.class);
    if (rgCPA != null){
      refManager = rgCPA.getRelyGuaranteeManager();
      locrefManager = rgCPA.getLocrefManager();
      lazyManager  = new RGLazyAbstractionManager(locrefManager);
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
    InterpolationTreeResult cexample = refManager.refine(targetElement, reachedSets, errorThr);
    cexample = locrefManager.refine(cexample);

    // if error is spurious refine
    if (cexample.isSpurious()) {

      if (lazy){
        performLazyRefinement(artReachedSets, cexample, algorithm);
      } else {
        performRestartingRefinement(artReachedSets, cexample, algorithm);


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


  private void performLazyRefinement(ARTReachedSet[] reachedSets, InterpolationTreeResult cexample, RGAlgorithm algorithm) throws RefinementFailedException {
    assert cexample.isSpurious();
    System.out.println();
    System.out.println("\t\t\t ----- Lazy refinement -----");

    Map<Integer, Map<ARTElement, Precision>> refMap = lazyManager.getRefinedElements(reachedSets, cexample);

    dropPivots(reachedSets, refMap);
    algorithm.removeDestroyedCandidates();
  }



  private void performRestartingRefinement(ARTReachedSet[] reachedSets, InterpolationTreeResult cexample, RGAlgorithm algorithm) throws RefinementFailedException {
    assert cexample.isSpurious();
    System.out.println();
    System.out.println("\t\t\t ----- Restarting refinement -----");

    boolean dataRef = cexample.getPathRefinementMap().isEmpty();

    Map<Integer, Map<ARTElement, Precision>> refMap;

    if (dataRef){
      refMap = getRestartingDataPrecision(reachedSets, cexample);
    } else {
      refMap = getRestartingLocationPrecision(reachedSets, cexample);
    }

    dropPivots(reachedSets, refMap);
    algorithm.resetEnvironment();
  }



  private void dropPivots(ARTReachedSet[] reachedSets, Map<Integer, Map<ARTElement, Precision>> refMap) {


    // drop subtrees and change precision
    for(int tid : refMap.keySet()){

      if (debug){
        System.out.println("Pivots and precision in thread "+tid);
      }

      for(ARTElement root : refMap.get(tid).keySet()){
        Precision precision = refMap.get(tid).get(root);
        Set<ARTElement> parents = new HashSet<ARTElement>(root.getLocalParents());

        if (debug){
          System.out.println("\t-"+root+",\n\t "+precision+"\n");
        }

        // TODO why does it take so long?
        reachedSets[tid].removeSubtree(root, precision);
      }
    }
  }



/*
  private DataPivots getDataPivots(ARTReachedSet[] reachedSets, InterpolationTreeResult cexample) {

    InterpolationTree tree = cexample.getTree();
    InterpolationTreeNode root = tree.getRoot();
    ARTElement rootElement = root.getArtElement();
    int rootTid = root.getTid();
    ARTPrecision rootPrec = (ARTPrecision) reachedSets[rootTid].getPrecision(rootElement);

    DataPivots pivots = new DataPivots();

    SetMultimap<InterpolationTreeNode, AbstractionPredicate> artMap = cexample.getArtRefinementMap();

    for (InterpolationTreeNode node : artMap.keySet()){
      ARTElement aelem = node.getArtElement();
      Set<AbstractionPredicate> itps = artMap.get(node);
      CFANode loc = aelem.retrieveLocationElement().getLocationNode();
      int tid = aelem.getTid();

      Precision prec = reachedSets[tid].getPrecision(aelem);
      RGPrecision rgPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
      HashSet<AbstractionPredicate> artPredicates = new HashSet<AbstractionPredicate>(rgPrec.getARTGlobalPredicates());
      artPredicates.addAll(rgPrec.getARTPredicateMap().get(loc));

      // add any pivot with interpolants
      if (!itps.isEmpty()){
        ARTElement laElem = RGCPA.findLastAbstractionARTElement(aelem);
        pivots.addPivotWithARTPredicates(tid, laElem, itps);
      }
    }

    SetMultimap<InterpolationTreeNode, AbstractionPredicate> envMap = cexample.getEnvRefinementMap();

    for (InterpolationTreeNode node : envMap.keySet()){
      ARTElement aelem = node.getArtElement();
      Set<AbstractionPredicate> itps = envMap.get(node);
      CFANode loc = aelem.retrieveLocationElement().getLocationNode();
      int tid = aelem.getTid();

      Precision prec = reachedSets[tid].getPrecision(aelem);
      RGPrecision rgPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
      HashSet<AbstractionPredicate> envPredicates = new HashSet<AbstractionPredicate>(rgPrec.getEnvGlobalPredicates());
      envPredicates.addAll(rgPrec.getEnvPredicateMap().get(loc));

      // add any pivot with interpolants
      if (!itps.isEmpty()){
        ARTElement laElem = RGCPA.findLastAbstractionARTElement(aelem);
        pivots.addPivotWithEnvPredicates(tid, laElem, itps);
      }
    }

    return pivots;

  }

*/
  /*
   * Get ART elements for which interpolants have been discovered. Optionally,
   * add only elements that don't have the interpolants in their precision yet.
   * @param reachedSets
   * @param info
   * @param onlyNewPrec
   * @return

  private Pivots getNewPrecisionElements(ARTReachedSet[] reachedSets, InterpolationTreeResult info, boolean onlyNewPrec) {

    Pivots pivots = new Pivots();

    SetMultimap<InterpolationTreeNode, AbstractionPredicate> artMap = info.getArtRefinementMap();

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
      if (onlyNewPrec && !artPredicates.containsAll(itps)){
        ARTElement laElem = RGCPA.findLastAbstractionARTElement(aelem);
        pivots.addPivotWithARTPredicates(tid, laElem, itps);
      }

      // add any pivot with interpolants
      if (!onlyNewPrec && !itps.isEmpty()){
        ARTElement laElem = RGCPA.findLastAbstractionARTElement(aelem);
        pivots.addPivotWithARTPredicates(tid, laElem, itps);
      }
    }

    SetMultimap<InterpolationTreeNode, AbstractionPredicate> envMap = info.getEnvRefinementMap();

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
      if (onlyNewPrec && !envPredicates.containsAll(itps)){
        ARTElement laElem = RGCPA.findLastAbstractionARTElement(aelem);
        pivots.addPivotWithEnvPredicates(tid, laElem, itps);
      }

      // add any pivot with interpolants
      if (!onlyNewPrec && !itps.isEmpty()){
        ARTElement laElem = RGCPA.findLastAbstractionARTElement(aelem);
        pivots.addPivotWithEnvPredicates(tid, laElem, itps);
      }
    }

    return pivots;
  }*/

  /*
   * Find pivots nodes and their new precision.
   * @param pReachedSets
   * @param pReachedSets
   * @param pCexample
   * @return
   * @throws RefinementFailedException

  private LocationPivots getLocationPivots(ARTReachedSet[] reachedSets, InterpolationTreeResult info) throws RefinementFailedException {

    InterpolationTreeNode root = info.getTree().getRoot();
    int errorTid = root.getTid();
    ARTElement errorElem = root.getArtElement();
    LocationPivots pivots = new LocationPivots(errorTid);

    // add the error node as a pivot with the mistmatches from its location mapping
    ARTPrecision errorPrec = (ARTPrecision) reachedSets[errorTid].getPrecision(errorElem);
    RGLocationMapping errorLM = errorPrec.getLocationMapping();
    pivots.addPivotWithLocationMistmatch(errorElem, errorLM.getMismatchesForPath());

    // for every path we take one pair of mistmatching location (e.g. the first one)
     Map<Path, List<Pair<ARTElement, Pair<CFANode, CFANode>>>> pathRefMap = info.getPathRefinementMap();

    for (Path pi : pathRefMap.keySet()){

      Pair<ARTElement, Pair<CFANode, CFANode>> pivotAndMistmatch = pathRefMap.get(pi).get(0);
      ARTElement pivot = pivotAndMistmatch.getFirst();
      Pair<CFANode, CFANode> mistmatch = pivotAndMistmatch.getSecond();

      pivots.addPivotWithLocationMistmatch(pi, pivot, mistmatch);
    }

    return pivots;
  }*/




  /**
   * Add all predicates in the subtree of the element to its precision.
   * @param refMap
   * @param reachedSets
   * @return
   */
  private Map<Integer, Map<ARTElement, Precision>> addDroppedPrecision(
      Map<Integer, Map<ARTElement, Precision>> refMap,
      ARTReachedSet[] reachedSets) {

    HashMap<Integer, Map<ARTElement, Precision>> newRefMap = new HashMap<Integer, Map<ARTElement, Precision>>(refMap);

    for (int tid : newRefMap.keySet()){
      Map<ARTElement, Precision> threadRefMap = newRefMap.get(tid);

      for (ARTElement pivot : threadRefMap.keySet()){
        Set<ARTElement> subtree = pivot.getLocalSubtree();
        Set<RGPrecision> toMerge = new LinkedHashSet<RGPrecision>();
        toMerge.add((RGPrecision) threadRefMap.get(pivot));

        for (ARTElement sub : subtree){

          if (sub.isCovered()){
            continue;
          }
          Precision prec = reachedSets[tid].getPrecision(sub);
          RGPrecision rgPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
          toMerge.add(rgPrec);
        }

        RGPrecision mrgPrec = RGPrecision.merge(toMerge);

        if (debug){
          SetView<AbstractionPredicate> artDiff = Sets.difference(mrgPrec.getARTGlobalPredicates(), ((RGPrecision) threadRefMap.get(pivot)).getARTGlobalPredicates());
          SetView<AbstractionPredicate> envDiff = Sets.difference(mrgPrec.getEnvGlobalPredicates(), ((RGPrecision) threadRefMap.get(pivot)).getEnvGlobalPredicates());

          if (!artDiff.isEmpty()){
            System.out.println("shared "+artDiff);
          }

          if (!envDiff.isEmpty()){
            System.out.println("shared "+artDiff);
          }
        }

        threadRefMap.put(pivot, mrgPrec);
      }
    }


    return newRefMap;
  }

  /**
   * Compute new precision for the top elements. This precision includes the current precision and all new precision of the
   * covered elements.
   * @param topElems
   * @param pivots
   * @param reachedSets
   * @return
   */
  private Map<Integer, Map<ARTElement, Precision>> gatherPrecision(Map<Integer, SetMultimap<ARTElement, ARTElement>> topElems,
      Pivots pivots, ARTReachedSet[] reachedSets) {

    Map<Integer, Map<ARTElement, Precision>> refMap = new HashMap<Integer, Map<ARTElement, Precision>>(pivots.getTids().size());

    for (int tid : topElems.keySet()){
      SetMultimap<ARTElement, ARTElement> coverMap = topElems.get(tid);

      for (ARTElement topElem : coverMap.keySet()){

        Precision prec;
        if (pivots.isDataRefinement()){
          prec = gatherDataPrecisionForElement(topElem, coverMap.get(topElem), (DataPivots) pivots, reachedSets[tid]);
        } else {
          prec = gatherLocationPrecisionForElement(topElem, coverMap.get(topElem), (LocationPivots) pivots, reachedSets[tid]);
        }

        Map<ARTElement, Precision> threadCutoff = refMap.get(tid);
        if (threadCutoff == null){
          threadCutoff = new HashMap<ARTElement, Precision>();
          refMap.put(tid, threadCutoff);
        }

        threadCutoff.put(topElem, prec);
      }
    }

    return refMap;
  }



  /**
   * Compute new data precision for the element. This precision includes the current precision and all new precision of the
   * covered pivots.
   * @param topElem
   * @param covered
   * @param predicates
   * @param reachedSets
   * @return
   */
  private RGPrecision gatherDataPrecisionForElement(ARTElement topElem, Set<ARTElement> covered, DataPivots pivots, ARTReachedSet reachedSets) {
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
   * Compute new location mapping for the element. This precision includes the mistmatches of all the covered pivots.
   */
  private Precision gatherLocationPrecisionForElement(ARTElement topElem, Set<ARTElement> covered, LocationPivots pivots, ARTReachedSet artReachedSet) {

    int errorTid = topElem.getTid();

/*


    // find the unique highest abstract point, s.t. all ART elements are in its subtree
    Set<ARTElement> absElems = new HashSet<ARTElement>();
    for (ARTElement elem : mismatchMap.keySet()){
      ARTElement la = RGCPA.findLastAbstractionARTElement(elem);
      absElems.add(la);
    }

    SetMultimap<ARTElement, ARTElement> map = this.findTopARTElements(absElems);
    assert map.size() == 1;
    ARTElement top = map.keySet().iterator().next();

    // refine the location map at top using all mistmatching pairs
    int tid = top.getTid();
    ARTPrecision prec = (ARTPrecision) reachedSets[tid].getPrecision(top);
    RGLocationMapping oldLM = prec.getLocationMapping();
    Collection<Pair<CFANode, CFANode>> mismatchColl = mismatchMap.values();
    RGLocationMapping newLM = locrefManager.monotonicLocationMapping(oldLM, mismatchColl);

    ARTPrecision newPrec = new ARTPrecision(newLM, prec.getWrappedPrecision());
    Map<ARTElement, Precision> threadRefMap = new HashMap<ARTElement, Precision>();
    threadRefMap.put(top, newPrec);
    refMap.put(tid, threadRefMap);*/
    return null;
  }


  /*
   * Returns for each tread a map from a uncovered, top element to the element it covers.
   * @param pivots
   * @return

  private Map<Integer, SetMultimap<ARTElement, ARTElement>> findTopElements(Pivots pivots) {

    // tid -> element -> covered elements
    Map<Integer, SetMultimap<ARTElement, ARTElement>> covered = new HashMap<Integer, SetMultimap<ARTElement, ARTElement>>();

    for (int tid : pivots.getTids()){
      Set<ARTElement> threadPivots = pivots.getPivotsForThread(tid);

      SetMultimap<ARTElement, ARTElement> map = findTopARTElements(threadPivots);

      if (!map.isEmpty()){
        // all pivots are covered
        covered.put(tid, map);
      }

    }

    if (debug){
      for (int tid : pivots.getTids()){
        for (ARTElement pivot : pivots.getPivotsForThread(tid)){
          boolean existsTop = false;

          if (pivot.isCovered()){
            continue;
          }

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
*/

  /**
   * Computes new precision for the inital elements.
   * @param reachedSets
   * @param info
   * @return
   */
  private Map<Integer, Map<ARTElement, Precision>> getRestartingDataPrecision(ARTReachedSet[] reachedSets, InterpolationTreeResult info){
    stats.restartingTimer.start();

    boolean newPred = false;
    Map<Integer, Map<ARTElement, Precision>> refMap = new HashMap<Integer, Map<ARTElement, Precision>>(threadNo);

    // for every thread gather all new predicates in a single precision, which be added to the root
    for (int i=0; i<threadNo; i++){
      ARTElement initial = reachedSets[i].getFirstElement();
      Precision prec = reachedSets[i].getPrecision(initial);
      RGPrecision oldPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
      Pair<RGPrecision, Boolean> pair = gatherAllDataPredicatesForThread(info, oldPrec, i);
      RGPrecision newPrec = pair.getFirst();

      newPred = newPred || pair.getSecond();

      HashMap<ARTElement, Precision> threadRefMap = new HashMap<ARTElement, Precision>();
      refMap.put(i, threadRefMap);

      for (ARTElement child : initial.getLocalChildren()){
        threadRefMap.put(child, newPrec);
      }
    }

    if (debug){
      assert newPred : "No new predicates nor location mapping found.";
    }

    stats.restartingTimer.stop();
    return refMap;
  }


  /**
   * Construct a new location mapping for the children of the inital elements.
   * @param reachedSets
   * @param info
   * @return
   * @throws RefinementFailedException
   */
  private Map<Integer, Map<ARTElement, Precision>> getRestartingLocationPrecision(ARTReachedSet[] reachedSets, InterpolationTreeResult info) throws RefinementFailedException {
    stats.restartingTimer.start();

    Map<Integer, Map<ARTElement, Precision>> refMap = new HashMap<Integer, Map<ARTElement, Precision>>(threadNo);

    /* for every path we take one pair of mistmatching location (e.g. the first one);
       all paths belong to the same thread */
    ImmutableSetMultimap<Path, Pair<CFANode, CFANode>> mismatchPerPath = getMistmachesPerPath(info.getPathRefinementMap());

    // find new location mapping for the error thread, and copy top precision for the others
    int errorTid = info.getPathRefinementMap().values().iterator().next().get(0).getFirst().getTid();

    for (int i=0; i<threadNo; i++){
      ARTElement initial = reachedSets[i].getFirstElement();
      ARTPrecision prec = (ARTPrecision) reachedSets[i].getPrecision(initial);

      ARTPrecision newPrec;

      if (i == errorTid){
        RGLocationMapping lm = prec.getLocationMapping();
        RGLocationMapping newLM = locrefManager.monotonicLocationMapping(lm, mismatchPerPath);

        /*if (debug){
          System.out.println("New location mapping: "+newLM+"\n");
        }*/

        newPrec = new ARTPrecision(newLM, prec.getWrappedPrecision());
      } else {
        newPrec = prec;
      }


      HashMap<ARTElement, Precision> threadRefMap = new HashMap<ARTElement, Precision>();
      refMap.put(i, threadRefMap);

      for (ARTElement child : initial.getLocalChildren()){
        threadRefMap.put(child, newPrec);
      }
    }

    return refMap;
  }





  /**
   * Picks one pair of mistmatching locations for every path.
   * @param pathRefMap
   * @return
   */
  private ImmutableSetMultimap<Path, Pair<CFANode, CFANode>> getMistmachesPerPath(
      Map<Path, List<Pair<ARTElement, Pair<CFANode, CFANode>>>> pathRefMap) {

    Builder<Path, Pair<CFANode, CFANode>> bldr = ImmutableSetMultimap.<Path, Pair<CFANode, CFANode>>builder();

    for (Path pi : pathRefMap.keySet()){
      List<Pair<ARTElement, Pair<CFANode, CFANode>>> list = pathRefMap.get(pi);
      Pair<ARTElement, Pair<CFANode, CFANode>> triple = list.get(0);

      bldr = bldr.put(pi, triple.getSecond());
    }

    return bldr.build();
  }

  /**
   * Combine all predicates for thread i into a single precision. Return a pair of new precision
   * and a boolean value that is true if some new predicate was found (this is checked only in debugging mode).
   * @param pInfo
   * @param oldPrec
   * @param pI
   * @return
   */
  private Pair<RGPrecision, Boolean> gatherAllDataPredicatesForThread(InterpolationTreeResult itpResult, RGPrecision oldPrec, int i) {

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

    SetMultimap<InterpolationTreeNode, AbstractionPredicate> envMap = result.getEnvRefinementMap();
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

    SetMultimap<InterpolationTreeNode, AbstractionPredicate> artMap = result.getArtRefinementMap();

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
