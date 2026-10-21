// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public record ArrayInitialization(
    CFANode loopHead,
    AcslTerm array,
    AcslIdTerm index,
    AcslTerm lowerbound,
    AcslTerm upperbound,
    Boolean upperboundInclusive,
    AcslTerm value)
    implements AcslInvariantProvider {

  @SuppressWarnings("unused")
  @Override
  public AcslPredicate toInvariant() {
    // I think I need the program scope to get the C objects for the array or at lest the index
    // TODO fill all nulls below with correct values
    AcslCExpression arrayAtIndex = null;
    AcslParameterDeclaration indexDecl = null;
    CVariableDeclaration ci = null;
    AcslIdTerm indexformDecl = null;

    FileLocation loc = FileLocation.DUMMY;
    AcslBinaryTermExpressionOperator upperBoundOp =
        upperboundInclusive
            ? AcslBinaryTermExpressionOperator.LESS_EQUAL
            : AcslBinaryTermExpressionOperator.LESS_THAN;

    AcslPredicate boundsPred =
        new AcslBinaryPredicate(
            loc,
            new AcslBinaryTermPredicate(
                loc, lowerbound, null, AcslBinaryTermExpressionOperator.LESS_EQUAL),
            new AcslBinaryTermPredicate(loc, null, upperbound, upperBoundOp),
            AcslBinaryPredicateOperator.AND);
    AcslPredicate arrayPred =
        new AcslBinaryTermPredicate(
            loc, arrayAtIndex, value, AcslBinaryTermExpressionOperator.EQUALS);

    AcslPredicate body =
        new AcslBinaryPredicate(
            loc, boundsPred, arrayPred, AcslBinaryPredicateOperator.IMPLICATION);

    return new AcslForallPredicate(loc, ImmutableList.of(), body); // TODO fill list with params
  }
}
