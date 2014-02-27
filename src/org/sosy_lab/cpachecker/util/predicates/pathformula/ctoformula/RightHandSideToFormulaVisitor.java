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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;

class RightHandSideToFormulaVisitor extends ExpressionToFormulaVisitor
                                     implements CRightHandSideVisitor<Formula, UnrecognizedCCodeException> {

  public RightHandSideToFormulaVisitor(CtoFormulaConverter pCtoFormulaConverter,
      CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, Constraints pCo) {
    super(pCtoFormulaConverter, pEdge, pFunction, pSsa, pCo);
  }

  @Override
  public Formula visit(CFunctionCallExpression fexp) throws UnrecognizedCCodeException {

    CExpression fn = fexp.getFunctionNameExpression();
    List<CExpression> pexps = fexp.getParameterExpressions();
    String func;
    CType expType = fexp.getExpressionType();
    if (fn instanceof CIdExpression) {
      func = ((CIdExpression)fn).getName();
      if (func.equals(CtoFormulaConverter.ASSUME_FUNCTION_NAME) && pexps.size() == 1) {
        BooleanFormula condition = conv.toBooleanFormula(pexps.get(0).accept(this));
        constraints.addConstraint(condition);

        return conv.makeFreshVariable(func, expType, ssa);

      } else if (conv.options.isNondetFunction(func)
          || conv.options.isMemoryAllocationFunction(func)
          || conv.options.isMemoryAllocationFunctionWithZeroing(func)) {
        // Function call like "random()".
        // Also "malloc()" etc. just return a random value, so handle them similarly.
        // Ignore parameters and just create a fresh variable for it.
        return conv.makeFreshVariable(func, expType, ssa);

      } else if (conv.options.isExternModelFunction(func)) {
        ExternModelLoader loader = new ExternModelLoader(conv.typeHandler, conv.bfmgr, conv.fmgr);
        BooleanFormula result = loader.handleExternModelFunction(fexp, pexps, ssa);
        FormulaType<?> returnFormulaType = conv.getFormulaTypeFromCType(fexp.getExpressionType());
        return conv.ifTrueThenOneElseZero(returnFormulaType, result);

      } else if (CtoFormulaConverter.UNSUPPORTED_FUNCTIONS.containsKey(func)) {
        throw new UnsupportedCCodeException(CtoFormulaConverter.UNSUPPORTED_FUNCTIONS.get(func), edge, fexp);

      } else if (!CtoFormulaConverter.PURE_EXTERNAL_FUNCTIONS.contains(func)) {
        if (pexps.isEmpty()) {
          // function of arity 0
          conv.logger.logOnce(Level.INFO, "Assuming external function", func, "to be a constant function.");
        } else {
          conv.logger.logOnce(Level.INFO, "Assuming external function", func, "to be a pure function.");
        }
      }
    } else {
      conv.logfOnce(Level.WARNING, edge, "Ignoring function call through function pointer %s", fn);
      func = ("<func>{" + CtoFormulaConverter.scoped(CtoFormulaConverter.exprToVarName(fn), function) + "}").intern();
    }

    if (pexps.isEmpty()) {
      // This is a function of arity 0 and we assume its constant.
      return conv.makeConstant(func, expType);

    } else {
      CFunctionDeclaration declaration = fexp.getDeclaration();
      if (declaration == null) {
        if (fn instanceof CIdExpression) {
          // This happens only if there are undeclared functions.
          conv.logger.logfOnce(Level.WARNING, "Cannot get declaration of function %s, ignoring calls to it.", fn);
        }
        return conv.makeFreshVariable(func, expType, ssa); // BUG when expType = void
      }

      if (declaration.getType().takesVarArgs()) {
        // Create a fresh variable instead of an UF for varargs functions.
        // This is sound but slightly more imprecise (we loose the UF axioms).
        return conv.makeFreshVariable(func, expType, ssa);
      }

      List<CType> paramTypes = declaration.getType().getParameters();
      func += "{" + paramTypes.size() + "}"; // add #arguments to function name to cope with varargs functions

      if (paramTypes.size() != pexps.size()) {
        throw new UnrecognizedCCodeException("Function " + declaration + " received " + pexps.size() + " parameters instead of the expected " + paramTypes.size(), edge, fexp);
      }

      List<Formula> args = new ArrayList<>(pexps.size());
      Iterator<CType> it1 = paramTypes.iterator();
      Iterator<CExpression> it2 = pexps.iterator();
      while (it1.hasNext() && it2.hasNext()) {

        CType paramType= it1.next();
        CExpression pexp = it2.next();
        pexp = conv.makeCastFromArrayToPointerIfNecessary(pexp, paramType);

        Formula arg = pexp.accept(this);
        args.add(conv.makeCast(pexp.getExpressionType(), paramType, arg, edge));
      }
      assert !it1.hasNext() && !it2.hasNext();

      CType returnType = conv.getReturnType(fexp, edge);
      FormulaType<?> t = conv.getFormulaTypeFromCType(returnType);
      return conv.ffmgr.createFuncAndCall(func, t, args);
    }
  }
}