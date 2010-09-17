package org.sosy_lab.cpachecker.fllesh;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.fllesh.cfa.TranslationUnit;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel.MathsatAssignable;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel.MathsatValue;

public class SimpleTestCase implements TestCase {

  public int[] mInputs;
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
  public CFAFunctionDefinitionNode getInputFunctionEntry() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isPrecise() {
    return mIsPrecise;
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
