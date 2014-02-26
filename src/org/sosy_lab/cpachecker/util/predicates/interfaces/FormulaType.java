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

import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a type of a formula.
 * @param <T> the static type of the FormulaType.
 */
public abstract class FormulaType<T extends Formula> {

  FormulaType() {}

  public abstract Class<T> getInterfaceType();

  public boolean isBitvectorType() {
    return false;
  }

  public boolean isBooleanType() {
    return false;
  }

  public boolean isNumeralType() {
    return false;
  }

  public boolean isRationalType() {
    return false;
  }

  public boolean isIntegerType() {
    return false;
  }

  @Override
  public abstract String toString();

  public abstract static class NumeralType<T extends NumeralFormula> extends FormulaType<T> {

    @Override
    public final boolean isNumeralType() {
      return true;
    }
  }

  public static final FormulaType<? extends NumeralFormula> RationalType = new NumeralType<RationalFormula>() {

    @Override
    public Class<RationalFormula> getInterfaceType() {
      return RationalFormula.class;
    }

    @Override
    public boolean isRationalType() {
      return true;
    }

    @Override
    public String toString() {
      return "Rational";
    }
  };

  public static final FormulaType<? extends NumeralFormula> IntegerType = new NumeralType<IntegerFormula>() {

    @Override
    public Class<IntegerFormula> getInterfaceType() {
      return IntegerFormula.class;
    }

    @Override
    public boolean isIntegerType() {
      return true;
    }

    @Override
    public String toString() {
      return "Integer";
    }
  };

  public static final FormulaType<BooleanFormula> BooleanType = new FormulaType<BooleanFormula>() {

    @Override
    public Class<BooleanFormula> getInterfaceType() {
      return BooleanFormula.class;
    }

    @Override
    public boolean isBooleanType() {
      return true;
    }

    @Override
    public String toString() {
      return "Boolean";
    }
  };

  public static final class BitvectorType extends FormulaType<BitvectorFormula> {
    private final int size;

    private BitvectorType(int size) {
      this.size = (size);
    }
    private static Map<Integer, FormulaType<BitvectorFormula>> table = new HashMap<>();
    /**
     * Gets the Raw Bitvector-Type with the given size.
     * Never call this method directly, always call the BitvectorFormulaManager.getFormulaType(int) method.
     * @param size
     * @return
     */
    public static FormulaType<BitvectorFormula> getBitvectorType(int size) {
      int hashValue = size;
      FormulaType<BitvectorFormula> value = table.get(hashValue);
      if (value == null) {
        value = new BitvectorType(size);
        table.put(hashValue, value);
      }
      return value;
    }

    @Override
    public boolean isBitvectorType() {
      return true;
    }

    public int getSize() {
      return size;
    }

    public BitvectorType withSize(int size) {
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
