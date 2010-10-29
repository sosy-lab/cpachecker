package org.sosy_lab.cpachecker.fllesh;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.testcases.TestCase;

import com.google.common.base.Preconditions;

public class FlleShResult {

  public static class Factory {
    
    private Set<ElementaryCoveragePattern> mFeasibleTestGoals;
    private Set<ElementaryCoveragePattern> mInfeasibleTestGoals;
    private Map<TestCase, Set<ElementaryCoveragePattern>> mTestSuite;
    private Set<TestCase> mImpreciseTestCases;
    
    private Factory() {
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
    
    public Collection<TestCase> getTestCases() {
      Set<TestCase> lTestCases = new HashSet<TestCase>();
      
      lTestCases.addAll(mTestSuite.keySet());
      lTestCases.addAll(mImpreciseTestCases);
      
      return lTestCases;
    }
    
    public FlleShResult create(double pTimeInReach, double pTimeInCover, double pTimeForFeasibleTestGoals, double pTimeForInfeasibleTestGoals) {
      return new FlleShResult(mFeasibleTestGoals.size(), mInfeasibleTestGoals.size(), mTestSuite.keySet().size(), mImpreciseTestCases.size(), pTimeInReach, pTimeInCover, pTimeForFeasibleTestGoals, pTimeForInfeasibleTestGoals);
    }
    
  }
  
  public static Factory factory() {
    return new Factory();
  }
  
  private int mNumberOfFeasibleTestGoals;
  private int mNumberOfInfeasibleTestGoals;
  private int mNumberOfTestCases;
  private int mNumberOfImpreciseTestCases;
  private double mTimeForFeasibleTestGoals; // seconds
  private double mTimeForInfeasibleTestGoals; // seconds
  private double mTimeInReach; // seconds
  private double mTimeInCover; // seconds
  
  private FlleShResult(int pNumberOfFeasibleTestGoals, int pNumberOfInfeasibleTestGoals, int pNumberOfTestCases, int pNumberOfImpreciseTestCases, double pTimeInReach, double pTimeInCover, double pTimeForFeasibleTestGoals, double pTimeForInfeasibleTestGoals) {
    mNumberOfFeasibleTestGoals = pNumberOfFeasibleTestGoals;
    mNumberOfInfeasibleTestGoals = pNumberOfInfeasibleTestGoals;
    mNumberOfTestCases = pNumberOfTestCases;
    mNumberOfImpreciseTestCases = pNumberOfImpreciseTestCases;
    mTimeForFeasibleTestGoals = pTimeForFeasibleTestGoals;
    mTimeForInfeasibleTestGoals = pTimeForInfeasibleTestGoals;
    mTimeInReach = pTimeInReach;
    mTimeInCover = pTimeInCover;
  }
  
  public int getNumberOfTestGoals() {
    return mNumberOfFeasibleTestGoals + mNumberOfInfeasibleTestGoals + mNumberOfImpreciseTestCases;
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
  
  public double getTimeInReach() {
    return mTimeInReach;
  }
  
  public double getTimeInCover() {
    return mTimeInCover;
  }
  
}
