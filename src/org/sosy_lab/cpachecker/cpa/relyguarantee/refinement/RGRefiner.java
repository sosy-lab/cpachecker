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
import java.util.HashSet;
import java.util.List;
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


  private final Stats stats;
  private final RGRefinementManager refManager;
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
      Multimap<Integer, Pair<ARTElement, RGPrecision>> refinementResult;

      System.out.println();
      System.out.println("\t\t\t ----- Restarting analysis -----");
      stats.restartingTimer.start();
      refinementResult = restartingRefinement(reachedSets, counterexampleInfo);

      // drop subtrees and change precision
      for(int tid : refinementResult.keySet()){
        for(Pair<ARTElement, RGPrecision> pair : refinementResult.get(tid)){
          ARTElement root = pair.getFirst();
          // drop cut-off node in every thread
          RGPrecision precision = pair.getSecond();
          Set<ARTElement> parents = new HashSet<ARTElement>(root.getParentARTs());


          // TODO why does it take so long?
          artReachedSets[tid].removeSubtree(root, precision);

          if (debug){
            for (ARTElement parent : parents){
              RGPrecision prec = Precisions.extractPrecisionByType(artReachedSets[tid].getPrecision(parent), RGPrecision.class);
              System.out.println("Precision for thread "+tid+":");
              System.out.println("\t-"+prec);
            }
          }
        }
      }

      System.out.println();
      System.out.println("\t\t\t --- Dropping all env transitions ---");
      this.algorithm.resetEnvironment();
      stats.restartingTimer.stop();

      for (int i=0; i<reachedSets.length;i++){
        assert reachedSets[i].getReached().size()==1;
      }

      stats.totalTimer.stop();
      return true;
    } else {

      // a real error
      stats.totalTimer.stop();
      return false;
    }
  }


  /**
   * Computes new precision for the inital elements.
   * @param reachedSets
   * @param info
   * @return
   */
  private Multimap<Integer, Pair<ARTElement, RGPrecision>> restartingRefinement(ReachedSet[] reachedSets, InterpolationTreeResult info){

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
      ARTElement initial = (ARTElement) reachedSets[i].getFirstElement();
      Precision prec = reachedSets[i].getPrecision(initial);
      RGPrecision oldPrec = Precisions.extractPrecisionByType(prec, RGPrecision.class);
      Pair<RGPrecision, Boolean> pair = gatherAllPredicatesForThread(info, oldPrec, i);
      RGPrecision newPrec = pair.getFirst();

      newPred = newPred || pair.getSecond();

      for (ARTElement child : initial.getChildARTs()){
        refMap.put(i, Pair.of(child, newPrec));
      }
    }

    if (debug){
      assert newPred || newLM : "No new predicates nor location mapping found.";
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

    SetMultimap<ARTElement, AbstractionPredicate> envMap = result.getEnvMap();
    for (ARTElement aElement : envMap.keySet()){
      RGAbstractElement rgElement = AbstractElements.extractElementByType(aElement, RGAbstractElement.class);
      int tid = rgElement.getTid();

      if (i == tid){
        CFANode loc = aElement.retrieveLocationElement().getLocationNode();
        map.putAll(loc, envMap.get(aElement));
      }

    }

    return map;
  }

  private Multimap<CFANode, AbstractionPredicate> gatherARTPredicates(InterpolationTreeResult result, int i) {
    Multimap<CFANode, AbstractionPredicate> map = LinkedHashMultimap.create();

    SetMultimap<ARTElement, AbstractionPredicate> artMap = result.getArtMap();

    for (ARTElement aElement : artMap.keySet()){
      RGAbstractElement rgElement = AbstractElements.extractElementByType(aElement, RGAbstractElement.class);
      int tid = rgElement.getTid();

      if (i == tid){
        CFANode loc = aElement.retrieveLocationElement().getLocationNode();
        map.putAll(loc, artMap.get(aElement));
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
