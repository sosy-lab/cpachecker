// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
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
import org.sosy_lab.cpachecker.exceptions.NoException;

public class SvLibLabelCollectingVisitor implements SvLibStatementVisitor<Void, NoException> {

  private final SvLibProcedureDeclaration procedure;
  private final ImmutableMap.Builder<String, CFANode> labelsToNodes;

  public SvLibLabelCollectingVisitor(
      SvLibProcedureDeclaration pProcedure, ImmutableMap.Builder<String, CFANode> pLabelsToNodes) {
    procedure = pProcedure;
    labelsToNodes = pLabelsToNodes;
  }

  @Override
  public Void visit(SvLibAssignmentStatement pSvLibAssignmentStatement) throws NoException {
    return null;
  }

  @Override
  public Void visit(SvLibHavocStatement pSvLibHavocStatement) throws NoException {
    return null;
  }

  @Override
  public Void visit(SvLibProcedureCallStatement pSvLibProcedureCallStatement) throws NoException {
    return null;
  }

  @Override
  public Void visit(SvLibSequenceStatement pSvLibSequenceStatement) throws NoException {
    for (SvLibStatement subStatement : pSvLibSequenceStatement.getStatements()) {
      subStatement.accept(this);
    }

    return null;
  }

  @Override
  public Void visit(SvLibAssumeStatement pSvLibAssumeStatement) throws NoException {
    return null;
  }

  @Override
  public Void visit(SvLibWhileStatement pSvLibWhileStatement) throws NoException {
    pSvLibWhileStatement.getBody().accept(this);

    return null;
  }

  @Override
  public Void visit(SvLibIfStatement pSvLibIfStatement) throws NoException {
    pSvLibIfStatement.getThenBranch().accept(this);
    if (pSvLibIfStatement.getElseBranch().isPresent()) {
      SvLibStatement elseBranch = pSvLibIfStatement.getElseBranch().orElseThrow();
      elseBranch.accept(this);
    }

    return null;
  }

  @Override
  public Void visit(SvLibBreakStatement pSvLibBreakStatement) throws NoException {
    return null;
  }

  @Override
  public Void visit(SvLibContinueStatement pSvLibContinueStatement) throws NoException {
    return null;
  }

  @Override
  public Void visit(SvLibReturnStatement pSvLibReturnStatement) throws NoException {
    return null;
  }

  @Override
  public Void visit(SvLibGotoStatement pSvLibGotoStatement) throws NoException {
    return null;
  }

  @Override
  public Void visit(SvLibLabelStatement pSvLibLabelStatement) throws NoException {
    CFANode dummyNode =
        new CFALabelNode(procedure.toSimpleDeclaration(), pSvLibLabelStatement.getLabel());
    labelsToNodes.put(pSvLibLabelStatement.getLabel(), dummyNode);
    return null;
  }

  @Override
  public Void visit(SvLibChoiceStatement pSvLibChoiceStatement) throws NoException {
    for (SvLibStatement possibleChoice : pSvLibChoiceStatement.getChoices()) {
      possibleChoice.accept(this);
    }

    return null;
  }
}
