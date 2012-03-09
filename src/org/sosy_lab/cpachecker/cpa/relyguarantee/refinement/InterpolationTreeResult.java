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
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;


/**
 * Stores information on the result of rely-guarantee interpolation -
 * either new predicates that eliminate a spurious counterexample or
 * trace(s) that witness the error.
 */
public class InterpolationTreeResult {

  private final boolean isSpurious;
  private final SetMultimap<InterpolationTreeNode, AbstractionPredicate> artMap;
  private final SetMultimap<InterpolationTreeNode, AbstractionPredicate> envMap;
  private final Map<Path, List<Pair<ARTElement, Pair<CFANode, CFANode>>>>  locMap;
  private final InterpolationTree tree;
  /** witness path for a feasible counterexample */
  private final Path path;


  /**
   * A spurious counterexample.
   * @return
   */
  public static InterpolationTreeResult spurious(InterpolationTree tree){
    SetMultimap<InterpolationTreeNode, AbstractionPredicate> artMap = LinkedHashMultimap.create();
    SetMultimap<InterpolationTreeNode, AbstractionPredicate> envMap = LinkedHashMultimap.create();
    Map<Path, List<Pair<ARTElement, Pair<CFANode, CFANode>>>>  locMap =
        new HashMap<Path, List<Pair<ARTElement, Pair<CFANode, CFANode>>>>();

    return new InterpolationTreeResult(true, artMap, envMap, locMap, tree, null);
  }

  /**
   * A feasible countrexample with its interpolation tree.
   * @param tree
   * @return
   */
  public static InterpolationTreeResult feasible(InterpolationTree tree){
    assert tree != null;

    return new InterpolationTreeResult(false, null, null, null, tree, null);
  }

  /**
   * A feasible countrexample with its interpolation tree and a concreate error path.
   * @param tree
   * @param witness
   * @return
   */
  public static InterpolationTreeResult feasibleWithWitness(InterpolationTree tree, Path witness){
    assert tree != null;
    assert witness != null;

    return new InterpolationTreeResult(false, null, null, null, tree, witness);
  }

  private InterpolationTreeResult(boolean spurious,
      SetMultimap<InterpolationTreeNode, AbstractionPredicate> artMap,
      SetMultimap<InterpolationTreeNode, AbstractionPredicate> envMap,
      Map<Path, List<Pair<ARTElement, Pair<CFANode, CFANode>>>>  locMap,
      InterpolationTree tree,
      Path path) {
    this.isSpurious = spurious;
    this.artMap = artMap;
    this.envMap = envMap;
    this.locMap = locMap;
    this.tree = tree;
    this.path = path;
  }


  public boolean isSpurious(){
    return isSpurious;
  }

  public void addPredicatesForRefinement(InterpolationTreeNode e, Collection<AbstractionPredicate> preds) {
    assert isSpurious : "Predicates for refimenent cannot be added to a feasible counterexample.";
    artMap.putAll(e, preds);
  }

  public void addEnvPredicatesForRefinement(InterpolationTreeNode e,  Collection<AbstractionPredicate> preds) {
    assert isSpurious : "Predicates for refimenent cannot be added to a feasible counterexample.";
    envMap.putAll(e, preds);
  }

  public void addLocationMapForRefinement(Map<Path, List<Pair<ARTElement, Pair<CFANode, CFANode>>>> pPathInqMap) {
    assert isSpurious : "Locations for refimenent cannot be added to a feasible counterexample.";
    locMap.putAll(pPathInqMap);
  }

  public InterpolationTree getTree() {
    return tree;
  }

  public Path getPath() {
    return path;
  }

  public SetMultimap<InterpolationTreeNode, AbstractionPredicate> getArtRefinementMap() {
    return artMap;
  }

  public SetMultimap<InterpolationTreeNode, AbstractionPredicate> getEnvRefinementMap() {
    return envMap;
  }

  public Map<Path, List<Pair<ARTElement, Pair<CFANode, CFANode>>>> getPathRefinementMap() {
    return locMap;
  }







}
