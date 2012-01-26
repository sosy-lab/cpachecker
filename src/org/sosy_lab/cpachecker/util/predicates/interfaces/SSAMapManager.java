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
package org.sosy_lab.cpachecker.util.predicates.interfaces;

import java.util.Collection;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;

public interface SSAMapManager {

    /**
     * Increments the indexes of specified variables, while others remain unchanged.
     * If a variable doesn't exist in the SSA map, then it has a default index of 1.
     * @param ssa
     * @param variables
     * @param shift
     * @return
     */
    SSAMap incrementMap(SSAMap ssa, Collection<String> variables, int shift);

    /**
     * Changes prime numbers of variables as specified by the map. Value -1 means unprimed variable.
     * @param ssa
     * @param map
     * @return
     */
    SSAMap changePrimeNo(SSAMap ssa, Map<Integer, Integer> map);

    /**
     * Returns the set of unprimed variable names that exist in ssa.
     * @param ssa
     * @return
     */
    Collection<String> getUnprimedVariables(SSAMap ssa);

    /**
     * builds a formula that represents the necessary variable assignments
     * to "merge" the two ssa maps. That is, for every variable X that has two
     * different ssa indices i and j in the maps, creates a new formula
     * (X_k = X_i) | (X_k = X_j), where k is a fresh ssa index.
     * Returns the formula described above, plus a new SSAMap that is the merge
     * of the two.
     *
     * @param ssa1 an SSAMap
     * @param ssa2 an SSAMap
     * @return A pair (Formula, SSAMap)
     */

    Pair<Pair<Formula, Formula>, SSAMap> mergeSSAMaps(SSAMap ssa1, SSAMap ssa2);


}
