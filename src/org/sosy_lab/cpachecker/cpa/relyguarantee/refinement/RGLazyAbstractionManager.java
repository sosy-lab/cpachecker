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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGPrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.pivots.DataPivot;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.pivots.LocationPivot;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.pivots.Pivot;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.pivots.Pivots;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
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
public class RGLazyAbstractionManager {

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

  public RGLazyAbstractionManager(RGLocationRefinementManager pLocrefManager, ParallelCFAS pcfa, Configuration config) throws InvalidConfigurationException {
    config.inject(this, RGLazyAbstractionManager.class);
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
  public Map<Integer, Map<ARTElement, Precision>> getRefinedElements(ARTReachedSet[] reachedSets, InterpolationTreeResult info) throws RefinementFailedException {
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
      pivots = getInitalDataPivots(reachedSets, info);
    } else {
      pivots = getInitalLocationPivots2(reachedSets, info);
    }

    if (debug){
      System.out.println(pivots.toString());
      System.out.println();
    }

    pivots = getPivotsByEnvironmentalApplications(pivots, isDataRefinement);

    if (debug){
      System.out.println(pivots.toString());
      System.out.println();
    }

    pivots = mergePivotsIntoTop(pivots);

    if (debug){
      System.out.println(pivots.toString());
      System.out.println();
    }

    pivots = balancePrecisions(pivots, reachedSets);

    if (debug){
      System.out.println(pivots.toString());
      System.out.println();
    }

    Map<Integer, Map<ARTElement, Precision>> refMap;
    if (isDataRefinement){
      refMap = createDataPrecision(pivots, reachedSets);
    } else {
      refMap = createLocationPrecision(pivots, reachedSets);
    }

    if (debug){
      for (int tid : refMap.keySet()){
        System.out.println("Thread "+tid+":");

        if (refMap.get(tid) == null){
          System.out.println(this.getClass());
        }

        for (ARTElement  aelem : refMap.get(tid).keySet()){
          Precision prec = refMap.get(tid).get(aelem);
          System.out.println("\t-"+aelem.getElementId()+" : "+prec);
        }
      }
      System.out.println();
    }


    /*if (shareDroppedPrecision){
      refMap = addDroppedPrecision(refMap, reachedSets);
    }*/


    return refMap;
  }


  /**
   * If two pivots would casue the same element to be readded to the waitlist, then
   * their precision should be the same, to avoid race conditions.
   * @param pPivots
   * @param pReachedSets
   * @return
   */
  private Pivots balancePrecisions(Pivots oldPivots, ARTReachedSet[] reachedSets) {
    Pivots pivots = new Pivots();
    pivots.addAll(oldPivots.getPivotMap());

    for (Integer tid : pivots.getTids()){

      Collection<Pivot> pivsForThread = pivots.getPivotsForThread(tid);
      if (pivsForThread.size() <=1 ){
        continue;
      }

      // readMap: elements that will readded -> pivots that cause it
      SetMultimap<ARTElement, Pivot> readdMap = LinkedHashMultimap.create();

      for (Pivot piv : pivsForThread){
        ARTElement aelem = piv.getElement();
        Set<ARTElement> readdSet = reachedSets[tid].readdedElements(aelem);

        for (ARTElement readded : readdSet){
          readdMap.put(readded, piv);
        }
      }

      // merge precision for elements that readded the same pivot
      for (ARTElement elem : readdMap.keySet()){

        Set<Pivot> pivs = readdMap.get(elem);
        if (pivs.size() == 1){
          continue;
        }

        // merge all elements in the first one
        Pivot first = pivs.iterator().next();

        for (Pivot piv : pivs){
          if (!piv.equals(first)){
            first.addPrecisionOf(piv);
          }
        }

        // add precision of the first element into all others
        for (Pivot piv : pivs){
          if (!piv.equals(first)){
            piv.addPrecisionOf(first);
          }
        }


      }
    }

    return pivots;
  }


  /**
   * Create a new precision for every pivot by adding old precison.
   * @param pPivots
   * @param pReachedSets
   * @return
   */
  private Map<Integer, Map<ARTElement, Precision>> createDataPrecision(Pivots pivots, ARTReachedSet[] reachedSets) {

    Map<Integer, Map<ARTElement, Precision>> refMap = new HashMap<Integer, Map<ARTElement, Precision>>();

    for (Integer tid : pivots.getTids()){

      Map<ARTElement, Precision> map = refMap.get(tid);
      if (map == null){
        map = new HashMap<ARTElement, Precision>();
        refMap.put(tid, map);
      }

      for (Pivot piv : pivots.getPivotsForThread(tid)){
        DataPivot dpiv = (DataPivot) piv;

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

    Map<Integer, Map<ARTElement, Precision>> refMap = new HashMap<Integer, Map<ARTElement, Precision>>();

    for (Integer tid : pivots.getTids()){

      Map<ARTElement, Precision> map = refMap.get(tid);
      if (map == null){
        map = new HashMap<ARTElement, Precision>();
        refMap.put(tid, map);
      }

      for (Pivot piv : pivots.getPivotsForThread(tid)){
        LocationPivot lpiv = (LocationPivot) piv;

        ARTElement elem = piv.getElement();
        ARTPrecision oldPrec = (ARTPrecision) reachedSets[tid].getPrecision(elem);
        RGLocationMapping lm = oldPrec.getLocationMapping();

        // add mistmatches from the pivot and the precision
        Builder<Path, Pair<CFANode, CFANode>> bldr = ImmutableSetMultimap.<Path, Pair<CFANode, CFANode>>builder();
        bldr = bldr.putAll(lm.getMismatchesForPath()).putAll(lpiv.getMismatchesPerPath());

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
  public Pivots getPivotsByEnvironmentalApplications(Pivots pivots, boolean isDataRefinement) {
    Pivots unprocessed = pivots;
    Pivots processed = new Pivots();
    processed.addAll(unprocessed.getPivotMap());

    while (!unprocessed.isEmpty()){

      Pivots newUnprocessed = new Pivots();

      for (int tid : unprocessed.getTids()){

        Collection<Pivot> pivs = unprocessed.getPivotsForThread(tid);

        for (Pivot piv : pivs){
          ARTElement abs = piv.getElement();
          Set<ARTElement> absElems = abs.getLocalSubtree();

          for (ARTElement aElem : absElems){
            Set<ARTElement> envChildren = aElem.getEnvChildMap().keySet();

            for (ARTElement child : envChildren){
              ARTElement la = RGCPA.findLastAbstractionARTElement(child);

              if (!processed.containsPivotsWithElement(la)){
                Pivot newPiv;
                if (isDataRefinement){
                  newPiv = new DataPivot(la);
                } else {
                  newPiv = new LocationPivot(la);
                }

                newUnprocessed.addPivot(newPiv);
              }
            }
          }
        }
      }

      processed.addAll(unprocessed.getPivotMap());
      unprocessed = newUnprocessed;
    }

    return processed;
  }


  /**
   * Check which pivots are in the subtree of others. The key set of the result
   * are the pivots that are not reacheable by any other pivot.
   * @param pivs
   * @return
   */
  public Map<Pivot, Set<Pivot>> determinePivotCoverage(Pivots pivs) {

    Map<Pivot, Set<Pivot>> map = new HashMap<Pivot, Set<Pivot>>();
    Set<Pivot> allCovered = new HashSet<Pivot>();

    for (Integer tid : pivs.getTids()){

      for (Pivot piv : pivs.getPivotsForThread(tid)){
        if (allCovered.contains(piv) || piv.getElement().isCovered()){
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

    Map<Pivot, Set<Pivot>> coverage = this.determinePivotCoverage(pivs);

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

    // create a pivot for the error state with its precsion
    ARTPrecision errorPrec = (ARTPrecision) reachedSets[errorTid].getPrecision(errorElem);
    RGPrecision rgErrorPrec = Precisions.extractPrecisionByType(errorPrec, RGPrecision.class);
    DataPivot errorPiv = new DataPivot(errorElem);
    errorPiv.addRGPrecision(rgErrorPrec);
    pivs.addPivot(errorPiv);

    // create a pivot for every point with new ART predicates
    SetMultimap<InterpolationTreeNode, AbstractionPredicate> artMap = info.getArtRefinementMap();

    for (InterpolationTreeNode node : artMap.keySet()){
      Set<AbstractionPredicate> preds = artMap.get(node);
      assert !preds.isEmpty();
      ARTElement elem = node.getArtElement();
      CFANode loc = elem.retrieveLocationElement().getLocationNode();

      DataPivot piv = new DataPivot(elem);
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

      DataPivot piv = new DataPivot(elem);
      piv.addEnvPredicates(loc, preds);
      pivs.addPivot(piv);

    }

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
    LocationPivot errorPiv = new LocationPivot(errorElem);
    errorPiv.addMismatchesPerPath(errorLM.getMismatchesForPath());
    pivs.addPivot(errorPiv);

    // for every path a pivot is create for one pair of mistmatching location (i.e. the first one)
    Map<Path, List<Pair<ARTElement, Pair<CFANode, CFANode>>>> pathRefMap = info.getPathRefinementMap();

    for (Path pi : pathRefMap.keySet()){
      Pair<ARTElement, Pair<CFANode, CFANode>> pivotAndMistmatch = pathRefMap.get(pi).get(0);
      ARTElement elem = pivotAndMistmatch.getFirst();
      Pair<CFANode, CFANode> mismatch = pivotAndMistmatch.getSecond();

      LocationPivot piv = new LocationPivot(elem);
      piv.addMismatchPerPath(pi, mismatch);
      pivs.addPivot(piv);
    }

    return pivs;
  }


  private Pivots getInitalLocationPivots2(ARTReachedSet[] reachedSets, InterpolationTreeResult info) {

    Pivots pivs = new Pivots();

    Pivots ordPivs = this.getInitalLocationPivots(reachedSets, info);

    pivs.addAll(ordPivs.getPivotMap());

    InterpolationTreeNode root = info.getTree().getRoot();
    int errorTid = root.getTid();
    ARTElement init = reachedSets[errorTid].getFirstElement();

    for (ARTElement child : init.getLocalChildren()){
      LocationPivot piv = new LocationPivot(child);
      pivs.addPivot(piv);
    }


    return pivs;
  }

  /*
  private Map<Integer, Map<ARTElement, Precision>> findCommonPrecision(ARTReachedSet[] reachedSets, Map<Integer, Map<ARTElement, Precision>> refMap) {

    Map<Integer, Map<ARTElement, Precision>> newRefMap = new HashMap<Integer, Map<ARTElement, Precision>>(refMap);

    for (int tid : refMap.keySet()){

      if (refMap.get(tid).size() <=1 ){
        continue;
      }

      // readMap: elements that will readded -> pivots that cause it
      Multimap<ARTElement, ARTElement> readdMap = LinkedHashMultimap.create();

      for (ARTElement aelem : refMap.get(tid).keySet()){
        Set<ARTElement> readdSet = reachedSets[tid].readdedElements(aelem);
        for (ARTElement readded : readdSet){
          readdMap.put(readded, aelem);
        }
      }

      // merge precision for elements that readded the same pivot
       Map<ARTElement, Precision> threadNewPivots = newRefMap.get(tid);

      for (ARTElement elem : readdMap.keySet()){

        Collection<ARTElement> pivs = readdMap.get(elem);
        if (pivs.size() == 1){
          continue;
        }

        List<RGPrecision> precs = new Vector<RGPrecision>(pivs.size());

        for (ARTElement aelem : pivs){
          precs.add((RGPrecision) threadNewPivots.get(aelem));
        }

        RGPrecision mPrec = RGPrecision.merge(precs);

        for (ARTElement aelem : pivs){
          threadNewPivots.put(aelem, mPrec);
        }
      }
    }

    return newRefMap;
  }
   */


}
