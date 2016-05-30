/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Test that the bundled specification files are all valid.
 */
@RunWith(Parameterized.class)
public class AutomatonFilesTest {

  @Parameters(name = "{0}")
  public static Object[] getAutomata() throws IOException {
    try (Stream<Path> configFiles = Files.walk(Paths.get("config/specification"))) {
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
            Language.C);
    assertThat(automata).named("automata from file " + automatonFile).isNotEmpty();
  }
}
