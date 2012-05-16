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
package org.sosy_lab.cpachecker.fshell.testcases;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.util.predicates.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.Model.Variable;

public abstract class TestCase {

  private int[] mInputs;
  private boolean mIsPrecise;

  protected TestCase(List<Integer> pInputs, boolean pIsPrecise) {
    mInputs = new int[pInputs.size()];

    int lIndex = 0;
    for (Integer lValue : pInputs) {
      mInputs[lIndex] = lValue;
      lIndex++;
    }

    mIsPrecise = pIsPrecise;
  }

  protected TestCase(int[] pInputs, boolean pIsPrecise) {
    mInputs = new int[pInputs.length];

    for (int lIndex = 0; lIndex < mInputs.length; lIndex++) {
      mInputs[lIndex] = pInputs[lIndex];
    }

    mIsPrecise = pIsPrecise;
  }

  protected static boolean equal(int[] lInputs1, int[] lInputs2) {
    if (lInputs1.length == lInputs2.length) {
      for (int lIndex = 0; lIndex < lInputs1.length; lIndex++) {
        if (lInputs1[lIndex] != lInputs2[lIndex]) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  protected boolean equalInputs(int[] lOtherInputs) {
    return equal(mInputs, lOtherInputs);
  }

  public boolean isPrecise() {
    return mIsPrecise;
  }

  public int[] getInputs() {
    return mInputs;
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
        return equalInputs(lTestCase.mInputs);
      }
    }

    return false;
  }

  public String toInputString() {
    StringBuilder lBuilder = new StringBuilder();

    for (int lValue : mInputs) {
      lBuilder.append(lValue + "\n");
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

    lCFunctionBuilder.append("int input()\n");
    lCFunctionBuilder.append("{\n");
    lCFunctionBuilder.append("  static int index = 0;\n");

    lCFunctionBuilder.append("  int testcase[] = {");

    for (int lIndex = 0; lIndex < mInputs.length; lIndex++) {
      if (lIndex > 0) {
        lCFunctionBuilder.append(",");
      }
      lCFunctionBuilder.append(mInputs[lIndex]);
    }

    lCFunctionBuilder.append("};\n");

    lCFunctionBuilder.append("  if (index == " + mInputs.length + ")\n");
    lCFunctionBuilder.append("  {\n");
    lCFunctionBuilder.append("    fprintf(stderr, \"[ERROR] test case too short!\\n\");\n");
    lCFunctionBuilder.append("    exit(-1);\n");
    lCFunctionBuilder.append("  }\n");
    lCFunctionBuilder.append("  return testcase[index++];\n");
    lCFunctionBuilder.append("}\n");

    return lCFunctionBuilder.toString();
  }

  @Override
  public String toString() {
    StringBuffer lBuffer = new StringBuffer();
    lBuffer.append(isPrecise()?"p":"i");

    for (int lIndex = 0; lIndex < mInputs.length; lIndex++) {
      lBuffer.append(",");
      lBuffer.append(mInputs[lIndex]);
    }

    return lBuffer.toString();
  }

  public static Collection<TestCase> fromFile(String pFileName) throws IOException {
    return fromFile(new File(pFileName));
  }

  public static Collection<TestCase> fromFile(File pFile) throws IOException {
    Collection<TestCase> lTestSuite = new LinkedList<TestCase>();

    BufferedReader lReader = new BufferedReader(new FileReader(pFile));

    String lLine = null;

    while ((lLine = lReader.readLine()) != null) {
      lLine = lLine.trim();

      if (lLine.length() != 0) {
        lTestSuite.add(fromString(lLine));
      }
    }

    return lTestSuite;
  }

  public static void toFile(Collection<TestCase> pTestSuite, File pTestSuiteFile) throws FileNotFoundException {
    PrintWriter lWriter = new PrintWriter(pTestSuiteFile);

    for (TestCase lTestCase : pTestSuite) {
      lWriter.println(lTestCase.toString());
    }

    lWriter.close();
  }

  public static TestCase fromString(String pTestCase) {
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
      return new ImpreciseInputsTestCase(lValues);
    }
  }

  public static TestCase fromCounterexample(CounterexampleInfo pTraceInfo, LogManager pLogManager) {
    Model lModel = (Model)pTraceInfo.getTargetPathAssignment();

    return fromCounterexample(lModel, pLogManager);
  }

  public static TestCase fromCounterexample(Model pCounterexample, LogManager pLogManager) {
    //Set<MathsatAssignable> lAssignables = pCounterexample.getAssignables();

    boolean lIsPrecise = true;

    SortedMap<Integer, Double> lNondetMap = new TreeMap<Integer, Double>();
    SortedMap<Integer, Boolean> lNondetFlagMap = new TreeMap<Integer, Boolean>();

    for (Map.Entry<AssignableTerm, Object> lAssignment : pCounterexample.entrySet()) {
      AssignableTerm lTerm = lAssignment.getKey();

      if (lTerm instanceof Variable) {
        Variable lVar = (Variable)lTerm;

        String lName = lVar.getName();

        if (lName.equals(CtoFormulaConverter.NONDET_VARIABLE)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          lNondetMap.put(lIndex, lDoubleValue);
        }
        else if (lName.equals(CtoFormulaConverter.NONDET_FLAG_VARIABLE)) {
          Integer lIndex = lVar.getSSAIndex();

          double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());

          if (lDoubleValue != 0.0) {
            lNondetFlagMap.put(lIndex, true);
          }
          else {
            lNondetFlagMap.put(lIndex, false);
          }
        }
      }
    }

    LinkedList<Integer> lInput = new LinkedList<Integer>();

    for (Map.Entry<Integer, Double> lEntry : lNondetMap.entrySet()) {
      Integer lKey = lEntry.getKey();

      if (lNondetFlagMap.get(lKey)) {
        Double lValue = lEntry.getValue();

        int lIntValue = lValue.intValue();

        if (lValue.doubleValue() != lIntValue) {
          lIsPrecise = false;
        }

        lInput.add(lIntValue);
      }
    }

    if (lIsPrecise) {
      return new PreciseInputsTestCase(lInput);
    }
    else {
      return new ImpreciseInputsTestCase(lInput);
    }
  }

}
