// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.error;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class DummyErrorState extends ARGState {

  private static final long serialVersionUID = 1338393013733003150L;

  public DummyErrorState(final AbstractState pWrapped) {
    super(pWrapped, null);
  }

  @Override
  public boolean isTarget() {
    return true;
  }

  @Override
  public Set<Property> getViolatedProperties() {
    return ImmutableSet.of();
  }

  @Override
  public Object getPartitionKey() {
    return null;
  }
}
