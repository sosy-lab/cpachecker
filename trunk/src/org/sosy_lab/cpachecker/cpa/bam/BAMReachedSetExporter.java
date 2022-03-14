// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.base.Predicates;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;

@Options(prefix = "cpa.bam")
class BAMReachedSetExporter implements Statistics {

  @Option(secure = true, description = "export blocked ARG as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path argFile = Path.of("BlockedARG.dot");

  @Option(
      secure = true,
      description = "export single blocked ARG as .dot files, should contain '%d'")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate indexedArgFile = PathTemplate.ofFormatString("ARGs/ARG_%d.dot");

  @Option(secure = true, description = "export used parts of blocked ARG as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path simplifiedArgFile = Path.of("BlockedARGSimplified.dot");

  private final LogManager logger;
  private final AbstractBAMCPA bamcpa;

  BAMReachedSetExporter(Configuration pConfig, LogManager pLogger, AbstractBAMCPA pCpa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    bamcpa = pCpa;
  }

  private static boolean highlightSummaryEdge(ARGState firstState, ARGState secondState) {
    return firstState.getEdgeToChild(secondState) instanceof FunctionSummaryEdge;
  }

  @Override
  public @Nullable String getName() {
    return null; // not needed
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    exportAllReachedSets(argFile, indexedArgFile, pReached);
    exportUsedReachedSets(simplifiedArgFile, pReached);
  }

  private void exportAllReachedSets(
      final Path superArgFile,
      final PathTemplate indexedFile,
      final UnmodifiableReachedSet mainReachedSet) {

    if (superArgFile != null) {

      final Set<UnmodifiableReachedSet> allReachedSets =
          new LinkedHashSet<>(bamcpa.getData().getCache().getAllCachedReachedStates());
      allReachedSets.add(mainReachedSet);

      final Set<ARGState> rootStates = new LinkedHashSet<>();
      final Multimap<ARGState, ARGState> connections = LinkedHashMultimap.create();

      for (final UnmodifiableReachedSet reachedSet : allReachedSets) {
        ARGState rootState = (ARGState) reachedSet.getFirstState();
        rootStates.add(rootState);
        Multimap<ARGState, ARGState> localConnections = LinkedHashMultimap.create();
        getConnections(rootState, localConnections);
        connections.putAll(localConnections);

        // dump small graph
        writeArg(
            indexedFile.getPath(((ARGState) reachedSet.getFirstState()).getStateId()),
            localConnections,
            Collections.singleton((ARGState) reachedSet.getFirstState()));
      }

      // dump super-graph
      writeArg(superArgFile, connections, rootStates);
    }
  }

  /** dump only those ReachedSets, that are reachable from mainReachedSet. */
  private void exportUsedReachedSets(
      final Path superArgFile, final UnmodifiableReachedSet mainReachedSet) {

    if (superArgFile != null) {

      final Multimap<ARGState, ARGState> connections = LinkedHashMultimap.create();
      final Set<ARGState> rootStates = getUsedRootStates(mainReachedSet, connections);
      writeArg(superArgFile, connections, rootStates);
    }
  }

  private void writeArg(
      final Path file,
      final Multimap<ARGState, ARGState> connections,
      final Set<ARGState> rootStates) {
    try (Writer w = IO.openOutputFile(file, Charset.defaultCharset())) {
      ARGToDotWriter.write(
          w,
          rootStates,
          connections,
          ARGState::getChildren,
          Predicates.alwaysTrue(),
          BAMReachedSetExporter::highlightSummaryEdge);
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, String.format("Could not write ARG to file: %s", file));
    }
  }

  private Set<ARGState> getUsedRootStates(
      final UnmodifiableReachedSet mainReachedSet, final Multimap<ARGState, ARGState> connections) {
    final Set<UnmodifiableReachedSet> finished = new LinkedHashSet<>();
    final Deque<UnmodifiableReachedSet> waitlist = new ArrayDeque<>();
    waitlist.add(mainReachedSet);
    while (!waitlist.isEmpty()) {
      final UnmodifiableReachedSet reachedSet = waitlist.pop();
      if (!finished.add(reachedSet)) {
        continue;
      }
      final ARGState rootState = (ARGState) reachedSet.getFirstState();
      final Set<ReachedSet> referencedReachedSets = getConnections(rootState, connections);
      waitlist.addAll(referencedReachedSets);
    }

    final Set<ARGState> rootStates = new LinkedHashSet<>();
    for (UnmodifiableReachedSet reachedSet : finished) {
      rootStates.add((ARGState) reachedSet.getFirstState());
    }
    return rootStates;
  }

  /**
   * This method iterates over all reachable states from rootState and searches for connections to
   * other reachedSets (a set of all those other reachedSets is returned). As side-effect we collect
   * a Multimap of all connections: - from a state (in current reachedSet) to its reduced state (in
   * other rechedSet) and - from a foreign state (in other reachedSet) to its expanded state(s) (in
   * current reachedSet).
   */
  private Set<ReachedSet> getConnections(
      final ARGState rootState, final Multimap<ARGState, ARGState> connections) {
    final BAMDataManager data = bamcpa.getData();
    final Set<ReachedSet> referencedReachedSets = new LinkedHashSet<>();
    final Set<ARGState> finished = new HashSet<>();
    final Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.add(rootState);
    while (!waitlist.isEmpty()) {
      ARGState state = waitlist.pop();
      if (!finished.add(state)) {
        continue;
      }
      if (data.hasInitialState(state)) {
        for (ARGState child : state.getChildren()) {
          assert data.hasExpandedState(child);
          ARGState reducedExitState = (ARGState) data.getReducedStateForExpandedState(child);
          if (reducedExitState.isDestroyed()) {
            continue; // skip deleted reached-set, TODO why is reached-set deleted?
          }
          ReachedSet target = data.getReachedSetForInitialState(state, reducedExitState);

          referencedReachedSets.add(target);
          ARGState targetState = (ARGState) target.getFirstState();
          connections.put(state, targetState);
        }
      }
      if (data.hasExpandedState(state)) {
        AbstractState sourceState = data.getReducedStateForExpandedState(state);
        connections.put((ARGState) sourceState, state);
      }
      waitlist.addAll(state.getChildren());
    }
    return referencedReachedSets;
  }
}
