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
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

public record ArrayCopy(
    CFANode loopHead,
    AcslTerm sourceArray,
    AcslTerm destinationArray,
    AcslIdTerm index,
    AcslTerm lowerbound)
    implements AcslInvariantProvider {

  /**
   * Builds and returns ACSL invariant from record: \forall signed int j; (lowerbound <= j && j <
   * index) ==> destinationArray[j] == sourceArray[j];
   */
  @SuppressWarnings("unused")
  @Override
  public AcslPredicate toInvariant(CProgramScope pScope, FileLocation pLoc, LogManager pLogger) {
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
            null);
    AcslParameterDeclaration paramiDecl =
        new AcslParameterDeclaration(
            pLoc, new AcslCType(basicInt()), "__ACSL_inv_quantifier_index");
    AcslIdTerm paramiTerm = new AcslIdTerm(pLoc, paramiDecl);

    String destArrayName = ((AcslIdTerm) destinationArray.accept(ssaRemover)).getName();
    CSimpleDeclaration destArrayDecl = pScope.lookupVariable(destArrayName);
    String sourceArrayName = ((AcslIdTerm) sourceArray.accept(ssaRemover)).getName();
    CSimpleDeclaration sourceArrayDecl = pScope.lookupVariable(sourceArrayName);
    if (sourceArrayDecl == null || destArrayDecl == null) {
      pLogger.log(
          Level.WARNING,
          "Could not create ACSL invariant, because an array does not exist in CProgram Scope.");
      return null;
    }
    CType destArrayElementType = ((CArrayType) destArrayDecl.getType()).getType();
    CType sourceArrayElementType = ((CArrayType) sourceArrayDecl.getType()).getType();

    AcslCExpression destArrayAtIndex =
        new AcslCExpression(
            pLoc,
            new CArraySubscriptExpression(
                pLoc,
                destArrayElementType,
                new CIdExpression(pLoc, destArrayDecl),
                new CIdExpression(pLoc, ci)));

    AcslCExpression sourceArrayAtIndex =
        new AcslCExpression(
            pLoc,
            new CArraySubscriptExpression(
                pLoc,
                sourceArrayElementType,
                new CIdExpression(pLoc, sourceArrayDecl),
                new CIdExpression(pLoc, ci)));

    AcslPredicate boundsPred =
        new AcslBinaryPredicate(
            pLoc,
            new AcslBinaryTermPredicate(
                pLoc,
                lowerbound.accept(ssaRemover),
                paramiTerm,
                AcslBinaryTermExpressionOperator.LESS_EQUAL),
            new AcslBinaryTermPredicate(
                pLoc,
                paramiTerm,
                index.accept(ssaRemover),
                AcslBinaryTermExpressionOperator.LESS_THAN),
            AcslBinaryPredicateOperator.AND);
    AcslPredicate arrayPred =
        new AcslBinaryTermPredicate(
            pLoc, destArrayAtIndex, sourceArrayAtIndex, AcslBinaryTermExpressionOperator.EQUALS);

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
