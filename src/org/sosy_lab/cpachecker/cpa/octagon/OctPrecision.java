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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.octagon")
public class OctPrecision implements Precision {

  private final Set<String> trackedVars;
  private final Configuration config;
  private final ValueAnalysisPrecision valuePrecision;

  @Option(name="refiner", description="turn the refiner on or off, default is off")
  private boolean refiner = false;

  public OctPrecision(Configuration pConfig) throws InvalidConfigurationException {
    valuePrecision = new ValueAnalysisPrecision("", pConfig, Optional.<VariableClassification>absent());
    config = pConfig;
    config.inject(this);
    trackedVars = new HashSet<>();
  }

  public OctPrecision(OctPrecision pOctPrecision, Multimap<CFANode, MemoryLocation> pIncrement) {
    valuePrecision = new ValueAnalysisPrecision(pOctPrecision.valuePrecision, pIncrement);
    config = pOctPrecision.config;
    trackedVars = new HashSet<>();
    for (MemoryLocation mem : pIncrement.values()) {
      trackedVars.add(mem.getAsSimpleString());
    }
  }

  public int getSize() {
    return trackedVars.size();
  }

  public boolean isTracked(String varName) {
    if(!refiner) {
      return true;
    }
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
