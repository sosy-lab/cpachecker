/**
 * 
 */
package common;

import java.util.Collection;

import cfa.objectmodel.CFANode;

import cpa.common.ProjectionWrapper;
import cpa.common.interfaces.AbstractElementWithLocation;

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
    return new ProjectionWrapper(mSet.get(loc));
  }

}
