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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointRoundingModeFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;
import com.google.common.base.Splitter;
import java.util.Map;
import org.sosy_lab.java_smt.api.Formula;




@SuppressWarnings("rawtypes")
public class DomainSpecificAbstraction<T> {
  protected final FormulaManagerView fmgr;
  protected FormulaManagerView oldFmgr;
  private String[] arrayVariablesThatAreUsedInBothParts;
  private String[] arrayVariablesThatAreNotUsedInBothParts;
  //private String[] arrayVariablesForFormulas;
  private List<BooleanFormula> formulas;
  //private Interpolator<T> myInterpolator;
  LogManager logger;
  private HashMap<String, List<FormulaType>> fullLatticeNamesTypes = new HashMap<>();
  private HashMap<String, FormulaType> latticeNamesTypes = new HashMap<>();
  public DomainSpecificAbstraction(/*ShutdownNotifier pShutdownNotifier, */
                                   FormulaManagerView pFmgr, /*BooleanFormulaManager pBfmgr, */
                                   FormulaManagerView oldFmgr0, /*Interpolator<T> pTInterpolator,
                                    */ LogManager pLogger) {
    fmgr = pFmgr;
    oldFmgr = oldFmgr0;
    //myInterpolator = pTInterpolator;
    logger = pLogger;
  }


@SuppressWarnings({"rawtypes", "unchecked"})
  public List<BooleanFormula> domainSpecificAbstractionsCheck(Solver mySolver,
                                                              List<BooleanFormula> oldFormulas)
      throws SolverException, InterruptedException {

    /*@SuppressWarnings("unchecked")
    InterpolatingProverEnvironment<BooleanFormula> myEnvironment =
        (InterpolatingProverEnvironment<BooleanFormula>) myInterpolator
            .newEnvironment(); */
  logger.log(Level.WARNING, "Entering domainSpecificAbstractionsCheck: ");
        List<BooleanFormula> interpolants = Lists.newArrayListWithExpectedSize(oldFormulas.size()
            - 1);
        // running the algorithm for every formula with its successor
    for (int it = 0; it < oldFormulas.size() - 1; it = it + 1) {
      BooleanFormula oldInterpolant;
      logger.log(Level.WARNING, "old Formulas: " + oldFormulas.get(it) + oldFormulas.get(it + 1));
      formulas = Lists.newArrayListWithExpectedSize(oldFormulas.size
          ());
      final List<Set<String>> variablesInFormulas =
          Lists.newArrayListWithExpectedSize(formulas.size());

      final List<Map<String, Formula>> variableTypes = Lists.newArrayListWithExpectedSize(oldFormulas.size()
          - 1);
      if (it == 0) {
        formulas.add(oldFormulas.get(it));
        formulas.add(oldFormulas.get(it + 1));
        //variablesInFormulas.add(oldFmgr.extractVariableNames(oldFormulas.get(it)));
       // variableTypes.add(oldFmgr.extractVariables(oldFormulas.get(it)));
      }
      else {
        oldInterpolant = oldFmgr.translateFrom(interpolants.get(it - 1), fmgr);
        formulas.add(oldInterpolant);
        formulas.add(oldFormulas.get(it));
        formulas.add(oldFormulas.get(it + 1));
        variablesInFormulas.add(oldFmgr.extractVariableNames(oldInterpolant));
        variableTypes.add(oldFmgr.extractVariables(oldInterpolant));
        logger.log(Level.WARNING, "Variables in Formulas after adding old interpolant:" +
            variablesInFormulas.toString());
      }

      for (int i = it; i < oldFormulas.size(); i++) {
        variablesInFormulas.add(oldFmgr.extractVariableNames(oldFormulas.get(i)));
        variableTypes.add(oldFmgr.extractVariables(oldFormulas.get(i)));
        logger.log(Level.WARNING, "Variables in Formulas after adding oldFormulas[:" + i + "] "
            + variablesInFormulas.toString());
      }
      // extracting the variables that have to be renamed - make external function?
      /*List<List<IntegerFormula>> frontierList = Lists.newArrayListWithExpectedSize(formulas.size
          ()); */
      List<List<Formula>> frontierList = Lists.newArrayListWithExpectedSize(formulas.size
          ());
      Set<String> variables1 = Sets.newHashSet();
      Set<String> variables2 = Sets.newHashSet();
      logger.log(Level.WARNING, "Formulas:");
      if (formulas != null) {
        for (int i = 0; i < formulas.size(); i++) {
          logger.log(Level.WARNING, i + ". Formel" + formulas.get(i).toString());
        }
      }
      logger.log(Level.WARNING, "Variables in Formulas:");
      if (variablesInFormulas != null) {
        for (int i = 0; i < variablesInFormulas.size(); i++) {
          logger.log(Level.WARNING, i + ". VariablesInFormulas" + variablesInFormulas.get(i).toString());
        }
      }
     // for (int i = 0; i < it + 1; i++) {
      if (it == 0) {
        logger.log(Level.WARNING, "it is equals to 0");
        variables1 = variablesInFormulas.get(0);
        for (int i = 1; i < variablesInFormulas.size(); i++) {
          for (String f : variablesInFormulas.get(i)) {
            variables2.add(f);
          }
        }
      }
      else {
        logger.log(Level.WARNING, "it is equals to > 0");
        for (String f : variablesInFormulas.get(0)) {
          variables1.add(f);
        }
        for (String f : variablesInFormulas.get(1)) {
          variables1.add(f);
        }
        for (int i = 2; i < variablesInFormulas.size(); i++) {
          for (String f : variablesInFormulas.get(i)) {
            variables2.add(f);
          }
        }
      }
     // }
    /*  for (int i = 2; i < variablesInFormulas.size(); i++) {
        for (String f : variablesInFormulas.get(i)) {
          variables2.add(f);
        }
      } */
      Set<String> variablesThatAreUsedInBothParts = Sets.intersection(variables1, variables2)
          .immutableCopy();
      Set<String> variablesThatAreNotUsedInBothParts = Sets.difference(variables1, variables2)
          .immutableCopy();
      HashMap<String, FormulaType> variablesUsedInBothPartsClasses = new HashMap<>();
      int m = 0;
      logger.log(Level.WARNING, "Variables1: " +
          variables1.toString());
      logger.log(Level.WARNING, "Variables2: " +
          variables2.toString());
      logger.log(Level.WARNING, "Variables That Are Used In Both Parts: " +
          variablesThatAreUsedInBothParts.toString());
   /*   for (Map<String, Formula> s : variableTypes) {
        s.get(variablesThatAreUsedInBothParts)
      }  */
/*   for(int i = 0; i < arrayVariablesThatAreUsedInBothParts.length; i++){
     Formula helperFormula;
     FormulaType helperFormulaType;
     for (Map<String, Formula> f : variableTypes){
       helperFormula = f.get(arrayVariablesThatAreUsedInBothParts[i]);
       helperFormulaType = oldFmgr.getFormulaType(helperFormula);
       variablesUsedInBothPartsClasses.put(arrayVariablesThatAreUsedInBothParts[i],
           helperFormulaType);
     }
   } */

 /*
      for (Iterator<String> myIt = variablesThatAreUsedInBothParts.iterator(); myIt.hasNext(); ) {
        String lookupItem = myIt.next();
          for (Map<String, Formula> f : variableTypes){
            Formula helperFormula = f.get(lookupItem);


            if (helperFormula.getClass().toString().contains("IntegerFormula")){
              variablesUsedInBothPartsClasses.put(lookupItem, "IntegerFormula");
            } else if (helperFormula.getClass().toString().contains("RationalFormula")){
              variablesUsedInBothPartsClasses.put(lookupItem, "RationalFormula");
            } */ /* else if (helperFormula.getClass().toString().contains("BooleanFormula")) {
              variablesUsedInBothPartsClasses.put(lookupItem, "BooleanFormula");
            } */
  /*            else if (helperFormula.getClass().toString().contains("FloatingPointRoundingMode")){
                variablesUsedInBothPartsClasses.put(lookupItem, "FloatingPointRoundingMode");
              }

          }
      } */

      String[] arrayVariables1 = variables1.toArray(new String[variables1.size()]);
      String[] arrayVariables2 = variables2.toArray(new String[variables2.size()]);
      arrayVariablesThatAreUsedInBothParts = variablesThatAreUsedInBothParts.toArray(new
          String[variablesThatAreUsedInBothParts.size
          ()]);
      arrayVariablesThatAreNotUsedInBothParts = variablesThatAreNotUsedInBothParts.toArray(new
          String[variablesThatAreNotUsedInBothParts.size
          ()]);



      for(int i = 0; i < arrayVariablesThatAreUsedInBothParts.length; i++){
        Formula helperFormula;
        FormulaType helperFormulaType;
        for (Map<String, Formula> f : variableTypes){
          helperFormula = f.get(arrayVariablesThatAreUsedInBothParts[i]);
          if (helperFormula != null) {
            helperFormulaType = oldFmgr.getFormulaType(helperFormula);
            variablesUsedInBothPartsClasses.put(arrayVariablesThatAreUsedInBothParts[i],
                helperFormulaType);
          }
        }
      }
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
      logger.log(Level.WARNING, "Allocating lattice: ");
     /* boolean[][] lattice = new boolean[(int) Math.pow(2, (arrayVariablesThatAreUsedInBothParts
          .length))][(int) Math.pow(2,
          (arrayVariablesThatAreUsedInBothParts.length))]; */
     // String[] latticeNames = new String[(arrayVariablesThatAreUsedInBothParts.length) + 1];
      String[] latticeNames = new String[(arrayVariablesThatAreUsedInBothParts.length)];
  /*    String[] powersetBase = new String[arrayVariablesThatAreUsedInBothParts.length];
      double d = arrayVariablesThatAreUsedInBothParts
          .length;
      double e = 2;
      logger.log(Level.WARNING, "Allocating full Lattice Names: ");
      String[] fullLatticeNames = new String[(int) Math.pow(e, d)
          ]; */
     //
      //
      // HashMap<String, FormulaType> latticeNamesTypes = new HashMap<String, FormulaType>();

      /* HashMap<String, List<FormulaType>> fullLatticeNamesTypes = new HashMap<String,
          List<FormulaType>>(); */

      //latticeNames[0] = "root";
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
          FormulaType resultType1 = variablesUsedInBothPartsClasses.get
              (arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variablesUsedInBothPartsClasses.get
              (arrayVariablesThatAreUsedInBothParts[i + 1]);
          if (!resultType1.equals(resultType2)){
            logger.log(Level.WARNING, "resulttype1 does not equal resulttype2: ");
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
            Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
            latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 1] =
                arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
            latticeNamesTypes.put(latticeNames[i], resultType1);
            latticeNamesTypes.put(latticeNames[i + 1], resultType2);
          }
          else if (resultType1.isArrayType() && resultType2.isArrayType()){
            ArrayFormulaType resultType1Array = (ArrayFormulaType) resultType1;
            FormulaType indexType1 = resultType1Array.getIndexType();
            FormulaType elementType1 = resultType1Array.getElementType();
            ArrayFormulaType resultType2Array = (ArrayFormulaType) resultType2;
            FormulaType indexType2 = resultType2Array.getIndexType();
            FormulaType elementType2 = resultType2Array.getElementType();
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
            Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.getArrayType(elementType2, indexType2),
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.getArrayType(elementType2, indexType2),
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
            latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 1] =
                arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
            latticeNamesTypes.put(latticeNames[i], resultType1);
            latticeNamesTypes.put(latticeNames[i + 1], resultType1);
          }
          else if (resultType1.isBooleanType() && resultType2.isBooleanType()){
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
            BooleanFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
            latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 1] =
                arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
            latticeNamesTypes.put(latticeNames[i], resultType1);
            latticeNamesTypes.put(latticeNames[i + 1], resultType1);
          } else if (resultType1.isBitvectorType() &&
              resultType2.isBitvectorType()) {
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
            BitvectorFormula helperFormula1, helperFormula2, helperFormula3,
                helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize
                    (1),
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
            latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 1] =
                arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
            latticeNamesTypes.put(latticeNames[i], resultType1);
            latticeNamesTypes.put(latticeNames[i + 1], resultType1);
          } else if (resultType1 == resultType2 && !(resultType1.isIntegerType()) && !
              (resultType1.isFloatingPointRoundingModeType()) && !(resultType1.isRationalType())){
              relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                  arrayVariablesThatAreUsedInBothParts[i] + "'";
              relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                  arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
              Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                  helperFormula7, helperFormula8;
              BooleanFormula helperFormula5, helperFormula6;
              helperFormula1 = fmgr.makeVariable(resultType1,
                  arrayVariablesThatAreUsedInBothParts[i]);
              helperFormula2 = fmgr.makeVariable(resultType2,
                  arrayVariablesThatAreUsedInBothParts[i + 1]);
              helperFormula7 = fmgr.makeVariable(resultType1,
                  arrayVariablesThatAreUsedInBothParts[i] + "'");
              helperFormula8 = fmgr.makeVariable(resultType2,
                  arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
              helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
              helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
              relationAbstraction1Formula.add(helperFormula6);
              relationAbstraction1Formula.add(helperFormula5);
              latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
              latticeNames[i + 1] =
                  arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
              latticeNamesTypes.put(latticeNames[i], resultType1);
              latticeNamesTypes.put(latticeNames[i + 1], resultType2);
          }
          else {
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'" + " - " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
        /*  FormulaType resultType1 = variablesUsedInBothPartsClasses.get
              (arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variablesUsedInBothPartsClasses.get
          (arrayVariablesThatAreUsedInBothParts[i + 1]); */
            if (resultType1.isIntegerType() && resultType2.isIntegerType()) {
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
            } else if (resultType1.isRationalType() && resultType2.isRationalType()) {
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
            } else if (resultType1.isFloatingPointRoundingModeType() &&
                resultType2.isFloatingPointRoundingModeType()) {
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
            }
            latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 1] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
                arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
            latticeNamesTypes.put(latticeNames[i], resultType1);
            latticeNamesTypes.put(latticeNames[i + 1], resultType1);
           /* latticeNamesTypes[i + 1] = resultType1;
            latticeNamesTypes[i + 2] = resultType2; */
          }
        }
        for (int i = 0; i < arrayVariablesThatAreUsedInBothParts.length; i = i + 2) {
          FormulaType resultType1 = variablesUsedInBothPartsClasses.get
              (arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variablesUsedInBothPartsClasses.get
              (arrayVariablesThatAreUsedInBothParts[i + 1]);
          if (!resultType1.equals(resultType2)){
            logger.log(Level.WARNING, "resulttype1 does not equal resulttype2: ");
            relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''";
            relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
            Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          }
          else if (resultType1.isArrayType() && resultType2.isArrayType()){
            ArrayFormulaType resultType1Array = (ArrayFormulaType) resultType1;
            FormulaType indexType1 = resultType1Array.getIndexType();
            FormulaType elementType1 = resultType1Array.getElementType();
            ArrayFormulaType resultType2Array = (ArrayFormulaType) resultType2;
            FormulaType indexType2 = resultType2Array.getIndexType();
            FormulaType elementType2 = resultType2Array.getElementType();
            relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''";
            relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
            Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.getArrayType(elementType2, indexType2),
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.getArrayType(elementType2, indexType2),
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          }
          else if (resultType1.isBooleanType() && resultType2.isBooleanType()){
            relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''";
            relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
            BooleanFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          } else if (resultType1.isBitvectorType() &&
              resultType2.isBitvectorType()) {
            relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''";
            relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
            BitvectorFormula helperFormula1, helperFormula2, helperFormula3,
                helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize
                    (1),
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          }else if (resultType1 == resultType2 && !(resultType1.isIntegerType()) && !
              (resultType1.isFloatingPointRoundingModeType()) && !(resultType1.isRationalType())){
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
            Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
          }
          else {
          relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
              arrayVariablesThatAreUsedInBothParts[i] + "''";
          relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
              arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
              arrayVariablesThatAreUsedInBothParts[i] + "''" + " - " +
              arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
         /* FormulaType resultType1 = variablesUsedInBothPartsClasses.get
          (arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variablesUsedInBothPartsClasses.get
          (arrayVariablesThatAreUsedInBothParts[i + 1]); */
          if (resultType1.isIntegerType() && resultType2.isIntegerType()) {
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
          } else if (resultType1.isRationalType() && resultType2.isRationalType()) {
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
          } else if (resultType1.isFloatingPointRoundingModeType() &&
              resultType2.isFloatingPointRoundingModeType()){
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
          }
        }}
      } else {
        int i;
        for (i = 0; i < arrayVariablesThatAreUsedInBothParts.length - 1; i = i + 2) {
          FormulaType resultType1 = variablesUsedInBothPartsClasses.get
              (arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variablesUsedInBothPartsClasses.get
              (arrayVariablesThatAreUsedInBothParts[i + 1]);
          if (!resultType1.equals(resultType2)){
            logger.log(Level.WARNING, "resulttype1 does not equal resulttype2: ");
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
            Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
            latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 1] =
                arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
            latticeNamesTypes.put(latticeNames[i], resultType1);
            latticeNamesTypes.put(latticeNames[i + 1], resultType2);
          }
          else if (resultType1.isArrayType() && resultType2.isArrayType()){
            ArrayFormulaType resultType1Array = (ArrayFormulaType) resultType1;
            FormulaType indexType1 = resultType1Array.getIndexType();
            FormulaType elementType1 = resultType1Array.getElementType();
            ArrayFormulaType resultType2Array = (ArrayFormulaType) resultType2;
            FormulaType indexType2 = resultType2Array.getIndexType();
            FormulaType elementType2 = resultType2Array.getElementType();
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
            Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.getArrayType(elementType2, indexType2),
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.getArrayType(elementType2, indexType2),
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
            latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 1] =
                arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
            latticeNamesTypes.put(latticeNames[i], resultType1);
            latticeNamesTypes.put(latticeNames[i + 1], resultType1);
          }
          else if (resultType1.isBooleanType() && resultType2.isBooleanType()) {
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
            BooleanFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
            latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 1] =
                arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
            latticeNamesTypes.put(latticeNames[i], resultType1);
            latticeNamesTypes.put(latticeNames[i + 1], resultType1);
          } else if (resultType1.isBitvectorType() &&
              resultType2.isBitvectorType()) {
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
            BitvectorFormula helperFormula1, helperFormula2, helperFormula3,
                helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize
                    (1),
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
            latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 1] =
                arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
            latticeNamesTypes.put(latticeNames[i], resultType1);
            latticeNamesTypes.put(latticeNames[i + 1], resultType1);
          } else if (resultType1 == resultType2 && !(resultType1.isIntegerType()) && !
              (resultType1.isFloatingPointRoundingModeType()) && !(resultType1.isRationalType())){
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
            Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i] + "'");
            helperFormula8 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
            latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 1] =
                arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
            latticeNamesTypes.put(latticeNames[i], resultType1);
            latticeNamesTypes.put(latticeNames[i + 1], resultType2);
          }

          else {
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "'" + " - " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "'";
         /* FormulaType resultType1 = variablesUsedInBothPartsClasses.get
          (arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variablesUsedInBothPartsClasses.get
          (arrayVariablesThatAreUsedInBothParts[i +
          1]); */
            if (resultType1.isIntegerType() && resultType2.isIntegerType()) {
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
            } else if (resultType1.isRationalType() && resultType2.isRationalType()) {
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
            } else if (resultType1.isFloatingPointRoundingModeType() &&
                resultType2.isFloatingPointRoundingModeType()) {
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
            }
            latticeNames[i] = arrayVariablesThatAreUsedInBothParts[i];
            latticeNames[i + 1] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
                arrayVariablesThatAreUsedInBothParts[i + 1];
            logger.log(Level.WARNING, "LatticeNames[" + (i) + "] = " + latticeNames[i + 1]);
            logger.log(Level.WARNING, "LatticeNames[" + (i+1) + "] = " + latticeNames[i + 2]);
            latticeNamesTypes.put(latticeNames[i], resultType1);
            latticeNamesTypes.put(latticeNames[i + 1], resultType1);
         /* latticeNamesTypes[i + 1] = resultType1;
          latticeNamesTypes[i + 2] = resultType2; */
          }
        }
        for (i = 0; i < arrayVariablesThatAreUsedInBothParts.length - 1; i = i + 2) {
          FormulaType resultType1 = variablesUsedInBothPartsClasses.get
              (arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variablesUsedInBothPartsClasses.get
              (arrayVariablesThatAreUsedInBothParts[i + 1]);
          if (!resultType1.equals(resultType2)){
            logger.log(Level.WARNING, "resulttype1 does not equal resulttype2: ");
            relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''";
            relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
            Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          }
          else if (resultType1.isArrayType() && resultType2.isArrayType()){
            ArrayFormulaType resultType1Array = (ArrayFormulaType) resultType1;
            FormulaType indexType1 = resultType1Array.getIndexType();
            FormulaType elementType1 = resultType1Array.getElementType();
            ArrayFormulaType resultType2Array = (ArrayFormulaType) resultType2;
            FormulaType indexType2 = resultType2Array.getIndexType();
            FormulaType elementType2 = resultType2Array.getElementType();
            relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''";
            relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
            Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.getArrayType(elementType2, indexType2),
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.getArrayType(elementType2, indexType2),
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          }
          else if (resultType1.isBooleanType() && resultType2.isBooleanType()) {
            relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''";
            relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
            BooleanFormula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.BooleanType,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          }
          else if (resultType1.isBitvectorType() &&
              resultType2.isBitvectorType()) {
            relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''";
            relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
            BitvectorFormula helperFormula1, helperFormula2, helperFormula3,
                helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize
                    (1),
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction2Formula.add(helperFormula6);
            relationAbstraction2Formula.add(helperFormula5);
          } else if (resultType1 == resultType2 && !(resultType1.isIntegerType()) && !
              (resultType1.isFloatingPointRoundingModeType()) && !(resultType1.isRationalType())){
            relationAbstraction1[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''";
            relationAbstraction1[i + 1] = arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
            Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
                helperFormula7, helperFormula8;
            BooleanFormula helperFormula5, helperFormula6;
            helperFormula1 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i]);
            helperFormula2 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1]);
            helperFormula7 = fmgr.makeVariable(resultType1,
                arrayVariablesThatAreUsedInBothParts[i] + "''");
            helperFormula8 = fmgr.makeVariable(resultType2,
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''");
            helperFormula5 = fmgr.makeEqual(helperFormula2, helperFormula8);
            helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
            relationAbstraction1Formula.add(helperFormula6);
            relationAbstraction1Formula.add(helperFormula5);
          }
          else {
            relationAbstraction2[i] = arrayVariablesThatAreUsedInBothParts[i] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''";
            relationAbstraction2[i + 1] = arrayVariablesThatAreUsedInBothParts[i] + " - " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + " = " +
                arrayVariablesThatAreUsedInBothParts[i] + "''" + " - " +
                arrayVariablesThatAreUsedInBothParts[i + 1] + "''";
          /*FormulaType resultType1 = variablesUsedInBothPartsClasses.get
              (arrayVariablesThatAreUsedInBothParts[i]);
          FormulaType resultType2 = variablesUsedInBothPartsClasses.get
          (arrayVariablesThatAreUsedInBothParts[i + 1]); */
            if (resultType1.isIntegerType() && resultType2.isIntegerType()) {
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
            } else if (resultType1.isRationalType() && resultType2.isRationalType()) {
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
            } else if (resultType1.isFloatingPointRoundingModeType() &&
                resultType2.isFloatingPointRoundingModeType()) {
              FloatingPointRoundingModeFormula helperFormula1, helperFormula2, helperFormula3,
                  helperFormula4,
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
            }
          }
        }
        relationAbstraction1[arrayVariablesThatAreUsedInBothParts.length - 1] =
            arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]
                + ""
                + " = " + arrayVariablesThatAreUsedInBothParts
                [arrayVariablesThatAreUsedInBothParts.length - 1] + "'";
        FormulaType resultType1 = variablesUsedInBothPartsClasses.get
            (arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]);
        FormulaType resultType2 = variablesUsedInBothPartsClasses.get
            (arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]);
       /* if (resultType1.isBooleanType()) {
          BooleanFormula helperFormula1, helperFormula2, helperFormula3;
          helperFormula1 = fmgr.makeVariable(FormulaType.BooleanType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula2 =
              fmgr.makeVariable(FormulaType.BooleanType, arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
          helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
          relationAbstraction1Formula.add(helperFormula3);
       IntegerFormula helperFormula1, helperFormula2;
       BooleanFormula helperFormula3;
       helperFormula1 = fmgr.makeVariable(FormulaType.IntegerType,
           arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]);
        helperFormula2 =
            fmgr.makeVariable(FormulaType.IntegerType, arrayVariablesThatAreUsedInBothParts
                [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
        helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
        relationAbstraction1Formula.add(helperFormula3);

        } else */
        if (!resultType1.equals(resultType2)){
          logger.log(Level.WARNING, "resulttype1 does not equal resulttype2: ");
          Formula helperFormula1, helperFormula2;
          BooleanFormula helperFormula3;
          helperFormula1 = fmgr.makeVariable(resultType1,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula2 =
              fmgr.makeVariable(resultType2, arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
          helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
          relationAbstraction1Formula.add(helperFormula3);
        }
        else if (resultType1.isArrayType() && resultType2.isArrayType()){
          ArrayFormulaType resultType1Array = (ArrayFormulaType) resultType1;
          FormulaType indexType1 = resultType1Array.getIndexType();
          FormulaType elementType1 = resultType1Array.getElementType();
          ArrayFormulaType resultType2Array = (ArrayFormulaType) resultType2;
          FormulaType indexType2 = resultType2Array.getIndexType();
          FormulaType elementType2 = resultType2Array.getElementType();
          Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
              helperFormula7, helperFormula8;
          BooleanFormula helperFormula5, helperFormula6;
          helperFormula1 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length -
                  1]);
          helperFormula7 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]
                  + "'");
          helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
          relationAbstraction1Formula.add(helperFormula6);
        }
       else if (resultType1.isIntegerType() && resultType2.isIntegerType()){
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
        } else if (resultType1.isIntegerType() && resultType2.isIntegerType()){
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
        } else if (resultType1.isFloatingPointRoundingModeType() && resultType2.isFloatingPointRoundingModeType()){
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
        } else if (resultType1.isBooleanType() && resultType2.isBooleanType()){
          BooleanFormula helperFormula1, helperFormula2;
          BooleanFormula helperFormula3;
          helperFormula1 = fmgr.makeVariable(FormulaType.BooleanType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula2 =
              fmgr.makeVariable(FormulaType.BooleanType, arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
          helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
          relationAbstraction1Formula.add(helperFormula3);
        } else if (resultType1.isBitvectorType() && resultType2.isBitvectorType()){
          BitvectorFormula helperFormula1, helperFormula2;
          BooleanFormula helperFormula3;
          helperFormula1 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula2 =
              fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                  arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
          helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
          relationAbstraction1Formula.add(helperFormula3);
        }  else if (resultType1 == resultType2){
          Formula helperFormula1, helperFormula2;
          BooleanFormula helperFormula3;
          helperFormula1 = fmgr.makeVariable(resultType1,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula2 =
              fmgr.makeVariable(resultType2, arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
          helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
          relationAbstraction1Formula.add(helperFormula3);
        }
        relationAbstraction2[arrayVariablesThatAreUsedInBothParts.length - 1] =
            arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]
                + ""
                + " = " + arrayVariablesThatAreUsedInBothParts
                [arrayVariablesThatAreUsedInBothParts.length - 1] + "''";
      /*  FormulaType resultType2 = variablesUsedInBothPartsClasses.get
            (arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length -
            1]); */
      /*  if (resultType2.isBooleanType()) {  */
       /*   BooleanFormula helperFormula4, helperFormula5, helperFormula6;
          helperFormula4 = fmgr.makeVariable(FormulaType.BooleanType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula5 = fmgr.makeVariable(FormulaType.BooleanType,
              arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "''");
          helperFormula6 = fmgr.makeEqual(helperFormula4, helperFormula5);
          relationAbstraction2Formula.add(helperFormula6); */
      /*  IntegerFormula helperFormula4, helperFormula5;
        BooleanFormula helperFormula6;
        helperFormula4 = fmgr.makeVariable(FormulaType.IntegerType,
            arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                - 1]);
        helperFormula5 = fmgr.makeVariable(FormulaType.IntegerType,
            arrayVariablesThatAreUsedInBothParts
                [arrayVariablesThatAreUsedInBothParts.length - 1] + "''");
        helperFormula6 = fmgr.makeEqual(helperFormula4, helperFormula5);
        relationAbstraction2Formula.add(helperFormula6);
        } else */
        if (!resultType2.equals(resultType1)){
          logger.log(Level.WARNING, "resulttype1 does not equal resulttype2: ");
          Formula helperFormula4, helperFormula5;
          BooleanFormula helperFormula6;
          helperFormula4 = fmgr.makeVariable(resultType1,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula5 = fmgr.makeVariable(resultType2,
              arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "''");
          helperFormula6 = fmgr.makeEqual(helperFormula4, helperFormula5);
          relationAbstraction2Formula.add(helperFormula6);
        }
        else if (resultType1.isArrayType() && resultType2.isArrayType()){
          ArrayFormulaType resultType1Array = (ArrayFormulaType) resultType1;
          FormulaType indexType1 = resultType1Array.getIndexType();
          FormulaType elementType1 = resultType1Array.getElementType();
          ArrayFormulaType resultType2Array = (ArrayFormulaType) resultType2;
          FormulaType indexType2 = resultType2Array.getIndexType();
          FormulaType elementType2 = resultType2Array.getElementType();
          Formula helperFormula1, helperFormula2, helperFormula3, helperFormula4,
              helperFormula7, helperFormula8;
          BooleanFormula helperFormula5, helperFormula6;
          helperFormula1 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length -
                  1]);
          helperFormula7 = fmgr.makeVariable(FormulaType.getArrayType(elementType1, indexType1),
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1]
                  + "''");
          helperFormula6 = fmgr.makeEqual(helperFormula1, helperFormula7);
          relationAbstraction2Formula.add(helperFormula6);
        }
      else if (resultType2.isIntegerType() && resultType1.isIntegerType()){
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
        } else if (resultType2.isRationalType() && resultType1.isRationalType()){
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
        } else if (resultType2.isFloatingPointRoundingModeType() && resultType1.isFloatingPointRoundingModeType()){
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
        } else if (resultType2.isBooleanType() && resultType1.isBooleanType()){
          BooleanFormula helperFormula4, helperFormula5;
          BooleanFormula helperFormula6;
          helperFormula4 = fmgr.makeVariable(FormulaType.BooleanType,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula5 = fmgr.makeVariable(FormulaType.BooleanType,
              arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "''");
          helperFormula6 = fmgr.makeEqual(helperFormula4, helperFormula5);
          relationAbstraction2Formula.add(helperFormula6);
        } else if (resultType1.isBitvectorType() && resultType2.isBitvectorType()){
          BitvectorFormula helperFormula1, helperFormula2;
          BooleanFormula helperFormula3;
          helperFormula1 = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula2 =
              fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(1),
                  arrayVariablesThatAreUsedInBothParts
                      [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
          helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
          relationAbstraction2Formula.add(helperFormula3);
        } else if (resultType1 == resultType2){
          Formula helperFormula1, helperFormula2;
          BooleanFormula helperFormula3;
          helperFormula1 = fmgr.makeVariable(resultType1,
              arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length
                  - 1]);
          helperFormula2 =
              fmgr.makeVariable(resultType2, arrayVariablesThatAreUsedInBothParts
                  [arrayVariablesThatAreUsedInBothParts.length - 1] + "'");
          helperFormula3 = fmgr.makeEqual(helperFormula1, helperFormula2);
          relationAbstraction1Formula.add(helperFormula3);
        }
        latticeNames[latticeNames.length - 1] =
            arrayVariablesThatAreUsedInBothParts[arrayVariablesThatAreUsedInBothParts.length - 1];
        latticeNamesTypes.put(latticeNames[latticeNames.length - 1], resultType1);
        logger.log(Level.WARNING, "LatticeNames[" + (latticeNames.length - 1) + "] = " +
            latticeNames[latticeNames.length - 1]);
        //latticeNamesTypes[latticeNamesTypes.length - 1] = resultType1;
      }

      //generating the nodes of the lattice
     /* FormulaType[] powersetBaseTypes = new FormulaType[arrayVariablesThatAreUsedInBothParts
          .length]; */
   /*   HashMap<String, FormulaType> powersetBaseTypes = new HashMap<>();
      for (int k = 1; k < latticeNames.length; k++){
        powersetBase[k - 1] = latticeNames[k];
        powersetBaseTypes.put(latticeNames[k], latticeNamesTypes.get(latticeNames[k]));
      }

     for (int i = 0; i < fullLatticeNames.length; i++){
       for (int j = 0; j < powersetBase.length; j++){
         if ((i & (1 << j)) == 0){
           if (fullLatticeNames[i] == null) {
             fullLatticeNames[i] = powersetBase[j];
             //fullLatticeNamesTypes[i] = powersetBaseTypes[j];
             List<FormulaType> helperList = Lists.newArrayListWithExpectedSize
                 (formulas.size());
             helperList.add(powersetBaseTypes.get(fullLatticeNames[i]));
             fullLatticeNamesTypes.put(fullLatticeNames[i], helperList);
           }
           else{
             List<FormulaType> helperList = fullLatticeNamesTypes.get(fullLatticeNames[i]);
             fullLatticeNames[i] = fullLatticeNames[i] + " ," + powersetBase[j];
             //fullLatticeNamesTypes[i] = fullLatticeNamesTypes[i] + " ," + powersetBaseTypes[j];
             helperList.add(powersetBaseTypes.get(powersetBase[j]));
             fullLatticeNamesTypes.put(fullLatticeNames[i], helperList);
           }
         }
       }
     }

      for (int i = 0; i < fullLatticeNames.length; i++){
        if (fullLatticeNames[i] == null){
          fullLatticeNames[i] = "root";
        }
      }

      for (int i = 0; i < fullLatticeNames.length; i++){
        logger.log(Level.WARNING, fullLatticeNames[i]);
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
      } */
    /*  String finalnode = new String();
      for (int i = 1; i < latticeNames.length - 1; i++){
        if (latticeNames[i] != null) {
          if (!finalnode.isEmpty()) {
            finalnode = finalnode + " ," + latticeNames[i];
          } else {
            finalnode = latticeNames[i];
          }
        }
      } */
     // latticeNames[latticeNames.length - 1] = finalnode;
      //arrayVariablesForFormulas = arrayVariablesThatAreUsedInBothParts;
      FirstPartRenamingFct renamer1 = new FirstPartRenamingFct
          (arrayVariablesThatAreUsedInBothParts, arrayVariablesThatAreNotUsedInBothParts);
      ScndPartRenamingFct renamer2 = new ScndPartRenamingFct
          (arrayVariablesThatAreUsedInBothParts, arrayVariablesThatAreNotUsedInBothParts);
      BooleanFormula firstPart;
      BooleanFormula scndPart;
      if (it == 0) {
        firstPart = formulas.get(0);
        scndPart = formulas.get(1);
      } else {
        firstPart = formulas.get(1);
        scndPart = formulas.get(2);
      }
      BooleanFormula firstPartChanged;
      BooleanFormula scndPartChanged;
      if (it == 0) {
        firstPartChanged = oldFmgr.renameFreeVariablesAndUFs(firstPart, renamer1);
        scndPartChanged = oldFmgr.renameFreeVariablesAndUFs(scndPart, renamer2);
      }
      else {
        firstPartChanged = oldFmgr.renameFreeVariablesAndUFs(firstPart, renamer1);
        scndPartChanged = oldFmgr.renameFreeVariablesAndUFs(scndPart, renamer2);
      }
      List<BooleanFormula> changed_formulas =
          Lists.newArrayListWithExpectedSize(formulas.size() - 1);
      List<BooleanFormula> changed_formulas_rest1 =
          Lists.newArrayListWithExpectedSize(formulas.size() - 1);
      List<BooleanFormula> changed_formulas_rest2 =
          Lists.newArrayListWithExpectedSize(formulas.size() - 1);
      changed_formulas.add(firstPartChanged);
      changed_formulas.add(scndPartChanged);

      BooleanFormula helperFormula1;
      BooleanFormula helperFormula2;

      firstPartChanged = fmgr.translateFrom(firstPartChanged, oldFmgr);
      scndPartChanged = fmgr.translateFrom(scndPartChanged, oldFmgr);
     /* for (int i = 0; i < it; i++){
        BooleanFormula addFormula = oldFormulas.get(i);
        BooleanFormula changedFormula = oldFmgr.renameFreeVariablesAndUFs(addFormula, renamer1);
        changedFormula = fmgr.translateFrom(changedFormula, oldFmgr);
        changed_formulas_rest1.add(changedFormula);
      } */
     if (it != 0){
       BooleanFormula addFormula = interpolants.get(it - 1);
       BooleanFormula changedFormula = fmgr.renameFreeVariablesAndUFs(addFormula, renamer1);
       changed_formulas_rest1.add(changedFormula);
     }
      for (int i = it + 2; i < oldFormulas.size(); i++){
        BooleanFormula addFormula = oldFormulas.get(i);
        BooleanFormula changedFormula = oldFmgr.renameFreeVariablesAndUFs(addFormula, renamer2);
        changedFormula = fmgr.translateFrom(changedFormula, oldFmgr);
        changed_formulas_rest2.add(changedFormula);
      }


      boolean abstractionFeasible = false;
      boolean isIncomparable = false;
      helperFormula1 = firstPartChanged;
      helperFormula2 = scndPartChanged;
      String latticenames_h = new String();

      logger.log(Level.WARNING, "Showing LatticeNames: ");
      for (int h = 0; h < latticeNames.length; h++) {
        logger.log(Level.WARNING, latticeNames[h]);
      }
      logger.log(Level.WARNING, "RelationAbstraction1: ");
      for (int h = 0; h < relationAbstraction1.length; h++) {
        logger.log(Level.WARNING, relationAbstraction1[h]);
      }
      logger.log(Level.WARNING, "RelationAbstraction2: ");
      for (int h = 0; h < relationAbstraction2.length; h++) {
        logger.log(Level.WARNING, relationAbstraction2[h]);
      }
      for (int h = 0; h < /*fullLatticeNames.length */ latticeNames.length; h++) {
       /* helperFormula1 = firstPartChanged;
        helperFormula2 = scndPartChanged; */
     //   Iterable<String> splitOperator = Splitter.on(" ,").split(/*fullLatticeNames[h]*/
      //      latticeNames[h]);
      //  for (String s : splitOperator) {
          for (int k = 0; k < relationAbstraction1.length; k++) {
            if (relationAbstraction1[k] != null && latticeNames[h] != null) {
              if ((relationAbstraction1[k]).contains(/*s*/ latticeNames[h] + " = ")) {
                helperFormula1 = fmgr.makeAnd(helperFormula1, relationAbstraction1Formula.get
                    (k));
                logger.log(Level.WARNING, "Updated helperformula1: " + helperFormula1.toString());
                if (latticenames_h.isEmpty() || (latticenames_h == null)) {
                  latticenames_h = latticeNames[h];
                  logger.log(Level.WARNING, "Latticenames_h: " + latticenames_h);
                } else {
                  latticenames_h = latticenames_h + " ," + latticeNames[h];
                  logger.log(Level.WARNING, "Latticenames_h: " + latticenames_h);
                }

              }
            }
            if (relationAbstraction2[k] != null && latticeNames[h] != null) {
              if ((relationAbstraction2[k]).contains(/*s*/ latticeNames[h] + " = ")) {
                helperFormula2 = fmgr.makeAnd(helperFormula2, relationAbstraction2Formula.get
                    (k));
                logger.log(Level.WARNING, "Updated helperformula2: " + helperFormula2.toString());

              }
            }
          }
       // }

        if (!latticenames_h.isEmpty() && !(latticenames_h == null)) {
          BooleanFormula toCheckFormula = fmgr.makeAnd(helperFormula1, helperFormula2);
          List<BooleanFormula> toCheckFormulaList =
              Lists.newArrayListWithExpectedSize(formulas.size() - 1);
          for (BooleanFormula f : changed_formulas_rest1) {
            toCheckFormulaList.add(f);
          }
          toCheckFormulaList.add(toCheckFormula);
          for (BooleanFormula f : changed_formulas_rest2) {
            toCheckFormulaList.add(f);
          }
          BlockFormulas toCheckFormulaBlocked = new BlockFormulas(toCheckFormulaList);

          abstractionFeasible = prove(toCheckFormulaBlocked, mySolver);
          if (abstractionFeasible) {
          /*List<List<IntegerFormula>> */
            List<List<Formula>> frontierListCopy = Lists
                .newArrayListWithExpectedSize(oldFormulas.size() - 1);
            for (/*List<IntegerFormula> */ List<Formula> s : frontierList) {
              frontierListCopy.add(s);
            }
            logger.log(Level.WARNING, "Comparability Check: Latticenames_h: " + latticenames_h);
            isIncomparable = checkComparability(frontierListCopy, /*fullLatticeNames[h] */
                latticenames_h, latticeNames);

            if (isIncomparable) {
            /*List<IntegerFormula> */
              List<Formula> new_frontier_elem = maximise(firstPartChanged,
                  scndPartChanged,
                  relationAbstraction1,
                  relationAbstraction2, relationAbstraction1Formula,
                  relationAbstraction2Formula /*lattice, fullLatticeNames, */, latticeNames, /*h,*/
                  latticenames_h,
                  mySolver);
              frontierList.add(new_frontier_elem);
            }
          }
        }
      }
  /*    helperFormula1 = firstPartChanged;
      helperFormula2 = scndPartChanged;


      for ( List<Formula> x : frontierList) {

        for ( Formula y : x) {

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
      } */

      //interpolationFormula = fmgr.makeAnd(helperFormula1, helperFormula2);
      List<BooleanFormula> interpolationFormulaList =
          Lists.newArrayListWithExpectedSize(formulas.size() - 1);

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
    /*  for (BooleanFormula f : interpolationFormulaList){
        myItpGroupIds.add(myItpProver.push(f));
      }  */
    /* for (BooleanFormula f : changed_formulas_rest1){
       myItpGroupIds.add(myItpProver.push(f));
     } */
    if (it != 0){
      for (BooleanFormula f : changed_formulas_rest1){
        logger.log(Level.WARNING, "Changed Formulas Rest 1:", changed_formulas_rest1.toString());
        myItpGroupIds.add(myItpProver.push(f));
      }
    }

     myItpGroupIds.add(myItpProver.push(helperFormula1));
        logger.log(Level.WARNING, "helper Formula 1:", helperFormula1.toString());
      //  myItpGroupIds.add(myItpProver.push(helperFormula2));
     myItpProver.push(helperFormula2);
        logger.log(Level.WARNING, "helper Formula 2:", helperFormula2.toString());
    /*  for (int i = 0; i < it; i++) {
        myItpProver.push(oldFormulas.get(i));
      }
      for (int i = it + 2; i < oldFormulas.size(); i++){
        myItpProver.push(oldFormulas.get(i));
      } */
    if (! changed_formulas_rest2.isEmpty()) {
      for (BooleanFormula f : changed_formulas_rest2) {
        logger.log(Level.WARNING, "Changed Formulas Rest 2:", changed_formulas_rest2.toString());
        myItpProver.push(f);
      }
    }

        if (!myItpProver.isUnsat()) {
            throw new SolverException("Interpolant kann nicht berechnet werden!");

        } else {

          BooleanFormula myInterpolant = myItpProver.getInterpolant
              (myItpGroupIds);
          logger.log(Level.WARNING, "Interpolant:", myInterpolant.toString());

          if (myInterpolant != null) {
            interpolants.add(myInterpolant);
            logger.log(Level.WARNING, "Current Interpolants:", interpolants.toString());
            fmgr.translateFrom(myInterpolant, mySolver.getFormulaManager());
          }
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
    logger.log(Level.WARNING, "Interpolants:", interpolants.toString());
    if (interpolants != null && !(interpolants.isEmpty())){
      return interpolants;
    } else {
      return Collections.emptyList();
    }
  }

@SuppressWarnings("rawtypes")
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
@SuppressWarnings("rawtypes")
  private /*List<IntegerFormula> */ List<Formula> maximise(BooleanFormula firstPartChanged,
                                                      BooleanFormula
      scndPartChanged, String[] relationAbstraction1, String[] relationAbstraction2,
                                  List<BooleanFormula> relationAbstraction1Formula,
                                  List<BooleanFormula>
                                      relationAbstraction2Formula,
                                  /*boolean[][] lattice, */
                                  String[]
      /*fullLatticeNames, */ latticeNames, /*int placeinlattice, */ String latticenames_h, Solver
                                                               mySolver){

    //String[] middleElement = new String[fullLatticeNames.length];
    String[] middleElement = new String[latticeNames.length];
    int middleElemIndex = 0;
    Boolean isFeasible = true;
    BooleanFormula helperFormula1;
    BooleanFormula helperFormula2;
    Boolean hasFeasibleSuccessor = false;
    int feasibleSuccessorPosition = 0;
    /*List<IntegerFormula>*/ List<Formula> maximumFeasibleAbstraction = Lists
        .newArrayListWithExpectedSize
        (formulas
        .size() - 1);

   // for (int i = placeinlattice + 1; i < fullLatticeNames.length; i++){
      for (int i = 1; i < latticeNames.length; i++) {
        /*if (lattice[placeinlattice][i] == true) */ if (!latticenames_h.contains
            (latticeNames[i])){
          //call prover function with fullLatticeNames[i] applied to firstPartChanged and
          // scndPartChanged
          latticenames_h = latticenames_h + " ," + latticeNames[i];
          helperFormula1 = firstPartChanged;
          helperFormula2 = scndPartChanged;
          //Iterable<String> splitOperator = Splitter.on(" ,").split(fullLatticeNames[i]);
          Iterable<String> splitOperator = Splitter.on(" ,").split(latticenames_h);
          for (String s : splitOperator) {

            for (int k = 0; k < relationAbstraction1.length; k++) {
              if (relationAbstraction1[k] != null && !(s == null)) {
                if ((relationAbstraction1[k]).contains(s + " = ")) {
                  //BooleanFormula helperFormula_1;
                  helperFormula1 = fmgr.makeAnd(helperFormula1, relationAbstraction1Formula.get
                      (k));


                }
              }
              if (relationAbstraction2[k] != null && !(s == null)) {
                if ((relationAbstraction2[k]).contains(s + " = ")) {
                  //BooleanFormula helperFormula;
                  helperFormula2 = fmgr.makeAnd(helperFormula2, relationAbstraction2Formula.get
                      (k));


                }
              }
            }
          }
          BooleanFormula toCheckFormula = fmgr.makeAnd(helperFormula1, helperFormula2);
          List<BooleanFormula> toCheckFormulaList =
              Lists.newArrayListWithExpectedSize(formulas.size() - 1);
          toCheckFormulaList.add(toCheckFormula);
          BlockFormulas toCheckFormulaBlocked = new BlockFormulas(toCheckFormulaList);
          isFeasible = prove(toCheckFormulaBlocked, mySolver);
          //if abstraction is feasible:
         /* if (isFeasible) {
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
          } */
         if (isFeasible){
           hasFeasibleSuccessor = true;
           for (int m = 1; m < latticeNames.length; m++){
             if (!latticenames_h.isEmpty() && !(latticenames_h == null) &&!latticenames_h.contains
                 (latticeNames[m])){
               middleElement[middleElemIndex] = latticenames_h + " ," + latticeNames[m];
               latticenames_h = middleElement[middleElemIndex];
               middleElemIndex++;
             }
           }
           break;
         }
        }
      }

 //   }
    if (middleElement[0] == null){
      List<FormulaType> formulaTypes = Lists.newArrayListWithExpectedSize(latticeNamesTypes.size()
          - 1);
      Iterable<String> splitOperator = Splitter.on(" ,").split(latticenames_h);
      for (String s : splitOperator) {
        for (int i = 1; i < latticeNames.length; i++) {
          if (latticeNames[i] != null && !(s == null) && s.equals(latticeNames[i])){
            formulaTypes.add(latticeNamesTypes.get(latticeNames[i]));
          }
        }
      }
      if (hasFeasibleSuccessor) {
        maximumFeasibleAbstraction = StringtoIntegerFormulaList
            ( /*fullLatticeNames[feasibleSuccessorPosition], fullLatticeNamesTypes.get
                (fullLatticeNames[feasibleSuccessorPosition]) */ latticenames_h, formulaTypes /*,
            formulas */);

      }
      else {
        maximumFeasibleAbstraction = StringtoIntegerFormulaList
            (/*fullLatticeNames[placeinlattice], fullLatticeNamesTypes.get
                    (fullLatticeNames[placeinlattice]) */ latticenames_h, formulaTypes
                /*, formulas */);
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
            if (relationAbstraction1[k] != null) {
              if ((relationAbstraction1[k]).contains(s + " = ")) {
                helperFormula1 = fmgr.makeAnd(helperFormula1, relationAbstraction1Formula.get
                    (k));


              }
            }
            if (relationAbstraction2[k] != null) {
              if ((relationAbstraction2[k]).contains(s + " = ")) {
                helperFormula2 = fmgr.makeAnd(helperFormula2, relationAbstraction2Formula.get
                    (k));


              }
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
          List<FormulaType> formulaTypes2 = Lists.newArrayListWithExpectedSize(latticeNamesTypes
              .size()
              - 1);
          Iterable<String> splitOperator2 = Splitter.on(" ,").split(middleElement[counter/2]);
          for (String s : splitOperator2) {
            for (int i = 1; i < latticeNames.length; i++) {
              if (latticeNames[i] != null && s.equals(latticeNames[i])){
                formulaTypes2.add(latticeNamesTypes.get(latticeNames[i]));
              }
            }
          }
          //maximumFeasibleAbstraction = middleElement[counter/2] transformed into a Boolean Formula.
          maximumFeasibleAbstraction = StringtoIntegerFormulaList
              (middleElement[counter/2], /*fullLatticeNamesTypes.get(middleElement[counter/2])*/
                  formulaTypes2);

        } else {
          List<FormulaType> formulaTypes = Lists.newArrayListWithExpectedSize(latticeNamesTypes.size()
              - 1);
          Iterable<String> splitOperator3 = Splitter.on(" ,").split(latticenames_h);
          for (String s : splitOperator3) {
            for (int i = 1; i < latticeNames.length; i++) {
              if (latticeNames[i] != null && s.equals(latticeNames[i])){
                formulaTypes.add(latticeNamesTypes.get(latticeNames[i]));
              }
            }
          }

          maximumFeasibleAbstraction = StringtoIntegerFormulaList
              (/*fullLatticeNames[placeinlattice], fullLatticeNamesTypes.get
              (fullLatticeNames[placeinlattice]) */ latticenames_h, formulaTypes /*,
               formulas */);

        }
    }
    return maximumFeasibleAbstraction;
  }
@SuppressWarnings({"rawtype", "unchecked"})
  private List<Formula> StringtoIntegerFormulaList(String input, List<FormulaType>
      formulaTypes){
    Formula helperFormula1, helperFormula2, helperFormula3;
    List<Formula> maximumFeasibleAbstraction = Lists.newArrayListWithExpectedSize(formulas.size() - 1);

  if (input.equals("root")){
    return Collections.emptyList();
  }
  if (input == null || input.isEmpty()){
    return Collections.emptyList();
  }
    String[] helperArray = new String[2];
  logger.log(Level.WARNING, "StringtoIntegerFormulaList: input: " + input + " formulaTypes: " +
      formulaTypes.toString());
    int j = 0;
    Iterable<String> splitOperator = Splitter.on(" ,").split(input);
    for (String s : splitOperator) {
      FormulaType currentType = formulaTypes.get(j);
      if (s.contains(" - ")){
        int i = 0;
        Iterable<String> splitOperator2 = Splitter.on(" - ").split(s);
        for (String t : splitOperator2){
          helperArray[i] = t;
          i++;
        }
        helperFormula1 = fmgr.makeVariable(latticeNamesTypes.get(helperArray[0]),
            helperArray[0]);
        helperFormula2 = fmgr.makeVariable(latticeNamesTypes.get(helperArray[0]), helperArray[1]);
        helperFormula3 = fmgr.makeMinus(helperFormula1, helperFormula2);

      }
      else {
        helperFormula3 = fmgr.makeVariable(currentType, s);
      }
      maximumFeasibleAbstraction.add(helperFormula3);
      j++;
    }
    return maximumFeasibleAbstraction;
  }

  private Boolean checkComparability(/*List<List<IntegerFormula>> */ List<List<Formula>>
      frontierListCopy,
                                     String
      /*fullLatticeNames_h*/ latticeNames_h, String[] latticeNames){
    List<FormulaType> formulaTypes = Lists.newArrayListWithExpectedSize(latticeNamesTypes.size()
        - 1);
    Iterable<String> splitOperator = Splitter.on(" ,").split(latticeNames_h);
    for (String s : splitOperator) {
      for (int i = 1; i < latticeNames.length; i++) {
        if (latticeNames[i] != null && s.equals(latticeNames[i])){
          formulaTypes.add(latticeNamesTypes.get(latticeNames[i]));
        }
      }
    }
    logger.log(Level.WARNING, "CheckComparability: latticeNames_h: " + latticeNames_h + " "
        + "formulaTypes: " + formulaTypes.toString());
    List<Formula> toCompareWith = StringtoIntegerFormulaList(latticeNames_h,
        /*fullLatticeNamesTypes.get(fullLatticeNames_h) */ formulaTypes);
    /*List<List<IntegerFormula>> compareList = Lists.newArrayListWithExpectedSize(formulas.size() -
        1); */
    List<List<Formula>> compareList = Lists.newArrayListWithExpectedSize(formulas.size() -
        1);
    Boolean isIncomparable = false;
    Boolean comparable = false;
    while (frontierListCopy.size() != 0) {
      /*List<IntegerFormula> */ List<Formula> smallestList = frontierListCopy.get(0);


      for (/*List<IntegerFormula> */ List<Formula> f : frontierListCopy) {
        if (f.size() < smallestList.size()) {
          smallestList = f;
        }
      }
      compareList.add(smallestList);
      frontierListCopy.remove(smallestList);
      for (Formula f : toCompareWith) {
        comparable = false;
        for (/*List<IntegerFormula> */ List<Formula> g : compareList) {
          for (/*IntegerFormula */ Formula h : g) {

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
