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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath.PathCase;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DeserializeBlockStateOperator;
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
    if (pState instanceof DssCallstackState dssState && dssState.allowsAllTransfers()) {
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
    switch (pMessage.getType()) {
      case POST_CONDITION, VIOLATION_CONDITION -> {
        result.append(" states=").append(pMessage.getNumberOfContainedStates().orElse(0));
        result.append(" status=").append(renderStatus(pMessage.getAlgorithmStatus()));
      }
      case RESULT -> result.append(" result=").append(pMessage.getResult());
      case WITNESS -> result.append(" witnessType=").append(pMessage.getWitnessType());
      case EXCEPTION ->
          result.append(" exception=").append(firstLine(pMessage.getExceptionMessage()));
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
   * Renders a message with its content decoded: the flat {@code state<i>.<CPA>.<field>} key space
   * is regrouped per contained state and per CPA, opaque blobs are elided, and the serialized
   * {@link BlockState} is expanded into block, history and witness.
   */
  public static String describe(DssMessage pMessage) {
    ImmutableMap<String, ImmutableMap<String, String>> json = pMessage.asLegacyJson();
    ImmutableMap<String, String> content =
        Objects.requireNonNullElse(json.get(DssMessageKeys.CONTENT), ImmutableMap.of());

    // Entries that do not belong to a specific state (states, status, result, ...).
    Map<String, String> meta = new LinkedHashMap<>();
    // stateIndex -> component -> field -> value
    Map<Integer, Map<String, Map<String, String>>> states = new LinkedHashMap<>();

    for (Entry<String, String> entry : content.entrySet()) {
      StateKey key = StateKey.parse(entry.getKey());
      if (key == null) {
        meta.put(entry.getKey(), entry.getValue());
        continue;
      }
      states
          .computeIfAbsent(key.index(), i -> new LinkedHashMap<>())
          .computeIfAbsent(key.component(), c -> new LinkedHashMap<>())
          .put(key.field(), entry.getValue());
    }

    StringBuilder body = new StringBuilder();
    List<List<String>> metaRows = new ArrayList<>();
    metaRows.add(ImmutableList.of("timestamp", String.valueOf(pMessage.getTimestamp())));
    for (Entry<String, String> entry : meta.entrySet()) {
      metaRows.add(ImmutableList.of(entry.getKey(), shortValue(entry.getValue())));
    }
    body.append(table(ImmutableList.of("key", "value"), metaRows));

    for (Entry<Integer, Map<String, Map<String, String>>> state : states.entrySet()) {
      body.append("\n\nstate ").append(state.getKey()).append(':');
      List<List<String>> rows = new ArrayList<>();
      for (Entry<String, Map<String, String>> component : state.getValue().entrySet()) {
        boolean hasReadable = component.getValue().containsKey("readable");
        for (Entry<String, String> field : component.getValue().entrySet()) {
          rows.add(
              ImmutableList.of(
                  component.getKey(),
                  field.getKey(),
                  renderContentValue(field.getKey(), field.getValue(), hasReadable)));
        }
        String blockStateValue = component.getValue().get("state");
        if ("BlockState".equals(component.getKey()) && blockStateValue != null) {
          rows.add(ImmutableList.of("", "-> decoded", decodeBlockState(blockStateValue)));
        }
      }
      body.append('\n')
          .append(indent("  ", table(ImmutableList.of("cpa", "field", "value"), rows)));
    }

    return box(summarize(pMessage), body.toString());
  }

  private static String renderContentValue(String pField, String pValue, boolean pHasReadable) {
    if (OPAQUE_FIELDS.contains(pField)) {
      return "<opaque blob, " + pValue.length() + " chars, elided>";
    }
    if (pHasReadable && "state".equals(pField)) {
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
      rows.add(
          ImmutableList.of(
              Integer.toString(index++),
              message.getType().name(),
              message.getSenderId(),
              Integer.toString(message.getNumberOfContainedStates().orElse(0)),
              String.valueOf(message.getTimestamp())));
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
    ImmutableMap<String, String> left = contentOf(pLeft);
    ImmutableMap<String, String> right = contentOf(pRight);

    Set<String> allKeys = new LinkedHashSet<>(left.keySet());
    allKeys.addAll(right.keySet());

    List<List<String>> rows = new ArrayList<>();
    for (String key : allKeys) {
      String leftValue = left.get(key);
      String rightValue = right.get(key);
      if (Objects.equals(leftValue, rightValue)) {
        continue;
      }
      StateKey parsed = StateKey.parse(key);
      String field = parsed == null ? key : parsed.field();
      rows.add(
          ImmutableList.of(
              parsed == null
                  ? key
                  : "state" + parsed.index() + "." + parsed.component() + "." + field,
              diffValue(field, leftValue),
              diffValue(field, rightValue)));
    }
    String body =
        rows.isEmpty()
            ? "contents are equal"
            : table(ImmutableList.of("key", "left", "right"), rows);
    return box("diff: " + summarize(pLeft) + "  vs  " + summarize(pRight), body);
  }

  private static String diffValue(String pField, @Nullable String pValue) {
    if (pValue == null) {
      return "<absent>";
    }
    if (OPAQUE_FIELDS.contains(pField)) {
      return "<opaque blob, " + pValue.length() + " chars, elided>";
    }
    if (pValue.isEmpty()) {
      return "<empty>";
    }
    // Narrower than shortValue: a diff has two value columns side by side.
    return abbreviate(singleLine(pValue), DEFAULT_MAX_VALUE_LENGTH / 2);
  }

  private static ImmutableMap<String, String> contentOf(DssMessage pMessage) {
    return Objects.requireNonNullElse(
        pMessage.asLegacyJson().get(DssMessageKeys.CONTENT), ImmutableMap.of());
  }

  /**
   * A parsed message-content key of the shape {@code state<index>.<package>.<Component>.<field>}.
   */
  private record StateKey(int index, String component, String field) {

    private static final Splitter DOT_SPLITTER = Splitter.on('.');

    static @Nullable StateKey parse(String pKey) {
      if (!pKey.startsWith("state")) {
        return null;
      }
      int dot = pKey.indexOf('.');
      if (dot < 0) {
        return null;
      }
      String digits = pKey.substring("state".length(), dot);
      if (digits.isEmpty() || !digits.chars().allMatch(Character::isDigit)) {
        return null;
      }
      ImmutableList<String> segments =
          ImmutableList.copyOf(DOT_SPLITTER.split(pKey.substring(dot + 1)));
      // The component is the first segment starting with an upper-case letter: everything before it
      // is the package, everything after it is the (possibly dotted) field name.
      for (int i = 0; i < segments.size() - 1; i++) {
        if (Character.isUpperCase(segments.get(i).charAt(0))) {
          return new StateKey(
              Integer.parseInt(digits),
              segments.get(i),
              Joiner.on('.').join(segments.subList(i + 1, segments.size())));
        }
      }
      return new StateKey(Integer.parseInt(digits), "?", Joiner.on('.').join(segments));
    }
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
