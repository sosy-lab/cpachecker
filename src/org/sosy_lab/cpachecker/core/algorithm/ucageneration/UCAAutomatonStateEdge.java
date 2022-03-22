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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;

public class UCAAutomatonStateEdge {
  private final AutomatonState source;
  private final Optional<AutomatonState> target;
  private final CFAEdge edge;

  public UCAAutomatonStateEdge(AutomatonState pSource, AutomatonState pTarget, CFAEdge pEdge) {
    this.source = pSource;
    this.target = Optional.of(pTarget);
    this.edge = pEdge;
  }

  public UCAAutomatonStateEdge(AutomatonState pSource, CFAEdge pEdge) {
    this.source = pSource;
    this.target = Optional.empty();
    this.edge = pEdge;
  }

  public String getSourceName() {
    return getName(source);
  }

  public String getTargetName() {
    return this.target.isPresent()
        ? getName(target.orElseThrow())
        : UCAGenerator.NAME_OF_TEMP_STATE;
  }

  public AutomatonState getSource() {
    return source;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof UCAAutomatonStateEdge)) {
      return false;
    }
    UCAAutomatonStateEdge ucaEdge = (UCAAutomatonStateEdge) pO;
    return Objects.equals(source, ucaEdge.source)
        && Objects.equals(target, ucaEdge.target)
        && Objects.equals(edge, ucaEdge.edge);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, target, edge);
  }

  @Override
  public String toString() {
    return "UCAEdge{"
        + getSourceName()
        + "-- "
        + UCAGenerator.getEdgeString(edge)
        + " ->"
        + getTargetName()
        + '}';
  }

  public CFAEdge getEdge() {
    return edge;
  }

  String getName(AutomatonState s) {
    return s.isTarget() ? UCAGenerator.NAME_OF_ERROR_STATE : s.getInternalStateName();
  }
}
