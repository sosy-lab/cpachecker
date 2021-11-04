// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ObjectOutputStream;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.NamedProperty;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.cpa.usage.storage.UnsafeDetector;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageConfiguration;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.util.AbstractStates;

@SuppressFBWarnings(justification = "No support for serialization", value = "SE_BAD_FIELD")
public class UsageReachedSet extends PartitionedReachedSet {

  private static final long serialVersionUID = 1L;

  private static final ImmutableSet<Property> RACE_PROPERTY =
      NamedProperty.singleton("Race condition");

  private final UsageConfiguration config;
  private final LogManager logger;
  private final UnsafeDetector unsafeDetector;

  private UsageContainer container = null;

  public UsageReachedSet(
      WaitlistFactory waitlistFactory, UsageConfiguration pConfig, LogManager pLogger) {
    super(waitlistFactory);
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
  public boolean hasViolatedProperties() {
    return getUsageContainer().getTotalUnsafeSize() > 0;
  }

  @Override
  public Set<Property> getViolatedProperties() {
    if (hasViolatedProperties()) {
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

  private void writeObject(@SuppressWarnings("unused") ObjectOutputStream stream) {
    throw new UnsupportedOperationException("cannot serialize Logger");
  }
}
