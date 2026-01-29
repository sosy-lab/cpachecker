// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c.export;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public abstract class CFunctionDefinitionStatement implements CExportStatement {

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

  /**
   * Basically {@link CFunctionDeclaration#toASTString()} with parameter names but without the
   * suffix {@code ;}.
   */
  private String buildSignature(AAstNodeRepresentation pAAstNodeRepresentation) {
    StringJoiner parameters = new StringJoiner(", ");
    declaration
        .getParameters()
        .forEach(p -> parameters.add(p.toASTString(pAAstNodeRepresentation)));
    String returnType = declaration.getType().getReturnType().toASTString("");
    return returnType + " " + declaration.getName() + "(" + parameters + ")";
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner rDefinition = new StringJoiner(System.lineSeparator());
    rDefinition.add(buildSignature(pAAstNodeRepresentation));
    rDefinition.add(body.toASTString(pAAstNodeRepresentation));
    return rDefinition.toString();
  }

  public final CFunctionCallStatement buildFunctionCallStatement(
      ImmutableList<CExpression> pParameters) {

    checkArgument(
        declaration.getParameters().size() == pParameters.size(),
        "pParameters.size() must equal parameter declaration amount");
    CFunctionCallExpression functionCallExpression =
        new CFunctionCallExpression(
            FileLocation.DUMMY, declaration.getType(), name, pParameters, declaration);
    return new CFunctionCallStatement(FileLocation.DUMMY, functionCallExpression);
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
