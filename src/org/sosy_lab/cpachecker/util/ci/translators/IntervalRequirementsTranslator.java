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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.cpa.interval.UnifyAnalysisState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class IntervalRequirementsTranslator extends CartesianRequirementsTranslator<UnifyAnalysisState> {

  public IntervalRequirementsTranslator(final LogManager pLog) {
    super(UnifyAnalysisState.class, pLog);
  }

  @Override
  protected List<String> getVarsInRequirements(final UnifyAnalysisState pRequirement) {
    return new ArrayList<>(pRequirement.getVariables());
  }

  @Override
  protected List<String> getListOfIndependentRequirements(final UnifyAnalysisState pRequirement,
      final SSAMap pIndices, final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    List<String> varList = new ArrayList<>();

    //TODO maybe it is possible to use this method without sort
    for (MemoryLocation var : pRequirement.getIntervalMap().keySet()) {
      if (pRequiredVars == null || pRequiredVars.contains(var.getAsSimpleString())) {
          varList.add(var.getAsSimpleString());
      }
    }
    Collections.sort(varList);
    for (String var : varList) {
        list.add(getRequirement(getVarWithIndex(var, pIndices), pRequirement.getInterval(var)));
      }
    return list;
  }

  private String getRequirement(final String var, final NumberInterface interval) {
    StringBuilder sb = new StringBuilder();
  //TODO instanseof
    boolean isMin = (interval.getLow().longValue() == Long.MIN_VALUE);
    boolean isMax = (interval.getHigh().longValue() == Long.MAX_VALUE);
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
