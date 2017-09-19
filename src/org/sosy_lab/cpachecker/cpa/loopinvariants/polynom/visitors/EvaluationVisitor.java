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
package org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Addition;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Constant;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Exponent;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Multiplication;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Variable;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors.Visitor.NoException;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.logging.Level;

public class EvaluationVisitor implements Visitor<OptionalDouble, NoException> {

  private Map<String, Double> valueMap;
  private LogManager logger;

  public EvaluationVisitor(Map<String, Double> values, LogManager log) {
    this.valueMap = values;
    this.logger = log;
  }

  @Override
  public OptionalDouble visit(Addition add) throws NoException {
    OptionalDouble s1 = add.getSummand1().accept(this);
    OptionalDouble s2 = add.getSummand2().accept(this);

    if (s1.isPresent()
        && s2.isPresent()) { return OptionalDouble.of(s1.getAsDouble() + s2.getAsDouble()); }
    return OptionalDouble.empty();
  }

  @Override
  public OptionalDouble visit(Multiplication mult) throws NoException {
    OptionalDouble f1 = mult.getFactor1().accept(this);
    OptionalDouble f2 = mult.getFactor2().accept(this);

    if (f1.isPresent()
        && f2.isPresent()) { return OptionalDouble.of(f1.getAsDouble() * f2.getAsDouble()); }
    return OptionalDouble.empty();
  }

  @Override
  public OptionalDouble visit(Exponent exp) throws NoException {
    OptionalDouble exponent = exp.getExponent().accept(this);
    OptionalDouble basis = exp.getBasis().accept(this);

    if (exponent.isPresent() && basis.isPresent()) { return OptionalDouble
        .of(Math.pow(basis.getAsDouble(), exponent.getAsDouble())); }
    return OptionalDouble.empty();
  }

  @Override
  public OptionalDouble visit(Variable var) throws NoException {
    if (valueMap != null && !valueMap.isEmpty() && valueMap.get(var.getIdentifier()) != null) {
      return OptionalDouble.of(valueMap.get(var.getIdentifier()));
    } else {
      logger.log(Level.WARNING, "No value for variable " + var.getIdentifier() + ".");
      return OptionalDouble.empty();
    }
  }

  @Override
  public OptionalDouble visit(Constant con) {
    return OptionalDouble.of(con.getValue());
  }

}
