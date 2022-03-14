// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.specification;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;

public class SpecificationTest {

  @Test
  public void testEquals() throws InvalidAutomatonException {
    final Automaton automaton =
        new Automaton(
            "Dummy",
            ImmutableMap.of(),
            ImmutableList.of(new AutomatonInternalState("init", ImmutableList.of())),
            "init");

    new EqualsTester()
        .addEqualityGroup(
            Specification.alwaysSatisfied(),
            Specification.alwaysSatisfied(),
            Specification.fromAutomata(ImmutableList.of()))
        .addEqualityGroup(Specification.fromAutomata(ImmutableList.of(automaton)))
        .addEqualityGroup(
            new Specification(
                ImmutableSet.of(),
                ImmutableSet.of(CommonVerificationProperty.REACHABILITY),
                ImmutableListMultimap.of(Path.of("test.spc"), automaton)))
        .testEquals();
  }

  @Test
  public void testEmpty() {
    Specification empty = Specification.alwaysSatisfied();
    assertThat(empty.getPathToSpecificationAutomata()).isEmpty();
    assertThat(empty.getProperties()).isEmpty();
    assertThat(empty.getFiles()).isEmpty();
    assertThat(empty.getSpecificationAutomata()).isEmpty();
  }
}
