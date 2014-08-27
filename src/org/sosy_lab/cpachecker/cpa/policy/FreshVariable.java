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
package org.sosy_lab.cpachecker.cpa.policy;

import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;

/**
 * Class for dealing with fresh variables.
 */
public class FreshVariable {
  public static final String FRESH_VAR_PREFIX = "POLICY_ITERATION_FRESH_VAR_%d";

  // NOTE: hm what if it overflows?
  // wouldn't it be safer to use a BigInteger here?
  private static long fresh_var_counter = 0;

  final long no;
  final NumeralFormula variable;

  private FreshVariable(long no, NumeralFormula variable) {
    this.no = no;
    this.variable = variable;
  }

  String name() {
    return name(no);
  }

  static String name(long counter) {
    return String.format(FRESH_VAR_PREFIX, counter);
  }

  /**
   * @return Unique fresh variable created using a global counter.
   */
  static FreshVariable createFreshVar(
      NumeralFormulaManager<NumeralFormula, NumeralFormula.RationalFormula> rfmgr
  ) {
    FreshVariable out = new FreshVariable(
        fresh_var_counter,
        rfmgr.makeVariable(FreshVariable.name(fresh_var_counter))
    );
    fresh_var_counter++;
    return out;
  }
}
