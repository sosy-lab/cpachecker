// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.Appenders.AbstractAppender;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ErrorPathShrinker;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.AdditionalInfoConverter;
import org.sosy_lab.cpachecker.util.Pair;

public class CounterexampleInfo extends AbstractAppender {

  private static final UniqueIdGenerator ID_GENERATOR = new UniqueIdGenerator();

  private final int uniqueId;

  private final boolean spurious;
  private final boolean isPreciseCounterExample;

  private final ARGPath targetPath;

  private final CFAPathWithAssumptions assignments;
  private final CFAPathWithAdditionalInfo additionalInfo;

  // list with additional information about the counterexample
  private final Collection<Pair<Object, PathTemplate>> furtherInfo;

  private static final CounterexampleInfo SPURIOUS =
      new CounterexampleInfo(true, null, null, false, CFAPathWithAdditionalInfo.empty());

  protected CounterexampleInfo(
      boolean pSpurious,
      ARGPath pTargetPath,
      CFAPathWithAssumptions pAssignments,
      boolean pIsPreciseCEX,
      CFAPathWithAdditionalInfo pAdditionalInfo) {
    uniqueId = ID_GENERATOR.getFreshId();
    spurious = pSpurious;
    targetPath = pTargetPath;
    assignments = pAssignments;
    additionalInfo = pAdditionalInfo;
    isPreciseCounterExample = pIsPreciseCEX;

    if (!spurious) {
      furtherInfo = new ArrayList<>(1);
    } else {
      furtherInfo = null;
    }
  }

  public static CounterexampleInfo spurious() {
    return SPURIOUS;
  }

  public int getUniqueId() {
    return uniqueId;
  }

  public boolean isPreciseCounterExample() {
    checkState(!spurious);
    return isPreciseCounterExample;
  }

  /**
   * Creates a feasible counterexample whose target path is marked as being imprecise.
   *
   * <p>Use this factory method <em>if and only if</em> the target path is not precise, i.e. it is
   * only guaranteed that its first and last state are the first and last state of the intended
   * precise target path, but the real states of the precise target path between its initial and
   * target state are unknown.
   *
   * <p>Do <em>not</em> use this factory method if the target path is precise but you do not have
   * any variable assignments or assumptions; instead, create a {@link CFAPathWithAssumptions}
   * without assumptions and use {@link #feasiblePrecise(ARGPath, CFAPathWithAssumptions)}.
   *
   * @param pTargetPath an imprecise representation of the path from the first state to the target
   *     state; the states between these two state are not required to be part of the intended
   *     actual target path.
   * @return an object representing information about an feasible counterexample with an imprecise
   *     and unreliable representation of the path from the first state to the target state.
   */
  public static CounterexampleInfo feasibleImprecise(ARGPath pTargetPath) {
    return feasibleImprecise(checkNotNull(pTargetPath), CFAPathWithAdditionalInfo.empty());
  }

  public static CounterexampleInfo feasibleImprecise(
      ARGPath pTargetPath, CFAPathWithAdditionalInfo pAdditionalInfo) {
    return new CounterexampleInfo(
        false, checkNotNull(pTargetPath), null, false, checkNotNull(pAdditionalInfo));
  }

  /**
   * Creates a feasible counterexample whose target path is marked as being precise.
   *
   * <p>Use this factory method <em>if and only if</em> the target path is precise, i.e. if it
   * precisely represents the intended path from the first state to the target state.
   *
   * @param pTargetPath a precise representation of the path from the first state to the target
   *     state.
   * @param pAssignments a mapping of assumptions over program variables, e.g. in the form of
   *     variable assignments to edges on the target path. This mapping must contain all edges on
   *     the target path, but providing assumptions over program variables in the context of these
   *     edges is optional.
   * @return an object representing information about an feasible counterexample with an precise
   *     representation of the path from the first state to the target state.
   */
  public static CounterexampleInfo feasiblePrecise(
      ARGPath pTargetPath, CFAPathWithAssumptions pAssignments) {
    return feasiblePrecise(
        checkNotNull(pTargetPath), pAssignments, CFAPathWithAdditionalInfo.empty());
  }

  public static CounterexampleInfo feasiblePrecise(
      ARGPath pTargetPath,
      CFAPathWithAssumptions pAssignments,
      CFAPathWithAdditionalInfo pAdditionalInfo) {
    checkArgument(!pAssignments.isEmpty());
    checkArgument(pAssignments.fitsPath(pTargetPath.getFullPath()));
    return new CounterexampleInfo(
        false, checkNotNull(pTargetPath), pAssignments, true, checkNotNull(pAdditionalInfo));
  }

  public boolean isSpurious() {
    return spurious;
  }

  /**
   * Gets the target path recorded for this counterexample.
   *
   * <p>If this counterexample is precise ({@link #isPreciseCounterExample()}), this path is
   * guaranteed to precisely represent the intended path to the target state.
   *
   * <p>If, on the other hand, the counterexample is imprecise, caution is advised, because only its
   * first and last state (target state) are guaranteed to be the first and last state of the
   * intended counterexample path; any states on the path between first and last state may or may
   * not be components of a valid counterexample path. Some producers of counterexamples may provide
   * further guarantees for the recorded target path of an imprecise counterexample, but these
   * additional guarantees are not enforced globally.
   *
   * @return the target path recorded for this counterexample.
   */
  public ARGPath getTargetPath() {
    checkState(!spurious);
    assert targetPath != null;

    return targetPath;
  }

  /**
   * Gets the target state of the counterexample path.
   *
   * @return the target state of the counterexample path.
   */
  public ARGState getTargetState() {
    return getTargetPath().getLastState();
  }

  /**
   * Gets the root state of the counterexample path.
   *
   * @return the root state of the counterexample path.
   */
  public ARGState getRootState() {
    return getTargetPath().getFirstState();
  }

  /**
   * Return a path that indicates which variables where assigned which values at what edge. Note
   * that not every value for every variable is available.
   *
   * <p>This is only available for precise counterexamples.
   */
  public CFAPathWithAssumptions getCFAPathWithAssignments() {
    checkState(!spurious);
    checkState(isPreciseCounterExample);
    return assignments;
  }

  /**
   * Return an assignment from ARGStates to variable values. Note that not every value for every
   * variable is available.
   *
   * <p>This is only available for precise counterexamples.
   */
  public ImmutableSetMultimap<ARGState, CFAEdgeWithAssumptions> getExactVariableValues() {
    checkState(!spurious);
    checkState(isPreciseCounterExample);
    return assignments.getExactVariableValues(targetPath);
  }

  public Map<ARGState, CFAEdgeWithAdditionalInfo> getAdditionalInfoMapping() {
    return additionalInfo.isEmpty()
        ? ImmutableMap.of()
        : additionalInfo.getAdditionalInfoMapping(targetPath);
  }

  public Set<AdditionalInfoConverter> getAdditionalInfoConverters() {
    return additionalInfo.isEmpty()
        ? ImmutableSet.of()
        : additionalInfo.getAdditionalInfoConverters();
  }

  /**
   * Create a JSON representation of this counterexample, which is used for the HTML report.
   *
   * @param sb The output to write to.
   */
  public void toJSON(Appendable sb) throws IOException {
    checkState(!spurious);
    int pathLength = targetPath.getFullPath().size();
    List<Map<?, ?>> path = new ArrayList<>(pathLength);

    ErrorPathShrinker pathShrinker = new ErrorPathShrinker();
    List<Pair<CFAEdgeWithAssumptions, Boolean>> shrinkedErrorPath =
        pathShrinker.shrinkErrorPath(targetPath, assignments);
    // Create Iterator for ShrinkedErrorPath
    Iterator<Pair<CFAEdgeWithAssumptions, Boolean>> shrinkedErrorPathIterator = null;
    if (shrinkedErrorPath != null) {
      // checkState(shrinkedErrorPath.size() == targetPath.size(), "Size of shrinkedErrorPath not
      // identical to the length of the targetPath!");
      shrinkedErrorPathIterator = shrinkedErrorPath.iterator();
    }

    PathIterator iterator = targetPath.fullPathIterator();
    while (iterator.hasNext()) {
      Map<String, Object> elem = new HashMap<>();
      CFAEdge edge = iterator.getOutgoingEdge();
      if (edge == null) {
        // TODO why not iterator.advance();
        continue; // in this case we do not need the edge
      }

      // compare path from counterexample with shrinkedErrorPath to identify the important edges
      elem.put("importance", 0);
      if (shrinkedErrorPathIterator != null && shrinkedErrorPathIterator.hasNext()) {
        Pair<CFAEdgeWithAssumptions, Boolean> shrinkedEdge = shrinkedErrorPathIterator.next();
        if (edge.equals(shrinkedEdge.getFirst().getCFAEdge())) {
          if (shrinkedEdge.getSecond()) {
            elem.put("importance", 1);
          }
        }
      }

      if (iterator.isPositionWithState()) {
        elem.put("argelem", iterator.getAbstractState().getStateId());
      }
      elem.put("source", edge.getPredecessor().getNodeNumber());
      elem.put("target", edge.getSuccessor().getNodeNumber());
      elem.put("desc", edge.getDescription().replace('\n', ' '));
      elem.put("line", edge.getFileLocation().getStartingLineInOrigin());
      elem.put("file", edge.getFileLocation().getFileName());

      // cfa path with assignments has no padding (only inner edges of argpath).
      if (assignments == null) {
        elem.put("val", "");
      } else {
        CFAEdgeWithAssumptions edgeWithAssignment = assignments.get(iterator.getIndex());
        elem.put(
            "val",
            edgeWithAssignment.prettyPrintCode(0).replace(System.lineSeparator(), "\n")
                + edgeWithAssignment.getComment().replace(System.lineSeparator(), "\n"));
      }
      addAdditionalInfo(elem, edge);

      path.add(elem);
      iterator.advance();
    }
    JSON.writeJSONString(path, sb);
  }

  /**
   * Method for classes that inherit from this class. Append additional information to JSON-Object
   *
   * @param element map that will be converted to JSON
   * @param edge the edge that is currently transformed into JSON format.
   */
  protected void addAdditionalInfo(Map<String, Object> element, CFAEdge edge) {}

  @Override
  public void appendTo(Appendable out) throws IOException {
    if (isSpurious()) {
      out.append("SPURIOUS COUNTEREXAMPLE");

    } else if (isPreciseCounterExample) {
      for (CFAEdgeWithAssumptions edgeWithAssignments : from(assignments).filter(notNull())) {
        printPreciseValues(out, edgeWithAssignments);
      }

    } else {
      targetPath.appendTo(out);
    }
  }

  private void printPreciseValues(Appendable out, CFAEdgeWithAssumptions edgeWithAssignments)
      throws IOException {
    // TODO Cleanup all string-producing methods of CFAEdgeWithAssumptions and merge with this
    out.append(edgeWithAssignments.getCFAEdge().toString());
    out.append(System.lineSeparator());

    String cCode = edgeWithAssignments.prettyPrintCode(1);
    if (!cCode.isEmpty()) {
      out.append(cCode);
    }

    String comment = edgeWithAssignments.getComment();

    if (!comment.isEmpty()) {
      out.append('\t');
      out.append(comment);
      out.append(System.lineSeparator());
    }
  }
  /**
   * Add some additional information about the counterexample.
   *
   * @param info The information.
   * @param dumpFile The file where "info.toString()" should be dumped (may be null).
   */
  public void addFurtherInformation(Object info, PathTemplate dumpFile) {
    checkState(!spurious);

    furtherInfo.add(Pair.of(checkNotNull(info), dumpFile));
  }

  /**
   * Get all additional information stored in this object. A file where to dump it may be associated
   * with each object, but this part of the pair may be null.
   */
  public Collection<Pair<Object, PathTemplate>> getAllFurtherInformation() {
    checkState(!spurious);

    return Collections.unmodifiableCollection(furtherInfo);
  }
}
