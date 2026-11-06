// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssumeStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibBreakStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibChoiceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibContinueStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibGotoStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibIfStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibLabelStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibReturnStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibSequenceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatementVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibWhileStatement;

class CToSvLibErrorLabelEncodingVisitor
    implements SvLibStatementVisitor<Void, IllegalArgumentException> {

  /**
   * The label of a node whose label in the C program is an error label, followed by the name of the
   * node (see CToSvLibTransformation#createAllLabels).
   */
  private final Pattern errorLabel;

  private final ImmutableList.Builder<SvLibTagReference> errorLabelTagReferences;

  /**
   * @param pErrorLabel the regular expression for the labels of the C program that the
   *     specification considers an error
   */
  CToSvLibErrorLabelEncodingVisitor(
      String pErrorLabel, ImmutableList.Builder<SvLibTagReference> pErrorLabelTagReferences) {
    errorLabel = Pattern.compile("(" + pErrorLabel + ")__N[0-9]+");
    errorLabelTagReferences = pErrorLabelTagReferences;
  }

  @Override
  public Void visit(SvLibAssignmentStatement pSvLibAssignmentStatement)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public Void visit(SvLibHavocStatement pSvLibHavocStatement) throws IllegalArgumentException {
    return null;
  }

  @Override
  public Void visit(SvLibProcedureCallStatement pSvLibProcedureCallStatement)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public Void visit(SvLibSequenceStatement pSvLibSequenceStatement)
      throws IllegalArgumentException {
    for (SvLibStatement substatement : pSvLibSequenceStatement.getStatements()) {
      substatement.accept(this);
    }

    return null;
  }

  @Override
  public Void visit(SvLibAssumeStatement pSvLibAssumeStatement) throws IllegalArgumentException {
    return null;
  }

  @Override
  public Void visit(SvLibWhileStatement pSvLibWhileStatement) throws IllegalArgumentException {
    pSvLibWhileStatement.getBody().accept(this);

    return null;
  }

  @Override
  public Void visit(SvLibIfStatement pSvLibIfStatement) throws IllegalArgumentException {
    pSvLibIfStatement.getThenBranch().accept(this);
    if (pSvLibIfStatement.getElseBranch().isPresent()) {
      SvLibStatement elseBranch = pSvLibIfStatement.getElseBranch().orElseThrow();
      elseBranch.accept(this);
    }

    return null;
  }

  @Override
  public Void visit(SvLibBreakStatement pSvLibBreakStatement) throws IllegalArgumentException {
    return null;
  }

  @Override
  public Void visit(SvLibContinueStatement pSvLibContinueStatement)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public Void visit(SvLibReturnStatement pSvLibReturnStatement) throws IllegalArgumentException {
    return null;
  }

  @Override
  public Void visit(SvLibGotoStatement pSvLibGotoStatement) throws IllegalArgumentException {
    return null;
  }

  @Override
  public Void visit(SvLibLabelStatement pSvLibLabelStatement) throws IllegalArgumentException {
    if (errorLabel.matcher(pSvLibLabelStatement.getLabel()).matches()) {
      ImmutableList<SvLibTagReference> tagReferences = pSvLibLabelStatement.getTagReferences();
      checkArgument(!tagReferences.isEmpty(), "Missing tag reference for ERROR label");
      errorLabelTagReferences.add(tagReferences.getFirst());
    }
    return null;
  }

  @Override
  public Void visit(SvLibChoiceStatement pSvLibChoiceStatement) throws IllegalArgumentException {
    for (SvLibStatement possibleChoice : pSvLibChoiceStatement.getChoices()) {
      possibleChoice.accept(this);
    }
    return null;
  }
}
