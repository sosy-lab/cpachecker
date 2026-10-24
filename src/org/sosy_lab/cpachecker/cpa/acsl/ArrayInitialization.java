// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula.AcslRemoveSsaIndexVisitor;

public record ArrayInitialization(
    CFANode loopHead,
    AcslTerm array,
    AcslIdTerm index,
    AcslTerm lowerbound,
    AcslTerm upperbound,
    Boolean upperboundInclusive,
    AcslTerm value)
    implements AcslInvariantProvider {

  @Override
  public AcslPredicate toInvariant(CProgramScope pScope, FileLocation pLoc, LogManager pLogger) {
    // I think I need the program scope to get the C objects for the array

    // Clean SSA Indices with this, if needed:
    // TODO decide if I want to remove SSA index from upper and lower bounds or if that is needed
    // info...
    AcslRemoveSsaIndexVisitor ssaRemover = new AcslRemoveSsaIndexVisitor();

    CVariableDeclaration ci =
        new CVariableDeclaration(
            pLoc,
            true,
            CStorageClass.AUTO,
            basicInt(),
            "__ACSL_inv_quantifier_index",
            "__ACSL_inv_quantifier_index",
            "__ACSL_inv_quantifier_index",
            null); // TODO do I need that?
    AcslParameterDeclaration paramiDecl =
        new AcslParameterDeclaration(
            pLoc, new AcslCType(basicInt()), "__ACSL_inv_quantifier_index");
    AcslIdTerm paramiTerm = new AcslIdTerm(pLoc, paramiDecl);

    String arrayName = ((AcslIdTerm) array.accept(ssaRemover)).getName();
    CSimpleDeclaration arrayDecl = pScope.lookupVariable(arrayName);
    if (arrayDecl == null) {
      pLogger.log(
          Level.WARNING,
          "Could not create ACSL invariant, because array does not exist in CProgram Scope.");
      return null;
    }

    AcslCExpression arrayAtIndex =
        new AcslCExpression(
            pLoc,
            new CArraySubscriptExpression(
                pLoc, basicInt(), new CIdExpression(pLoc, arrayDecl), new CIdExpression(pLoc, ci)));

    AcslBinaryTermExpressionOperator upperBoundOp =
        upperboundInclusive
            ? AcslBinaryTermExpressionOperator.LESS_EQUAL
            : AcslBinaryTermExpressionOperator.LESS_THAN;

    AcslPredicate boundsPred =
        new AcslBinaryPredicate(
            pLoc,
            new AcslBinaryTermPredicate(
                pLoc, lowerbound, paramiTerm, AcslBinaryTermExpressionOperator.LESS_EQUAL),
            new AcslBinaryTermPredicate(pLoc, paramiTerm, upperbound, upperBoundOp),
            AcslBinaryPredicateOperator.AND);
    AcslPredicate arrayPred =
        new AcslBinaryTermPredicate(
            pLoc, arrayAtIndex, value, AcslBinaryTermExpressionOperator.EQUALS);

    AcslPredicate body =
        new AcslBinaryPredicate(
            pLoc, boundsPred, arrayPred, AcslBinaryPredicateOperator.IMPLICATION);

    return new AcslForallPredicate(pLoc, ImmutableList.of(paramiDecl), body);
  }

  private CSimpleType basicInt() {
    return new CSimpleType(
        CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);
  }
}
