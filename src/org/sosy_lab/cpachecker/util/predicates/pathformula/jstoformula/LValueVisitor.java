/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;



import org.sosy_lab.cpachecker.cfa.ast.js.DefaultJSExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSDeclaredByExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.java_smt.api.Formula;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
class LValueVisitor extends DefaultJSExpressionVisitor<Formula, UnrecognizedCodeException> {

  private final JSToFormulaConverter conv;
  private final CFAEdge       edge;
  private final String        function;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints   constraints;
  private final ErrorConditions errorConditions;

  LValueVisitor(
      JSToFormulaConverter pConv,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions) {

    conv = pConv;
    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    pts = pPts;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
  }

  @Override
  protected Formula visitDefault(final JSExpression exp) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("Not handled yet", exp);
  }

  @Override
  public Formula visit(final JSFloatLiteralExpression pLiteral) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("Not handled yet", pLiteral);
  }

  @Override
  public Formula visit(final JSIntegerLiteralExpression pIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("Not handled yet", pIntegerLiteralExpression);
  }

  @Override
  public Formula visit(final JSBooleanLiteralExpression pBooleanLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("Not handled yet", pBooleanLiteralExpression);
  }

  @Override
  public Formula visit(final JSNullLiteralExpression pNullLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("Not handled yet", pNullLiteralExpression);
  }

  @Override
  public Formula visit(final JSObjectLiteralExpression pObjectLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("JSObjectLiteralExpression not handled yet", pObjectLiteralExpression);
  }

  @Override
  public Formula visit(final JSUndefinedLiteralExpression pUndefinedLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("Not handled yet", pUndefinedLiteralExpression);
  }

  @Override
  public Formula visit(final JSThisExpression pThisExpression) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("Not handled yet", pThisExpression);
  }

  @Override
  public Formula visit(final JSDeclaredByExpression pDeclaredByExpression)
      throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException(
        "JSDeclaredByExpression not handled yet", pDeclaredByExpression);
  }

  @Override
  public Formula visit(final JSIdExpression pIdExpression) throws UnrecognizedCodeException {
    return conv.makeFreshVariable(pIdExpression.getDeclaration().getQualifiedName(), pIdExpression.getExpressionType
        (), ssa);
  }
}