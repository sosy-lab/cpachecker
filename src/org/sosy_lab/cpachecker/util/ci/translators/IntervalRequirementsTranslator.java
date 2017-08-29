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
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.base.Preconditions;

public class IntervalRequirementsTranslator extends CartesianRequirementsTranslator<IntervalAnalysisState> {

  public IntervalRequirementsTranslator(final LogManager pLog) {
    super(IntervalAnalysisState.class, pLog);
  }

  @Override
  protected List<String> getVarsInRequirements(final IntervalAnalysisState pRequirement) {
    return new ArrayList<>(pRequirement.getIntervalMapView().keySet());
  }

  @Override
  protected List<String> getListOfIndependentRequirements(final IntervalAnalysisState pRequirement,
      final SSAMap pIndices, final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    for (String var : pRequirement.getIntervalMapView().keySet()) {
      if (pRequiredVars == null || pRequiredVars.contains(var)) {
        list.add(getRequirement(getVarWithIndex(var, pIndices), pRequirement.getIntervalMapView().get(var)));
      }
    }
    return list;
  }

  private String getRequirement(final String var, final Interval interval) {
    StringBuilder sb = new StringBuilder();
    boolean isMin = (interval.getLow() == Long.MIN_VALUE);
    boolean isMax = (interval.getHigh() == Long.MAX_VALUE);
    Preconditions.checkArgument(!isMin || !isMax);
    Preconditions.checkArgument(!interval.isEmpty());

    if (!isMin && !isMax) {
      sb.append(TranslatorsUtils.getVarInBoundsRequirement(var, interval.getLow(), interval.getHigh()));

    } else if (!isMin) {
      sb.append(TranslatorsUtils.getVarGreaterOrEqualValRequirement(var, interval.getLow()));

    } else if (!isMax) {
      sb.append(TranslatorsUtils.getVarLessOrEqualValRequirement(var, interval.getHigh()));
    }

    return sb.toString();
  }

}
