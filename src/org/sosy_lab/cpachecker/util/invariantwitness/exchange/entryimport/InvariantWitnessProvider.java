// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.entryimport;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantStoreUtil;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.LoopInvariantEntry;

/** Represents an invariant witness source. */
public final class InvariantWitnessProvider implements AutoCloseable {
  private final InvariantStoreEntryParser parser;
  private final FromDiskEntryProvider entryProvider;
  private final Set<InvariantWitness> knownWitnesses;

  private InvariantWitnessProvider(
      InvariantStoreEntryParser pParser, FromDiskEntryProvider pEntryProvider) {
    parser = Objects.requireNonNull(pParser);
    entryProvider = Objects.requireNonNull(pEntryProvider);

    knownWitnesses = ConcurrentHashMap.newKeySet();
  }

  /**
   * Returns a new instance of this class. The instance is configured according to the given config.
   *
   * <p>The returned instance loads the entries from disk. That is, it watches a configured
   * directory for new invariant files and loads them.
   *
   * @param pConfig Configuration with which the instance shall be created
   * @param pCFA CFA representing the program of the invariants that the instance loads
   * @param pLogger Logger
   * @param pShutdownNotifier ShutdownNotifier
   * @return Instance of this class
   * @throws InvalidConfigurationException if the configuration is (semantically) invalid
   * @throws IOException if the program files can not be accessed (access is required to translate
   *     the location mapping)
   */
  public static InvariantWitnessProvider getNewFromDiskWitnessProvider(
      Configuration pConfig, CFA pCFA, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, IOException {

    ListMultimap<String, Integer> lineOffsetsByFile =
        InvariantStoreUtil.getLineOffsetsByFile(pCFA.getFileNames());
    FromDiskEntryProvider entryProvider =
        FromDiskEntryProvider.getNewFromDiskEntryProvider(pConfig);
    entryProvider.start();

    // Note that the witnessProvider actually takes ownership of the entryProvider and is
    // responsible for closing it. Consequently, never make the entryProvider visible to the outside
    // (e.g. by adding it as an
    // argument to other static methods).
    return new InvariantWitnessProvider(
        InvariantStoreEntryParser.getNewInvariantStoreEntryParser(
            pConfig, pLogger, pShutdownNotifier, pCFA, lineOffsetsByFile),
        entryProvider);
  }

  /**
   * Returns a snapshot of the currently available invariant witnesses. The returned collection will
   * not update automatically and returns all the invariants that are known (not only new ones since
   * the last invocation of this method).
   *
   * <p>Note that calling this method might trigger the provider to check for and parse new
   * invariants, which is a potentially longer running operation.
   *
   * @return Current witnesses
   */
  public synchronized Collection<InvariantWitness> getCurrentWitnesses()
      throws InterruptedException, IOException {
    ImmutableSet.Builder<InvariantWitness> newWitnesses = ImmutableSet.builder();
    Optional<LoopInvariantEntry> newEntry = entryProvider.getNext();

    while (newEntry.isPresent()) {
      newWitnesses.addAll(parser.parseStoreEntry(newEntry.orElseThrow()));
      newEntry = entryProvider.getNext();
    }

    knownWitnesses.addAll(newWitnesses.build());

    return ImmutableSet.copyOf(knownWitnesses);
  }

  @Override
  public void close() throws IOException {
    entryProvider.close();
  }
}
