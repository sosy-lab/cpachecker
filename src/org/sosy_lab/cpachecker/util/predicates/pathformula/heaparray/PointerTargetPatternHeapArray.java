/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.heaparray;

import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetPattern;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetManager;

/**
 * A {@code PointerTargetPattern} specifically for the heap modelling package using the SMT
 * theory of arrays.
 *
 * @see PointerTargetPattern
 */
public class PointerTargetPatternHeapArray extends PointerTargetPattern {

  private static final long serialVersionUID = -2136863379558076666L;

  /**
   * The class should only be instantiated by the methods
   * {@link #any()} or {@link #forBase(String)}, hence we have the constructor private.
   */
  private PointerTargetPatternHeapArray() {
    super();
  }

  /**
   * The class should only be instantiated by the methods
   * {@link #any()} or {@link #forBase(String)}, hence we have the constructor private.
   *
   * @param pBase The name of the base to create a {@code PointerTargetPattern} for.
   */
  private PointerTargetPatternHeapArray(String pBase) {
    super(pBase);
  }

  /**
   * Create a {@code PointerTargetPattern} that matches any possible target.
   */
  public static PointerTargetPattern any() {
    return new PointerTargetPatternHeapArray();
  }

  /**
   * Create a {@code PointerTargetPattern} in the memory block with the specified base name and
   * offset {@code 0}.
   *
   * @param pBase The specified base name.
   * @return A {@code PointerTargetPattern} in the memory.
   */
  public static PointerTargetPattern forBase(final String pBase) {
    return new PointerTargetPatternHeapArray(pBase);
  }

  /**
   * Create a {@code PointerTargetPattern} in the memory block for a specific left hand side of a
   * C expression.
   *
   * @param pLeftHandSide The C expression on the left hand side.
   * @param pTypeHandler The type handler for C formulas.
   * @param pPointerTargetSetManager The manager for all pointer target sets.
   * @param pCFAEdge The current edge of the CFA.
   * @param pPointerTargetSetBuilder The builder for new pointer target sets.
   * @return A {@code PointerTargetPattern} for a specific left hand side.
   * @throws UnrecognizedCCodeException If an expression was unrecognizable.
   */
  public static PointerTargetPattern forLeftHandSide(
      final CLeftHandSide pLeftHandSide,
      final CtoFormulaTypeHandler pTypeHandler,
      final PointerTargetSetManager pPointerTargetSetManager,
      final CFAEdge pCFAEdge,
      final PointerTargetSetBuilder pPointerTargetSetBuilder)
      throws UnrecognizedCCodeException {

    LvalueToPointerTargetPatternHeapArrayVisitor v =
        new LvalueToPointerTargetPatternHeapArrayVisitor(
            pTypeHandler, pPointerTargetSetManager, pCFAEdge, pPointerTargetSetBuilder);

    return pLeftHandSide.accept(v);
  }
}
