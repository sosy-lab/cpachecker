// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.parser;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAssertCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAssumeStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCustomType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclareConstCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclareFunCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclareSortCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibEnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibGetWitnessCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIfStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibScript;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSequenceStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSetInfoCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSetLogicCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSetOptionCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSmtLibType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSortDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibWhileStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.builder.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.SvLibToAstParser.SvLibAstParseException;

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
    SvLibScript parsed = SvLibToAstParser.parseScript(programString);
    assert parsed.equals(output) : "Parsed object does not match expected object";
  }

  @Test
  public void parseSimpleCorrectProgram() throws SvLibAstParseException {
    SvLibParameterDeclaration x =
        new SvLibParameterDeclaration(FileLocation.DUMMY, SvLibSmtLibType.INT, "x", "f1");
    SvLibParameterDeclaration y =
        new SvLibParameterDeclaration(FileLocation.DUMMY, SvLibSmtLibType.INT, "y", "f1");
    SvLibProcedureDeclaration procedureDeclaration =
        new SvLibProcedureDeclaration(
            FileLocation.DUMMY,
            "f1",
            ImmutableList.of(x, y),
            ImmutableList.of(),
            ImmutableList.of());
    SvLibVariableDeclaration w =
        new SvLibVariableDeclaration(
            FileLocation.DUMMY, true, false, SvLibSmtLibType.INT, "w", "w", "w");
    SvLibVariableDeclaration z =
        new SvLibVariableDeclaration(
            FileLocation.DUMMY, true, false, SvLibSmtLibType.INT, "z", "z", "z");

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
                                        new SvLibIdTerm(x, FileLocation.DUMMY),
                                        new SvLibIdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new SvLibAssumeStatement(
                                FileLocation.DUMMY,
                                new SvLibSymbolApplicationTerm(
                                    new SvLibIdTerm(
                                        SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new SvLibIdTerm(x, FileLocation.DUMMY),
                                        new SvLibIdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(
                                    new SvLibCheckTrueTag(
                                        new SvLibSymbolApplicationTerm(
                                            new SvLibIdTerm(
                                                SmtLibTheoryDeclarations.INT_EQUALITY,
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new SvLibIdTerm(x, FileLocation.DUMMY),
                                                new SvLibIdTerm(y, FileLocation.DUMMY)),
                                            FileLocation.DUMMY),
                                        FileLocation.DUMMY)),
                                ImmutableList.of())),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of())),
                new SvLibVerifyCallCommand(
                    procedureDeclaration,
                    ImmutableList.of(
                        new SvLibIdTerm(w, FileLocation.DUMMY),
                        new SvLibIdTerm(z, FileLocation.DUMMY)),
                    FileLocation.DUMMY)));
    Path filePath = Path.of(examplesPath(), "simple-correct.svlib");

    testScriptParsing(filePath, output);
  }

  @Test
  public void parseSimpleIncorrectProgram() throws SvLibAstParseException {
    SvLibParameterDeclaration x =
        new SvLibParameterDeclaration(FileLocation.DUMMY, SvLibSmtLibType.INT, "x", "f1");
    SvLibParameterDeclaration y =
        new SvLibParameterDeclaration(FileLocation.DUMMY, SvLibSmtLibType.INT, "y", "f1");
    SvLibProcedureDeclaration procedureDeclaration =
        new SvLibProcedureDeclaration(
            FileLocation.DUMMY,
            "f1",
            ImmutableList.of(x, y),
            ImmutableList.of(),
            ImmutableList.of());
    SvLibVariableDeclaration w =
        new SvLibVariableDeclaration(
            FileLocation.DUMMY, true, false, SvLibSmtLibType.INT, "w", "w", "w");
    SvLibVariableDeclaration z =
        new SvLibVariableDeclaration(
            FileLocation.DUMMY, true, false, SvLibSmtLibType.INT, "z", "z", "z");

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
                                        new SvLibIdTerm(x, FileLocation.DUMMY),
                                        new SvLibIdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new SvLibAssumeStatement(
                                FileLocation.DUMMY,
                                new SvLibSymbolApplicationTerm(
                                    new SvLibIdTerm(
                                        SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new SvLibIdTerm(x, FileLocation.DUMMY),
                                        new SvLibIdTerm(y, FileLocation.DUMMY)),
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
                                                        new SvLibIdTerm(x, FileLocation.DUMMY),
                                                        new SvLibIdTerm(y, FileLocation.DUMMY)),
                                                    FileLocation.DUMMY)),
                                            FileLocation.DUMMY),
                                        FileLocation.DUMMY)),
                                ImmutableList.of())),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of())),
                new SvLibVerifyCallCommand(
                    procedureDeclaration,
                    ImmutableList.of(
                        new SvLibIdTerm(w, FileLocation.DUMMY),
                        new SvLibIdTerm(z, FileLocation.DUMMY)),
                    FileLocation.DUMMY)));
    Path filePath = Path.of(examplesPath(), "simple-incorrect.svlib");

    testScriptParsing(filePath, output);
  }

  @Test
  public void parseLoopAdd() throws SvLibAstParseException {

    SvLibParameterDeclaration x0 =
        new SvLibParameterDeclaration(FileLocation.DUMMY, SvLibSmtLibType.INT, "x0", "add");
    SvLibParameterDeclaration y0 =
        new SvLibParameterDeclaration(FileLocation.DUMMY, SvLibSmtLibType.INT, "y0", "add");
    SvLibVariableDeclaration w0Const =
        new SvLibVariableDeclaration(
            FileLocation.DUMMY, true, true, SvLibSmtLibType.INT, "w0", "w0", "w0");
    SvLibVariableDeclaration z0Const =
        new SvLibVariableDeclaration(
            FileLocation.DUMMY, true, true, SvLibSmtLibType.INT, "z0", "z0", "z0");
    SvLibParameterDeclaration x =
        new SvLibParameterDeclaration(FileLocation.DUMMY, SvLibSmtLibType.INT, "x", "add");
    SvLibParameterDeclaration y =
        new SvLibParameterDeclaration(FileLocation.DUMMY, SvLibSmtLibType.INT, "y", "add");
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
                                    new SvLibIdTerm(x0, FileLocation.DUMMY),
                                    y,
                                    new SvLibIdTerm(y0, FileLocation.DUMMY)),
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
                                        new SvLibIdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                new SvLibAssignmentStatement(
                                    ImmutableMap.of(
                                        x,
                                        new SvLibSymbolApplicationTerm(
                                            new SvLibIdTerm(
                                                SmtLibTheoryDeclarations.intAddition(2),
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new SvLibIdTerm(x, FileLocation.DUMMY),
                                                new SvLibIntegerConstantTerm(
                                                    BigInteger.ONE, FileLocation.DUMMY)),
                                            FileLocation.DUMMY),
                                        y,
                                        new SvLibSymbolApplicationTerm(
                                            new SvLibIdTerm(
                                                SmtLibTheoryDeclarations.INT_MINUS,
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new SvLibIdTerm(y, FileLocation.DUMMY),
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
                                    new SvLibTagReference("while-1", FileLocation.DUMMY, null)),
                                FileLocation.DUMMY)),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of(
                            // Using null for the scope is not the best solution but works for
                            // tests, since equality over tags must be based on the name only.
                            new SvLibTagReference("proc-add", FileLocation.DUMMY, null)))),
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
                                        SvLibVariableDeclaration.dummyVariableForName("y0"),
                                        FileLocation.DUMMY)),
                                FileLocation.DUMMY),
                            FileLocation.DUMMY),
                        new SvLibEnsuresTag(
                            new SvLibSymbolApplicationTerm(
                                new SvLibIdTerm(
                                    SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                ImmutableList.of(
                                    new SvLibIdTerm(
                                        SvLibVariableDeclaration.dummyVariableForName("x"),
                                        FileLocation.DUMMY),
                                    new SvLibSymbolApplicationTerm(
                                        new SvLibIdTerm(
                                            SmtLibTheoryDeclarations.intAddition(2),
                                            FileLocation.DUMMY),
                                        ImmutableList.of(
                                            new SvLibIdTerm(
                                                SvLibVariableDeclaration.dummyVariableForName("x0"),
                                                FileLocation.DUMMY),
                                            new SvLibIdTerm(
                                                SvLibVariableDeclaration.dummyVariableForName("y0"),
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
                            new SvLibIdTerm(w0Const, FileLocation.DUMMY)),
                        FileLocation.DUMMY),
                    FileLocation.DUMMY),
                new SvLibVerifyCallCommand(
                    procedureDeclaration,
                    ImmutableList.of(
                        new SvLibIdTerm(w0Const, FileLocation.DUMMY),
                        new SvLibIdTerm(z0Const, FileLocation.DUMMY)),
                    FileLocation.DUMMY),
                new SvLibGetWitnessCommand(FileLocation.DUMMY)));

    Path filePath = Path.of(examplesPath(), "loop-add.svlib");

    testScriptParsing(filePath, output);
  }

  @Test
  public void parseSimpleLoop() throws SvLibAstParseException {

    SvLibParameterDeclaration resultVar =
        new SvLibParameterDeclaration(
            FileLocation.DUMMY, SvLibSmtLibType.INT, "|c#result|", "main");
    SvLibParameterDeclaration a =
        new SvLibParameterDeclaration(FileLocation.DUMMY, SvLibSmtLibType.INT, "a", "main");

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
                        true,
                        new SvLibCustomType("|c#ptr|", 1),
                        "|c#ptr|",
                        "|c#ptr|",
                        "|c#ptr|"),
                    FileLocation.DUMMY),
                new SvLibDeclareSortCommand(
                    new SvLibSortDeclaration(
                        FileLocation.DUMMY,
                        true,
                        new SvLibCustomType("|c#heap|", 0),
                        "|c#heap|",
                        "|c#heap|",
                        "|c#heap|"),
                    FileLocation.DUMMY),
                new SvLibVariableDeclarationCommand(
                    new SvLibVariableDeclaration(
                        FileLocation.DUMMY,
                        true,
                        false,
                        new SvLibCustomType("|c#heap|", 0),
                        "|c#heap|",
                        "|c#heap|",
                        "|c#heap|"),
                    FileLocation.DUMMY),
                new SvLibDeclareFunCommand(
                    new SvLibFunctionDeclaration(
                        FileLocation.DUMMY,
                        "|c#bitxor|",
                        ImmutableList.of(SvLibSmtLibType.INT, SvLibSmtLibType.INT),
                        SvLibSmtLibType.INT),
                    FileLocation.DUMMY),
                new SvLibDeclareFunCommand(
                    new SvLibFunctionDeclaration(
                        FileLocation.DUMMY,
                        "|c#bitand|",
                        ImmutableList.of(SvLibSmtLibType.INT, SvLibSmtLibType.INT),
                        SvLibSmtLibType.INT),
                    FileLocation.DUMMY),
                new SvLibDeclareFunCommand(
                    new SvLibFunctionDeclaration(
                        FileLocation.DUMMY,
                        "|c#bitor|",
                        ImmutableList.of(SvLibSmtLibType.INT, SvLibSmtLibType.INT),
                        SvLibSmtLibType.INT),
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
                                        new SvLibIdTerm(a, FileLocation.DUMMY),
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
                                                new SvLibIdTerm(a, FileLocation.DUMMY),
                                                new SvLibIntegerConstantTerm(
                                                    BigInteger.ONE, FileLocation.DUMMY)),
                                            FileLocation.DUMMY)),
                                    FileLocation.DUMMY,
                                    ImmutableList.of(),
                                    ImmutableList.of()),
                                ImmutableList.of(),
                                ImmutableList.of(
                                    new SvLibTagReference("while-loop", FileLocation.DUMMY, null)),
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
                                                new SvLibIdTerm(a, FileLocation.DUMMY),
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
                        ImmutableList.of(
                            new SvLibTagReference("proc-main", FileLocation.DUMMY, null)))),
                new SvLibVerifyCallCommand(
                    mainProcedureDeclaration, ImmutableList.of(), FileLocation.DUMMY),
                new SvLibGetWitnessCommand(FileLocation.DUMMY)));

    Path filePath = Path.of(examplesPath(), "loop-simple-safe.svlib");

    testScriptParsing(filePath, expectedOutput);
  }
}
