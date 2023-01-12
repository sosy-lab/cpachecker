// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;
import org.sosy_lab.java_smt.api.StringFormula;

/** Wrapping for models. */
class ModelView implements Model {

  private static final Pattern Z3_IRRELEVANT_MODEL_TERM_PATTERN = Pattern.compile(".*![0-9]+");

  static boolean isRelevantModelTerm(ValueAssignment valueAssignment) {
    return !Z3_IRRELEVANT_MODEL_TERM_PATTERN.matcher(valueAssignment.getName()).matches();
  }

  private final Model delegate;
  private final FormulaWrappingHandler wrappingHandler;

  ModelView(Model pDelegate, FormulaWrappingHandler pWrappingHandler) {
    delegate = pDelegate;
    wrappingHandler = pWrappingHandler;
  }

  @Nullable
  private Object evaluateImpl(Formula f) {
    return delegate.evaluate(wrappingHandler.unwrap(f));
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

  @Nullable
  @Override
  public String evaluate(StringFormula f) {
    return (String) evaluateImpl(f);
  }

  @Override
  @Nullable
  public <T extends Formula> T eval(T f) {
    return wrappingHandler.wrap(
        wrappingHandler.getFormulaType(f), delegate.eval(wrappingHandler.unwrap(f)));
  }

  @Override
  public Iterator<ValueAssignment> iterator() {
    return Iterators.filter(delegate.iterator(), ModelView::isRelevantModelTerm);
  }

  @Override
  public ImmutableList<ValueAssignment> asList() {
    return from(delegate.asList()).filter(ModelView::isRelevantModelTerm).toList();
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
