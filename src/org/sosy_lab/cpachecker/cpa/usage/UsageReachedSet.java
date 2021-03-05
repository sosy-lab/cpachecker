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
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.cpa.usage.storage.ConcurrentUsageExtractor;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageConfiguration;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@SuppressFBWarnings(justification = "No support for serialization", value = "SE_BAD_FIELD")
public class UsageReachedSet extends PartitionedReachedSet {

  private static final long serialVersionUID = 1L;

  private boolean usagesExtracted = false;

  public static class RaceProperty implements Property {
    @Override
    public String toString() {
      return "Race condition";
    }
  }

  private static final ImmutableSet<Property> RACE_PROPERTY = ImmutableSet.of(new RaceProperty());

  private final LogManager logger;
  private final UsageConfiguration usageConfig;
  private ConcurrentUsageExtractor extractor = null;

  private final UsageContainer container;

  public UsageReachedSet(
      WaitlistFactory waitlistFactory, UsageConfiguration pConfig, LogManager pLogger) {
    super(waitlistFactory);
    logger = pLogger;
    container = new UsageContainer(pConfig, logger);
    usageConfig = pConfig;
  }

  @Override
  public void remove(AbstractState pState) {
    super.remove(pState);
    UsageState ustate = UsageState.get(pState);
    container.removeState(ustate);
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    super.add(pState, pPrecision);

    /*UsageState USstate = UsageState.get(pState);
    USstate.saveUnsafesInContainerIfNecessary(pState);*/
  }

  @Override
  public void clear() {
    container.resetUnrefinedUnsafes();
    usagesExtracted = false;
    super.clear();
  }

  @Override
  public boolean hasViolatedProperties() {
    if (!usagesExtracted) {
      extractor.extractUsages(getFirstState());
      usagesExtracted = true;
    }
    return container.hasUnsafes();
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
    return container;
  }

  public Collection<Pair<UsageInfo, UsageInfo>> getUnsafes() {
    return container.getStableUnsafes();
  }

  private void writeObject(@SuppressWarnings("unused") ObjectOutputStream stream) {
    throw new UnsupportedOperationException("cannot serialize Logger");
  }

  @Override
  public void finalize(ConfigurableProgramAnalysis pCpa) {
    BAMCPA bamCPA = CPAs.retrieveCPA(pCpa, BAMCPA.class);
    if (bamCPA != null) {
      UsageCPA uCpa = CPAs.retrieveCPA(pCpa, UsageCPA.class);
      uCpa.getStats().setBAMCPA(bamCPA);
    }
    extractor = new ConcurrentUsageExtractor(pCpa, logger, container, usageConfig);
  }

  public void printStatistics(StatisticsWriter pWriter) {
    extractor.printStatistics(pWriter);
  }

  public BAMDataManager getBAMDataManager() {
    return extractor.getManager();
  }

}
