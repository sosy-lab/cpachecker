// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.java_smt.api.BooleanFormula;

public interface WellFoundednessChecker {

  /**
   * A method checking whether a formula represents a well-founded relation.
   *
   * @param pFormula representing a relation
   * @param pSupportingInvariants that can strengthen the transition invariant
   * @return true if the formula is well-founded, return false otherwise
   */
  boolean isWellFounded(
      BooleanFormula pFormula,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      Loop pLoop,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapPrevVarsToCurrVars)
      throws InterruptedException, CPAException;

  /**
   * A method checking whether a formula represents a disjunctively well-founded relation.
   *
   * @param pFormula representing a relation
   * @param pSupportingInvariants that can strengthen the transition invariant
   * @return true if the formula is disjunctively well-founded, return false otherwise
   */
  boolean isDisjunctivelyWellFounded(
      BooleanFormula pFormula,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      Loop pLoop,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapPrevVarsToCurrVars)
      throws InterruptedException, CPAException;
}
