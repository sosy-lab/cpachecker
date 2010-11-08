package org.sosy_lab.cpachecker.cpa.assume;

import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;

public class AssumeDomain extends FlatLatticeDomain {
  
  public AssumeDomain() {
    super(AssumeTopElement.getInstance());
  }
  
}
