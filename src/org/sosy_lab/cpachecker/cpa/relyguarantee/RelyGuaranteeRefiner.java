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
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
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
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;


@Options(prefix="cpa.relyguarantee")
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

  @Option(description="List of variables global to multiple threads")
  protected String[] globalVariables = {};

  private RelyGuaranteeRefinementManager manager;
  private final ARTCPA[] artCpas;

  private Object lastErrorPath;

  private static RelyGuaranteeRefiner rgRefiner;

  /**
   * Singleton instance of RelyGuaranteeRefiner.
   * @param cpas
   * @return
   * @throws InvalidConfigurationException
   */
  public static RelyGuaranteeRefiner getInstance(final ConfigurableProgramAnalysis[] cpas) throws InvalidConfigurationException {
    if (rgRefiner == null){
      rgRefiner = new RelyGuaranteeRefiner(cpas);
    }
    return rgRefiner;
  }

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
      //rgCPA.getConfiguration().inject(this, RelyGuaranteeRefiner.class);
    } else {
      throw new InvalidConfigurationException("RelyGuaranteeCPA needed for refinement");
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
  public boolean performRefinment(ReachedSet[] reachedSets, RelyGuaranteeEnvironment environment, int errorThr) throws InterruptedException, CPAException {
    int threadNo = reachedSets.length;

    //assert checkART(reachedSets[errorThr]);
    assert reachedSets[errorThr].getLastElement() instanceof ARTElement;
    ARTElement targetElement = (ARTElement) reachedSets[errorThr].getLastElement();
    assert (targetElement).isTarget();

    ARTReachedSet[] artReachedSets = new ARTReachedSet[threadNo];
    for (int i=0; i<threadNo; i++){
      artReachedSets[i] = new ARTReachedSet(reachedSets[i], artCpas[i]);
    }
    System.out.println("\t\t --- Checking feasability ---");
    CounterexampleTraceInfo mCounterexampleTraceInfo = manager.buildRgCounterexampleTrace(targetElement, reachedSets, errorThr);

    // if error is spurious refine
    if (mCounterexampleTraceInfo.isSpurious()) {
      System.out.println();
      System.out.println("\t\t --- Lazy abstraction ---");
      Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> refinementResult = performRefinement(reachedSets, mCounterexampleTraceInfo, errorThr);

      environment.printUnprocessedTransitions();
      // drop subtrees and change precision
      System.out.println();
      for(int tid : refinementResult.keySet()){
        for(Pair<ARTElement, RelyGuaranteePrecision> pair : refinementResult.get(tid)){
          ARTElement root = pair.getFirst();
          System.out.println("Removing subtree rooted at id:"+root.getElementId()+" at thread: "+tid);
          // kill the env transitions that were generated in the drop ARTs
          // if they killed transitions covered some other transitions, then make them valid again
          environment.killEnvironmetalEdges(root);
          // drop cut-off node in every thread
          RelyGuaranteePrecision precision = pair.getSecond();
          artReachedSets[tid].removeSubtree(root, precision);
        }
      }
      // process the remaining environmental transition

      environment.processEnvTransitions(errorThr);

      return true;
    } else {
      // we have a real error
      return false;
    }
  }


  /**
   * Returns multimap mapping: thread id -> cut-off element, new precision
   */
  private Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> performRefinement(ReachedSet[] reachedSets, CounterexampleTraceInfo info, int errorThr) throws CPAException {
    // TODO add cutting at the highest node, if no predicates found for a thread

    // multimap : thread no -> (ART element)
    Multimap<Integer, ARTElement> artMap = HashMultimap.create();


    // group interpolation elements  by threads
    for (AbstractElement aElement : info.getPredicatesForRefinmentKeys()){
      //Collection<AbstractionPredicate> newpreds = info.getPredicatesForRefinement(aElement);
      assert aElement instanceof ARTElement;
      ARTElement artElement = (ARTElement) aElement;
      RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(artElement, RelyGuaranteeAbstractElement.class);
      int tid = rgElement.getTid();
      artMap.put(tid, artElement);
    }

    Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> refinementMap = HashMultimap.create();
    // for every thread get cut-off elements and their new precision

    for (int tid : artMap.keySet()){
      System.out.println("Thread "+tid);
      Collection<ARTElement> artElements = artMap.get(tid);
      List<ARTNode> nodes = new Vector<ARTNode>(artElements.size());
      // find reachability relation between ART elements of the thread
      for (ARTElement artElem : artElements){
        ARTNode node = new ARTNode(artElem);
        nodes.add(node);
      }
      List<ARTNode> covered = new Vector<ARTNode>(artElements.size());
      for (ARTNode nodeA : nodes){
        Collection<AbstractionPredicate> preds = info.getPredicatesForRefinement(nodeA.getArtElement());
        if (!preds.isEmpty()){
          for  (ARTNode nodeB : nodes){
            if (nodeA != nodeB && !covered.contains(nodeB)){
              if (belongsToProperSubtree(nodeA.getArtElement(),nodeB.getArtElement())){
                nodeA.addChild(nodeB);
                covered.add(nodeB);
              }
            }
          }
        }
      }
      // ART element unreachable by other elements are the cut-off node
      Vector<ARTNode> cutoffNodes = new Vector<ARTNode>();
      for (ARTNode node : nodes){
        Collection<AbstractionPredicate> preds = info.getPredicatesForRefinement(node.getArtElement());
        if (node.getParent() == null && !preds.isEmpty()){
            cutoffNodes.add(node);
        }
      }

      // get new precision for the cutoff nodes;
      // precision of a cut-off node should include the precision of the elements below
      for (ARTNode node : cutoffNodes) {
        ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
        // in the error thread, add the precision of the error element
        if (tid == errorThr){
          AbstractElement targetElement = reachedSets[errorThr].getLastElement();
          Precision oldPrecision = reachedSets[tid].getPrecision(targetElement);
          RelyGuaranteePrecision oldRgPrecision = Precisions.extractPrecisionByType(oldPrecision, RelyGuaranteePrecision.class);
          pmapBuilder.putAll(oldRgPrecision.getPredicateMap());
        }
        for (ARTElement artElement : node.getARTSubtree()){
          // add old precision of the nodes below the cut-off node
          System.out.println("Getting element "+artElement.getElementId());
          Precision oldPrecision = reachedSets[tid].getPrecision(artElement);
          if (!reachedSets[tid].contains(artElement)){
            System.out.println();
          }
          RelyGuaranteePrecision oldRgPrecision = Precisions.extractPrecisionByType(oldPrecision, RelyGuaranteePrecision.class);
          pmapBuilder.putAll(oldRgPrecision.getPredicateMap());

          // add the new interpolants
          Collection<AbstractionPredicate> newpreds = info.getPredicatesForRefinement(artElement);
          CFANode loc = AbstractElements.extractLocation(artElement);
          pmapBuilder.putAll(loc, newpreds);
        }
        RelyGuaranteePrecision newPrecision = new RelyGuaranteePrecision(pmapBuilder.build(), new HashSet<AbstractionPredicate>());
        ARTElement artCutoffElement = node.getArtElement();
        refinementMap.put(tid, Pair.of(artCutoffElement, newPrecision));

        System.out.println("- Cut-off node id:"+node.getArtElement().getElementId()+" precision "+newPrecision);
      }
    }

    return refinementMap;

    // find cut points and new precision for every thread - one thread may have several cut p



/*
    List<Pair<ARTElement, RelyGuaranteePrecision>>  result = new Vector<Pair<ARTElement, RelyGuaranteePrecision>>();
    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> mapBuilder = ImmutableSetMultimap.builder();
    for (AbstractElement element : info.getPredicatesForRefinmentKeys()){


      ARTElement artElement = (ARTElement) element;
      RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(element, RelyGuaranteeAbstractElement.class);


    }



    for (int i=0; i<oldRgPrecisions.size(); i++){
     info.getPredicatesForRefinmentKeys()
    }
    Multimap<CFANode, AbstractionPredicate> oldPredicateMap = oldPrecision.getPredicateMap();
    Set<AbstractionPredicate> globalPredicates = oldPrecision.getGlobalPredicates();

    Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> firstInterpolationPoint = null;
    boolean newPredicatesFound = false;

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();

    pmapBuilder.putAll(oldPredicateMap);

    // iterate through pPath and find first point with new predicates, from there we have to cut the ART
    System.out.println("New predicates:");
    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> interpolationPoint : pPath) {
      CFANode loc = interpolationPoint.getSecond();
      Collection<AbstractionPredicate> newpreds = getPredicatesForARTElement(pInfo, interpolationPoint);
      System.out.println("- at "+loc+" : "+newpreds);

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
    System.out.println("Predicate map now is "+newPredicateMap);


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
    System.out.println("Root is id:"+root.getElementId());
    return Pair.of(root, newPrecision);
  }

  protected Collection<AbstractionPredicate> getPredicatesForARTElement(
      CounterexampleTraceInfo pInfo, Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> pInterpolationPoint) {
    return pInfo.getPredicatesForRefinement(pInterpolationPoint.getThird());
  }*/

}

  /**
   * Returns true if artElement1 lies on the path from the root to artElement2
   * @param pArtElement
   * @param pArtElement2
   * @return
   */
  private boolean belongsToProperSubtree(ARTElement artElement1, ARTElement artElement2) {

    Path cfaPath = ARTUtils.getOnePathTo(artElement2);
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> path = null;
    try {
      path = transformPath(cfaPath);
    } catch (CPATransferException e) {
      e.printStackTrace();
    }
    // find the highest occurence of artElement1 on the path
    boolean occursOnPath = false;
    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> triple: path){
      if (triple.getFirst().equals(artElement1)){
        occursOnPath = true;
        break;
      }
      else if  (triple.getFirst().equals(artElement2)){
        break;
      }

    }
    return occursOnPath;
  }



}


/**
 * Represents reachability relation between elements in an ART
 */
class ARTNode{

  private ARTElement  artElement;
  private ARTNode parent;
  private Collection<ARTNode> children;

  public ARTNode(ARTElement  artElement){
    this.artElement = artElement;
    this.parent = null;
    this.children = new HashSet<ARTNode>();
  }

  public ARTElement getArtElement() {
    return artElement;
  }

  public ARTNode getParent() {
    return parent;
  }

  public Collection<ARTNode> getChildren() {
    return children;
  }

  public void setParent(ARTNode pParent) {
    parent = pParent;
  }

  public void addChild(ARTNode child) {
    child.setParent(this);
    children.add(child);
  }

  public Collection<ARTElement> getARTSubtree() {
    Collection<ARTElement> reached = new HashSet<ARTElement>();
    reached.add(artElement);
    for (ARTNode child : children){
     reached.add(child.getArtElement());
    }
    return reached;
  }

  public String toString() {
    return ""+artElement.getElementId();
  }

}
