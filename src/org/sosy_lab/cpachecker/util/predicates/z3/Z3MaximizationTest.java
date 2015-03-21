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

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.util.NativeLibraries;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment.OptStatus;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.collect.ImmutableList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * Tests for the maximization using the Z3 opt branch.
 */
@SuppressWarnings("unused")
@SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
public class Z3MaximizationTest {

  private Z3FormulaManager mgr;
  private Z3RationalFormulaManager rfmgr;
  private Z3IntegerFormulaManager ifmgr;
  private Z3BooleanFormulaManager bfmgr;

  @Before
  public void loadZ3() throws Exception {
    NativeLibraries.loadLibrary("z3j");
    Configuration config = Configuration.defaultConfiguration();
    LogManager logger = TestLogManager.getInstance();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.create();
    mgr = Z3FormulaManager.create(logger, config, shutdownNotifier, null, 42);
    rfmgr = (Z3RationalFormulaManager) mgr.getRationalFormulaManager();
    ifmgr = (Z3IntegerFormulaManager) mgr.getIntegerFormulaManager();
    bfmgr = (Z3BooleanFormulaManager) mgr.getBooleanFormulaManager();
  }

  @Test public void testUnbounded() throws Exception {
    try (OptEnvironment prover = mgr.newOptEnvironment()) {
      RationalFormula x, obj;
      x = rfmgr.makeVariable("x");
      obj = rfmgr.makeVariable("obj");
      List<BooleanFormula> constraints = ImmutableList.of(
        rfmgr.greaterOrEquals(x, rfmgr.makeNumber("10")),
        rfmgr.equal(x, obj)
      );
      prover.addConstraint(bfmgr.and(constraints));
      int handle = prover.maximize(obj);
      @SuppressWarnings("unused")
      OptEnvironment.OptStatus response = prover.check();
      Assert.assertTrue(!prover.upper(handle, Rational.ZERO).isPresent());
    }
  }

  @Test public void testUnfeasible() throws Exception {
    try (OptEnvironment prover = mgr.newOptEnvironment()) {
      RationalFormula x, y;
      x = rfmgr.makeVariable("x");
      y = rfmgr.makeVariable("y");
      List<BooleanFormula> constraints = ImmutableList.of(
          rfmgr.lessThan(x, y),
          rfmgr.greaterThan(x, y)
      );
      prover.addConstraint(bfmgr.and(constraints));
      @SuppressWarnings("unused")
      int handle = prover.maximize(x);
      OptEnvironment.OptStatus response = prover.check();
      Assert.assertEquals(OptEnvironment.OptStatus.UNSAT,
          response);
    }
  }

  @Test public void testOptimal() throws Exception {
    try (OptEnvironment prover = mgr.newOptEnvironment()) {

      RationalFormula x, y, obj;
      x = rfmgr.makeVariable("x");
      y = rfmgr.makeVariable("y");
      obj = rfmgr.makeVariable("obj");

      /*
        real x, y, obj
        x <= 10
        y <= 15
        obj = x + y
        x - y >= 1
       */
      List<BooleanFormula> constraints = ImmutableList.of(
          rfmgr.lessOrEquals(x, rfmgr.makeNumber(10)),
          rfmgr.lessOrEquals(y, rfmgr.makeNumber(15)),
          rfmgr.equal(obj, rfmgr.add(x, y)),
          rfmgr.greaterOrEquals(rfmgr.subtract(x, y), rfmgr.makeNumber(1))
      );

      prover.addConstraint(bfmgr.and(constraints));
      int handle = prover.maximize(obj);

      // Maximize for x.
      OptEnvironment.OptStatus response = prover.check();

      Assert.assertEquals(OptEnvironment.OptStatus.OPT, response);

      Model model = prover.getModel();
      Assert.assertEquals("obj : Real: 19\nx : Real: 10\ny : Real: 9",
          model.toString());

      // Check the value.
      Assert.assertEquals(Rational.ofString("19"), prover.upper(handle, Rational.ZERO).get());
    }
  }

  @Test public void testNonlinearity() throws Exception {
    try (OptEnvironment prover = mgr.newOptEnvironment()) {
      NumeralFormula.IntegerFormula x, y, z, one;
      x = ifmgr.makeVariable("x");
      y = ifmgr.makeVariable("y");
      one = ifmgr.makeNumber(2);
      prover.addConstraint(ifmgr.lessOrEquals(x, ifmgr.makeNumber(10)));
      prover.addConstraint(ifmgr.lessOrEquals(y, ifmgr.makeNumber(10)));

      z = ifmgr.divide(x, one);

      int handle = prover.maximize(z);
      OptEnvironment.OptStatus response = prover.check();
    }
  }

  @Test public void testSwitchingObjectives() throws Exception {
    try (OptEnvironment prover = mgr.newOptEnvironment()) {
      RationalFormula x, y, obj;
      x = rfmgr.makeVariable("x");
      y = rfmgr.makeVariable("y");
      obj = rfmgr.makeVariable("obj");

      /*
        real x, y, obj
        x <= 10
        y <= 15
        obj = x + y
        x - y >= 1
       */
      List<BooleanFormula> constraints = ImmutableList.of(
          rfmgr.lessOrEquals(x, rfmgr.makeNumber(10)),
          rfmgr.lessOrEquals(y, rfmgr.makeNumber(15)),
          rfmgr.equal(obj, rfmgr.add(x, y)),
          rfmgr.greaterOrEquals(rfmgr.subtract(x, y), rfmgr.makeNumber(1))
      );
      prover.addConstraint(bfmgr.and(constraints));
      OptStatus response;

      prover.push();

      int handle = prover.maximize(obj);
      response = prover.check();
      assertThat(response).isEqualTo(OptStatus.OPT);
      assertThat(prover.upper(handle, Rational.ZERO).get()).isEqualTo(Rational.ofString("19"));

      prover.pop();
      prover.push();

      handle = prover.maximize(x);
      response = prover.check();
      assertThat(response).isEqualTo(OptStatus.OPT);
      assertThat(prover.upper(handle, Rational.ZERO).get()).isEqualTo(Rational.ofString("10"));

      prover.pop();
      prover.push();

      handle = prover.maximize(rfmgr.makeVariable("y"));
      response = prover.check();
      assertThat(response).isEqualTo(OptStatus.OPT);
      assertThat(prover.upper(handle, Rational.ZERO).get()).isEqualTo(Rational.ofString("9"));

      prover.pop();
    }
  }

}
