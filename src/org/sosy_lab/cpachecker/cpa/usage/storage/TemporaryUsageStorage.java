/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage.storage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

@SuppressFBWarnings(
  justification = "Serialization of container is useless and not supported",
  value = "SE_BAD_FIELD"
)
public class TemporaryUsageStorage extends AbstractUsageStorage {
  private static final long serialVersionUID = -8932709343923545136L;

  // Not set! There was a bug, when two similar usages of different ids are overlapped.
  private final List<UsageInfo> withoutARGState;

  private TemporaryUsageStorage previousStorage;

  private TemporaryUsageStorage(TemporaryUsageStorage previous) {
    super(previous);
    // Copy states without ARG to set it later
    withoutARGState = new ArrayList<>(previous.withoutARGState);
    previousStorage = previous;
  }

  public TemporaryUsageStorage() {
    withoutARGState = new ArrayList<>();
    previousStorage = null;
  }

  @Override
  public boolean add(SingleIdentifier id, UsageInfo info) {
    if (info.getKeyState() == null) {
      withoutARGState.add(info);
    }
    return super.add(id, info);
  }

  public void setKeyState(ARGState state) {
    withoutARGState.forEach(s -> s.setKeyState(state));
    withoutARGState.clear();
  }

  @Override
  public void clear() {
    clearSets();
    TemporaryUsageStorage previous = previousStorage;
    //We cannot use recursion, due to large callstack and stack overflow exception
    while (previous != null) {
      previous.clearSets();
      previous = previous.previousStorage;
    }
    previousStorage = null;
  }

  public TemporaryUsageStorage copy() {
    return new TemporaryUsageStorage(this);
  }

  private void clearSets() {
    super.clear();
    withoutARGState.clear();
  }
}
