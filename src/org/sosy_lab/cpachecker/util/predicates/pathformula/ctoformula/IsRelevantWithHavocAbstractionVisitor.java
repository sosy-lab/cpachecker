/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;

public class IsRelevantWithHavocAbstractionVisitor extends IsRelevantLhsVisitor
  implements CRightHandSideVisitor<Boolean, RuntimeException>{

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
  public Boolean visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws RuntimeException {
    return true;
  }
}
