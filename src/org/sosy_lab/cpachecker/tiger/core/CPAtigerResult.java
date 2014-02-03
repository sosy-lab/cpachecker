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
package org.sosy_lab.cpachecker.tiger.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.tiger.testcases.TestCase;
import org.sosy_lab.cpachecker.tiger.testgen.IncrementalARTReusingFQLTestGenerator;

import com.google.common.base.Preconditions;

public class CPAtigerResult {

  public static class Factory {

    private Set<ElementaryCoveragePattern> mFeasibleTestGoals;
    private Set<ElementaryCoveragePattern> mInfeasibleTestGoals;
    private Set<ElementaryCoveragePattern> mBugRevealingTestGoals;
    private Map<TestCase, Set<ElementaryCoveragePattern>> mTestSuite;
    private Set<TestCase> mImpreciseTestCases;
    private Set<TestCase> mBugRevealingTestCases;
    private boolean mFinished = true;
    private int numberOfTestGoals;

    private Factory(int pNumberOfTestGoals) {
      numberOfTestGoals = pNumberOfTestGoals;
      mFeasibleTestGoals = new HashSet<>();
      mInfeasibleTestGoals = new HashSet<>();
      mBugRevealingTestGoals = new HashSet<>();
      mTestSuite = new HashMap<>();
      mImpreciseTestCases = new HashSet<>();
    }

    public void setUnfinished() {
      mFinished = false;
    }

    public void add(ElementaryCoveragePattern pECP, boolean pIsFeasible) {
      if (pIsFeasible) {
        mFeasibleTestGoals.add(pECP);
      }
      else {
        mInfeasibleTestGoals.add(pECP);
      }
    }

    public void addFeasibleTestCase(ElementaryCoveragePattern pECP, TestCase pTestCase) {
      mFeasibleTestGoals.add(pECP);
      Set<ElementaryCoveragePattern> lTestSuite = getTestSuite(pTestCase);
      lTestSuite.add(pECP);
    }

    public void addInfeasibleTestCase(ElementaryCoveragePattern pECP) {
      mInfeasibleTestGoals.add(pECP);
    }

    public void addImpreciseTestCase(TestCase pTestCase) {
      Preconditions.checkNotNull(pTestCase);
      //Preconditions.checkArgument(!pTestCase.isPrecise());
      // TODO activate precision check again!
      IncrementalARTReusingFQLTestGenerator.getInstance().mOutput.println("TODO: activate precision check again!");

      mImpreciseTestCases.add(pTestCase);
    }

    public void addBugRevealingTestCase(ElementaryCoveragePattern pECP, TestCase pTestCase) {
      mBugRevealingTestGoals.add(pECP);
      mBugRevealingTestCases.add(pTestCase);
    }

    private Set<ElementaryCoveragePattern> getTestSuite(TestCase pTestCase) {
      if (mTestSuite.containsKey(pTestCase)) {
        return mTestSuite.get(pTestCase);
      }
      else {
        Set<ElementaryCoveragePattern> lTestSuite = new HashSet<>();

        mTestSuite.put(pTestCase, lTestSuite);

        return lTestSuite;
      }
    }

    public Collection<TestCase> getTestCases() {
      Set<TestCase> lTestCases = new HashSet<>();

      lTestCases.addAll(mTestSuite.keySet());
      lTestCases.addAll(mImpreciseTestCases);

      return lTestCases;
    }

    public CPAtigerResult create(double pTimeInReach, double pTimeInCover, double pTimeForFeasibleTestGoals, double pTimeForInfeasibleTestGoals) {
      return new CPAtigerResult(numberOfTestGoals,   mFeasibleTestGoals.size(), mInfeasibleTestGoals.size(),
          mBugRevealingTestGoals.size(), mTestSuite.keySet().size(), mImpreciseTestCases.size(), pTimeInReach, pTimeInCover,
          pTimeForFeasibleTestGoals, pTimeForInfeasibleTestGoals, mFinished);
    }

  }

  public static Factory factory(int lNumberOfTestGoals) {
    return new Factory(lNumberOfTestGoals);
  }

  private int mNumberOfTestGoals;
  private int mNumberOfFeasibleTestGoals;
  private int mNumberOfInfeasibleTestGoals;
  private int mNumberOfBugRevealingTestGoals;
  private int mNumberOfTestCases;
  private int mNumberOfImpreciseTestCases;
  private double mTimeForFeasibleTestGoals; // seconds
  private double mTimeForInfeasibleTestGoals; // seconds
  private double mTimeInReach; // seconds
  private double mTimeInCover; // seconds
  private boolean mFinished;


  private CPAtigerResult(int pNumberOfTestGoals, int pNumberOfFeasibleTestGoals, int pNumberOfInfeasibleTestGoals,
      int pNumberOfBugRevealingTestGoals, int pNumberOfTestCases, int pNumberOfImpreciseTestCases, double pTimeInReach,
      double pTimeInCover, double pTimeForFeasibleTestGoals, double pTimeForInfeasibleTestGoals, boolean pFinished) {
    mNumberOfTestGoals = pNumberOfTestGoals;
    mNumberOfFeasibleTestGoals = pNumberOfFeasibleTestGoals;
    mNumberOfInfeasibleTestGoals = pNumberOfInfeasibleTestGoals;
    mNumberOfBugRevealingTestGoals = pNumberOfBugRevealingTestGoals;
    mNumberOfTestCases = pNumberOfTestCases;
    mNumberOfImpreciseTestCases = pNumberOfImpreciseTestCases;
    mTimeForFeasibleTestGoals = pTimeForFeasibleTestGoals;
    mTimeForInfeasibleTestGoals = pTimeForInfeasibleTestGoals;
    mTimeInReach = pTimeInReach;
    mTimeInCover = pTimeInCover;
    mFinished = pFinished;
  }

  public boolean hasFinished() {
    return mFinished;
  }

  public int getNumberOfTestGoals() {
    return mNumberOfTestGoals;
  }

  public int getNumberOfFeasibleTestGoals() {
    return mNumberOfFeasibleTestGoals;
  }

  public int getNumberOfInfeasibleTestGoals() {
    return mNumberOfInfeasibleTestGoals;
  }

  public int getNumberOfBugRevealingTestGoals() {
    return mNumberOfBugRevealingTestGoals;
  }

  public int getNumberOfTestCases() {
    return mNumberOfTestCases;
  }

  public int getNumberOfImpreciseTestCases() {
    return mNumberOfImpreciseTestCases;
  }

  public double getTimeForFeasibleTestGoals() {
    return mTimeForFeasibleTestGoals;
  }

  public double getTimeForInfeasibleTestGoals() {
    return mTimeForInfeasibleTestGoals;
  }

  public double getTimeInReach() {
    return mTimeInReach;
  }

  public double getTimeInCover() {
    return mTimeInCover;
  }

}
