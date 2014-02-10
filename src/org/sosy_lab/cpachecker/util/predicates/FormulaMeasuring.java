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
package org.sosy_lab.cpachecker.util.predicates;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.ImmutableSet;


public class FormulaMeasuring {

  public static class FormulaMeasures {
    private int trues = 0;
    private int falses = 0;
    private int conjunctions = 0;
    private int disjunctions = 0;
    private int negations = 0;
    private int atoms = 0;
    private int arithmeticEquals = 0;
    private int arithmeticIsLess = 0;
    private int arithmeticIsLessOrEquals = 0;
    private int arithmeticIsGreater = 0;
    private int arithmeticIsGreaterOrEquals = 0;
    private final Set<String> variables;

    public FormulaMeasures() {
      this.variables = new TreeSet<>();
    }

    public int getArithmeticEquals() { return arithmeticEquals; }
    public int getArithmeticIsGreater() { return arithmeticIsGreater; }
    public int getArithmeticIsGreaterOrEquals() { return arithmeticIsGreaterOrEquals; }
    public int getArithmeticIsLess() { return arithmeticIsLess; }
    public int getArithmeticIsLessOrEquals() { return arithmeticIsLessOrEquals; }
    public int getAtoms() { return atoms; }
    public int getConjunctions() { return conjunctions; }
    public int getDisjunctions() { return disjunctions; }
    public int getFalses() { return falses; }
    public int getNegations() { return negations; }
    public int getTrues() { return trues; }
    public ImmutableSet<String> getVariables() { return ImmutableSet.copyOf(this.variables); }
  }

  private final FormulaManagerView managerView;
  private final BooleanFormulaManager rawBooleanManager;
  private final RationalFormulaManager rawNumericManager;
  private final UnsafeFormulaManager unsafeManager;

  public FormulaMeasuring(FormulaManagerView pManagerView) {
    this.managerView = pManagerView;

    this.rawBooleanManager = managerView.getBooleanFormulaManager();
    this.rawNumericManager = managerView.getRationalFormulaManager();
    this.unsafeManager = managerView.getUnsafeFormulaManager();
  }


  public FormulaMeasures measure(BooleanFormula formula) {
    FormulaMeasures result = new FormulaMeasures();

    Set<BooleanFormula> handled = new HashSet<>();
    List<BooleanFormula> atoms = new ArrayList<>();

    Deque<BooleanFormula> toProcess = new ArrayDeque<>();
    toProcess.push(formula);
    handled.add(formula);

    while (!toProcess.isEmpty()) {
      BooleanFormula subFormula = toProcess.pop();
      assert handled.contains(subFormula);

      if (rawBooleanManager.isTrue(subFormula)) {
        result.trues++;
        continue;
      }

      if (rawBooleanManager.isFalse(subFormula)) {
        result.falses++;
        continue;
      }

      if (unsafeManager.isAtom(subFormula)) {
        result.atoms++;

        subFormula = managerView.uninstantiate(subFormula);

        if (managerView.isPurelyArithmetic(subFormula)) {
          if (rawNumericManager.isEqual(subFormula)) { result.arithmeticEquals++; }
          if (rawNumericManager.isLessOrEquals(subFormula)) { result.arithmeticIsLessOrEquals++; }
          if (rawNumericManager.isLessThan(subFormula)) { result.arithmeticIsLess++; }
          if (rawNumericManager.isGreaterOrEquals(subFormula)) { result.arithmeticIsGreaterOrEquals++; }
          if (rawNumericManager.isGreaterThan(subFormula)) { result.arithmeticIsGreater++; }
        } else {
          atoms.add(subFormula);
        }

        result.variables.addAll(managerView.extractVariables(subFormula));
      } else {
        if (rawBooleanManager.isNot(subFormula)) { result.negations++; }
        if (rawBooleanManager.isAnd(subFormula)) { result.conjunctions++; }
        if (rawBooleanManager.isOr(subFormula)) { result.disjunctions++; }

        for (int i = 0; i < unsafeManager.getArity(subFormula); ++i) {
          BooleanFormula c = unsafeManager.typeFormula(FormulaType.BooleanType, unsafeManager.getArg(subFormula, i));
          if (handled.add(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    return result;
  }



}
