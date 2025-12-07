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
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibAstParseException;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibParsingResult;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;

public class SvLibAstSerializationTest {
  private String examplesPath() {
    return Path.of("test", "programs", "sv-lib").toAbsolutePath().toString();
  }

  private void testAstSerialization(Path inputPath) throws SvLibAstParseException {

    String programString;
    try {
      programString = Joiner.on("\n").join(Files.readAllLines(inputPath));
    } catch (IOException e) {
      throw new SvLibAstParseException("Could not read input file: " + inputPath, e);
    }
    SvLibParsingResult parsed = SvLibToAstParser.parseScript(programString);

    String serializedProgramString =
        Joiner.on("\n")
            .join(parsed.script().getCommands().stream().map(SvLibCommand::toASTString).toList());

    SvLibParsingResult serializedParsed = SvLibToAstParser.parseScript(serializedProgramString);

    Truth.assertWithMessage("Scripts have different number of commands")
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
  public void parseVerySimpleDeclarationProgram() throws SvLibAstParseException {
    Path filepath = Path.of(examplesPath(), "very-simple-declaration.svlib");
    testAstSerialization(filepath);
  }

  @Test
  public void parseVerySimpleSequenceProgram() throws SvLibAstParseException {
    Path filepath = Path.of(examplesPath(), "very-simple-sequence.svlib");
    testAstSerialization(filepath);
  }

  @Test
  public void parseLoopSimpleAddProgram() throws SvLibAstParseException {
    Path filepath = Path.of(examplesPath(), "loop-simple-add-invalid.svlib");
    testAstSerialization(filepath);
  }
}
