/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.cpa.usage.storage.AbstractUsagePointSet;
import org.sosy_lab.cpachecker.cpa.usage.storage.RefinedUsagePointSet;
import org.sosy_lab.cpachecker.cpa.usage.storage.UnsafeDetector;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.usage")
public abstract class ErrorTracePrinter {

  @Option(name = "falseUnsafesOutput", description = "path to write results", secure = true)
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputFalseUnsafes = Paths.get("FalseUnsafes");

  @Option(
    name = "filterMissedFiles",
    description = "if a file do not exist, do not include the corresponding edge",
    secure = true
  )
  private boolean filterMissedFiles = false;

  @Option(description = "print all unsafe cases in report", secure = true)
  private boolean printFalseUnsafes = false;

  @Option(description = "output only true unsafes", secure = true)
  private boolean printOnlyTrueUnsafes = false;

  // private final BAMTransferRelation transfer;
  protected final LockTransferRelation lockTransfer;

  private final StatTimer preparationTimer = new StatTimer("Time for preparation");
  private final StatTimer unsafeDetectionTimer = new StatTimer("Time for unsafe detection");
  private final StatTimer writingUnsafeTimer = new StatTimer("Time for dumping the unsafes");
  private final StatCounter emptyLockSetUnsafes =
      new StatCounter("Number of unsafes with empty lock sets");
  protected final StatCounter printedUnsafes =
      new StatCounter("Number of successfully printed unsafes");

  protected final Configuration config;
  protected final LogManager logger;
  protected final CFA cfa;
  protected UsageContainer container;

  protected Predicate<CFAEdge> FILTER_EMPTY_FILE_LOCATIONS;
  private final BAMMultipleCEXSubgraphComputer subgraphComputer;

  public ErrorTracePrinter(
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
    FILTER_EMPTY_FILE_LOCATIONS =
        Predicates.and(
            e -> e != null,
            e ->
                (e.getFileLocation() != null
                    && !e.getFileLocation().getFileName().equals("<none>")));

    if (filterMissedFiles) {
      FILTER_EMPTY_FILE_LOCATIONS =
          Predicates.and(
              FILTER_EMPTY_FILE_LOCATIONS,
              e -> (Files.exists(Paths.get(e.getFileLocation().getFileName()))));
    }
    subgraphComputer = t;
    cfa = pCfa;
  }

  protected String createUniqueName(SingleIdentifier id) {
    return id.getType().toASTString("_" + id.toString()).replace(" ", "_");
  }

  public void printErrorTraces(UsageReachedSet uReached) {
    preparationTimer.start();
    container = uReached.getUsageContainer();
    UnsafeDetector detector = container.getUnsafeDetector();

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

      boolean refined = uinfo instanceof RefinedUsagePointSet;

      if (printOnlyTrueUnsafes && !refined) {
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
      if (tmpPair.getFirst().getLockState().getSize() == 0
          && tmpPair.getSecond().getLockState().getSize() == 0) {
        emptyLockSetUnsafes.inc();
      }

      unsafeDetectionTimer.stop();

      writingUnsafeTimer.start();
      printUnsafe(id, tmpPair, refined);
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
          logger.log(Level.SEVERE, e.getMessage());
        }
      }
    }
    finish();
  }

  public void printStatistics(StatisticsWriter out) {

    out.spacer()
        .put(preparationTimer)
        .put(unsafeDetectionTimer)
        .put(writingUnsafeTimer)
        .put(printedUnsafes)
        .put(emptyLockSetUnsafes);

    container.printUsagesStatistics(out);
  }

  protected String getNoteFor(CFAEdge pEdge) {
    return lockTransfer == null || pEdge == null ? "" : lockTransfer.doesChangeTheState(pEdge);
  }

  protected List<CFAEdge> getPath(UsageInfo usage) {
    List<CFAEdge> path = usage.getPath();

    if (path != null) {
      return path;
    } else {
      assert usage.getKeyState() != null;

      ARGState target = (ARGState) usage.getKeyState();
      ARGPath aPath;
      if (subgraphComputer != null) {
        // BAM: we need to update target state considering BAM caches
        aPath = subgraphComputer.computePath(target);
      } else {
        aPath = ARGUtils.getOnePathTo(target);
      }
      if (aPath == null) {
        logger.log(Level.SEVERE, "Cannot compute path for: " + usage);
        return Collections.emptyList();
      }
      return aPath.getInnerEdges();
    }
  }

  protected Iterator<CFAEdge> getIterator(List<CFAEdge> path) {
    return from(path).filter(FILTER_EMPTY_FILE_LOCATIONS).iterator();
  }

  protected Iterator<CFAEdge> getPathIterator(UsageInfo usage) {
    return getIterator(getPath(usage));
  }

  protected abstract void printUnsafe(
      SingleIdentifier id, Pair<UsageInfo, UsageInfo> pair, boolean refined);

  protected void init() {}

  protected void finish() {}
}
