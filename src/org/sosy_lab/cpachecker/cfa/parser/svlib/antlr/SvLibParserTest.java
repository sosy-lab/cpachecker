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
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibEnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibAstParseException;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibParsingResult;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibScript;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSortDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAssertCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareConstCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareFunCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareSortCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibGetWitnessCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetInfoCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetLogicCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetOptionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssumeStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibIfStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibReturnStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibSequenceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibWhileStatement;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibCustomType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;

public class SvLibParserTest {

  private String examplesPath() {
    return Path.of("test", "programs", "sv-lib").toAbsolutePath().toString();
  }

  private void testScriptParsing(Path inputPath, SvLibScript output) throws SvLibAstParseException {

    String programString;
    try {
      programString = Joiner.on("\n").join(Files.readAllLines(inputPath));
    } catch (IOException e) {
      throw new SvLibAstParseException("Could not read input file: " + inputPath, e);
    }
    SvLibParsingResult parsed = SvLibToAstParser.parseScript(programString);

    Truth.assertWithMessage("Scripts have different number of commands")
        .that(parsed.script().getCommands().size())
        .isEqualTo(output.getCommands().size());
    for (int i = 0; i < parsed.script().getCommands().size(); i++) {
      SvLibCommand parsedCommand = parsed.script().getCommands().get(i);
      SvLibCommand expectedCommand = output.getCommands().get(i);
      Truth.assertWithMessage("Command %s differs", i)
          .that(parsedCommand)
          .isEqualTo(expectedCommand);
    }
  }

  @Test
  public void parseSimpleCorrectProgram() throws SvLibAstParseException {
    SvLibParsingParameterDeclaration x =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY, SvLibSmtLibPredefinedType.INT, "x", "f1");
    SvLibParsingParameterDeclaration y =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY, SvLibSmtLibPredefinedType.INT, "y", "f1");
    SvLibProcedureDeclaration procedureDeclaration =
        new SvLibProcedureDeclaration(
            FileLocation.DUMMY,
            "f1",
            ImmutableList.of(x, y),
            ImmutableList.of(),
            ImmutableList.of());
    SvLibParsingVariableDeclaration w =
        new SvLibParsingVariableDeclaration(
            FileLocation.DUMMY, true, false, SvLibSmtLibPredefinedType.INT, "w", "w", "w");
    SvLibParsingVariableDeclaration z =
        new SvLibParsingVariableDeclaration(
            FileLocation.DUMMY, true, false, SvLibSmtLibPredefinedType.INT, "z", "z", "z");

    SvLibScript output =
        new SvLibScript(
            ImmutableList.of(
                new SvLibSetLogicCommand(SmtLibLogic.LIA, FileLocation.DUMMY),
                new SvLibSetInfoCommand(":format-version", "1.0", FileLocation.DUMMY),
                new SvLibVariableDeclarationCommand(w, FileLocation.DUMMY),
                new SvLibVariableDeclarationCommand(z, FileLocation.DUMMY),
                new SvLibProcedureDefinitionCommand(
                    FileLocation.DUMMY,
                    procedureDeclaration,
                    new SvLibSequenceStatement(
                        ImmutableList.of(
                            new SvLibAssumeStatement(
                                FileLocation.DUMMY,
                                new SvLibSymbolApplicationTerm(
                                    new SvLibIdTerm(
                                        SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new SvLibIdTerm(
                                            x.toSimpleDeclaration(), FileLocation.DUMMY),
                                        new SvLibIdTerm(
                                            y.toSimpleDeclaration(), FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new SvLibAssumeStatement(
                                FileLocation.DUMMY,
                                new SvLibSymbolApplicationTerm(
                                    new SvLibIdTerm(
                                        SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new SvLibIdTerm(
                                            x.toSimpleDeclaration(), FileLocation.DUMMY),
                                        new SvLibIdTerm(
                                            y.toSimpleDeclaration(), FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(
                                    new SvLibCheckTrueTag(
                                        new SvLibSymbolApplicationTerm(
                                            new SvLibIdTerm(
                                                SmtLibTheoryDeclarations.INT_EQUALITY,
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new SvLibIdTerm(
                                                    x.toSimpleDeclaration(), FileLocation.DUMMY),
                                                new SvLibIdTerm(
                                                    y.toSimpleDeclaration(), FileLocation.DUMMY)),
                                            FileLocation.DUMMY),
                                        FileLocation.DUMMY)),
                                ImmutableList.of())),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of(new SvLibTagReference("proc-f1", FileLocation.DUMMY)))),
                new SvLibVerifyCallCommand(
                    procedureDeclaration,
                    ImmutableList.of(
                        new SvLibIdTerm(w.toSimpleDeclaration(), FileLocation.DUMMY),
                        new SvLibIdTerm(z.toSimpleDeclaration(), FileLocation.DUMMY)),
                    FileLocation.DUMMY)),
            FileLocation.DUMMY);
    Path filePath = Path.of(examplesPath(), "simple-correct.svlib");

    testScriptParsing(filePath, output);
  }

  @Test
  public void parseSimpleIncorrectProgram() throws SvLibAstParseException {
    SvLibParsingParameterDeclaration x =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY, SvLibSmtLibPredefinedType.INT, "x", "f1");
    SvLibParsingParameterDeclaration y =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY, SvLibSmtLibPredefinedType.INT, "y", "f1");
    SvLibProcedureDeclaration procedureDeclaration =
        new SvLibProcedureDeclaration(
            FileLocation.DUMMY,
            "f1",
            ImmutableList.of(x, y),
            ImmutableList.of(),
            ImmutableList.of());
    SvLibParsingVariableDeclaration w =
        new SvLibParsingVariableDeclaration(
            FileLocation.DUMMY, true, false, SvLibSmtLibPredefinedType.INT, "w", "w", "w");
    SvLibParsingVariableDeclaration z =
        new SvLibParsingVariableDeclaration(
            FileLocation.DUMMY, true, false, SvLibSmtLibPredefinedType.INT, "z", "z", "z");

    SvLibScript output =
        new SvLibScript(
            ImmutableList.of(
                new SvLibSetLogicCommand(SmtLibLogic.LIA, FileLocation.DUMMY),
                new SvLibSetInfoCommand(":format-version", "1.0", FileLocation.DUMMY),
                new SvLibVariableDeclarationCommand(w, FileLocation.DUMMY),
                new SvLibVariableDeclarationCommand(z, FileLocation.DUMMY),
                new SvLibProcedureDefinitionCommand(
                    FileLocation.DUMMY,
                    procedureDeclaration,
                    new SvLibSequenceStatement(
                        ImmutableList.of(
                            new SvLibAssumeStatement(
                                FileLocation.DUMMY,
                                new SvLibSymbolApplicationTerm(
                                    new SvLibIdTerm(
                                        SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new SvLibIdTerm(
                                            x.toSimpleDeclaration(), FileLocation.DUMMY),
                                        new SvLibIdTerm(
                                            y.toSimpleDeclaration(), FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new SvLibAssumeStatement(
                                FileLocation.DUMMY,
                                new SvLibSymbolApplicationTerm(
                                    new SvLibIdTerm(
                                        SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new SvLibIdTerm(
                                            x.toSimpleDeclaration(), FileLocation.DUMMY),
                                        new SvLibIdTerm(
                                            y.toSimpleDeclaration(), FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(
                                    new SvLibCheckTrueTag(
                                        new SvLibSymbolApplicationTerm(
                                            new SvLibIdTerm(
                                                SmtLibTheoryDeclarations.BOOL_NEGATION,
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new SvLibSymbolApplicationTerm(
                                                    new SvLibIdTerm(
                                                        SmtLibTheoryDeclarations.INT_EQUALITY,
                                                        FileLocation.DUMMY),
                                                    ImmutableList.of(
                                                        new SvLibIdTerm(
                                                            x.toSimpleDeclaration(),
                                                            FileLocation.DUMMY),
                                                        new SvLibIdTerm(
                                                            y.toSimpleDeclaration(),
                                                            FileLocation.DUMMY)),
                                                    FileLocation.DUMMY)),
                                            FileLocation.DUMMY),
                                        FileLocation.DUMMY)),
                                ImmutableList.of())),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of(new SvLibTagReference("proc-f1", FileLocation.DUMMY)))),
                new SvLibVerifyCallCommand(
                    procedureDeclaration,
                    ImmutableList.of(
                        new SvLibIdTerm(w.toSimpleDeclaration(), FileLocation.DUMMY),
                        new SvLibIdTerm(z.toSimpleDeclaration(), FileLocation.DUMMY)),
                    FileLocation.DUMMY)),
            FileLocation.DUMMY);
    Path filePath = Path.of(examplesPath(), "simple-incorrect.svlib");

    testScriptParsing(filePath, output);
  }

  @Test
  public void parseLoopAdd() throws SvLibAstParseException {

    SvLibParsingParameterDeclaration x0 =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY, SvLibSmtLibPredefinedType.INT, "x0", "add");
    SvLibParsingParameterDeclaration y0 =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY, SvLibSmtLibPredefinedType.INT, "y0", "add");
    SvLibParsingVariableDeclaration w0Const =
        new SvLibParsingVariableDeclaration(
            FileLocation.DUMMY, true, true, SvLibSmtLibPredefinedType.INT, "w0", "w0", "w0");
    SvLibParsingVariableDeclaration z0Const =
        new SvLibParsingVariableDeclaration(
            FileLocation.DUMMY, true, true, SvLibSmtLibPredefinedType.INT, "z0", "z0", "z0");
    SvLibParsingParameterDeclaration x =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY, SvLibSmtLibPredefinedType.INT, "x", "add");
    SvLibParsingParameterDeclaration y =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY, SvLibSmtLibPredefinedType.INT, "y", "add");
    SvLibProcedureDeclaration procedureDeclaration =
        new SvLibProcedureDeclaration(
            FileLocation.DUMMY,
            "add",
            ImmutableList.of(x0, y0),
            ImmutableList.of(x),
            ImmutableList.of(y));

    SvLibScript output =
        new SvLibScript(
            ImmutableList.of(
                new SvLibSetLogicCommand(SmtLibLogic.LIA, FileLocation.DUMMY),
                new SvLibSetInfoCommand(":format-version", "1.0", FileLocation.DUMMY),
                new SvLibProcedureDefinitionCommand(
                    FileLocation.DUMMY,
                    procedureDeclaration,
                    new SvLibSequenceStatement(
                        ImmutableList.of(
                            new SvLibAssignmentStatement(
                                ImmutableMap.of(
                                    x,
                                    new SvLibIdTerm(x0.toSimpleDeclaration(), FileLocation.DUMMY),
                                    y,
                                    new SvLibIdTerm(y0.toSimpleDeclaration(), FileLocation.DUMMY)),
                                FileLocation.DUMMY,
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new SvLibWhileStatement(
                                new SvLibSymbolApplicationTerm(
                                    new SvLibIdTerm(
                                        SmtLibTheoryDeclarations.INT_LESS_THAN, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new SvLibIntegerConstantTerm(
                                            BigInteger.ZERO, FileLocation.DUMMY),
                                        new SvLibIdTerm(
                                            y.toSimpleDeclaration(), FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                new SvLibAssignmentStatement(
                                    ImmutableMap.of(
                                        x,
                                        new SvLibSymbolApplicationTerm(
                                            new SvLibIdTerm(
                                                SmtLibTheoryDeclarations.intAddition(2),
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new SvLibIdTerm(
                                                    x.toSimpleDeclaration(), FileLocation.DUMMY),
                                                new SvLibIntegerConstantTerm(
                                                    BigInteger.ONE, FileLocation.DUMMY)),
                                            FileLocation.DUMMY),
                                        y,
                                        new SvLibSymbolApplicationTerm(
                                            new SvLibIdTerm(
                                                SmtLibTheoryDeclarations.intSubtraction(2),
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new SvLibIdTerm(
                                                    y.toSimpleDeclaration(), FileLocation.DUMMY),
                                                new SvLibIntegerConstantTerm(
                                                    BigInteger.ONE, FileLocation.DUMMY)),
                                            FileLocation.DUMMY)),
                                    FileLocation.DUMMY,
                                    ImmutableList.of(),
                                    ImmutableList.of()),
                                ImmutableList.of(),
                                ImmutableList.of(
                                    // Using null for the scope is not the best solution but works
                                    // for tests, since equality over tags must be based on the
                                    // nameonly.
                                    new SvLibTagReference("while-1", FileLocation.DUMMY)),
                                FileLocation.DUMMY)),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of(
                            // Using null for the scope is not the best solution but works for
                            // tests, since equality over tags must be based on the name only.
                            new SvLibTagReference("proc-add", FileLocation.DUMMY)))),
                new SvLibAnnotateTagCommand(
                    "proc-add",
                    ImmutableList.of(
                        new SvLibRequiresTag(
                            new SvLibSymbolApplicationTerm(
                                new SvLibIdTerm(
                                    SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN,
                                    FileLocation.DUMMY),
                                ImmutableList.of(
                                    new SvLibIntegerConstantTerm(
                                        BigInteger.ZERO, FileLocation.DUMMY),
                                    new SvLibIdTerm(
                                        SvLibParsingVariableDeclaration.dummyVariableForName("y0")
                                            .toSimpleDeclaration(),
                                        FileLocation.DUMMY)),
                                FileLocation.DUMMY),
                            FileLocation.DUMMY),
                        new SvLibEnsuresTag(
                            new SvLibSymbolApplicationTerm(
                                new SvLibIdTerm(
                                    SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                ImmutableList.of(
                                    new SvLibIdTerm(
                                        SvLibParsingVariableDeclaration.dummyVariableForName("x")
                                            .toSimpleDeclaration(),
                                        FileLocation.DUMMY),
                                    new SvLibSymbolApplicationTerm(
                                        new SvLibIdTerm(
                                            SmtLibTheoryDeclarations.intAddition(2),
                                            FileLocation.DUMMY),
                                        ImmutableList.of(
                                            new SvLibIdTerm(
                                                SvLibParsingVariableDeclaration
                                                    .dummyVariableForName("x0")
                                                    .toSimpleDeclaration(),
                                                FileLocation.DUMMY),
                                            new SvLibIdTerm(
                                                SvLibParsingVariableDeclaration
                                                    .dummyVariableForName("y0")
                                                    .toSimpleDeclaration(),
                                                FileLocation.DUMMY)),
                                        FileLocation.DUMMY)),
                                FileLocation.DUMMY),
                            FileLocation.DUMMY)),
                    FileLocation.DUMMY),
                new SvLibDeclareConstCommand(w0Const, FileLocation.DUMMY),
                new SvLibDeclareConstCommand(z0Const, FileLocation.DUMMY),
                new SvLibAssertCommand(
                    new SvLibSymbolApplicationTerm(
                        new SvLibIdTerm(
                            SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN, FileLocation.DUMMY),
                        ImmutableList.of(
                            new SvLibIntegerConstantTerm(BigInteger.ZERO, FileLocation.DUMMY),
                            new SvLibIdTerm(w0Const.toSimpleDeclaration(), FileLocation.DUMMY)),
                        FileLocation.DUMMY),
                    FileLocation.DUMMY),
                new SvLibVerifyCallCommand(
                    procedureDeclaration,
                    ImmutableList.of(
                        new SvLibIdTerm(w0Const.toSimpleDeclaration(), FileLocation.DUMMY),
                        new SvLibIdTerm(z0Const.toSimpleDeclaration(), FileLocation.DUMMY)),
                    FileLocation.DUMMY),
                new SvLibGetWitnessCommand(FileLocation.DUMMY)),
            FileLocation.DUMMY);

    Path filePath = Path.of(examplesPath(), "loop-add.svlib");

    testScriptParsing(filePath, output);
  }

  @Test
  public void parseSimpleLoop() throws SvLibAstParseException {

    SvLibParsingParameterDeclaration resultVar =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY, SvLibSmtLibPredefinedType.INT, "|c#result|", "main");
    SvLibParsingParameterDeclaration a =
        new SvLibParsingParameterDeclaration(
            FileLocation.DUMMY, SvLibSmtLibPredefinedType.INT, "a", "main");

    SvLibProcedureDeclaration mainProcedureDeclaration =
        new SvLibProcedureDeclaration(
            FileLocation.DUMMY,
            "main",
            ImmutableList.of(),
            ImmutableList.of(resultVar),
            ImmutableList.of(a));

    SvLibScript expectedOutput =
        new SvLibScript(
            ImmutableList.of(
                new SvLibSetLogicCommand(SmtLibLogic.LIA, FileLocation.DUMMY),
                new SvLibSetOptionCommand(
                    ":witness-output-channel", "./output/witness.svlib", FileLocation.DUMMY),
                new SvLibSetOptionCommand(
                    ":produce-correctness-witnesses", "true", FileLocation.DUMMY),
                new SvLibSetOptionCommand(
                    ":produce-violation-witnesses", "true", FileLocation.DUMMY),
                new SvLibSetInfoCommand(":format-version", "1.0", FileLocation.DUMMY),
                new SvLibDeclareSortCommand(
                    new SvLibSortDeclaration(
                        FileLocation.DUMMY,
                        new SvLibCustomType("|c#ptr|", 1),
                        "|c#ptr|",
                        "|c#ptr|"),
                    FileLocation.DUMMY),
                new SvLibDeclareSortCommand(
                    new SvLibSortDeclaration(
                        FileLocation.DUMMY,
                        new SvLibCustomType("|c#heap|", 0),
                        "|c#heap|",
                        "|c#heap|"),
                    FileLocation.DUMMY),
                new SvLibVariableDeclarationCommand(
                    new SvLibParsingVariableDeclaration(
                        FileLocation.DUMMY,
                        true,
                        false,
                        new SvLibCustomType("|c#heap|", 0),
                        "|c#heap|",
                        "|c#heap|",
                        "|c#heap|"),
                    FileLocation.DUMMY),
                new SvLibDeclareFunCommand(
                    new SvLibSmtFunctionDeclaration(
                        FileLocation.DUMMY,
                        "|c#bitxor|",
                        ImmutableList.of(
                            SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
                        SvLibSmtLibPredefinedType.INT),
                    FileLocation.DUMMY),
                new SvLibDeclareFunCommand(
                    new SvLibSmtFunctionDeclaration(
                        FileLocation.DUMMY,
                        "|c#bitand|",
                        ImmutableList.of(
                            SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
                        SvLibSmtLibPredefinedType.INT),
                    FileLocation.DUMMY),
                new SvLibDeclareFunCommand(
                    new SvLibSmtFunctionDeclaration(
                        FileLocation.DUMMY,
                        "|c#bitor|",
                        ImmutableList.of(
                            SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
                        SvLibSmtLibPredefinedType.INT),
                    FileLocation.DUMMY),
                new SvLibProcedureDefinitionCommand(
                    FileLocation.DUMMY,
                    mainProcedureDeclaration,
                    new SvLibSequenceStatement(
                        ImmutableList.of(
                            new SvLibAssignmentStatement(
                                ImmutableMap.of(
                                    a,
                                    new SvLibIntegerConstantTerm(
                                        BigInteger.valueOf(6), FileLocation.DUMMY)),
                                FileLocation.DUMMY,
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new SvLibAssignmentStatement(
                                ImmutableMap.of(
                                    a,
                                    new SvLibIntegerConstantTerm(
                                        BigInteger.ZERO, FileLocation.DUMMY)),
                                FileLocation.DUMMY,
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new SvLibWhileStatement(
                                new SvLibSymbolApplicationTerm(
                                    new SvLibIdTerm(
                                        SmtLibTheoryDeclarations.INT_LESS_THAN, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new SvLibIdTerm(
                                            a.toSimpleDeclaration(), FileLocation.DUMMY),
                                        new SvLibIntegerConstantTerm(
                                            BigInteger.valueOf(6), FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                new SvLibAssignmentStatement(
                                    ImmutableMap.of(
                                        a,
                                        new SvLibSymbolApplicationTerm(
                                            new SvLibIdTerm(
                                                SmtLibTheoryDeclarations.intAddition(2),
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new SvLibIdTerm(
                                                    a.toSimpleDeclaration(), FileLocation.DUMMY),
                                                new SvLibIntegerConstantTerm(
                                                    BigInteger.ONE, FileLocation.DUMMY)),
                                            FileLocation.DUMMY)),
                                    FileLocation.DUMMY,
                                    ImmutableList.of(),
                                    ImmutableList.of()),
                                ImmutableList.of(),
                                ImmutableList.of(
                                    new SvLibTagReference("while-loop", FileLocation.DUMMY)),
                                FileLocation.DUMMY),
                            new SvLibIfStatement(
                                FileLocation.DUMMY,
                                ImmutableList.of(),
                                ImmutableList.of(),
                                new SvLibSymbolApplicationTerm(
                                    new SvLibIdTerm(
                                        SmtLibTheoryDeclarations.BOOL_NEGATION, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new SvLibSymbolApplicationTerm(
                                            new SvLibIdTerm(
                                                SmtLibTheoryDeclarations.INT_EQUALITY,
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new SvLibIdTerm(
                                                    a.toSimpleDeclaration(), FileLocation.DUMMY),
                                                new SvLibIntegerConstantTerm(
                                                    BigInteger.valueOf(6), FileLocation.DUMMY)),
                                            FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                new SvLibSequenceStatement(
                                    ImmutableList.of(),
                                    FileLocation.DUMMY,
                                    ImmutableList.of(
                                        new SvLibCheckTrueTag(
                                            new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
                                            FileLocation.DUMMY)),
                                    ImmutableList.of())),
                            new SvLibAssignmentStatement(
                                ImmutableMap.of(
                                    resultVar,
                                    new SvLibIntegerConstantTerm(
                                        BigInteger.ONE, FileLocation.DUMMY)),
                                FileLocation.DUMMY,
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new SvLibReturnStatement(
                                FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of())),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of(new SvLibTagReference("proc-main", FileLocation.DUMMY)))),
                new SvLibVerifyCallCommand(
                    mainProcedureDeclaration, ImmutableList.of(), FileLocation.DUMMY),
                new SvLibGetWitnessCommand(FileLocation.DUMMY)),
            FileLocation.DUMMY);

    Path filePath = Path.of(examplesPath(), "loop-simple-safe.svlib");

    testScriptParsing(filePath, expectedOutput);
  }
}
