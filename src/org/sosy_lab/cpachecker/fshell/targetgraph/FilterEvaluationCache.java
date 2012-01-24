/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.fshell.targetgraph;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.fshell.fql2.ast.filter.Filter;

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
