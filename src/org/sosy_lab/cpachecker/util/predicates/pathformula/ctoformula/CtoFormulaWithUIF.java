/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Arrays;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaList;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;

import com.google.common.collect.ImmutableList;

class CtoFormulaWithUIF {
  private CtoFormulaWithUIF() {}

  static final String OP_ADDRESSOF_NAME = "__ptrAmp__";
  static final String OP_STAR_NAME = "__ptrStar__";
  static final String OP_ARRAY_SUBSCRIPT = "__array__";
}

class ExpressionToFormulaVisitorUIF extends ExpressionToFormulaVisitor {

  public ExpressionToFormulaVisitorUIF(CtoFormulaConverter pCtoFormulaConverter, CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
    super(pCtoFormulaConverter, pEdge, pFunction, pSsa, pCo);
  }

  private Formula makeUIF(String name, CType type, SSAMapBuilder ssa, Formula... args) {
    FormulaList l = new AbstractFormulaList(args);
    int idx = ssa.getIndex(name, l);
    if (idx <= 0) {
      conv.logger.log(Level.ALL, "DEBUG_3",
          "WARNING: Auto-instantiating lval: ", name, "(", l, ")");
      idx = 1;
      ssa.setIndex(name, l, type, idx);
    }
    return conv.ffmgr.createFuncAndCall(name, idx, conv.getFormulaTypeFromCType(type), Arrays.asList(args));
  }

  @Override
  public Formula visit(CArraySubscriptExpression aexp) throws UnrecognizedCCodeException {
    CExpression arrexp = aexp.getArrayExpression();
    CExpression subexp = aexp.getSubscriptExpression();
    Formula aterm = arrexp.accept(this);
    Formula sterm = subexp.accept(this);

    String ufname = CtoFormulaWithUIF.OP_ARRAY_SUBSCRIPT;
    return makeUIF(ufname, aexp.getExpressionType(), ssa, aterm, sterm);
  }

  @Override
  public Formula visit(CFieldReference fexp) throws UnrecognizedCCodeException {
    String field = fexp.getFieldName();
    CExpression owner = getRealFieldOwner(fexp);
    Formula term = owner.accept(this);

    String tpname = CtoFormulaConverter.getTypeName(owner.getExpressionType());
    String ufname = ".{" + tpname + "," + field + "}";

    // see above for the case of &x and *x
    return makeUIF(ufname, fexp.getExpressionType(), ssa, term);
  }

  @Override
  public Formula visit(CUnaryExpression exp) throws UnrecognizedCCodeException {
    UnaryOperator op = exp.getOperator();
    switch (op) {
    case AMPER:
      String opname = CtoFormulaWithUIF.OP_ADDRESSOF_NAME;

      Formula term = exp.getOperand().accept(this);

      CType expType = exp.getExpressionType();

      // PW make SSA index of * independent from argument
      int idx = conv.getIndex(opname, expType, ssa);
      //int idx = getIndex(
      //    opname, term, ssa, absoluteSSAIndices);

      // build the  function corresponding to this operation.

      return conv.ffmgr.createFuncAndCall(
          opname, idx, conv.getFormulaTypeFromCType(expType), ImmutableList.of(term));

    default:
      return super.visit(exp);
    }
  }

  @Override
  public Formula visit(CPointerExpression exp) throws UnrecognizedCCodeException {

        Formula term = exp.getOperand().accept(this);

        CType expType = exp.getExpressionType();

        // PW make SSA index of * independent from argument
        int idx = conv.getIndex(CtoFormulaWithUIF.OP_STAR_NAME, expType, ssa);
        //int idx = getIndex(
        //    opname, term, ssa, absoluteSSAIndices);

        // build the  function corresponding to this operation.

        return conv.ffmgr.createFuncAndCall(
            CtoFormulaWithUIF.OP_STAR_NAME, idx, conv.getFormulaTypeFromCType(expType), ImmutableList.of(term));
  }
}

class LvalueVisitorUIF extends LvalueVisitor {

  public LvalueVisitorUIF(CtoFormulaConverter pCtoFormulaConverter, CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
    super(pCtoFormulaConverter, pEdge, pFunction, pSsa, pCo);
  }

  @Override
  public Formula visit(CUnaryExpression uExp) throws UnrecognizedCCodeException {
    UnaryOperator op = uExp.getOperator();
    CExpression operand = uExp.getOperand();
    String opname;
    CType opType = operand.getExpressionType();
    CType result;
    switch (op) {
    case AMPER:
      opname = CtoFormulaWithUIF.OP_ADDRESSOF_NAME;
      result = new CPointerType(false, false, opType);
      break;
    default:
      throw new UnrecognizedCCodeException("Invalid unary operator for lvalue", edge, uExp);
    }
    Formula term = conv.buildTerm(operand, edge, function, ssa, constraints);

    FormulaType<?> formulaType = conv.getFormulaTypeFromCType(result);
    // PW make SSA index of * independent from argument
    int idx = conv.makeFreshIndex(opname, result, ssa);
    //int idx = makeLvalIndex(opname, term, ssa, absoluteSSAIndices);

    // build the "updated" function corresponding to this operation.
    // what we do is the following:
    // C            |     MathSAT
    // *x = 1       |     <ptr_*>::2(x) = 1
    // ...
    // &(*x) = 2    |     <ptr_&>::2(<ptr_*>::1(x)) = 2
    return conv.ffmgr.createFuncAndCall(opname, idx, formulaType, ImmutableList.of(term));
  }

  @Override
  public Formula visit(CPointerExpression uExp) throws UnrecognizedCCodeException {
    CExpression operand = uExp.getOperand();
    CType opType = operand.getExpressionType();
    CPointerType opTypeP = (CPointerType)opType;
    CType result = opTypeP.getType();

    Formula term = conv.buildTerm(operand, edge, function, ssa, constraints);

    FormulaType<?> formulaType = conv.getFormulaTypeFromCType(result);
    // PW make SSA index of * independent from argument
    int idx = conv.makeFreshIndex(CtoFormulaWithUIF.OP_STAR_NAME, result, ssa);
    //int idx = makeLvalIndex(opname, term, ssa, absoluteSSAIndices);

    // build the "updated" function corresponding to this operation.
    // what we do is the following:
    // C            |     MathSAT
    // *x = 1       |     <ptr_*>::2(x) = 1
    // ...
    // &(*x) = 2    |     <ptr_&>::2(<ptr_*>::1(x)) = 2
    return conv.ffmgr.createFuncAndCall(CtoFormulaWithUIF.OP_STAR_NAME, idx, formulaType, ImmutableList.of(term));
  }

  @Override
  public Formula visit(CFieldReference fexp) throws UnrecognizedCCodeException {
    if (!conv.handleFieldAccess) {
      String field = fexp.getFieldName();
      CExpression owner = getRealFieldOwner(fexp);
      Formula term = conv.buildTerm(owner, edge, function, ssa, constraints);

      String tpname = CtoFormulaConverter.getTypeName(owner.getExpressionType());
      String ufname = ".{" + tpname + "," + field + "}";
      FormulaList args = new AbstractFormulaList(term);


      CType expType = fexp.getExpressionType();
      FormulaType<?> formulaType = conv.getFormulaTypeFromCType(expType);
      int idx = CtoFormulaConverter.makeLvalIndex(ufname, expType, args, ssa);

      // see above for the case of &x and *x
      return conv.ffmgr.createFuncAndCall(
         ufname, idx, formulaType, ImmutableList.of(term));
    }

    // When handleFieldAccess is true we can handle this case already
    return super.visit(fexp);
  }

  @Override
  public Formula visit(CArraySubscriptExpression aexp) throws UnrecognizedCCodeException {
    CExpression arrexp = aexp.getArrayExpression();
    CExpression subexp = aexp.getSubscriptExpression();
    Formula aterm = conv.buildTerm(arrexp, edge, function, ssa, constraints);
    Formula sterm = conv.buildTerm(subexp, edge, function, ssa, constraints);

    String ufname = CtoFormulaWithUIF.OP_ARRAY_SUBSCRIPT;
    FormulaList args = new AbstractFormulaList(aterm, sterm);
    CType expType = aexp.getExpressionType();
    FormulaType<?> formulaType = conv.getFormulaTypeFromCType(expType);
    int idx = CtoFormulaConverter.makeLvalIndex(ufname, expType, args, ssa);

    return conv.ffmgr.createFuncAndCall(
        ufname, idx, formulaType, ImmutableList.of(aterm, sterm));
  }
}