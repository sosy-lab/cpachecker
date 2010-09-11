package org.sosy_lab.cpachecker.fllesh;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;

import com.google.common.base.Preconditions;

public class FlleShResult {

  public static class Factory {
    
    private Task mTask;
    private Set<ElementaryCoveragePattern> mFeasibleTestGoals;
    private Set<ElementaryCoveragePattern> mInfeasibleTestGoals;
    private Map<TestCase, Set<ElementaryCoveragePattern>> mTestSuite;
    private Set<TestCase> mImpreciseTestCases;
    
    private Factory(Task pTask) {
      mTask = pTask;
      mFeasibleTestGoals = new HashSet<ElementaryCoveragePattern>();
      mInfeasibleTestGoals = new HashSet<ElementaryCoveragePattern>();
      mTestSuite = new HashMap<TestCase, Set<ElementaryCoveragePattern>>();
      mImpreciseTestCases = new HashSet<TestCase>();
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
      Preconditions.checkArgument(!pTestCase.isPrecise());
      
      mImpreciseTestCases.add(pTestCase);
    }
    
    private Set<ElementaryCoveragePattern> getTestSuite(TestCase pTestCase) {
      if (mTestSuite.containsKey(pTestCase)) {
        return mTestSuite.get(pTestCase);
      }
      else {
        Set<ElementaryCoveragePattern> lTestSuite = new HashSet<ElementaryCoveragePattern>();
        
        mTestSuite.put(pTestCase, lTestSuite);
        
        return lTestSuite;
      }
    }
    
    public FlleShResult create(double pTimeForFeasibleTestGoals, double pTimeForInfeasibleTestGoals) {
      return new FlleShResult(mTask, mFeasibleTestGoals.size(), mInfeasibleTestGoals.size(), mTestSuite.keySet().size(), mImpreciseTestCases.size(), pTimeForFeasibleTestGoals, pTimeForInfeasibleTestGoals);
    }
    
  }
  
  public static Factory factory(Task pTask) {
    return new Factory(pTask);
  }
  
  private Task mTask;
  private int mNumberOfFeasibleTestGoals;
  private int mNumberOfInfeasibleTestGoals;
  private int mNumberOfTestCases;
  private int mNumberOfImpreciseTestCases;
  private double mTimeForFeasibleTestGoals; // seconds
  private double mTimeForInfeasibleTestGoals; // seconds
  
  private FlleShResult(Task pTask, int pNumberOfFeasibleTestGoals, int pNumberOfInfeasibleTestGoals, int pNumberOfTestCases, int pNumberOfImpreciseTestCases, double pTimeForFeasibleTestGoals, double pTimeForInfeasibleTestGoals) {
    mTask = pTask;
    mNumberOfFeasibleTestGoals = pNumberOfFeasibleTestGoals;
    mNumberOfInfeasibleTestGoals = pNumberOfInfeasibleTestGoals;
    mNumberOfTestCases = pNumberOfTestCases;
    mNumberOfImpreciseTestCases = pNumberOfImpreciseTestCases;
    mTimeForFeasibleTestGoals = pTimeForFeasibleTestGoals;
    mTimeForInfeasibleTestGoals = pTimeForInfeasibleTestGoals;
  }
  
  public Task getTask() {
    return mTask;
  }
  
  public int getNumberOfFeasibleTestGoals() {
    return mNumberOfFeasibleTestGoals;
  }
  
  public int getNumberOfInfeasibleTestGoals() {
    return mNumberOfInfeasibleTestGoals;
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
  
}
