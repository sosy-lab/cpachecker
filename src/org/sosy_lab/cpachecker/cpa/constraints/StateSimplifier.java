/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints;

import java.util.Iterator;
import java.util.Map;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicIdentifier;

/**
 * Simplifier for {@link ConstraintsState}s.
 * Provides different methods for simplifying a <code>ConstraintsState</code>
 * through {@link #simplify(ConstraintsState)}.
 */
public class StateSimplifier {

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  public StateSimplifier(MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {
    machineModel = pMachineModel;
    logger = pLogger;
  }

  /**
   * Simplifies the given {@link ConstraintsState}.
   * Applies different simplifications to it.
   *
   * <p>The returned state will hold the same amount of information as the given state.</p>
   *
   * @param pState the state to simplify
   * @return the simplified state
   */
  public ConstraintsState simplify(ConstraintsState pState) {
    ConstraintsState newState;

    newState = replaceSymbolicIdentifiersWithConcreteValues(pState);
    newState = removeTrivialConstraints(newState);

    return newState;
  }

  public ConstraintsState replaceSymbolicIdentifiersWithConcreteValues(ConstraintsState pState) {
    final IdentifierAssignment definiteAssignment = pState.getDefiniteAssignment();

    if (definiteAssignment.isEmpty()) {
      return pState;
    }

    ConstraintsState newState = pState.copyOf();
    newState.clear();

    for (Map.Entry<SymbolicIdentifier, Value> entry : definiteAssignment.entrySet()) {
      final SymbolicIdentifier id = entry.getKey();
      final Value newValue = entry.getValue();

      ConstraintIdentifierReplacer replacer = new ConstraintIdentifierReplacer(id, newValue, machineModel, logger);
      for (Constraint c : pState) {
        newState.add(c.accept(replacer));
      }
    }

    return newState;
  }

  private ConstraintsState removeTrivialConstraints(ConstraintsState pState) {
    ConstraintsState newState = pState.copyOf();

    Iterator<Constraint> it = newState.iterator();

    while (it.hasNext()) {
      Constraint currConstraint = it.next();

      if (currConstraint.isTrivial()) {
        it.remove();
      }
    }

    return newState;
  }
}
