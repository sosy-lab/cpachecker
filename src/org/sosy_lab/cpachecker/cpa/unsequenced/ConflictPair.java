// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.cpa.unsequenced;

import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * A pair of side‐effect accesses that conflict due to unsequenced memory operations.
 *
 * <p>Use {@link #of} to create an ordered pair so that (a,b) and (b,a) produce the same object.
 *
 * @param accessA first side‐effect (lexicographically ≤ accessB)
 * @param accessB second side‐effect (lexicographically ≥ accessA)
 * @param location CFA edge where the conflict was found
 * @param exprA AST node for accessA
 * @param exprB AST node for accessB
 */
public record ConflictPair(
    SideEffectInfo accessA,
    SideEffectInfo accessB,
    CFAEdge location,
    CRightHandSide exprA,
    CRightHandSide exprB) {

  // Static factory method to ensure (a, b) == (b, a)
  public static ConflictPair of(
      SideEffectInfo accessA,
      SideEffectInfo accessB,
      CFAEdge location,
      CRightHandSide exprA,
      CRightHandSide exprB) {
    if (accessA.toString().compareTo(accessB.toString()) > 0) {
      return new ConflictPair(accessB, accessA, location, exprB, exprA);
    } else {
      return new ConflictPair(accessA, accessB, location, exprA, exprB);
    }
  }
}
