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
package org.sosy_lab.cpachecker.cpa.explicit.refiner.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGElement;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;


public class PredicateMap {
  /**
   * the mapping from program location to a collection of predicates
   */
  private ImmutableSetMultimap<CFANode, AbstractionPredicate> predicateMap;

  public Pair<ARGElement, CFANode> firstInterpolationPoint = null;

  /**
   * This method acts as the constructor of the class.
   *
   * Given a list of sets of predicates and a program path, it creates the mapping from program location to a collection of predicates.
   *
   * @param pathPredicates the predicates as returned from the refinement
   * @param pPath the path to the error location
   */
  public PredicateMap(List<Collection<AbstractionPredicate>> pathPredicates, List<Pair<ARGElement, CFANode>> pPath) {
    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> builder = ImmutableSetMultimap.builder();

    int i = 0;
    for (Collection<AbstractionPredicate> predicates : pathPredicates) {
      if (predicates.size() > 0) {
        CFANode currentLocation = pPath.get(i).getSecond();

        // add each predicate to the respective set of the predicate map
        for (AbstractionPredicate predicate : predicates) {
          builder.put(currentLocation, predicate);
        }

        if(firstInterpolationPoint == null)
          firstInterpolationPoint = Pair.of(pPath.get(i).getFirst(), pPath.get(i).getSecond());
      }

      i++;
    }

    predicateMap = builder.build();
  }

  public ImmutableMultimap<CFANode, AbstractionPredicate> getPredicateMapping() {
    return predicateMap;
  }

  /**
   * This method decides whether or not the given location is a interpolation point.
   *
   * @param location the location for which to decide whether it is a interpolation point or not
   * @return true if it is a interpolation point, else false
   */
  public boolean isInterpolationPoint(CFANode location) {
    return predicateMap.containsKey(location);
  }

  /**
   * This method returns those variables that are referenced in the predicates and groups them by program locations.
   *
   * @return a mapping from program locations to variables referenced in predicates at that program location
   */
  public Multimap<CFANode, String> determinePrecisionIncrement(FormulaManager fmgr) {
    Multimap<CFANode, String> increment = HashMultimap.create();

    // for each program location in the mapping ...
    for (Map.Entry<CFANode, AbstractionPredicate> predicateAtLocation : predicateMap.entries()) {
      CFANode currentLocation               = predicateAtLocation.getKey();
      AbstractionPredicate currentPredicate = predicateAtLocation.getValue();

      // ... get the names of the variables referenced in that predicate ...
      Collection<String> atoms = fmgr.extractVariables(currentPredicate.getSymbolicAtom());

      // ... and add them to variables-at-location-mapping
      if(!atoms.isEmpty()) {
        increment.putAll(currentLocation, atoms);
      }
    }

    return increment;
  }

  @Override
  public String toString() {
    return predicateMap.toString();
  }
}