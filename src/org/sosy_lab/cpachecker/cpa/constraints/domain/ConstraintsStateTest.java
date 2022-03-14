// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.domain;

import static com.google.common.truth.Truth.assertThat;

import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Unit tests for {@link ConstraintsState} */
public class ConstraintsStateTest {

  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

  private final Type defType = CNumericTypes.INT;

  private final MemoryLocation memLoc1 = MemoryLocation.forIdentifier("id1");
  private final SymbolicIdentifier id1 = factory.newIdentifier(memLoc1);
  private final SymbolicExpression idExp1 = factory.asConstant(id1, defType);
  private final SymbolicExpression numExp = factory.asConstant(new NumericValue(5), defType);

  private final Constraint constr1 = factory.equal(idExp1, numExp, defType, defType);
  private final Constraint constr2 =
      (Constraint) factory.lessThan(idExp1, numExp, defType, defType);
  private final Constraint constr3 =
      (Constraint) factory.lessThanOrEqual(idExp1, numExp, defType, defType);

  private ConstraintsState state;

  @Before
  public void setUp() {
    state = new ConstraintsState();

    state.add(constr1);
    state.add(constr2);
    state.add(constr3);
  }

  @Test
  public void testIterator() {
    Iterator<Constraint> it = state.iterator();

    it.next();
    it.remove();
    assertThat(state).hasSize(2);
    assertThat(it.hasNext()).isTrue();
    assertThat(state).contains(constr2);
    assertThat(state).contains(constr3);

    it.next();
    it.remove();
    assertThat(it.hasNext()).isTrue();
    assertThat(state).hasSize(1);
    assertThat(state).contains(constr3);

    it.next();
    it.remove();
    assertThat(it.hasNext()).isFalse();
    assertThat(state).isEmpty();
  }
}
