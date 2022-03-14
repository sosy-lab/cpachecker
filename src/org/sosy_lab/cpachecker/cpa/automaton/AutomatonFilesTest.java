// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.truth.Truth.assertWithMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

/** Test that the bundled specification files are all valid. */
@RunWith(Parameterized.class)
public class AutomatonFilesTest {

  @Parameters(name = "{0}")
  public static Object[] getAutomata() throws IOException {
    try (Stream<Path> configFiles = Files.walk(Path.of("config/specification"))) {
      return configFiles
          .filter(path -> path.getFileName().toString().endsWith(".spc"))
          .sorted()
          .toArray();
    }
  }

  @Parameter(0)
  public Path automatonFile;

  @Test
  public void parse() throws InvalidConfigurationException {
    List<Automaton> automata =
        AutomatonParser.parseAutomatonFile(
            automatonFile,
            TestDataTools.configurationForTest().build(),
            LogManager.createTestLogManager(),
            MachineModel.LINUX32,
            CProgramScope.empty(),
            Language.C,
            ShutdownNotifier.createDummy());
    assertWithMessage("automata from file %s,", automatonFile).that(automata).isNotEmpty();
  }
}
