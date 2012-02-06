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

  @Option(name="refinement.addPredicatesGlobally",
      description="refinement will add all discovered predicates "
        + "to all the locations in the abstract trace")
        private boolean addEnvPredicatesGlobally = true;


  private final Stats stats;
  private final RGRefinementManager refManager;
  private final RGLocationRefinementManager locrefManager;
  private final ARTCPA[] artCpas;

  private final  RGEnvironmentManager rgEnvironment;
  private static RGRefiner singleton;

  /**
   * Singleton instance of RelyGuaranteeRefiner.
   * @param cpas
   * @param rgEnvironment
   * @param pConfig
   * @return
   * @throws InvalidConfigurationException
   */
  public static RGRefiner getInstance(final ConfigurableProgramAnalysis[] cpas, RGEnvironmentManager rgEnvironment, Configuration pConfig) throws InvalidConfigurationException {
    if (singleton == null){
      singleton = new RGRefiner(cpas, rgEnvironment, pConfig);
    }
    return singleton;
  }

  public RGRefiner(final ConfigurableProgramAnalysis[] cpas, RGEnvironmentManager rgEnvironment, Configuration pConfig) throws InvalidConfigurationException{
    pConfig.inject(this, RGRefiner.class);
    artCpas = new ARTCPA[cpas.length];
    for (int i=0; i<cpas.length; i++){
      if (cpas[i] instanceof ARTCPA) {
        artCpas[i] = (ARTCPA) cpas[i];
      } else {
        throw new InvalidConfigurationException("ART CPA needed for refinement");
      }
    }

    this.rgEnvironment = rgEnvironment;
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
            System.out.println();
            for (ARTElement parent : parents){
              RGPrecision prec = Precisions.extractPrecisionByType(artReachedSets[tid].getPrecision(parent), RGPrecision.class);
              System.out.println("Precision for thread "+tid+":");
              System.out.println("\t-ART local "  + prec.getPredicateMap());
              System.out.println("\t-ART global " + prec.getGlobalPredicates());
              System.out.println("\t-Env local "  +  rgEnvironment.getEnvPrecision()[tid]);
              System.out.println("\t-Env global " + rgEnvironment.getEnvGlobalPrecision()[tid]);
            }
          }
        }
      }

      System.out.println();
      System.out.println("\t\t\t --- Dropping all env transitions ---");
      environment.resetEnvironment();
      stats.restartingTimer.stop();

      environment.resetEnvironment();
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
   * For each thread adds new interpolants to the initial element.
   * @param reachedSets
   * @param info
   * @param errorThr
   * @return
   */
  private Multimap<Integer, Pair<ARTElement, RGPrecision>> restartingRefinement(ReachedSet[] reachedSets, InterpolationTreeResult info) {
    // TODO rewrite - a bit sloopy
    Multimap<Integer, Pair<ARTElement, RGPrecision>> refinementMap = HashMultimap.create();
    // multimap : thread no -> (ART element)
    Multimap<Integer, ARTElement> artMap = HashMultimap.create();

    boolean newPredicates = false;

    if (debug){
      System.out.println("New predicates:");
    }

    // add env. predicates to precision
    for (ARTElement aElement : info.getEnvPredicatesForRefinmentKeys()){
      ARTElement artElement = aElement;
      RGAbstractElement rgElement = AbstractElements.extractElementByType(artElement, RGAbstractElement.class);
      int tid = rgElement.getTid();
      CFANode loc = AbstractElements.extractLocation(artElement);
      Collection<AbstractionPredicate> preds = info.getEnvPredicatesForRefinement(aElement);

      if (!this.addEnvPredicatesGlobally){
        SetMultimap<CFANode, AbstractionPredicate> tPrec = rgEnvironment.getEnvPrecision()[tid];
        if (!tPrec.get(loc).containsAll(preds)){
          newPredicates = true;
          Collection<AbstractionPredicate> cNewPreds = new HashSet<AbstractionPredicate>(preds);
          cNewPreds.removeAll(tPrec.get(loc));
          if (debug){
            System.out.println("\t- env: "+loc+" -> "+cNewPreds);
          }
        }
        tPrec.putAll(loc, preds);
      } else {
        Set<AbstractionPredicate> tPrec = rgEnvironment.getEnvGlobalPrecision()[tid];
        if (!tPrec.containsAll(preds)){
          newPredicates = true;
          Collection<AbstractionPredicate> cNewPreds = new HashSet<AbstractionPredicate>(preds);
          cNewPreds.removeAll(tPrec);
          if (debug){
            System.out.println("\t- env: "+loc+" -> "+cNewPreds);
          }
        }
        tPrec.addAll(preds);
      }
    }

    // group interpolation elements  by threads
    for (ARTElement aElement : info.getPredicatesForRefinmentKeys()){
      //Collection<AbstractionPredicate> newpreds = info.getPredicatesForRefinement(aElement);
      RGAbstractElement rgElement = AbstractElements.extractElementByType(aElement, RGAbstractElement.class);
      int tid = rgElement.getTid();
      if (!info.getPredicatesForRefinement(aElement).isEmpty()){
        artMap.put(tid, aElement);
      }
    }


    // for every thread sum up interpolants and add it to the initial element
    for (int tid=0 ; tid < reachedSets.length; tid++){
      ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
      // add the precision of the initial element
      ARTElement inital  = (ARTElement) reachedSets[tid].getFirstElement();
      Precision prec = reachedSets[tid].getPrecision(inital);
      RGPrecision rgPrecision = Precisions.extractPrecisionByType(prec, RGPrecision.class);
      SetMultimap<CFANode, AbstractionPredicate> oldPreds = rgPrecision.getPredicateMap();

      pmapBuilder.putAll(oldPreds);

      for (ARTElement artElement : artMap.get(tid)){
        // add the new interpolants

        Collection<AbstractionPredicate> newpreds = info.getPredicatesForRefinement(artElement);
        CFANode loc = AbstractElements.extractLocation(artElement);
        if (addPredicatesGlobally){
          Set<AbstractionPredicate> gpreds  = rgPrecision.getGlobalPredicates();
          if (!gpreds.containsAll(newpreds)){
            newPredicates = true;
            if (debug){
              Collection<AbstractionPredicate> cNewPreds = new HashSet<AbstractionPredicate>(newpreds);
              cNewPreds.removeAll(gpreds);
              System.out.println("\t- ART: "+loc+" -> "+cNewPreds);
            }

          }
          Set<AbstractionPredicate> ngpreds = new HashSet<AbstractionPredicate>(gpreds);
          ngpreds.addAll(newpreds);
          rgPrecision.setGlobalPredicates(ImmutableSet.copyOf(ngpreds));
        } else {
          pmapBuilder.putAll(loc, newpreds);
          if (!oldPreds.get(loc).containsAll(newpreds)){
            newPredicates = true;
            if (debug){
              Collection<AbstractionPredicate> cNewPreds = new HashSet<AbstractionPredicate>(newpreds);
              cNewPreds.removeAll(oldPreds.get(loc));
              System.out.println("\t- ART: "+loc+" -> "+cNewPreds);
            }
          }
        }


      }

      ImmutableSetMultimap<CFANode, AbstractionPredicate> newPredMap = pmapBuilder.build();
      RGPrecision newPrecision = new RGPrecision(newPredMap, rgPrecision.getGlobalPredicates());

      // for statistics check the number of predicates per location
      for (CFANode node : newPredMap.keySet()){
        stats.maxPredicatesPerLoc = Math.max(stats.maxPredicatesPerLoc, newPredMap.get(node).size());
      }

      for (ARTElement initChild : inital.getChildARTs()){
        refinementMap.put(tid, Pair.of(initChild, newPrecision));
      }

    }
    assert newPredicates;

    return refinementMap;
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
