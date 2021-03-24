// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;

/** Represents one abstract state of the Usage CPA. */
public class UsageStateConservative extends UsageState
{

  private static final long serialVersionUID = -8505134232125928168L;

  private UsageStateConservative(
      final AbstractState pWrappedElement,
      final ImmutableMap<AbstractIdentifier, AbstractIdentifier> pVarBind,
      final UsageState.StateStatistics pStats) {
    super(pWrappedElement, pVarBind, pStats);
  }

  public static UsageStateConservative createInitialState(final AbstractState pWrappedElement) {
    return new UsageStateConservative(
        pWrappedElement,
        ImmutableMap.of(),
        new UsageState.StateStatistics());
  }

  @Override
  public UsageStateConservative copy(final AbstractState pWrappedState) {
    return new UsageStateConservative(pWrappedState, this.variableBindingRelation, this.stats);
  }

  @Override
  protected UsageStateConservative createState(
      final AbstractState pWrappedState,
      final ImmutableMap<AbstractIdentifier, AbstractIdentifier> pVarBind,
      final UsageState.StateStatistics pStats) {
    return new UsageStateConservative(pWrappedState, pVarBind, pStats);
  }

  @Override
  public void filterAliases(AbstractIdentifier pIdentifier, Collection<AbstractIdentifier> pSet) {
    return;
  }
}
