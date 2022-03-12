// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.defuse;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class DefUseDefinition implements AbstractState {
  private final String variableName;
  private final CFAEdge assigningEdge;

  public DefUseDefinition(String variableName, CFAEdge assigningEdge) {
    this.variableName = Preconditions.checkNotNull(variableName);
    this.assigningEdge = assigningEdge;
  }

  public String getVariableName() {
    return variableName;
  }

  public CFAEdge getAssigningEdge() {
    return assigningEdge;
  }

  @Override
  public int hashCode() {
    return variableName.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof DefUseDefinition)) {
      return false;
    }

    DefUseDefinition otherDef = (DefUseDefinition) other;
    return otherDef.variableName.equals(variableName)
        && Objects.equals(otherDef.assigningEdge, assigningEdge);
  }
}
