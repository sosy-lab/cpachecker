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
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.AddExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Addition;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Constant;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.ExpoExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Exponent;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.MultExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Multiplication;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.PolynomExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Variable;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors.EvaluationVisitor;

import java.util.HashMap;
import java.util.OptionalDouble;

/**
 *
 */
public class Polynom {

  private PolynomExpression poly;
  private LogManager logger;

  public Polynom(PolynomExpression pPoly, LogManager log) {
    super();
    poly = pPoly;
    logger = log;
  }

  public Polynom() {}


  /**
   * parses a string to a PolynomExpression
   *
   * @param pInvariant polynom in sympy-representation
   */
  public void fromString(String pInvariant) {
    pInvariant = pInvariant.startsWith("-") ? pInvariant.replaceFirst("-", "(-1) * ") : pInvariant;
    String addition = pInvariant.replace(" -", " + (-1) * ");
    String[] summands = addition.split("\\+");
    PolynomExpression exp = getAddExpression(summands[summands.length - 1]);
    for (int i = summands.length - 2; i >= 0; i--) {
      exp = new Addition(getAddExpression(summands[i]), (AddExpression) exp);
    }

    this.poly = exp;
  }

  private MultExpression getAddExpression(String summand) {
    String[] factors = summand.split("\\*");
    MultExpression exp = getMult(factors[factors.length - 1]);
    for (int i = factors.length - 2; i >= 0; i--) {
      exp = new Multiplication((ExpoExpression) getMult(factors[i]), exp);
    }
    return exp;
  }

  private MultExpression getMult(String factor) {
    MultExpression exp = null;
    factor = factor.replace(" ", "");
    if (factor.matches("[0-9]*") || factor.matches("[0-9]*\\.[0-9]*")) {
      exp = new Constant(Double.parseDouble(factor));
    } else if (factor.matches("[a-zA-Z0-9\\(\\)]*")) {
      exp = new Variable(factor);
    } else if (factor.contains("^")) {
      String[] expo = factor.split("\\^");
      exp = new Exponent(new Variable(expo[0]), getMult(expo[1]));
    } else if (factor.matches("\\(-1\\)")) {
      exp = new Constant(-1);
    }
    return exp;
  }

  public OptionalDouble evalPolynom(HashMap<String, Double> variableValueMap) {
    EvaluationVisitor evalVisitor = new EvaluationVisitor(variableValueMap, logger);
    return poly.accept(evalVisitor);
  }

  public boolean equalToZero(HashMap<String, Double> variableValueMap) {
    OptionalDouble res = evalPolynom(variableValueMap);
    return res.orElse(1) == 0;
  }

  /*
   * Methode zum Testen
   */
  PolynomExpression getPoly() {
    return this.poly;
  }

  @Override
  public String toString() {
    return poly.toString();
  }

}
