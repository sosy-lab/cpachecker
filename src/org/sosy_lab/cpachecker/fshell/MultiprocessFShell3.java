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
package org.sosy_lab.cpachecker.fshell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.fshell.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.fshell.testcases.TestSuite;

public class MultiprocessFShell3 {

  public static class CommunicatingJob implements Runnable {

    private final String mCoverageSpecification;
    private final String mSourceFile;
    private final String mEntryFunction;
    private final int mMinGoalIndex;
    private final int mMaxGoalIndex;
    private final String mGlobalTestsuiteFile;

    public CommunicatingJob(String pCoverageSpecification, String pSourceFile, String pEntryFunction, int pMinGoalIndex, int pMaxGoalIndex, String pGlobalTestsuiteFile) {
      mCoverageSpecification = pCoverageSpecification;
      mSourceFile = pSourceFile;
      mEntryFunction = pEntryFunction;
      mMinGoalIndex = pMinGoalIndex;
      mMaxGoalIndex = pMaxGoalIndex;
      mGlobalTestsuiteFile = pGlobalTestsuiteFile;
    }

    @Override
    public void run() {
      LinkedList<Pair<Integer, Integer>> lJobs = new LinkedList<Pair<Integer, Integer>>();

      File lTmpTestsuiteFile = null;
      try {
        lTmpTestsuiteFile = File.createTempFile("testsuite", ".tst");
        lTmpTestsuiteFile.deleteOnExit();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      File lTmpFeasibilityFile = null;
      try {
        lTmpFeasibilityFile = File.createTempFile("testsuite", ".tst");
        lTmpFeasibilityFile.deleteOnExit();
      } catch (IOException e) {
        e.printStackTrace();
      }

      FeasibilityInformation lFeasibilityInformation = new FeasibilityInformation();
      try {
        lFeasibilityInformation.setTestsuiteFilename(lTmpTestsuiteFile.getCanonicalPath());
      } catch (IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }

      try {
        lFeasibilityInformation.write(lTmpFeasibilityFile.getCanonicalPath());
      } catch (FileNotFoundException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      } catch (IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }

      // a) subdivide jobs
      int lNumberOfTestGoals = mMaxGoalIndex - mMinGoalIndex + 1;

      int lSize = 1000;

      if (lNumberOfTestGoals > lSize) {
        int lMinIndex;

        for (lMinIndex = mMinGoalIndex; lMinIndex < mMaxGoalIndex; lMinIndex += lSize) {
          Pair<Integer, Integer> lJob = Pair.of(lMinIndex, lMinIndex + lSize - 1);
          lJobs.add(lJob);
        }

        Pair<Integer, Integer> lLastJob = Pair.of(lMinIndex, mMaxGoalIndex);
        lJobs.add(lLastJob);
      }
      else {
        Pair<Integer, Integer> lJob = Pair.of(mMinGoalIndex, mMaxGoalIndex);
        lJobs.add(lJob);
      }

      for (Pair<Integer, Integer> lJob : lJobs) {
        // b) start job

        // b.1) read global testsuite
        TestSuite lGlobalTestsuite = readGlobalTestsuite(mGlobalTestsuiteFile);

        if (lGlobalTestsuite != null) {
          try {
            lGlobalTestsuite.write(lTmpTestsuiteFile);
          } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }

        String[] lArguments = new String[6];

        lArguments[0] = mCoverageSpecification;
        lArguments[1] = mSourceFile;
        lArguments[2] = mEntryFunction;
        lArguments[3] = "--min=" + lJob.getFirst();
        lArguments[4] = "--max=" + lJob.getSecond();

        try {
          lArguments[5] = "--in=" + lTmpFeasibilityFile.getCanonicalPath();
        } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }

        try {
          RestartingFShell3.main(lArguments);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        // b.2) write global testsuite
        try {
          writeGlobalTestsuite(lTmpTestsuiteFile.getAbsolutePath(), mGlobalTestsuiteFile);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

  }

  private static synchronized TestSuite readGlobalTestsuite(String pFilename) {
    TestSuite lTestSuite = null;

    try {
      lTestSuite = TestSuite.load(pFilename);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return lTestSuite;
  }

  private static synchronized void writeGlobalTestsuite(String pLocalTestsuite, String pGlobalTestsuite) throws IOException {
    TestSuite lLocalTestsuite = TestSuite.load(pLocalTestsuite);

    TestSuite lGlobalTestsuite = TestSuite.load(pGlobalTestsuite);

    lGlobalTestsuite.add(lLocalTestsuite);

    lGlobalTestsuite.write(pGlobalTestsuite);
  }

  public static class FShell3Runnable implements Runnable {

    private final String mCoverageSpecification;
    private final String mSourceFile;
    private final String mEntryFunction;
    private final int mMinGoalIndex;
    private final int mMaxGoalIndex;
    private final String mInitialTestsuite;

    public FShell3Runnable(String pCoverageSpecification, String pSourceFile, String pEntryFunction, int pMinGoalIndex, int pMaxGoalIndex) {
      this(pCoverageSpecification, pSourceFile, pEntryFunction, pMinGoalIndex, pMaxGoalIndex, null);
    }

    public FShell3Runnable(String pCoverageSpecification, String pSourceFile, String pEntryFunction, int pMinGoalIndex, int pMaxGoalIndex, String pInitialTestsuite) {
      mCoverageSpecification = pCoverageSpecification;
      mSourceFile = pSourceFile;
      mEntryFunction = pEntryFunction;
      mMinGoalIndex = pMinGoalIndex;
      mMaxGoalIndex = pMaxGoalIndex;
      mInitialTestsuite = pInitialTestsuite;
    }

    @Override
    public void run() {
      String[] lArguments;

      if (mInitialTestsuite != null) {
        lArguments = new String[6];
      }
      else {
        lArguments = new String[5];
      }

      lArguments[0] = mCoverageSpecification;
      lArguments[1] = mSourceFile;
      lArguments[2] = mEntryFunction;
      lArguments[3] = "--min=" + mMinGoalIndex;
      lArguments[4] = "--max=" + mMaxGoalIndex;

      if (mInitialTestsuite != null) {
        lArguments[5] = "--in=" + mInitialTestsuite;
      }

      try {
        RestartingFShell3.main(lArguments);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

  }

  private static FQLSpecification getFQLSpecification(String pFQLSpecification) {
    // Parse FQL Specification
    FQLSpecification lFQLSpecification;
    try {
      lFQLSpecification = FQLSpecification.parse(pFQLSpecification);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return lFQLSpecification;
  }

  private static int getNumberOfTestGoals(String pCoverageSpecification, String pSourceFile, String pEntryFunction) {
    Map<String, CFAFunctionDefinitionNode> lCFAMap;
    CFAFunctionDefinitionNode lMainFunction;

    try {
      Configuration mConfiguration = FShell3.createConfiguration(pSourceFile, pEntryFunction);
      LogManager mLogManager = new LogManager(mConfiguration);

      lCFAMap = FShell3.getCFAMap(pSourceFile, mConfiguration, mLogManager);
      lMainFunction = lCFAMap.get(pEntryFunction);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    CoverageSpecificationTranslator mCoverageSpecificationTranslator = new CoverageSpecificationTranslator(lMainFunction);

    FQLSpecification lFQLSpecification = getFQLSpecification(pCoverageSpecification);

    IncrementalCoverageSpecificationTranslator lTranslator = new IncrementalCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator);

    int lNumberOfTestGoals = lTranslator.getNumberOfTestGoals(lFQLSpecification.getCoverageSpecification());

    return lNumberOfTestGoals;
  }

  public static void main(String[] args) throws InterruptedException, IOException {

    String lCoverageSpecification = args[0];
    String lSourceFile = args[1];
    String lEntryFunction = args[2];
    int lNumberOfProcesses = Integer.valueOf(args[3]);


    // 1) determine number of test goals
    int lNumberOfTestGoals = getNumberOfTestGoals(lCoverageSpecification, lSourceFile, lEntryFunction);
    System.out.println("NUMBER OF TESTGOALS: " + lNumberOfTestGoals);


    // 2) determine jobs (goal intervals)
    int lJobSize = lNumberOfTestGoals/lNumberOfProcesses;

    LinkedList<Pair<Integer, Integer>> lJobs = new LinkedList<Pair<Integer, Integer>>();

    for (int lIndex = 0; lIndex < lNumberOfProcesses - 1; lIndex++) {
      int lMin = lIndex * lJobSize;
      int lMax = lIndex * lJobSize + lJobSize - 1;

      Pair<Integer, Integer> lJob = Pair.of(lMin, lMax);

      lJobs.add(lJob);
    }

    // last job
    int lMin = (lNumberOfProcesses - 1) * lJobSize;
    int lMax = lNumberOfTestGoals;

    Pair<Integer, Integer> lLastJob = Pair.of(lMin, lMax);

    lJobs.add(lLastJob);


    File lGlobalTestsuiteFile = File.createTempFile("testsuite", ".tst");

    // 3) start threads
    LinkedList<Thread> lThreads = new LinkedList<Thread>();

    for (Pair<Integer, Integer> lJob : lJobs) {
      /*System.out.println("Starting job [" + lJob.getFirst() + ", " + lJob.getSecond() + "]");

      Thread lThread = new Thread(new FShell3Runnable(lCoverageSpecification, lSourceFile, lEntryFunction, lJob.getFirst(), lJob.getSecond()));

      lThread.start();

      lThreads.add(lThread);*/

      System.out.println("Starting job [" + lJob.getFirst() + ", " + lJob.getSecond() + "]");

      Thread lThread = new Thread(new CommunicatingJob(lCoverageSpecification, lSourceFile, lEntryFunction, lJob.getFirst(), lJob.getSecond(), lGlobalTestsuiteFile.getAbsolutePath()));

      lThread.start();

      lThreads.add(lThread);
    }

    for (Thread lThread : lThreads) {
      lThread.join();
    }

    // 4) output results

    // TODO implement sharing of test suites generated by different processes

  }

}
