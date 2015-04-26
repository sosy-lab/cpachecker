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
package org.sosy_lab.cpachecker.util.ci.translators;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

public class IntervalRequirementsTranslator extends CartesianRequirementsTranslator<IntervalAnalysisState> {

  public IntervalRequirementsTranslator(Class<IntervalAnalysisState> pAbstractStateClass, Configuration pConfig,
      ShutdownNotifier pShutdownNotifier, LogManager pLog) {
    super(pAbstractStateClass, pConfig, pShutdownNotifier, pLog);
  }

  @Override
  protected List<String> getVarsInRequirements(IntervalAnalysisState pRequirement) {
    List<String> list = new ArrayList<>();
    list.addAll(pRequirement.getIntervalMapView().keySet());
    return list;
  }

  @Override
  protected List<String> getListOfIndependentRequirements(IntervalAnalysisState pRequirement, SSAMap pIndices) {
    List<String> list = new ArrayList<>();
    for (String key : pRequirement.getIntervalMapView().keySet()) {
      Interval interval = pRequirement.getIntervalMapView().get(key);
      list.add("(= " + getVarWithIndex(key, pIndices) + " " + interval + ")");
    }
    return list;
  }

  private String getRequirement(String var, Interval interval) {
    StringBuilder sb = new StringBuilder();
    boolean isMin = (interval.getLow() == Long.MIN_VALUE);
    boolean isMax = (interval.getLow() == Long.MAX_VALUE);

    if (!isMin && !isMax) {
      sb.append("(and (>= ");
      sb.append(var);
      sb.append(" ");
      sb.append(interval.getLow());
      sb.append(") (<= ");
      sb.append(var);
      sb.append(" ");
      sb.append(interval.getHigh());
      sb.append("))");

    } else if (!isMin) {
      sb.append("(>= ");
      sb.append(var);
      sb.append(" ");
      sb.append(interval.getLow());
      sb.append(")");

    } else if (!isMax) {
      sb.append("(<= ");
      sb.append(var);
      sb.append(" ");
      sb.append(interval.getHigh());
      sb.append(")");
    }

    return sb.toString();
  }

}
