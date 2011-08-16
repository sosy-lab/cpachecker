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
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
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
import com.google.common.collect.SetMultimap;


@Options(prefix="cpa.relyguarantee")
public class RelyGuaranteeRefiner{
  @Option(description="Print debugging info?")
  private boolean print=true;

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

  @Option(name="refinement.restartAnalysis",
      description="restart analysis after refinement")
      private boolean restartAnalysis = false;

  @Option(description="List of variables global to multiple threads")
  protected String[] globalVariables = {};

  private RelyGuaranteeRefinementManager manager;
  private final ARTCPA[] artCpas;

  private Object lastErrorPath;

  private static RelyGuaranteeRefiner rgRefiner;

  /**
   * Singleton instance of RelyGuaranteeRefiner.
   * @param cpas
   * @param pConfig
   * @return
   * @throws InvalidConfigurationException
   */
  public static RelyGuaranteeRefiner getInstance(final ConfigurableProgramAnalysis[] cpas, Configuration pConfig) throws InvalidConfigurationException {
    if (rgRefiner == null){
      rgRefiner = new RelyGuaranteeRefiner(cpas, pConfig);
    }
    return rgRefiner;
  }

  public RelyGuaranteeRefiner(final ConfigurableProgramAnalysis[] cpas, Configuration pConfig) throws InvalidConfigurationException{
    pConfig.inject(this, RelyGuaranteeRefiner.class);
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

    Timer refinementTimer = new Timer();
    refinementTimer.start();

    //assert checkART(reachedSets[errorThr]);
    assert reachedSets[errorThr].getLastElement() instanceof ARTElement;
    ARTElement targetElement = (ARTElement) reachedSets[errorThr].getLastElement();
    assert (targetElement).isTarget();

    ARTReachedSet[] artReachedSets = new ARTReachedSet[threadNo];
    for (int i=0; i<threadNo; i++){
      artReachedSets[i] = new ARTReachedSet(reachedSets[i], artCpas[i]);
    }
    System.out.println("\t\t\t ----- Interpolation -----");
    CounterexampleTraceInfo mCounterexampleTraceInfo = manager.buildRgCounterexampleTrace(targetElement, reachedSets, errorThr);

    // if error is spurious refine
    if (mCounterexampleTraceInfo.isSpurious()) {
      System.out.println();

      Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> refinementResult;
      if (restartAnalysis){
        System.out.println("\t\t\t ----- Restarting analysis -----");
        refinementResult = restartingRefinement(reachedSets, mCounterexampleTraceInfo);
      } else {
        System.out.println("\t\t\t ----- Lazy abstraction -----");
        refinementResult = lazyRefinement(reachedSets, mCounterexampleTraceInfo, errorThr);
      }

      // drop subtrees and change precision
      System.out.println();
      for(int tid : refinementResult.keySet()){
        for(Pair<ARTElement, RelyGuaranteePrecision> pair : refinementResult.get(tid)){
          ARTElement root = pair.getFirst();
          // drop cut-off node in every thread
          RelyGuaranteePrecision precision = pair.getSecond();
          Set<ARTElement> parents = new HashSet<ARTElement>(root.getParents());

          System.out.println();
          System.out.println("BEFORE: parents of id:"+root.getElementId());
          for (ARTElement parent : parents){
            System.out.println("-parent id:"+parent.getElementId()+" precision: "+Precisions.extractPrecisionByType(artReachedSets[tid].getPrecision(parent), RelyGuaranteePrecision.class));
          }
          artReachedSets[tid].removeSubtree(root, precision);
          System.out.println();
          System.out.println("AFTER: parents of id:"+root.getElementId());
          for (ARTElement parent : parents){
            System.out.println("-parent id:"+parent.getElementId()+" precision: "+Precisions.extractPrecisionByType(artReachedSets[tid].getPrecision(parent), RelyGuaranteePrecision.class));
          }
        }
      }
      if (restartAnalysis){
        System.out.println("\t\t\t --- Dropping all env transitions ---");
        environment.resetEnvironment();

        environment.resetEnvironment();
        for (int i=0; i<reachedSets.length;i++){
          assert reachedSets[i].getReached().size()==1;
        }

      } else {
        // kill the env transitions that were generated in the drop ARTs
        // if they killed transitions covered some other transitions, then make them valid again
        System.out.println("\t\t\t --- Processing env transitions ---");
        environment.printUnprocessedTransitions();
        environment.killEnvironmetalEdges(refinementResult.keySet(), artReachedSets);
        // process the remaining environmental transition
        environment.processEnvTransitions(errorThr);
      }
      refinementTimer.stop();
      System.out.println();
      System.out.println("Refinement time: "+refinementTimer.printMaxTime());
      return true;
    } else {
      // a real error
      refinementTimer.stop();
      System.out.println();
      System.out.println("Refinement time: "+refinementTimer.printMaxTime());
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
  private Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> restartingRefinement(ReachedSet[] reachedSets, CounterexampleTraceInfo info) {

    Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> refinementMap = HashMultimap.create();
    // multimap : thread no -> (ART element)
    Multimap<Integer, ARTElement> artMap = HashMultimap.create();

    // group interpolation elements  by threads
    for (AbstractElement aElement : info.getPredicatesForRefinmentKeys()){
      //Collection<AbstractionPredicate> newpreds = info.getPredicatesForRefinement(aElement);
      assert aElement instanceof ARTElement;
      ARTElement artElement = (ARTElement) aElement;
      RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(artElement, RelyGuaranteeAbstractElement.class);
      int tid = rgElement.getTid();
      if (!info.getPredicatesForRefinement(aElement).isEmpty()){
        artMap.put(tid, artElement);
      }
    }

    boolean newPredicates = false;
    // for every thread sum up interpolants and add it to the initial element
    for (int tid=0 ; tid < reachedSets.length; tid++){
      ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
      // add the precision of the initial element
      ARTElement inital  = (ARTElement) reachedSets[tid].getFirstElement();
      Precision prec = reachedSets[tid].getPrecision(inital);
      RelyGuaranteePrecision rgPrecision = Precisions.extractPrecisionByType(prec, RelyGuaranteePrecision.class);
      SetMultimap<CFANode, AbstractionPredicate> oldPreds = rgPrecision.getPredicateMap();

      pmapBuilder.putAll(oldPreds);

      for (ARTElement artElement : artMap.get(tid)){
        // add the new interpolants

        Collection<AbstractionPredicate> newpreds = info.getPredicatesForRefinement(artElement);
        CFANode loc = AbstractElements.extractLocation(artElement);
        pmapBuilder.putAll(loc, newpreds);
        if (!oldPreds.get(loc).containsAll(newpreds)){
          newPredicates = true;
        }
      }

      RelyGuaranteePrecision newPrecision = new RelyGuaranteePrecision(pmapBuilder.build(), rgPrecision.getGlobalPredicates());
      for (ARTElement initChild : inital.getChildren()){
        refinementMap.put(tid, Pair.of(initChild, newPrecision));
        System.out.println();
        System.out.println("Thread "+tid+": cut-off node id:"+initChild.getElementId()+" precision "+newPrecision);
      }

    }
    assert newPredicates;

    return refinementMap;
  }

  /**
   * Returns multimap mapping: thread id -> cut-off element, new precision
   */
  private Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> lazyRefinement(ReachedSet[] reachedSets, CounterexampleTraceInfo info, int errorThr) throws CPAException {
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
      Collection<ARTElement> artElements = artMap.get(tid);
      List<ARTNode> nodes = new Vector<ARTNode>(artElements.size());
      // find reachability relation between ART elements of the thread
      for (ARTElement artElem : artElements){
        ARTNode node = new ARTNode(artElem);
        nodes.add(node);
      }
      List<ARTNode> covered = new Vector<ARTNode>(artElements.size());
      for (ARTNode nodeA : nodes){
        CFANode loc = AbstractElements.extractLocation(nodeA.getArtElement());
        Precision prec = reachedSets[tid].getPrecision(nodeA.getArtElement());
        RelyGuaranteePrecision rgPrec = Precisions.extractPrecisionByType(prec, RelyGuaranteePrecision.class);
        SetMultimap<CFANode, AbstractionPredicate> oldPredmap = rgPrec.getPredicateMap();
        Set<AbstractionPredicate> oldPreds = oldPredmap.get(loc);
        Collection<AbstractionPredicate> newPreds = info.getPredicatesForRefinement(nodeA.getArtElement());
        if (!oldPreds.containsAll(newPreds)){
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
        CFANode loc = AbstractElements.extractLocation(node.getArtElement());
        Precision prec = reachedSets[tid].getPrecision(node.getArtElement());
        RelyGuaranteePrecision rgPrec = Precisions.extractPrecisionByType(prec, RelyGuaranteePrecision.class);
        SetMultimap<CFANode, AbstractionPredicate> oldPredmap = rgPrec.getPredicateMap();
        Set<AbstractionPredicate> oldPreds = oldPredmap.get(loc);
        Collection<AbstractionPredicate> newPreds = info.getPredicatesForRefinement(node.getArtElement());
        if (node.getParent() == null && !oldPreds.containsAll(newPreds)){
          cutoffNodes.add(node);
        }
      }

      // correctness assertion
      assertionCutoffNodes(tid, nodes, cutoffNodes, reachedSets, info);

      // get new precision for the cutoff nodes;
      // precision of a cut-off node should include the precision of the elements below
      for (ARTNode node : cutoffNodes) {
        ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
        // add precision of the dropped element
        ARTElement artCutoffElement = node.getArtElement();
        Precision prec = reachedSets[tid].getPrecision(artCutoffElement);
        RelyGuaranteePrecision rgPrecision = Precisions.extractPrecisionByType(prec, RelyGuaranteePrecision.class);
        pmapBuilder.putAll(rgPrecision.getPredicateMap());

        // in the error thread, add the precision of the error element
        if (tid == errorThr){
          AbstractElement targetElement = reachedSets[errorThr].getLastElement();
          Precision oldPrecision = reachedSets[tid].getPrecision(targetElement);
          RelyGuaranteePrecision oldRgPrecision = Precisions.extractPrecisionByType(oldPrecision, RelyGuaranteePrecision.class);
          pmapBuilder.putAll(oldRgPrecision.getPredicateMap());
        }
        for (ARTElement artElement : node.getARTSubtree()){
          // add old precision of the nodes below the cut-off node
          Precision oldPrecision = reachedSets[tid].getPrecision(artElement);
          RelyGuaranteePrecision oldRgPrecision = Precisions.extractPrecisionByType(oldPrecision, RelyGuaranteePrecision.class);
          pmapBuilder.putAll(oldRgPrecision.getPredicateMap());

          // add the new interpolants
          Collection<AbstractionPredicate> newpreds = info.getPredicatesForRefinement(artElement);
          CFANode loc = AbstractElements.extractLocation(artElement);
          pmapBuilder.putAll(loc, newpreds);
        }
        RelyGuaranteePrecision newPrecision = new RelyGuaranteePrecision(pmapBuilder.build(), new HashSet<AbstractionPredicate>());

        refinementMap.put(tid, Pair.of(artCutoffElement, newPrecision));
        System.out.println();
        System.out.println("Thread "+tid+": cut-off node id:"+node.getArtElement().getElementId()+" precision "+newPrecision);
      }

    }

    return refinementMap;
  }

  /**
   * Checks if cut-off nodes are correct for thread tid.
   * @param pTid
   * @param pNodes
   * @param pCutoffNodes
   * @param pReachedSets
   * @param pInfo
   */
  private void assertionCutoffNodes(int tid, List<ARTNode> nodes, Vector<ARTNode> cutoffNodes, ReachedSet[] reachedSets, CounterexampleTraceInfo info) {


    // check correctnes of cutoffNodes
    for (ARTNode node : nodes){
      CFANode loc = AbstractElements.extractLocation(node.getArtElement());
      Precision prec = reachedSets[tid].getPrecision(node.getArtElement());
      RelyGuaranteePrecision rgPrec = Precisions.extractPrecisionByType(prec, RelyGuaranteePrecision.class);
      SetMultimap<CFANode, AbstractionPredicate> oldPredmap = rgPrec.getPredicateMap();
      Set<AbstractionPredicate> oldPreds = oldPredmap.get(loc);
      Collection<AbstractionPredicate> newPreds = info.getPredicatesForRefinement(node.getArtElement());
      // cut-off has some new predicates


      if (cutoffNodes.contains(node)){
        assert (!oldPreds.containsAll(newPreds));
      } else {
        // node is independent if it has no predictates and no cut-off node can reach it
        boolean indi=false;
        if (oldPreds.containsAll(newPreds)){
          indi = true;
          for (ARTNode cutNode : cutoffNodes){
            if (belongsToProperSubtree(cutNode.getArtElement(),node.getArtElement())){
              indi = false;
              break;
            }
          }
        }

        // node belongs to some cut-off node
        boolean belongs=false;
        for (ARTNode cutNode : cutoffNodes){
          if (cutNode.getARTSubtree().contains(node.getArtElement())){
            belongs = true;
            break;
          }
        }
        if ((indi && belongs) || (!indi && !belongs)){
          System.out.println();
        }
        assert (indi || belongs) && (!indi || !belongs);
      }
    }
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

  public Set<ARTElement> getARTSubtree() {
    Set<ARTElement> reached = new HashSet<ARTElement>();
    reached.add(artElement);
    for (ARTNode child : children){
      reached.addAll(child.getARTSubtree());
    }
    return reached;
  }

  public String toString() {
    return ""+artElement.getElementId();
  }

}
