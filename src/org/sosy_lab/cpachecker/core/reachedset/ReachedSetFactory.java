/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
import org.sosy_lab.cpachecker.core.waitlist.ComparatorWaitlist.ComparatorWaitlistFactory;
import org.sosy_lab.cpachecker.core.waitlist.ExplicitSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.TopologicallySortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;

@Options(prefix="analysis")
public class ReachedSetFactory {

  private static enum ReachedSetType {
    NORMAL, LOCATIONMAPPED, PARTITIONED
  }

  @Option(name="traversal.order",
      description="which strategy to adopt for visiting states?")
  Waitlist.TraversalMethod traversalMethod = Waitlist.TraversalMethod.DFS;

  @Option(name = "traversal.useCallstack",
      description = "handle states with a deeper callstack first?"
      + "\nThis needs the CallstackCPA to have any effect.")
  boolean useCallstack = false;

  @Option(name = "traversal.useTopsort",
      description = "Use an implementation of topsort strategy that allows to select "
      + "a secondary strategy that is used if there are two elements with the same topsort id. "
      + "The secondary strategy is selected with 'analysis.traversal.order'. "
      + "The secondary strategy may not be TOPSORT.")
  boolean useTopSort = false;

  @Option(name = "traversal.useExplicitInformation",
      description = "handle more abstract states (with less information) first? (only for ExplicitCPA)")
  boolean useExplicitInformation = false;

  @Option(name = "traversal.useComparatorWaitlist",
        description = "Use comparator-based waitlist")
  boolean useComparatorWaitlist = false;

  @Option(name = "traversal.comparator",
        description = "Choose comparator for comparator waitlist.")
  private ComparatorWaitlistFactory comparator = ComparatorWaitlistFactory.ENVAPP_MIN;

  @Option(name = "reachedSet",
      description = "which reached set implementation to use?"
      + "\nNORMAL: just a simple set"
      + "\nLOCATIONMAPPED: a different set per location "
      + "(faster, elements with different locations cannot be merged)"
      + "\nPARTITIONED: partitioning depending on CPAs (e.g Location, Callstack etc.)")
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
    if (useExplicitInformation) {
      waitlistFactory = ExplicitSortedWaitlist.factory(waitlistFactory);
    }

    if (useComparatorWaitlist){
      waitlistFactory = comparator;
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
