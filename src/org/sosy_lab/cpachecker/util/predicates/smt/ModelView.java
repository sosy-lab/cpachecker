/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.predicates.smt;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Wrapping for models.
 */
class ModelView implements Model {

  private static final Pattern Z3_IRRELEVANT_MODEL_TERM_PATTERN = Pattern.compile(".*![0-9]+");
  static final Predicate<ValueAssignment> FILTER_MODEL_TERM =
      valueAssignment ->
          !Z3_IRRELEVANT_MODEL_TERM_PATTERN.matcher(valueAssignment.getName()).matches();

  private final Model delegate;
  private final FormulaWrappingHandler wrappingHandler;

  ModelView(Model pDelegate, FormulaWrappingHandler pWrappingHandler) {
    delegate = pDelegate;
    wrappingHandler = pWrappingHandler;
  }

  @Nullable
  private Object evaluateImpl(Formula f) {
    return delegate.evaluate(
        wrappingHandler.unwrap(f)
    );
  }

  @Nullable
  @Override
  public Object evaluate(Formula f) {
    return evaluateImpl(f);
  }

  @Nullable
  @Override
  public BigInteger evaluate(IntegerFormula f) {
    return (BigInteger) evaluateImpl(f);
  }

  @Nullable
  @Override
  public Rational evaluate(RationalFormula f) {
    return (Rational) evaluateImpl(f);
  }

  @Nullable
  @Override
  public Boolean evaluate(BooleanFormula f) {
    return (Boolean) evaluateImpl(f);
  }

  @Nullable
  @Override
  public BigInteger evaluate(BitvectorFormula f) {
    return (BigInteger) evaluateImpl(f);
  }

  @Override
  public Iterator<ValueAssignment> iterator() {
    return Iterators.filter(delegate.iterator(), FILTER_MODEL_TERM);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public void close() {
    delegate.close();
  }
}
