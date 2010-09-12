package org.sosy_lab.cpachecker.fllesh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
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

public class StringBasedTestCase implements TestCase {

  public static final String INPUT_FUNCTION_NAME = "input";
  public static final String INPUT_INDEX_VARIABLE = "__FLLESH__input_index";
  
  private String mInputFunction;
  private CFAFunctionDefinitionNode mInputFunctionEntry;
  private boolean mIsPrecise;
  
  private StringBasedTestCase(CFAFunctionDefinitionNode pInputFunctionEntry, String pInputFunction, boolean pIsPrecise) {
    mInputFunctionEntry = pInputFunctionEntry;
    mInputFunction = pInputFunction;
    mIsPrecise = pIsPrecise;
  }
  
  public void toFile(String pFileName) throws FileNotFoundException {
    toFile(new File(pFileName));
  }
  
  public void toFile(File pFile) throws FileNotFoundException {
    PrintWriter lWriter = new PrintWriter(pFile);
    
    lWriter.println("extern int " + StringBasedTestCase.INPUT_INDEX_VARIABLE + ";");
    lWriter.println();
    lWriter.print(mInputFunction);
    
    lWriter.close();
  }
  
  public String getInputFunction() {
    return mInputFunction;
  }
  
  @Override
  public CFAFunctionDefinitionNode getInputFunctionEntry() {
    return mInputFunctionEntry;
  }
  
  @Override
  public String toString() {
    return getInputFunction();
  }
  
  public static StringBasedTestCase fromCounterexample(MathsatModel pCounterexample, LogManager pLogManager) {
    Set<MathsatAssignable> lAssignables = pCounterexample.getAssignables();
    
    boolean lIsPrecise = true;
    
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
      
      Double lValue = lEntry.getValue();
      
      int lIntValue = lEntry.getValue().intValue();
      
      if (lValue.doubleValue() != lIntValue) {
        lIsPrecise = false;
        lWriter.println("    /* precise value: " + lValue.doubleValue() + " */");
      }
      
      lWriter.println("    value = " + lIntValue + ";");
      lWriter.println("  }");
      
      lIndex++;
    }
    
    lWriter.println("  __FLLESH__input_index = __FLLESH__input_index + 1;");
    
    lWriter.println("  return (value);");
    lWriter.println("}");
    
    String lInputFunctionSource = lInputFunction.toString();
    
    TranslationUnit lTranslationUnit = TranslationUnit.parseString(lInputFunctionSource, pLogManager);
    
    return new StringBasedTestCase(lTranslationUnit.getFunction(StringBasedTestCase.INPUT_FUNCTION_NAME), lInputFunctionSource, lIsPrecise);
  }

  @Override
  public boolean isPrecise() {
    return mIsPrecise;
  }
  
}
