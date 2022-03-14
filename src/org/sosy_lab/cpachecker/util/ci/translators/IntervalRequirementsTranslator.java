// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci.translators;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

public class IntervalRequirementsTranslator
    extends CartesianRequirementsTranslator<IntervalAnalysisState> {

  public IntervalRequirementsTranslator(final LogManager pLog) {
    super(IntervalAnalysisState.class, pLog);
  }

  @Override
  protected List<String> getVarsInRequirements(final IntervalAnalysisState pRequirement) {
    return new ArrayList<>(pRequirement.getIntervalMap().keySet());
  }

  @Override
  protected List<String> getListOfIndependentRequirements(
      final IntervalAnalysisState pRequirement,
      final SSAMap pIndices,
      final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    for (String var : pRequirement.getIntervalMap().keySet()) {
      if (pRequiredVars == null || pRequiredVars.contains(var)) {
        list.add(getRequirement(getVarWithIndex(var, pIndices), pRequirement.getInterval(var)));
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
      sb.append(
          TranslatorsUtils.getVarInBoundsRequirement(var, interval.getLow(), interval.getHigh()));

    } else if (!isMin) {
      sb.append(TranslatorsUtils.getVarGreaterOrEqualValRequirement(var, interval.getLow()));

    } else if (!isMax) {
      sb.append(TranslatorsUtils.getVarLessOrEqualValRequirement(var, interval.getHigh()));
    }

    return sb.toString();
  }
}
