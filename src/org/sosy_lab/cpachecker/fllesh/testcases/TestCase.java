package org.sosy_lab.cpachecker.fllesh.testcases;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.c.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Model;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.symbpredabstraction.CounterexampleTraceInfo;

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
  
  public static TestCase fromCounterexample(CounterexampleTraceInfo pTraceInfo, LogManager pLogManager) {
    Model lModel = pTraceInfo.getCounterexample();
    
    return fromCounterexample(lModel, pLogManager);
  }
  
  public static TestCase fromCounterexample(Model pCounterexample, LogManager pLogManager) {
    //Set<MathsatAssignable> lAssignables = pCounterexample.getAssignables();
    
    boolean lIsPrecise = true;
    
    String lNondetPrefix = CtoFormulaConverter.NONDET_VARIABLE + "@";
    String lNondetFlagPrefix = CtoFormulaConverter.NONDET_FLAG_VARIABLE + "@"; 
    
    SortedMap<Integer, Double> lNondetMap = new TreeMap<Integer, Double>();
    SortedMap<Integer, Boolean> lNondetFlagMap = new TreeMap<Integer, Boolean>();
    
    for (Map.Entry<AssignableTerm, Object> lAssignment : pCounterexample.entrySet()) {
      AssignableTerm lTerm = lAssignment.getKey();
      
      String lName = lTerm.getName();
      
      if (lName.startsWith(lNondetPrefix)) {
        String lNumberString = lName.substring(lNondetPrefix.length());
        
        Integer lIndex = Integer.valueOf(lNumberString);
        
        double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());
        
        lNondetMap.put(lIndex, lDoubleValue);
      }
      else if (lName.startsWith(lNondetFlagPrefix)) {
        String lNumberString = lName.substring(lNondetFlagPrefix.length());
        
        Integer lIndex = Integer.valueOf(lNumberString);
        
        double lDoubleValue = Double.parseDouble(lAssignment.getValue().toString());
        
        if (lDoubleValue != 0.0) {
          lNondetFlagMap.put(lIndex, true);
        }
        else {
          lNondetFlagMap.put(lIndex, false);
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
