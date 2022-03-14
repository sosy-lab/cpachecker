// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class IsRelevantWithHavocAbstractionVisitor extends IsRelevantLhsVisitor
    implements CRightHandSideVisitor<Boolean, NoException> {

  public IsRelevantWithHavocAbstractionVisitor(CtoFormulaConverter pConv) {
    super(pConv);
  }

  @Override
  public Boolean visit(final CFieldReference e) {
    if (e.isPointerDereference() || !e.getFieldOwner().accept(this)) {
      return false;
    }
    return super.visit(e);
  }

  @Override
  public Boolean visit(CBinaryExpression e) {
    return e.getOperand1().accept(this) && e.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(CPointerExpression e) {
    return false;
  }

  @Override
  protected Boolean visitDefault(CExpression e) {
    return true;
  }

  @Override
  public Boolean visit(CFunctionCallExpression pIastFunctionCallExpression) {
    return true;
  }
}
