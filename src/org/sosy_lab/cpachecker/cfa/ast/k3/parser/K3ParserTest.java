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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssertTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssumeStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IdTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Script;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SequenceStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Type;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.VerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.K3ToAstParser.K3AstParseException;

public class K3ParserTest {

  private String examplesPath() {
    return Path.of("test", "programs", "k3").toAbsolutePath().toString();
  }

  private void testScriptParsing(Path inputPath, K3Script output) throws K3AstParseException {

    String programString;
    try {
      programString = Joiner.on("\n").join(Files.readAllLines(inputPath));
    } catch (IOException pE) {
      throw new K3AstParseException("Could not read input file: " + inputPath, pE);
    }
    K3Script parsed = K3ToAstParser.parseScript(programString);
    assert parsed.equals(output) : "Parsed object does not match expected object";
  }

  @Test
  public void parseSimpleCorrectProgram() throws K3AstParseException {
    K3ParameterDeclaration x =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3Type.getTypeForString("Int"), "x");
    K3ParameterDeclaration y =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3Type.getTypeForString("Int"), "y");
    K3ProcedureDeclaration procedureDeclaration =
        new K3ProcedureDeclaration(
            FileLocation.DUMMY,
            "f1",
            ImmutableList.of(x, y),
            ImmutableList.of(),
            ImmutableList.of());
    K3VariableDeclaration w =
        new K3VariableDeclaration(
            FileLocation.DUMMY, true, K3Type.getTypeForString("Int"), "w", "w", "w");
    K3VariableDeclaration z =
        new K3VariableDeclaration(
            FileLocation.DUMMY, true, K3Type.getTypeForString("Int"), "z", "z", "z");

    K3Script output =
        new K3Script(
            ImmutableList.of(
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
                                    "=",
                                    ImmutableList.of(
                                        new K3IdTerm(x, FileLocation.DUMMY),
                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new K3AssumeStatement(
                                FileLocation.DUMMY,
                                new K3SymbolApplicationTerm(
                                    "=",
                                    ImmutableList.of(
                                        new K3IdTerm(x, FileLocation.DUMMY),
                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(
                                    new K3AssertTag(
                                        new K3SymbolApplicationTerm(
                                            "=",
                                            ImmutableList.of(
                                                new K3IdTerm(x, FileLocation.DUMMY),
                                                new K3IdTerm(y, FileLocation.DUMMY)),
                                            FileLocation.DUMMY),
                                        FileLocation.DUMMY)),
                                ImmutableList.of())),
                        FileLocation.DUMMY,
                        ImmutableList.of(),
                        ImmutableList.of())),
                new VerifyCallCommand(
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
        new K3ParameterDeclaration(FileLocation.DUMMY, K3Type.getTypeForString("Int"), "x");
    K3ParameterDeclaration y =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3Type.getTypeForString("Int"), "y");
    K3ProcedureDeclaration procedureDeclaration =
        new K3ProcedureDeclaration(
            FileLocation.DUMMY,
            "f1",
            ImmutableList.of(x, y),
            ImmutableList.of(),
            ImmutableList.of());
    K3VariableDeclaration w =
        new K3VariableDeclaration(
            FileLocation.DUMMY, true, K3Type.getTypeForString("Int"), "w", "w", "w");
    K3VariableDeclaration z =
        new K3VariableDeclaration(
            FileLocation.DUMMY, true, K3Type.getTypeForString("Int"), "z", "z", "z");

    K3Script output =
        new K3Script(
            ImmutableList.of(
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
                                    "=",
                                    ImmutableList.of(
                                        new K3IdTerm(x, FileLocation.DUMMY),
                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new K3AssumeStatement(
                                FileLocation.DUMMY,
                                new K3SymbolApplicationTerm(
                                    "=",
                                    ImmutableList.of(
                                        new K3IdTerm(x, FileLocation.DUMMY),
                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(
                                    new K3AssertTag(
                                        new K3SymbolApplicationTerm(
                                            "not",
                                            ImmutableList.of(
                                                new K3SymbolApplicationTerm(
                                                    "=",
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
                new VerifyCallCommand(
                    procedureDeclaration,
                    ImmutableList.of(
                        new K3IdTerm(w, FileLocation.DUMMY), new K3IdTerm(z, FileLocation.DUMMY)),
                    FileLocation.DUMMY)));
    Path filePath = Path.of(examplesPath(), "simple-incorrect.smt2");

    testScriptParsing(filePath, output);
  }

  @Test
  public void parseLoopAdd() throws K3AstParseException {

    K3ParameterDeclaration x =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3Type.getTypeForString("Int"), "x");
    K3ParameterDeclaration y =
        new K3ParameterDeclaration(FileLocation.DUMMY, K3Type.getTypeForString("Int"), "y");
    K3ProcedureDeclaration procedureDeclaration =
        new K3ProcedureDeclaration(
            FileLocation.DUMMY,
            "f1",
            ImmutableList.of(x, y),
            ImmutableList.of(),
            ImmutableList.of());
    K3VariableDeclaration w =
        new K3VariableDeclaration(
            FileLocation.DUMMY, true, K3Type.getTypeForString("Int"), "w", "w", "w");
    K3VariableDeclaration z =
        new K3VariableDeclaration(
            FileLocation.DUMMY, true, K3Type.getTypeForString("Int"), "z", "z", "z");

    K3Script output =
        new K3Script(
            ImmutableList.of(
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
                                    "=",
                                    ImmutableList.of(
                                        new K3IdTerm(x, FileLocation.DUMMY),
                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(),
                                ImmutableList.of()),
                            new K3AssumeStatement(
                                FileLocation.DUMMY,
                                new K3SymbolApplicationTerm(
                                    "=",
                                    ImmutableList.of(
                                        new K3IdTerm(x, FileLocation.DUMMY),
                                        new K3IdTerm(y, FileLocation.DUMMY)),
                                    FileLocation.DUMMY),
                                ImmutableList.of(
                                    new K3AssertTag(
                                        new K3SymbolApplicationTerm(
                                            "not",
                                            ImmutableList.of(
                                                new K3SymbolApplicationTerm(
                                                    "=",
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
                new VerifyCallCommand(
                    procedureDeclaration,
                    ImmutableList.of(
                        new K3IdTerm(w, FileLocation.DUMMY), new K3IdTerm(z, FileLocation.DUMMY)),
                    FileLocation.DUMMY)));

    Path filePath = Path.of(examplesPath(), "loop-add.smt2");

    testScriptParsing(filePath, output);
  }
}
