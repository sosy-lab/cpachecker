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
import java.util.Set;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGVariables;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvTransitionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.Model.TermType;
import org.sosy_lab.cpachecker.util.predicates.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * Handles refinement of locations.
 */

@Options(prefix="cpa.rg.refinement")
public class RGLocationRefinementManager<T> implements StatisticsProvider{

  @Option(description="Print debugging info?")
  private boolean debug=false;

  @Option(toUppercase=true, values={"NONE", "WITNESS","MONOTONIC", "NONMONOTONIC"},
      description="Perform refinement of control locations if a feasible counterexample is found.")
  private String locationRefinement =  "NONE";

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  private final FormulaManager fManager;
  private final PathFormulaManager pfManager;
  private final RGEnvTransitionManager etManager;
  private final RGAbstractionManager absManager;
  private final SSAMapManager ssaManager;
  private final TheoremProver thmProver;
  private final InterpolatingTheoremProver<T> itpProver;
  private final RegionManager rManager;
  private final LogManager logger;
  private final Stats stats;

  private static RGLocationRefinementManager singleton;

  public static RGLocationRefinementManager<?> getInstance(FormulaManager pFManager, PathFormulaManager pPfManager, RGEnvTransitionManager pEtManager, RGAbstractionManager absManager, SSAMapManager pSsaManager,TheoremProver pThmProver, InterpolatingTheoremProver<?> itpProver, RegionManager pRManager, RGVariables variables, Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    if (singleton == null){
      singleton = new RGLocationRefinementManager(pFManager, pPfManager, pEtManager, absManager, pSsaManager, pThmProver,itpProver, pRManager, variables, pConfig, pLogger);
    }
    return singleton;
  }


  private RGLocationRefinementManager(FormulaManager pFManager, PathFormulaManager pPfManager, RGEnvTransitionManager pEtManager, RGAbstractionManager absManager, SSAMapManager pSsaManager,TheoremProver pThmProver, InterpolatingTheoremProver<T> itpProver, RegionManager pRManager, RGVariables variables, Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    pConfig.inject(this, RGLocationRefinementManager.class);

    this.fManager = pFManager;
    this.pfManager = pPfManager;
    this.etManager = pEtManager;
    this.absManager = absManager;
    this.ssaManager = pSsaManager;
    this.thmProver = pThmProver;
    this.itpProver = itpProver;
    this.rManager = pRManager;
    this.logger  = pLogger;

    this.stats = new Stats();
  }

  /**
   * Analyze the feasible counterexample. Depending on the configuration
   * it doesn't do anything, finds a witness or refines on locations.
   * @param counterexample
   * @return
   */
  public InterpolationTreeResult refine(InterpolationTreeResult counterexample) {
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
   */
  private InterpolationTreeResult monotonicRefinement(InterpolationTreeResult counterexample) {
    return counterexample;
  }



  private InterpolationTreeResult nonMonotonicRefinement(InterpolationTreeResult counterexample) {
    // TODO Auto-generated method stub
    return null;
  }



  private void getErrorPathsForTrunk(InterpolationTree tree) throws CPATransferException {

    List<InterpolationTreeNode> trunk = tree.getTrunk();
    Collection<ARTElement> allElems = getAllARTElementsOnBranch(trunk);
    Map<Pair<ARTElement, CFAEdge>, Formula> predMap = getBranchingPredicates(allElems);


    assert predMap.isEmpty();
    Map<Pair<ARTElement, CFAEdge>, Boolean> bMap = Collections.emptyMap();

    if (!predMap.isEmpty()){
      Formula bf = buildBranchingFormula(predMap, tree);
      assert false;
      // not supported yet
    }

    Collection<Path> paths = getPathsForBranch(trunk, allElems, bMap);




  }

  /**
   * Construct all paths on the branch using the map.
   * @param branch
   * @param elems
   * @param bMap
   * @return
   */
  private Collection<Path> getPathsForBranch(List<InterpolationTreeNode> branch, Collection<ARTElement> elems, Map<Pair<ARTElement, CFAEdge>, Boolean> bMap) {
    ARTElement start = branch.get(0).getArtElement();
    ARTElement stop  = branch.get(branch.size()-1).getArtElement();

    return getPathsBetween(start, stop, elems, bMap);
  }


  /**
   * Contruct all paths between start and stop, whose elements belong to the set of elements.
   * Branches are resolved using the map.
   * @param start
   * @param stop
   * @param elems
   * @param bMap
   * @return
   */
  private Collection<Path> getPathsBetween(ARTElement start, ARTElement stop, Collection<ARTElement> elems, Map<Pair<ARTElement, CFAEdge>, Boolean> bMap) {

    Deque<ARTElement> queue = new LinkedList<ARTElement>();
    // paths for the element
    Multimap<ARTElement, Path> pathMap = HashMultimap.create();
    pathMap.put(start, new Path());

    queue.add(start);

    // DFS
    while(queue.isEmpty()){
      ARTElement elem = queue.pop();

      if (elem.equals(stop)){
        continue;
      }

      Set<ARTElement> children = new LinkedHashSet<ARTElement>(elem.getChildMap().size());

      for (ARTElement child : elem.getChildARTs()){
        if (elems.contains(child)){
          children.add(child);
        }
      }

      if (children.size() > 1){
        // find the branch
      }
      else if (children.size() == 1){
        // extend the path
        ARTElement child = children.iterator().next();

        CFAEdge edge = getParentChildEdge(elem, child);
        assert edge != null;
        Collection<Path> paths = pathMap.get(elem);
        Collection<Path> edgePaths = pathsForEdge(edge, elems, bMap);
        Collection<Path> childPaths = concatPaths(paths, edgePaths);
        pathMap.putAll(child, childPaths);

      }
    }

    return null;
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


  private Collection<Path> concatPaths(Collection<Path> pPaths,
      Collection<Path> pEdgePaths) {
    // TODO Auto-generated method stub
    return null;
  }


  private Collection<Path> pathsForEdge(CFAEdge pEdge,Collection<ARTElement> pElems, Map<Pair<ARTElement, CFAEdge>, Boolean> pBMap) {
    // TODO Auto-generated method stub
    return null;
  }






  /**
   * Find all ART elements, local and environmental, on the branch.
   * @param tree
   * @return
   */
  private Collection<ARTElement> getAllARTElementsOnBranch(List<InterpolationTreeNode> branch) {

    InterpolationTreeNode lastNode = branch.get(branch.size()-1);
    ARTElement target = lastNode.getArtElement();
    Set<ARTElement> allElems = ARTUtils.getAllElementsOnPathsTo(target);


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


      CFANode pLoc = pathElement.retrieveLocationElement().getLocationNode();
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
  private Formula buildBranchingFormula(Map<Pair<ARTElement, CFAEdge>, Formula> bMap, InterpolationTree tree) throws CPATransferException {
    Formula bf = fManager.makeTrue();


    int trunkId = tree.getRoot().getUniqueId();
    Map<Integer, Integer> rMap = new HashMap<Integer, Integer>(1);
    rMap.put(-1, trunkId);

    for (Pair<ARTElement, CFAEdge> key : bMap.keySet()){
      ARTElement elem = key.getFirst();
      Formula pred = bMap.get(key);
      RGAbstractElement rgElem = AbstractElements.extractElementByType(elem, RGAbstractElement.class);
      CFAEdge edge = key.getSecond();

      Formula edgeF = null;

      if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
        RGCFAEdge rgEdge = (RGCFAEdge) edge;
        RGEnvTransition et = rgEdge.getRgEnvTransition();


        /* Find the unique id of the first tree node
         * matching the abstraction point */
        int tid = et.getTid();
        int elemId = et.getAbstractionElement().getElementId();

        Integer uniqueId = null;
        Set<InterpolationTreeNodeKey> nodeKeys = tree.getNodeMap().keySet();
        for (InterpolationTreeNodeKey nodeKey : nodeKeys){
          if (nodeKey.tid == tid && nodeKey.artElementId == elemId){
            uniqueId = nodeKey.getUniqueId();
            break;
          }
        }

        assert uniqueId != null;

        // debuging test - see it is the only matching node
        if (debug){
          for (InterpolationTreeNodeKey nodeKey : nodeKeys){
            if (nodeKey.tid == tid && nodeKey.artElementId == elemId){
              assert uniqueId.equals(nodeKey.uniqueId);
            }
          }
        }

        // get the refinement formula with the unique id
        edgeF = etManager.formulaForRefinement(rgElem, rgEdge.getRgEnvTransition(), uniqueId).getFormula();

        // rename the local part to the trunk
        edgeF = fManager.changePrimedNo(edgeF, rMap);
      }
      else if (edge.getEdgeType() == CFAEdgeType.AssumeEdge){
        PathFormula pf = rgElem.getPathFormula();
        pf = pfManager.makeEmptyPathFormula(pf);
        edgeF = pfManager.makeAnd(pf, edge).getFormula();

        // rename the local part to the trunk
        edgeF = fManager.changePrimedNo(edgeF, rMap);
      }
      else {
        throw new UnsupportedOperationException("An edge for branching must be either Assume or RelyGuarantee");
      }

      Formula equiv = fManager.makeEquivalence(pred, edgeF);
      bf = fManager.makeAnd(bf, equiv);
    }

    return bf;
  }

  /*private Path createPathFromPredicateValues(Path pPath, Map<Integer, Boolean> preds) {

    ARTElement errorElement = pPath.getLast().getFirst();
    Set<ARTElement> errorPathElements = ARTUtils.getAllElementsOnPathsTo(errorElement);

    Path result = new Path();
    ARTElement currentElement = pPath.getFirst().getFirst();
    while (!currentElement.isTarget()) {
      Set<ARTElement> children = currentElement.getChildren();

      ARTElement child;
      CFAEdge edge;
      switch (children.size()) {

      case 0:
        logger.log(Level.WARNING, "ART target path terminates without reaching target element!");
        return null;

      case 1: // only one successor, easy
        child = Iterables.getOnlyElement(children);
        edge = currentElement.getEdgeToChild(child);
        break;

      case 2: // branch
        // first, find out the edges and the children
        CFAEdge trueEdge = null;
        CFAEdge falseEdge = null;
        ARTElement trueChild = null;
        ARTElement falseChild = null;

        for (ARTElement currentChild : children) {
          CFAEdge currentEdge = currentElement.getEdgeToChild(currentChild);
          if (!(currentEdge instanceof AssumeEdge)) {
            logger.log(Level.WARNING, "ART branches where there is no AssumeEdge!");
            return null;
          }

          if (((AssumeEdge)currentEdge).getTruthAssumption()) {
            trueEdge = currentEdge;
            trueChild = currentChild;
          } else {
            falseEdge = currentEdge;
            falseChild = currentChild;
          }
        }
        if (trueEdge == null || falseEdge == null) {
          logger.log(Level.WARNING, "ART branches with non-complementary AssumeEdges!");
          return null;
        }
        assert trueChild != null;
        assert falseChild != null;

        // search first idx where we have a predicate for the current branching
        Boolean predValue = preds.get(currentElement.getElementId());
        if (predValue == null) {
          logger.log(Level.WARNING, "ART branches without direction information from solver!");
          return null;
        }

        // now select the right edge
        if (predValue) {
          edge = trueEdge;
          child = trueChild;
        } else {
          edge = falseEdge;
          child = falseChild;
        }
        break;

      default:
        logger.log(Level.WARNING, "ART splits with more than two branches!");
        return null;
      }

      if (!errorPathElements.contains(child)) {
        logger.log(Level.WARNING, "ART and direction information from solver disagree!");
        return null;
      }

      result.add(Pair.of(currentElement, edge));
      currentElement = child;
    }

    // need to add another pair with target element and outgoing edge
    Pair<ARTElement, CFAEdge> lastPair = pPath.getLast();
    if (currentElement != lastPair.getFirst()) {
      logger.log(Level.WARNING, "ART target path reached the wrong target element!");
      return null;
    }
    result.add(lastPair);

    return result;
  }*/

  protected Map<Integer, Boolean> getPredicateValuesFromModel(Model model) {

    Map<Integer, Boolean> preds = Maps.newTreeMap();
    for (AssignableTerm a : model.keySet()) {
      if (a instanceof Variable && a.getType() == TermType.Boolean) {

        String name = BRANCHING_PREDICATE_NAME_PATTERN.matcher(a.getName()).replaceFirst("");
        if (!name.equals(a.getName())) {
          // pattern matched, so it's a variable with __ART__ in it

          // no NumberFormatException because of RegExp match earlier
          Integer nodeId = Integer.parseInt(name);

          assert !preds.containsKey(nodeId);


          Boolean value = (Boolean)model.get(a);
          preds.put(nodeId, value);
        }
      }
    }
    return preds;
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
