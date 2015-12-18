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
package org.sosy_lab.cpachecker.util.predicates.ldd;

import java.util.Collection;
import java.util.LinkedList;

import org.sosy_lab.cpachecker.util.Pair;

import edu.cmu.sei.rtss.jldd.swig.CIntArray;
import edu.cmu.sei.rtss.jldd.swig.JLDD;
import edu.cmu.sei.rtss.jldd.swig.theory_t;


class LDDFactory {

  private final theory_t theory;

  private final int lddManagerPtr;

  /**
   * Creates a new LDD factory for the given number of variables.
   *
   * @param size the number of variables supported by the factory.
   */
  public LDDFactory(int size) {
    this.theory = createTheory(TheoryType.TVPI, size);
    this.lddManagerPtr = JLDD.jldd_init(this.theory);
    setExistsType(ExistsType.FM);
    setDynamicVariableOrdering(true);
    /*// The following test reveals the limitations of LDDs concerning multiple variable labels:

    int constant = theory.create_int_cst(5);
    boolean leq = true;
    CIntArray array = new CIntArray(size);
    int leqParameter = leq ? 0 : 1;
    array.setitem(0, 1);
    int linearTerm = this.theory.create_linterm(array.cast(), size);
    int constraint = this.theory.create_cons(linearTerm, leqParameter, constant);
    int testXleq5 = JLDD.Ldd_FromCons(this.lddManagerPtr, constraint);
    JLDD.jldd_ref(testXleq5);

    constant = theory.create_int_cst(-5);
    leq = true;
    array = new CIntArray(size);
    leqParameter = leq ? 0 : 1;
    array.setitem(0, -1);
    linearTerm = this.theory.create_linterm(array.cast(), size);
    constraint = this.theory.create_cons(linearTerm, leqParameter, constant);
    int testXgeq5 = JLDD.Ldd_FromCons(this.lddManagerPtr, constraint);
    JLDD.jldd_ref(testXgeq5);

    int testXeq5 = JLDD.Ldd_And(lddManagerPtr, testXleq5, testXgeq5);
    JLDD.jldd_ref(testXeq5);
    //"Is x <= 5 && -x < -5 false? " + (testXeq5 == JLDD.Ldd_GetFalse(this.lddManagerPtr)));

    constant = theory.create_int_cst(10);
    leq = true;
    array = new CIntArray(size);
    leqParameter = leq ? 0 : 1;
    array.setitem(1, 1);
    linearTerm = this.theory.create_linterm(array.cast(), size);
    constraint = this.theory.create_cons(linearTerm, leqParameter, constant);
    int testYleq10 = JLDD.Ldd_FromCons(this.lddManagerPtr, constraint);
    JLDD.jldd_ref(testYleq10);

    constant = theory.create_int_cst(-10);
    leq = true;
    array = new CIntArray(size);
    leqParameter = leq ? 0 : 1;
    array.setitem(1, -1);
    linearTerm = this.theory.create_linterm(array.cast(), size);
    constraint = this.theory.create_cons(linearTerm, leqParameter, constant);
    int testYgeq10 = JLDD.Ldd_FromCons(this.lddManagerPtr, constraint);
    JLDD.jldd_ref(testYgeq10);

    int testYeq10 = JLDD.Ldd_And(lddManagerPtr, testYleq10, testYgeq10);
    JLDD.jldd_ref(testYeq10);
    //"Is y <= 10 && -y < -10 false? " + (testYeq10 == JLDD.Ldd_GetFalse(this.lddManagerPtr)));

    int testXeq5AndYeq10 = JLDD.Ldd_And(lddManagerPtr, testXeq5, testYeq10);
    JLDD.jldd_ref(testXeq5AndYeq10);
    //"Is x == 5 && y == 10 false? " + (testXeq5AndYeq10 == JLDD.Ldd_GetFalse(this.lddManagerPtr)));

    constant = theory.create_int_cst(0);
    leq = true;
    array = new CIntArray(size);
    leqParameter = leq ? 0 : 1;
    array.setitem(0, -1);
    array.setitem(1, 1);
    linearTerm = this.theory.create_linterm(array.cast(), size);
    constraint = this.theory.create_cons(linearTerm, leqParameter, constant);
    int testYMinusXleq0 = JLDD.Ldd_FromCons(this.lddManagerPtr, constraint);
    JLDD.jldd_ref(testYMinusXleq0);

    //"Is -x + y <= 0 false? " + (testYMinusXleq0 == JLDD.Ldd_GetFalse(this.lddManagerPtr)));

    int testAll = JLDD.Ldd_And(this.lddManagerPtr, testXeq5AndYeq10, testYMinusXleq0);
    JLDD.jldd_ref(testAll);

    //"Is -x + y <= 0 && x == 5 && y == 10 false? " + (testAll == JLDD.Ldd_GetFalse(this.lddManagerPtr)));
    //"What is it really? " + (-5 + 10 <= 0));

    */
  }

  private static theory_t createTheory(TheoryType type, int size) {
    switch (type) {
      case TVPI:
        return JLDD.tvpi_create_theory(size);
      case TVPIZ:
        return JLDD.tvpi_create_tvpiz_theory(size);
      case UTVPIZ:
        return JLDD.tvpi_create_utvpiz_theory(size);
      default:
        throw new AssertionError("Unhandled enum value in switch: " + type);
      }
  }

  public void setExistsType(ExistsType type) {
    switch (type) {
      case FM:
        JLDD.jldd_use_fm_exists(this.lddManagerPtr);
        break;
      case SFM:
        JLDD.jldd_use_sfm_exists(this.lddManagerPtr);
        break;
      case LW:
          JLDD.jldd_use_lw_exists(this.lddManagerPtr);
          break;
      default:
        throw new AssertionError("Unhandled enum value in switch: " + type);
    }
  }

  public void setDynamicVariableOrdering(boolean enable) {
    if (enable) {
      JLDD.jldd_autodyn_enable(this.lddManagerPtr);
    } else {
      JLDD.jldd_autodyn_disable(this.lddManagerPtr);
    }
  }

  /**
   * Creates the terminal zero node.
   *
   * @return the terminal zero node.
   */
  public LDD zero() {
    return createLDD(JLDD.Ldd_GetFalse(this.lddManagerPtr));
  }

  /**
   * Creates the terminal one node.
   *
   * @return the terminal one node.
   */
  public LDD one() {
    return createLDD(JLDD.Ldd_GetTrue(this.lddManagerPtr));
  }

  /**
   * Creates an LDD with the given pointer.
   *
   * @param lddPtr the pointer.
   * @return a new LDD.
   */
  private LDD createLDD(int lddPtr) {
    JLDD.jldd_ref(lddPtr);
    return new LDD(this, lddPtr);
  }

  /**
   * Creates the disjunction of the two given LDDs.
   *
   * @param pLdd the first LDD.
   * @param pLdd2 the second LDD.
   *
   * @return the disjunction of the given LDDs.
   */
  public LDD createOr(LDD pLdd, LDD pLdd2) {
    int ldd = JLDD.Ldd_Or(this.lddManagerPtr, pLdd.getLddPtr(), pLdd2.getLddPtr());
    return createLDD(ldd);
  }

  /**
   * Creates the conjunction of the two given LDDs.
   *
   * @param pLdd the first LDD.
   * @param pLdd2 the second LDD.
   * @return the conjunction of the given LDDs.
   */
  public LDD createAnd(LDD pLdd, LDD pLdd2) {
    int ldd = JLDD.Ldd_And(this.lddManagerPtr, pLdd.getLddPtr(), pLdd2.getLddPtr());
    return createLDD(ldd);
  }

  public LDD createXor(LDD pLdd, LDD pLdd2) {
    int ldd = JLDD.Ldd_Xor(this.lddManagerPtr, pLdd.getLddPtr(), pLdd2.getLddPtr());
    return createLDD(ldd);
  }

  public LDD createExists(LDD pLdd, LDD pLdd2) {
    int ldd = JLDD.Ldd_ExistsAbstract(this.lddManagerPtr, pLdd.getLddPtr(), pLdd2.getLddPtr());
    return createLDD(ldd);
  }

  public LDD createIfThenElse(LDD condition, LDD positive, LDD negative) {
    int ldd = JLDD.Ldd_Ite(this.lddManagerPtr, condition.getLddPtr(), positive.getLddPtr(), negative.getLddPtr());
    return createLDD(ldd);
  }

  /**
   * Creates an LDD representing an assignment or equality term.
   *
   * @param varCoeffs the variable coefficients.
   * @param varCount the number of total variables.
   * @param constValue the constant value.
   * @return the LDD representing the term.
   */
  public LDD makeConstantAssignment(Collection<Pair<Integer, Integer>> varCoeffs, int varCount, int constValue) {
    LDD positive = makeNode(varCoeffs, varCount, true, constValue);
    Collection<Pair<Integer, Integer>> negVarCoeffs = new LinkedList<>();
    for (Pair<Integer, Integer> varCoefficient : varCoeffs) {
      negVarCoeffs.add(Pair.of(varCoefficient.getFirst(), -varCoefficient.getSecond()));
    }
    LDD negative = makeNode(negVarCoeffs, varCount, true, -constValue);
    return positive.and(negative);
  }

  /**
   * Creates an LDD node labeled with the given linear inequality.
   * @param varCoeffs the variable coefficients.
   * @param varCount the number of total variables.
   * @param leq if <code>true</code>, the term uses less or equal, otherwise it uses less than.
   * @param constValue the constant value.
   * @return an LDD labeled with the given linear term.
   */
  public LDD makeNode(Collection<Pair<Integer, Integer>> varCoeffs, int varCount, boolean leq, int constValue) {
    int constant = theory.create_int_cst(constValue);
    CIntArray array = new CIntArray(varCount);
    for (Pair<Integer, Integer> varCoefficient : varCoeffs) {
      array.setitem(varCoefficient.getFirst(), varCoefficient.getSecond());
    }
    int leqParameter = leq ? 0 : 1;
    int linearTerm = this.theory.create_linterm(array.cast(), varCount);
    int constraint = this.theory.create_cons(linearTerm, leqParameter, constant);
    int ldd = JLDD.Ldd_FromCons(this.lddManagerPtr, constraint);
    return createLDD(ldd);
  }

  /**
   * Replaces the variable with the given index with the given linear term.
   * @param previous the LDD to replace the variable in.
   * @param varIndex the index of the variable.
   * @param pIndexCoefficients the linear coefficients in the term.
   * @param varCount the number of total variables.
   * @param constValue the constant value.
   * @return the new LDD.
   */
  public LDD replace(LDD previous, int varIndex, Collection<Pair<Integer, Integer>> pIndexCoefficients, int varCount, int constValue) {
    int constant = theory.create_int_cst(constValue);
    CIntArray array = new CIntArray(varCount);
    for (Pair<Integer, Integer> varCoefficient : pIndexCoefficients) {
      array.setitem(varCoefficient.getFirst(), varCoefficient.getSecond());
    }
    int linearTerm = this.theory.create_linterm(array.cast(), varCount);
    int ldd = JLDD.Ldd_SubstTermForVar(this.lddManagerPtr, previous.getLddPtr(), varIndex, linearTerm, constant);
    return createLDD(ldd);
  }

}
