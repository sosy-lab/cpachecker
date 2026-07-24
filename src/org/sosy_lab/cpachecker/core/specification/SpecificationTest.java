// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.specification;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.util.test.TestUtils;

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

  /**
   * Combining an SV-LIB property with a C property is invalid, independent of whether the analyzed
   * program is an SV-LIB or a C program. In both cases {@link Specification#fromFiles} must reject
   * the combination.
   */
  @Test
  public void testInvalidCombinationSvLibCProperty() throws Exception {
    // Can be enabled once
    // https://gitlab.com/sosy-lab/software/cpachecker/-/work_items/1674
    // is fixed
    // assertInvalidCombinationSvLibCProperty("SvLib", "test/programs/sv-lib/simple-correct.svlib");
    assertInvalidCombinationSvLibCProperty("C", "test/programs/simple/__VERIFIER_assume.c");
  }

  private void assertInvalidCombinationSvLibCProperty(String language, String program)
      throws Exception {
    Configuration config = TestUtils.configurationForTest().setOption("language", language).build();
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();

    CFA cfa =
        new CFACreator(config, logger, shutdownNotifier)
            .parseFileAndCreateCFA(ImmutableList.of(program));

    ImmutableList<Path> specFiles =
        ImmutableList.of(
            // property file specifying the SV-LIB annotation-correctness property
            Path.of("config/properties/correct-annotations.prp"),
            // specification automaton for C programs (matches against C statements)
            Path.of("config/properties/unreach-call.prp"));

    assertThrows(
        InvalidConfigurationException.class,
        () -> Specification.fromFiles(specFiles, cfa, config, logger, shutdownNotifier));
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
