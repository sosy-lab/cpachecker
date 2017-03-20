/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.waitlist.AutomatonFailedMatchesWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.AutomatonMatchesWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.CallstackSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.ExplicitSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.LoopstackSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.PostorderSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.ReversePostorderSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.ThreadingSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariableWaitlist;
import org.sosy_lab.cpachecker.cpa.usage.UsageReachedSet;

@Options(prefix="analysis")
public class ReachedSetFactory {

  private static enum ReachedSetType {
    NORMAL, LOCATIONMAPPED, PARTITIONED, PSEUDOPARTITIONED, USAGESTATISTICS
  }

  @Option(secure=true, name="traversal.order",
      description="which strategy to adopt for visiting states?")
  Waitlist.TraversalMethod traversalMethod = Waitlist.TraversalMethod.DFS;

  @Option(secure=true, name = "traversal.useCallstack",
      description = "handle states with a deeper callstack first"
      + "\nThis needs the CallstackCPA instance to have any effect.")
  boolean useCallstack = false;

  @Option(secure=true, name="traversal.useLoopstack",
    description= "handle states with a deeper loopstack first.")
  boolean useLoopstack = false;

  @Option(secure=true, name="traversal.useReverseLoopstack",
      description= "handle states with a more shallow loopstack first.")
  boolean useReverseLoopstack = false;

  @Option(secure=true, name = "traversal.useReversePostorder",
      description = "Use an implementation of reverse postorder strategy that allows to select "
      + "a secondary strategy that is used if there are two states with the same reverse postorder id. "
      + "The secondary strategy is selected with 'analysis.traversal.order'.")
  boolean useReversePostorder = false;

  @Option(secure=true, name = "traversal.usePostorder",
      description = "Use an implementation of postorder strategy that allows to select "
      + "a secondary strategy that is used if there are two states with the same postorder id. "
      + "The secondary strategy is selected with 'analysis.traversal.order'.")
  boolean usePostorder = false;

  @Option(secure=true, name = "traversal.useExplicitInformation",
      description = "handle more abstract states (with less information) first? (only for ExplicitCPA)")
  boolean useExplicitInformation = false;

  @Option(secure=true, name = "traversal.useAutomatonInformation",
      description = "handle abstract states with more automaton matches first? (only if AutomatonCPA enabled)")
  boolean useAutomatonInformation = false;

  @Option(secure=true, name = "traversal.byAutomatonVariable",
      description = "traverse in the order defined by the values of an automaton variable")
  @Nullable String byAutomatonVariable = null;

  @Option(secure=true, name = "traversal.useNumberOfThreads",
      description = "handle abstract states with fewer running threads first? (needs ThreadingCPA)")
  boolean useNumberOfThreads = false;

  @Option(secure=true, name = "reachedSet",
      description = "which reached set implementation to use?"
      + "\nNORMAL: just a simple set"
      + "\nLOCATIONMAPPED: a different set per location "
      + "(faster, states with different locations cannot be merged)"
      + "\nPARTITIONED: partitioning depending on CPAs (e.g Location, Callstack etc.)"
      + "\nPSEUDOPARTITIONED: based on PARTITIONED, uses additional info about the states' lattice "
      + "(maybe faster for some special analyses which use merge_sep and stop_sep")
  ReachedSetType reachedSet = ReachedSetType.PARTITIONED;

  public ReachedSetFactory(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  public ReachedSet create() {
    WaitlistFactory waitlistFactory = traversalMethod;

    if (useAutomatonInformation) {
      waitlistFactory = AutomatonMatchesWaitlist.factory(waitlistFactory);
      waitlistFactory = AutomatonFailedMatchesWaitlist.factory(waitlistFactory);
    }
    if (useReversePostorder) {
      waitlistFactory = ReversePostorderSortedWaitlist.factory(waitlistFactory);
    }
    if (usePostorder) {
      waitlistFactory = PostorderSortedWaitlist.factory(waitlistFactory);
    }
    if (useLoopstack) {
      waitlistFactory = LoopstackSortedWaitlist.factory(waitlistFactory);
    }
    if (useReverseLoopstack) {
      waitlistFactory = LoopstackSortedWaitlist.reversedFactory(waitlistFactory);
    }
    if (useCallstack) {
      waitlistFactory = CallstackSortedWaitlist.factory(waitlistFactory);
    }
    if (useExplicitInformation) {
      waitlistFactory = ExplicitSortedWaitlist.factory(waitlistFactory);
    }
    if (byAutomatonVariable != null) {
      waitlistFactory = AutomatonVariableWaitlist.factory(waitlistFactory, byAutomatonVariable);
    }
    if (useNumberOfThreads) {
      waitlistFactory = ThreadingSortedWaitlist.factory(waitlistFactory);
    }

    switch (reachedSet) {
    case PARTITIONED:
      return new PartitionedReachedSet(waitlistFactory);

    case PSEUDOPARTITIONED:
      return new PseudoPartitionedReachedSet(waitlistFactory);

    case LOCATIONMAPPED:
      return new LocationMappedReachedSet(waitlistFactory);

    case USAGESTATISTICS:
      return new UsageReachedSet(waitlistFactory);

    case NORMAL:
    default:
      return new DefaultReachedSet(waitlistFactory);
    }
  }
}
