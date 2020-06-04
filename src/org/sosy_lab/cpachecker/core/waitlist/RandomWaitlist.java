// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedList;
import java.util.Random;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/** Waitlist implementation that considers states in a random order for pop(). */
@SuppressFBWarnings(
    value = "BC_BAD_CAST_TO_CONCRETE_COLLECTION",
    justification = "warnings is only because of casts introduced by generics")
@SuppressWarnings({"checkstyle:IllegalType", "JdkObsolete"})
public class RandomWaitlist extends AbstractWaitlist<LinkedList<AbstractState>> {

  private static final long serialVersionUID = 1L;

  private final Random rand = new Random(0);

  protected RandomWaitlist() {
    super(new LinkedList<>());
  }

  @Override
  public AbstractState pop() {
    int r = rand.nextInt(waitlist.size());
    return waitlist.remove(r);
  }
}
