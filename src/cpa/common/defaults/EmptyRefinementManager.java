/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.common.defaults;

import cpa.common.Path;
import cpa.common.ReachedElements;
import cpa.common.RefinementOutcome;
import cpa.common.interfaces.RefinementManager;

/**
 * This class implements a RefinementManager for CPAs where actually no refinement can 
 * be performed, so that they still can implement the interface RefinableCPA.
 * An empty refinement outcome is returned.
 * 
 * @author endler
 */
public class EmptyRefinementManager implements RefinementManager {

  @Override
  public RefinementOutcome performRefinement(ReachedElements pReached, Path pPath) {
    return new RefinementOutcome();
  }

  private static final RefinementManager instance = new EmptyRefinementManager();
  
  public static RefinementManager getInstance() {
    return instance;
  }
  
}
