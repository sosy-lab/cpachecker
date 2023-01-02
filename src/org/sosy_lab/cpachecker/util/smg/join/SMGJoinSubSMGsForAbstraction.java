// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGNode;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/** Class implementing join algorithm from FIT-TR-2013-4 (Appendix C.8) */
public class SMGJoinSubSMGsForAbstraction extends SMGAbstractJoin {

  private final PersistentSet<SMGObject> smgObjectsMappedinSMG1;
  private final PersistentSet<SMGObject> smgObjectsMappedinSMG2;
  private final PersistentSet<SMGValue> smgValuesMappedinSMG1;
  private final PersistentSet<SMGValue> smgValuesMappedinSMG2;

  // Implementation of Algorithm 11
  public SMGJoinSubSMGsForAbstraction(
      SMGJoinStatus initialStatus,
      SMG pSMG1,
      SMG pSMG2,
      SMG pDestSMG,
      SMGObject pObj1,
      SMGObject pObj2,
      BigInteger headOffset,
      BigInteger nextOffset,
      BigInteger prevOffset) {
    super(initialStatus, pSMG1, pSMG2, pDestSMG, new NodeMapping(), new NodeMapping());
    // step 1
    Optional<SMGHasValueEdge> nextEdgeObj1 =
        destSMG.getHasValueEdgeByPredicate(pObj1, e -> e.getOffset().equals(nextOffset));
    Optional<SMGHasValueEdge> prevEdgeObj1 =
        destSMG.getHasValueEdgeByPredicate(pObj1, e -> e.getOffset().equals(prevOffset));
    Optional<SMGHasValueEdge> nextEdgeObj2 =
        destSMG.getHasValueEdgeByPredicate(pObj2, e -> e.getOffset().equals(nextOffset));
    Optional<SMGHasValueEdge> prevEdgeObj2 =
        destSMG.getHasValueEdgeByPredicate(pObj2, e -> e.getOffset().equals(prevOffset));

    // step 2
    Map<Pair<SMGObject, SMGHasValueEdge>, SMGHasValueEdge> tempEdgeMapping =
        replaceEdgesByZeroEdges(
            pObj1, pObj2, prevEdgeObj1, nextEdgeObj1, nextEdgeObj2, prevEdgeObj2);
    // step 3
    SMGObject newObject = extendDestWithFreshDls(pObj1, pObj2, nextOffset, prevOffset, headOffset);
    // step 4
    int lDiff = pObj1.getClass().equals(pObj2.getClass()) ? 0 : isDLLS(pObj1) ? 1 : -1;

    // step 5
    mapping1.addMapping(pObj1, newObject);
    mapping2.addMapping(pObj2, newObject);
    SMGJoinSubSMGs smgJoinSubSMGs =
        new SMGJoinSubSMGs(
            SMGJoinStatus.EQUAL,
            inputSMG1,
            inputSMG2,
            destSMG,
            mapping1,
            mapping2,
            pObj1,
            pObj2,
            newObject,
            lDiff);
    // step 5 join failed
    if (!smgJoinSubSMGs.isDefined) {
      setBottomState();
      smgObjectsMappedinSMG1 = PersistentSet.of();
      smgObjectsMappedinSMG2 = PersistentSet.of();
      smgValuesMappedinSMG1 = PersistentSet.of();
      smgValuesMappedinSMG2 = PersistentSet.of();
      return;
    }
    // step 5 join successful
    copyJoinState(smgJoinSubSMGs);

    // step 6
    if (resultDLSHaveNewCycles()) {
      setBottomState();
      smgObjectsMappedinSMG1 = PersistentSet.of();
      smgObjectsMappedinSMG2 = PersistentSet.of();
      smgValuesMappedinSMG1 = PersistentSet.of();
      smgValuesMappedinSMG2 = PersistentSet.of();
      return;
    }
    // step 7
    if (isRegion(pObj1) && isRegion(pObj2)) {
      relabelPTEdges(newObject);
      increaseLevelOfMapped();
    }
    // step 8
    dropTempZeroEdges(tempEdgeMapping);
    // step 9 create result sets
    // TODO the old SMG implementation additionally excludes objects and values which are in both
    // mappings,this was not explicitly done in the paper
    Set<SMGObject> tmpObjectsMappedinSMG1 = new HashSet<>();
    Set<SMGObject> tmpObjectsMappedinSMG2 = new HashSet<>();

    Set<SMGValue> tmpSMGValuesMappedinSMG1 = new HashSet<>();
    Set<SMGValue> tmpSMGValuesMappedinSMG2 = new HashSet<>();
    fillWithIntersectingNodes(
        tmpObjectsMappedinSMG1, mapping1.getMappedObjects(), destSMG.getObjects());
    fillWithIntersectingNodes(
        tmpObjectsMappedinSMG2, mapping2.getMappedObjects(), destSMG.getObjects());
    fillWithIntersectingNodes(
        tmpSMGValuesMappedinSMG1, mapping1.getMappedValues(), destSMG.getValues());
    fillWithIntersectingNodes(
        tmpSMGValuesMappedinSMG2, mapping2.getMappedValues(), destSMG.getValues());
    smgObjectsMappedinSMG1 = PersistentSet.copyOf(tmpObjectsMappedinSMG1);
    smgObjectsMappedinSMG2 = PersistentSet.copyOf(tmpObjectsMappedinSMG2);
    smgValuesMappedinSMG1 = PersistentSet.copyOf(tmpSMGValuesMappedinSMG1);
    smgValuesMappedinSMG2 = PersistentSet.copyOf(tmpSMGValuesMappedinSMG2);
  }

  /**
   * Utility function for step 9. Fills a target collection with SMGNodes that are in both the
   * mapping and original node collection.
   *
   * @param <V> the generic SMGNode type
   * @param target collection
   * @param mapping collection containing created mappings
   * @param original collection containing created SMGNodes
   */
  private <V extends SMGNode> void fillWithIntersectingNodes(
      Collection<V> target, Collection<V> mapping, Collection<V> original) {
    for (V node : mapping) {
      if (original.contains(node)) {
        target.add(node);
      }
    }
  }

  /**
   * Utility function for step 8.
   *
   * @param pTempEdgeMapping the temporary created mapping
   */
  private void dropTempZeroEdges(
      Map<Pair<SMGObject, SMGHasValueEdge>, SMGHasValueEdge> pTempEdgeMapping) {
    pTempEdgeMapping
        .entrySet()
        .forEach(
            entry -> {
              destSMG =
                  destSMG.copyAndReplaceHVEdge(
                      entry.getKey().getFirst(), entry.getValue(), entry.getKey().getSecond());
            });
  }

  /** Utility function for step 7. Increase the level of all mapped nodes by 1. */
  private void increaseLevelOfMapped() {
    for (SMGObject mappedObj : mapping1.getMappedObjects()) {
      mapping1.replaceObjectMapping(
          mappedObj, mappedObj.withNestingLevelAndCopy(mappedObj.getNestingLevel() + 1));
    }
    for (SMGValue mappedVal : mapping1.getMappedValues()) {
      mapping1.replaceValueMapping(
          mappedVal, mappedVal.withNestingLevelAndCopy(mappedVal.getNestingLevel() + 1));
    }
    for (SMGObject mappedObj : mapping2.getMappedObjects()) {
      mapping2.replaceObjectMapping(
          mappedObj, mappedObj.withNestingLevelAndCopy(mappedObj.getNestingLevel() + 1));
    }
    for (SMGValue mappedVal : mapping2.getMappedValues()) {
      mapping2.replaceValueMapping(
          mappedVal, mappedVal.withNestingLevelAndCopy(mappedVal.getNestingLevel() + 1));
    }
  }

  /**
   * Utility function for step 7. Relabel the target specifier of the edges pointing to the newly
   * created object.
   *
   * @param pNewObject the newly created object
   */
  private void relabelPTEdges(SMGObject pNewObject) {
    destSMG
        .getPTEdgesByTarget(pNewObject)
        .forEach(
            ptEdge -> {
              // TODO this modifies nodes, maybe it would be better to replace existing with
              // modified copy
              ptEdge.setTargetSpecifier(SMGTargetSpecifier.IS_ALL_POINTER);
            });
  }

  /**
   * Utility function for step 5. Replace existing edges with new edges pointing to zero.
   *
   * @param pObj1 first object
   * @param pObj2 second object
   * @param pNextEdge1 optional of next edge pointing from pObj1 if there is such
   * @param pPrevEdge1 optional of prev edge pointing from pObj1 if there is such
   * @param pNextEdge2 optional of next edge pointing from pObj2 if there is such
   * @param pPrevEdge2 optional of prev edge pointing from pObj2 if there is such
   * @return mapping of existing edges and zeroed temp edges.
   */
  private Map<Pair<SMGObject, SMGHasValueEdge>, SMGHasValueEdge> replaceEdgesByZeroEdges(
      SMGObject pObj1,
      SMGObject pObj2,
      Optional<SMGHasValueEdge> pNextEdge1,
      Optional<SMGHasValueEdge> pPrevEdge1,
      Optional<SMGHasValueEdge> pNextEdge2,
      Optional<SMGHasValueEdge> pPrevEdge2) {
    Map<Pair<SMGObject, SMGHasValueEdge>, SMGHasValueEdge> retMap = new HashMap<>();
    insertTempZeroEdgeMapping(retMap, pObj1, pNextEdge1);
    insertTempZeroEdgeMapping(retMap, pObj1, pPrevEdge1);
    insertTempZeroEdgeMapping(retMap, pObj2, pNextEdge2);
    insertTempZeroEdgeMapping(retMap, pObj2, pPrevEdge2);
    return retMap;
  }

  /**
   * Utility function for replaceEdgesByZeroEdges. Copy given edge if present and replace target
   * with ZeroValue. Map the newly created edge to the old edge.
   *
   * @param mapping - where the edge mapping will be inserted
   * @param pObj - source object
   * @param edgeOptional - with the edge to be replaced if present do nothing else
   */
  private void insertTempZeroEdgeMapping(
      Map<Pair<SMGObject, SMGHasValueEdge>, SMGHasValueEdge> mapping,
      SMGObject pObj,
      Optional<SMGHasValueEdge> edgeOptional) {
    if (edgeOptional.isPresent()) {
      SMGHasValueEdge pOldEdge = edgeOptional.orElseThrow();
      SMGHasValueEdge newEdge =
          new SMGHasValueEdge(SMGValue.zeroValue(), pOldEdge.getOffset(), pOldEdge.getSizeInBits());
      destSMG = destSMG.copyAndReplaceHVEdge(pObj, pOldEdge, newEdge);
      mapping.put(Pair.of(pObj, pOldEdge), newEdge);
    }
  }

  /**
   * Utility function for Step 3. Create new join target dls and extend dest SMGS.
   *
   * @param pObj1 first join input
   * @param pObj2 second join input
   * @param pNextOffset next offset label
   * @param pPrevOffset previous offset label
   * @param pHeadOffset head offset label
   * @return the newly created and inserted dls
   */
  private SMGObject extendDestWithFreshDls(
      SMGObject pObj1,
      SMGObject pObj2,
      BigInteger pNextOffset,
      BigInteger pPrevOffset,
      BigInteger pHeadOffset) {

    int minLength = 0;
    // pObj1 or pObj2 might not be of type DLS
    if (isDLLS(pObj1)) {
      minLength += ((SMGDoublyLinkedListSegment) pObj1).getMinLength();
    }
    if (isDLLS(pObj2)) {
      minLength += ((SMGDoublyLinkedListSegment) pObj2).getMinLength();
    }

    SMGDoublyLinkedListSegment dls =
        new SMGDoublyLinkedListSegment(
            pObj1.getNestingLevel(),
            pObj1.getSize(),
            pObj1.getOffset(),
            pHeadOffset,
            pNextOffset,
            pPrevOffset,
            minLength);
    destSMG = destSMG.copyAndAddObject(dls);
    return dls;
  }

  public PersistentSet<SMGObject> getSmgObjectsMappedinSMG1() {
    return smgObjectsMappedinSMG1;
  }

  public PersistentSet<SMGObject> getSmgObjectsMappedinSMG2() {
    return smgObjectsMappedinSMG2;
  }

  public PersistentSet<SMGValue> getSmgValuesMappedinSMG1() {
    return smgValuesMappedinSMG1;
  }

  public PersistentSet<SMGValue> getSmgValuesMappedinSMG2() {
    return smgValuesMappedinSMG2;
  }
}
