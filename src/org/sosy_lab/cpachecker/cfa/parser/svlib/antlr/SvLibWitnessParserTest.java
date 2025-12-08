// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibInvariantTag;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibAstParseException;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibCorrectnessWitness;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibWitness;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetLogicCommand;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibAnyType;

public class SvLibWitnessParserTest {

  private String examplesPath() {
    return Path.of("test", "programs", "sv-lib-validation").toAbsolutePath().toString();
  }

  private void testWitnessParsing(Path inputPath, SvLibWitness expectedOutput)
      throws SvLibAstParseException {

    String programString;
    try {
      programString = Joiner.on("\n").join(Files.readAllLines(inputPath));
    } catch (IOException e) {
      throw new SvLibAstParseException("Could not read input file: " + inputPath, e);
    }
    SvLibWitness parsed = SvLibToAstParser.parseWitness(programString);

    if (expectedOutput instanceof SvLibCorrectnessWitness expectedCorrectnessWitness) {
      assertCorrectnessWitnessEquality(
          (SvLibCorrectnessWitness) parsed, expectedCorrectnessWitness);
    } else {
      throw new UnsupportedOperationException(
          "Unsupported witness type: " + expectedOutput.getClass());
    }
  }

  private void assertCorrectnessWitnessEquality(
      SvLibCorrectnessWitness actual, SvLibCorrectnessWitness expected) {
    // Check each field separately to make it easier to spot differences.
    Truth.assertThat(actual.getMetadataCommands().size())
        .isEqualTo(expected.getMetadataCommands().size());
    for (int i = 0; i < actual.getMetadataCommands().size(); i++) {
      Truth.assertThat(actual.getMetadataCommands().get(i))
          .isEqualTo(expected.getMetadataCommands().get(i));
    }

    Truth.assertThat(actual.getSmtLibCommands().size())
        .isEqualTo(expected.getSmtLibCommands().size());
    for (int i = 0; i < actual.getSmtLibCommands().size(); i++) {
      Truth.assertThat(actual.getSmtLibCommands().get(i))
          .isEqualTo(expected.getSmtLibCommands().get(i));
    }

    Truth.assertThat(actual.getAnnotateTagCommands().size())
        .isEqualTo(expected.getAnnotateTagCommands().size());
    for (int i = 0; i < actual.getAnnotateTagCommands().size(); i++) {
      Truth.assertThat(actual.getAnnotateTagCommands().get(i))
          .isEqualTo(expected.getAnnotateTagCommands().get(i));
    }

    // Finally check that the full objects are equal.
    Truth.assertThat(actual).isEqualTo(expected);
  }

  @SuppressWarnings("unused")
  @Test
  public void parseSimpleCorrectWitness() throws SvLibAstParseException {
    SvLibParsingVariableDeclaration a =
        new SvLibParsingVariableDeclaration(
            FileLocation.DUMMY, true, false, new SvLibAnyType(), "a", "a", null);

    SvLibWitness expectedWitness =
        new SvLibCorrectnessWitness(
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of(new SvLibSetLogicCommand(SmtLibLogic.LIA, FileLocation.DUMMY)),
            ImmutableList.of(
                new SvLibAnnotateTagCommand(
                    "while-loop",
                    ImmutableList.of(
                        new SvLibInvariantTag(
                            new SvLibSymbolApplicationTerm(
                                new SvLibIdTerm(
                                    SmtLibTheoryDeclarations.boolConjunction(2),
                                    FileLocation.DUMMY),
                                ImmutableList.of(
                                    new SvLibSymbolApplicationTerm(
                                        new SvLibIdTerm(
                                            SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN,
                                            FileLocation.DUMMY),
                                        ImmutableList.of(
                                            new SvLibIntegerConstantTerm(
                                                BigInteger.ZERO, FileLocation.DUMMY),
                                            new SvLibIdTerm(
                                                a.toSimpleDeclaration(), FileLocation.DUMMY)),
                                        FileLocation.DUMMY),
                                    new SvLibSymbolApplicationTerm(
                                        new SvLibIdTerm(
                                            SmtLibTheoryDeclarations.INT_GREATER_EQUAL_THAN,
                                            FileLocation.DUMMY),
                                        ImmutableList.of(
                                            new SvLibIdTerm(
                                                a.toSimpleDeclaration(), FileLocation.DUMMY),
                                            new SvLibIntegerConstantTerm(
                                                BigInteger.valueOf(6), FileLocation.DUMMY)),
                                        FileLocation.DUMMY)),
                                FileLocation.DUMMY),
                            FileLocation.DUMMY)),
                    FileLocation.DUMMY)));

    testWitnessParsing(
        Path.of(examplesPath(), "loop-simple-safe-validation-witness.svlib"), expectedWitness);
  }
}
