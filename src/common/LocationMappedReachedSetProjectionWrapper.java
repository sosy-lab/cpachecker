/**
 * 
 */
package common;

import java.util.Collection;
import java.util.Set;

import cfa.objectmodel.CFANode;

import cpa.common.ProjectionWrapper;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;

/**
 * @author holzera
 *
 */
public class LocationMappedReachedSetProjectionWrapper extends
                                                      ProjectionWrapper {
  private LocationMappedReachedSet mSet;
  
  public LocationMappedReachedSetProjectionWrapper(
                                                   LocationMappedReachedSet pSet) {
    super(pSet);
    
    mSet = pSet;
  }
  
  public Collection<AbstractElementWithLocation> get(CFANode loc) {
    Set<Pair<AbstractElementWithLocation,Precision>> lSet = mSet.get(loc);
    
    if (lSet == null) {
      return null;
    }
    
    return new ProjectionWrapper(lSet);
  }

}
