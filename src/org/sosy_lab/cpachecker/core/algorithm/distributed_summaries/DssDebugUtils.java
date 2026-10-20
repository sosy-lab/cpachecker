// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssExceptionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssHeaderPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageWithStates;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssNoPrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssResultMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStateAndPrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatusPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssWitnessMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath.PathCase;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DeserializeBlockStateOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DssBlockStatePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Human-readable renderings of the data structures of the distributed-summary-synthesis (DSS)
 * algorithm.
 *
 * <p>Everything in here exists purely for debugging. The methods are static, side-effect free
 * (except for the explicit {@code dump*} methods), and deliberately defensive: a renderer never
 * throws because a state is unexpectedly shaped, it renders what it can and marks the rest. That
 * makes them safe to call from a temporary {@code logger.log(...)} line, from a conditional
 * breakpoint, or from a debugger's expression evaluator.
 *
 * <p>The entry points are grouped as follows.
 *
 * <ul>
 *   <li>Abstract states: {@link #oneLine(AbstractState)}, {@link #describe(AbstractState)}, {@link
 *       #describeStates(Iterable)}.
 *   <li>Block graph: {@link #describe(BlockNode)}, {@link #describe(BlockGraph)}, {@link
 *       #blockGraphToDot(BlockGraph)}.
 *   <li>Paths through the block graph: {@link #render(BlockGraphPath)}, {@link
 *       #explainPathCase(BlockGraphPath, BlockGraphPath)}, {@link #pathCaseMatrix(Iterable,
 *       Iterable)}.
 *   <li>Messages: {@link #summarize(DssMessage)}, {@link #describe(DssMessage)}, {@link
 *       #messageTable(Iterable)}, {@link #diff(DssMessage, DssMessage)}.
 *   <li>Reached sets and ARGs: {@link #describeReachedSet(UnmodifiableReachedSet)}, {@link
 *       #describe(ARGPath)}, {@link #argToDot(UnmodifiableReachedSet)}.
 *   <li>Pre-/violation conditions of a block: {@link #prettyPrintBlock(String, Multimap,
 *       Multimap)}.
 *   <li>Writing any of the above to disk: {@link #dump(String, String)}, {@link #dumpArg(String,
 *       UnmodifiableReachedSet)}.
 * </ul>
 */
public final class DssDebugUtils {

  private DssDebugUtils() {}

  /** Values longer than this are abbreviated by the renderers that take no explicit limit. */
  public static final int DEFAULT_MAX_VALUE_LENGTH = 240;

  /**
   * Directory that {@link #dump(String, String)} writes to. Override with the system property
   * {@code dss.debug.dir}.
   */
  public static final Path DEBUG_DIR =
      Path.of(System.getProperty("dss.debug.dir", "output/dss-debug"));

  private static final Splitter LINE_SPLITTER = Splitter.on('\n');
  private static final Joiner LINE_JOINER = Joiner.on('\n');
  private static final Joiner SPACE_JOINER = Joiner.on(' ');

  private static final AtomicInteger DUMP_COUNTER = new AtomicInteger();

  /** Content fields whose values are machine-only and never worth printing. */
  private static final ImmutableSet<String> OPAQUE_FIELDS = ImmutableSet.of("pts");

  // ===================================================================================
  // Formatting primitives
  // ===================================================================================

  /** Upper bound for the rules drawn by {@link #box(String, String)}. */
  private static final int MAX_RULE_WIDTH = 120;

  /**
   * Puts {@code pBody} between two rules, the upper one carrying {@code pTitle}.
   *
   * <pre>{@code
   * === title ==========
   * first body line
   * ====================
   * }</pre>
   *
   * <p>The body is never padded or wrapped, so that wide tables and long formulas stay copyable and
   * greppable.
   */
  public static String box(String pTitle, String pBody) {
    int longestLine =
        ImmutableList.copyOf(LINE_SPLITTER.split(pBody)).stream()
            .mapToInt(String::length)
            .max()
            .orElse(0);
    int width = Math.max(pTitle.length() + 8, Math.min(longestLine, MAX_RULE_WIDTH));
    String titlePrefix = "=== " + pTitle + " ";
    return titlePrefix
        + "=".repeat(width - titlePrefix.length())
        + "\n"
        + pBody
        + "\n"
        + "=".repeat(width);
  }

  /** Renders a column-aligned table. Missing cells are rendered as the empty string. */
  public static String table(List<String> pHeader, List<? extends List<String>> pRows) {
    int columns = pHeader.size();
    for (List<String> row : pRows) {
      columns = Math.max(columns, row.size());
    }
    int[] widths = new int[columns];
    for (int i = 0; i < pHeader.size(); i++) {
      widths[i] = pHeader.get(i).length();
    }
    for (List<String> row : pRows) {
      for (int i = 0; i < row.size(); i++) {
        widths[i] = Math.max(widths[i], cell(row, i).length());
      }
    }
    StringBuilder result = new StringBuilder();
    appendRow(result, pHeader, widths, columns);
    for (int i = 0; i < columns; i++) {
      boolean isLast = i + 1 == columns;
      // The last column is never padded, so its rule may be capped: a 300-char rule helps nobody.
      result.append("-".repeat(isLast ? Math.min(widths[i], 60) : widths[i]));
      if (!isLast) {
        result.append("  ");
      }
    }
    result.append('\n');
    for (List<String> row : pRows) {
      appendRow(result, row, widths, columns);
    }
    return result.toString().stripTrailing();
  }

  private static void appendRow(
      StringBuilder pTarget, List<String> pRow, int[] pWidths, int pColumns) {
    for (int i = 0; i < pColumns; i++) {
      String value = cell(pRow, i);
      pTarget.append(value);
      if (i + 1 < pColumns) {
        pTarget.append(" ".repeat(pWidths[i] - value.length())).append("  ");
      }
    }
    pTarget.append('\n');
  }

  private static String cell(List<String> pRow, int pIndex) {
    String value = pIndex < pRow.size() ? Objects.requireNonNullElse(pRow.get(pIndex), "") : "";
    // A line break inside a cell would destroy the column alignment of the whole table.
    return value.indexOf('\n') < 0 ? value : singleLine(value);
  }

  /** Prefixes every line of {@code pText} with {@code pIndent}. */
  public static String indent(String pIndent, String pText) {
    return LINE_JOINER.join(
        FluentIterable.from(LINE_SPLITTER.split(pText)).transform(l -> pIndent + l));
  }

  /** Collapses a multi-line value into one line so that it fits into a table cell. */
  public static String singleLine(String pValue) {
    return SPACE_JOINER
        .join(
            FluentIterable.from(LINE_SPLITTER.split(pValue))
                .transform(String::strip)
                .filter(line -> !line.isEmpty()))
        .strip();
  }

  /** Shortens {@code pValue} to {@code pMaxLength} characters, noting the original length. */
  public static String abbreviate(String pValue, int pMaxLength) {
    if (pValue.length() <= pMaxLength) {
      return pValue;
    }
    return pValue.substring(0, pMaxLength) + "... <" + pValue.length() + " chars total>";
  }

  private static String shortValue(String pValue) {
    return abbreviate(singleLine(pValue), DEFAULT_MAX_VALUE_LENGTH);
  }

  // ===================================================================================
  // Abstract states
  // ===================================================================================

  /**
   * Renders the DSS-relevant facts of an abstract state on a single line: ARG id, program location,
   * block, block-graph history, callstack, and predicate formula.
   *
   * <p>This is the workhorse for log lines and for the state renderer used by {@link
   * #prettyPrintBlock(String, Multimap, Multimap)}.
   */
  public static String oneLine(@Nullable AbstractState pState) {
    if (pState == null) {
      return "null";
    }
    List<String> parts = new ArrayList<>();

    ARGState argState = AbstractStates.extractStateByType(pState, ARGState.class);
    if (argState != null) {
      StringBuilder arg = new StringBuilder("#").append(argState.getStateId());
      if (argState.isTarget()) {
        arg.append("!target");
      }
      if (argState.isCovered()) {
        arg.append("!coveredBy#").append(argState.getCoveringState().getStateId());
      }
      parts.add(arg.toString());
    }

    CFANode location = AbstractStates.extractLocation(pState);
    if (location != null) {
      parts.add("@" + location + "(" + location.getFunctionName() + ")");
    }

    BlockState blockState = AbstractStates.extractStateByType(pState, BlockState.class);
    if (blockState != null) {
      parts.add(blockState.getBlockNode().getId() + ":" + blockState.getType());
      if (!blockState.getHistory().path().isEmpty()) {
        parts.add("hist=" + render(blockState.getHistory()));
      }
      if (blockState.getWitness().size() > 0) {
        parts.add("wit=" + blockState.getWitness().size());
      }
      if (!blockState.getViolationConditions().isEmpty()) {
        parts.add("vcs=" + blockState.getViolationConditions().size());
      }
    }

    CallstackState callstack = AbstractStates.extractStateByType(pState, CallstackState.class);
    if (callstack != null) {
      parts.add("cs=" + renderCallstack(callstack));
    }

    PredicateAbstractState predicate =
        AbstractStates.extractStateByType(pState, PredicateAbstractState.class);
    if (predicate != null) {
      parts.add(
          (predicate.isAbstractionState() ? "abs=" : "pf=")
              + abbreviate(singleLine(formulaOf(predicate)), 120));
    }

    if (parts.isEmpty()) {
      return shortValue(pState.toString());
    }
    return SPACE_JOINER.join(parts);
  }

  /**
   * Renders one line per component CPA of {@code pState}, with a specialized rendering for the
   * components that matter for DSS and {@code toString()} for the rest.
   */
  public static String describe(@Nullable AbstractState pState) {
    if (pState == null) {
      return "null";
    }
    List<List<String>> rows = new ArrayList<>();
    for (AbstractState component : AbstractStates.asIterable(pState)) {
      rows.add(
          ImmutableList.of(component.getClass().getSimpleName(), describeComponent(component)));
    }
    return box(oneLine(pState), table(ImmutableList.of("component", "value"), rows));
  }

  private static String describeComponent(AbstractState pComponent) {
    if (pComponent instanceof ARGState argState) {
      return "id="
          + argState.getStateId()
          + " parents="
          + argIds(argState.getParents())
          + " children="
          + argIds(argState.getChildren())
          + (argState.isCovered() ? " coveredBy=#" + argState.getCoveringState().getStateId() : "")
          + (argState.isTarget() ? " TARGET" : "");
    }
    if (pComponent instanceof BlockState blockState) {
      return "block="
          + blockState.getBlockNode().getId()
          + " type="
          + blockState.getType()
          + " node="
          + blockState.getLocationNode()
          + " history="
          + render(blockState.getHistory())
          + " witness="
          + shortValue(blockState.getWitness().serialize())
          + " violationConditions="
          + blockState.getViolationConditions().size()
          + (blockState.isTarget() ? " TARGET" : "");
    }
    if (pComponent instanceof CallstackState callstackState) {
      return renderCallstack(callstackState) + " (depth " + callstackState.getDepth() + ")";
    }
    if (pComponent instanceof PredicateAbstractState predicateState) {
      return (predicateState.isAbstractionState() ? "abstraction=" : "pathFormula=")
          + shortValue(formulaOf(predicateState))
          + " ssa="
          + shortValue(String.valueOf(predicateState.getPathFormula().getSsa()));
    }
    return shortValue(String.valueOf(pComponent));
  }

  /** Renders a numbered list of {@link #oneLine(AbstractState) one-line} state summaries. */
  public static String describeStates(Iterable<? extends AbstractState> pStates) {
    List<List<String>> rows = new ArrayList<>();
    int index = 0;
    for (AbstractState state : pStates) {
      rows.add(ImmutableList.of(Integer.toString(index++), oneLine(state)));
    }
    if (rows.isEmpty()) {
      return "<no states>";
    }
    return table(ImmutableList.of("#", "state"), rows);
  }

  /** Renders a {@link StateAndPrecision} pair, abbreviating the precision. */
  public static String describe(@Nullable StateAndPrecision pStateAndPrecision) {
    if (pStateAndPrecision == null) {
      return "null";
    }
    return oneLine(pStateAndPrecision.state())
        + "  |  precision="
        + shortValue(String.valueOf(pStateAndPrecision.precision()));
  }

  private static String argIds(Collection<ARGState> pStates) {
    return FluentIterable.from(pStates).transform(s -> "#" + s.getStateId()).join(Joiner.on(","));
  }

  private static String renderCallstack(CallstackState pState) {
    if (pState instanceof DssCallstackState dssState && dssState.canBeTopState()) {
      return "__ignore";
    }
    Deque<String> frames = new ArrayDeque<>();
    for (CallstackState current = pState; current != null; current = current.getPreviousState()) {
      frames.addFirst(current.getCallNode().getNodeNumber() + "." + current.getCurrentFunction());
    }
    return Joiner.on("->").join(frames);
  }

  private static String formulaOf(PredicateAbstractState pState) {
    return pState.isAbstractionState()
        ? String.valueOf(pState.getAbstractionFormula().asFormula())
        : String.valueOf(pState.getPathFormula().getFormula());
  }

  /**
   * Shows the program-point hash a distributed CPA computes for {@code pState} together with the
   * location-carrying components it is derived from.
   *
   * <p>Two states that should be merged but are not (or vice versa) are almost always a
   * program-point-hash problem; this makes the inputs of that hash visible.
   */
  public static String programPointHash(
      DistributedConfigurableProgramAnalysis pDcpa, AbstractState pState) {
    List<List<String>> rows = new ArrayList<>();
    for (AbstractState component : AbstractStates.asIterable(pState)) {
      if (component instanceof BlockState
          || component instanceof CallstackState
          || component instanceof LocationState
          || component.getClass().getSimpleName().contains("FunctionPointer")) {
        rows.add(
            ImmutableList.of(
                component.getClass().getSimpleName(), shortValue(describeComponent(component))));
      }
    }
    return box(
        "program point = " + pDcpa.computeProgramPointId(pState),
        table(ImmutableList.of("component", "value"), rows));
  }

  // ===================================================================================
  // Block graph
  // ===================================================================================

  /** Renders a block node with its interface to the rest of the block graph and its code. */
  public static String describe(BlockNode pBlock) {
    String body =
        table(
            ImmutableList.of("property", "value"),
            ImmutableList.of(
                ImmutableList.of("predecessors", String.valueOf(pBlock.getPredecessorIds())),
                ImmutableList.of("successors", String.valueOf(pBlock.getSuccessorIds())),
                ImmutableList.of("entry", String.valueOf(pBlock.getInitialLocation())),
                ImmutableList.of("exit", String.valueOf(pBlock.getFinalLocation())),
                ImmutableList.of(
                    "violationConditionLocation",
                    pBlock.getViolationConditionLocation()
                        + (pBlock.isAbstractionPossible() ? " (abstraction possible)" : "")),
                ImmutableList.of("nodes", Integer.toString(pBlock.getNodes().size())),
                ImmutableList.of("edges", Integer.toString(pBlock.getEdges().size()))));
    return box(
        "Block " + pBlock.getId() + (pBlock.isRoot() ? " (root)" : ""),
        body + "\n\n" + pBlock.getCode().strip());
  }

  /** Renders the whole block graph: an overview table followed by every block's code. */
  public static String describe(BlockGraph pGraph) {
    List<List<String>> rows = new ArrayList<>();
    for (BlockNode block : sortedBlocks(pGraph)) {
      rows.add(
          ImmutableList.of(
              block.getId() + (block.equals(pGraph.getRoot()) ? " (root)" : ""),
              String.valueOf(block.getPredecessorIds()),
              String.valueOf(block.getSuccessorIds()),
              block.getInitialLocation() + " -> " + block.getFinalLocation(),
              String.valueOf(block.getViolationConditionLocation()),
              abbreviate(singleLine(block.getCode()), 60)));
    }
    StringBuilder result =
        new StringBuilder(
            box(
                "BlockGraph (" + pGraph.getNodes().size() + " blocks)",
                table(
                    ImmutableList.of("block", "pred", "succ", "entry->exit", "vcLoc", "code"),
                    rows)));
    for (BlockNode block : sortedBlocks(pGraph)) {
      result.append('\n').append(describe(block));
    }
    return result.toString();
  }

  /**
   * Renders the block graph in DOT format. Write the result next to {@code output/cfa.dot} and
   * compare the two when a decomposition looks wrong.
   */
  public static String blockGraphToDot(BlockGraph pGraph) {
    StringBuilder result = new StringBuilder("digraph BlockGraph {\nrankdir=TB;\n");
    for (BlockNode block : sortedBlocks(pGraph)) {
      String label =
          block.getId()
              + " ["
              + block.getInitialLocation()
              + " -> "
              + block.getFinalLocation()
              + "]\\n"
              + escapeDot(abbreviate(block.getCode().strip(), 400));
      result
          .append('"')
          .append(block.getId())
          .append("\" [shape=box, label=\"")
          .append(label)
          .append('"')
          .append(block.equals(pGraph.getRoot()) ? ", style=bold" : "")
          .append("];\n");
    }
    for (BlockNode block : sortedBlocks(pGraph)) {
      for (String successor : block.getSuccessorIds()) {
        result
            .append('"')
            .append(block.getId())
            .append("\" -> \"")
            .append(successor)
            .append("\";\n");
      }
    }
    return result.append("}\n").toString();
  }

  private static ImmutableList<BlockNode> sortedBlocks(BlockGraph pGraph) {
    return ImmutableList.sortedCopyOf(Comparator.comparing(BlockNode::getId), pGraph.getNodes());
  }

  private static String escapeDot(String pText) {
    return pText.replace("\\", "\\\\").replace("\"", "'").replace("\n", "\\n");
  }

  // ===================================================================================
  // Paths through the block graph
  // ===================================================================================

  /** Renders a block-graph path as {@code [B0 -> B1 -> B2]}. */
  public static String render(@Nullable BlockGraphPath pPath) {
    if (pPath == null) {
      return "null";
    }
    if (pPath.path().isEmpty()) {
      return "[]";
    }
    return "[" + Joiner.on(" -> ").join(pPath.path()) + "]";
  }

  /**
   * Explains why {@link BlockGraphPath#getFirstMatchingCase(BlockGraphPath)} picks the case it
   * picks, by showing the outcome of every individual predicate rather than only the winner.
   *
   * <p>This is the decision that {@code PathBasedPreconditionHandler} bases its
   * replace-or-keep-or-stop choice on, so when a fixpoint is not reached (or reached too early),
   * this is the first thing to look at.
   */
  public static String explainPathCase(BlockGraphPath pIncoming, BlockGraphPath pExisting) {
    PathCase matched = pIncoming.getFirstMatchingCase(pExisting);
    return table(
        ImmutableList.of("predicate", "result"),
        ImmutableList.of(
            ImmutableList.of("incoming", render(pIncoming)),
            ImmutableList.of("existing", render(pExisting)),
            ImmutableList.of("existing.isSuffixOf(incoming)", isSuffix(pExisting, pIncoming)),
            ImmutableList.of("incoming.overlapsWith(existing)", overlaps(pIncoming, pExisting)),
            ImmutableList.of("existing.isPrefixOf(incoming)", isPrefix(pExisting, pIncoming)),
            ImmutableList.of("=> first matching case", matched.name())));
  }

  private static String isSuffix(BlockGraphPath pInner, BlockGraphPath pOuter) {
    return Boolean.toString(pInner.isSuffixOf(pOuter));
  }

  private static String isPrefix(BlockGraphPath pInner, BlockGraphPath pOuter) {
    return Boolean.toString(pInner.isPrefixOf(pOuter));
  }

  private static String overlaps(BlockGraphPath pLeft, BlockGraphPath pRight) {
    return Boolean.toString(pLeft.overlapsWith(pRight));
  }

  /**
   * Renders the {@link PathCase} of every (incoming, existing) path pair as a matrix.
   *
   * <p>Call this with the paths of a freshly received postcondition and the paths already stored by
   * a block to see, in one glance, which stored preconditions are about to be replaced, kept, or
   * declared a fixpoint.
   */
  public static String pathCaseMatrix(
      Iterable<BlockGraphPath> pIncoming, Iterable<BlockGraphPath> pExisting) {
    ImmutableList<BlockGraphPath> existing = ImmutableList.copyOf(pExisting);
    ImmutableList.Builder<String> header = ImmutableList.builder();
    header.add("incoming \\ existing");
    for (BlockGraphPath path : existing) {
      header.add(render(path));
    }
    List<List<String>> rows = new ArrayList<>();
    for (BlockGraphPath incoming : pIncoming) {
      ImmutableList.Builder<String> row = ImmutableList.builder();
      row.add(render(incoming));
      for (BlockGraphPath old : existing) {
        row.add(incoming.getFirstMatchingCase(old).name());
      }
      rows.add(row.build());
    }
    if (rows.isEmpty() || existing.isEmpty()) {
      return "<no path pairs to compare>";
    }
    return table(header.build(), rows);
  }

  // ===================================================================================
  // Messages
  // ===================================================================================

  /** One-line summary of a message: type, sender, number of states, status, timestamp. */
  public static String summarize(DssMessage pMessage) {
    StringBuilder result =
        new StringBuilder(pMessage.getType().name())
            .append(" from ")
            .append(pMessage.getSenderId());
    switch (pMessage) {
      case DssConditionMessage conditionMessage -> {
        result.append(" states=").append(conditionMessage.getNumberOfContainedStates());
        result.append(" status=").append(renderStatus(conditionMessage.getAlgorithmStatus()));
      }
      case DssResultMessage resultMessage -> {
        result.append(" result=").append(resultMessage.getResult());
      }
      case DssWitnessMessage witnessMessage -> {
        result.append(" witnessType=").append(witnessMessage.getWitnessType());
      }
      case DssExceptionMessage exceptionMessage -> {
        result.append(" exception=").append(firstLine(exceptionMessage.getExceptionMessage()));
      }
    }
    return result.append(" at ").append(pMessage.getTimestamp()).toString();
  }

  private static String renderStatus(AlgorithmStatus pStatus) {
    return (pStatus.isSound() ? "sound" : "UNSOUND")
        + "+"
        + (pStatus.isPrecise() ? "precise" : "IMPRECISE")
        + (pStatus.wasPropertyChecked() ? "" : "+NO_PROPERTY_CHECKED");
  }

  /**
   * Renders a message with its content decoded: the state list with {@code <CPA>.<field>} key space
   * is regrouped per contained state and per CPA, opaque blobs are elided, and the serialized
   * {@link BlockState} is expanded into block, history and witness.
   */
  public static String describe(DssMessage pMessage) {
    StringBuilder body = new StringBuilder();
    body.append(renderMessageMetadata(pMessage));
    if (pMessage instanceof DssMessageWithStates messageWithStates) {
      body.append(renderMessageStates(messageWithStates));
    }
    return box(summarize(pMessage), body.toString());
  }

  private static String renderMessageMetadata(DssMessage pMessage) {
    List<List<String>> rows = new ArrayList<>();

    rows.add(ImmutableList.of("senderId", pMessage.getSenderId()));
    rows.add(ImmutableList.of("type", pMessage.getType().name()));
    rows.add(ImmutableList.of("timestamp", String.valueOf(pMessage.getTimestamp())));

    switch (pMessage) {
      case DssConditionMessage conditionMessage -> {
        rows.add(ImmutableList.of("status", renderStatus(conditionMessage.getAlgorithmStatus())));
      }
      case DssResultMessage resultMessage -> {
        rows.add(ImmutableList.of("result", String.valueOf(resultMessage.getResult())));
      }
      case DssExceptionMessage exceptionMessage -> {
        rows.add(ImmutableList.of("exception", exceptionMessage.getExceptionMessage()));
      }
      case DssWitnessMessage witnessMessage -> {
        rows.add(ImmutableList.of("witnessType", witnessMessage.getWitnessType().name()));
      }
    }
    return table(ImmutableList.of("key", "value"), rows);
  }

  private static String renderMessageStates(DssMessageWithStates pMessage) {
    StringBuilder body = new StringBuilder();

    for (int i = 0; i < pMessage.getNumberOfContainedStates(); i++) {
      body.append("\n\nstate ").append(i).append(':');
      body.append('\n').append(indent("  ", renderStateContent(pMessage.getStates().get(i))));
    }
    return body.toString();
  }

  private static String renderStateContent(DssStateAndPrecisionPayload pStateContent) {
    List<List<String>> rows = new ArrayList<>();
    String componentKey = pStateContent.state().getComponentKey().getSerializedName();
    addPayloadRows(rows, "state", componentKey, pStateContent.state());
    addPayloadRows(rows, "precision", componentKey, pStateContent.precision());
    return table(ImmutableList.of("kind", "cpa", "field", "value"), rows);
  }

  private static void addPayloadRows(
      List<List<String>> pRows, String pKind, String pComponent, Object pPayload) {
    if (pPayload instanceof DssNoPrecisionPayload) {
      return;
    }

    boolean hasReadable = hasNonNullRecordComponent(pPayload, "readableFormula");
    addStructuredRows(pRows, pKind, pComponent, "", pPayload, hasReadable);

    if (pPayload instanceof DssBlockStatePayload blockStatePayload) {
      pRows.add(
          ImmutableList.of(
              pKind,
              pComponent,
              "-> decoded",
              decodeBlockState(blockStatePayload.getWitnessString())));
    }
  }

  private static void addStructuredRows(
      List<List<String>> pRows,
      String pKind,
      String pComponent,
      String pPath,
      @Nullable Object pValue,
      boolean pHasReadable) {
    if (pValue == null) {
      pRows.add(ImmutableList.of(pKind, pComponent, pPath, "<absent>"));
      return;
    }
    if (pValue instanceof DssNoPrecisionPayload) {
      return;
    }
    if (pValue instanceof Record record) {
      RecordComponent[] components = record.getClass().getRecordComponents();
      if (components == null) {
        addScalarRow(pRows, pKind, pComponent, pPath, pValue, pHasReadable);
        return;
      }
      for (RecordComponent component : components) {
        String path = joinPath(pPath, component.getName());
        try {
          addStructuredRows(
              pRows, pKind, pComponent, path, component.getAccessor().invoke(record), pHasReadable);
        } catch (ReflectiveOperationException e) {
          pRows.add(ImmutableList.of(pKind, pComponent, path, "<unavailable: " + e + ">"));
        }
      }
      return;
    }
    if (pValue instanceof Map<?, ?> map) {
      for (Entry<?, ?> entry : map.entrySet()) {
        addStructuredRows(
            pRows,
            pKind,
            pComponent,
            joinPath(pPath, "[" + entry.getKey() + "]"),
            entry.getValue(),
            pHasReadable);
      }
      return;
    }
    if (pValue instanceof Iterable<?> iterable) {
      int index = 0;
      for (Object value : iterable) {
        addStructuredRows(
            pRows, pKind, pComponent, joinPath(pPath, "[" + index++ + "]"), value, pHasReadable);
      }
      return;
    }
    addScalarRow(pRows, pKind, pComponent, pPath, pValue, pHasReadable);
  }

  private static void addScalarRow(
      List<List<String>> pRows,
      String pKind,
      String pComponent,
      String pPath,
      Object pValue,
      boolean pHasReadable) {
    String field = lastPathPart(pPath);
    String value = String.valueOf(pValue);
    pRows.add(
        ImmutableList.of(pKind, pComponent, pPath, renderContentValue(field, value, pHasReadable)));
  }

  private static boolean hasNonNullRecordComponent(Object pValue, String pName) {
    if (!(pValue instanceof Record record)) {
      return false;
    }
    RecordComponent[] components = record.getClass().getRecordComponents();
    if (components == null) {
      return false;
    }
    for (RecordComponent component : components) {
      if (component.getName().equals(pName)) {
        try {
          return component.getAccessor().invoke(record) != null;
        } catch (ReflectiveOperationException e) {
          return false;
        }
      }
    }
    return false;
  }

  private static String joinPath(String pPrefix, String pPart) {
    return pPrefix.isEmpty() ? pPart : pPrefix + "." + pPart;
  }

  private static String lastPathPart(String pPath) {
    int dot = pPath.lastIndexOf('.');
    return dot < 0 ? pPath : pPath.substring(dot + 1);
  }

  private static String renderContentValue(String pField, String pValue, boolean pHasReadable) {
    if (OPAQUE_FIELDS.contains(pField)) {
      return "<opaque blob, " + pValue.length() + " chars, elided>";
    }
    if (pHasReadable && ("state".equals(pField) || "formula".equals(pField))) {
      return "<smt2, " + pValue.length() + " chars; see 'readable'>";
    }
    if (pValue.isEmpty()) {
      return "<empty>";
    }
    return shortValue(pValue);
  }

  private static String decodeBlockState(String pSerialized) {
    try {
      DeserializeBlockStateOperator.ParseResult parsed =
          DeserializeBlockStateOperator.parseWitness(pSerialized);
      return "block="
          + parsed.serializedBlockState()
          + " history="
          + render(parsed.history())
          + " witnessSegments="
          + parsed.witness().size();
    } catch (RuntimeException e) {
      return "<unparsable: " + e + ">";
    }
  }

  /** Renders one summary row per message, in the given order. */
  public static String messageTable(Iterable<DssMessage> pMessages) {
    List<List<String>> rows = new ArrayList<>();
    int index = 0;
    for (DssMessage message : pMessages) {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      builder.add(Integer.toString(index++), message.getType().name(), message.getSenderId());
      if (message instanceof DssMessageWithStates messageWithStates) {
        builder.add(Integer.toString(messageWithStates.getNumberOfContainedStates()));
      } else {
        builder.add("0");
      }

      builder.add(String.valueOf(message.getTimestamp()));
      rows.add(builder.build());
    }
    if (rows.isEmpty()) {
      return "<no messages>";
    }
    return table(ImmutableList.of("#", "type", "sender", "states", "timestamp"), rows);
  }

  /**
   * Compares the content of two messages key by key.
   *
   * <p>Two consecutive postconditions of the same block that ought to be equal but are not is the
   * classic reason for a non-terminating fixpoint. This shows exactly which key differs.
   */
  public static String diff(DssMessage pLeft, DssMessage pRight) {
    DssMessagePayload left = pLeft.asJsonPayload();
    DssMessagePayload right = pRight.asJsonPayload();

    List<List<String>> rows = new ArrayList<>();
    addHeaderDiff(rows, left.header(), right.header());
    addStatusDiff(rows, left.status(), right.status());
    addStatesDiff(rows, left.states(), right.states());
    addContentDiff(rows, left.content(), right.content());

    String body =
        rows.isEmpty()
            ? "contents are equal"
            : table(ImmutableList.of("key", "left", "right"), rows);
    return box("diff: " + summarize(pLeft) + "  vs  " + summarize(pRight), body);
  }

  private static void addHeaderDiff(
      List<List<String>> pRows, DssHeaderPayload pLeft, DssHeaderPayload pRight) {
    addDiffRow(
        pRows,
        DssMessageKeys.HEADER + "." + DssMessageKeys.SENDER_ID,
        pLeft.senderId(),
        pRight.senderId());
    addDiffRow(
        pRows,
        DssMessageKeys.HEADER + "." + DssMessageKeys.MESSAGE_TYPE,
        pLeft.messageType(),
        pRight.messageType());
    addDiffRow(
        pRows,
        DssMessageKeys.HEADER + "." + DssMessageKeys.TIMESTAMP,
        pLeft.timestamp(),
        pRight.timestamp());
    addDiffRow(
        pRows,
        DssMessageKeys.HEADER + "." + DssMessageKeys.IDENTIFIER,
        pLeft.identifier(),
        pRight.identifier());
  }

  private static void addStatusDiff(
      List<List<String>> pRows,
      @Nullable DssStatusPayload pLeft,
      @Nullable DssStatusPayload pRight) {
    if (pLeft == null || pRight == null) {
      addDiffRow(pRows, DssMessageKeys.STATUS, pLeft, pRight);
      return;
    }

    addDiffRow(
        pRows, DssMessageKeys.STATUS + "." + DssMessageKeys.SOUND, pLeft.sound(), pRight.sound());
    addDiffRow(
        pRows,
        DssMessageKeys.STATUS + "." + DssMessageKeys.PRECISE,
        pLeft.precise(),
        pRight.precise());
    addDiffRow(
        pRows,
        DssMessageKeys.STATUS + "." + DssMessageKeys.PROPERTY,
        pLeft.propertyChecked(),
        pRight.propertyChecked());
  }

  private static void addStatesDiff(
      List<List<String>> pRows,
      ImmutableList<DssStateAndPrecisionPayload> pLeft,
      ImmutableList<DssStateAndPrecisionPayload> pRight) {
    int maxStates = Math.max(pLeft.size(), pRight.size());
    for (int i = 0; i < maxStates; i++) {
      if (i >= pLeft.size()) {
        addDiffRow(pRows, DssMessageKeys.STATES + "[" + i + "]", null, "<present>");
      } else if (i >= pRight.size()) {
        addDiffRow(pRows, DssMessageKeys.STATES + "[" + i + "]", "<present>", null);
      } else {
        addStateDiff(pRows, DssMessageKeys.STATES + "[" + i + "]", pLeft.get(i), pRight.get(i));
      }
    }
  }

  private static void addStateDiff(
      List<List<String>> pRows,
      String pPrefix,
      DssStateAndPrecisionPayload pLeft,
      DssStateAndPrecisionPayload pRight) {
    String leftComponentKey = pLeft.state().getComponentKey().getSerializedName();
    String rightComponentKey = pRight.state().getComponentKey().getSerializedName();

    addDiffRow(pRows, pPrefix + ".component", leftComponentKey, rightComponentKey);
    addPayloadDiff(pRows, pPrefix + ".state", pLeft.state(), pRight.state());
    addPrecisionDiff(pRows, pPrefix + ".precision", pLeft.precision(), pRight.precision());
  }

  private static void addPayloadDiff(
      List<List<String>> pRows, String pPrefix, Object pLeft, Object pRight) {
    addStructuredDiff(pRows, pPrefix, pLeft, pRight);
  }

  private static void addPrecisionDiff(
      List<List<String>> pRows, String pPrefix, Object pLeft, Object pRight) {
    addStructuredDiff(pRows, pPrefix, pLeft, pRight);
  }

  private static void addStructuredDiff(
      List<List<String>> pRows, String pPath, @Nullable Object pLeft, @Nullable Object pRight) {
    if (Objects.equals(pLeft, pRight)) {
      return;
    }

    if (pLeft == null || pRight == null) {
      addDiffRow(pRows, pPath, pLeft, pRight);
      return;
    }
    if (pLeft instanceof DssNoPrecisionPayload || pRight instanceof DssNoPrecisionPayload) {
      addDiffRow(pRows, pPath, pLeft, pRight);
      return;
    }
    if (pLeft instanceof Record leftRecord && pRight instanceof Record rightRecord) {
      if (!pLeft.getClass().equals(pRight.getClass())) {
        addDiffRow(pRows, pPath, pLeft, pRight);
        return;
      }
      RecordComponent[] components = leftRecord.getClass().getRecordComponents();
      if (components == null) {
        addDiffRow(pRows, pPath, pLeft, pRight);
        return;
      }
      for (RecordComponent component : components) {
        try {
          addStructuredDiff(
              pRows,
              joinPath(pPath, component.getName()),
              component.getAccessor().invoke(leftRecord),
              component.getAccessor().invoke(rightRecord));
        } catch (ReflectiveOperationException e) {
          addDiffRow(pRows, joinPath(pPath, component.getName()), "<unavailable>", e);
        }
      }
      return;
    }
    if (pLeft instanceof Map<?, ?> leftMap && pRight instanceof Map<?, ?> rightMap) {
      Set<Object> keys = new LinkedHashSet<>(leftMap.keySet());
      keys.addAll(rightMap.keySet());
      for (Object key : keys) {
        addStructuredDiff(
            pRows, joinPath(pPath, "[" + key + "]"), leftMap.get(key), rightMap.get(key));
      }
      return;
    }
    if (pLeft instanceof List<?> leftList && pRight instanceof List<?> rightList) {
      int maxSize = Math.max(leftList.size(), rightList.size());
      for (int i = 0; i < maxSize; i++) {
        addStructuredDiff(
            pRows,
            joinPath(pPath, "[" + i + "]"),
            i < leftList.size() ? leftList.get(i) : null,
            i < rightList.size() ? rightList.get(i) : null);
      }
      return;
    }

    addDiffRow(pRows, pPath, pLeft, pRight);
  }

  private static void addContentDiff(
      List<List<String>> pRows,
      ImmutableMap<String, String> pLeft,
      ImmutableMap<String, String> pRight) {
    addMapDiff(pRows, DssMessageKeys.CONTENT, pLeft, pRight);
  }

  private static void addMapDiff(
      List<List<String>> pRows,
      String pPrefix,
      ImmutableMap<String, String> pLeft,
      ImmutableMap<String, String> pRight) {
    Set<String> keys = new LinkedHashSet<>(pLeft.keySet());
    keys.addAll(pRight.keySet());

    for (String key : keys) {
      addDiffRow(pRows, pPrefix + "." + key, pLeft.get(key), pRight.get(key));
    }
  }

  private static void addDiffRow(
      List<List<String>> pRows, String pField, @Nullable Object pLeft, @Nullable Object pRight) {
    if (Objects.equals(pLeft, pRight)) {
      return;
    }

    pRows.add(ImmutableList.of(pField, diffValue(pField, pLeft), diffValue(pField, pRight)));
  }

  private static String diffValue(String pField, @Nullable Object pValue) {
    if (pValue == null) {
      return "<absent>";
    }
    if (pValue instanceof DssNoPrecisionPayload) {
      return "<none>";
    }
    String value = String.valueOf(pValue);
    String fieldName = pField.substring(pField.lastIndexOf('.') + 1);
    if (OPAQUE_FIELDS.contains(fieldName)) {
      return "<opaque blob, " + value.length() + " chars, elided>";
    }
    if (value.isEmpty()) {
      return "<empty>";
    }
    // Narrower than shortValue: a diff has two value columns side by side.
    return abbreviate(singleLine(value), DEFAULT_MAX_VALUE_LENGTH / 2);
  }

  // ===================================================================================
  // Reached sets and ARGs
  // ===================================================================================

  /**
   * Renders a reached set as a table: ARG id, parents, location, block state, waitlist membership,
   * and the predicate formula.
   */
  public static String describeReachedSet(UnmodifiableReachedSet pReachedSet) {
    Set<AbstractState> waitlist = new LinkedHashSet<>(pReachedSet.getWaitlist());
    List<List<String>> rows = new ArrayList<>();
    for (AbstractState state : pReachedSet.asCollection()) {
      ARGState argState = AbstractStates.extractStateByType(state, ARGState.class);
      BlockState blockState = AbstractStates.extractStateByType(state, BlockState.class);
      PredicateAbstractState predicate =
          AbstractStates.extractStateByType(state, PredicateAbstractState.class);
      rows.add(
          ImmutableList.of(
              argState == null ? "-" : "#" + argState.getStateId(),
              argState == null ? "-" : argIds(argState.getParents()),
              String.valueOf(AbstractStates.extractLocation(state)),
              blockState == null
                  ? "-"
                  : blockState.getBlockNode().getId() + ":" + blockState.getType(),
              waitlist.contains(state) ? "waiting" : "",
              AbstractStates.isTargetState(state) ? "TARGET" : "",
              predicate == null ? "-" : abbreviate(singleLine(formulaOf(predicate)), 90)));
    }
    return box(
        "ReachedSet (" + pReachedSet.size() + " states, " + waitlist.size() + " waiting)",
        table(
            ImmutableList.of("arg", "parents", "location", "block", "waitlist", "target", "pred"),
            rows));
  }

  /** Renders an ARG path as an alternating sequence of states and edges. */
  public static String describe(ARGPath pPath) {
    StringBuilder result = new StringBuilder();
    ImmutableList<ARGState> states = pPath.asStatesList();
    List<@Nullable CFAEdge> edges = pPath.getInnerEdges();
    for (int i = 0; i < states.size(); i++) {
      result.append(oneLine(states.get(i))).append('\n');
      if (i < edges.size()) {
        result
            .append("      | ")
            .append(edges.get(i) == null ? "<no edge>" : singleLine(edges.get(i).toString()))
            .append('\n');
      }
    }
    return box("ARGPath (" + pPath.size() + " states)", result.toString().stripTrailing());
  }

  /** Renders the ARG of a reached set in DOT format. */
  public static String argToDot(UnmodifiableReachedSet pReachedSet) throws IOException {
    StringBuilder result = new StringBuilder();
    ARGToDotWriter.write(
        result,
        FluentIterable.from(pReachedSet.asCollection()).filter(ARGState.class).toList(),
        "ARG");
    return result.toString();
  }

  // ===================================================================================
  // Pre- and violation conditions of a block
  // ===================================================================================

  /**
   * Renders the preconditions and violation conditions currently stored by a block, grouped by the
   * key they are stored under (a {@link BlockGraphPath} for preconditions, a sender id for
   * violation conditions).
   */
  public static <Pre, Viol> String prettyPrintBlock(
      String pId,
      Multimap<Pre, @NonNull StateAndPrecision> pPreconditions,
      Multimap<Viol, @NonNull StateAndPrecision> pViolationConditions) {
    return prettyPrintBlock(pId, pPreconditions, pViolationConditions, DssDebugUtils::oneLine);
  }

  /**
   * Same as {@link #prettyPrintBlock(String, Multimap, Multimap)}, but renders every state with
   * {@code pStateToString}.
   */
  public static <Pre, Viol> String prettyPrintBlock(
      String pId,
      Multimap<Pre, @NonNull StateAndPrecision> pPreconditions,
      Multimap<Viol, @NonNull StateAndPrecision> pViolationConditions,
      Function<AbstractState, String> pStateToString) {
    String body =
        "preconditions ("
            + pPreconditions.size()
            + " states in "
            + pPreconditions.keySet().size()
            + " groups):\n"
            + indent("  ", renderConditions(pPreconditions, pStateToString))
            + "\n\nviolation conditions ("
            + pViolationConditions.size()
            + " states in "
            + pViolationConditions.keySet().size()
            + " groups):\n"
            + indent("  ", renderConditions(pViolationConditions, pStateToString));
    return box("Block " + pId, body);
  }

  private static <K> String renderConditions(
      Multimap<K, @NonNull StateAndPrecision> pConditions,
      Function<AbstractState, String> pStateToString) {
    if (pConditions.isEmpty()) {
      return "<none>";
    }
    List<List<String>> rows = new ArrayList<>();
    for (K key : pConditions.keySet()) {
      String renderedKey = key instanceof BlockGraphPath path ? render(path) : String.valueOf(key);
      for (StateAndPrecision stateAndPrecision : pConditions.get(key)) {
        rows.add(ImmutableList.of(renderedKey, pStateToString.apply(stateAndPrecision.state())));
        renderedKey = "";
      }
    }
    return table(ImmutableList.of("group", "state"), rows);
  }

  /**
   * Renders the pre-/violation conditions of a predicate-based block analysis, showing the
   * abstraction formula and the block-graph history of every state.
   */
  public static <Pre, Viol> String prettyPrintPredicateAnalysisBlock(
      BlockNode pBlockNode,
      Multimap<Pre, @NonNull StateAndPrecision> pPreconditions,
      Multimap<Viol, @NonNull StateAndPrecision> pViolationConditions) {
    return prettyPrintBlock(
        pBlockNode.getId(),
        pPreconditions,
        pViolationConditions,
        DssDebugUtils::predicateAndHistory);
  }

  private static String predicateAndHistory(AbstractState pState) {
    PredicateAbstractState predicate =
        AbstractStates.extractStateByType(pState, PredicateAbstractState.class);
    BlockState blockState = AbstractStates.extractStateByType(pState, BlockState.class);
    return (predicate == null ? "<no predicate state>" : shortValue(formulaOf(predicate)))
        + " ("
        + (blockState == null ? "<no block state>" : render(blockState.getHistory()))
        + ")";
  }

  // ===================================================================================
  // Dumping to disk
  // ===================================================================================

  /**
   * Writes {@code pContent} to a fresh file below {@link #DEBUG_DIR} and returns its path.
   *
   * <p>The file name carries a global counter and the current thread name, so that repeated dumps
   * from concurrently running workers neither overwrite each other nor lose their ordering. If
   * {@code pName} has no file extension, {@code .txt} is appended.
   *
   * @throws UncheckedIOException if the file cannot be written; debugging code should not be forced
   *     to handle checked exceptions
   */
  public static Path dump(String pName, String pContent) {
    String name = sanitize(pName);
    if (name.lastIndexOf('.') <= 0) {
      name += ".txt";
    }
    Path target =
        DEBUG_DIR.resolve(
            String.format(
                "%04d-%s-%s",
                DUMP_COUNTER.getAndIncrement(), sanitize(Thread.currentThread().getName()), name));
    try {
      Files.createDirectories(DEBUG_DIR);
      IO.writeFile(target, StandardCharsets.UTF_8, pContent);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write DSS debug dump to " + target, e);
    }
    return target;
  }

  /** Dumps the ARG of {@code pReachedSet} in DOT format below {@link #DEBUG_DIR}. */
  public static Path dumpArg(String pName, UnmodifiableReachedSet pReachedSet) {
    try {
      return dump(pName + ".dot", argToDot(pReachedSet));
    } catch (IOException e) {
      throw new UncheckedIOException("Could not render ARG of " + pName, e);
    }
  }

  private static String sanitize(String pName) {
    return pName.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  private static String firstLine(String pText) {
    return LINE_SPLITTER.split(pText).iterator().next();
  }

  /**
   * Timestamp helper for correlating a log line with the messages exported by the {@code
   * DssVisualizationWorker}, which uses the same nanosecond representation.
   */
  @SuppressWarnings("JavaInstantGetSecondsGetNano")
  public static long toExportedNanos(Instant pInstant) {
    return pInstant.getEpochSecond() * 1_000_000_000L + pInstant.getNano();
  }
}
