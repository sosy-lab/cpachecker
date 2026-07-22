// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import com.google.common.base.Joiner;
import com.google.common.truth.Truth;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibAstParseException;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibParsingResult;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;

public class SvLibAstSerializationTest {
  private Path examplesPath() {
    return Path.of("test", "programs", "sv-lib").toAbsolutePath();
  }

  private void testAstSerialization(Path inputPath) throws SvLibAstParseException {

    String programString;
    try {
      programString = Joiner.on("\n").join(Files.readAllLines(inputPath));
    } catch (IOException e) {
      throw new SvLibAstParseException("Could not read input file: " + inputPath, e);
    }
    SvLibParsingResult parsed = SvLibToAstParser.parseScript(programString);

    String serializedProgramString = parsed.script().toASTString();

    SvLibParsingResult serializedParsed = SvLibToAstParser.parseScript(serializedProgramString);

    Truth.assertWithMessage("Scripts have different number of commands for input: %s", inputPath)
        .that(parsed.script().getCommands().size())
        .isEqualTo(serializedParsed.script().getCommands().size());

    for (int i = 0; i < parsed.script().getCommands().size(); i++) {
      SvLibCommand parsedCommand = parsed.script().getCommands().get(i);
      SvLibCommand roundtripParsedCommand = serializedParsed.script().getCommands().get(i);
      Truth.assertWithMessage("Command %s differs", i)
          .that(parsedCommand)
          .isEqualTo(roundtripParsedCommand);
    }
  }

  @Test
  public void serializeTest() throws SvLibAstParseException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(examplesPath(), "*.svlib")) {
      for (Path path : stream) {
        testAstSerialization(path);
      }
    } catch (IOException e) {
      throw new SvLibAstParseException("Could not read input files", e);
    }
  }
}
