// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSExpression;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;

public final class SvLibProceduresRecDefinitionCommand implements SvLibCommand {

  @Serial private static final long serialVersionUID = 8618438768197362510L;
  private final FileLocation fileLocation;
  private final ImmutableList<SvLibProcedureDeclaration> procedureDeclarations;
  private final ImmutableList<SvLibStatement> bodies;

  public SvLibProceduresRecDefinitionCommand(
      FileLocation pFileLocation,
      List<SvLibProcedureDeclaration> pProcedureDeclarations,
      List<SvLibStatement> pBodies) {
    checkArgument(
        pProcedureDeclarations.size() == pBodies.size(),
        "Number of procedure declarations must match number of bodies");
    fileLocation = pFileLocation;
    procedureDeclarations = ImmutableList.copyOf(pProcedureDeclarations);
    bodies = ImmutableList.copyOf(pBodies);
  }

  public List<SvLibProcedureDeclaration> getProcedureDeclarations() {
    return procedureDeclarations;
  }

  public List<SvLibStatement> getBodies() {
    return bodies;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    // Every declaration and every body is a list of its own, see Fig. 9 of the SV-LIB paper
    // (https://arxiv.org/abs/2511.21509).
    return SvLibSExpression.list(
            FluentIterable.of(SvLibSExpression.atom("define-procs-rec"))
                .append(
                    FluentIterable.from(procedureDeclarations)
                        .transform(
                            declaration -> SvLibSExpression.list(declaration.toSExpressions())))
                .append(
                    FluentIterable.from(bodies)
                        .transform(body -> SvLibSExpression.list(body.toSExpression())))
                .toList())
        .format();
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibCommandVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    return procedureDeclarations.hashCode() * 31 + bodies.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibProceduresRecDefinitionCommand other
        && procedureDeclarations.equals(other.procedureDeclarations)
        && bodies.equals(other.bodies);
  }
}
