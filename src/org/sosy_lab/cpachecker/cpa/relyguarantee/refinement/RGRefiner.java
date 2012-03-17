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
import java.util.List;
import java.util.Map;

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
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.interpolation.InterpolationTreeResult;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.interpolation.RGInterpolationManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.lazy.RGLazyRefinementManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.lazy.RGLazyRefinementResult;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.locations.RGLocationRefinementManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.restarting.RGRestartingRefinementManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;


@Options(prefix="cpa.rg")
public class RGRefiner implements StatisticsProvider{

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(name="refinement.lazy",
      description="Use lazy refinement rather than restart the analysis.")
  private boolean lazy = false;

  private final RGInterpolationManager<?, ?> refManager;
  private final RGLocationRefinementManager locrefManager;
  private final RGLazyRefinementManager lazyManager;
  private final RGRestartingRefinementManager restartManager;

  private final ARTCPA[] artCpas;
  private final ParallelCFAS pcfa;
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
    this.algorithm    = pAlgorithm;
    this.pcfa        = this.algorithm.getPcfa();
    this.stats        = new Stats();

    for (int i=0; i<cpas.length; i++){
      if (cpas[i] instanceof ARTCPA) {
        artCpas[i] = (ARTCPA) cpas[i];
      } else {
        throw new InvalidConfigurationException("ART CPA needed for refinement");
      }
    }

    RGCPA rgCPA = artCpas[0].retrieveWrappedCpa(RGCPA.class);
    if (rgCPA != null){
      refManager = rgCPA.getRelyGuaranteeManager();
      locrefManager = rgCPA.getLocrefManager();
      lazyManager  = new RGLazyRefinementManager(locrefManager, this.pcfa, pConfig);
      restartManager = new RGRestartingRefinementManager(locrefManager, this.pcfa, pConfig);
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

    RGLazyRefinementResult refResult = lazyManager.getRefinedElements(reachedSets, cexample);

    dropPivots(reachedSets, refResult);
    algorithm.removeDestroyedCandidates();
  }





  private void performRestartingRefinement(ARTReachedSet[] reachedSets, InterpolationTreeResult cexample, RGAlgorithm algorithm) throws RefinementFailedException {
    assert cexample.isSpurious();
    System.out.println();
    System.out.println("\t\t\t ----- Restarting refinement -----");

    Map<Integer, Map<ARTElement, Precision>> refMap = restartManager.getRefinedElements(reachedSets, cexample);

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

        if (debug){
          System.out.println("\t-"+root+",\n\t "+precision+"\n");
        }

        // TODO why does it take so long?
        reachedSets[tid].removeSubtree(root, precision);
      }
    }
  }

  private void dropPivots(ARTReachedSet[] reachedSets, RGLazyRefinementResult result) {

    SetMultimap<Integer, ARTElement> elementsToDrop = result.getElementsToDrop();

    for (Integer tid : elementsToDrop.keySet()){

      for (ARTElement root : elementsToDrop.get(tid)){
        reachedSets[tid].removeSubtreeOf(root);
      }

    }

    Map<Pair<Integer, ARTElement>, Precision> precisionToAdjust = result.getPrecisionToAdjust();

    for (Pair<Integer, ARTElement> pair : precisionToAdjust.keySet()){
      Integer tid = pair.getFirst();
      ARTElement elem = pair.getSecond();
      Precision prec = precisionToAdjust.get(pair);
      reachedSets[tid].readdWithPrecision(elem, prec);
    }
  }


  @Override
  public void collectStatistics(Collection<Statistics> scoll) {
    scoll.add(stats);
    refManager.collectStatistics(scoll);
    locrefManager.collectStatistics(scoll);
  }


  public static class  Stats implements Statistics {

    public final Timer totalTimer          = new Timer();
    public int maxPredicatesPerLoc      = 0;

    @Override
    public String getName() {
      return "RGRefiners";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached){
      out.println("total time on refinement:        " + totalTimer);
      out.println("max. predicates per location     " + formatInt(maxPredicatesPerLoc));

    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }
  }



}
