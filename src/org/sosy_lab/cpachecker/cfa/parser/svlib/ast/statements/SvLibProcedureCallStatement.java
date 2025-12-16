// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;

public final class SvLibProcedureCallStatement extends SvLibStatement {
  @Serial private static final long serialVersionUID = -2879361994769890189L;

  private final SvLibProcedureDeclaration procedureDeclaration;
  private final ImmutableList<SvLibTerm> arguments;
  private final ImmutableList<SvLibSimpleParsingDeclaration> returnVariables;

  public SvLibProcedureCallStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences,
      SvLibProcedureDeclaration pProcedureDeclaration,
      List<SvLibTerm> pArguments,
      List<SvLibSimpleParsingDeclaration> pReturnVariables) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    procedureDeclaration = pProcedureDeclaration;
    arguments = ImmutableList.copyOf(pArguments);
    returnVariables = ImmutableList.copyOf(pReturnVariables);
  }

  public SvLibProcedureDeclaration getProcedureDeclaration() {
    return procedureDeclaration;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return super.getFileLocation();
  }

  @Override
  public String toASTStringWithoutTags() {
    return "(call "
        + procedureDeclaration.getName()
        + " ("
        + String.join(" ", arguments.stream().map(arg -> arg.toASTString()).toList())
        + ") ("
        + String.join(" ", returnVariables.stream().map(var -> var.toASTString()).toList())
        + "))";
  }

  public ImmutableList<SvLibSimpleParsingDeclaration> getReturnVariables() {
    return returnVariables;
  }

  public ImmutableList<SvLibTerm> getArguments() {
    return arguments;
  }
}
