package org.sosy_lab.cpachecker.cpa.cfapath;

import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;

public class CFAPathDomain extends FlatLatticeDomain {
  
  private static final CFAPathDomain sDomainInstance = new CFAPathDomain();
  
  public static CFAPathDomain getInstance() {
    return sDomainInstance;
  }
  
  public CFAPathDomain() {
    super(CFAPathTopElement.getInstance(), CFAPathBottomElement.getInstance());
  }

}
