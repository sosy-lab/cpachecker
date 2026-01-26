// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSmtFunctionDeclaration;

public final class SmtLibDefineFunsRecCommand implements SmtLibCommand, SvLibCommand {
  @Serial private static final long serialVersionUID = 3049346957426478591L;
  private final ImmutableList<SvLibSmtFunctionDeclaration> functionDeclarations;
  private final ImmutableList<SvLibTerm> bodies;
  private final FileLocation fileLocation;

  public SmtLibDefineFunsRecCommand(
      List<SvLibSmtFunctionDeclaration> pFunctionDeclarations,
      List<SvLibTerm> pBodies,
      FileLocation pFileLocation) {
    checkArgument(
        pFunctionDeclarations.size() == pBodies.size(),
        "Number of function declarations must match number of bodies");
    functionDeclarations = ImmutableList.copyOf(pFunctionDeclarations);
    bodies = ImmutableList.copyOf(pBodies);
    fileLocation = pFileLocation;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibCommandVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(define-funs-rec (("
        + Joiner.on(") (")
            .join(
                functionDeclarations.stream()
                    .map(
                        pFunctionDeclaration ->
                            pFunctionDeclaration.getName()
                                + pFunctionDeclaration.getType().toASTString())
                    .toList())
        + ")) ("
        + Joiner.on(") (").join(bodies.stream().map(b -> b.toASTString()).toList())
        + "))";
  }

  public List<SvLibSmtFunctionDeclaration> getFunctionDeclarations() {
    return functionDeclarations;
  }

  public List<SvLibTerm> getBodies() {
    return bodies;
  }

  @Override
  public int hashCode() {
    return functionDeclarations.hashCode() * 31 + bodies.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SmtLibDefineFunsRecCommand other
        && functionDeclarations.equals(other.functionDeclarations)
        && bodies.equals(other.bodies);
  }
}
