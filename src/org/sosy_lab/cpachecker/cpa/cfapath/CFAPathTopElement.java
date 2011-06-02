package org.sosy_lab.cpachecker.cpa.cfapath;

import java.util.Collections;
import java.util.Set;

public class CFAPathTopElement implements CFAPathElement {

  private static final CFAPathTopElement sInstance = new CFAPathTopElement();
  private static final Set<CFAPathTopElement> sSingleton = Collections.singleton(sInstance);

  public static CFAPathTopElement getInstance() {
    return sInstance;
  }

  public static Set<CFAPathTopElement> getSingleton() {
    return sSingleton;
  }

  private CFAPathTopElement() {

  }

}
