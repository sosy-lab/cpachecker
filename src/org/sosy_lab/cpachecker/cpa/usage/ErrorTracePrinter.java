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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.StructureIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.usage")
public abstract class ErrorTracePrinter {

  private static class TraceCore {
    private final String function;
    private final List<CompatibleNode> compatibleNodes;

    private TraceCore(UsageInfo pTmpUsage) {
      compatibleNodes = pTmpUsage.getCompatibleNodes();
      function = pTmpUsage.getCFANode().getFunctionName();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Objects.hashCode(compatibleNodes);
      result = prime * result + Objects.hashCode(function);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      TraceCore other = (TraceCore) obj;
      return Objects.equals(compatibleNodes, other.compatibleNodes)
          && Objects.equals(function, other.function);
    }

  }

  @Option(name = "falseUnsafesOutput", description = "path to write results", secure = true)
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputFalseUnsafes = Paths.get("FalseUnsafes");

  @Option(
    name = "filterMissedFiles",
    description = "if a file do not exist, do not include the corresponding edge",
    secure = true
  )
  private boolean filterMissedFiles = false;

  @Option(description = "filter unsafes, which are too similar", secure = true)
  private boolean filterSimilarUnsafes = false;

  @Option(description = "print all unsafe cases in report", secure = true)
  private boolean printFalseUnsafes = false;

  @Option(description = "output only true unsafes", secure = true)
  private boolean printOnlyTrueUnsafes = false;

  @Option(description = "print unsafes with empty lock states", secure = true)
  private boolean printEmptyLockStates = true;

  @Option(description = "a list of functions, which do not produce notes", secure = true)
  private Set<String> disableNotesFor = ImmutableSet.of();

  // private final BAMTransferRelation transfer;
  protected final LockTransferRelation lockTransfer;

  private final StatTimer preparationTimer = new StatTimer("Time for preparation");
  private final StatTimer writingUnsafeTimer = new StatTimer("Time for dumping the unsafes");
  private final StatTimer filteringUnsafeTimer = new StatTimer("Time for filtering unsafes");
  private final StatCounter emptyLockSetUnsafes =
      new StatCounter("Number of unsafes with empty lock sets");
  protected final StatCounter printedUnsafes =
      new StatCounter("Number of successfully printed unsafes");
  protected final StatCounter skippedUnsafes = new StatCounter("Number of filtered out unsafes");

  protected final Configuration config;
  protected final LogManager logger;
  protected final CFA cfa;
  protected UsageContainer container;

  protected Predicate<CFAEdge> FILTER_EMPTY_FILE_LOCATIONS;
  private final BAMMultipleCEXSubgraphComputer subgraphComputer;

  private final Map<Set<TraceCore>, AbstractIdentifier> printedTraces;

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

    final FunctionEntryNode main = pCfa.getMainFunction();

    FILTER_EMPTY_FILE_LOCATIONS =
        Predicates.and(
            e -> e != null,
            e -> !AutomatonGraphmlCommon.getFileLocationsFromCfaEdge0(e, main).isEmpty());

    if (filterMissedFiles) {
      FILTER_EMPTY_FILE_LOCATIONS =
          Predicates.and(
              FILTER_EMPTY_FILE_LOCATIONS,
              e -> Files.exists(Paths.get(e.getFileLocation().getFileName())));
    }

    subgraphComputer = t;
    cfa = pCfa;
    printedTraces = new HashMap<>();
  }

  protected String createUniqueName(SingleIdentifier id) {
    CType type = id.getType();
    if (type instanceof CCompositeType) {
      // It includes declarations of all fields
      return (((CCompositeType) type).getQualifiedName() + "_" + id.toString()).replace(" ", "_");
    } else {
      return id.getType().toASTString("_" + id.toString()).replace(" ", "_");
    }
  }

  public void printErrorTraces(UsageReachedSet uReached) {
    preparationTimer.start();

    logger.log(Level.FINEST, "Processing unsafe identifiers");
    List<Pair<UsageInfo, UsageInfo>> unsafes = uReached.getUnsafes();

    if (unsafes == null) {
      // Means that there are no stable unsafes, nothing to print, but warn
      logger.log(
          Level.WARNING,
          "Can not find information about unsafes, it does not mean that the program is safe");
      return;
    }

    container = uReached.getUsageContainer();

    init();
    preparationTimer.stop();
    for (Pair<UsageInfo, UsageInfo> unsafe : unsafes) {
      UsageInfo uinfo1 = unsafe.getFirst();
      UsageInfo uinfo2 = unsafe.getSecond();
      SingleIdentifier id = uinfo1.getId();
      if (id instanceof StructureIdentifier) {
        id = ((StructureIdentifier) id).toStructureFieldIdentifier();
      }

      boolean refined = uinfo1.getPath() != null && uinfo2.getPath() != null;

      if (printOnlyTrueUnsafes && !refined) {
        continue;
      }

      if (uinfo1.getLockState().getSize() == 0 && uinfo2.getLockState().getSize() == 0) {
        if (printEmptyLockStates) {
          emptyLockSetUnsafes.inc();
        } else {
          continue;
        }
      }

      if (filterSimilarUnsafes && shouldBeSkipped(unsafe, id)) {
        continue;
      }

      printedUnsafes.inc();
      writingUnsafeTimer.start();
      printUnsafe(id, unsafe, refined);
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

  private boolean shouldBeSkipped(Pair<UsageInfo, UsageInfo> pTmpPair, AbstractIdentifier id) {
    filteringUnsafeTimer.start();
    TraceCore first = new TraceCore(pTmpPair.getFirst());
    TraceCore second = new TraceCore(pTmpPair.getSecond());

    Set<TraceCore> trace = Sets.newHashSet(first, second);
    boolean result = printedTraces.containsKey(trace);
    if (!result) {
      printedTraces.put(trace, id);
    } else {
      logger.log(Level.INFO, "Filter out " + id + " as it similar to " + printedTraces.get(trace));
      skippedUnsafes.inc();
    }

    filteringUnsafeTimer.stop();
    return result;
  }

  public void printStatistics(StatisticsWriter out) {

    out.spacer()
        .put(preparationTimer)
        .put(writingUnsafeTimer)
        .put(filteringUnsafeTimer)
        .put(printedUnsafes)
        .put(skippedUnsafes)
        .put(emptyLockSetUnsafes);

    if (container != null) {
      // Timeout
      container.printUsagesStatistics(out);
    }
  }

  protected String getNoteFor(CFAEdge pEdge) {
    if (pEdge == null || lockTransfer == null) {
      return "";
    }
    String function = pEdge.getPredecessor().getFunctionName();
    if (disableNotesFor.contains(function)) {
      return "";
    }
    return lockTransfer.doesChangeTheState(pEdge);
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
        return ImmutableList.of();
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
