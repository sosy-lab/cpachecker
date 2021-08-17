// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import java.util.Map.Entry;
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
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
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
      compatibleNodes = pTmpUsage.getUsagePoint().getCompatibleNodes();
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
      if (!(obj instanceof TraceCore)) {
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
  protected final StatCounter potentialAliases =
      new StatCounter("Number of unsafes with potential aliases");
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
    String concat;
    if (type instanceof CCompositeType) {
      // It includes declarations of all fields
      concat = ((CCompositeType) type).getQualifiedName() + " " + id.toString();
    } else if (type instanceof CElaboratedType) {
      String typeName = ((CElaboratedType) type).getQualifiedName();
      if (typeName.contains("anonstruct") || typeName.contains("anonunion")) {
        // The number of fields in anonstruct are unique build to build, and Klever can not match the warnings,
        // if it have different declaration, thus, remove the number
        typeName = typeName.replaceAll("_(\\d)*$", "");
      }
      concat = typeName + " " + id.toString();
    } else {
      concat = id.getType().toASTString(id.toString());
    }
    if (concat.contains("cif_")) {
      // CIF also adds numbers of instrumented functions randomly, and Klever cannot match them
      concat = concat.replaceAll("\\d", "");
    }
    return concat;
  }

  public void printErrorTraces(UsageReachedSet uReached) {
    preparationTimer.start();

    logger.log(Level.FINEST, "Processing unsafe identifiers");
    Map<SingleIdentifier, Pair<UsageInfo, UsageInfo>> unsafes = uReached.getUnsafes();

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
    for (Entry<SingleIdentifier, Pair<UsageInfo, UsageInfo>> unsafe : unsafes.entrySet()) {
      UsageInfo uinfo1 = unsafe.getValue().getFirst();
      UsageInfo uinfo2 = unsafe.getValue().getSecond();
      SingleIdentifier id = unsafe.getKey();
      if (id instanceof StructureIdentifier) {
        id = ((StructureIdentifier) id).toStructureFieldIdentifier();
      }

      boolean refined = uinfo1.getPath() != null && uinfo2.getPath() != null;

      if (printOnlyTrueUnsafes && !refined) {
        continue;
      }

      if (uinfo1.getLockNode().getSize() == 0 && uinfo2.getLockNode().getSize() == 0) {
        if (printEmptyLockStates) {
          emptyLockSetUnsafes.inc();
        } else {
          continue;
        }
      }

      if (filterSimilarUnsafes && shouldBeSkipped(unsafe.getValue(), id)) {
        continue;
      }

      writingUnsafeTimer.start();
      printUnsafe(id, unsafe.getValue(), refined);
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
        .put(emptyLockSetUnsafes)
        .put(potentialAliases);

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
        aPath = subgraphComputer.computePath(target, usage.getExpandedStack());
      } else {
        aPath = ARGUtils.getOnePathTo(target);
      }
      if (aPath == null) {
        logger.log(Level.SEVERE, "Cannot compute path for: " + usage);
        return ImmutableList.of();
      }
      // Full path for effects
      return aPath.getFullPath();
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
