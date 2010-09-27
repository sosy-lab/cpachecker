package org.sosy_lab.cpachecker.fllesh;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.c.CtoFormulaConverter;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat.MathsatModel;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat.MathsatModel.MathsatAssignable;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat.MathsatModel.MathsatValue;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.trace.CounterexampleTraceInfo;

public class TestCase {

  private int[] mInputs;
  private boolean mIsPrecise;
  
  private TestCase(LinkedList<Integer> pInputs, boolean pIsPrecise) {
    mInputs = new int[pInputs.size()];
    
    int lIndex = 0;
    for (Integer lValue : pInputs) {
      mInputs[lIndex] = lValue;
      lIndex++;
    }
    
    mIsPrecise = pIsPrecise;
  }
  
  private TestCase(int[] pInputs, boolean pIsPrecise) {
    mInputs = new int[pInputs.length];
    
    for (int lIndex = 0; lIndex < mInputs.length; lIndex++) {
      mInputs[lIndex] = pInputs[lIndex];
    }
    
    mIsPrecise = pIsPrecise;
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
      
      if (mInputs.length == lTestCase.mInputs.length) {
        if (mIsPrecise == lTestCase.mIsPrecise) {
          for (int lIndex = 0; lIndex < mInputs.length; lIndex++) {
            if (mInputs[lIndex] != lTestCase.mInputs[lIndex]) {
              return false;
            }
          }
          
          return true;
        }
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
    
    return new TestCase(lValues, lIsPrecise);
  }
  
  public static TestCase fromCounterexample(CounterexampleTraceInfo pTraceInfo, LogManager pLogManager) {
    return fromCounterexample((MathsatModel)pTraceInfo.getCounterexample(), pLogManager);
  }
  
  public static TestCase fromCounterexample(MathsatModel pCounterexample, LogManager pLogManager) {
    Set<MathsatAssignable> lAssignables = pCounterexample.getAssignables();
    
    boolean lIsPrecise = true;
    
    String lNondetPrefix = CtoFormulaConverter.NONDET_VARIABLE + "@";
    String lNondetFlagPrefix = CtoFormulaConverter.NONDET_FLAG_VARIABLE + "@"; 
    
    SortedMap<Integer, Double> lNondetMap = new TreeMap<Integer, Double>();
    SortedMap<Integer, Boolean> lNondetFlagMap = new TreeMap<Integer, Boolean>();
    
    for (MathsatAssignable lAssignable : lAssignables) {
      String lName = lAssignable.getName();
      
      if (lName.startsWith(lNondetPrefix)) {
        String lNumberString = lName.substring(lNondetPrefix.length());
        
        Integer lIndex = Integer.valueOf(lNumberString);
        
        MathsatValue lValue = pCounterexample.getValue(lAssignable);
        
        double lDoubleValue = Double.parseDouble(lValue.toString());
        
        lNondetMap.put(lIndex, lDoubleValue);
      }
      else if (lName.startsWith(lNondetFlagPrefix)) {
        String lNumberString = lName.substring(lNondetFlagPrefix.length());
        
        Integer lIndex = Integer.valueOf(lNumberString);
        
        MathsatValue lValue = pCounterexample.getValue(lAssignable);
        
        double lDoubleValue = Double.parseDouble(lValue.toString());
        
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
    
    return new TestCase(lInput, lIsPrecise);
  }

}
