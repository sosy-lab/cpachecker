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

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.RGCFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGApplicationInfo;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGVariables;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvTransitionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver.AllSatPredicates;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Handles refinement of locations.
 */

@Options(prefix="cpa.rg.refinement")
public class RGLocationRefinementManager implements StatisticsProvider{

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(toUppercase=true, values={"NONE", "WITNESS","MONOTONIC", "NONMONOTONIC"},
      description="Perform refinement of control locations if a feasible counterexample is found.")
  private String locationRefinement =  "MONOTONIC";

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  private final FormulaManager fManager;
  private final PathFormulaManager pfManager;
  private final RGEnvTransitionManager etManager;
  private final RGAbstractionManager absManager;
  private final SSAMapManager ssaManager;
  private final TheoremProver thmProver;
  private final RegionManager rManager;
  private final RGEnvironmentManager envManager;
  private final RGCFA[] cfas;
  private final int tidNo;
  private final LogManager logger;
  private final Stats stats;

  private static RGLocationRefinementManager singleton;

  public static RGLocationRefinementManager getInstance(FormulaManager pFManager, PathFormulaManager pPfManager, RGEnvTransitionManager pEtManager, RGAbstractionManager absManager, SSAMapManager pSsaManager,TheoremProver pThmProver, RegionManager pRManager, RGEnvironmentManager envManager, RGCFA[] cfas, RGVariables variables, Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    if (singleton == null){
      singleton = new RGLocationRefinementManager(pFManager, pPfManager, pEtManager, absManager, pSsaManager, pThmProver, pRManager, envManager, cfas, variables, pConfig, pLogger);
    }
    return singleton;
  }


  private RGLocationRefinementManager(FormulaManager pFManager, PathFormulaManager pPfManager, RGEnvTransitionManager pEtManager, RGAbstractionManager absManager, SSAMapManager pSsaManager,TheoremProver pThmProver,  RegionManager pRManager, RGEnvironmentManager envManager, RGCFA[] cfas, RGVariables variables, Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    pConfig.inject(this, RGLocationRefinementManager.class);

    this.fManager = pFManager;
    this.pfManager = pPfManager;
    this.etManager = pEtManager;
    this.absManager = absManager;
    this.ssaManager = pSsaManager;
    this.thmProver = pThmProver;
    this.rManager = pRManager;
    this.envManager = envManager;
    this.cfas = cfas;
    this.tidNo = cfas.length;
    this.logger  = pLogger;

    this.stats = new Stats();
  }

  /**
   * Analyze the feasible counterexample. Depending on the configuration
   * it doesn't do anything, finds a witness or refines on locations.
   * @param counterexample
   * @return
   * @throws CPATransferException
   */
  public InterpolationTreeResult refine(InterpolationTreeResult counterexample) throws CPATransferException {
    if (counterexample.isSpurious() || locationRefinement.equals("NONE")){
      // already refined or nothing to do
      return counterexample;
    }
    else if (locationRefinement.equals("WITNESS")){
      return findWitness(counterexample);
    }
    else if (locationRefinement.equals("MONOTONIC")){
      return monotonicRefinement(counterexample);
    }
    else {
      return nonMonotonicRefinement(counterexample);
    }
  }

  private InterpolationTreeResult findWitness(InterpolationTreeResult counterexample) {
    return null;
  }

  /**
   * Monotonic refinement of the trace.
   * @param counterexample
   * @return
   * @throws CPATransferException
   */
  private InterpolationTreeResult monotonicRefinement(InterpolationTreeResult counterexample) throws CPATransferException {
    Collection<Path> paths = getErrorPathsForTrunk(counterexample.getTree());

    /* Map: tid -> pair of unequal nodes */
    Multimap <Integer, Pair<CFANode, CFANode>> inqMap = LinkedHashMultimap.create();

    for (Path pi : paths){
      Multimap <Integer, Pair<CFANode, CFANode>> threadLocInq = findLocationInequalities(pi);
      inqMap.putAll(threadLocInq);
    }

    return counterexample;
  }

  /**
   * Traverses the path and finds mismatching locations that make it spurious.
   * Returns a map: thread id -> pair of mismatchig locations. The map is empty if
   * the path is feasible w.r.t to locations.
   * @param pi
   * @return
   */
  private Multimap<Integer, Pair<CFANode, CFANode>> findLocationInequalities(Path pi) {

    Multimap<Integer, Pair<CFANode, CFANode>> inqMap = LinkedHashMultimap.create();

    // expected locations
    CFANode[] pc = new CFANode[tidNo];

    // initialize
    for (int i=0; i<tidNo; i++){
      pc[i] = cfas[i].getStartNode();
    }

    // execute
    for (Pair<ARTElement, CFAEdge> pair : pi){
      ARTElement element = pair.getFirst();
      CFAEdge edge = pair.getSecond();

      RGAbstractElement rgElement = AbstractElements.extractElementByType(element, RGAbstractElement.class);
      int tid = rgElement.getTid();
      CFANode loc = element.retrieveLocationElement().getLocationNode();

      if (pc[tid].equals(loc)){
        pc[tid] = edge.getSuccessor();
      } else {
        // mismatch pc[tid] != loc
        Pair<CFANode, CFANode> mismatch = Pair.of(pc[tid], loc);
        inqMap.put(tid, mismatch);
        break;
      }
    }

    return inqMap;
  }


  private InterpolationTreeResult nonMonotonicRefinement(InterpolationTreeResult counterexample) {
    // TODO Auto-generated method stub
    return null;
  }


  private Collection<Path> getErrorPathsForTrunk(InterpolationTree tree) throws CPATransferException {

    List<InterpolationTreeNode> trunk = tree.getTrunk();
    Collection<ARTElement> allElems = getAllARTElementsOnBranch(trunk);
    Map<Pair<ARTElement, CFAEdge>, Formula> predMap = getBranchingPredicates(allElems);

    // one model describes one path - no branching is specified by the empty model
    Collection<RGBranchingModel> bModels;

    if (!predMap.isEmpty()){
      bModels = getBranchingModels(predMap, tree);

    } else {
      RGBranchingModel emptyModel = new RGBranchingModel();
      bModels = Collections.singleton(emptyModel);
    }

    Collection<Path> paths = getPathsForBranch(trunk, allElems, bModels);
    return paths;
  }

  /**
   * Determines all feasible combinations of branches specified in predMap.
   * @param predMap
   * @param bf
   * @param tree
   * @return
   * @throws CPATransferException
   */
  private Collection<RGBranchingModel> getBranchingModels(Map<Pair<ARTElement, CFAEdge>, Formula> predMap, InterpolationTree tree) throws CPATransferException {

    Formula bf = buildBranchingFormula(predMap, tree);

    System.out.println("bf: "+bf);

    for (InterpolationTreeNode node : tree){
      Formula f = node.getPathFormula().getFormula();
      System.out.println("\t-"+node+" "+f);
      bf = fManager.makeAnd(bf,f);
    }

    AllSatPredicates allPredSat = thmProver.allSatPredicate(bf, predMap.values());

    assert allPredSat.getCount() < Integer.MAX_VALUE;

    Collection<RGBranchingModel> bModels = new Vector<RGBranchingModel>(allPredSat.getCount());

    for (Map<Formula, Boolean> predModel : allPredSat.getPredicateModels()){
      RGBranchingModel bModel = getBranchningModel(predMap, predModel);
      bModels.add(bModel);
    }

    return bModels;
  }


  /**
   * Constructs a branching model by composing the predicate map with the predicate model.
   * @param predMap
   * @param predModel
   * @return
   */
  private RGBranchingModel getBranchningModel(Map<Pair<ARTElement, CFAEdge>, Formula> predMap, Map<Formula, Boolean> predModel) {

    RGBranchingModel bModel = new RGBranchingModel();

    for (Pair<ARTElement, CFAEdge> key : predMap.keySet()){
      Formula pred = predMap.get(key);
      Boolean value = predModel.get(pred);
      assert value != null;
      bModel.put(key, value);
    }

    return bModel;
  }


  /**
   * Construct all paths on the branch using the map.
   * @param branch
   * @param elems
   * @param bModels
   * @return
   */
  private Collection<Path> getPathsForBranch(List<InterpolationTreeNode> branch, Collection<ARTElement> elems, Collection<RGBranchingModel> bModels) {
    assert !bModels.isEmpty();

    Collection<Path> pis = new Vector<Path>(bModels.size());
    ARTElement start  = branch.get(branch.size()-1).getArtElement();
    ARTElement stop = branch.get(0).getArtElement();

    // get path for each model
    for (RGBranchingModel bModel : bModels){
      Path pi = getPathBetween(start, stop, elems, bModel);
      pathPrinter(pi);
      System.out.println("\n\n\n");
      pis.add(pi);
    }

    return pis;
  }


  /**
   * Constructs the paths between start and stop, whose elements belong to the set of elements.
   * Branches are resolved using the map, which should specify exactly one path.<
   * @param start
   * @param stop
   * @param elems
   * @param bModel
   * @return
   */
  private Path getPathBetween(ARTElement start, ARTElement stop, Collection<ARTElement> elems, RGBranchingModel bModel) {
    assert !stop.isDestroyed();

    // paths for elements
    Map<ARTElement, Path> pathMap = new HashMap<ARTElement, Path>();
    Deque<ARTElement> queue = new LinkedList<ARTElement>();
    pathMap.put(start, new Path());

    queue.add(start);

    // DFS
    while(!queue.isEmpty()){
      ARTElement elem = queue.pop();
      assert !elem.isDestroyed();

      Path elemPath = pathMap.get(elem);

      if (elem.equals(stop)){
        continue;
      }

      // find children that are in elems
      Map<ARTElement, CFAEdge> childMap = new HashMap<ARTElement, CFAEdge>();
      for (ARTElement child : elem.getChildARTs()){
        if (elems.contains(child)){
          CFAEdge edge = elem.getChildMap().get(child);
          childMap.put(child, edge);
        }
      }

      ARTElement childTaken = null;
      if (childMap.size() > 1){
        // find the branch
        for (Entry<ARTElement, CFAEdge> entry : childMap.entrySet()){
          Pair<ARTElement, CFAEdge> key = Pair.of(elem, entry.getValue());
          boolean isTaken = bModel.get(key);
          if (isTaken){
            // one and only one branch can be taken
            assert childTaken == null;
            childTaken = entry.getKey();
          }
        }
      }
      else if (childMap.size() == 1){
        // extend the path
        Entry<ARTElement, CFAEdge> entry = childMap.entrySet().iterator().next();
        childTaken = entry.getKey();
      }

      assert childTaken != null;
      CFAEdge edge = childMap.get(childTaken);
      Path edgePath = pathsForEdge(edge, elem, bModel);
      Path childPath = new Path(elemPath);
      childPath.addAll(edgePath);
      pathMap.put(childTaken, childPath);
      queue.addLast(childTaken);
    }

    Path pi = pathMap.get(stop);
    assert pi != null;
    return pi;
  }


  /**
   * Constructs the path for a local or environmental edge.
   * @param edge
   * @param elem
   * @param bModel
   * @return
   */
  private Path pathsForEdge(CFAEdge edge, ARTElement elem, RGBranchingModel bModel) {
    if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
      // environmental edge
      RGCFAEdge rgEdge = (RGCFAEdge) edge;
      RGEnvTransition et = rgEdge.getRgEnvTransition();

      // if the target element is covered, then use the covering element
      ARTElement reachedTarget = et.getTargetARTElement();
      if (reachedTarget.isDestroyed()){
        reachedTarget = reachedTarget.getMergedWith();
      }

      return getPathBetween(et.getSourceARTElement(), reachedTarget, et.getGeneratingARTElements(), bModel);
    }
    else {
      // local edge
      Path pi = new Path();
      pi.push(Pair.of(elem, edge));
      return pi;
    }
  }


  /**
   * Returns the edge between start and child.
   * @param start
   * @param child
   * @return
   */
  private CFAEdge getParentChildEdge(ARTElement start, ARTElement child) {
    CFAEdge edge = start.getChildMap().get(child);
    return edge;
  }


  /**
   * Find all ART elements, local and environmental, on the branch.
   * @param tree
   * @return
   */
  private Collection<ARTElement> getAllARTElementsOnBranch(List<InterpolationTreeNode> branch) {

    if (branch.isEmpty()){
      return Collections.emptySet();
    }

    InterpolationTreeNode lastNode = branch.get(0);
    ARTElement target = lastNode.getArtElement();
    Set<ARTElement> allElems = ARTUtils.getAllElementsOnPathsTo(target);

    // check if all nodes were found
    if (debug){
      for (InterpolationTreeNode node : branch){
        assert allElems.contains(node.getArtElement());
      }
    }

    // add generating nodes for env. applications
    for (InterpolationTreeNode node : branch){
      RGApplicationInfo appInfo = node.getAppInfo();

      if (appInfo != null){
        Map<Integer, RGEnvTransition> envMap = node.getAppInfo().envMap;

        for (RGEnvTransition et: envMap.values()){
          allElems.addAll(et.getGeneratingARTElements());
        }
      }
    }

    return allElems;
  }

  /**
   * Returns a map from branching CFA edges, both local and environmental, to their predicates.
   * @param elementsOnPath
   * @return
   * @throws CPATransferException
   */
  private Map<Pair<ARTElement, CFAEdge>, Formula> getBranchingPredicates(Collection<ARTElement> elementsOnPath) throws CPATransferException {

    Map<Pair<ARTElement, CFAEdge>, Formula> predMap = new HashMap<Pair<ARTElement, CFAEdge>, Formula>();

    for (final ARTElement pathElement : elementsOnPath) {
      assert !pathElement.isDestroyed();

      Set<ARTElement> children = new LinkedHashSet<ARTElement>(pathElement.getChildMap().size());
      int localChildren = 0;

      // consider only the children that are on the path
      for (ARTElement child : pathElement.getChildARTs()){
        if (elementsOnPath.contains(child)){
          children.add(child);
        }
      }

      if (children.size() > 1) {

        for (ARTElement child : children){
          CFAEdge edge = getParentChildEdge(pathElement, child);

          Pair<ARTElement, CFAEdge> key = Pair.of(pathElement, edge);

          if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
            String name = BRANCHING_PREDICATE_NAME+pathElement.getElementId()+"_"+child.getElementId();
            Formula pred = fManager.makePredicateVariable(name, 0);
            predMap.put(key, pred);
          }
          else if (edge instanceof AssumeEdge){
            localChildren++;
            if (localChildren > 2){
              throw new UnsupportedOperationException("Can't handle more than two local edges");
            }

            String name = BRANCHING_PREDICATE_NAME+pathElement.getElementId()+"_"+child.getElementId();
            Formula pred = fManager.makePredicateVariable(name, 0);
            predMap.put(key, pred);
          }
        }
      }
    }
    return predMap;
  }

  /**
   * Build a branching formula from the map.
   * @param tree
   * @param pBMap
   * @return
   * @throws CPATransferException
   */
  private Formula buildBranchingFormula(Map<Pair<ARTElement, CFAEdge>, Formula> predMap, InterpolationTree tree) throws CPATransferException {
    Formula bf = fManager.makeTrue();

    Map<Integer, Integer> rMap = new HashMap<Integer, Integer>(1);

    for (Pair<ARTElement, CFAEdge> key : predMap.keySet()){
      ARTElement elem = key.getFirst();
      Formula pred = predMap.get(key);
      RGAbstractElement rgElem = AbstractElements.extractElementByType(elem, RGAbstractElement.class);
      CFAEdge edge = key.getSecond();

      /*
       * Find nodes matching the tid and element id of the abstraction point. All these
       * nodes represent the same part of ART, so for branching its enough to first the
       * first one.
       */
      int tid;
      int elemId;

      if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
        RGCFAEdge rgEdge = (RGCFAEdge) edge;
        RGEnvTransition et = rgEdge.getRgEnvTransition();
        tid = et.getTid();
        elemId = et.getAbstractionElement().getElementId();
      }
      else if (edge.getEdgeType() == CFAEdgeType.AssumeEdge){
        ARTElement laElement = RGCPA.findLastAbstractionARTElement(elem);
        elemId = laElement.getElementId();
        tid = rgElem.getTid();
      }
      else {
        throw new UnsupportedOperationException("branching must be either Assume or RelyGuarantee type. Problem: "+edge);
      }

      Collection<InterpolationTreeNode> nodes = tree.getNodesByTidAndElementId(tid, elemId);
      InterpolationTreeNode node = nodes.iterator().next();
      int uniqueId = node.getUniqueId();

      /* Construct the edge formula */
      Formula edgeF;
      if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
        RGCFAEdge rgEdge = (RGCFAEdge) edge;
        edgeF = etManager.formulaForRefinement(rgElem, rgEdge.getRgEnvTransition(), uniqueId).getFormula();
      }
      else if (edge.getEdgeType() == CFAEdgeType.AssumeEdge){
        PathFormula pf = rgElem.getPathFormula();
        pf = pfManager.makeEmptyPathFormula(pf);
        edgeF = pfManager.makeAnd(pf, edge).getFormula();
      }
      else {
        throw new UnsupportedOperationException("branching must be either Assume or RelyGuarantee type. Problem: "+edge);
      }

      /* Rename the edge formula */

      if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
        InterpolationTreeNode parentBranchNode = tree.getParentBranchId(node);

        if (parentBranchNode != null){
          rMap.put(-1, parentBranchNode.uniqueId);
        }
      } else {
        rMap.put(-1, uniqueId);
      }

      edgeF = fManager.changePrimedNo(edgeF, rMap);

      Formula equiv = fManager.makeEquivalence(pred, edgeF);
      bf = fManager.makeAnd(bf, equiv);
    }

    return bf;
  }

  private void pathPrinter(Path pi) {
    System.out.println();
    for (Pair<ARTElement, CFAEdge> pair : pi){
      ARTElement element = pair.getFirst();
      RGAbstractElement rgElement = AbstractElements.extractElementByType(element, RGAbstractElement.class);
      CFANode loc = element.retrieveLocationElement().getLocationNode();
      CFAEdge edge = pair.getSecond();
      System.out.println("("+rgElement.getTid()+","+element.getElementId()+","+loc+")\t"+edge.getRawStatement());
    }
  }


  @Override
  public void collectStatistics(Collection<Statistics> scoll) {
    scoll.add(stats);
  }

  public static class Stats implements Statistics {
    public final Timer nonModularTimer     = new Timer();

    public int formulaNo                = 0;
    public int unsatChecks              = 0;

    @Override
    public String getName() {
      return "RGLocationRefinementManager";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached){
      out.println("time on non-modular refinement:  " + nonModularTimer);
    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }

  }



}
