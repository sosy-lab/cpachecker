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
package org.sosy_lab.cpachecker.util.predicates.interfaces;

import java.math.BigInteger;

/**
 * This interface represents the Bitvector-Theory
 */
public interface BitvectorFormulaManager {

  public BitvectorFormula makeBitvector(int length, long pI);
  public BitvectorFormula makeBitvector(int length, BigInteger pI);
  public BitvectorFormula makeBitvector(int length, String pI);

  public BitvectorFormula makeVariable(int length, String pVar);

  public FormulaType<BitvectorFormula> getFormulaType(int length);

  public int getLength(BitvectorFormula number);

  // Numeric Operations

  public BitvectorFormula negate(BitvectorFormula number);
  public boolean isNegate(BitvectorFormula number);

  public BitvectorFormula add(BitvectorFormula number1, BitvectorFormula number2);
  public boolean isAdd(BitvectorFormula number);

  public BitvectorFormula subtract(BitvectorFormula number1, BitvectorFormula number2);
  public boolean isSubtract(BitvectorFormula number);

  public BitvectorFormula divide(BitvectorFormula number1, BitvectorFormula number2, boolean signed);
  public boolean isDivide(BitvectorFormula number, boolean signed);

  public BitvectorFormula modulo(BitvectorFormula number1, BitvectorFormula number2, boolean signed);
  public boolean isModulo(BitvectorFormula number, boolean signed);

  public BitvectorFormula multiply(BitvectorFormula number1, BitvectorFormula number2);
  public boolean isMultiply(BitvectorFormula number);

  // ----------------- Numeric relations -----------------

  public BooleanFormula equal(BitvectorFormula number1, BitvectorFormula number2);
  public boolean isEqual(BooleanFormula number);

  public BooleanFormula greaterThan(BitvectorFormula number1, BitvectorFormula number2, boolean signed);
  public boolean isGreaterThan(BooleanFormula number, boolean signed);

  public BooleanFormula greaterOrEquals(BitvectorFormula number1, BitvectorFormula number2, boolean signed);
  public boolean isGreaterOrEquals(BooleanFormula number, boolean signed);

  public BooleanFormula lessThan(BitvectorFormula number1, BitvectorFormula number2, boolean signed);
  public boolean isLessThan(BooleanFormula number, boolean signed);

  public BooleanFormula lessOrEquals(BitvectorFormula number1, BitvectorFormula number2, boolean signed);
  public boolean isLessOrEquals(BooleanFormula number, boolean signed);

  // Bitvector operations

  /**
   * Creates a formula representing a negation of the argument.
   * @param f a Formula
   * @return (!f1)
   */
  public BitvectorFormula not(BitvectorFormula bits);

  /**
   * Creates a formula representing an AND of the two arguments.
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (f1 & f2)
   */
  public BitvectorFormula and(BitvectorFormula bits1, BitvectorFormula bits2);

  /**
   * Creates a formula representing an OR of the two arguments.
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (f1 | f2)
   */
  public BitvectorFormula or(BitvectorFormula bits1, BitvectorFormula bits2);

  public BitvectorFormula xor(BitvectorFormula bits1, BitvectorFormula bits2);

  public boolean isNot(BitvectorFormula bits);
  public boolean isAnd(BitvectorFormula bits);
  public boolean isOr(BitvectorFormula bits);
  public boolean isXor(BitvectorFormula bits);

  /**
   * Returns a term representing the (arithmetic if signed is true) right shift of number by toShift.
   */
  public BitvectorFormula shiftRight(BitvectorFormula number, BitvectorFormula toShift, boolean signed);

  public BitvectorFormula shiftLeft(BitvectorFormula number, BitvectorFormula toShift);

  public BitvectorFormula concat(BitvectorFormula number, BitvectorFormula append);
  public BitvectorFormula extract(BitvectorFormula number, int msb, int lsb);

  /**
   * Extend a bitvector to the left (add most significant bits).
   * @param number The bitvector to extend.
   * @param extensionBits How many bits to add.
   * @param signed Whether the extension should depend on the sign bit.
   */
  public BitvectorFormula extend(BitvectorFormula number, int extensionBits, boolean signed);
}
