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
package org.sosy_lab.cpachecker.util.predicates.z3;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.NativeLibraries;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;


/**
 * Tests for the opti-z3 branch.
 */
public class TestZ3Maximization {

  /**
   * Tests only get to run if Z3 can be loaded.
   */
  public boolean canLoadZ3() {
    try {
      NativeLibraries.loadLibrary("z3j");
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  @Test
  public void testMaximization() throws
      InvalidConfigurationException,
      SolverException, InterruptedException {

    Assume.assumeTrue(canLoadZ3());
    Configuration config = Configuration.defaultConfiguration();
    Z3FormulaManager mgr = Z3FormulaManager.create(null, config);
    Z3RationalFormulaManager rfmgr =
        (Z3RationalFormulaManager) mgr.getRationalFormulaManager();

    try (ProverEnvironment prover = new Z3TheoremProver(mgr)) {

      NumeralFormula.RationalFormula x = rfmgr.makeVariable("x");
      NumeralFormula.RationalFormula ten = rfmgr.makeNumber("10");

      // Assert x <= 10.
      BooleanFormula f = rfmgr.lessOrEquals(x, ten);
      prover.push(f);

      // Maximize for x.
      ProverEnvironment.OptResult response = prover.isOpt(x, true);

      Assert.assertEquals(response, ProverEnvironment.OptResult.OPT);

      // Check the value.
      Model model = prover.getModel();

      ExtendedRational value = (ExtendedRational) model.get(new Model.Constant("x", Model.TermType.Real));

      Assert.assertEquals(value, ExtendedRational.ofString("10"));
    }
  }

}
