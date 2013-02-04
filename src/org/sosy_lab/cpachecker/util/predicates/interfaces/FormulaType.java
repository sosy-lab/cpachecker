/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.util.Hashtable;
import java.util.Map;

/**
 * Represents a type of a formula.
 * @param <T> the static type of the FormulaType.
 */
public abstract class FormulaType<T extends Formula> {
  public abstract Class<T> getInterfaceType();
  FormulaType(){}
  public static FormulaType<RationalFormula> RationalType = new FormulaTypeImpl<>(RationalFormula.class);
  public static FormulaType<BooleanFormula> BooleanType = new FormulaTypeImpl<>(BooleanFormula.class);

  @Override
  public abstract String toString();

  static class FormulaTypeImpl<T extends Formula> extends FormulaType<T> {
    private Class<T> interfaceClass;
    private FormulaTypeImpl(Class<T> interfaceClass){
      this.interfaceClass = interfaceClass;
    }
    @Override
    public Class<T> getInterfaceType() {
      return interfaceClass;
    }
    @Override
    public String toString() {
      return interfaceClass.toString();
    }
  }

  public static <T extends Formula> boolean isBitVectorType(FormulaType<T> f) {
    return f instanceof BitvectorType;
  }
  public static <T extends Formula> boolean isRationalType(FormulaType<T> f) {
    return f == RationalType;
  }
  public static <T extends Formula> boolean isBooleanType(FormulaType<T> f) {
    return f == BooleanType;
  }

  public static class BitvectorType extends FormulaType<BitvectorFormula> {
    private int size;

    private BitvectorType(int size){
      this.size = (size);
    }
    private static Map<Integer, FormulaType<BitvectorFormula>> table = new Hashtable<>();
    /**
     * Gets the Raw Bitvector-Type with the given size.
     * Never call this method directly, always call the BitvectorFormulaManager.getFormulaType(int) method.
     * @param size
     * @return
     */
    public static FormulaType<BitvectorFormula> getBitvectorType(int size){
      int hashValue = size;
      FormulaType<BitvectorFormula> value = table.get(hashValue);
      if (value == null){
        value = new BitvectorType(size);
        table.put(hashValue, value);
      }
      return value;
    }

    public int getSize() {
      return size;
    }

    public BitvectorType withSize(int size){
      return (BitvectorType) getBitvectorType(size);
    }

    @Override
    public Class<BitvectorFormula> getInterfaceType() {
      return BitvectorFormula.class;
    }

    @Override
    public String toString() {
      return "Bitvector<" + getSize() + ">";
    }
  }
}
