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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of this class are invariants formula visitors used to replace
 * parts of the visited formulae with other formulae.
 *
 * @param <T> the type of the constants used in the formulae.
 */
public class ReplaceVisitor<T> extends RecursiveDefaultFormulaVisitor<T> {

  /**
   * The formula to be replaced.
   */
  private final InvariantsFormula<T> toReplace;

  /**
   * The replacement formula.
   */
  private final InvariantsFormula<T> replacement;

  /**
   * Creates a new replace visitor for replacing occurrences of the first given
   * formula by the second given formula in visited formulae.
   *
   * @param pToReplace the formula to be replaced.
   * @param pReplacement the replacement formula.
   */
  public ReplaceVisitor(InvariantsFormula<T> pToReplace,
      InvariantsFormula<T> pReplacement) {
    this.toReplace = pToReplace;
    this.replacement = pReplacement;
  }

  @Override
  protected InvariantsFormula<T> visitPost(InvariantsFormula<T> pFormula) {
    if (pFormula.equals(this.toReplace)) {
      return replacement;
    }
    return pFormula;
  }

}
