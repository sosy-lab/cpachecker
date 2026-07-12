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
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class LoopAccelerationTest {

  private ImmutableCFA cfa;
  private CFANode loopHead;

  @Before
  public void init() throws InvalidConfigurationException, IOException, ParserException,
                            InterruptedException {
    ConfigurationBuilder testConfig = TestDataTools.configurationForTest();
    testConfig.setOption("cfa.useProgramTransformations", "LOOP_ACCELERATION, TAIL_RECURSION_ELIMINATION");
    Path program_path = Path.of("test/programs/program_transformation/loop_acceleration_simple.c");
    cfa =
        TestDataTools.makeCFA(
            testConfig.build(),
            IOUtils.toString(
                MoreFiles.asByteSource(program_path).openStream(), StandardCharsets.UTF_8));
    loopHead = cfa.getMetadata().getLoopStructure().orElseThrow().getAllLoopHeads().iterator().next();
  }

  @Test
  public void test() {
    Optional<ProgramTransformationInformation> successfulTransformation =
        new LoopAccelerationProgramTransformation()
            .transform(cfa, loopHead);
    assertThat(successfulTransformation.isEmpty()).isFalse();
  }
}
