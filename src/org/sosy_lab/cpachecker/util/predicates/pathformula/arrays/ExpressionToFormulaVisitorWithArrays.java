/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.arrays;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;

public class ExpressionToFormulaVisitorWithArrays extends ExpressionToFormulaVisitor {

  private final ArrayFormulaManagerView amgr;
  private final CToFormulaConverterWithArrays ctfa;
  private final MachineModel machine;

  public ExpressionToFormulaVisitorWithArrays(CToFormulaConverterWithArrays pCtoFormulaConverter,
      FormulaManagerView pMgr, MachineModel pMachineModel, CFAEdge pEdge,
      String pFunction, SSAMapBuilder pSsa, Constraints pConstraints) {
    super(pCtoFormulaConverter, pMgr, pEdge, pFunction, pSsa, pConstraints);

    amgr = mgr.getArrayFormulaManager();
    ctfa = pCtoFormulaConverter;
    machine = pMachineModel;
  }

  @Override
  public Formula visit(CUnaryExpression pExp) throws UnrecognizedCCodeException {
    final CExpression operand = pExp.getOperand();
    final UnaryOperator op = pExp.getOperator();

    if (op == UnaryOperator.AMPER && operand instanceof CArraySubscriptExpression) {
      // Example use-case:
      //  C expression
      //      a[2] == b[i]
      //  that is (semantically equivalent) transformed to
      //    &(a[2]) == &(b[i])
      //
      //  Example for a CArraySubscriptExpression: a[2]
      //   .arrayExpression: a
      //   .subscriptExpression: 2
      //   .type: (int)[]

      final CArraySubscriptExpression ase = (CArraySubscriptExpression) operand;
      if (!(ase.getArrayExpression() instanceof CIdExpression)) {
        throw new UnrecognizedCCodeException("CArraySubscriptExpression: Assuming that every array-expression is a CidExpression!", pExp);
      }

      final String arrayVarName = ((CIdExpression) ase.getArrayExpression()).getDeclaration().getQualifiedName();
      final CType arrayType = ase.getArrayExpression().getExpressionType();

      final ArrayFormula<?, ?> arrayDeclaration = (ArrayFormula<?, ?>) ctfa.makeVariableForMe(arrayVarName, arrayType, ssa);
      // Make a cast of the subscript expression to the type of the array domain (type of for the index)
      final Formula arrayIndexExpr = ase.getSubscriptExpression().accept(this);
      final Formula arrayIndexExprCasted = ctfa.makeCastForMe(
            ase.getSubscriptExpression().getExpressionType(),
            machine.getPointerEquivalentSimpleType(), arrayIndexExpr, null, null);

      return amgr.select(arrayDeclaration, arrayIndexExprCasted);

    } else {
      return super.visit(pExp);
    }
  }
}
