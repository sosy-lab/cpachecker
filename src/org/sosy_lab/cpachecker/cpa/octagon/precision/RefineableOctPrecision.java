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
package org.sosy_lab.cpachecker.cpa.octagon.precision;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;


public class RefineableOctPrecision implements IOctPrecision {
  private final Set<String> trackedVars;
  private final ValueAnalysisPrecision valuePrecision;

  public RefineableOctPrecision(Configuration config) throws InvalidConfigurationException  {
    valuePrecision = new ValueAnalysisPrecision("", config, Optional.<VariableClassification>absent());
    trackedVars = new HashSet<>();
  }

  /**
   * A constructor which increments the included ValueAnalysisPrecision and the
   * OctPrecision.
   */
  public RefineableOctPrecision(RefineableOctPrecision pOctPrecision, Multimap<CFANode, MemoryLocation> pIncrement) {
    valuePrecision = new ValueAnalysisPrecision(pOctPrecision.valuePrecision, pIncrement);
    trackedVars = new HashSet<>();
    trackedVars.addAll(pOctPrecision.trackedVars);
    for (MemoryLocation mem : pIncrement.values()) {
      trackedVars.add(mem.getAsSimpleString());
    }
  }

  /**
   * A constructor which only increments the OctPrecision, and lets the included
   * ValueAnalysisPrecision as it was.
   */
  public RefineableOctPrecision(RefineableOctPrecision pOctPrecision, Set<String> pIncrement) {
    valuePrecision = pOctPrecision.valuePrecision;
    trackedVars = new HashSet<>();
    trackedVars.addAll(pOctPrecision.trackedVars);
    trackedVars.addAll(pIncrement);
  }

  public Set<String> getTrackedVars() {
    return Collections.unmodifiableSet(trackedVars);
  }

  public int getSize() {
    return trackedVars.size();
  }

  @Override
  public boolean isTracked(String varName, CType type) {
    return trackedVars.contains(varName);
  }

  public ValueAnalysisPrecision getValueAnalysisPrecision() {
    return valuePrecision;
  }

  @Override
  public String toString() {
    return trackedVars.toString();
  }
}
