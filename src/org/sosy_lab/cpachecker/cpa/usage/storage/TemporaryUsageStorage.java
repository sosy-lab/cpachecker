// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

import static com.google.common.collect.FluentIterable.from;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

@SuppressFBWarnings(
    justification = "Serialization of container is useless and not supported",
    value = "SE_BAD_FIELD")
public class TemporaryUsageStorage extends AbstractUsageStorage {
  private static final long serialVersionUID = -8932709343923545136L;

  // Not set! There was a bug, when two similar usages of different ids are overlapped.
  private final List<UsageInfo> withoutARGState;

  private TemporaryUsageStorage(TemporaryUsageStorage previous) {
    super(previous);
    // Copy states without ARG to set it later
    withoutARGState = new ArrayList<>(previous.withoutARGState);
  }

  public TemporaryUsageStorage() {
    withoutARGState = new ArrayList<>();
  }

  @Override
  public void addUsages(SingleIdentifier id, NavigableSet<UsageInfo> usages) {
    from(usages).filter(u -> u.getKeyState() == null).forEach(withoutARGState::add);
    super.addUsages(id, usages);
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

  public TemporaryUsageStorage copy() {
    return new TemporaryUsageStorage(this);
  }

  @Override
  public void clear() {
    super.clear();
    withoutARGState.clear();
  }
}
