// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.cpa.usage.storage.AbstractUsagePointSet;
import org.sosy_lab.cpachecker.cpa.usage.storage.UnsafeDetector;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.usage")
public abstract class ErrorTracePrinter {

  @Option(name = "falseUnsafesOutput", description = "path to write results", secure = true)
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputFalseUnsafes = Path.of("FalseUnsafes");

  @Option(
      name = "filterMissedFiles",
      description = "if a file do not exist, do not include the corresponding edge",
      secure = true)
  private boolean filterMissedFiles = true;

  @Option(description = "print all unsafe cases in report", secure = true)
  private boolean printFalseUnsafes = false;

  // private final BAMTransferRelation transfer;
  protected final LockTransferRelation lockTransfer;

  private final StatTimer preparationTimer = new StatTimer("Time for preparation");
  private final StatTimer unsafeDetectionTimer = new StatTimer("Time for unsafe detection");
  private final StatTimer writingUnsafeTimer = new StatTimer("Time for dumping the unsafes");

  protected final Configuration config;
  protected UsageContainer container;
  protected final LogManager logger;
  protected UnsafeDetector detector;
  protected final CFA cfa;

  private BAMMultipleCEXSubgraphComputer subgraphComputer;

  protected ErrorTracePrinter(
      Configuration c,
      BAMMultipleCEXSubgraphComputer t,
      CFA pCfa,
      LogManager l,
      LockTransferRelation lT)
      throws InvalidConfigurationException {
    logger = l;
    config = c;
    lockTransfer = lT;
    config.inject(this, ErrorTracePrinter.class);
    subgraphComputer = t;
    cfa = pCfa;
  }

  protected boolean hasRelevantFileLocation(CFAEdge e) {
    if (e == null) {
      return false;
    }
    FileLocation loc = e.getFileLocation();
    if (loc == null || !loc.isRealLocation()) {
      return false;
    }
    if (filterMissedFiles && !Files.exists(loc.getFileName())) {
      return false;
    }
    return true;
  }

  private List<CFAEdge> createPath(UsageInfo usage) {
    assert usage.getKeyState() != null;

    ARGState target = (ARGState) usage.getKeyState();
    ARGPath path;
    if (subgraphComputer != null) {
      // BAM: we need to update target state considering BAM caches
      path = subgraphComputer.computePath(target);
    } else {
      path = ARGUtils.getOnePathTo(target);
    }
    if (path == null) {
      logger.log(Level.SEVERE, "Cannot compute path for: " + usage);
      return ImmutableList.of();
    }
    return path.getInnerEdges();
  }

  protected String createUniqueName(SingleIdentifier id) {
    return id.getType().toASTString("_" + id).replace(" ", "_");
  }

  public void printErrorTraces(UnmodifiableReachedSet reached) {
    preparationTimer.start();
    ReachedSet reachedSet;
    if (reached instanceof ForwardingReachedSet) {
      reachedSet = ((ForwardingReachedSet) reached).getDelegate();
    } else {
      reachedSet = (ReachedSet) reached;
    }
    UsageReachedSet uReached = (UsageReachedSet) reachedSet;
    container = uReached.getUsageContainer();
    detector = container.getUnsafeDetector();

    logger.log(Level.FINEST, "Processing unsafe identifiers");
    Iterator<SingleIdentifier> unsafeIterator = container.getUnsafeIterator();

    init();
    preparationTimer.stop();
    while (unsafeIterator.hasNext()) {
      SingleIdentifier id = unsafeIterator.next();
      final AbstractUsagePointSet uinfo = container.getUsages(id);

      if (uinfo == null || uinfo.size() == 0) {
        continue;
      }

      unsafeDetectionTimer.start();
      if (!detector.isUnsafe(uinfo)) {
        // In case of interruption during refinement,
        // We may get a situation, when a path is removed, but the verdict is not updated
        unsafeDetectionTimer.stop();
        continue;
      }
      Pair<UsageInfo, UsageInfo> tmpPair = detector.getUnsafePair(uinfo);
      unsafeDetectionTimer.stop();

      writingUnsafeTimer.start();
      printUnsafe(id, tmpPair);
      writingUnsafeTimer.stop();
    }
    if (printFalseUnsafes) {
      Set<SingleIdentifier> falseUnsafes = container.getFalseUnsafes();

      if (!falseUnsafes.isEmpty()) {
        try (Writer writer =
            Files.newBufferedWriter(outputFalseUnsafes, Charset.defaultCharset())) {
          logger.log(Level.FINE, "Print statistics about false unsafes");

          for (SingleIdentifier id : falseUnsafes) {
            writer.append(createUniqueName(id) + "\n");
          }
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write error trace");
        }
      }
    }
    finish();
  }

  public void printStatistics(StatisticsWriter out) {

    out.spacer().put(preparationTimer).put(unsafeDetectionTimer).put(writingUnsafeTimer);

    container.printUsagesStatistics(out);
  }

  protected String getNoteFor(CFAEdge pEdge) {
    return lockTransfer == null || pEdge == null ? "" : lockTransfer.doesChangeTheState(pEdge);
  }

  protected List<CFAEdge> getPath(UsageInfo usage) {
    List<CFAEdge> path = usage.getPath();

    if (usage.getPath() == null) {
      path = createPath(usage);
    }

    return path.isEmpty() ? null : path;
  }

  protected abstract void printUnsafe(SingleIdentifier id, Pair<UsageInfo, UsageInfo> pair);

  protected void init() {}

  protected void finish() {}
}
