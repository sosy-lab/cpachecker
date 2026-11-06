// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;

/** The CFA of an SV-LIB script starts in the same function for a file and for a string. */
public class SvLibCfaCreatorTest {

  private static final Path PROGRAM = Path.of("test", "programs", "sv-lib", "loop-add.svlib");

  private static CFACreator createCfaCreator() throws Exception {
    Configuration config =
        Configuration.builder()
            .loadFromFile(Path.of("config", "predicateAnalysis-svlib.properties"))
            .build();
    return new CFACreator(
        config, LogManager.createTestLogManager(), ShutdownNotifier.createDummy());
  }

  private static String expectedMainFunction() {
    return SvLibFunctionDeclaration.mainFunctionDeclaration().getName();
  }

  @Test
  public void testMainFunctionOfFile() throws Exception {
    CFA cfa = createCfaCreator().parseFileAndCreateCFA(ImmutableList.of(PROGRAM.toString()));
    assertThat(cfa.getMainFunction().getFunctionName()).isEqualTo(expectedMainFunction());
  }

  @Test
  public void testMainFunctionOfString() throws Exception {
    CFA cfa =
        createCfaCreator()
            .parseSourceAndCreateCFA(Files.readString(PROGRAM, StandardCharsets.UTF_8));
    assertThat(cfa.getMainFunction().getFunctionName()).isEqualTo(expectedMainFunction());
  }
}
