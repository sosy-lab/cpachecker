/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopinvariants;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Addition;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Constant;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Multiplication;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Variable;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors.PrintVisitor;

import java.util.List;
import java.util.logging.Level;

public class PolynomTest {

  private static LogManager logger = LogManager.createTestLogManager();

  public static void main(String[] args) {

    test2();

  }

  public static void test2() {
    logger.log(Level.INFO, "\n \n Test2: ");

    PrintVisitor printer = new PrintVisitor();
    Polynom polynom1 = new Polynom(new Addition(new Variable("x(n)"), new Addition(new Constant(5),
        new Multiplication(new Constant(-1), new Variable("x(n+1)")))), logger);
    //x(n) + 5 - x(n+1)



    Polynom polynom3 = new Polynom(new Addition(new Multiplication(new Variable("z(n)"), new Constant(6)),
        new Multiplication(new Constant(-1), new Variable("z(n+1)"))), logger);
    //z(n)*6 - z(n+1)

  Polynom polynom2 = new Polynom(new Addition(new Multiplication(new Variable("y(n)"), new Constant(3)),
  new Multiplication(new Constant(-1), new Variable("y(n+1)"))), logger);
// y(n)*3 - y(n+1)

    LoopInvariantsState state = new LoopInvariantsState();
    state.addPolynom(polynom1.getPoly());
    state.addPolynom(polynom3.getPoly());
    state.addPolynom(polynom2.getPoly());

    state.addVariableValue("x", 2);

    logger.log(Level.INFO, "\n CalculationHelper:");

    try {
      CalculationHelper.calculateGroebnerBasis(state, logger);
    } catch (Exception e) {
      logger.logException(Level.SEVERE, e, "Fehler beim Berechnen");
    }

    List<Polynom> polynomList = state.getInvariant();
    for (Polynom invariant : polynomList) {
      try {
        logger.log(Level.INFO, invariant.getPoly().accept(printer));
      } catch (Exception e) {
        logger.logException(Level.INFO, e, "Fehler beim Ausgeben");
      }
    }
  }

}
