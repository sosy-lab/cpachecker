package org.sosy_lab.cpachecker.fllesh;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.fllesh.cfa.TranslationUnit;

import com.google.common.base.Preconditions;

public class FileBasedTestCase implements TestCase {

  private String mSourceFileName;
  private CFAFunctionDefinitionNode mInputFunctionEntry;
  
  private FileBasedTestCase(CFAFunctionDefinitionNode pInputFunctionEntry, String pSourceFileName) {
    Preconditions.checkNotNull(pInputFunctionEntry);
    Preconditions.checkNotNull(pSourceFileName);
    
    mInputFunctionEntry = pInputFunctionEntry;
    mSourceFileName = pSourceFileName;
  }
  
  public String getSourceFileName() {
    return mSourceFileName;
  }
  
  @Override
  public CFAFunctionDefinitionNode getInputFunctionEntry() {
    return mInputFunctionEntry;
  }
  
  @Override
  public String toString() {
    return getSourceFileName();
  }
  
  public static FileBasedTestCase fromCFile(String pSourceFileName, LogManager pLogManager) {
    TranslationUnit lTranslationUnit = TranslationUnit.parseFile(pSourceFileName, pLogManager);
    
    CFAFunctionDefinitionNode lInputFunctionEntry = lTranslationUnit.getFunction(StringBasedTestCase.INPUT_FUNCTION_NAME);
    
    FileBasedTestCase lTestCase = new FileBasedTestCase(lInputFunctionEntry, pSourceFileName);
    
    return lTestCase;
  }

}
