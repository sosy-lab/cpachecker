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
package org.sosy_lab.cpachecker.util.invariants.balancer.prh12;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionSet;


public class ColumnChoiceFrame {

  private Map<PivotRow2, UsableColumn> choices;
  private List<PivotRow2> rows;
  private int ptr;
  private int limit;
  private List<ChallengeType> ctypes;

  public ColumnChoiceFrame(List<PivotRow2> r, ChallengeType c) {
    List<ChallengeType> ct = new Vector<>(r.size());
    for (int i = 0; i < r.size(); i++) {
      ct.add(c);
    }
    construct(r, ct);
  }

  public ColumnChoiceFrame(List<PivotRow2> r, List<ChallengeType> c) {
    construct(r, c);
  }

  private void construct(List<PivotRow2> r, List<ChallengeType> c) {
    // r and c must have the same size!
    assert (r.size() == c.size());
    rows = r;
    ctypes = c;
    choices = new HashMap<>();
    if (r.size() > 0) {
      PivotRow2 pr = r.get(0);
      ChallengeType ct = c.get(0);
      switch (ct) {
      case AUGCOLUMN:
        limit = pr.getTotalOptionCount();
        break;
      case FREECOLUMN:
        limit = pr.getFreeColOptionCount();
      }
    }
    ptr = 0;
  }


  public enum ChallengeType {
    FREECOLUMN,
    AUGCOLUMN;
  }

  boolean isComplete() {
    return rows.size() == 0;
  }

  /*
   * Say whether this frame has a next choice, or not.
   */
  boolean hasNext() {
    return ptr < limit;
  }

  private void setChoices(Map<PivotRow2, UsableColumn> cm) {
    choices = cm;
  }

  /*
   * Make all the column requests held in the choices map.
   */
  private void makeRequests() {
    for (PivotRow2 pr : choices.keySet()) {
      UsableColumn uc = choices.get(pr);
      uc.makeRequest(pr);
    }
  }

  AssumptionSet getAssumptionSet() {
    makeRequests();
    AssumptionSet aset = new AssumptionSet();
    for (UsableColumn uc : choices.values()) {
      // Get assumptions from this column.
      AssumptionSet a = uc.getRequestedAssumptions();
      //Can we get a logger?
      //logger.log(Level.ALL,"Column",u.getColNum(),"produced assumption set",a);
      aset.addAll(a);
      // Clear the requests to this column.
      // This is important in case we need to backtrack to an earlier choice frame.
      uc.clearRequests();
    }
    return aset;
  }

  /*
   * If this frame is incomplete, then make the next choice, advance pointers accordingly,
   * and return a new frame. Otherwise return null.
   */
  ColumnChoiceFrame next() {
    // If already complete, or incomplete but out of choices, then return null.
    if (isComplete() || !hasNext()) {
      return null;
    }
    // Otherwise, get the next column choice.
    PivotRow2 pr = rows.get(0);
    UsableColumn uc;
    if (ctypes.get(0) == ChallengeType.AUGCOLUMN) {
      uc = pr.getGeneralOption(ptr);
    } else {
      uc = pr.getFreeOption(ptr);
    }
    // Add it to the choices map for the next frame.
    Map<PivotRow2, UsableColumn> nextChoices = new HashMap<>(choices);
    nextChoices.put(pr, uc);
    // Prepare the row list for the next frame.
    // Start by simply copying the present row list into a deque...
    LinkedList<PivotRow2> nextRows = new LinkedList<>(rows);
    // ...and deleting the first row.
    nextRows.removeFirst();
    // Now do likewise with the list of challenge types.
    LinkedList<ChallengeType> nextCtypes = new LinkedList<>(ctypes);
    nextCtypes.removeFirst();
    // Have we seen this column before?
    if (!choices.values().contains(uc)) {
      // If not, then we need to see which rows it challenges, and add these to the front of the queue,
      // deleting any duplicate occurrences first.
      List<PivotRow2> challenged = uc.query();
      // But first we discard those rows in 'challenged' which are already in
      // choices.keySet(), EXCEPT that among such rows, if the choice they have already made
      // is the aug col, then they DO need to be rechallenged, since it is now a pivot challenge.
      List<PivotRow2> discard = new Vector<>();
      for (PivotRow2 p : challenged) {
        if (choices.keySet().contains(p)) {
          UsableColumn u = choices.get(p);
          if (!u.isAugCol()) {
            // In this case p has already made a choice, and has chosen a nonaug col, so it
            // does not need to be challenged again. Therefore we will remove it from the
            // list 'challenged'.
            discard.add(p);
          }
        }
      }
      challenged.removeAll(discard);
      // Delete duplicates in nextRows, and delete corresponding ctypes at the same time.
      for (PivotRow2 p : challenged) {
        if (nextRows.contains(p)) {
          int i = nextRows.indexOf(p);
          nextRows.remove(i);
          nextCtypes.remove(i);
        }
      }
      // Now add the challenged rows at the front of the queue.
      nextRows.addAll(0, challenged);
      // And add as many free column challenge types to the front of the ctypes queue.
      nextCtypes.addAll(0, Collections.nCopies(challenged.size(), ChallengeType.FREECOLUMN));
    }
    // Now construct a new frame using the row and ctype lists constructed.
    ColumnChoiceFrame nextCCF = new ColumnChoiceFrame(nextRows, nextCtypes);
    // Set its choice map.
    nextCCF.setChoices(nextChoices);
    // Finally, advance the present frame's pointer.
    ptr++;
    // And return the new frame.
    return nextCCF;
  }

}
