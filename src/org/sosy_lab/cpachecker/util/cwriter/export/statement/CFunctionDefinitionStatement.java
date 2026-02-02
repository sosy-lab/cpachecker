// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.statement;


import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A {@link CExportStatement} to represent an entire C function, including return {@link CType},
 * function name as {@link CIdExpression}, {@link CFunctionDeclaration} with parameters and a {@link
 * CCompoundStatement} to represent the body of the function. Example:
 *
 * <pre>{@code
 * int main(int arg) {
 *     int x = 1;
 * }
 * }</pre>
 */
public final class CFunctionDefinitionStatement implements CExportStatement {

  /**
   * The {@link CFunctionDeclaration} only contains a {@link String} representation of the name, but
   * a {@link CFunctionCallExpression} requires a {@link CIdExpression}, so we keep this separate.
   */
  private final CIdExpression name;

  private final CFunctionDeclaration declaration;

  private final CCompoundStatement body;

  public CFunctionDefinitionStatement(CFunctionDeclaration pDeclaration, CCompoundStatement pBody) {
    name = new CIdExpression(FileLocation.DUMMY, pDeclaration);
    declaration = pDeclaration;
    body = pBody;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner rDefinition = new StringJoiner(System.lineSeparator());

    StringJoiner parameters = new StringJoiner(", ");
    declaration
        .getParameters()
        .forEach(p -> parameters.add(p.toASTString(pAAstNodeRepresentation)));
    String returnType = declaration.getType().getReturnType().toASTString("");
    rDefinition.add(returnType + " " + declaration.getName() + "(" + parameters + ")");

    rDefinition.add(body.toASTString(pAAstNodeRepresentation));
    return rDefinition.toString();
  }

  public CType getReturnType() {
    return declaration.getType().getReturnType();
  }

  public CIdExpression getName() {
    return name;
  }

  public CFunctionDeclaration getDeclaration() {
    return declaration;
  }

  public CCompoundStatement getBody() {
    return body;
  }
}
