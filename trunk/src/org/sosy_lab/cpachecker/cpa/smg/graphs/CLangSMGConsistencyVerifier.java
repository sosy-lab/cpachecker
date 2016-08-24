/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import com.google.common.collect.Sets;

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
  static private boolean verifyCLangSMGProperty(boolean pResult, LogManager pLogger, String pMessage) {
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
  static private boolean verifyDisjunctHeapAndGlobal(LogManager pLogger, CLangSMG pSmg) {
    Map<String, SMGRegion> globals = pSmg.getGlobalObjects();
    Set<SMGObject> heap = pSmg.getHeapObjects();

    boolean toReturn = Collections.disjoint(globals.values(), heap);

    if (! toReturn) {
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
  static private boolean verifyDisjunctHeapAndStack(LogManager pLogger, CLangSMG pSmg) {
    Deque<CLangStackFrame> stack_frames = pSmg.getStackFrames();
    Set<SMGObject> stack = new HashSet<>();

    for (CLangStackFrame frame: stack_frames) {
      stack.addAll(frame.getAllObjects());
    }
    Set<SMGObject> heap = pSmg.getHeapObjects();

    boolean toReturn = Collections.disjoint(stack, heap);

    if (! toReturn) {
      pLogger.log(Level.SEVERE, "CLangSMG inconsistent, heap and stack objects are not disjoint: " + Sets.intersection(stack, heap));
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
  static private boolean verifyDisjunctGlobalAndStack(LogManager pLogger, CLangSMG pSmg) {
    Deque<CLangStackFrame> stack_frames = pSmg.getStackFrames();
    Set<SMGObject> stack = new HashSet<>();

    for (CLangStackFrame frame: stack_frames) {
      stack.addAll(frame.getAllObjects());
    }
    Map<String, SMGRegion> globals = pSmg.getGlobalObjects();

    boolean toReturn = Collections.disjoint(stack, globals.values());

    if (! toReturn) {
      pLogger.log(Level.SEVERE, "CLangSMG inconsistent, global and stack objects are not disjoint");
    }

    return toReturn;
  }

  /**
   * Verifies that heap, global and stack union is equal to the set of all objects
   *
   * @param pLogger Logger to log the message
   * @param pSmg SMG to check
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  static private boolean verifyStackGlobalHeapUnion(LogManager pLogger, CLangSMG pSmg) {
    HashSet<SMGObject> object_union = new HashSet<>();

    object_union.addAll(pSmg.getHeapObjects());
    object_union.addAll(pSmg.getGlobalObjects().values());

    for (CLangStackFrame frame : pSmg.getStackFrames()) {
      object_union.addAll(frame.getAllObjects());
    }

    boolean toReturn = object_union.containsAll(pSmg.getObjects()) &&
                       pSmg.getObjects().containsAll(object_union);

    if (! toReturn) {
      pLogger.log(Level.SEVERE, "CLangSMG inconsistent: union of stack, heap and global object is not the same set as the set of SMG objects");
    }

    return toReturn;
  }

  /**
   * Verifies several NULL object-related properties
   * @param pLogger Logger to log the message
   * @param pSmg SMG to check
   *
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  static private boolean verifyNullObjectCLangProperties(LogManager pLogger, CLangSMG pSmg) {
    // Verify that there is no NULL object in global scope
    for (SMGObject obj: pSmg.getGlobalObjects().values()) {
      if (! obj.notNull()) {
        pLogger.log(Level.SEVERE, "CLangSMG inconsistent: null object in global object set [" + obj + "]");
        return false;
      }
    }

    // Verify there is no more than one NULL object in the heap object set
    SMGObject firstNull = null;
    for (SMGObject obj: pSmg.getHeapObjects()) {
      if (! obj.notNull()) {
        if (firstNull != null) {
          pLogger.log(Level.SEVERE, "CLangSMG inconsistent: second null object in heap object set [first=" + firstNull + ", second=" + obj +"]" );
          return false;
        } else {
          firstNull = obj;
        }
      }
    }

    // Verify there is no NULL object in the stack object set
    for (CLangStackFrame frame: pSmg.getStackFrames()) {
      for (SMGObject obj: frame.getAllObjects()) {
        if (! obj.notNull()) {
          pLogger.log(Level.SEVERE, "CLangSMG inconsistent: null object in stack object set [" + obj + "]");
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
   * Verify the global scope is consistent: each record points to an
   * appropriately labeled object
   *
   * @param pLogger Logger to log the message
   * @param pSmg SMG to check
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  static private boolean verifyGlobalNamespace(LogManager pLogger, CLangSMG pSmg) {
    Map<String, SMGRegion> globals = pSmg.getGlobalObjects();

    for (String label: pSmg.getGlobalObjects().keySet()) {
      String globalLabel = globals.get(label).getLabel();
      if (! globalLabel.equals(label)) {
        pLogger.log(Level.SEVERE,  "CLangSMG inconsistent: label [" + label + "] points to an object with label [" + pSmg.getGlobalObjects().get(label).getLabel() + "]");
        return false;
      }
    }

    return true;
  }

  /**
   * Verify the stack name space: each record points to an appropriately
   * labeled object
   *
   * @param pLogger Logger to log the message
   * @param pSmg the current smg
   * @return True if pSmg is consistent w.r.t. this criteria. False otherwise.
   */
  static private boolean verifyStackNamespaces(LogManager pLogger, CLangSMG pSmg) {
    HashSet<SMGObject> stack_objects = new HashSet<>();

    for (CLangStackFrame frame : pSmg.getStackFrames()) {
      for (SMGObject object : frame.getAllObjects()) {
        if (stack_objects.contains(object)) {
          pLogger.log(Level.SEVERE, "CLangSMG inconsistent: object [" + object + "] present multiple times in the stack");
          return false;
        }
        stack_objects.add(object);
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
  static public boolean verifyCLangSMG(LogManager pLogger, CLangSMG pSmg) {
    boolean toReturn = SMGConsistencyVerifier.verifySMG(pLogger, pSmg);

    pLogger.log(Level.FINEST, "Starting constistency check of a CLangSMG");

    toReturn = toReturn && verifyCLangSMGProperty(
        verifyDisjunctHeapAndGlobal(pLogger, pSmg),
        pLogger,
        "Checking CLangSMG consistency: heap and global object sets are disjunt");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyDisjunctHeapAndStack(pLogger, pSmg),
        pLogger,
        "Checking CLangSMG consistency: heap and stack objects are disjunct");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyDisjunctGlobalAndStack(pLogger, pSmg),
        pLogger,
        "Checking CLangSMG consistency: global and stack objects are disjunct");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyStackGlobalHeapUnion(pLogger, pSmg),
        pLogger,
        "Checking CLangSMG consistency: global, stack and heap object union contains all objects in SMG");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyNullObjectCLangProperties(pLogger, pSmg),
        pLogger,
        "Checking CLangSMG consistency: null object invariants hold");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyGlobalNamespace(pLogger, pSmg),
        pLogger,
        "Checking CLangSMG consistency: global namespace problem");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyStackNamespaces(pLogger, pSmg),
        pLogger,
        "Checking CLangSMG consistency: stack namespace");

    pLogger.log(Level.FINEST, "Ending consistency check of a CLangSMG");

    return toReturn;
  }
}