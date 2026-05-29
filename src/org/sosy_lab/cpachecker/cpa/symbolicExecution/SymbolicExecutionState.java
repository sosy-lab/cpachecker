// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.symbolicExecution;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierRenamer;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public record SymbolicExecutionState(
    ValueAnalysisState valueAnalysisState, ConstraintsState constraintsState)
    implements AbstractState {

  public SymbolicExecutionState renameIDs(Set<SymbolicIdentifier> toRename) {
    SymbolicIdentifierRenamer visitor = new SymbolicIdentifierRenamer(new HashMap<>(), toRename);
    return new SymbolicExecutionState(
        valueAnalysisState().renameIDs(visitor), constraintsState().renameIDs(visitor));
  }

  public Set<SymbolicIdentifier> getSymbolicIdentifiers() {
    Set<SymbolicIdentifier> identifiers = new HashSet<>();

    for (Constraint constraint : constraintsState) {
      assert constraint != null;
      identifiers.addAll(SymbolicValues.getContainedSymbolicIdentifiers(constraint));
    }

    for (Entry<MemoryLocation, ValueAndType> constant : valueAnalysisState.getConstants()) {
      if (constant.getValue().getValue() instanceof SymbolicValue symVal) {
        identifiers.addAll(SymbolicValues.getContainedSymbolicIdentifiers(symVal));
      }
    }
    return identifiers;
  }

  public static SymbolicExecutionState copyOf(SymbolicExecutionState pState) {
    return new SymbolicExecutionState(
        ValueAnalysisState.copyOf(pState.valueAnalysisState()), pState.constraintsState());
  }
}
