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
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSetBuilder;

class RightHandSideToFormulaVisitor extends ExpressionToFormulaVisitor
                                     implements CRightHandSideVisitor<Formula, UnrecognizedCCodeException> {

  private final PointerTargetSetBuilder pts;
  private final ErrorConditions errorConditions;

  public RightHandSideToFormulaVisitor(CtoFormulaConverter pCtoFormulaConverter,
      CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, PointerTargetSetBuilder pPts,
      Constraints pCo, ErrorConditions pErrorConditions) {
    super(pCtoFormulaConverter, pEdge, pFunction, pSsa, pCo);
    pts = pPts;
    errorConditions = pErrorConditions;
  }

  @Override
  public Formula visit(CFunctionCallExpression e) throws UnrecognizedCCodeException {
    final CExpression functionNameExpression = e.getFunctionNameExpression();
    final CType returnType = e.getExpressionType();
    final List<CExpression> parameters = e.getParameterExpressions();

    // First let's handle special cases such as assumes, allocations, nondets, external models, etc.
    final String functionName;
    if (functionNameExpression instanceof CIdExpression) {
      functionName = ((CIdExpression)functionNameExpression).getName();
      if (functionName.equals(CtoFormulaConverter.ASSUME_FUNCTION_NAME) && parameters.size() == 1) {
        final BooleanFormula condition = conv.makePredicate(parameters.get(0), true, edge, function, ssa, pts, constraints, errorConditions);
        constraints.addConstraint(condition);
        return conv.makeFreshVariable(functionName, returnType, ssa);

      } else if (conv.options.isNondetFunction(functionName)
          || conv.options.isMemoryAllocationFunction(functionName)
          || conv.options.isMemoryAllocationFunctionWithZeroing(functionName)) {
        // Function call like "random()".
        // Also "malloc()" etc. just return a random value, so handle them similarly.
        // Ignore parameters and just create a fresh variable for it.
        return conv.makeFreshVariable(functionName, returnType, ssa);

      } else if (conv.options.isExternModelFunction(functionName)) {
        ExternModelLoader loader = new ExternModelLoader(conv.typeHandler, conv.bfmgr, conv.fmgr);
        BooleanFormula result = loader.handleExternModelFunction(e, parameters, ssa);
        FormulaType<?> returnFormulaType = conv.getFormulaTypeFromCType(e.getExpressionType());
        return conv.ifTrueThenOneElseZero(returnFormulaType, result);

      } else if (CtoFormulaConverter.UNSUPPORTED_FUNCTIONS.containsKey(functionName)) {
        throw new UnsupportedCCodeException(CtoFormulaConverter.UNSUPPORTED_FUNCTIONS.get(functionName), edge, e);

      } else if (!CtoFormulaConverter.PURE_EXTERNAL_FUNCTIONS.contains(functionName)) {
        if (parameters.isEmpty()) {
          // function of arity 0
          conv.logger.logOnce(Level.INFO, "Assuming external function", functionName, "to be a constant function.");
        } else {
          conv.logger.logOnce(Level.INFO, "Assuming external function", functionName, "to be a pure function.");
        }
      }
    } else {
      conv.logfOnce(Level.WARNING, edge, "Ignoring function call through function pointer %s", functionNameExpression);
      String escapedName = CtoFormulaConverter.scoped(CtoFormulaConverter.exprToVarName(functionNameExpression), function);
      functionName = ("<func>{" + escapedName + "}").intern();
    }

    // Now let's handle "normal" functions assumed to be pure
    if (parameters.isEmpty()) {
      // This is a function of arity 0 and we assume its constant.
      return conv.makeConstant(functionName, returnType);

    } else {
      final CFunctionDeclaration functionDeclaration = e.getDeclaration();
      if (functionDeclaration == null) {
        if (functionNameExpression instanceof CIdExpression) {
          // This happens only if there are undeclared functions.
          conv.logger.logfOnce(Level.WARNING, "Cannot get declaration of function %s, ignoring calls to it.",
                               functionNameExpression);
        }
        return conv.makeFreshVariable(functionName, returnType, ssa); // BUG when expType = void
      }

      if (functionDeclaration.getType().takesVarArgs()) {
        // Create a fresh variable instead of an UF for varargs functions.
        // This is sound but slightly more imprecise (we loose the UF axioms).
        return conv.makeFreshVariable(functionName, returnType, ssa);
      }

      final List<CType> formalParameterTypes = functionDeclaration.getType().getParameters();
      if (formalParameterTypes.size() != parameters.size()) {
        throw new UnrecognizedCCodeException("Function " + functionDeclaration
            + " received " + parameters.size() + " parameters"
            + " instead of the expected " + formalParameterTypes.size(),
            edge, e);
      }

      final List<Formula> arguments = new ArrayList<>(parameters.size());
      final Iterator<CType> formalParameterTypesIt = formalParameterTypes.iterator();
      final Iterator<CExpression> parametersIt = parameters.iterator();
      while (formalParameterTypesIt.hasNext() && parametersIt.hasNext()) {
        final CType formalParameterType = formalParameterTypesIt.next();
        CExpression parameter = parametersIt.next();
        parameter = conv.makeCastFromArrayToPointerIfNecessary(parameter, formalParameterType);

        Formula argument = parameter.accept(this);
        arguments.add(conv.makeCast(parameter.getExpressionType(), formalParameterType, argument, edge));
      }
      assert !formalParameterTypesIt.hasNext() && !parametersIt.hasNext();

      final CType realReturnType = conv.getReturnType(e, edge);
      final FormulaType<?> resultFormulaType = conv.getFormulaTypeFromCType(realReturnType);
      return conv.ffmgr.createFuncAndCall(functionName, resultFormulaType, arguments);
    }
  }
}