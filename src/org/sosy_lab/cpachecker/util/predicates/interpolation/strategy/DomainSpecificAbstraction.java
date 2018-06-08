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
package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.jas.arith.Rational;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.logging.Level;
import jpl.Float;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager.Interpolator;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointRoundingModeFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;
import com.google.common.base.Splitter;




public class DomainSpecificAbstraction<T> {
  protected final FormulaManagerView fmgr;
  protected FormulaManagerView oldFmgr;
  String[] arrayVariablesThatAreUsedInBothParts;
  public static String[] arrayVariablesForFormulas;
  private List<BooleanFormula> formulas;
  private Interpolator<T> myInterpolator;
  LogManager logger;
  public DomainSpecificAbstraction(ShutdownNotifier pShutdownNotifier,
                                   FormulaManagerView pFmgr, BooleanFormulaManager pBfmgr,
                                   FormulaManagerView oldFmgr0, Interpolator<T> pTInterpolator, LogManager pLogger) {
    fmgr = pFmgr;
    oldFmgr = oldFmgr0;
    myInterpolator = pTInterpolator;
    logger = pLogger;
  }



  public List<BooleanFormula> domainSpecificAbstractionsCheck(Solver mySolver,
                                                              List<BooleanFormula> oldFormulas)
      throws SolverException, InterruptedException {

    /*@SuppressWarnings("unchecked")
    InterpolatingProverEnvironment<BooleanFormula> myEnvironment =
        (InterpolatingProverEnvironment<BooleanFormula>) myInterpolator
            .newEnvironment(); */

        BooleanFormula interpolationFormula;
        List<BooleanFormula> interpolants = Lists.newArrayListWithExpectedSize(oldFormulas.size()
            - 1);
        // running the algorithm for every formula with its successor
    for (int it = 0; it < oldFormulas.size() - 1; it = it + 1) {
      formulas = Lists.newArrayListWithExpectedSize(oldFormulas.size
          ());
      formulas.add(oldFormulas.get(it));
      formulas.add(oldFormulas.get(it + 1));
      final List<Set<String>> variablesInFormulas =
          Lists.newArrayListWithExpectedSize(formulas.size());
    //  final HashMap<String, FormulaType> variableTypes = new HashMap<String, FormulaType>();
      for (BooleanFormula f : formulas) {
        variablesInFormulas.add(oldFmgr.extractVariableNames(f));
      }
      // extracting the variables that have to be renamed - make external function?
      List<List<IntegerFormula>> frontierList = Lists.newArrayListWithExpectedSize(formulas.size());
      Set<String> variables1 = variablesInFormulas.get(0);
      Set<String> variables2 = variablesInFormulas.get(1);
      Set<String> variablesThatAreUsedInBothParts = Sets.intersection(variables1, variables2)
          .immutableCopy();
      int m = 0;
    /*  for (String s : variablesThatAreUsedInBothParts) {
        variableTypes.put(s, oldFmgr.getFormulaType(oldFmgr.parse(s)));
      } */

      String[] arrayVariables1 = variables1.toArray(new String[variables1.size()]);
      String[] arrayVariables2 = variables2.toArray(new String[variables2.size()]);
      arrayVariablesThatAreUsedInBothParts = variablesThatAreUsedInBothParts.toArray(new
          String[variablesThatAreUsedInBothParts.size
          ()]);
      // not necessary, could be deleted
      for (int i = 0; i < arrayVariablesThatAreUsedInBothParts.length; i++) {
        for (int j = 0; j < arrayVariables1.length; j++) {
          if (arrayVariablesThatAreUsedInBothParts[i].equals(arrayVariables1[j])) {
            arrayVariables1[j] = arrayVariables1[j] + "'";
          }
        }
        for (int j = 0; j < arrayVariables2.length; j++) {
          if (arrayVariablesThatAreUsedInBothParts[i].equals(arrayVariables2[j])) {
            arrayVariables2[j] = arrayVariables2[j] + "''";
          }
        }
      }

      // building the lattice structure
      boolean[][] lattice = new boolean[(int) Math.pow(2, (arrayVariablesThatAreUsedInBothParts
          .length))][(int) Math.pow(2,
          (arrayVariablesThatAreUsedInBothParts.length))];
      String[] latticeNames = new String[(arrayVariablesThatAreUsedInBothParts.length) + 1];
      String[] powersetBase = new String[arrayVariablesThatAreUsedInBothParts.length];
      double d = (double) (arrayVariablesThatAreUsedInBothParts
          .length);
      double e = (double) 2;
      String[] fullLatticeNames = new String[(int) Math.pow(e, d)
          ];

      latticeNames[0] = "root";
      List<BooleanFormula> relationAbstraction1Formula =
          Lists.newArrayListWithExpectedSize(variablesThatAreUsedInBothParts
              .size());
      List<BooleanFormula> relationAbstraction2Formula =
          Lists.newArrayListWithExpectedSize(variablesThatAreUsedInBothParts
              .size());

    // building the relation abstractions for both formulas in Formula Version and in String
      // Version -
      // should be done in
      // an
      // external
      // method with an exchangeable strategy
      String[] relationAbstraction1 = new String[variablesThatAreUsedInBothParts.size()];
      String[] relationAbstraction2 = new String[variablesThatAreUsedInBothParts.size()];

      if ((arrayVariablesThatAreUsedInBothParts.length % 2) == 0) {
        for (int i = 0; i < arrayVariablesThatAreUsedInBothParts.length; i = i + 2) {
          relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
              arrayVariablesThatAreUsedInBothParts[i] + "'";
          relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
              arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
              arrayVariablesThatAreUsedInBothParts[i] + "'" + " - " +
              arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
    /*      FormulaType resultType1 = variableTypes.get(arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variableTypes.get(arrayVariablesThatAreUsedInBothParts[i + 1]);
          if (resultType1.isIntegerType() & resultType2.isIntegerType()) { */
            IntegerFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
       /*   } else if (resultType1.isRationalType() & resultType2.isRationalType()){
            RationalFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
          } else if (resultType1.isFloatingPointRoundingModeType() & resultType2.isFloatingPointRoundingModeType()){
            FloatingPointRoundingModeFormula helperFormula1, helperFormula2, helperFormula3,
                helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
          } */
            latticeNames[i + 1] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 2] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
                arrayVariablesThatAreUsedInBothParts[i + 1];

        }
        for (int i = 0; i < arrayVariablesThatAreUsedInBothParts.length; i = i + 2) {
          relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
              arrayVariablesThatAreUsedInBothParts[i] + "''";
          relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
              arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
              arrayVariablesThatAreUsedInBothParts[i] + "''" + " - " +
              arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
      /*    FormulaType resultType1 = variableTypes.get(arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variableTypes.get(arrayVariablesThatAreUsedInBothParts[i + 1]);
          if (resultType1.isIntegerType() & resultType2.isIntegerType()) { */
            IntegerFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula
                helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
       /*   } else if (resultType1.isRationalType() & resultType2.isRationalType()) {
            RationalFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula
                helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          } else if (resultType1.isFloatingPointRoundingModeType() & resultType2.isFloatingPointRoundingModeType()){
            FloatingPointRoundingModeFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula
                helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          } */
        }
      } else {
        int i;
        for (i = 0; i < arrayVariablesThatAreUsedInBothParts.length - 1; i = i + 2) {
          relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
              arrayVariablesThatAreUsedInBothParts[i] + "'";
          relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
              arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
              arrayVariablesThatAreUsedInBothParts[i] + "'" + " - " +
              arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
     /*     FormulaType resultType1 = variableTypes.get(arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variableTypes.get(arrayVariablesThatAreUsedInBothParts[i +
          1]);
          if (resultType1.isIntegerType() & resultType2.isIntegerType()) { */
            IntegerFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
     /*     } else if (resultType1.isRationalType() & resultType2.isRationalType()){
            RationalFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
          } else if (resultType1.isFloatingPointRoundingModeType() & resultType2.isFloatingPointRoundingModeType()){
            FloatingPointRoundingModeFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
          } */
          latticeNames[i + 1] = arrayVariablesThatAreUsedInBothParts[i];
          latticeNames[i + 2] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
              arrayVariablesThatAreUsedInBothParts[i + 1];
        }

        for (i = 0; i < arrayVariablesThatAreUsedInBothParts.length - 1; i = i + 2) {
          relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
              arrayVariablesThatAreUsedInBothParts[i] + "''";
          relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
              arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
              arrayVariablesThatAreUsedInBothParts[i] + "''" + " - " +
              arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
     /*     FormulaType resultType1 = variableTypes.get(arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variableTypes.get(arrayVariablesThatAreUsedInBothParts[i + 1]);
          if (resultType1.isIntegerType() & resultType2.isIntegerType()) { */
            IntegerFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula
                helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.IntegerType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
    /*      } else if (resultType1.isRationalType() & resultType2.isRationalType()){
            RationalFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula
                helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.RationalType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          } else if (resultType1.isFloatingPointRoundingModeType() & resultType2.isFloatingPointRoundingModeType()){
            FloatingPointRoundingModeFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula
                helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);
            helperFormula7 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula4 = fmgr.makeMinus(helperFormula7, helperFormula8);
            helperFormula5 = fmgr.makeEqual(helperFormula3, helperFormula4);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          } */
        }
        relationAbstraction1[arrayVariablesThatAreUsedInBothParts.length - 1] =
            arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]
                + ""
                + " = " + arrayVariablesThatAreUsedInBothParts
                [arrayVariablesThatAreUsedInBothParts.length - 1] + "'";
    /*    FormulaType resultType1 = variableTypes.get
            (arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]);
        if (resultType1.isBooleanType()) { */
       /*   BooleanFormula helperFormula1, helperFormula2, helperFormula3;
          helperFormula1 = fmgr.makeVariable(FormulaType.BooleanType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula2 =
              fmgr.makeVariable(FormulaType.BooleanType, arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
          helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
          relationAbstraction1Formula.add(helperFormula3); */
       IntegerFormula helperFormula1, helperFormula2;
       BooleanFormula helperFormula3;
       helperFormula1 = fmgr.makeVariable(FormulaType.IntegerType,
           arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]);
        helperFormula2 =
            fmgr.makeVariable(FormulaType.IntegerType, arrayVariablesThatAreUsedInBothParts
                [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
        helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
        relationAbstraction1Formula.add(helperFormula3);
    /*    } else if (resultType1.isIntegerType()){
          IntegerFormula helperFormula1, helperFormula2;
          BooleanFormula helperFormula3;
          helperFormula1 = fmgr.makeVariable(FormulaType.IntegerType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula2 =
              fmgr.makeVariable(FormulaType.IntegerType, arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
          helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
          relationAbstraction1Formula.add(helperFormula3);
        } else if (resultType1.isRationalType()){
          RationalFormula helperFormula1, helperFormula2;
          BooleanFormula helperFormula3;
          helperFormula1 = fmgr.makeVariable(FormulaType.RationalType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula2 =
              fmgr.makeVariable(FormulaType.RationalType, arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
          helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
          relationAbstraction1Formula.add(helperFormula3);
        } else if (resultType1.isFloatingPointRoundingModeType()){
          FloatingPointRoundingModeFormula helperFormula1, helperFormula2;
          BooleanFormula helperFormula3;
          helperFormula1 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula2 =
              fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType, arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
          helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
          relationAbstraction1Formula.add(helperFormula3);
        } */
        relationAbstraction2[arrayVariablesThatAreUsedInBothParts.length - 1] =
            arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]
                + ""
                + " = " + arrayVariablesThatAreUsedInBothParts
                [arrayVariablesThatAreUsedInBothParts.length - 1] + "''";
     /*   FormulaType resultType2 = variableTypes.get
            (arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]);
        if (resultType2.isBooleanType()) { */
       /*   BooleanFormula helperFormula4, helperFormula5, helperFormula6;
          helperFormula4 = fmgr.makeVariable(FormulaType.BooleanType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula5 = fmgr.makeVariable(FormulaType.BooleanType,
              arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "''");
          helperFormula6 = fmgr.makeEqual(helperFormula4, helperFormula5);
          relationAbstraction2Formula.add(helperFormula6); */
        IntegerFormula helperFormula4, helperFormula5;
        BooleanFormula helperFormula6;
        helperFormula4 = fmgr.makeVariable(FormulaType.IntegerType,
            arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                - 1]);
        helperFormula5 = fmgr.makeVariable(FormulaType.IntegerType,
            arrayVariablesThatAreUsedInBothParts
                [arrayVariablesThatAreUsedInBothParts.length - 1] + "''");
        helperFormula6 = fmgr.makeEqual(helperFormula4, helperFormula5);
        relationAbstraction2Formula.add(helperFormula6);
      /*  } else if (resultType2.isIntegerType()){
          IntegerFormula helperFormula4, helperFormula5;
          BooleanFormula helperFormula6;
          helperFormula4 = fmgr.makeVariable(FormulaType.IntegerType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula5 = fmgr.makeVariable(FormulaType.IntegerType,
              arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "''");
          helperFormula6 = fmgr.makeEqual(helperFormula4, helperFormula5);
          relationAbstraction2Formula.add(helperFormula6);
        } else if (resultType1.isRationalType()){
          RationalFormula helperFormula4, helperFormula5;
          BooleanFormula helperFormula6;
          helperFormula4 = fmgr.makeVariable(FormulaType.RationalType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula5 = fmgr.makeVariable(FormulaType.RationalType,
              arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "''");
          helperFormula6 = fmgr.makeEqual(helperFormula4, helperFormula5);
          relationAbstraction2Formula.add(helperFormula6);
        } else if (resultType1.isFloatingPointRoundingModeType()){
          FloatingPointRoundingModeFormula helperFormula4, helperFormula5;
          BooleanFormula helperFormula6;
          helperFormula4 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula5 = fmgr.makeVariable(FormulaType.FloatingPointRoundingModeType,
              arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "''");
          helperFormula6 = fmgr.makeEqual(helperFormula4, helperFormula5);
          relationAbstraction2Formula.add(helperFormula6);
        } */
        latticeNames[latticeNames.length - 1] =
            arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1];
      }

      //generating the nodes of the lattice
      for (int k = 1; k < latticeNames.length; k++){
        powersetBase[k - 1] = latticeNames[k];
      }

     for (int i = 0; i < fullLatticeNames.length; i++){
       for (int j = 0; j < powersetBase.length; j++){
         if ((i & (1 << j)) == 0){
           if (fullLatticeNames[i] == null) {
             fullLatticeNames[i] = powersetBase[j];
           }
           else{
             fullLatticeNames[i] = fullLatticeNames[i] + " ," + powersetBase[j];
           }
         }
       }
     }
      for (int i = 0; i < fullLatticeNames.length; i++){
        if (fullLatticeNames[i] == null){
          fullLatticeNames[i] = "root";
        }
      }


      // generating the relationships between the lattice nodes
      for (int x = 0; x < fullLatticeNames.length; x++) {
        for (int y = 0; y < fullLatticeNames.length; y++) {
          if (fullLatticeNames[x] == null) {
            lattice[x][y] = false;
          } else {
            if (fullLatticeNames[x].equals(fullLatticeNames[y])) {
              lattice[x][y] = false;
            } else {
              if (fullLatticeNames[y] == null) {
                lattice[x][y] = false;
              } else {
                if (fullLatticeNames[y].contains(fullLatticeNames[x])) {
                  lattice[x][y] = true;
                } else {
                  lattice[x][y] = false;
                }
              }
            }
          }
        }
      }
      arrayVariablesForFormulas = arrayVariablesThatAreUsedInBothParts;
      FirstPartRenamingFct renamer1 = new FirstPartRenamingFct();
      ScndPartRenamingFct renamer2 = new ScndPartRenamingFct();
      BooleanFormula firstPart = formulas.get(0);
      BooleanFormula scndPart = formulas.get(1);
      BooleanFormula firstPartChanged = oldFmgr.renameFreeVariablesAndUFs(firstPart, renamer1);
      BooleanFormula scndPartChanged = oldFmgr.renameFreeVariablesAndUFs(scndPart, renamer2);
      List<BooleanFormula> changed_formulas =
          Lists.newArrayListWithExpectedSize(formulas.size() - 1);
      List<BooleanFormula> changed_formulas_rest =
          Lists.newArrayListWithExpectedSize(formulas.size() - 1);
      changed_formulas.add(firstPartChanged);
      changed_formulas.add(scndPartChanged);

      BooleanFormula helperFormula1;
      BooleanFormula helperFormula2;

      firstPartChanged = fmgr.translateFrom(firstPartChanged, oldFmgr);
      scndPartChanged = fmgr.translateFrom(scndPartChanged, oldFmgr);
      for (int i = 0; i < it; i++){
        BooleanFormula addFormula = oldFormulas.get(i);
        BooleanFormula changedFormula = oldFmgr.renameFreeVariablesAndUFs(addFormula, renamer1);
        changedFormula = fmgr.translateFrom(changedFormula, oldFmgr);
        changed_formulas_rest.add(changedFormula);
      }
      for (int i = it + 2; i < oldFormulas.size(); i++){
        BooleanFormula addFormula = oldFormulas.get(i);
        BooleanFormula changedFormula = oldFmgr.renameFreeVariablesAndUFs(addFormula, renamer2);
        changedFormula = fmgr.translateFrom(changedFormula, oldFmgr);
        changed_formulas_rest.add(changedFormula);
      }

      boolean abstractionFeasible = false;
      boolean isIncomparable = false;

      for (int h = 0; h < fullLatticeNames.length; h++) {
        helperFormula1 = firstPartChanged;
        helperFormula2 = scndPartChanged;
        Iterable<String> splitOperator = Splitter.on(" ,").split(fullLatticeNames[h]);
        for (String s : splitOperator) {
          for (int k = 0; k < relationAbstraction1.length; k++) {
            if ((relationAbstraction1[k]).contains(s + " = ")) {
              helperFormula1 = fmgr.makeAnd(helperFormula1, relationAbstraction1Formula.get
                  (k));

            }
            if ((relationAbstraction2[k]).contains(s + " = ")) {
              helperFormula2 = fmgr.makeAnd(helperFormula2, relationAbstraction2Formula.get
                  (k));

            }
          }
        }

        BooleanFormula toCheckFormula = fmgr.makeAnd(helperFormula1, helperFormula2);
        List<BooleanFormula> toCheckFormulaList =
            Lists.newArrayListWithExpectedSize(formulas.size() - 1);
        toCheckFormulaList.add(toCheckFormula);
        BlockFormulas toCheckFormulaBlocked = new BlockFormulas(toCheckFormulaList);

        abstractionFeasible = prove(toCheckFormulaBlocked, mySolver);
        if (abstractionFeasible) {
          List<List<IntegerFormula>> frontierListCopy = Lists.newArrayListWithExpectedSize(oldFormulas.size() - 1);;
          for (List<IntegerFormula> s : frontierList){
            frontierListCopy.add(s);
          }
          isIncomparable = checkComparability(frontierListCopy, fullLatticeNames[h]);
          if (isIncomparable) {
            List<IntegerFormula> new_frontier_elem = maximise(firstPartChanged, scndPartChanged,
                relationAbstraction1,
                relationAbstraction2, relationAbstraction1Formula,
                relationAbstraction2Formula, lattice, fullLatticeNames, h, mySolver);
            frontierList.add(new_frontier_elem);

          }
        }
      }
      helperFormula1 = firstPartChanged;
      helperFormula2 = scndPartChanged;

      for (List<IntegerFormula> x : frontierList) {

        for (IntegerFormula y : x) {

          for (int k = 0; k < relationAbstraction1.length; k++) {

            if ((relationAbstraction1Formula.get(k).toString()).contains("= " + y.toString())) {
              helperFormula1 = fmgr.makeAnd(helperFormula1, relationAbstraction1Formula.get
                  (k));


            }

            if ((relationAbstraction2Formula.get(k).toString()).contains("= " + y.toString())) {
              helperFormula2 = fmgr.makeAnd(helperFormula2, relationAbstraction2Formula.get
                  (k));


            }
          }

        }
      }

      //interpolationFormula = fmgr.makeAnd(helperFormula1, helperFormula2);
      List<BooleanFormula> interpolationFormulaList =
          Lists.newArrayListWithExpectedSize(formulas.size() - 1);
      // there should be an interpolant computed in this place, but I realised that it won't work
      // because
      // it's
      // relying on group ids, which I don't have.
      //interpolationFormulaList.add(interpolationFormula);
      interpolationFormulaList.add(helperFormula1);
      interpolationFormulaList.add(helperFormula2);
      try (@SuppressWarnings("unchecked")
           InterpolatingProverEnvironment<T> myItpProver =
               (InterpolatingProverEnvironment<T>) mySolver.newProverEnvironmentWithInterpolation
                   ()) {

      /*  List<T> myItpGroupIds = new ArrayList<>(formulas.size());
        for (BooleanFormula f : formulas) {
          myItpGroupIds.add(myItpProver.push(f));
        } */

      List<T> myItpGroupIds = new ArrayList<>(formulas.size());
     /* for (BooleanFormula f : interpolationFormulaList){
        myItpGroupIds.add(myItpProver.push(f));
      } */
     myItpGroupIds.add(myItpProver.push(helperFormula1));
     myItpProver.push(helperFormula2);

    /*  for (int i = 0; i < it; i++) {
        myItpProver.push(oldFormulas.get(i));
      }
      for (int i = it + 2; i < oldFormulas.size(); i++){
        myItpProver.push(oldFormulas.get(i));
      } */
    for (BooleanFormula f : changed_formulas_rest){
      myItpProver.push(f);
    }

        if (!myItpProver.isUnsat()) {
            throw new UnknownError("Interpolant kann nicht berechnet werden!");

        } else {

          BooleanFormula myInterpolant = myItpProver.getInterpolant
              (myItpGroupIds);


          interpolants.add(myInterpolant);
        }
      }
 /*     @SuppressWarnings("unchecked")
      InterpolatingProverEnvironment<BooleanFormula> newEnvironment =
          (InterpolatingProverEnvironment<BooleanFormula>) myInterpolator.itpProver; */
      /*BooleanFormula myInterpolant = myInterpolator.itpProver.getInterpolant
          ((List<T>) interpolationFormulaList); */
 /*     BooleanFormula myInterpolant = newEnvironment.getInterpolant(interpolationFormulaList);

      interpolants.add(myInterpolant); */
/*    BooleanFormula myInterpolant = my_environment.getInterpolant(interpolationFormulaList);
     */


    }
   // return interpolationFormulaList;
     // myInterpolator.itpProver.getInterpolant((List<T>) interpolationFormulaList);

   //return Collections.emptyList();
    return interpolants;

  };

  private Boolean prove(BlockFormulas toCheckFormulaBlocked, Solver mySolver){
    Boolean abstractionFeasible = false;
    try (ProverEnvironment prover = mySolver
        .newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      for (BooleanFormula block : toCheckFormulaBlocked.getFormulas()) {
        prover.push(block);
      }
      if (!prover.isUnsat()) {


        abstractionFeasible = false;

      } else {

        abstractionFeasible = true;
      }
    } catch (InterruptedException pE) {
      logger.log(Level.WARNING, "Interrupted Exception!");
    } catch (SolverException pE) {
      logger.log(Level.WARNING, "Solver Exception!");
    }
    return abstractionFeasible;
  }

  private List<IntegerFormula> maximise(BooleanFormula firstPartChanged, BooleanFormula
      scndPartChanged, String[] relationAbstraction1, String[] relationAbstraction2,
                                  List<BooleanFormula> relationAbstraction1Formula,
                                  List<BooleanFormula>
                                      relationAbstraction2Formula,
                                  boolean[][] lattice,
                                  String[]
      fullLatticeNames, int placeinlattice, Solver mySolver){

    String[] middleElement = new String[fullLatticeNames.length];
    int middleElemIndex = 0;
    Boolean isFeasible = true;
    BooleanFormula helperFormula1;
    BooleanFormula helperFormula2;
    Boolean hasFeasibleSuccessor = false;
    int feasibleSuccessorPosition = 0;
    List<IntegerFormula> maximumFeasibleAbstraction = Lists.newArrayListWithExpectedSize(formulas
        .size() - 1);

    for (int i = placeinlattice + 1; i < fullLatticeNames.length; i++){

          if (lattice[placeinlattice][i] == true){
            //call prover function with fullLatticeNames[i] applied to firstPartChanged and
            // scndPartChanged
            helperFormula1 = firstPartChanged;
            helperFormula2 = scndPartChanged;
            Iterable<String> splitOperator = Splitter.on(" ,").split(fullLatticeNames[i]);
            for (String s : splitOperator){

              for (int k = 0; k < relationAbstraction1.length; k++){
                if ((relationAbstraction1[k]).contains(s + " = ")){
                  //BooleanFormula helperFormula_1;
                  helperFormula1 = fmgr.makeAnd(helperFormula1, relationAbstraction1Formula.get
                      (k));


                }
                if ((relationAbstraction2[k]).contains(s + " = ")){
                  //BooleanFormula helperFormula;
                  helperFormula2 = fmgr.makeAnd(helperFormula2, relationAbstraction2Formula.get
                      (k));


                }
              }
            }
            BooleanFormula toCheckFormula = fmgr.makeAnd(helperFormula1, helperFormula2);
            List<BooleanFormula> toCheckFormulaList = Lists.newArrayListWithExpectedSize(formulas.size() - 1);
            toCheckFormulaList.add(toCheckFormula);
            BlockFormulas toCheckFormulaBlocked = new BlockFormulas(toCheckFormulaList);
            isFeasible = prove(toCheckFormulaBlocked, mySolver);
            //if abstraction is feasible:
            if (isFeasible) {
              hasFeasibleSuccessor = true;
              feasibleSuccessorPosition = i;
              for (int k = 0; k < fullLatticeNames.length; k++) {
                if (lattice[i][k] == true) {
                  middleElement[middleElemIndex] = (fullLatticeNames[k]);
                  middleElemIndex++;
                  i = k;
                }
              }
              break;
            }
          }

    }
    if (middleElement[0] == null){
      if (hasFeasibleSuccessor) {

        maximumFeasibleAbstraction = StringtoIntegerFormulaList
            (fullLatticeNames[feasibleSuccessorPosition] /*, formulas */);

      }
      else {
        maximumFeasibleAbstraction = StringtoIntegerFormulaList
            (fullLatticeNames[placeinlattice] /*, formulas */);
      }

    } else {
      int counter = 0;
      for (int i = 0; i < middleElement.length; i++) {
        if (middleElement[i] != null) {
          counter++;
        }
      }
        Boolean middleElemFeasible;
        String middleElementString = middleElement[counter / 2];
        helperFormula1 = firstPartChanged;
        helperFormula2 = scndPartChanged;
        Iterable<String> splitOperator = Splitter.on(" ,").split(middleElementString);
        for (String s : splitOperator) {
          for (int k = 0; k < relationAbstraction1.length; k++) {
            if ((relationAbstraction1[k]).contains(s + " = ")) {
              helperFormula1 = fmgr.makeAnd(helperFormula1, relationAbstraction1Formula.get
                  (k));


            }
            if ((relationAbstraction2[k]).contains(s + " = ")) {
              helperFormula2 = fmgr.makeAnd(helperFormula2, relationAbstraction2Formula.get
                  (k));


            }
          }
        }
        BooleanFormula toCheckFormula = fmgr.makeAnd(helperFormula1, helperFormula2);
        List<BooleanFormula> toCheckFormulaList =
            Lists.newArrayListWithExpectedSize(formulas.size() - 1);
        toCheckFormulaList.add(toCheckFormula);
        BlockFormulas toCheckFormulaBlocked = new BlockFormulas(toCheckFormulaList);
        middleElemFeasible = prove(toCheckFormulaBlocked, mySolver);
        if (middleElemFeasible) {

          //maximumFeasibleAbstraction = middleElement[counter/2] transformed into a Boolean Formula.
          maximumFeasibleAbstraction = StringtoIntegerFormulaList
              (middleElement[counter/2]);

        } else {

          maximumFeasibleAbstraction = StringtoIntegerFormulaList
              (fullLatticeNames[placeinlattice] /*, formulas */);

        }
    }
    return maximumFeasibleAbstraction;
  }

  private List<IntegerFormula> StringtoIntegerFormulaList(String input){
    IntegerFormula helperFormula1, helperFormula2, helperFormula3;
    List<IntegerFormula> maximumFeasibleAbstraction = Lists.newArrayListWithExpectedSize(formulas.size() - 1);

    String[] helperArray = new String[2];
    int i = 0;
    Iterable<String> splitOperator = Splitter.on(" ,").split(input);
    for (String s : splitOperator) {
      if (s.contains(" - ")){
        Iterable<String> splitOperator2 = Splitter.on(" - ").split(s);
        for (String t : splitOperator2){
          helperArray[i] = t;
          i++;
        }
        helperFormula1 = fmgr.makeVariable(FormulaType.IntegerType, helperArray[0]);
        helperFormula2 = fmgr.makeVariable(FormulaType.IntegerType, helperArray[1]);
        helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);

      }
      else {
        helperFormula3 = fmgr.makeVariable(FormulaType.IntegerType, s);
      }
      maximumFeasibleAbstraction.add(helperFormula3);
    }
    return maximumFeasibleAbstraction;
  }

  private Boolean checkComparability(List<List<IntegerFormula>> frontierListCopy, String
      fullLatticeNames_h){
    List<IntegerFormula> toCompareWith = StringtoIntegerFormulaList(fullLatticeNames_h);
    List<List<IntegerFormula>> compareList = Lists.newArrayListWithExpectedSize(formulas.size() -
        1);
    Boolean isIncomparable = false;
    Boolean comparable = false;
    while (frontierListCopy.size() != 0) {
      List<IntegerFormula> smallestList = frontierListCopy.get(0);


      for (List<IntegerFormula> f : frontierListCopy) {
        if (f.size() < smallestList.size()) {
          smallestList = f;
        }
      }
      compareList.add(smallestList);
      frontierListCopy.remove(smallestList);
      for (IntegerFormula f : toCompareWith) {
        comparable = false;
        for (List<IntegerFormula> g : compareList) {
          for (IntegerFormula h : g) {

            if (h.equals(f)) {

              comparable = true;
            }
          }

        }
        if (comparable == false) {
          break;
        }
      }

    }
    if (comparable == false) {
      isIncomparable = true;
    }
    return isIncomparable;
  }

}
