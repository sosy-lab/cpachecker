/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.invariants.balancer.prh12;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionSet;


public class AugmentationColumn extends UsableColumn {

  private final int colNum = -1;
  private Map<Integer, AssumptionSet> asets;
  private Set<Integer> requests;
  final int height = Integer.MIN_VALUE;

  public AugmentationColumn() {
    asets = new HashMap<>();
    requests = new HashSet<>();
  }

  @Override
  public int getColNum() {
    return colNum;
  }

  @Override
  boolean isAugCol() {
    return true;
  }

  @Override
  public void clearRequests() {
    requests = new HashSet<>();
  }

  public void addSet(Integer r, AssumptionSet a) {
    asets.put(r, a);
  }

  public boolean rowHasAugColOption(Integer r) {
    return asets.keySet().contains(r);
  }

  @Override
  public void makeRequest(Integer r) {
    requests.add(r);
  }

  @Override
  public AssumptionSet getRequestedAssumptions() {
    AssumptionSet aset = new AssumptionSet();
    for (Integer r : requests) {
      aset.addAll(asets.get(r));
    }
    return aset;
  }


}
