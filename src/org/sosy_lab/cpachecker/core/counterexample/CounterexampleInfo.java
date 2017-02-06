/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.counterexample;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.Appenders.AbstractAppender;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.Pair;

public class CounterexampleInfo extends AbstractAppender {

  private static final UniqueIdGenerator ID_GENERATOR = new UniqueIdGenerator();

  private final int uniqueId;

  private final boolean spurious;
  private final boolean isPreciseCounterExample;

  private final ARGPath targetPath;
  private final CFAPathWithAssumptions assignments;

  // list with additional information about the counterexample
  private final Collection<Pair<Object, PathTemplate>> furtherInfo;

  private static final CounterexampleInfo SPURIOUS = new CounterexampleInfo(true, null, null, false);

  private CounterexampleInfo(boolean pSpurious, ARGPath pTargetPath,
      CFAPathWithAssumptions pAssignments, boolean pIsPreciseCEX) {
    uniqueId = ID_GENERATOR.getFreshId();
    spurious = pSpurious;
    targetPath = pTargetPath;
    assignments = pAssignments;
    isPreciseCounterExample = pIsPreciseCEX;

    if (!spurious) {
      furtherInfo = Lists.newArrayListWithExpectedSize(1);
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
   */
  public static CounterexampleInfo feasibleImprecise(ARGPath pTargetPath) {
    return new CounterexampleInfo(false, checkNotNull(pTargetPath), null, false);
  }

  /**
   * Creates a feasible counterexample whose target path is marked as being precise.
   */
  public static CounterexampleInfo feasiblePrecise(ARGPath pTargetPath, CFAPathWithAssumptions pAssignments) {
    checkArgument(!pAssignments.isEmpty());
    checkArgument(pAssignments.fitsPath(pTargetPath.getFullPath()));
    return new CounterexampleInfo(false, checkNotNull(pTargetPath), pAssignments, true);
  }

  public boolean isSpurious() {
    return spurious;
  }

  public ARGPath getTargetPath() {
    checkState(!spurious);
    assert targetPath != null;

    return targetPath;
  }

  /**
   * Return a path that indicates which variables where assigned which values at
   * what edge. Note that not every value for every variable is available.
   *
   * This is only available for precise counterexamples.
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
  public Multimap<ARGState, CFAEdgeWithAssumptions> getExactVariableValues() {
    checkState(!spurious);
    checkState(isPreciseCounterExample);
    return assignments.getExactVariableValues(targetPath);
  }

  /**
   * Create a JSON representation of this counterexample,
   * which is used for the HTML report.
   * @param sb The output to write to.
   */
  public void toJSON(Appendable sb) throws IOException {
    checkState(!spurious);
    int pathLength = targetPath.getFullPath().size();
    List<Map<?, ?>> path = new ArrayList<>(pathLength);

    PathIterator iterator = targetPath.fullPathIterator();
    while (iterator.hasNext()) {
      Map<String, Object> elem = new HashMap<>();
      CFAEdge edge = iterator.getOutgoingEdge();
      if (edge == null) {
        continue; // in this case we do not need the edge
      }
      if (iterator.isPositionWithState()) {
        elem.put("argelem", iterator.getAbstractState().getStateId());
      }
      elem.put("source", edge.getPredecessor().getNodeNumber());
      elem.put("target", edge.getSuccessor().getNodeNumber());
      elem.put("desc", edge.getDescription().replaceAll("\n", " "));
      elem.put("line", edge.getFileLocation().getStartingLineNumber());
      elem.put("file", edge.getFileLocation().getFileName());

      // cfa path with assignments has no padding (only inner edges of argpath).
      if (assignments == null) {
        elem.put("val", "");
      } else {
        CFAEdgeWithAssumptions edgeWithAssignment = assignments.get(iterator.getIndex());
        elem.put("val", edgeWithAssignment.printForHTML());
      }

      path.add(elem);
      iterator.advance();
    }
    JSON.writeJSONString(path, sb);
  }

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
   * Get all additional information stored in this object.
   * A file where to dump it may be associated with each object, but this part
   * of the pair may be null.
   */
  public Collection<Pair<Object, PathTemplate>> getAllFurtherInformation() {
    checkState(!spurious);

    return Collections.unmodifiableCollection(furtherInfo);
  }
}
