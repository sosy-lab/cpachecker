/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.qMultiInterval;

import com.google.common.collect.TreeMultimap;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;

/** Some static operations which were needed on maps */
public class TreeMultimapOperations {

  /**
   * Merges two Multimaps and keeps all entries
   *
   * @param first first Multimap
   * @param second Multimap to add
   * @return the combined Multimap
   */
  public static TreeMultimap<Variable, Range> fuseMaps(
      TreeMultimap<Variable, Range> first, TreeMultimap<Variable, Range> second) {
    TreeMultimap<Variable, Range> temp = TreeMultimap.create(first);
    temp.putAll(second);

    return temp;
  }

  /**
   * Deprecated.Not used any more. It combines two Multimaps but combines two variables with
   * different Ranges to one variable with one Range. Using at own Risk.
   *
   * @param map1 first map
   * @param map2 map to add
   * @return the combination of two maps with each variable only being contained once
   */
  public static TreeMultimap<Variable, Range> easySumm(
      TreeMultimap<Variable, Range> map1, TreeMultimap<Variable, Range> map2) {
    TreeSet<Variable> vars = new TreeSet<>();
    TreeSet<Range> values = new TreeSet<>();
    TreeMultimap<Variable, Range> newmap = TreeMultimap.create();


    vars.addAll(map1.keySet());
    vars.addAll(map2.keySet());

    for (Variable var : vars) {
      values.addAll(map1.get(var));
      values.addAll(map2.get(var));
      assert !values.isEmpty();
      Range combinedRange = values.pollFirst();
      while (!values.isEmpty()) {
        combinedRange = combinedRange.union(values.pollFirst());

      }
      newmap.put(var, combinedRange);
    }

    return newmap;
  }

  /**
   * Combines two TreeMaps to one and applies the union operator to the Ranges if one variable is
   * contained in both maps.
   *
   * @param map1 first TreeMap
   * @param map2 TreeMap to combine
   * @return the combined version of the TreeMaps.
   */
  public static TreeMap<Variable, Range> easySumm(
      TreeMap<Variable, Range> map1, TreeMap<Variable, Range> map2) {
    TreeSet<Variable> vars = new TreeSet<>();
    TreeMap<Variable, Range> newmap = new TreeMap<>();


    vars.addAll(map1.keySet());
    vars.addAll(map2.keySet());

    for (Variable var : vars) {
      newmap.put(var, map1.get(var).clone().union(map2.get(var)));
    }

    return newmap;
  }
}
