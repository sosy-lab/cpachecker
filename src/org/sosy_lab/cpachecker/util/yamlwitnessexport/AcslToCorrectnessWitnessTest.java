// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

public class AcslToCorrectnessWitnessTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private static String describe(AbstractInvariantEntry pEntry) {
    return switch (pEntry) {
      case InvariantEntry invariant ->
          invariant.getType()
              + " "
              + invariant.getLocation().getLine()
              + ": "
              + invariant.getValue();
      case FunctionContractEntry contract ->
          contract.getType()
              + " "
              + contract.getLocation().getLine()
              + ": "
              + contract.getRequires()
              + " / "
              + contract.getEnsures();
      default -> throw new AssertionError(pEntry);
    };
  }

  @Test
  public void exportAnnotationsInAcsl() throws Exception {
    Path witnessDirectory = tempFolder.getRoot().toPath();
    Configuration config =
        TestUtils.configurationForTest()
            .setOption("parser.collectACSLAnnotations", "true")
            .setOption("cpa.arg.yamlProofWitness", witnessDirectory.resolve("%s.yml").toString())
            .build();
    LogManager logger = LogManager.createTestLogManager();
    CFA cfa =
        new CFACreator(config, logger, ShutdownNotifier.createDummy())
            .parseFileAndCreateCFA(ImmutableList.of("test/programs/acsl/round_trip.c"));

    new AcslToCorrectnessWitness(config, cfa, Specification.alwaysSatisfied(), logger).export();

    List<AbstractEntry> entries =
        AutomatonWitnessV2ParserUtils.parseYAML(
            witnessDirectory.resolve(YAMLWitnessVersion.V2d2 + ".yml"));
    assertThat(entries).hasSize(1);
    List<AbstractInvariantEntry> content = ((InvariantSetEntry) entries.getFirst()).content;
    assertThat(FluentIterable.from(content).transform(AbstractInvariantEntry::getFormat).toSet())
        .containsExactly(YAMLWitnessExpressionType.ACSL);
    assertThat(FluentIterable.from(content).transform(AcslToCorrectnessWitnessTest::describe))
        .containsAtLeast(
            "loop_invariant 66: (i <= 10) && (0 <= i)",
            "location_invariant 66: n > 0 ? n >= 1 : n <= 0",
            "location_invariant 74: i == 10",
            "function_contract 37: ((0 <= x) && (x < 100)) && \\valid(p) / (((\\result =="
                + " (square(x))) && (\\result >= \\old(x))) && (positive(\\result) ||"
                + " (!\\old(x > 0)))) && ((\\result == 0) || (!\\old(x == 0)))");
  }
}
