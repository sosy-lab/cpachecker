// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter.SMGEdgeHasValueFilterByObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

/**
 * This class implements a faster way to test, if one smg is less or equal to another. Simply
 * joining two smg and requesting its status takes too long.
 */
@SuppressWarnings("deprecation") // remove ThreadSafeTimerContainer
public class SMGIsLessOrEqual {

  // FIXME static state is bad and produces wrong statistics!
  public static final ThreadSafeTimerContainer isLEQTimer =
      new ThreadSafeTimerContainer("Time for joining SMGs");
  public static final ThreadSafeTimerContainer globalsTimer =
      new ThreadSafeTimerContainer("Time for joining globals");
  public static final ThreadSafeTimerContainer stackTimer =
      new ThreadSafeTimerContainer("Time for joining stacks");
  public static final ThreadSafeTimerContainer heapTimer =
      new ThreadSafeTimerContainer("Time for joining heaps");

  private SMGIsLessOrEqual() {} // Utility class.

  /**
   * Checks, if smg2 is less or equal to smg1.
   *
   * @return true, iff smg1 is less or equal to smg2, false otherwise.
   */
  public static boolean isLessOrEqual(UnmodifiableCLangSMG pSMG1, UnmodifiableCLangSMG pSMG2) {

    TimerWrapper timer = isLEQTimer.getNewTimer();
    timer.start();
    try {

      // if smg1 is smg2, smg1 is equal to smg2
      if (pSMG1 == pSMG2) {
        return true;
      }

      // if smg1 has not allocated the same number of SMGObjects in the heap, it is not equal to
      // smg2
      if (pSMG1.getHeapObjects().size() != pSMG2.getHeapObjects().size()) {
        return false;
      }

      if (pSMG1.getStackFrames().size() != pSMG2.getStackFrames().size()) {
        return false;
      }

      TimerWrapper gt = globalsTimer.getNewTimer();
      gt.start();
      try {
        if (!maybeGlobalsLessOrEqual(pSMG1, pSMG2)) {
          return false;
        }
      } finally {
        gt.stop();
      }

      TimerWrapper st = stackTimer.getNewTimer();
      st.start();
      try {
        if (!maybeStackLessOrEqual(pSMG1, pSMG2)) {
          return false;
        }
      } finally {
        st.stop();
      }

      TimerWrapper ht = heapTimer.getNewTimer();
      ht.start();
      try {
        if (!maybeHeapLessOrEqual(pSMG1, pSMG2)) {
          return false;
        }
      } finally {
        ht.stop();
      }

      return true;

    } finally {
      timer.stop();
    }
  }

  /** returns whether globals variables are "maybe LEQ" or "definitely not LEQ". */
  private static boolean maybeGlobalsLessOrEqual(
      UnmodifiableCLangSMG pSMG1, UnmodifiableCLangSMG pSMG2) {
    Map<String, SMGRegion> globals_in_smg1 = pSMG1.getGlobalObjects();
    Map<String, SMGRegion> globals_in_smg2 = pSMG2.getGlobalObjects();

    // technically, one should look if any SMGHVE exist in additional region in SMG1
    if (globals_in_smg1.size() > globals_in_smg2.size()) {
      return false;
    }

    // Check, whether global variables of smg1 is less or equal to smg2
    for (Entry<String, SMGRegion> entry : globals_in_smg1.entrySet()) {
      SMGObject globalInSMG1 = entry.getValue();
      String globalVar = entry.getKey();

      // technically, one should look if any SMGHVE exist in additional region in SMG1
      if (!globals_in_smg2.containsKey(globalVar)) {
        return false;
      }

      SMGObject globalInSMG2 = globals_in_smg2.get(entry.getKey());
      if (!isLessOrEqualFields(pSMG1, pSMG2, globalInSMG1, globalInSMG2)) {
        return false;
      }
    }

    return true;
  }

  /** returns whether variables on the stack are "maybe LEQ" or "definitely not LEQ". */
  private static boolean maybeStackLessOrEqual(
      UnmodifiableCLangSMG pSMG1, UnmodifiableCLangSMG pSMG2) {
    Iterator<CLangStackFrame> smg1stackIterator = pSMG1.getStackFrames().iterator();
    Iterator<CLangStackFrame> smg2stackIterator = pSMG2.getStackFrames().iterator();

    // Check, whether the stack frames of smg1 are less or equal to smg 2
    while (smg1stackIterator.hasNext() && smg2stackIterator.hasNext()) {
      CLangStackFrame frameInSMG1 = smg1stackIterator.next();
      CLangStackFrame frameInSMG2 = smg2stackIterator.next();

      // check, whether it is the same stack
      if (!frameInSMG1
          .getFunctionDeclaration()
          .getOrigName()
          .equals(frameInSMG2.getFunctionDeclaration().getOrigName())) {
        return false;
      }

      // technically, one should look if any SMGHVE exist in additional region in SMG1
      if (frameInSMG1.getAllObjects().size() > frameInSMG2.getAllObjects().size()) {
        return false;
      }

      // check, whether they have different return values if present
      if (!(frameInSMG1.getFunctionDeclaration().getType().getReturnType().getCanonicalType()
              instanceof CVoidType)
          && !isLessOrEqualFields(
              pSMG1, pSMG2, frameInSMG1.getReturnObject(), frameInSMG2.getReturnObject())) {
        return false;
      }

      for (String localVar : frameInSMG1.getVariables().keySet()) {

        // technically, one should look if any SMGHVE exist in additional region in SMG1
        if (!frameInSMG2.containsVariable(localVar)) {
          return false;
        }

        SMGRegion localInSMG1 = frameInSMG1.getVariable(localVar);
        SMGRegion localInSMG2 = frameInSMG2.getVariable(localVar);

        if (!isLessOrEqualFields(pSMG1, pSMG2, localInSMG1, localInSMG2)) {
          return false;
        }
      }
    }

    return true;
  }

  /** returns whether two heaps are "maybe LEQ" or "definitely not LEQ". */
  private static boolean maybeHeapLessOrEqual(
      UnmodifiableCLangSMG pSMG1, UnmodifiableCLangSMG pSMG2) {
    PersistentSet<SMGObject> heap_in_smg1 = pSMG1.getHeapObjects();
    PersistentSet<SMGObject> heap_in_smg2 = pSMG2.getHeapObjects();

    for (SMGObject object_in_smg1 : heap_in_smg1) {

      // technically, one should look if any SMGHVE exist in additional region in SMG1
      if (!heap_in_smg2.contains(object_in_smg1)) {
        return false;
      }

      // FIXME SMG Objects in heap have to be the same object to be comparable
      if (!isLessOrEqualFields(pSMG1, pSMG2, object_in_smg1, object_in_smg1)) {
        return false;
      }

      if (pSMG1.isObjectValid(object_in_smg1) != pSMG2.isObjectValid(object_in_smg1)) {
        return false;
      }
    }

    return true;
  }

  /** check whether an object is LEQ than another object. */
  private static boolean isLessOrEqualFields(
      UnmodifiableCLangSMG pSMG1,
      UnmodifiableCLangSMG pSMG2,
      SMGObject pSMGObject1,
      SMGObject pSMGObject2) {

    checkArgument(
        pSMGObject1.getSize() == pSMGObject2.getSize(),
        "SMGJoinFields object arguments need to have identical size");

    checkArgument(
        (pSMG1.getObjects().contains(pSMGObject1) && pSMG2.getObjects().contains(pSMGObject2)),
        "SMGJoinFields object arguments need to be included in parameter SMGs");

    SMGEdgeHasValueFilterByObject filterForSMG1 = SMGEdgeHasValueFilter.objectFilter(pSMGObject1);
    SMGEdgeHasValueFilterByObject filterForSMG2 = SMGEdgeHasValueFilter.objectFilter(pSMGObject2);

    SMGHasValueEdges HVE2 = pSMG2.getHVEdges(filterForSMG2);

    // TODO Merge Zero.
    for (SMGEdgeHasValue edge1 : pSMG1.getHVEdges(filterForSMG1)) {
      if (!HVE2.filter(
              filterForSMG2
                  .filterAtOffset(edge1.getOffset())
                  .filterBySize(edge1.getSizeInBits())
                  .filterHavingValue(edge1.getValue()))
          .iterator()
          .hasNext()) {
        return false;
      }

      SMGValue value = edge1.getValue();

      if (pSMG1.isPointer(value)) {
        if (!pSMG2.isPointer(value)) {
          return false;
        }

        SMGEdgePointsTo ptE1 = pSMG1.getPointer(value);
        SMGEdgePointsTo ptE2 = pSMG2.getPointer(value);
        String label1 = ptE1.getObject().getLabel();
        String label2 = ptE2.getObject().getLabel();
        long offset1 = ptE1.getOffset();
        long offset2 = ptE2.getOffset();

        // TODO How does one check, if two pointers point to the same region? You would have to
        // recover the stack frame.
        if (!(offset1 == offset2 && label1.equals(label2))) {
          return false;
        }
      }
    }

    return true;
  }
}
