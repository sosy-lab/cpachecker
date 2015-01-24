/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.predicates.z3;


import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.NativeLibraries;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;

@SuppressWarnings("unused")
public class Z3InterpolationTest {
  private Z3FormulaManager mgr;
  private Z3IntegerFormulaManager ifmgr;

  @Before
  public void loadZ3() throws Exception {
    NativeLibraries.loadLibrary("z3j");
    Configuration config = Configuration.defaultConfiguration();
    LogManager logger = TestLogManager.getInstance();
    mgr = Z3FormulaManager.create(logger, config, ShutdownNotifier.create(), null);
    ifmgr = (Z3IntegerFormulaManager) mgr.getIntegerFormulaManager();
  }

  @Test public void testInterpolation() throws Exception {
    try (Z3InterpolatingProver prover = new Z3InterpolatingProver(mgr)) {
      IntegerFormula x, y, z;
      x = ifmgr.makeVariable("x");
      y = ifmgr.makeVariable("y");
      z = ifmgr.makeVariable("z");
      prover.push(ifmgr.equal(
          y,
          ifmgr.multiply(ifmgr.makeNumber(2), x)
      ));
      Z3Formula formula2 = (Z3Formula) ifmgr.equal(
              y,
              ifmgr.add(
                  ifmgr.makeNumber(1),
                  ifmgr.multiply(z, ifmgr.makeNumber(2))
              )
          );
      BooleanFormula interpolant = prover.getInterpolant(
          Collections.singletonList(formula2.getFormulaInfo()));
    }
  }
}
