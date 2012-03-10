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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.restarting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGPrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.interpolation.InterpolationTreeNode;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.interpolation.InterpolationTreeResult;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.locations.RGLocationRefinementManager;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSetMultimap.Builder;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

@Options(prefix="cpa.rg")
public class RGRestartingRefinementManager {

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

  private final RGLocationRefinementManager locRefManager;
  private final ParallelCFAS pcfa;
  private final int threadNo;

  public RGRestartingRefinementManager(RGLocationRefinementManager locRefManager, ParallelCFAS pcfa, Configuration config) throws InvalidConfigurationException {
    config.inject(this, RGRestartingRefinementManager.class);
    this.locRefManager  = locRefManager;
    this.pcfa           = pcfa;
    this.threadNo       = pcfa.getThreadNo();
  }

  public Map<Integer, Map<ARTElement, Precision>> getRefinedElements(ARTReachedSet[] reachedSets, InterpolationTreeResult cexample) throws RefinementFailedException {

    boolean dataRef = cexample.getPathRefinementMap().isEmpty();
    Map<Integer, Map<ARTElement, Precision>> refMap;

    if (dataRef){
      refMap = getRestartingDataPrecision(reachedSets, cexample);
    } else {
      refMap = getRestartingLocationPrecision(reachedSets, cexample);
    }

    return refMap;
  }




  /**
   * Computes new precision for the inital elements.
   * @param reachedSets
   * @param info
   * @return
   */
  private Map<Integer, Map<ARTElement, Precision>> getRestartingDataPrecision(ARTReachedSet[] reachedSets, InterpolationTreeResult info){


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

    Map<Integer, Map<ARTElement, Precision>> refMap = new HashMap<Integer, Map<ARTElement, Precision>>(threadNo);
    int errorTid = info.getPathRefinementMap().values().iterator().next().get(0).getFirst().getTid();

    /* for every path we take one pair of mistmatching location (e.g. the first one);
       all paths belong to the same thread */
    SetMultimap<Path, Pair<CFANode, CFANode>> mismatchPerPath = getMistmachesPerPath(info.getPathRefinementMap());

    // find new location mapping for the error thread, and copy top precision for the others
    for (int i=0; i<threadNo; i++){
      ARTElement initial = reachedSets[i].getFirstElement();
      ARTPrecision prec = (ARTPrecision) reachedSets[i].getPrecision(initial);

      ARTPrecision newPrec;

      if (i == errorTid){
        // add mismatches from the current precision
        RGLocationMapping initLM = prec.getLocationMapping();
        mismatchPerPath.putAll(initLM.getMismatchesForPath());
        RGLocationMapping newLM = locRefManager.monotonicLocationMapping(ImmutableSetMultimap.copyOf(mismatchPerPath),
            errorTid);

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


  /**
   * Picks one pair of mistmatching locations for every path.
   * @param pathRefMap
   * @return
   */
  private SetMultimap<Path, Pair<CFANode, CFANode>> getMistmachesPerPath(
      Map<Path, List<Pair<ARTElement, Pair<CFANode, CFANode>>>> pathRefMap) {

    SetMultimap<Path, Pair<CFANode, CFANode>> mmap = LinkedHashMultimap.create();

    for (Path pi : pathRefMap.keySet()){
      List<Pair<ARTElement, Pair<CFANode, CFANode>>> list = pathRefMap.get(pi);
      Pair<ARTElement, Pair<CFANode, CFANode>> triple = list.get(0);

      mmap.put(pi, triple.getSecond());
    }

    return mmap;
  }


}
