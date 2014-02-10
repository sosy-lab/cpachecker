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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.types.CtoFormulaTypeUtils.getRealFieldOwner;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;

import com.google.common.base.Optional;

class LvalueVisitor extends
    DefaultCExpressionVisitor<Formula, UnrecognizedCCodeException> {

  protected final CtoFormulaConverter conv;
  protected final CFAEdge       edge;
  protected final String        function;
  protected final SSAMapBuilder ssa;
  protected final Constraints   constraints;

  public LvalueVisitor(CtoFormulaConverter pCtoFormulaConverter, CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
    conv = pCtoFormulaConverter;
    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    constraints = pCo;
  }

  @Override
  protected BitvectorFormula visitDefault(CExpression exp) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Unknown lvalue", edge, exp);
  }

  @Override
  public Formula visit(CIdExpression idExp) {
    Variable var = conv.scopedIfNecessary(idExp, ssa, function);
    return conv.makeFreshVariable(var.getName(), var.getType(), ssa);
  }

  /**  This method is called when we don't know what else to do. */
  protected Formula giveUpAndJustMakeVariable(CExpression exp) {
    return conv.makeVariableUnsafe(exp, function, ssa, true);
  }


  @Override
  public Formula visit(CUnaryExpression pE) throws UnrecognizedCCodeException {
    return giveUpAndJustMakeVariable(pE);
  }

  @Override
  public Formula visit(CComplexCastExpression pE) throws UnrecognizedCCodeException {
    if(pE.isImaginaryCast()) {
      throw new UnrecognizedCCodeException("Unknown lvalue", edge, pE);
    }
    // TODO complex numbers are not supported for evaluation right now
    return giveUpAndJustMakeVariable(pE);
  }

  @Override
  public Formula visit(CPointerExpression pE) throws UnrecognizedCCodeException {
    return giveUpAndJustMakeVariable(pE);
  }

  @Override
  public Formula visit(CFieldReference fexp) throws UnrecognizedCCodeException {
    if (!conv.options.handleFieldAccess()) {
      CExpression fieldRef = fexp.getFieldOwner();
      if (fieldRef instanceof CIdExpression) {
        CSimpleDeclaration decl = ((CIdExpression) fieldRef).getDeclaration();
        if (decl instanceof CDeclaration && ((CDeclaration)decl).isGlobal()) {
          // this is the reference to a global field variable
          // we don't need to scope the variable reference
          String var = CtoFormulaConverter.exprToVarName(fexp);

          return conv.makeFreshVariable(var, fexp.getExpressionType(), ssa);
        }
      }
      return giveUpAndJustMakeVariable(fexp);
    }

    // s.a = ...
    // s->b = ...
    // make a new s and return the formula accessing the field
    // as constraint add that all other fields (the rest of the bitvector) remains the same.
    CExpression owner = getRealFieldOwner(fexp);
    // This will just create the formula with the current ssa-index.
    Formula oldStructure = conv.buildTerm(owner, edge, function, ssa, constraints);
    // This will eventually increment the ssa-index and return the new formula.
    Formula newStructure = owner.accept(this);

    // Other fields did not change.
    Formula oldRestS = conv.replaceField(fexp, oldStructure, Optional.<Formula>absent());
    Formula newRestS = conv.replaceField(fexp, newStructure, Optional.<Formula>absent());
    constraints.addConstraint(conv.fmgr.makeEqual(oldRestS, newRestS));

    Formula fieldFormula = conv.accessField(fexp, newStructure);
    return fieldFormula;
  }

  @Override
  public Formula visit(CArraySubscriptExpression pE) throws UnrecognizedCCodeException {
    return giveUpAndJustMakeVariable(pE);
  }
}