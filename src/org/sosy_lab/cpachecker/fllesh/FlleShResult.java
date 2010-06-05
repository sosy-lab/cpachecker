package org.sosy_lab.cpachecker.fllesh;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;

public class FlleShResult {

  public static class Factory {
    
    private Task mTask;
    private Set<ElementaryCoveragePattern> mFeasibleTestGoals;
    private Set<ElementaryCoveragePattern> mInfeasibleTestGoals;
    private Map<Object, Set<ElementaryCoveragePattern>> mTestSuite;
    
    private Factory(Task pTask) {
      mTask = pTask;
      mFeasibleTestGoals = new HashSet<ElementaryCoveragePattern>();
      mInfeasibleTestGoals = new HashSet<ElementaryCoveragePattern>();
      mTestSuite = new HashMap<Object, Set<ElementaryCoveragePattern>>();
    }
    
    public void add(ElementaryCoveragePattern pECP, boolean pIsFeasible) {
      if (pIsFeasible) {
        mFeasibleTestGoals.add(pECP);
      }
      else {
        mInfeasibleTestGoals.add(pECP);
      }
    }
    
    public void addFeasibleTestCase(ElementaryCoveragePattern pECP, Object pTestCase) {
      mFeasibleTestGoals.add(pECP);
      Set<ElementaryCoveragePattern> lTestSuite = getTestSuite(pTestCase);
      lTestSuite.add(pECP);
    }
    
    public void addInfeasibleTestCase(ElementaryCoveragePattern pECP) {
      mInfeasibleTestGoals.add(pECP);
    }
    
    private Set<ElementaryCoveragePattern> getTestSuite(Object pTestCase) {
      if (mTestSuite.containsKey(pTestCase)) {
        return mTestSuite.get(pTestCase);
      }
      else {
        Set<ElementaryCoveragePattern> lTestSuite = new HashSet<ElementaryCoveragePattern>();
        
        mTestSuite.put(pTestCase, lTestSuite);
        
        return lTestSuite;
      }
    }
    
    public FlleShResult create() {
      return new FlleShResult(mTask, mFeasibleTestGoals.size(), mInfeasibleTestGoals.size(), mTestSuite.keySet().size());
    }
    
  }
  
  public static Factory factory(Task pTask) {
    return new Factory(pTask);
  }
  
  private Task mTask;
  private int mNumberOfFeasibleTestGoals;
  private int mNumberOfInfeasibleTestGoals;
  private int mNumberOfTestCases;
  
  private FlleShResult(Task pTask, int pNumberOfFeasibleTestGoals, int pNumberOfInfeasibleTestGoals, int pNumberOfTestCases) {
    mTask = pTask;
    mNumberOfFeasibleTestGoals = pNumberOfFeasibleTestGoals;
    mNumberOfInfeasibleTestGoals = pNumberOfInfeasibleTestGoals;
    mNumberOfTestCases = pNumberOfTestCases;
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
  
}
