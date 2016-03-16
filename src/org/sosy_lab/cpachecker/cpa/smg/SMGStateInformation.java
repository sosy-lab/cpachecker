/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class SMGStateInformation {

  private static final SMGStateInformation EMPTY = new SMGStateInformation();

  private final Set<SMGEdgeHasValue> hvEdges;
  private final Map<Integer, SMGEdgePointsTo> ptEdges;

  private SMGStateInformation() {
    hvEdges = ImmutableSet.of();
    ptEdges = ImmutableMap.of();
  }

  private SMGStateInformation(Set<SMGEdgeHasValue> pHvEdges, Map<Integer, SMGEdgePointsTo> pPtEdges) {
    hvEdges = ImmutableSet.copyOf(pHvEdges);
    ptEdges = ImmutableMap.copyOf(pPtEdges);
  }

  public static SMGStateInformation of() {
    return EMPTY;
  }

  public static SMGStateInformation of(Set<SMGEdgeHasValue> pHvEdges, Map<Integer, SMGEdgePointsTo> pPtEdges) {
    return new SMGStateInformation(pHvEdges, pPtEdges);
  }

  public Map<Integer, SMGEdgePointsTo> getPtEdges() {
    return ptEdges;
  }

  public Set<SMGEdgeHasValue> getHvEdges() {
    return hvEdges;
  }

  @Override
  public String toString() {
    return hvEdges.toString() + "\n" + ptEdges.toString();
  }
}