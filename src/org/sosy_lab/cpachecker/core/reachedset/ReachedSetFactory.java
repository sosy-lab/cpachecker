/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.reachedset;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.waitlist.CallstackSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.TopologicallySortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;

@Options(prefix="analysis")
public class ReachedSetFactory {

  private static enum ReachedSetType {
    NORMAL, LOCATIONMAPPED, PARTITIONED
  }
    
  @Option(name="traversal.order")
  Waitlist.TraversalMethod traversalMethod = Waitlist.TraversalMethod.DFS;

  @Option(name="traversal.useCallstack")
  boolean useCallstack = false;

  @Option(name="traversal.useTopsort")
  boolean useTopSort = false;

  @Option(name="reachedSet")
  ReachedSetType reachedSet = ReachedSetType.PARTITIONED;
    
  public ReachedSetFactory(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }
  
  public ReachedSet create() {  
    WaitlistFactory waitlistFactory = traversalMethod;
    if (useTopSort) {
      waitlistFactory = TopologicallySortedWaitlist.factory(waitlistFactory);
    }
    if (useCallstack) {
      waitlistFactory = CallstackSortedWaitlist.factory(waitlistFactory);
    }
    
    switch (reachedSet) {
    case PARTITIONED:
      return new PartitionedReachedSet(waitlistFactory);

    case LOCATIONMAPPED:
      return new LocationMappedReachedSet(waitlistFactory);

    case NORMAL:
    default:
      return new ReachedSet(waitlistFactory);
    } 
  }
}
