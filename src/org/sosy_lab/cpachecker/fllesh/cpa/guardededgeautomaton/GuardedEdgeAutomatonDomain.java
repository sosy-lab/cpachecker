package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton;

import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;

public class GuardedEdgeAutomatonDomain extends FlatLatticeDomain {

  public GuardedEdgeAutomatonDomain() {
    super(GuardedEdgeAutomatonTopElement.getInstance(), GuardedEdgeAutomatonBottomElement.getInstance());
  }

}
