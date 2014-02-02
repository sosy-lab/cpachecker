/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.tiger.testcases;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.core.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

public abstract class TestCase {

  public static final int NONDET_INT_INDEX = 0;
  public static final int NONDET_LONG_INDEX = 1;
  public static final int NONDET_UINT_INDEX = 2;
  public static final int NONDET_BOOL_INDEX = 3;
  public static final int NONDET_CHAR_INDEX = 4;

  public static final int NUMBER_OF_NONDET_VARIABLES = 5;

  private int[][] mInputsMap;
  private boolean mIsPrecise;

  protected TestCase(List<Integer>[] pInputs, boolean pIsPrecise) {
    assert (pInputs != null);
    assert (pInputs.length == NUMBER_OF_NONDET_VARIABLES);

    mInputsMap = new int[NUMBER_OF_NONDET_VARIABLES][];

    for (int i = 0; i < NUMBER_OF_NONDET_VARIABLES; i++) {
      assert(pInputs[i] != null);
      mInputsMap[i] = new int[pInputs[i].size()];

      int lIndex = 0;
      for (Integer lValue : pInputs[i]) {
        mInputsMap[i][lIndex] = lValue;
        lIndex++;
      }
    }

    mIsPrecise = pIsPrecise;
  }

  protected TestCase(int[][] pInputs, boolean pIsPrecise) {
    assert (pInputs != null);
    assert (pInputs.length == NUMBER_OF_NONDET_VARIABLES);

    mInputsMap = new int[NUMBER_OF_NONDET_VARIABLES][];

    for (int i = 0; i < NUMBER_OF_NONDET_VARIABLES; i++) {
      assert(pInputs[i] != null);
      mInputsMap[i] = new int[pInputs[i].length];

      for (int j = 0; j < pInputs[i].length; j++) {
        mInputsMap[i][j] = pInputs[i][j];
      }
    }

    mIsPrecise = pIsPrecise;
  }

  public int[][] getInputs() {
    return mInputsMap;
  }

  public boolean isPrecise() {
    return mIsPrecise;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (getClass().equals(pOther.getClass())) {
      TestCase lTestCase = (TestCase)pOther;

      if (mIsPrecise == lTestCase.mIsPrecise) {
        for (int i = 0; i < NUMBER_OF_NONDET_VARIABLES; i++) {
          if (mInputsMap[i].length != lTestCase.mInputsMap[i].length) {
            return false;
          }

          for (int j = 0; j < mInputsMap[i].length; j++) {
            if (mInputsMap[i][j] != lTestCase.mInputsMap[i][j]) {
              return false;
            }
          }
        }

        return true;
      }
    }

    return false;
  }

  public String toInputString() {
    StringBuilder lBuilder = new StringBuilder();

    for (int i = 0; i < NUMBER_OF_NONDET_VARIABLES; i++) {
      switch (i) {
      case NONDET_INT_INDEX:
        lBuilder.append(PathFormulaManagerImpl.NONDET_VARIABLE + ":\n");
        break;
      case NONDET_LONG_INDEX:
        lBuilder.append(PathFormulaManagerImpl.NONDET_VARIABLE_LONG + ":\n");
        break;
      case NONDET_UINT_INDEX:
        lBuilder.append(PathFormulaManagerImpl.NONDET_VARIABLE_UINT + ":\n");
        break;
      case NONDET_BOOL_INDEX:
        lBuilder.append(PathFormulaManagerImpl.NONDET_VARIABLE_BOOL + ":\n");
        break;
      case NONDET_CHAR_INDEX:
        lBuilder.append(PathFormulaManagerImpl.NONDET_VARIABLE_CHAR + ":\n");
        break;
      }

      for (int j = 0; j < mInputsMap[i].length; j++) {
        lBuilder.append(mInputsMap[i][j] + "\n");
      }
    }

    return lBuilder.toString();
  }

  public void toInputFile(File pFile) throws FileNotFoundException {
    PrintWriter lWriter = new PrintWriter(pFile);

    lWriter.print(toInputString());

    lWriter.close();
  }

  public String toCFunction() {
    StringBuilder lCFunctionBuilder = new StringBuilder();

    lCFunctionBuilder.append("#include <stdio.h>\n");
    lCFunctionBuilder.append("#include <stdlib.h>\n");
    lCFunctionBuilder.append("\n");

    for (int i = 0; i < NUMBER_OF_NONDET_VARIABLES; i++) {
      switch (i) {
      case NONDET_INT_INDEX:
        lCFunctionBuilder.append("int " + PathFormulaManagerImpl.NONDET_VARIABLE + "()\n");
        break;
      case NONDET_LONG_INDEX:
        lCFunctionBuilder.append("int " + PathFormulaManagerImpl.NONDET_VARIABLE_LONG + "()\n");
        break;
      case NONDET_UINT_INDEX:
        lCFunctionBuilder.append("int " + PathFormulaManagerImpl.NONDET_VARIABLE_UINT + "()\n");
        break;
      case NONDET_BOOL_INDEX:
        lCFunctionBuilder.append("int " + PathFormulaManagerImpl.NONDET_VARIABLE_BOOL + "()\n");
        break;
      case NONDET_CHAR_INDEX:
        lCFunctionBuilder.append("int " + PathFormulaManagerImpl.NONDET_VARIABLE_CHAR + "()\n");
        break;
      }

      lCFunctionBuilder.append("{\n");
      lCFunctionBuilder.append("  static int index = 0;\n");

      lCFunctionBuilder.append("  int testcase[] = {");

      for (int lIndex = 0; lIndex < mInputsMap[i].length; lIndex++) {
        if (lIndex > 0) {
          lCFunctionBuilder.append(",");
        }
        lCFunctionBuilder.append(mInputsMap[i][lIndex]);
      }

      lCFunctionBuilder.append("};\n");

      lCFunctionBuilder.append("  if (index == " + mInputsMap[i].length + ")\n");
      lCFunctionBuilder.append("  {\n");
      lCFunctionBuilder.append("    fprintf(stderr, \"[ERROR] test case too short!\\n\");\n");
      lCFunctionBuilder.append("    exit(-1);\n");
      lCFunctionBuilder.append("  }\n");
      lCFunctionBuilder.append("  return testcase[index++];\n");
      lCFunctionBuilder.append("}\n\n");
    }

    return lCFunctionBuilder.toString();
  }

  @Override
  public String toString() {
    return asString();
  }

  public String asString() {
    StringBuffer lBuffer = new StringBuffer();
    lBuffer.append(isPrecise()?"p":"i");

    for (int i = 0; i < NUMBER_OF_NONDET_VARIABLES; i++) {
      switch (i) {
      case NONDET_INT_INDEX:
        lBuffer.append("[" + PathFormulaManagerImpl.NONDET_VARIABLE);
        break;
      case NONDET_LONG_INDEX:
        lBuffer.append("[" + PathFormulaManagerImpl.NONDET_VARIABLE_LONG);
        break;
      case NONDET_UINT_INDEX:
        lBuffer.append("[" + PathFormulaManagerImpl.NONDET_VARIABLE_UINT);
        break;
      case NONDET_BOOL_INDEX:
        lBuffer.append("[" + PathFormulaManagerImpl.NONDET_VARIABLE_BOOL);
        break;
      case NONDET_CHAR_INDEX:
        lBuffer.append("[" + PathFormulaManagerImpl.NONDET_VARIABLE_CHAR);
        break;
      }

      for (int j = 0; j < mInputsMap[i].length; j++) {
        lBuffer.append(",");
        lBuffer.append(mInputsMap[i][j]);
      }

      lBuffer.append("]");
    }

    return lBuffer.toString();
  }

  @Override
  public int hashCode() {
    int lHashcode = 0;
    for (int i = 0; i < NUMBER_OF_NONDET_VARIABLES; i++) {
      lHashcode += (mInputsMap[i].hashCode() * (i + 345));
    }

    return lHashcode * (mIsPrecise?13:27);
  }

  public static Collection<TestCase> fromFile(String pFileName) throws IOException {
    return fromFile(new File(pFileName));
  }

  public static Collection<TestCase> fromFile(File pFile) throws IOException {
    Collection<TestCase> lTestSuite = new LinkedList<>();

    BufferedReader lReader = new BufferedReader(new FileReader(pFile));

    String lLine = null;

    while ((lLine = lReader.readLine()) != null) {
      lLine = lLine.trim();

      if (lLine.length() != 0) {
        lTestSuite.add(fromString(lLine));
      }
    }

    lReader.close();

    return lTestSuite;
  }

  public static void toFile(Collection<TestCase> pTestSuite, File pTestSuiteFile) throws FileNotFoundException {
    PrintWriter lWriter = new PrintWriter(pTestSuiteFile);

    for (TestCase lTestCase : pTestSuite) {
      lWriter.println(lTestCase.asString());
    }

    lWriter.close();
  }

  public static TestCase fromString(String pTestCase) {
    throw new RuntimeException("Implement!");

    /*

    boolean lIsPrecise;

    String[] lParts = pTestCase.split(",");

    if (lParts.length == 0) {
      throw new RuntimeException();
    }

    if (lParts[0].equals("p")) {
      lIsPrecise = true;
    }
    else if (lParts[0].equals("i")) {
      lIsPrecise = false;
    }
    else {
      throw new RuntimeException();
    }

    int[] lValues = new int[lParts.length - 1];

    for (int lIndex = 0; lIndex < lValues.length; lIndex++) {
      lValues[lIndex] = Integer.parseInt(lParts[lIndex + 1]);
    }

    if (lIsPrecise) {
      return new PreciseInputsTestCase(lValues);
    }
    else {
      // TODO what about imprecise exeution test cases ?
      double[] lEmpty = new double[0];
      return new ImpreciseInputsTestCase(lValues, lEmpty);
    }

    */
  }

  public static TestCase fromCounterexample(CounterexampleInfo pTraceInfo, LogManager pLogManager) {
    Model lModel = pTraceInfo.getTargetPathModel();

    TestCase lTestcase = fromCounterexample(lModel, pLogManager);

    return lTestcase;
  }

  public static TestCase fromCounterexample(CounterexampleTraceInfo pTraceInfo, LogManager pLogManager) {
    Model lModel = pTraceInfo.getModel();

    return fromCounterexample(lModel, pLogManager);
  }

  public static final String INPUT_PREFIX = "__VERIFIER_nondet_";

  public static TestCase fromCounterexample(Model pCounterexample, LogManager pLogManager) {
    boolean lIsPrecise = true;

    List<TreeMap<Integer, Double>> lNondetMaps = new ArrayList<>(NUMBER_OF_NONDET_VARIABLES);
    List<TreeMap<Integer, Boolean>> lNondetFlagMaps = new ArrayList<>(NUMBER_OF_NONDET_VARIABLES);

    for (int i = 0; i < NUMBER_OF_NONDET_VARIABLES; i++) {
      lNondetMaps.add(new TreeMap<Integer, Double>());
      lNondetFlagMaps.add(new TreeMap<Integer, Boolean>());
    }

    for (Map.Entry<AssignableTerm, Object> lAssignment : pCounterexample.entrySet()) {
      AssignableTerm lTerm = lAssignment.getKey();

      if (lTerm instanceof Variable) {

        Variable lVar = (Variable)lTerm;
        String lName = lVar.getName();

        if (lName.equals(PathFormulaManagerImpl.NONDET_VARIABLE)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          lNondetMaps.get(NONDET_INT_INDEX).put(lIndex, lDoubleValue);
        }
        else if (lName.equals(PathFormulaManagerImpl.NONDET_VARIABLE_LONG)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          lNondetMaps.get(NONDET_LONG_INDEX).put(lIndex, lDoubleValue);
        }
        else if (lName.equals(PathFormulaManagerImpl.NONDET_VARIABLE_UINT)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          lNondetMaps.get(NONDET_UINT_INDEX).put(lIndex, lDoubleValue);
        }
        else if (lName.equals(PathFormulaManagerImpl.NONDET_VARIABLE_BOOL)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          lNondetMaps.get(NONDET_BOOL_INDEX).put(lIndex, lDoubleValue);
        }
        else if (lName.equals(PathFormulaManagerImpl.NONDET_VARIABLE_CHAR)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          lNondetMaps.get(NONDET_CHAR_INDEX).put(lIndex, lDoubleValue);
        }
        else if (lName.equals(PathFormulaManagerImpl.NONDET_FLAG_VARIABLE)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          if (lDoubleValue != 0.0) {
            lNondetFlagMaps.get(NONDET_INT_INDEX).put(lIndex, true);
          }
          else {
            lNondetFlagMaps.get(NONDET_INT_INDEX).put(lIndex, false);
          }
        }
        else if (lName.equals(PathFormulaManagerImpl.NONDET_FLAG_VARIABLE_LONG)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          if (lDoubleValue != 0.0) {
            lNondetFlagMaps.get(NONDET_LONG_INDEX).put(lIndex, true);
          }
          else {
            lNondetFlagMaps.get(NONDET_LONG_INDEX).put(lIndex, false);
          }
        }
        else if (lName.equals(PathFormulaManagerImpl.NONDET_FLAG_VARIABLE_UINT)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          if (lDoubleValue != 0.0) {
            lNondetFlagMaps.get(NONDET_UINT_INDEX).put(lIndex, true);
          }
          else {
            lNondetFlagMaps.get(NONDET_UINT_INDEX).put(lIndex, false);
          }
        }
        else if (lName.equals(PathFormulaManagerImpl.NONDET_FLAG_VARIABLE_BOOL)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          if (lDoubleValue != 0.0) {
            lNondetFlagMaps.get(NONDET_BOOL_INDEX).put(lIndex, true);
          }
          else {
            lNondetFlagMaps.get(NONDET_BOOL_INDEX).put(lIndex, false);
          }
        }
        else if (lName.equals(PathFormulaManagerImpl.NONDET_FLAG_VARIABLE_CHAR)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          if (lDoubleValue != 0.0) {
            lNondetFlagMaps.get(NONDET_CHAR_INDEX).put(lIndex, true);
          }
          else {
            lNondetFlagMaps.get(NONDET_CHAR_INDEX).put(lIndex, false);
          }
        }
        else if (lName.startsWith(INPUT_PREFIX)) {
          throw new RuntimeException("nondet flags are only implemented for " + PathFormulaManagerImpl.NONDET_VARIABLE + " but not for " + lName);
        }
      }
    }

    List<List<Integer>> lInputList = new ArrayList<>(NUMBER_OF_NONDET_VARIABLES);
    List<List<Double>> lValuesList = new ArrayList<>(NUMBER_OF_NONDET_VARIABLES);

    for (int i = 0; i < NUMBER_OF_NONDET_VARIABLES; i++) {
      LinkedList<Integer> lInput = new LinkedList<>();
      LinkedList<Double> lValues = new LinkedList<>();

      for (Map.Entry<Integer, Double> lEntry : lNondetMaps.get(i).entrySet()) {
        Integer lKey = lEntry.getKey();

        if (lNondetFlagMaps.get(i).get(lKey)) {
          Double lValue = lEntry.getValue();

          int lIntValue = lValue.intValue();

          if (lValue.doubleValue() != lIntValue) {
            lIsPrecise = false;
          }

          lInput.add(lIntValue);
          lValues.add(lValue);
        }
      }

      lInputList.add(lInput);
      lValuesList.add(lValues);
    }

    int[][] lFinalInputs = new int[NUMBER_OF_NONDET_VARIABLES][];
    double[][] lFinalValues = new double[NUMBER_OF_NONDET_VARIABLES][];

    for (int i = 0; i < NUMBER_OF_NONDET_VARIABLES; i++) {
      lFinalInputs[i] = new int[lInputList.get(i).size()];
      lFinalValues[i] = new double[lValuesList.get(i).size()];

      for (int j = 0; j < lFinalInputs[i].length; j++) {
        lFinalInputs[i][j] = lInputList.get(i).get(j);
        lFinalValues[i][j] = lValuesList.get(i).get(j);
      }
    }

    if (lIsPrecise) {
      // TODO replace casting by something nicer
      return new PreciseInputsTestCase(lFinalInputs);
    }
    else {
      // TODO replace casting by something nicer
      return new ImpreciseInputsTestCase(lFinalInputs, lFinalValues);
    }
  }

}
