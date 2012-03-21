/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.lazy;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ParallelCFAS;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTPrecision;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGPrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.interpolation.InterpolationTreeNode;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.interpolation.InterpolationTreeResult;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.locations.RGLocationRefinementManager;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSetMultimap.Builder;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * Performs lazy abstraction for rely-guarantee analysis.
 */
@Options(prefix="cpa.rg")
public class RGLazyRefinementManager {

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(name="refinement.lazy.shareDroppedPrecision",
      description="Add all predicates in the subtree to the pivot element.")
  private boolean shareDroppedPrecision = false;

  @Option(name="refinement.addPredicatesGlobally",
      description="refinement will add all discovered predicates "
          + "to all the locations in the abstract trace")
  private boolean addPredicatesGlobally = true;

  @Option(name="refinement.addEnvPredicatesGlobally",
      description="refinement will add all discovered predicates "
          + "to all the locations in the abstract trace")
  private boolean addEnvPredicatesGlobally = true;

  private final RGLocationRefinementManager locrefManager;
  private final ParallelCFAS pcfa;

  public RGLazyRefinementManager(RGLocationRefinementManager pLocrefManager, ParallelCFAS pcfa, Configuration config) throws InvalidConfigurationException {
    config.inject(this, RGLazyRefinementManager.class);
    this.locrefManager  = pLocrefManager;
    this.pcfa           = pcfa;
  }


  /**
   * Finds cut-off points and new precision for them.
   * @param pReachedSets
   * @param pCounterexampleInfo
   * @return
   * @throws RefinementFailedException
   */
  public RGLazyRefinementResult getRefinedElements(ARTReachedSet[] reachedSets, InterpolationTreeResult info) throws RefinementFailedException {
    assert info.isSpurious();
    /*
     * The interpolation tree is an unwinding of the ART, so the same ART element may
     * appear several times with different predicates. Moreover, when an element
     * that generated an env. transition is dropped, then the abstract point above its
     * application also has to be dropped. To find the top pivots nodes and their precision
     * we first find all points to be dropped and then check which are the top ones.
     */

    boolean isDataRefinement = info.getPathRefinementMap().isEmpty();

    Pivots pivots;
    if (isDataRefinement){
      pivots = getInitalDataPivots2(reachedSets, info);
    } else {
      pivots = getInitalLocationPivots2(reachedSets, info);
    }

    if (debug){
      System.out.println("Inital pivots");
      System.out.println(pivots.toString());
      System.out.println();
    }

    addPivotsByEnvironmentalApplications(pivots);

    if (debug){
      System.out.println("Pivots with env. applications");
      System.out.println(pivots.toString());
      System.out.println();
    }

    addPivotsByCoverage(pivots);

    if (debug){
      System.out.println("Pivots with coverage pivots");
      System.out.println(pivots.toString());
      System.out.println();
    }


    pivots = mergePivotsIntoTop(pivots);

    if (debug){
      System.out.println("Merged pivots");
      System.out.println(pivots.toString());
      System.out.println();
    }

    addDroppedPrecision(reachedSets, pivots);

    if (debug){
      System.out.println("Pivots with dropped precision");
      System.out.println(pivots.toString());
      System.out.println();
    }

    Map<Pair<Integer, ARTElement>, Precision> precisionToAdjust =
        createPrecisionsToAdjust(pivots, reachedSets, isDataRefinement);

    SetMultimap<Integer, ARTElement> elementsToDrop = getElementsToDrop(pivots);

    RGLazyRefinementResult result = new RGLazyRefinementResult();
    result.addElementsToDrop(elementsToDrop);
    result.addPrecisionToAdjust(precisionToAdjust);


    if (debug){
      assert result.checkCorrectness();
      System.out.println(result);
      System.out.println();
    }


    return result;
  }


  /**
   * Creates a map that consists of ART elements of pivots.
   * @param pivots
   * @return
   */
  private SetMultimap<Integer, ARTElement> getElementsToDrop(Pivots pivots) {
    SetMultimap<Integer, ARTElement> map = LinkedHashMultimap.create();

    for (Integer tid : pivots.getTids()){

      for (Pivot piv : pivots.getPivotsForThread(tid)){

        map.put(tid, piv.getElement());
      }
    }

    return map;
  }


  /**
   * For a pivot A it adds a pivot with the same precision
   * for every node that is covered by an element in the subtree A.
   * @param pPivots
   */
  private void addPivotsByCoverage(Pivots pivots) {
   Pivots newPivots = new Pivots();
   //Multimap<Integer, Pivot> newPivots = LinkedHashMultimap.create();

    for (Integer tid : pivots.getTids()){

      for (Pivot piv : pivots.getPivotsForThread(tid)){
        ARTElement elem = piv.getElement();
        Set<ARTElement> subtree = piv.getLocalSubtree();

        for (ARTElement dropped : subtree){
         Set<ARTElement> covered = dropped.getCoveredByThis();

         for (ARTElement cov : covered){
           assert cov.isCovered();
           Pivot covPiv = new Pivot(cov);
           covPiv.addPrecisionOf(piv);
           newPivots.addPivot(covPiv);
         }
        }
      }
    }

    pivots.addAll(newPivots);
  }


  /**
   * For every pivot it adds the current precision of all elements in its subtree, including
   * the element itself.
   * @param reachedSets
   * @param pivots
   */
  private void addDroppedPrecision(ARTReachedSet[] reachedSets, Pivots pivots) {

    for (Integer tid : pivots.getTids()){

      for (Pivot piv : pivots.getPivotsForThread(tid)){

        Set<ARTElement> subtree = piv.getElement().getLocalSubtree();
        Set<ARTPrecision> seenPrecision = new HashSet<ARTPrecision>();

        for (ARTElement elem : subtree){

          if (elem.isCovered()){
            continue;
          }

          ARTPrecision prec = (ARTPrecision) reachedSets[tid].getPrecision(elem);

          if (!seenPrecision.contains(prec)){
            seenPrecision.add(prec);
            piv.addARTPrecision(prec);
          }
        }
      }
    }

    return;
  }


  /**
   * Creates precision for parents of pivots. The precision includes all precision
   * of pivot children.
   * @param pPivots
   * @param pReachedSets
   * @return
   * @throws RefinementFailedException
   */
  private Map<Pair<Integer, ARTElement>, Precision> createPrecisionsToAdjust(Pivots pivots,
      ARTReachedSet[] reachedSets, boolean isDataRefinement) throws RefinementFailedException {

    // readMap: elements that will be readded -> pivots that cause it
    SetMultimap<ARTElement, Pivot> readdMap = LinkedHashMultimap.create();

    for (Integer tid : pivots.getTids()){

      for (Pivot piv : pivots.getPivotsForThread(tid)){
        ARTElement aelem = piv.getElement();
        Set<ARTElement> parents = aelem.getLocalParents();

        for (ARTElement parent : parents){
          readdMap.put(parent, piv);
        }
      }
    }

    if (debug){
      System.out.println("Parent of a pivot -> Pivot");
      for (ARTElement elem : readdMap.keySet()){
        System.out.print("\t-"+elem.getElementId()+" : ");

        if (readdMap.get(elem).size() > 1){
          System.out.println(this.getClass());
        }

        for (Pivot piv : readdMap.get(elem)){
          System.out.print(piv.getElement().getElementId()+", ");
        }
        System.out.println();
      }
      System.out.println();
    }

    // create precision for parents
    Map<Pair<Integer, ARTElement>, Precision> precisionToAdjust =
        new LinkedHashMap<Pair<Integer, ARTElement>, Precision>();

    for (ARTElement parent : readdMap.keySet()){

      Precision prec;
      if (isDataRefinement){
        prec = mergeDataPrecision(readdMap.get(parent), parent, reachedSets);
      } else {
        prec = mergeLocationPrecision(readdMap.get(parent), parent, reachedSets);
      }
      Integer tid = parent.getTid();
      Pair<Integer, ARTElement> pair = Pair.of(tid, parent);
      precisionToAdjust.put(pair, prec);
    }

    return precisionToAdjust;
  }


  /**
   * Merges data and location precision of arguments and addits to the existing
   * precision of the element.
   * @param set
   * @param parent
   * @param reachedSets
   * @return
   * @throws RefinementFailedException
   */
  private Precision mergeLocationPrecision(Set<Pivot> set, ARTElement parent,
      ARTReachedSet[] reachedSets) throws RefinementFailedException {

    Builder<CFANode, AbstractionPredicate> artMapBldr =
        ImmutableSetMultimap.<CFANode, AbstractionPredicate>builder();
    com.google.common.collect.ImmutableSet.Builder<AbstractionPredicate> artGlobalBldr =
        ImmutableSet.<AbstractionPredicate>builder();
    Builder<CFANode, AbstractionPredicate> envMapBldr =
        ImmutableSetMultimap.<CFANode, AbstractionPredicate>builder();
    com.google.common.collect.ImmutableSet.Builder<AbstractionPredicate> envGlobalBldr =
        ImmutableSet.<AbstractionPredicate>builder();
    Builder<Path, Pair<CFANode, CFANode>> mismatchBldr =
        ImmutableSetMultimap.<Path, Pair<CFANode, CFANode>>builder();

    // add old precision for the elements
    ARTPrecision aprec = (ARTPrecision) reachedSets[parent.getTid()].getPrecision(parent);
    RGPrecision rgprec = Precisions.extractPrecisionByType(aprec, RGPrecision.class);

    artMapBldr    = artMapBldr.putAll(rgprec.getARTPredicateMap());
    artGlobalBldr = artGlobalBldr.addAll(rgprec.getARTGlobalPredicates());
    envMapBldr    = envMapBldr.putAll(rgprec.getEnvPredicateMap());
    envGlobalBldr = envGlobalBldr.addAll(rgprec.getEnvGlobalPredicates());
    mismatchBldr  = mismatchBldr.putAll(aprec.getLocationMapping().getMismatchesForPath());


    for (Pivot piv : set){

      if (this.addPredicatesGlobally){
        artGlobalBldr = artGlobalBldr.addAll(piv.getArtPredicateMap().values());
      } else {
        artMapBldr    = artMapBldr.putAll(piv.getArtPredicateMap());
      }

      artGlobalBldr   = artGlobalBldr.addAll(piv.getArtGlobalPredicates());

      if (this.addEnvPredicatesGlobally){
        envGlobalBldr = envGlobalBldr.addAll(piv.getEnvPredicatesMap().values());
      } else {
        envMapBldr    = envMapBldr.putAll(piv.getEnvPredicatesMap());
      }

      envGlobalBldr   = envGlobalBldr.addAll(piv.getEnvGlobalPredicates());


      mismatchBldr  = mismatchBldr.putAll(piv.getMismatchesPerPath());
    }

    RGLocationMapping newLM = locrefManager.monotonicLocationMapping(mismatchBldr.build(), parent.getTid());
    ARTPrecision newARTPrec = new ARTPrecision(newLM, aprec.getWrappedPrecision());
    RGPrecision newRgPrec = new RGPrecision(artMapBldr.build(), artGlobalBldr.build(),
        envMapBldr.build(), envGlobalBldr.build());
    Precision result = newARTPrec.replaceWrappedPrecision(newRgPrec, RGPrecision.class);
    assert result != null;

    return result;
  }

  /**
   * Builds precision by merging location mistmatches for the arguments and creating
   * new location mapping out of it.
   * @param elem
   * @param set
   * @param reachedSets
   * @return
   * @throws RefinementFailedException
   */
  private Precision mergeLocationPrecision2(Set<Pivot> set, ARTElement elem,
      ARTReachedSet[] reachedSets) throws RefinementFailedException {

    Builder<Path, Pair<CFANode, CFANode>> bldr = ImmutableSetMultimap.<Path, Pair<CFANode, CFANode>>builder();

    Integer tid = null;
    for (Pivot piv : set){

      if (tid == null){
        tid = piv.getTid();
      } else {
        assert tid.equals(piv.getTid());
      }

      bldr = bldr.putAll(piv.getMismatchesPerPath());
    }

    ARTPrecision oldPrec = (ARTPrecision) reachedSets[tid].getPrecision(elem);
    RGLocationMapping newLM = locrefManager.monotonicLocationMapping(bldr.build(), tid);
    ARTPrecision newPrec = new ARTPrecision(newLM, oldPrec.getWrappedPrecision());

    return newPrec;
  }


  /**
   * Builds precision by merging the data precision of the arguments.
   * @param set
   * @param parent
   * @param pReachedSets
   * @return
   */
  private RGPrecision mergeDataPrecision(Set<Pivot> set, ARTElement parent,
      ARTReachedSet[] reachedSets) {

    Builder<CFANode, AbstractionPredicate> artMapBldr =
        ImmutableSetMultimap.<CFANode, AbstractionPredicate>builder();
    com.google.common.collect.ImmutableSet.Builder<AbstractionPredicate> artGlobalBldr =
        ImmutableSet.<AbstractionPredicate>builder();
    Builder<CFANode, AbstractionPredicate> envMapBldr =
        ImmutableSetMultimap.<CFANode, AbstractionPredicate>builder();
    com.google.common.collect.ImmutableSet.Builder<AbstractionPredicate> envGlobalBldr =
        ImmutableSet.<AbstractionPredicate>builder();

    // add old precision for the elements
    ARTPrecision aprec = (ARTPrecision) reachedSets[parent.getTid()].getPrecision(parent);
    RGPrecision rgprec = Precisions.extractPrecisionByType(aprec, RGPrecision.class);

    artMapBldr    = artMapBldr.putAll(rgprec.getARTPredicateMap());
    artGlobalBldr = artGlobalBldr.addAll(rgprec.getARTGlobalPredicates());
    envMapBldr    = envMapBldr.putAll(rgprec.getEnvPredicateMap());
    envGlobalBldr = envGlobalBldr.addAll(rgprec.getEnvGlobalPredicates());

    for (Pivot piv : set){

      if (this.addPredicatesGlobally){
        artGlobalBldr = artGlobalBldr.addAll(piv.getArtPredicateMap().values());
      } else {
        artMapBldr    = artMapBldr.putAll(piv.getArtPredicateMap());
      }

      artGlobalBldr   = artGlobalBldr.addAll(piv.getArtGlobalPredicates());

      if (this.addEnvPredicatesGlobally){
        envGlobalBldr = envGlobalBldr.addAll(piv.getEnvPredicatesMap().values());
      } else {
        envMapBldr    = envMapBldr.putAll(piv.getEnvPredicatesMap());
      }

      envGlobalBldr   = envGlobalBldr.addAll(piv.getEnvGlobalPredicates());

    }


    RGPrecision prec = new RGPrecision(artMapBldr.build(), artGlobalBldr.build(),
        envMapBldr.build(), envGlobalBldr.build());

    return prec;
  }





  /**
   * Create a new precision for every pivot by adding old precison.
   * @param pPivots
   * @param pReachedSets
   * @return
   */
  private Map<Integer, Map<ARTElement, Precision>> createDataPrecision(Pivots pivots, ARTReachedSet[] reachedSets) {

    Map<Integer, Map<ARTElement, Precision>> refMap = new LinkedHashMap<Integer, Map<ARTElement, Precision>>();

    for (Integer tid : pivots.getTids()){

      Map<ARTElement, Precision> map = refMap.get(tid);
      if (map == null){
        //map = new LinkedHashMap<ARTElement, Precision>();
        map = new TreeMap<ARTElement, Precision>();
        refMap.put(tid, map);
      }

      for (Pivot piv : pivots.getPivotsForThread(tid)){
        Pivot dpiv = piv;

        ARTElement elem = piv.getElement();
        ARTPrecision oldPrec = (ARTPrecision) reachedSets[tid].getPrecision(elem);
        RGPrecision oldRgPrec = Precisions.extractPrecisionByType(oldPrec, RGPrecision.class);

        Builder<CFANode, AbstractionPredicate> artMapBldr =
            ImmutableSetMultimap.<CFANode, AbstractionPredicate>builder();
        com.google.common.collect.ImmutableSet.Builder<AbstractionPredicate> artGlobalBldr =
            ImmutableSet.<AbstractionPredicate>builder();
        Builder<CFANode, AbstractionPredicate> envMapBldr =
            ImmutableSetMultimap.<CFANode, AbstractionPredicate>builder();
        com.google.common.collect.ImmutableSet.Builder<AbstractionPredicate> envGlobalBldr =
            ImmutableSet.<AbstractionPredicate>builder();

        // add old predicates
        artMapBldr    = artMapBldr.putAll(oldRgPrec.getARTPredicateMap());
        artGlobalBldr = artGlobalBldr.addAll(oldRgPrec.getARTGlobalPredicates());
        envMapBldr    = envMapBldr.putAll(oldRgPrec.getEnvPredicateMap());
        envGlobalBldr = envGlobalBldr.addAll(oldRgPrec.getEnvGlobalPredicates());


        // add pivot precision
        if (this.addPredicatesGlobally){
          artGlobalBldr = artGlobalBldr.addAll(dpiv.getArtPredicateMap().values());
        } else {
          artMapBldr    = artMapBldr.putAll(dpiv.getArtPredicateMap());
        }

        artGlobalBldr   = artGlobalBldr.addAll(dpiv.getArtGlobalPredicates());

        if (this.addEnvPredicatesGlobally){
          envGlobalBldr = envGlobalBldr.addAll(dpiv.getEnvPredicatesMap().values());
        } else {
          envMapBldr    = envMapBldr.putAll(dpiv.getEnvPredicatesMap());
        }

        envGlobalBldr   = envGlobalBldr.addAll(dpiv.getEnvGlobalPredicates());


        Precision prec = new RGPrecision(artMapBldr.build(), artGlobalBldr.build(),
            envMapBldr.build(), envGlobalBldr.build());

        map.put(elem, prec);
      }
    }

    return refMap;
  }

  /**
   * Create a new precision for every pivot by adding old precison.
   * @param pPivots
   * @param pReachedSets
   * @return
   * @throws RefinementFailedException
   */
  private Map<Integer, Map<ARTElement, Precision>> createLocationPrecision(Pivots pivots,
      ARTReachedSet[] reachedSets) throws RefinementFailedException {

    Map<Integer, Map<ARTElement, Precision>> refMap = new LinkedHashMap<Integer, Map<ARTElement, Precision>>();

    for (Integer tid : pivots.getTids()){

      Map<ARTElement, Precision> map = refMap.get(tid);
      if (map == null){
        map = new LinkedHashMap<ARTElement, Precision>();
        refMap.put(tid, map);
      }

      for (Pivot piv : pivots.getPivotsForThread(tid)){

        ARTElement elem = piv.getElement();
        ARTPrecision oldPrec = (ARTPrecision) reachedSets[tid].getPrecision(elem);
        RGLocationMapping lm = oldPrec.getLocationMapping();

        // add mistmatches from the pivot and the precision
        Builder<Path, Pair<CFANode, CFANode>> bldr = ImmutableSetMultimap.<Path, Pair<CFANode, CFANode>>builder();
        bldr = bldr.putAll(lm.getMismatchesForPath()).putAll(piv.getMismatchesPerPath());

        RGLocationMapping newLM = locrefManager.monotonicLocationMapping(bldr.build(), tid);

        Precision newPrec = new ARTPrecision(newLM, oldPrec.getWrappedPrecision());

        map.put(elem, newPrec);
      }
    }

    return refMap;
  }


  /**
   * If the subtree of some pivot generated an env. transition, then the abstraction point for its application
   * also becomes a pivot. This function return the least fixed point of this induction.
   * Pivots discovered this way are not assigned any new precision.
   * @param pivots
   * @param isDataRefinement
   * @return
   */
  public void  addPivotsByEnvironmentalApplications(Pivots pivots) {
    Pivots unprocessed = new Pivots(pivots);

    while (!unprocessed.isEmpty()){

      Pivots newUnprocessed = new Pivots();

      for (int tid : unprocessed.getTids()){

        Collection<Pivot> pivs = unprocessed.getPivotsForThread(tid);

        for (Pivot piv : pivs){

          Set<ARTElement> absElems = piv.getLocalSubtree();

          for (ARTElement aElem : absElems){
            Set<ARTElement> envChildren = aElem.getEnvChildMap().keySet();

            for (ARTElement child : envChildren){
              ARTElement la = RGCPA.findLastAbstractionARTElement(child);

              if (pivots.getElemsInSubtrees().contains(la)){
                continue;
              }
              Pivot newPiv = new Pivot(la);
              newUnprocessed.addPivot(newPiv);
            }
          }
        }
      }

      pivots.addAll(unprocessed);
      unprocessed = newUnprocessed;
    }

    return;
  }


  /**
   * Check which pivots are in the subtree of others. The key set of the result
   * are the pivots that are not reacheable by any other pivot.
   * @param pivs
   * @return
   */
  public Map<Pivot, Set<Pivot>> determinePivotCoverage(Pivots pivs) {

    Map<Pivot, Set<Pivot>> map = new LinkedHashMap<Pivot, Set<Pivot>>();
    Set<Pivot> allCovered = new HashSet<Pivot>();

    for (Integer tid : pivs.getTids()){

      for (Pivot piv : pivs.getPivotsForThread(tid)){

        if (allCovered.contains(piv)){
          continue;
        }

        Set<Pivot> coveredByPiv = new LinkedHashSet<Pivot>();
        map.put(piv, coveredByPiv);

        Set<ARTElement> subtree = piv.getElement().getLocalSubtree();

        for (Pivot otherPiv : pivs.getPivotsForThread(tid)){
          // compare only uncovered nodes
          if (otherPiv.equals(piv) || allCovered.contains(otherPiv)){
            continue;
          }

          if (subtree.contains(otherPiv.getElement())){
            // pivot coveres otherPiv
            coveredByPiv.add(otherPiv);

            if (map.containsKey(otherPiv)){
              coveredByPiv.addAll(map.get(otherPiv));
              map.remove(otherPiv);
            }
          }
        }

        allCovered.addAll(coveredByPiv);
      }
    }


    return map;
  }


  /**
   * Add precisions of the covered predicates into the top ones.
   * @param pivs
   * @return
   */
  public Pivots mergePivotsIntoTop(Pivots pivs){
    Pivots topPivs = new Pivots();

    Map<Pivot, Set<Pivot>> coverage = determinePivotCoverage(pivs);

    for (Pivot topPiv : coverage.keySet()){
      Set<Pivot> covered = coverage.get(topPiv);

      for (Pivot covPiv : covered){
        topPiv.addPrecisionOf(covPiv);
      }

      topPivs.addPivot(topPiv);
    }

    return topPivs;
  }



  /**
   * A pivot is created for every ART element for which interpolation is found.
   * Additionally, a pivot with the predicates at the error element.
   * @param reachedSets
   * @param info
   * @return
   */
  private Pivots getInitalDataPivots(ARTReachedSet[] reachedSets, InterpolationTreeResult info) {
    Pivots pivs = new Pivots();

    InterpolationTreeNode root = info.getTree().getRoot();
    int errorTid = root.getTid();
    ARTElement errorElem = root.getArtElement();

    /* create a pivot for the error state with its precsion
    ARTPrecision errorPrec = (ARTPrecision) reachedSets[errorTid].getPrecision(errorElem);
    RGPrecision rgErrorPrec = Precisions.extractPrecisionByType(errorPrec, RGPrecision.class);
    DataPivot errorPiv = new DataPivot(errorElem);
    errorPiv.addRGPrecision(rgErrorPrec);
    pivs.addPivot(errorPiv);*/

    // create a pivot for every point with new ART predicates
    SetMultimap<InterpolationTreeNode, AbstractionPredicate> artMap = info.getArtRefinementMap();

    for (InterpolationTreeNode node : artMap.keySet()){
      Set<AbstractionPredicate> preds = artMap.get(node);
      assert !preds.isEmpty();
      ARTElement elem = node.getArtElement();
      CFANode loc = elem.retrieveLocationElement().getLocationNode();

      Pivot piv = new Pivot(elem);
      piv.addArtPredicates(loc, preds);
      pivs.addPivot(piv);
    }

    // create a pivot for every point with new Env predicates
    SetMultimap<InterpolationTreeNode, AbstractionPredicate> envMap = info.getEnvRefinementMap();

    for (InterpolationTreeNode node : envMap.keySet()){
      Set<AbstractionPredicate> preds = envMap.get(node);
      assert !preds.isEmpty();
      ARTElement elem = node.getArtElement();
      CFANode loc = elem.retrieveLocationElement().getLocationNode();

      Pivot piv = new Pivot(elem);
      piv.addEnvPredicates(loc, preds);
      pivs.addPivot(piv);

    }

    return pivs;
  }


  /**
   * A pivot is created for every ART element for which interpolation is found.
   * Additionally, a pivot with the predicates at the error element.
   * @param reachedSets
   * @param info
   * @return
   */
  private Pivots getInitalDataPivots2(ARTReachedSet[] reachedSets, InterpolationTreeResult info) {
    Pivots pivs = new Pivots();

    InterpolationTreeNode root = info.getTree().getRoot();
    int errorTid = root.getTid();
    ARTElement errorElem = root.getArtElement();

    /* create a pivot for the error state with its precsion
    ARTPrecision errorPrec = (ARTPrecision) reachedSets[errorTid].getPrecision(errorElem);
    RGPrecision rgErrorPrec = Precisions.extractPrecisionByType(errorPrec, RGPrecision.class);
    DataPivot errorPiv = new DataPivot(errorElem);
    errorPiv.addRGPrecision(rgErrorPrec);
    pivs.addPivot(errorPiv);*/

    // create a pivot for every point with new ART predicates
    SetMultimap<InterpolationTreeNode, AbstractionPredicate> artMap = info.getArtRefinementMap();
    boolean foundNewPrec = false;

    for (InterpolationTreeNode node : artMap.keySet()){
      Set<AbstractionPredicate> preds = artMap.get(node);
      assert !preds.isEmpty();
      ARTElement elem = node.getArtElement();
      CFANode loc = elem.retrieveLocationElement().getLocationNode();

      // create a node only if the predicats are not in the elements precision
      RGAbstractElement.AbstractionElement absElem = AbstractElements.extractElementByType(elem, RGAbstractElement.AbstractionElement.class);
      assert absElem != null;

      RGPrecision currentPrec = absElem.getAbstractionPrecision();
      Set<AbstractionPredicate> currentARTPreds = new HashSet<AbstractionPredicate>(currentPrec.getARTGlobalPredicates());
      currentARTPreds.addAll(currentPrec.getARTPredicates(loc));

      if (currentARTPreds.containsAll(preds)){
        continue;
      }

      foundNewPrec = true;
      Pivot piv = new Pivot(elem);
      piv.addArtPredicates(loc, preds);
      pivs.addPivot(piv);
    }

    // create a pivot for every point with new Env predicates
    SetMultimap<InterpolationTreeNode, AbstractionPredicate> envMap = info.getEnvRefinementMap();

    for (InterpolationTreeNode node : envMap.keySet()){
      Set<AbstractionPredicate> preds = envMap.get(node);
      assert !preds.isEmpty();
      ARTElement elem = node.getArtElement();
      CFANode loc = elem.retrieveLocationElement().getLocationNode();

      // create a node only if the predicats are not in the elements precision
      RGAbstractElement.AbstractionElement absElem = AbstractElements.extractElementByType(elem, RGAbstractElement.AbstractionElement.class);
      assert absElem != null;

      RGPrecision currentPrec = absElem.getAbstractionPrecision();
      if (currentPrec != null){
        System.out.println();
      }

      Set<AbstractionPredicate> currentEnvPreds = new HashSet<AbstractionPredicate>(currentPrec.getEnvGlobalPredicates());
      currentEnvPreds.addAll(currentPrec.getEnvPredicates(loc));

      if (currentEnvPreds.containsAll(preds)){
        continue;
      }

      foundNewPrec = true;

      Pivot piv = new Pivot(elem);
      piv.addEnvPredicates(loc, preds);
      pivs.addPivot(piv);

    }

    assert foundNewPrec : "No new data predicates found.";

    return pivs;
  }

  /**
   * For every path create one pivot with a mistmatching pair of locations. Another pivot
   * contains the location mismatches of the location mapping at the error element.
   * @param reachedSets
   * @param info
   * @return
   */
  private Pivots getInitalLocationPivots(ARTReachedSet[] reachedSets, InterpolationTreeResult info) {
    Pivots pivs = new Pivots();

    InterpolationTreeNode root = info.getTree().getRoot();
    int errorTid = root.getTid();
    ARTElement errorElem = root.getArtElement();

    // add the error node as a pivot with the mistmatches from its location mapping
    ARTPrecision errorPrec = (ARTPrecision) reachedSets[errorTid].getPrecision(errorElem);
    RGLocationMapping errorLM = errorPrec.getLocationMapping();
    Pivot errorPiv = new Pivot(errorElem);
    errorPiv.addMismatchesPerPath(errorLM.getMismatchesForPath());
    pivs.addPivot(errorPiv);

    // for every path a pivot is create for one pair of mistmatching location (i.e. the first one)
    Map<Path, List<Pair<ARTElement, Pair<CFANode, CFANode>>>> pathRefMap = info.getPathRefinementMap();

    for (Path pi : pathRefMap.keySet()){
      Pair<ARTElement, Pair<CFANode, CFANode>> pivotAndMistmatch = pathRefMap.get(pi).get(0);
      ARTElement elem = pivotAndMistmatch.getFirst();
      Pair<CFANode, CFANode> mismatch = pivotAndMistmatch.getSecond();

      Pivot piv = new Pivot(elem);
      piv.addMismatchPerPath(pi, mismatch);
      pivs.addPivot(piv);
    }

    return pivs;
  }


  private Pivots getInitalLocationPivots2(ARTReachedSet[] reachedSets, InterpolationTreeResult info) {

    Pivots ordPivs = this.getInitalLocationPivots(reachedSets, info);

    InterpolationTreeNode root = info.getTree().getRoot();
    int errorTid = root.getTid();
    ARTElement init = reachedSets[errorTid].getFirstElement();

    for (ARTElement child : init.getLocalChildren()){
      Pivot piv = new Pivot(child);
      ordPivs.addPivot(piv);
    }


    return ordPivs;
  }

  /**
   * Returns true iff pivots contain some interpolant that is not yet in the precision.
   * Works only for data refinement and global predicates. PROBABLY WORNG.
   * @param pReachedSets
   * @param pPivots
   * @return
   */
  private boolean checkIfNewFound(ARTReachedSet[] reachedSets, Pivots pivots) {

    for (Integer tid : pivots.getTids()){

      for (Pivot piv : pivots.getPivotsForThread(tid)){
        ARTPrecision aprec = (ARTPrecision) reachedSets[tid].getPrecision(piv.getElement());
        RGPrecision rgprec = Precisions.extractPrecisionByType(aprec, RGPrecision.class);
        if (!rgprec.getARTGlobalPredicates().containsAll(piv.getArtGlobalPredicates())){
          return true;
        }

        if (!rgprec.getARTGlobalPredicates().containsAll(piv.getArtPredicateMap().values())){
          return true;
        }


        if (!rgprec.getEnvGlobalPredicates().containsAll(piv.getEnvGlobalPredicates())){
          return true;
        }

        if (!rgprec.getEnvGlobalPredicates().containsAll(piv.getEnvPredicatesMap().values())){
          return true;
        }
      }


    }
    return false;
  }


}
