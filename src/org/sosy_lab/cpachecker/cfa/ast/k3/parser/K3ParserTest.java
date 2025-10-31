// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssertCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssertTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssumeStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3BooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3CustomType;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3DeclareConstCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3DeclareFunCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3DeclareSortCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3EnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3FunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3GetWitnessCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IdTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IfStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3RequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Script;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SequenceStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SetLogicCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SetOptionCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SmtLibType;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SortDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagReference;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3WhileStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.K3ToAstParser.K3AstParseException;

public class K3ParserTest {

  private String examplesPath() {
    return Path.of("test", "programs", "k3").toAbsolutePath().toString();
  }

  private void testScriptParsing(Path inputPath, K3Script output) throws K3AstParseException {

    String programString;
    try {
      programString = Joiner.on("\n").join(Files.readAllLines(inputPath));
    } catch (IOException e) {
      throw new K3AstParseException("Could not read input file: " + inputPath, e);
    }
    K3Script parsed = K3ToAstParser.parseScript(programString);
    assert parsed.equals(output) : "Parsed object does not match expected object";
  }

  @Test
  public void parseSimpleCorrectProgram() throws K3AstParseException {
    K3ParameterDeclaration x =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3SmtLibType.INT, "x", "f1");
    K3ParameterDeclaration y =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3SmtLibType.INT, "y", "f1");
    K3ProcedureDeclaration procedureDeclaration =
        new K3ProcedureDeclaration(
            FileLocation.DUMMY,
            "f1",
            ImmutableList.of(x, y),
            ImmutableList.of(),
            ImmutableList.of());
    K3VariableDeclaration w =
        new K3VariableDeclaration(FileLocation.DUMMY, true, false, K3SmtLibType.INT, "w", "w", "w");
    K3VariableDeclaration z =
        new K3VariableDeclaration(FileLocation.DUMMY, true, false, K3SmtLibType.INT, "z", "z", "z");

    K3Script output =
        new K3Script(
            ImmutableList.of(
                new K3SetLogicCommand(SmtLibLogic.LIA, FileLocation.DUMMY),
                new K3VariableDeclarationCommand(w, FileLocation.DUMMY),
                new K3VariableDeclarationCommand(z, FileLocation.DUMMY),
                new K3ProcedureDefinitionCommand(
                    FileLocation.DUMMY,
                    procedureDeclaration,
                    new K3SequenceStatement(
                        ImmutableList.of(
                            new K3AssumeStatement(
                                FileLocation.DUMMY,
                                new K3SymbolApplicationTerm(
                                    new K3IdTerm(
                                        SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new K3IdTerm(x, FileLocation.DUMMY),
                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new K3AssumeStatement(
                                FileLocation.DUMMY,
                                new K3SymbolApplicationTerm(
                                    new K3IdTerm(
                                        SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new K3IdTerm(x, FileLocation.DUMMY),
                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(
                                    new K3AssertTag(
                                        new K3SymbolApplicationTerm(
                                            new K3IdTerm(
                                                SmtLibTheoryDeclarations.INT_EQUALITY,
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new K3IdTerm(x, FileLocation.DUMMY),
                                                new K3IdTerm(y, FileLocation.DUMMY)),
                                            FileLocation.DUMMY),
                                        FileLocation.DUMMY)),
                                ImmutableList.of())),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of())),
                new K3VerifyCallCommand(
                    procedureDeclaration,
                    ImmutableList.of(
                        new K3IdTerm(w, FileLocation.DUMMY), new K3IdTerm(z, FileLocation.DUMMY)),
                    FileLocation.DUMMY)));
    Path filePath = Path.of(examplesPath(), "simple-correct.smt2");

    testScriptParsing(filePath, output);
  }

  @Test
  public void parseSimpleIncorrectProgram() throws K3AstParseException {
    K3ParameterDeclaration x =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3SmtLibType.INT, "x", "f1");
    K3ParameterDeclaration y =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3SmtLibType.INT, "y", "f1");
    K3ProcedureDeclaration procedureDeclaration =
        new K3ProcedureDeclaration(
            FileLocation.DUMMY,
            "f1",
            ImmutableList.of(x, y),
            ImmutableList.of(),
            ImmutableList.of());
    K3VariableDeclaration w =
        new K3VariableDeclaration(FileLocation.DUMMY, true, false, K3SmtLibType.INT, "w", "w", "w");
    K3VariableDeclaration z =
        new K3VariableDeclaration(FileLocation.DUMMY, true, false, K3SmtLibType.INT, "z", "z", "z");

    K3Script output =
        new K3Script(
            ImmutableList.of(
                new K3SetLogicCommand(SmtLibLogic.LIA, FileLocation.DUMMY),
                new K3VariableDeclarationCommand(w, FileLocation.DUMMY),
                new K3VariableDeclarationCommand(z, FileLocation.DUMMY),
                new K3ProcedureDefinitionCommand(
                    FileLocation.DUMMY,
                    procedureDeclaration,
                    new K3SequenceStatement(
                        ImmutableList.of(
                            new K3AssumeStatement(
                                FileLocation.DUMMY,
                                new K3SymbolApplicationTerm(
                                    new K3IdTerm(
                                        SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new K3IdTerm(x, FileLocation.DUMMY),
                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new K3AssumeStatement(
                                FileLocation.DUMMY,
                                new K3SymbolApplicationTerm(
                                    new K3IdTerm(
                                        SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new K3IdTerm(x, FileLocation.DUMMY),
                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(
                                    new K3AssertTag(
                                        new K3SymbolApplicationTerm(
                                            new K3IdTerm(
                                                SmtLibTheoryDeclarations.BOOL_NEGATION,
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new K3SymbolApplicationTerm(
                                                    new K3IdTerm(
                                                        SmtLibTheoryDeclarations.INT_EQUALITY,
                                                        FileLocation.DUMMY),
                                                    ImmutableList.of(
                                                        new K3IdTerm(x, FileLocation.DUMMY),
                                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                                    FileLocation.DUMMY)),
                                            FileLocation.DUMMY),
                                        FileLocation.DUMMY)),
                                ImmutableList.of())),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of())),
                new K3VerifyCallCommand(
                    procedureDeclaration,
                    ImmutableList.of(
                        new K3IdTerm(w, FileLocation.DUMMY), new K3IdTerm(z, FileLocation.DUMMY)),
                    FileLocation.DUMMY)));
    Path filePath = Path.of(examplesPath(), "simple-incorrect.smt2");

    testScriptParsing(filePath, output);
  }

  @Test
  public void parseLoopAdd() throws K3AstParseException {

    K3ParameterDeclaration x0 =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3SmtLibType.INT, "x0", "add");
    K3ParameterDeclaration y0 =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3SmtLibType.INT, "y0", "add");
    K3VariableDeclaration w0Const =
        new K3VariableDeclaration(
            FileLocation.DUMMY, true, true, K3SmtLibType.INT, "w0", "w0", "w0");
    K3VariableDeclaration z0Const =
        new K3VariableDeclaration(
            FileLocation.DUMMY, true, true, K3SmtLibType.INT, "z0", "z0", "z0");
    K3ParameterDeclaration x =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3SmtLibType.INT, "x", "add");
    K3ParameterDeclaration y =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3SmtLibType.INT, "y", "add");
    K3ProcedureDeclaration procedureDeclaration =
        new K3ProcedureDeclaration(
            FileLocation.DUMMY,
            "add",
            ImmutableList.of(x0, y0),
            ImmutableList.of(x),
            ImmutableList.of(y));

    K3Script output =
        new K3Script(
            ImmutableList.of(
                new K3SetLogicCommand(SmtLibLogic.LIA, FileLocation.DUMMY),
                new K3ProcedureDefinitionCommand(
                    FileLocation.DUMMY,
                    procedureDeclaration,
                    new K3SequenceStatement(
                        ImmutableList.of(
                            new K3AssignmentStatement(
                                ImmutableMap.of(
                                    x,
                                    new K3IdTerm(x0, FileLocation.DUMMY),
                                    y,
                                    new K3IdTerm(y0, FileLocation.DUMMY)),
                                FileLocation.DUMMY,
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new K3WhileStatement(
                                new K3SymbolApplicationTerm(
                                    new K3IdTerm(
                                        SmtLibTheoryDeclarations.INT_LESS_THAN, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new K3IntegerConstantTerm(
                                            BigInteger.ZERO, FileLocation.DUMMY),
                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                new K3AssignmentStatement(
                                    ImmutableMap.of(
                                        x,
                                        new K3SymbolApplicationTerm(
                                            new K3IdTerm(
                                                SmtLibTheoryDeclarations.intAddition(2),
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new K3IdTerm(x, FileLocation.DUMMY),
                                                new K3IntegerConstantTerm(
                                                    BigInteger.ONE, FileLocation.DUMMY)),
                                            FileLocation.DUMMY),
                                        y,
                                        new K3SymbolApplicationTerm(
                                            new K3IdTerm(
                                                SmtLibTheoryDeclarations.INT_MINUS,
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new K3IdTerm(y, FileLocation.DUMMY),
                                                new K3IntegerConstantTerm(
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
                                    new K3TagReference("while-1", FileLocation.DUMMY, null)),
                                FileLocation.DUMMY)),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of(
                            // Using null for the scope is not the best solution but works for
                            // tests, since equality over tags must be based on the name only.
                            new K3TagReference("proc-add", FileLocation.DUMMY, null)))),
                new K3AnnotateTagCommand(
                    "proc-add",
                    ImmutableList.of(
                        new K3RequiresTag(
                            new K3SymbolApplicationTerm(
                                new K3IdTerm(
                                    SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN,
                                    FileLocation.DUMMY),
                                ImmutableList.of(
                                    new K3IntegerConstantTerm(BigInteger.ZERO, FileLocation.DUMMY),
                                    new K3IdTerm(
                                        K3VariableDeclaration.dummyVariableForName("y0"),
                                        FileLocation.DUMMY)),
                                FileLocation.DUMMY),
                            FileLocation.DUMMY),
                        new K3EnsuresTag(
                            new K3SymbolApplicationTerm(
                                new K3IdTerm(
                                    SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
                                ImmutableList.of(
                                    new K3IdTerm(
                                        K3VariableDeclaration.dummyVariableForName("x"),
                                        FileLocation.DUMMY),
                                    new K3SymbolApplicationTerm(
                                        new K3IdTerm(
                                            SmtLibTheoryDeclarations.intAddition(2),
                                            FileLocation.DUMMY),
                                        ImmutableList.of(
                                            new K3IdTerm(
                                                K3VariableDeclaration.dummyVariableForName("x0"),
                                                FileLocation.DUMMY),
                                            new K3IdTerm(
                                                K3VariableDeclaration.dummyVariableForName("y0"),
                                                FileLocation.DUMMY)),
                                        FileLocation.DUMMY)),
                                FileLocation.DUMMY),
                            FileLocation.DUMMY)),
                    FileLocation.DUMMY),
                new K3DeclareConstCommand(w0Const, FileLocation.DUMMY),
                new K3DeclareConstCommand(z0Const, FileLocation.DUMMY),
                new K3AssertCommand(
                    new K3SymbolApplicationTerm(
                        new K3IdTerm(
                            SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN, FileLocation.DUMMY),
                        ImmutableList.of(
                            new K3IntegerConstantTerm(BigInteger.ZERO, FileLocation.DUMMY),
                            new K3IdTerm(w0Const, FileLocation.DUMMY)),
                        FileLocation.DUMMY),
                    FileLocation.DUMMY),
                new K3VerifyCallCommand(
                    procedureDeclaration,
                    ImmutableList.of(
                        new K3IdTerm(w0Const, FileLocation.DUMMY),
                        new K3IdTerm(z0Const, FileLocation.DUMMY)),
                    FileLocation.DUMMY),
                new K3GetWitnessCommand(FileLocation.DUMMY)));

    Path filePath = Path.of(examplesPath(), "loop-add.smt2");

    testScriptParsing(filePath, output);
  }

  @Test
  public void parseSimpleLoop() throws K3AstParseException {

    K3ParameterDeclaration resultVar =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3SmtLibType.INT, "|c#result|", "main");
    K3ParameterDeclaration a =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3SmtLibType.INT, "a", "main");

    K3ProcedureDeclaration mainProcedureDeclaration =
        new K3ProcedureDeclaration(
            FileLocation.DUMMY,
            "main",
            ImmutableList.of(),
            ImmutableList.of(resultVar),
            ImmutableList.of(a));

    K3Script expectedOutput =
        new K3Script(
            ImmutableList.of(
                new K3SetLogicCommand(SmtLibLogic.LIA, FileLocation.DUMMY),
                new K3SetOptionCommand(
                    ":witness-output-channel", "./witness.svlib", FileLocation.DUMMY),
                new K3DeclareSortCommand(
                    new K3SortDeclaration(
                        FileLocation.DUMMY,
                        true,
                        new K3CustomType("|c#ptr|", 1),
                        "|c#ptr|",
                        "|c#ptr|",
                        "|c#ptr|"),
                    FileLocation.DUMMY),
                new K3DeclareSortCommand(
                    new K3SortDeclaration(
                        FileLocation.DUMMY,
                        true,
                        new K3CustomType("|c#heap|", 0),
                        "|c#heap|",
                        "|c#heap|",
                        "|c#heap|"),
                    FileLocation.DUMMY),
                new K3VariableDeclarationCommand(
                    new K3VariableDeclaration(
                        FileLocation.DUMMY,
                        true,
                        false,
                        new K3CustomType("|c#heap|", 0),
                        "|c#heap|",
                        "|c#heap|",
                        "|c#heap|"),
                    FileLocation.DUMMY),
                new K3DeclareFunCommand(
                    new K3FunctionDeclaration(
                        FileLocation.DUMMY,
                        "|c#bitxor|",
                        ImmutableList.of(K3SmtLibType.INT, K3SmtLibType.INT),
                        K3SmtLibType.INT),
                    FileLocation.DUMMY),
                new K3DeclareFunCommand(
                    new K3FunctionDeclaration(
                        FileLocation.DUMMY,
                        "|c#bitand|",
                        ImmutableList.of(K3SmtLibType.INT, K3SmtLibType.INT),
                        K3SmtLibType.INT),
                    FileLocation.DUMMY),
                new K3DeclareFunCommand(
                    new K3FunctionDeclaration(
                        FileLocation.DUMMY,
                        "|c#bitor|",
                        ImmutableList.of(K3SmtLibType.INT, K3SmtLibType.INT),
                        K3SmtLibType.INT),
                    FileLocation.DUMMY),
                new K3ProcedureDefinitionCommand(
                    FileLocation.DUMMY,
                    mainProcedureDeclaration,
                    new K3SequenceStatement(
                        ImmutableList.of(
                            new K3AssignmentStatement(
                                ImmutableMap.of(
                                    a,
                                    new K3IntegerConstantTerm(
                                        BigInteger.valueOf(6), FileLocation.DUMMY)),
                                FileLocation.DUMMY,
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new K3AssignmentStatement(
                                ImmutableMap.of(
                                    a,
                                    new K3IntegerConstantTerm(BigInteger.ZERO, FileLocation.DUMMY)),
                                FileLocation.DUMMY,
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new K3WhileStatement(
                                new K3SymbolApplicationTerm(
                                    new K3IdTerm(
                                        SmtLibTheoryDeclarations.INT_LESS_THAN, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new K3IdTerm(a, FileLocation.DUMMY),
                                        new K3IntegerConstantTerm(
                                            BigInteger.valueOf(6), FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                new K3AssignmentStatement(
                                    ImmutableMap.of(
                                        a,
                                        new K3SymbolApplicationTerm(
                                            new K3IdTerm(
                                                SmtLibTheoryDeclarations.intAddition(2),
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new K3IdTerm(a, FileLocation.DUMMY),
                                                new K3IntegerConstantTerm(
                                                    BigInteger.ONE, FileLocation.DUMMY)),
                                            FileLocation.DUMMY)),
                                    FileLocation.DUMMY,
                                    ImmutableList.of(),
                                    ImmutableList.of()),
                                ImmutableList.of(),
                                ImmutableList.of(
                                    new K3TagReference("while-loop", FileLocation.DUMMY, null)),
                                FileLocation.DUMMY),
                            new K3IfStatement(
                                FileLocation.DUMMY,
                                ImmutableList.of(),
                                ImmutableList.of(),
                                new K3SymbolApplicationTerm(
                                    new K3IdTerm(
                                        SmtLibTheoryDeclarations.BOOL_NEGATION, FileLocation.DUMMY),
                                    ImmutableList.of(
                                        new K3SymbolApplicationTerm(
                                            new K3IdTerm(
                                                SmtLibTheoryDeclarations.INT_EQUALITY,
                                                FileLocation.DUMMY),
                                            ImmutableList.of(
                                                new K3IdTerm(a, FileLocation.DUMMY),
                                                new K3IntegerConstantTerm(
                                                    BigInteger.valueOf(6), FileLocation.DUMMY)),
                                            FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                new K3SequenceStatement(
                                    ImmutableList.of(),
                                    FileLocation.DUMMY,
                                    ImmutableList.of(
                                        new K3AssertTag(
                                            new K3BooleanConstantTerm(false, FileLocation.DUMMY),
                                            FileLocation.DUMMY)),
                                    ImmutableList.of())),
                            new K3AssignmentStatement(
                                ImmutableMap.of(
                                    resultVar,
                                    new K3IntegerConstantTerm(BigInteger.ONE, FileLocation.DUMMY)),
                                FileLocation.DUMMY,
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new K3ReturnStatement(
                                FileLocation.DUMMY, ImmutableList.of(), ImmutableList.of())),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of())),
                new K3VerifyCallCommand(
                    mainProcedureDeclaration, ImmutableList.of(), FileLocation.DUMMY),
                new K3GetWitnessCommand(FileLocation.DUMMY)));

    Path filePath = Path.of(examplesPath(), "loop-simple.smt2");

    testScriptParsing(filePath, expectedOutput);
  }
}
