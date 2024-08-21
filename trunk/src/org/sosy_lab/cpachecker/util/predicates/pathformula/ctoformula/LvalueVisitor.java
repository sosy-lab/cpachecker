// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeUtils.getRealFieldOwner;

import java.util.Optional;
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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.Formula;

class LvalueVisitor extends DefaultCExpressionVisitor<Formula, UnrecognizedCodeException> {

  private final CtoFormulaConverter conv;
  private final CFAEdge edge;
  private final String function;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;

  LvalueVisitor(
      CtoFormulaConverter pConv,
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
  protected BitvectorFormula visitDefault(CExpression exp) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("Unknown lvalue", edge, exp);
  }

  @Override
  public Formula visit(CIdExpression idExp) {
    return conv.makeFreshVariable(
        idExp.getDeclaration().getQualifiedName(), idExp.getExpressionType(), ssa);
  }

  /** This method is called when we don't know what else to do. */
  private Formula giveUpAndJustMakeVariable(CExpression exp) {
    return conv.makeVariableUnsafe(exp, function, ssa, true);
  }

  @Override
  public Formula visit(CUnaryExpression pE) throws UnrecognizedCodeException {
    return giveUpAndJustMakeVariable(pE);
  }

  @Override
  public Formula visit(CComplexCastExpression pE) throws UnrecognizedCodeException {
    if (pE.isImaginaryCast()) {
      throw new UnrecognizedCodeException("Unknown lvalue", edge, pE);
    }
    // TODO complex numbers are not supported for evaluation right now
    return giveUpAndJustMakeVariable(pE);
  }

  @Override
  public Formula visit(CPointerExpression pE) throws UnrecognizedCodeException {
    return giveUpAndJustMakeVariable(pE);
  }

  @Override
  public Formula visit(CFieldReference fexp) throws UnrecognizedCodeException {
    if (!conv.options.handleFieldAccess()) {
      CExpression fieldRef = fexp.getFieldOwner();
      if (fieldRef instanceof CIdExpression) {
        CSimpleDeclaration decl = ((CIdExpression) fieldRef).getDeclaration();
        if (decl instanceof CDeclaration && ((CDeclaration) decl).isGlobal()) {
          // this is the reference to a global field variable
          // we don't need to scope the variable reference
          String var = CtoFormulaConverter.exprToVarNameUnscoped(fexp);

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
    Formula oldStructure =
        conv.buildTerm(owner, edge, function, ssa, pts, constraints, errorConditions);
    // This will eventually increment the ssa-index and return the new formula.
    Formula newStructure = owner.accept(this);

    // Other fields did not change.
    Formula oldRestS = conv.replaceField(fexp, oldStructure, Optional.empty());
    Formula newRestS = conv.replaceField(fexp, newStructure, Optional.empty());
    constraints.addConstraint(conv.fmgr.makeEqual(oldRestS, newRestS));

    Formula fieldFormula = conv.accessField(fexp, newStructure);
    return fieldFormula;
  }

  @Override
  public Formula visit(CArraySubscriptExpression pE) throws UnrecognizedCodeException {
    return giveUpAndJustMakeVariable(pE);
  }
}
