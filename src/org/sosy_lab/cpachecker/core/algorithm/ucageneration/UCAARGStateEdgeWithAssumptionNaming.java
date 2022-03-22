// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.ucageneration;

import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;

public class UCAARGStateEdgeWithAssumptionNaming extends UCAARGStateEdge {
  public UCAARGStateEdgeWithAssumptionNaming(
      ARGState pSource, ARGState pTarget, CFAEdge pEdge, Optional<AbstractionFormula> pAssumption) {
    super(pSource, pTarget, pEdge, pAssumption);
  }

  public UCAARGStateEdgeWithAssumptionNaming(ARGState pSource, CFAEdge pEdge) {
    super(pSource, pEdge);
  }

  public UCAARGStateEdgeWithAssumptionNaming(ARGState pSource, ARGState pTarget, CFAEdge pEdge) {
    super(pSource, pTarget, pEdge);
  }

  @Override
  public String getSourceName() {
    Optional<AutomatonState> automatonState = UCAGenerator.getWitnessAutomatonState(source);
    if (automatonState.isPresent()) {
      return UCAGenerator.getName(automatonState.orElseThrow());
    }
    return UCAGenerator.getName(source);
  }

  @Override
  public String getTargetName() {
    if (this.target.isPresent()) {
      Optional<AutomatonState> automatonState =
          UCAGenerator.getWitnessAutomatonState(target.orElseThrow());
      if (automatonState.isPresent()) {
        return UCAGenerator.getName(automatonState.orElseThrow());
      }
    }
    return this.target.isPresent()
        ? UCAGenerator.getName(target.orElseThrow())
        : UCAGenerator.NAME_OF_TEMP_STATE;
  }
}
