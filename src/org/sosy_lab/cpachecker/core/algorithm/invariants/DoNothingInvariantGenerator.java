/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/**
 * Trivial implementation of an invariant generator
 * that does nothing and always returns an empty reached set.
 */
public class DoNothingInvariantGenerator implements InvariantGenerator {

  private final UnmodifiableReachedSet reachedSet;
  private final Timer timer = new Timer();

  public DoNothingInvariantGenerator(ReachedSetFactory factory) {
    reachedSet = factory.create();
  }

  @Override
  public void start(CFANode pInitialLocation) { }

  @Override
  public void cancel() { }

  @Override
  public UnmodifiableReachedSet get()  {
    return reachedSet;
  }

  @Override
  public Timer getTimeOfExecution() {
    return timer;
  }
}
