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
package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.util.AbstractStates;

@SuppressFBWarnings(justification = "No support for serialization", value = "SE_BAD_FIELD")
public class UsageReachedSet extends PartitionedReachedSet {

  private static final long serialVersionUID = 1L;

  public static class RaceProperty implements Property {
    @Override
    public String toString() {
      return "Race condition";
    }
  }

  private static final RaceProperty propertyInstance = new RaceProperty();

  private final Configuration config;
  private final LogManager logger;

  private UsageContainer container = null;

  public UsageReachedSet(
      WaitlistFactory waitlistFactory, Configuration pConfig, LogManager pLogger) {
    super(waitlistFactory);
    config = pConfig;
    logger = pLogger;
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
    UsageContainer container = getUsageContainer();
    return container == null ? false : container.getTotalUnsafeSize() > 0;
  }

  @Override
  public Set<Property> getViolatedProperties() {
    if (hasViolatedProperties()) {
      return Collections.singleton(propertyInstance);
    } else {
      return ImmutableSet.of();
    }
  }

  public UsageContainer getUsageContainer() {
    try {
      if (container == null) {
        container = new UsageContainer(config, logger);
      }
      // TODO lastState = null
      UsageState lastState = UsageState.get(getLastState());
      container.initContainerIfNecessary(lastState.getFunctionContainer());
      return container;

    } catch (InvalidConfigurationException e) {
      logger.log(Level.SEVERE, e.getMessage());
      return null;
    }
  }

  private void writeObject(@SuppressWarnings("unused") ObjectOutputStream stream) {
    throw new UnsupportedOperationException("cannot serialize Loger and Configuration.");
  }
}
