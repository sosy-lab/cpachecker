package org.sosy_lab.cpachecker.fllesh;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat.MathsatModel;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat.MathsatModel.MathsatAssignable;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat.MathsatModel.MathsatValue;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.trace.CounterexampleTraceInfo;

public class SimpleTestCase implements TestCase {

  private int[] mInputs;
  private boolean mIsPrecise;
  
  private SimpleTestCase(LinkedList<Integer> pInputs, boolean pIsPrecise) {
    mInputs = new int[pInputs.size()];
    
    int lIndex = 0;
    for (Integer lValue : pInputs) {
      mInputs[lIndex] = lValue;
      lIndex++;
    }
    
    mIsPrecise = pIsPrecise;
  }

  @Override
  public boolean isPrecise() {
    return mIsPrecise;
  }
  
  @Override
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
      SimpleTestCase lTestCase = (SimpleTestCase)pOther;
      
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
    
    for (int lIndex = 0; lIndex < mInputs.length; lIndex++) {
      lBuffer.append(mInputs[lIndex] + ", ");
    }
    
    return lBuffer.toString();
  }
  
  public static SimpleTestCase fromCounterexample(CounterexampleTraceInfo pTraceInfo, LogManager pLogManager) {
    return fromCounterexample((MathsatModel)pTraceInfo.getCounterexample(), pLogManager);
  }
  
  public static SimpleTestCase fromCounterexample(MathsatModel pCounterexample, LogManager pLogManager) {
    Set<MathsatAssignable> lAssignables = pCounterexample.getAssignables();
    
    boolean lIsPrecise = true;
    
    String lNondetPrefix = "__nondet__@";
    
    SortedMap<Integer, Double> lNondetMap = new TreeMap<Integer, Double>();
    
    for (MathsatAssignable lAssignable : lAssignables) {
      String lName = lAssignable.getName();
      
      if (lName.startsWith(lNondetPrefix)) {
        String lNumberString = lName.substring(lNondetPrefix.length());
        
        Integer lIndex = Integer.valueOf(lNumberString);
        
        MathsatValue lValue = pCounterexample.getValue(lAssignable);
        
        double lDoubleValue = Double.parseDouble(lValue.toString());
        
        lNondetMap.put(lIndex, lDoubleValue);
      }
    }
    
    LinkedList<Integer> lInput = new LinkedList<Integer>();
    
    for (Map.Entry<Integer, Double> lEntry : lNondetMap.entrySet()) {
      Double lValue = lEntry.getValue();
      
      int lIntValue = lEntry.getValue().intValue();
      
      if (lValue.doubleValue() != lIntValue) {
        lIsPrecise = false;
      }
      
      lInput.add(lIntValue);
    }
    
    return new SimpleTestCase(lInput, lIsPrecise);
  }

}
