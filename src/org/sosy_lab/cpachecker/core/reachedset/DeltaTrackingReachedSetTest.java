// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.alwaystop.AlwaysTopCPA;

public class DeltaTrackingReachedSetTest {

  private static final class DummyState implements AbstractState {}

  private final AbstractState s1 = new DummyState();
  private final AbstractState s2 = new DummyState();

  private static ReachedSet delegate() {
    return new DefaultReachedSet(AlwaysTopCPA.INSTANCE, Waitlist.TraversalMethod.DFS);
  }

  @Test
  public void observersAreIndependent() {
    DeltaTrackingReachedSet reached = new DeltaTrackingReachedSet(delegate());
    reached.registerObserver("a");
    reached.registerObserver("b");

    reached.addNoWaitlist(s1, SingletonPrecision.getInstance());
    reached.clearDelta("a");
    reached.addNoWaitlist(s2, SingletonPrecision.getInstance());

    assertThat(reached.getDelta("a")).containsExactly(s2);
    assertThat(reached.getDelta("b")).containsExactly(s1, s2).inOrder();
  }

  @Test
  public void duplicateIdThrows() {
    DeltaTrackingReachedSet reached = new DeltaTrackingReachedSet(delegate());
    reached.registerObserver("a");

    assertThrows(IllegalArgumentException.class, () -> reached.registerObserver("a"));
  }
}