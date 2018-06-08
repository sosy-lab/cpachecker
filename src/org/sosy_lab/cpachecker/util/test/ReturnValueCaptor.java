/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public final class ReturnValueCaptor<R> {
  private final List<R> returnValues = new ArrayList<>();

  /**
   * Get a captured return value.
   *
   * @param pTimeCalled Pass <code>0</code> to get the return value of the first call to the wrapped
   *     function, <code>1</code> to get the return value of the second call to the wrapped
   *     function, etc,
   * @return The captured return value of the nth-call to the wrapped function.
   */
  public R getReturnValue(final int pTimeCalled) {
    return returnValues.get(pTimeCalled);
  }

  public int getTimesCalled() {
    return returnValues.size();
  }

  public <T, U> BiFunction<T, U, R> captureReturn(@Nonnull final BiFunction<T, U, R> pFunction) {
    return captureReturnOfBiFunction(pFunction);
  }

  public <T> Function<T, R> captureReturn(@Nonnull final Function<T, R> pFunction) {
    return captureReturnOfFunction(pFunction);
  }

  public Supplier<R> captureReturn(@Nonnull final Supplier<R> pFunction) {
    return captureReturnOfSupplier(pFunction);
  }

  @SuppressWarnings("WeakerAccess")
  public <T, U> BiFunction<T, U, R> captureReturnOfBiFunction(@Nonnull final BiFunction<T, U, R> pFunction) {
    return (final T pFirst, final U pSecond) -> {
      final R returnValue = pFunction.apply(pFirst, pSecond);
      returnValues.add(returnValue);
      return returnValue;
    };
  }

  public BinaryOperator<R> captureReturnOfBinaryOperator(@Nonnull final BinaryOperator<R> pFunction) {
    return (final R pFirst, final R pSecond) -> {
      final R returnValue = pFunction.apply(pFirst, pSecond);
      returnValues.add(returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("WeakerAccess")
  public <T> Function<T, R> captureReturnOfFunction(@Nonnull final Function<T, R> pFunction) {
    return (final T pFirst) -> {
      final R returnValue = pFunction.apply(pFirst);
      returnValues.add(returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("WeakerAccess")
  public Supplier<R> captureReturnOfSupplier(@Nonnull final Supplier<R> pFunction) {
    return () -> {
      final R returnValue = pFunction.get();
      returnValues.add(returnValue);
      return returnValue;
    };
  }

  public IntFunction<R> captureReturnOfIntFunction(@Nonnull final IntFunction<R> pFunction) {
    return (final int pParameter) -> {
      final R returnValue = pFunction.apply(pParameter);
      returnValues.add(returnValue);
      return returnValue;
    };
  }

  public LongFunction<R> captureReturnOfLongFunction(@Nonnull final LongFunction<R> pFunction) {
    return (final long pParameter) -> {
      final R returnValue = pFunction.apply(pParameter);
      returnValues.add(returnValue);
      return returnValue;
    };
  }

  public DoubleFunction<R> captureReturnOfDoubleFunction(@Nonnull final DoubleFunction<R> pFunction) {
    return (final double pParameter) -> {
      final R returnValue = pFunction.apply(pParameter);
      returnValues.add(returnValue);
      return returnValue;
    };
  }

  public UnaryOperator<R> captureReturnOfUnaryOperator(@Nonnull final UnaryOperator<R> pFunction) {
    return (final R pOperand) -> {
      final R returnValue = pFunction.apply(pOperand);
      returnValues.add(returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public IntUnaryOperator captureReturnOfIntUnaryOperator(@Nonnull final IntUnaryOperator pFunction) {
    return (final int pOperand) -> {
      final Integer returnValue = pFunction.applyAsInt(pOperand);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public LongUnaryOperator captureReturnOfLongUnaryOperator(@Nonnull final LongUnaryOperator pFunction) {
    return (final long pOperand) -> {
      final Long returnValue = pFunction.applyAsLong(pOperand);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public DoubleUnaryOperator captureReturnOfDoubleUnaryOperator(@Nonnull final DoubleUnaryOperator pFunction) {
    return (final double pOperand) -> {
      final Double returnValue = pFunction.applyAsDouble(pOperand);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public <T> ToIntFunction<T> captureReturnOfToIntFunction(@Nonnull final ToIntFunction<T> pFunction) {
    return (final T pParameter) -> {
      final Integer returnValue = pFunction.applyAsInt(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public <T> ToLongFunction<T> captureReturnOfToLongFunction(@Nonnull final ToLongFunction<T> pFunction) {
    return (final T pParameter) -> {
      final Long returnValue = pFunction.applyAsLong(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public <T> ToDoubleFunction<T> captureReturnOfToDoubleFunction(@Nonnull final ToDoubleFunction<T> pFunction) {
    return (final T pParameter) -> {
      final Double returnValue = pFunction.applyAsDouble(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public <T, U> ToIntBiFunction<T, U> captureReturnOfToIntBiFunction(
      @Nonnull final ToIntBiFunction<T, U> pFunction) {
    return (final T pFirst, final U pSecond) -> {
      final Integer returnValue = pFunction.applyAsInt(pFirst, pSecond);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public <T, U> ToLongBiFunction<T, U> captureReturnOfToLongBiFunction(
      @Nonnull final ToLongBiFunction<T, U> pFunction) {
    return (final T pFirst, final U pSecond) -> {
      final Long returnValue = pFunction.applyAsLong(pFirst, pSecond);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public <T, U> ToDoubleBiFunction<T, U> captureReturnOfToDoubleBiFunction(
      @Nonnull final ToDoubleBiFunction<T, U> pFunction) {
    return (final T pFirst, final U pSecond) -> {
      final Double returnValue = pFunction.applyAsDouble(pFirst, pSecond);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public BooleanSupplier captureReturnOfBooleanSupplier(@Nonnull final BooleanSupplier pFunction) {
    return () -> {
      final Boolean returnValue = pFunction.getAsBoolean();
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public IntSupplier captureReturnOfIntSupplier(@Nonnull final IntSupplier pFunction) {
    return () -> {
      final Integer returnValue = pFunction.getAsInt();
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public DoubleSupplier captureReturnOfDoubleSupplier(@Nonnull final DoubleSupplier pFunction) {
    return () -> {
      final Double returnValue = pFunction.getAsDouble();
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public LongSupplier captureReturnOfLongSupplier(@Nonnull final LongSupplier pFunction) {
    return () -> {
      final Long returnValue = pFunction.getAsLong();
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public <T> Predicate<T> captureReturnOfPredicate(@Nonnull final Predicate<T> pFunction) {
    return (final T pParameter) -> {
      final Boolean returnValue = pFunction.test(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public IntPredicate captureReturnOfIntPredicate(@Nonnull final IntPredicate pFunction) {
    return (final int pParameter) -> {
      final Boolean returnValue = pFunction.test(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public DoublePredicate captureReturnOfDoublePredicate(@Nonnull final DoublePredicate pFunction) {
    return (final double pParameter) -> {
      final Boolean returnValue = pFunction.test(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public LongPredicate captureReturnOfLongPredicate(@Nonnull final LongPredicate pFunction) {
    return (final long pParameter) -> {
      final Boolean returnValue = pFunction.test(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public <T, U> BiPredicate<T, U> captureReturnOfBiPredicate(@Nonnull final BiPredicate<T, U> pFunction) {
    return (final T pFirst, final U pSecond) -> {
      final Boolean returnValue = pFunction.test(pFirst, pSecond);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public DoubleToIntFunction captureReturnOfDoubleToIntFunction(@Nonnull final DoubleToIntFunction pFunction) {
    return (final double pParameter) -> {
      final Integer returnValue = pFunction.applyAsInt(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public IntToDoubleFunction captureReturnOfIntToDoubleFunction(@Nonnull final IntToDoubleFunction pFunction) {
    return (final int pParameter) -> {
      final Double returnValue = pFunction.applyAsDouble(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public LongToIntFunction captureReturnOfLongToIntFunction(@Nonnull final LongToIntFunction pFunction) {
    return (final long pParameter) -> {
      final Integer returnValue = pFunction.applyAsInt(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public IntToLongFunction captureReturnOfIntToLongFunction(@Nonnull final IntToLongFunction pFunction) {
    return (final int pParameter) -> {
      final Long returnValue = pFunction.applyAsLong(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public DoubleToLongFunction captureReturnOfDoubleToLongFunction(@Nonnull final DoubleToLongFunction pFunction) {
    return (final double pParameter) -> {
      final Long returnValue = pFunction.applyAsLong(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public LongToDoubleFunction captureReturnOfLongToDoubleFunction(@Nonnull final LongToDoubleFunction pFunction) {
    return (final long pParameter) -> {
      final Double returnValue = pFunction.applyAsDouble(pParameter);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public IntBinaryOperator captureReturnOfIntBinaryOperator(@Nonnull final IntBinaryOperator pFunction) {
    return (final int pFirst, final int pSecond) -> {
      final Integer returnValue = pFunction.applyAsInt(pFirst, pSecond);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public LongBinaryOperator captureReturnOfLongBinaryOperator(@Nonnull final LongBinaryOperator pFunction) {
    return (final long pFirst, final long pSecond) -> {
      final Long returnValue = pFunction.applyAsLong(pFirst, pSecond);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }

  @SuppressWarnings("unchecked")
  public DoubleBinaryOperator captureReturnOfDoubleBinaryOperator(@Nonnull final DoubleBinaryOperator pFunction) {
    return (final double pFirst, final double pSecond) -> {
      final Double returnValue = pFunction.applyAsDouble(pFirst, pSecond);
      returnValues.add((R) returnValue);
      return returnValue;
    };
  }
}
