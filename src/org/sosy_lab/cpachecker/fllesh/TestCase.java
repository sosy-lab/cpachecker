package org.sosy_lab.cpachecker.fllesh;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

public interface TestCase {

  public CFAFunctionDefinitionNode getInputFunctionEntry();
  public boolean isPrecise();
  
}
