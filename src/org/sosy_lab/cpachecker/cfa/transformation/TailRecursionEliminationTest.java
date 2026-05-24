// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@SuppressWarnings("unused")
public class TailRecursionEliminationTest {

  private ImmutableCFA tailRecursiveCFA;
  private ImmutableCFA nonTailRecursiveCFA;

  @Before
  public void init() throws IOException, ParserException, InterruptedException,
                            InvalidConfigurationException {
    ConfigurationBuilder testConfig = TestDataTools.configurationForTest();
    testConfig.setOption("cfa.useProgramTransformations", "true");
    testConfig.setOption("analysis.interprocedural", "false");
    Configuration tstConfig = testConfig.build();
    Path program_path = Path.of("test/programs/program_transformation/tail_recursion_add.c");
    tailRecursiveCFA =
        TestDataTools.makeCFA(
            testConfig.build(),
            IOUtils.toString(
                MoreFiles.asByteSource(program_path).openStream(), StandardCharsets.UTF_8));
    program_path = Path.of("test/programs/program_transformation/non_tail_recursion_add.c");
    nonTailRecursiveCFA =
        TestDataTools.makeCFA(
            testConfig.build(),
            IOUtils.toString(
                MoreFiles.asByteSource(program_path).openStream(), StandardCharsets.UTF_8));
  }

  @Test
  public void testSuccessfulTransformation(){
    Optional<SubCFA> successfulTransformation = new TailRecursionEliminationProgramTransformation().transform(tailRecursiveCFA, tailRecursiveCFA.getAllFunctions().get("add"));
    assertThat(successfulTransformation.isEmpty()).isFalse();
  }

  @Test
  public void testUnsuccessfulTransformation(){
    Optional<SubCFA> successfulTransformation = new TailRecursionEliminationProgramTransformation().transform(nonTailRecursiveCFA, nonTailRecursiveCFA.getAllFunctions().get("add"));
    assertThat(successfulTransformation.isEmpty()).isTrue();
  }
}
