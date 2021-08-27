// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.cpa.usage.storage.UnsafeDetector;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageConfiguration;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class UsageReachedSet extends PartitionedReachedSet {

  private static final ImmutableSet<TargetInformation> RACE_PROPERTY =
      SimpleTargetInformation.singleton("Race condition");

  private final UsageConfiguration config;
  private final LogManager logger;
  private final UnsafeDetector unsafeDetector;

  private UsageContainer container = null;

  public UsageReachedSet(
      ConfigurableProgramAnalysis pCpa,
      WaitlistFactory waitlistFactory,
      UsageConfiguration pConfig,
      LogManager pLogger) {
    super(pCpa, waitlistFactory);
    config = pConfig;
    logger = pLogger;
    unsafeDetector = new UnsafeDetector(pConfig);
  }

  @Override
  public void remove(AbstractState pState) {
    super.remove(pState);
    if (container != null) {
      UsageState ustate = UsageState.get(pState);
      container.removeState(ustate);
    }
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    super.add(pState, pPrecision);

    UsageState USstate = AbstractStates.extractStateByType(pState, UsageState.class);
    USstate.saveUnsafesInContainerIfNecessary(pState);
  }

  @Override
  public void clear() {
    if (container != null) {
      container.resetUnrefinedUnsafes();
    }
    super.clear();
  }

  @Override
  public boolean wasTargetReached() {
    return getUsageContainer().getTotalUnsafeSize() > 0;
  }

  @Override
  public Set<TargetInformation> getTargetInformation() {
    if (wasTargetReached()) {
      return RACE_PROPERTY;
    } else {
      return ImmutableSet.of();
    }
  }

  public UsageContainer getUsageContainer() {
    if (container == null) {
      container = new UsageContainer(config, logger, unsafeDetector);
    }
    // TODO lastState = null
    UsageState lastState = UsageState.get(getLastState());
    container.initContainerIfNecessary(lastState.getFunctionContainer());
    return container;
  }
}
