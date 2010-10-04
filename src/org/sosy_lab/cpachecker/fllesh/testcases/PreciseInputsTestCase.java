package org.sosy_lab.cpachecker.fllesh.testcases;

import java.util.List;

public class PreciseInputsTestCase extends TestCase {

  public PreciseInputsTestCase(int[] pInputs) {
    super(pInputs, true);
  }
  
  public PreciseInputsTestCase(List<Integer> pInputs) {
    super(pInputs, true);
  }
  
}
