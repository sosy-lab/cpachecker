package org.sosy_lab.cpachecker.fshell;

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

public class MultiprocessFShell3 {

  public static class FShell3Runnable implements Runnable {

    private final String mCoverageSpecification;
    private final String mSourceFile;
    private final String mEntryFunction;
    private final int mMinGoalIndex;
    private final int mMaxGoalIndex;
    
    public FShell3Runnable(String pCoverageSpecification, String pSourceFile, String pEntryFunction, int pMinGoalIndex, int pMaxGoalIndex) {
      mCoverageSpecification = pCoverageSpecification;
      mSourceFile = pSourceFile;
      mEntryFunction = pEntryFunction;
      mMinGoalIndex = pMinGoalIndex;
      mMaxGoalIndex = pMaxGoalIndex;
    }
    
    @Override
    public void run() {
      String[] lArguments = new String[5];
      
      lArguments[0] = mCoverageSpecification;
      lArguments[1] = mSourceFile;
      lArguments[2] = mEntryFunction;
      lArguments[3] = "--min=" + mMinGoalIndex;
      lArguments[4] = "--max=" + mMaxGoalIndex;
      
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
  
  public static void main(String[] args) throws InterruptedException {
    
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
    
    
    // 3) start threads
    LinkedList<Thread> lThreads = new LinkedList<Thread>();
    
    for (Pair<Integer, Integer> lJob : lJobs) {
      System.out.println("Starting job [" + lJob.getFirst() + ", " + lJob.getSecond() + "]");
      
      Thread lThread = new Thread(new FShell3Runnable(lCoverageSpecification, lSourceFile, lEntryFunction, lJob.getFirst(), lJob.getSecond()));
      
      lThread.start();
      
      lThreads.add(lThread);
    }
    
    for (Thread lThread : lThreads) {
      lThread.join();
    }
    
    
    // 4) output results
    
  }
  
}
