// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.collect.Queues;
import java.util.Deque;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdge;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter.SMGEdgeHasValueFilterByObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;

final class SMGConsistencyVerifier {
  private SMGConsistencyVerifier() {} /* utility class */

  /**
   * Simply log a message about a result of a single check.
   *
   * @param pResult A result of the check
   * @param pLogger A logger instance to which the message will be sent
   * @param pMessage A message to log
   * @return True, if the check was successful (equivalent to { @link pResult }
   */
  private static boolean verifySMGProperty(boolean pResult, LogManager pLogger, String pMessage) {
    pLogger.log(Level.FINEST, "Checking SMG consistency: ", pMessage, ":", pResult);
    return pResult;
  }

  /**
   * A consistency checks related to the NULL object
   *
   * @param pLogger A logger to record results
   * @param pSmg A SMG to verify
   * @return True, if pSmg satisfies all consistency criteria
   */
  private static boolean verifyNullObject(LogManager pLogger, UnmodifiableSMG pSmg) {
    SMGValue null_value = null;

    // Find a null value in values
    for (SMGValue value : pSmg.getValues()) {
      if (pSmg.getObjectPointedBy(value) == SMGNullObject.INSTANCE) {
        null_value = value;
        break;
      }
    }

    // Verify that one value pointing to NULL object is present in values
    if (null_value == null) {
      pLogger.log(Level.SEVERE, "SMG inconsistent: no value pointing to null object");
      return false;
    }

    // Verify that NULL value returned by getNullValue() points to NULL object
    if (pSmg.getObjectPointedBy(SMGZeroValue.INSTANCE) != SMGNullObject.INSTANCE) {
      pLogger.log(Level.SEVERE, "SMG inconsistent: null value not pointing to null object");
      return false;
    }

    // Verify that the value found in values is the one returned by getNullValue()
    if (SMGZeroValue.INSTANCE != null_value) {
      pLogger.log(
          Level.SEVERE,
          "SMG inconsistent: null value in values set not returned by getNullValue()");
      return false;
    }

    // Verify that NULL object has no value
    SMGEdgeHasValueFilterByObject filter =
        SMGEdgeHasValueFilter.objectFilter(SMGNullObject.INSTANCE);

    if (!pSmg.getHVEdges(filter).isEmpty()) {
      pLogger.log(Level.SEVERE, "SMG inconsistent: null object has some value");
      return false;
    }

    // Verify that the NULL object is invalid
    if (pSmg.isObjectValid(SMGNullObject.INSTANCE)) {
      pLogger.log(Level.SEVERE, "SMG inconsistent: null object is not invalid");
      return false;
    }

    // Verify that the size of the NULL object is zero
    if (SMGNullObject.INSTANCE.getSize() != 0) {
      pLogger.log(Level.SEVERE, "SMG inconsistent: null object does not have zero size");
      return false;
    }

    return true;
  }

  /**
   * Verifies that invalid regions do not have any Has-Value edges, as this is forbidden in
   * consistent SMGs
   *
   * @param pLogger A logger to record results
   * @param pSmg A SMG to verify
   * @return True, if pSmg satisfies all consistency criteria.
   */
  private static boolean verifyInvalidRegionsHaveNoHVEdges(
      LogManager pLogger, UnmodifiableSMG pSmg) {
    for (SMGObject obj : pSmg.getObjects()) {
      if (pSmg.isObjectValid(obj) || pSmg.isObjectExternallyAllocated(obj)) {
        continue;
      }
      // Verify that the HasValue edge set for this invalid object is empty
      SMGEdgeHasValueFilterByObject filter = SMGEdgeHasValueFilter.objectFilter(obj);

      if (!pSmg.getHVEdges(filter).isEmpty()) {
        pLogger.log(Level.SEVERE, "SMG inconsistent: invalid object has a HVEdge");
        return false;
      }
    }

    return true;
  }

  /**
   * Verifies that any fields (as designated by Has-Value edges) do not exceed the boundary of the
   * object.
   *
   * @param pLogger A logger to record results
   * @param pObject An object to verify
   * @param pSmg A SMG to verify
   * @return True, if Object in pSmg satisfies all consistency criteria. False otherwise.
   */
  private static boolean checkSingleFieldConsistency(
      LogManager pLogger, SMGObject pObject, UnmodifiableSMG pSmg) {

    // For all fields in the object, verify that sizeof(type)+field_offset < object_size
    SMGEdgeHasValueFilterByObject filter = SMGEdgeHasValueFilter.objectFilter(pObject);

    for (SMGEdgeHasValue hvEdge : pSmg.getHVEdges(filter)) {
      if ((hvEdge.getOffset() + hvEdge.getSizeInBits()) > pObject.getSize()) {
        pLogger.log(Level.SEVERE, "SMG inconistent: field exceedes boundary of the object");
        pLogger.log(Level.SEVERE, "Object: ", pObject);
        pLogger.log(Level.SEVERE, "Field: ", hvEdge);
        return false;
      }
    }
    return true;
  }

  /**
   * Verify all objects satisfy the Field Consistency criteria
   *
   * @param pLogger A logger to record results
   * @param pSmg A SMG to verify
   * @return True, if pSmg satisfies all consistency criteria. False otherwise.
   */
  private static boolean verifyFieldConsistency(LogManager pLogger, UnmodifiableSMG pSmg) {
    for (SMGObject obj : pSmg.getObjects()) {
      if (!checkSingleFieldConsistency(pLogger, obj, pSmg)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Verify that the edges are consistent in the SMG
   *
   * @param pLogger A logger to record results
   * @param pSmg A SMG to verify
   * @param pEdges A set of edges for consistency verification
   * @return True, if all edges in pEdges satisfy consistency criteria. False otherwise.
   */
  private static boolean verifyEdgeConsistency(
      LogManager pLogger, UnmodifiableSMG pSmg, Iterable<? extends SMGEdge> pEdges) {
    Deque<SMGEdge> to_verify = Queues.newArrayDeque(pEdges);

    while (!to_verify.isEmpty()) {
      SMGEdge edge = to_verify.pop();

      // Verify that the object assigned to the edge exists in the SMG
      if (!pSmg.getObjects().contains(edge.getObject())) {
        pLogger.log(Level.SEVERE, "SMG inconsistent: Edge from a nonexistent object:", edge);
        return false;
      }

      // Verify that the value assigned to the edge exists in the SMG
      if (!pSmg.getValues().contains(edge.getValue())) {
        pLogger.log(Level.SEVERE, "SMG inconsistent: Edge to a nonexistent value:", edge);
        return false;
      }

      // Verify that the edge is consistent to all remaining edges
      //  - edges of different type are inconsistent
      //  - two Has-Value edges are inconsistent iff:
      //    - leading from the same object AND
      //    - have same type AND
      //    - have same offset AND
      //    - leading to DIFFERENT values
      //  - two Points-To edges are inconsistent iff:
      //    - different values point to same place (object, offset)
      //    - same values do not point to the same place
      for (SMGEdge other_edge : to_verify) {
        if (!edge.isConsistentWith(other_edge)) {
          pLogger.log(Level.SEVERE, "SMG inconsistent: inconsistent edges");
          pLogger.log(Level.SEVERE, "First edge:  ", edge);
          pLogger.log(Level.SEVERE, "Second edge: ", other_edge);
          return false;
        }
      }
    }
    return true;
  }

  private static boolean verifyObjectConsistency(LogManager pLogger, UnmodifiableSMG pSmg) {
    for (SMGObject obj : pSmg.getObjects()) {
      try {
        pSmg.isObjectValid(obj);
      } catch (IllegalArgumentException e) {
        pLogger.log(Level.SEVERE, "SMG inconsistent: object does not have validity");
        return false;
      }

      if (obj.getSize() < 0) {
        pLogger.log(Level.SEVERE, "SMG inconsistent: object with size lower than 0");
        return false;
      }
    }
    return true;
  }

  // TODO: NEQ CONSISTENCY

  /**
   * Verify a single SMG if it meets all consistency criteria.
   *
   * @param pLogger A logger to record results
   * @param pSmg A SMG to verify
   * @return True, if pSmg satisfies all consistency criteria
   */
  public static boolean verifySMG(LogManager pLogger, UnmodifiableSMG pSmg) {
    boolean toReturn = true;
    pLogger.log(Level.FINEST, "Starting constistency check of a SMG");

    toReturn =
        toReturn
            && verifySMGProperty(
                verifyNullObject(pLogger, pSmg), pLogger, "null object invariants hold");
    toReturn =
        toReturn
            && verifySMGProperty(
                verifyInvalidRegionsHaveNoHVEdges(pLogger, pSmg),
                pLogger,
                "invalid regions have no outgoing edges");
    toReturn =
        toReturn
            && verifySMGProperty(
                verifyFieldConsistency(pLogger, pSmg), pLogger, "field consistency");
    toReturn =
        toReturn
            && verifySMGProperty(
                verifyEdgeConsistency(pLogger, pSmg, pSmg.getHVEdges()),
                pLogger,
                "Has Value edge consistency");
    toReturn =
        toReturn
            && verifySMGProperty(
                verifyEdgeConsistency(pLogger, pSmg, pSmg.getPTEdges()),
                pLogger,
                "Points To edge consistency");
    toReturn =
        toReturn
            && verifySMGProperty(
                verifyObjectConsistency(pLogger, pSmg), pLogger, "Validity consistency");

    pLogger.log(Level.FINEST, "Ending consistency check of a SMG");

    return toReturn;
  }
}
