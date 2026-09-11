// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class ExecutionWitnessIntegrationTest {

  @Rule public TemporaryFolder temporary = new TemporaryFolder();

  private static final String PROGRAM =
      """
      int witness_step(int amount) {
        return amount + 1;
      }
      int main(void) {
        int witness_value = 0;
        while (witness_value < 3) {
          witness_value = witness_step(witness_value);
        }
        witness_step(witness_value);
        return 0;
      }
      """;

  @BeforeClass
  public static void requireExtendedTests() {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
  }

  private ConfigurationBuilder config(String pConfig) throws Exception {
    return TestUtils.configurationForTestWithOutput(temporary)
        .loadFromFile("config/" + pConfig + ".properties")
        .setOption("specification", "test/programs/benchmarks/properties/unreach-call.prp")
        .setOption("analysis.entryFunction", "main")
        .setOption("cpa.arg.yamlProofWitness", "proof-%s.yml")
        .setOption("counterexample.export.yaml", "violation.yml");
  }

  private IntegrationTestResult run(ConfigurationBuilder pConfig, String pSource) throws Exception {
    Path program = temporary.newFile("program.c").toPath();
    Files.writeString(program, pSource);
    IntegrationTestResult result = IntegrationTestRunner.run(pConfig.build(), program.toString());
    result.cpaCheckerResult().writeOutputFiles();
    return result;
  }

  private Path output(String pName) {
    return temporary.getRoot().toPath().resolve("output").resolve(pName);
  }

  private JsonNode witness(String pName) throws Exception {
    return new ObjectMapper(new YAMLFactory()).readTree(output(pName).toFile()).get(0);
  }

  private void assertOnlyWitnesses() throws Exception {
    try (var files = Files.list(output(""))) {
      List<String> names = files.map(path -> path.getFileName().toString()).toList();
      assertThat(
              names.stream()
                  .filter(name -> name.startsWith("Counterexample."))
                  .filter(name -> name.endsWith(".c") || name.endsWith(".txt"))
                  .toList())
          .isEmpty();
      assertThat(names).doesNotContain("ARG.dot");
      assertThat(
              names.stream()
                  .filter(name -> name.contains("graphml") || name.contains("witness.dot"))
                  .toList())
          .isEmpty();
    }
  }

  @Test
  public void violationOnlyMarksTheTarget() throws Exception {
    IntegrationTestResult result =
        run(
            config("execution"),
            """
            extern void reach_error(void);
            int main(void) {
              int witness_steps = 0;
              while (witness_steps < 1000) { witness_steps++; }
              reach_error();
            }
            """);
    result.assertIsUnsafe();
    assertThat(result.cpaCheckerResult().getReached().size()).isAtMost(2);
    JsonNode content = witness("violation.yml").get("content");
    assertThat(content.size()).isEqualTo(1);
    JsonNode segment = content.get(0).get("segment");
    assertThat(segment.size()).isEqualTo(1);
    JsonNode waypoint = segment.get(0).get("waypoint");
    assertThat(waypoint.get("type").asText()).isEqualTo("target");
    assertThat(waypoint.get("location").get("line").asInt()).isEqualTo(5);
    assertOnlyWitnesses();
  }

  @Test
  public void loopAndFunctionSnapshotsReachBothWitnessVersions() throws Exception {
    IntegrationTestResult result =
        run(
            config("execution").setOption("witness.yamlexporter.witnessVersions", "V2,V2d1"),
            PROGRAM);
    result.assertIsSafe();
    assertThat(result.cpaCheckerResult().getReached().size()).isAtMost(2);
    for (String version : ImmutableList.of("2.0", "2.1")) {
      JsonNode content = witness("proof-" + version + ".yml").get("content");
      boolean foundLoop = false;
      boolean foundCall = false;
      boolean foundEntry = false;
      for (JsonNode entry : content) {
        JsonNode invariant = entry.get("invariant");
        if (invariant == null) {
          continue;
        }
        String type = invariant.get("type").asText();
        if (type.equals("loop_invariant")) {
          foundLoop = true;
          assertThat(invariant.get("location").get("line").asInt()).isEqualTo(6);
          String value = invariant.get("value").asText();
          for (int i = 0; i <= 3; i++) {
            assertThat(value).contains("witness_value == (" + i + ")");
          }
        }
        if (type.equals("location_invariant")
            && invariant.get("location").get("line").asInt() == 9) {
          foundCall = true;
          assertThat(invariant.get("value").asText()).contains("witness_value == (3)");
        }
        if (invariant.get("location").get("function").asText().equals("witness_step")) {
          foundEntry = true;
          String value =
              type.equals("function_contract")
                  ? invariant.get("requires").asText()
                  : invariant.get("value").asText();
          assertThat(value).contains("amount == (1)");
          assertThat(value).contains("amount == (2)");
        }
      }
      assertThat(foundLoop).isTrue();
      assertThat(foundCall).isTrue();
      assertThat(foundEntry).isTrue();
    }
    assertOnlyWitnesses();
  }

  @Test
  public void exceedingTheLimitDropsTheWholeInvariant() throws Exception {
    run(config("execution").setOption("cpa.execution.maxAssignmentsPerLocation", "2"), PROGRAM)
        .assertIsSafe();
    String yaml = Files.readString(output("proof-2.0.yml"));
    assertThat(yaml).doesNotContain("loop_invariant");
    // The single observation of the call after the loop is still available.
    assertThat(yaml).contains("witness_value == (3)");
  }

  @Test
  public void portfolioExportsTheWinningExecutionWitness() throws Exception {
    IntegrationTestResult result =
        IntegrationTestRunner.run(
            config("simpleChecks").build(), "test/programs/execution/recursive-factorial-false.c");
    result.assertIsUnsafe();
    result.cpaCheckerResult().writeOutputFiles();
    assertThat(witness("violation.yml").get("content").size()).isEqualTo(1);
    assertOnlyWitnesses();
  }

  @Test
  public void nonterminationExportsOnlyTheReportedLocation() throws Exception {
    IntegrationTestResult result =
        IntegrationTestRunner.run(
            config("trivialRules--termination")
                .setOption("specification", "test/programs/benchmarks/properties/termination.prp")
                .build(),
            "test/programs/trivialrules/endless-loop-false.c");
    result.assertIsUnsafe();
    result.cpaCheckerResult().writeOutputFiles();
    JsonNode marker = witness("violation.yml");
    assertThat(marker.get("content").size()).isEqualTo(1);
    assertThat(marker.findValuesAsText("type")).containsExactly("target");
    assertOnlyWitnesses();
  }

  @Test
  public void unknownDoesNotExportAWitness() throws Exception {
    IntegrationTestResult result =
        IntegrationTestRunner.run(
            config("execution").build(), "test/programs/execution/nondeterministic-input.c");
    result.assertIs(Result.UNKNOWN);
    result.cpaCheckerResult().writeOutputFiles();
    assertThat(Files.exists(output("proof-2.0.yml"))).isFalse();
    assertThat(Files.exists(output("violation.yml"))).isFalse();
  }
}
