package org.sosy_lab.cpachecker.cpa.guardededgeautomaton;

import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;

public class GuardedEdgeAutomatonDomain extends FlatLatticeDomain {

  private static final GuardedEdgeAutomatonDomain sInstance = new GuardedEdgeAutomatonDomain();

  public static GuardedEdgeAutomatonDomain getInstance() {
    return sInstance;
  }

  private GuardedEdgeAutomatonDomain() {
    super(GuardedEdgeAutomatonTopElement.getInstance());
  }

}
