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

/**
 * Unit tests for {@link ConstraintsState}
 */
public class ConstraintsStateTest {

  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

  private final Type defType = CNumericTypes.INT;

  private final MemoryLocation memLoc1 = MemoryLocation.valueOf("id1");
  private final SymbolicIdentifier id1 = factory.newIdentifier(memLoc1);
  private final SymbolicExpression idExp1 = factory.asConstant(id1, defType);
  private final SymbolicExpression numExp = factory.asConstant(new NumericValue(5), defType);

  private final Constraint constr1 = factory.equal(idExp1, numExp, defType, defType);
  private final Constraint constr2 = (Constraint) factory.lessThan(idExp1, numExp, defType, defType);
  private final Constraint constr3 = (Constraint) factory.lessThanOrEqual(idExp1, numExp, defType,
      defType);

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
