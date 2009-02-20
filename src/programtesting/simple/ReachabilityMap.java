/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting.simple;

import java.util.HashMap;

/**
 *
 * @author holzera
 */
public class ReachabilityMap<E> {
  public enum ReachabilityStatus {
    REACHABLE,
    UNKNOWN,
    UNREACHABLE
  }
  
  private HashMap<E, Boolean> mReachabilityMap;
  
  public ReachabilityMap() {
    mReachabilityMap = new HashMap<E, Boolean>();
  }
  
  public ReachabilityStatus isReachable(E pElement) {
    assert(pElement != null);
    
    if (mReachabilityMap.containsKey(pElement)) {
      if (mReachabilityMap.get(pElement).booleanValue()) {
        return ReachabilityStatus.REACHABLE;
      }
      else {
        return ReachabilityStatus.UNREACHABLE;
      }
    }
    else {
      return ReachabilityStatus.UNKNOWN;
    }
  }
  
  public void set(E pElement, ReachabilityStatus pStatus) {
    assert(pElement != null);
    assert(pStatus != null);
    
    switch (pStatus) {
      case REACHABLE:
        mReachabilityMap.put(pElement, Boolean.TRUE);
        break;
      case UNREACHABLE:
        mReachabilityMap.put(pElement, Boolean.FALSE);
        break;
      case UNKNOWN:
        mReachabilityMap.remove(pElement);
    }
  }
  
  public void setReachable(E pElement) {
    set(pElement, ReachabilityStatus.REACHABLE);
  }
  
  public void setUnreachable(E pElement) {
    set(pElement, ReachabilityStatus.UNREACHABLE);
  }
  
  public void setUnknown(E pElement) {
    set(pElement, ReachabilityStatus.UNREACHABLE);
  }
}
