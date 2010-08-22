package org.sosy_lab.cpachecker.fllesh;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel.MathsatAssignable;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel.MathsatValue;

public class TestCase {

  public static String INPUT_FUNCTION_NAME = "input";
  
  private String mInputFunction;
  
  private TestCase(String pInputFunction) {
    mInputFunction = pInputFunction;
  }
  
  public static TestCase toTestCase(MathsatModel pCounterexample) {
    Set<MathsatAssignable> lAssignables = pCounterexample.getAssignables();
    
    StringWriter lInputFunction = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lInputFunction);

    lWriter.println("int input()");
    lWriter.println("{");
    lWriter.println("  int value;");
    
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
    
    int lIndex = 0;
    
    for (Map.Entry<Integer, Double> lEntry : lNondetMap.entrySet()) {
      lWriter.println("  if (__FLLESH__input_index == " + lIndex + ")");
      lWriter.println("  {");
      lWriter.println("    value = " + lEntry.getValue().intValue() + ";");
      lWriter.println("  }");
      
      lIndex++;
    }
    
    lWriter.println("  __FLLESH__input_index = __FLLESH__input_index + 1;");
    
    lWriter.println("  return (value);");
    lWriter.println("}");
    
    return new TestCase(lInputFunction.toString());
  }
  
  public String getInputFunction() {
    return mInputFunction;
  }
  
  @Override
  public String toString() {
    return getInputFunction();
  }
  
}
