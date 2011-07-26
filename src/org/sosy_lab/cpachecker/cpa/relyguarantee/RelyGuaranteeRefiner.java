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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;



public class RelyGuaranteeRefiner{
  @Option(name="refinement.addPredicatesGlobally",
      description="refinement will add all discovered predicates "
        + "to all the locations in the abstract trace")
  private boolean addPredicatesGlobally = false;

  @Option(name="errorPath.export",
      description="export one satisfying assignment for the error path")
  private boolean exportErrorPath = true;

  @Option(name="errorPath.file", type=Option.Type.OUTPUT_FILE,
      description="export one satisfying assignment for the error path")
  private File exportFile = new File("ErrorPathAssignment.txt");

  @Option(name="refinement.msatCexFile", type=Option.Type.OUTPUT_FILE,
      description="where to dump the counterexample formula in case the error location is reached")
  private File dumpCexFile = new File("counterexample.msat");

  private RelyGuaranteeRefinementManager manager;
  private final ARTCPA[] artCpas;

  private Object lastErrorPath;

  public RelyGuaranteeRefiner(final ConfigurableProgramAnalysis[] cpas) throws InvalidConfigurationException{

    artCpas = new ARTCPA[cpas.length];
    for (int i=0; i<cpas.length; i++){
      if (cpas[i] instanceof ARTCPA) {
        artCpas[i] = (ARTCPA) cpas[i];
      } else {
        throw new InvalidConfigurationException("ART CPA needed for refinement");
      }
    }

    RelyGuaranteeCPA rgCPA = artCpas[0].retrieveWrappedCpa(RelyGuaranteeCPA.class);
    if (rgCPA != null){
      manager = rgCPA.getRelyGuaranteeManager();
    } else {
      throw new InvalidConfigurationException("RelyGuaranteeCPA needed for refinement");
    }

  }


  /**
   *
   * @param pReachedSets
   * @param pErrorThr
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean performRefinment(ReachedSet[] reachedSets, int errorThr) throws InterruptedException, CPAException {


    //assert checkART(reachedSets[errorThr]);
    assert reachedSets[errorThr].getLastElement() instanceof ARTElement;
    ARTElement targetElement = (ARTElement) reachedSets[errorThr].getLastElement();
    assert (targetElement).isTarget();


    //List<Formula> f = manager.getRelyGuaranteeFormulaForNode(targetElement, reachedSets, errorThr);


    // get the path from the root to the error in the relevant thread
    Path cfaPath = computePath(targetElement, reachedSets[errorThr]);

    //
    ARTReachedSet[] artReachedSets = new ARTReachedSet[reachedSets.length];
    for (int i=0; i<reachedSets.length; i++){
      artReachedSets[i] = new ARTReachedSet(reachedSets[i], artCpas[i]);
    }

    Set<ARTElement> elementsOnPath = ARTUtils.getAllElementsOnPathsTo(targetElement);
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> path = transformPath(cfaPath);

    Precision oldPrecision = reachedSets[errorThr].getPrecision(targetElement);
    RelyGuaranteePrecision oldPredicatePrecision = Precisions.extractPrecisionByType(oldPrecision, RelyGuaranteePrecision.class);

    List<RelyGuaranteeAbstractElement> abstractTrace = Lists.transform(path, Triple.<RelyGuaranteeAbstractElement>getProjectionToThird());
    CounterexampleTraceInfo mCounterexampleTraceInfo = manager.buildRgCounterexampleTrace(abstractTrace, elementsOnPath);

    // if error is spurious refine
    if (mCounterexampleTraceInfo.isSpurious()) {
      Pair<ARTElement, RelyGuaranteePrecision> refinementResult =  performRefinement(oldPredicatePrecision, path, mCounterexampleTraceInfo);

      artReachedSets[errorThr].removeSubtree(refinementResult.getFirst(), refinementResult.getSecond());
      //pReached.removeSubtree(refinementResult.getFirst(), refinementResult.getSecond());
      return true;
    } else {
      // we have a real error
      return false;
    }


  }





  protected Path computePath(ARTElement pLastElement, ReachedSet pReached) throws InterruptedException, CPAException {
    return ARTUtils.getOnePathTo(pLastElement);
  }

  protected List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> transformPath(Path pPath) throws CPATransferException {
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> result = Lists.newArrayList();

    for (ARTElement ae : skip(transform(pPath, Pair.<ARTElement>getProjectionToFirst()), 1)) {
      RelyGuaranteeAbstractElement pe = extractElementByType(ae, RelyGuaranteeAbstractElement.class);
      if (pe instanceof AbstractionElement) {
        CFANode loc = AbstractElements.extractLocation(ae);
        result.add(Triple.of(ae, loc, pe));
      }
    }

    assert pPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  /**
   * pPath and pArtPath need to fit together such that
   * pPath.get(i) == pArtPath.get(i).retrieveWrappedElement(PredicateAbstractElement)
   */
  private Pair<ARTElement, RelyGuaranteePrecision> performRefinement(RelyGuaranteePrecision oldPrecision,
      List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> pPath,
      CounterexampleTraceInfo pInfo) throws CPAException {

    Multimap<CFANode, AbstractionPredicate> oldPredicateMap = oldPrecision.getPredicateMap();
    Set<AbstractionPredicate> globalPredicates = oldPrecision.getGlobalPredicates();

    Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> firstInterpolationPoint = null;
    boolean newPredicatesFound = false;

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();

    pmapBuilder.putAll(oldPredicateMap);

    // iterate through pPath and find first point with new predicates, from there we have to cut the ART
    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> interpolationPoint : pPath) {
      CFANode loc = interpolationPoint.getSecond();
      Collection<AbstractionPredicate> newpreds = getPredicatesForARTElement(pInfo, interpolationPoint);

      if (firstInterpolationPoint == null && newpreds.size() > 0) {
        firstInterpolationPoint = interpolationPoint;
      }
      if (!newPredicatesFound && !oldPredicateMap.get(loc).containsAll(newpreds)) {
        // new predicates for this location
        newPredicatesFound = true;
      }



      pmapBuilder.putAll(loc, newpreds);
      pmapBuilder.putAll(loc, globalPredicates);
    }
    assert firstInterpolationPoint != null;

    ImmutableSetMultimap<CFANode, AbstractionPredicate> newPredicateMap = pmapBuilder.build();
    RelyGuaranteePrecision newPrecision;
    if (addPredicatesGlobally) {
      newPrecision = new RelyGuaranteePrecision(newPredicateMap.values());
    } else {
      newPrecision = new RelyGuaranteePrecision(newPredicateMap, globalPredicates);
    }

    System.out.println();
    System.out.println("--------------------------------------- -------------------------------");
    System.out.println("# Predicate map now is "+newPredicateMap);


    List<CFANode> absLocations = ImmutableList.copyOf(transform(pPath, Triple.<CFANode>getProjectionToSecond()));

    // We have two different strategies for the refinement root: set it to
    // the firstInterpolationPoint or set it to highest location in the ART
    // where the same CFANode appears.
    // Both work, so this is a heuristics question to get the best performance.
    // My benchmark showed, that at least for the benchmarks-lbe examples it is
    // best to use strategy one iff newPredicatesFound.

    ARTElement root = null;
    if (newPredicatesFound) {
      root = firstInterpolationPoint.getFirst();



    } else {
      if (absLocations.equals(lastErrorPath)) {
        throw new RefinementFailedException(RefinementFailedException.Reason.NoNewPredicates, null);
      }

      CFANode loc = firstInterpolationPoint.getSecond();



      // find first element in path with location == loc,
      // this is not necessary equal to firstInterpolationPoint.getFirst()
      for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> abstractionPoint : pPath) {
        if (abstractionPoint.getSecond().equals(loc)) {
          root = abstractionPoint.getFirst();
          break;
        }
      }
      if (root == null) {
        throw new CPAException("Inconsistent ART, did not find element for " + loc);
      }
    }
    lastErrorPath = absLocations;
    return Pair.of(root, newPrecision);
  }

  protected Collection<AbstractionPredicate> getPredicatesForARTElement(
      CounterexampleTraceInfo pInfo, Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> pInterpolationPoint) {
    return pInfo.getPredicatesForRefinement(pInterpolationPoint.getThird());
  }

}

