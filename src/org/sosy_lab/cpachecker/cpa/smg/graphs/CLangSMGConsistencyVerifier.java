// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;

public class CLangSMGConsistencyVerifier {
  private CLangSMGConsistencyVerifier() {} /* utility class */

  /**
   * Records a result of a single check to a logger along with a message
   *
   * @param pResult Result of the check
   * @param pLogger Logger to log the message
   * @param pMessage Message to be logged
   * @return The result of the check, i.e. equivalent to pResult
   */
  private static boolean verifyCLangSMGProperty(
      boolean pResult, LogManager pLogger, String pMessage) {
    pLogger.log(Level.FINEST, pMessage, ":", pResult);
    return pResult;
  }

  /**
   * Verifies that heap and global object sets are disjunct
   *
   * @param pLogger Logger to log the message
   * @param pSmg SMG to check
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  private static boolean verifyDisjunctHeapAndGlobal(
      LogManager pLogger, UnmodifiableCLangSMG pSmg) {
    Map<String, SMGRegion> globals = pSmg.getGlobalObjects();
    Set<SMGObject> heap = pSmg.getHeapObjects().asSet();

    boolean toReturn = Collections.disjoint(globals.values(), heap);

    if (!toReturn) {
      pLogger.log(Level.SEVERE, "CLangSMG inconsistent, heap and global objects are not disjoint");
    }

    return toReturn;
  }

  /**
   * Verifies that heap and stack object sets are disjunct
   *
   * @param pLogger Logger to log the message
   * @param pSmg SMG to check
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  private static boolean verifyDisjunctHeapAndStack(LogManager pLogger, UnmodifiableCLangSMG pSmg) {
    Set<SMGObject> stack = new HashSet<>();
    for (CLangStackFrame frame : pSmg.getStackFrames()) {
      stack.addAll(frame.getAllObjects());
    }
    Set<SMGObject> heap = pSmg.getHeapObjects().asSet();

    boolean toReturn = Collections.disjoint(stack, heap);

    if (!toReturn) {
      pLogger.log(
          Level.SEVERE,
          "CLangSMG inconsistent, heap and stack objects are not disjoint: "
              + Sets.intersection(stack, heap));
    }

    return toReturn;
  }

  /**
   * Verifies that global and stack object sets are disjunct
   *
   * @param pLogger Logger to log the message
   * @param pSmg SMG to check
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  private static boolean verifyDisjunctGlobalAndStack(
      LogManager pLogger, UnmodifiableCLangSMG pSmg) {
    Set<SMGObject> stack = new HashSet<>();
    for (CLangStackFrame frame : pSmg.getStackFrames()) {
      stack.addAll(frame.getAllObjects());
    }
    Map<String, SMGRegion> globals = pSmg.getGlobalObjects();

    boolean toReturn = Collections.disjoint(stack, globals.values());

    if (!toReturn) {
      pLogger.log(Level.SEVERE, "CLangSMG inconsistent, global and stack objects are not disjoint");
    }

    return toReturn;
  }

  /**
   * Verifies that heap, global and stack union is equal to the set of all valid objects
   *
   * @param pLogger Logger to log the message
   * @param pSmg SMG to check
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  private static boolean verifyStackGlobalHeapUnion(LogManager pLogger, UnmodifiableCLangSMG pSmg) {
    Set<SMGObject> object_union = new HashSet<>();

    Iterables.addAll(object_union, pSmg.getHeapObjects());
    object_union.addAll(pSmg.getGlobalObjects().values());

    for (CLangStackFrame frame : pSmg.getStackFrames()) {
      object_union.addAll(frame.getAllObjects());
    }

    if (!object_union.containsAll(pSmg.getValidObjects())) {
      pLogger.log(
          Level.SEVERE,
          "CLangSMG inconsistent: union of stack, heap and global object "
              + "contains less objects than the set of SMG objects. Missing object:",
          Sets.difference(pSmg.getValidObjects(), object_union));
      return false;
    }

    // TODO: Require workaround for free heap objects
    if (!pSmg.getValidObjects()
        .addAndCopy(SMGNullObject.INSTANCE)
        .containsAll(pSmg.getGlobalObjects().values())) {
      pLogger.log(
          Level.SEVERE,
          "CLangSMG inconsistent: union of stack, heap and global object "
              + "contains more objects than the set of SMG objects. Additional object:",
          Sets.difference(object_union, pSmg.getObjects().asSet()));
      return false;
    }

    return true;
  }

  /**
   * Verifies several NULL object-related properties
   *
   * @param pLogger Logger to log the message
   * @param pSmg SMG to check
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  private static boolean verifyNullObjectCLangProperties(
      LogManager pLogger, UnmodifiableCLangSMG pSmg) {
    // Verify that there is no NULL object in global scope
    for (SMGObject obj : pSmg.getGlobalObjects().values()) {
      if (obj == SMGNullObject.INSTANCE) {
        pLogger.log(
            Level.SEVERE, "CLangSMG inconsistent: null object in global object set [" + obj + "]");
        return false;
      }
    }

    // Verify there is no more than one NULL object in the heap object set
    SMGObject firstNull = null;
    for (SMGObject obj : pSmg.getHeapObjects()) {
      if (obj == SMGNullObject.INSTANCE) {
        if (firstNull != null) {
          pLogger.log(
              Level.SEVERE,
              "CLangSMG inconsistent: second null object in heap object set [first="
                  + firstNull
                  + ", second="
                  + obj
                  + "]");
          return false;
        } else {
          firstNull = obj;
        }
      }
    }

    // Verify there is no NULL object in the stack object set
    for (CLangStackFrame frame : pSmg.getStackFrames()) {
      for (SMGObject obj : frame.getAllObjects()) {
        if (obj == SMGNullObject.INSTANCE) {
          pLogger.log(
              Level.SEVERE, "CLangSMG inconsistent: null object in stack object set [" + obj + "]");
          return false;
        }
      }
    }

    // Verify there is at least one NULL object
    if (firstNull == null) {
      pLogger.log(Level.SEVERE, "CLangSMG inconsistent: no null object");
      return false;
    }

    return true;
  }

  /**
   * Verify the global scope is consistent: each record points to an appropriately labeled object
   *
   * @param pLogger Logger to log the message
   * @param pSmg SMG to check
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  private static boolean verifyGlobalNamespace(LogManager pLogger, UnmodifiableCLangSMG pSmg) {
    Map<String, SMGRegion> globals = pSmg.getGlobalObjects();

    for (String label : pSmg.getGlobalObjects().keySet()) {
      String globalLabel = globals.get(label).getLabel();
      if (!globalLabel.equals(label)) {
        pLogger.logf(
            Level.SEVERE,
            "CLangSMG inconsistent: label [%s] points to an object with label [%s]",
            label,
            pSmg.getGlobalObjects().get(label).getLabel());
        return false;
      }
    }

    return true;
  }

  /**
   * Verify the stack name space: each record points to an appropriately labeled object
   *
   * @param pLogger Logger to log the message
   * @param pSmg the current smg
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  private static boolean verifyStackNamespaces(LogManager pLogger, UnmodifiableCLangSMG pSmg) {
    Set<SMGObject> stack_objects = new HashSet<>();

    for (CLangStackFrame frame : pSmg.getStackFrames()) {
      for (SMGObject object : frame.getAllObjects()) {
        if (stack_objects.contains(object)) {
          pLogger.log(
              Level.SEVERE,
              "CLangSMG inconsistent: object [" + object + "] present multiple times in the stack");
          return false;
        }
        stack_objects.add(object);
      }
    }

    return true;
  }

  /**
   * Verify pointers: each pointer should have correct size
   *
   * @param pLogger Logger to log the message
   * @param pSmg the current smg
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  private static boolean verifyPointersSize(LogManager pLogger, UnmodifiableCLangSMG pSmg) {

    for (SMGEdgePointsTo ptEdge : pSmg.getPTEdges()) {
      if (!ptEdge.getValue().isZero()) {
        SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.valueFilter(ptEdge.getValue());
        for (SMGEdgeHasValue hvEdge : pSmg.getHVEdges(filter)) {
          if (hvEdge.getSizeInBits() != pSmg.getSizeofPtrInBits()) {
            pLogger.log(
                Level.SEVERE,
                "CLangSMG inconsistent: pointer ["
                    + ptEdge
                    + "] is stored with wrong size by hvEdge "
                    + hvEdge);
            return false;
          }
        }
      }
    }

    return true;
  }

  /**
   * Verify all the consistency properties related to CLangSMG
   *
   * @param pLogger Logger to log results
   * @param pSmg SMG to check
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  public static boolean verifyCLangSMG(LogManager pLogger, UnmodifiableCLangSMG pSmg) {
    boolean toReturn = SMGConsistencyVerifier.verifySMG(pLogger, pSmg);

    pLogger.log(Level.FINEST, "Starting constistency check of a CLangSMG");

    toReturn =
        toReturn
            && verifyCLangSMGProperty(
                verifyDisjunctHeapAndGlobal(pLogger, pSmg),
                pLogger,
                "Checking CLangSMG consistency: heap and global object sets are disjunt");
    toReturn =
        toReturn
            && verifyCLangSMGProperty(
                verifyDisjunctHeapAndStack(pLogger, pSmg),
                pLogger,
                "Checking CLangSMG consistency: heap and stack objects are disjunct");
    toReturn =
        toReturn
            && verifyCLangSMGProperty(
                verifyDisjunctGlobalAndStack(pLogger, pSmg),
                pLogger,
                "Checking CLangSMG consistency: global and stack objects are disjunct");
    toReturn =
        toReturn
            && verifyCLangSMGProperty(
                verifyStackGlobalHeapUnion(pLogger, pSmg),
                pLogger,
                "Checking CLangSMG consistency: global, stack and heap object union contains all"
                    + " objects in SMG");
    toReturn =
        toReturn
            && verifyCLangSMGProperty(
                verifyNullObjectCLangProperties(pLogger, pSmg),
                pLogger,
                "Checking CLangSMG consistency: null object invariants hold");
    toReturn =
        toReturn
            && verifyCLangSMGProperty(
                verifyGlobalNamespace(pLogger, pSmg),
                pLogger,
                "Checking CLangSMG consistency: global namespace problem");
    toReturn =
        toReturn
            && verifyCLangSMGProperty(
                verifyStackNamespaces(pLogger, pSmg),
                pLogger,
                "Checking CLangSMG consistency: stack namespace");
    toReturn =
        toReturn
            && verifyCLangSMGProperty(
                verifyPointersSize(pLogger, pSmg),
                pLogger,
                "Checking CLangSMG consistency: pointer size");

    pLogger.log(Level.FINEST, "Ending consistency check of a CLangSMG");

    return toReturn;
  }
}
