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
package org.sosy_lab.cpachecker.fshell.testcases;

import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;

public class ImpreciseExecutionException extends Exception {

  private static final long serialVersionUID = 1L;
  private final ImpreciseExecutionTestCase mTestCase;
  private final GuardedEdgeAutomatonCPA mCoverCPA;
  private final GuardedEdgeAutomatonCPA mPassingCPA;

  public ImpreciseExecutionException(TestCase pTestCase, GuardedEdgeAutomatonCPA pCoverCPA, GuardedEdgeAutomatonCPA pPassingCPA) {
    super();
    mTestCase = new ImpreciseExecutionTestCase(pTestCase);

    mCoverCPA = pCoverCPA;
    mPassingCPA = pPassingCPA;
  }

  public ImpreciseExecutionTestCase getTestCase() {
    return mTestCase;
  }

  public GuardedEdgeAutomatonCPA getCoverCPA() {
    return mCoverCPA;
  }

  public GuardedEdgeAutomatonCPA getPassingCPA() {
    return mPassingCPA;
  }

  @Override
  public String toString() {
    return "The test case " + mTestCase.toString() + " causes a wrong execution.\n" + mCoverCPA.toString() + ((mPassingCPA == null)?"":("\n" + mPassingCPA.toString()));
  }

}
