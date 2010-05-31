package org.sosy_lab.cpachecker.fllesh.targetgraph;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Filter;

public class FilterEvaluationCache {
  
  private Map<TargetGraph, Map<Filter, TargetGraph>> mCache;
  
  public FilterEvaluationCache() {
    mCache = new HashMap<TargetGraph, Map<Filter, TargetGraph>>();
  }
  
  public boolean isCached(TargetGraph pTargetGraph, Filter pFilter) {
    if (mCache.containsKey(pTargetGraph)) {
      return mCache.get(pTargetGraph).containsKey(pFilter);
    }
    else {
      return false;
    }
  }
  
  public TargetGraph get(TargetGraph pTargetGraph, Filter pFilter) {
    return mCache.get(pTargetGraph).get(pFilter);
  }
  
  public void add(TargetGraph pSourceGraph, Filter pFilter, TargetGraph pResultGraph) {
    if (!mCache.containsKey(pSourceGraph)) {
      mCache.put(pSourceGraph, new HashMap<Filter, TargetGraph>());
    }
    
    mCache.get(pSourceGraph).put(pFilter, pResultGraph);
  }
  
}
