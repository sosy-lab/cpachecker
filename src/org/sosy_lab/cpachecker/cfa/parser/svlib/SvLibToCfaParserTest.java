// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.SvLibParserException;

public class SvLibToCfaParserTest {
  private Path examplesPath() {
    return Path.of("test", "programs", "sv-lib").toAbsolutePath();
  }

  private void testCfaBuilding(List<String> inputPaths)
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException {

    SvLibToCfaParser parser =
        new SvLibToCfaParser(
            LogManager.createTestLogManager(),
            Configuration.defaultConfiguration(),
            MachineModel.LINUX32,
            ShutdownNotifier.createDummy());
    parser.parseFiles(inputPaths);
  }

  @Test
  public void serializeTest()
      throws SvLibParserException, InterruptedException, InvalidConfigurationException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(examplesPath(), "*.svlib")) {
      for (Path path : stream) {
        testCfaBuilding(ImmutableList.of(path.toAbsolutePath().toString()));
      }
    } catch (IOException | ParserException e) {
      throw new SvLibParserException("Could not read input files", e);
    }
  }
}
