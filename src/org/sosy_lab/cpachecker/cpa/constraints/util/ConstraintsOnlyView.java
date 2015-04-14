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
package org.sosy_lab.cpachecker.cpa.constraints.util;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableSet;

/**
 * View for {@link ConstraintsState} that displays definite assignments for symbolic identifiers
 * as constraints.
 * This way, when, for example, comparing constraints states, one only has to look at the
 * constraints of the view.
 *
 * <p>Note: The types of the constraints are not always accurate, as constraints have to be
 * created out of definite assignments where only a value is available.</p>
 */
public class ConstraintsOnlyView extends ForwardingSet<Constraint> {
  private static final Type DUMMY_TYPE_NUMERIC = CNumericTypes.INT;

  private final ImmutableSet<Constraint> constraints;

  public ConstraintsOnlyView(final ConstraintsState pState) {
    constraints = getAllValues(pState);
  }

  public ConstraintsOnlyView(
      final Set<Constraint> pConstraints,
      final IdentifierAssignment pDefiniteAssignment
  ) {
    constraints = getAllValues(pConstraints, pDefiniteAssignment);
  }

  private ImmutableSet<Constraint> getAllValues(ConstraintsState pState) {
    return getAllValues(pState, pState.getDefiniteAssignment());
  }

  private ImmutableSet<Constraint> getAllValues(
      final Set<Constraint> pConstraints,
      final IdentifierAssignment pDefiniteAssignment
  ) {
    Set<Constraint> allValues = new HashSet<>();

    for (Constraint c : pConstraints) {
      allValues.add(c);
    }

    allValues.addAll(transformToConstraints(pDefiniteAssignment));

    return ImmutableSet.copyOf(allValues);
  }

  private Set<Constraint> transformToConstraints(final IdentifierAssignment pDefiniteAssignment) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    Set<Constraint> constraints = new HashSet<>();

    // each definite assignment in itself is a constraint, so we add these too
    for (Entry<SymbolicIdentifier, Value> entry : pDefiniteAssignment.entrySet()) {
      SymbolicIdentifier id = entry.getKey();
      Value value = entry.getValue();

      assert !(value instanceof SymbolicValue)
          : "Definite assignment of symbolic identifier is symbolic value";

      Type type = getType(value);

      SymbolicExpression idExp = factory.asConstant(id, type);
      SymbolicExpression valueExp = factory.asConstant(value, type);

      // the type doesn't really matter here, as long as its the same for all constraints created
      // this way.
      constraints.add(factory.equal(idExp, valueExp, DUMMY_TYPE_NUMERIC, DUMMY_TYPE_NUMERIC));
    }

    return constraints;
  }

  private Type getType(Value pValue) {
    // We only use CTypes. The type compatibility doesn't really matter, as long as the expressions
    // are comparable.

    if (pValue instanceof BooleanValue) {
      return CNumericTypes.BOOL;
    } else {
      return DUMMY_TYPE_NUMERIC;
    }
  }

  @Override
  protected Set<Constraint> delegate() {
    return constraints;
  }
}
