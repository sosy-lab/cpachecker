// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains utility classes for program slicing.
 *
 * @see org.sosy_lab.cpachecker.util.dependencegraph
 */
package org.sosy_lab.cpachecker.util.smg.join;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collector;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/** Class implementing join algorithm from FIT-TR-2013-4 (Appendix C) */
public class SMGJoinFields {

  private SMG smg1;
  private SMG smg2;

  private SMGJoinStatus status = SMGJoinStatus.EQUAL;

  public SMGJoinFields(SMG pSmg1, SMG pSmg2) {
    smg1 = pSmg1;
    smg2 = pSmg2;
  }

  /**
   * Implementation of Algorithm 3.
   *
   * @param obj1 - SMGObject of smg1
   * @param obj2 - SMGObject of smg2
   */
  public void joinFields(SMGObject obj1, SMGObject obj2) {

    Preconditions.checkArgument(
        obj1.getSize().equals(obj2.getSize()), "SMG fields with different sizes cannot be joined.");
    Preconditions.checkArgument(
        smg1.getObjects().contains(obj1) && smg2.getObjects().contains(obj2),
        "Only objects of givens SMGs can be joined.");
    // Step 2
    PersistentSet<SMGHasValueEdge> obj1Edges = processHasValueEdgeSet(obj1, obj2, smg1, smg2);
    PersistentSet<SMGHasValueEdge> obj2Edges = processHasValueEdgeSet(obj2, obj1, smg2, smg1);

    SMG newSMG1 = smg1.copyAndSetHVEdges(obj1Edges, obj1);
    SMG newSMG2 = smg2.copyAndSetHVEdges(obj2Edges, obj2);
    // Step 4
    updateStatus(smg1, newSMG1, SMGJoinStatus.LEFT_ENTAIL, obj1);
    updateStatus(smg2, newSMG2, SMGJoinStatus.RIGHT_ENTAIL, obj2);

    // Step 5
    FluentIterable<SMGHasValueEdge> addObj1Edges = mergeNonNullValues(newSMG2, newSMG1, obj2, obj1);
    FluentIterable<SMGHasValueEdge> addObj2Edges = mergeNonNullValues(newSMG1, newSMG2, obj1, obj2);

    for (SMGHasValueEdge edge : addObj1Edges) {
      newSMG1 = newSMG1.copyAndAddValue(edge.hasValue());
      newSMG1 = newSMG1.copyAndAddHVEdge(edge, obj1);
    }
    for (SMGHasValueEdge edge : addObj2Edges) {
      newSMG2 = newSMG2.copyAndAddValue(edge.hasValue());
      newSMG2 = newSMG2.copyAndAddHVEdge(edge, obj2);
    }

    smg1 = newSMG1;
    smg2 = newSMG2;
  }

  /**
   * Implementation of Algorithm 3 step 5.
   *
   * @param pSMG1 - the smg with defined edges
   * @param pSMG2 - the smg with undefined edges
   * @param pObject1 - the object with defined edges
   * @param pObject2 - the object with defined edges
   * @return extension edge set for pSMG2
   */
  @VisibleForTesting
  FluentIterable<SMGHasValueEdge> mergeNonNullValues(
      SMG pSMG1, SMG pSMG2, SMGObject pObject1, SMGObject pObject2) {

    PersistentSortedMap<BigInteger, SMGHasValueEdge> obj2OffsetToEdges =
        pSMG2.getEdges(pObject2).stream().collect(mapOffsetToEdgeCollector());

    return FluentIterable.from(pSMG1.getEdges(pObject1))
        .filter(
            edge -> {
              // filter overlapping edges
              return !edge.hasValue().isZero()
                  && !obj2OffsetToEdges
                      .subMap(edge.getOffset(), edge.getOffset().add(edge.getSizeInBits()))
                      .isEmpty();
            })
        .transform(
            // add fresh edges for offset and size tuples, which are defined in o1 and undefined
            // in o2
            edge ->
                new SMGHasValueEdge(
                    SMGValue.of(edge.hasValue().getNestingLevel()),
                    edge.getOffset(),
                    edge.getSizeInBits()));
  }

  /**
   * Implementation of Algorithm 3 step 4.
   *
   * @param oldSmg - the SMG before the join
   * @param newSmg - the SMG after the join
   * @param pNewStatus - Status update to applied maybe
   * @param object - the joined object
   */
  @VisibleForTesting
  void updateStatus(SMG oldSmg, SMG newSmg, SMGJoinStatus pNewStatus, SMGObject object) {
    // filter for null edges and map offsets on sizes
    PersistentSortedMap<BigInteger, BigInteger> oldEdgesWithZeroOffsetToSize =
        getNullEdgesMapOffsetToSize(object, oldSmg);
    PersistentSortedMap<BigInteger, BigInteger> newEdgesWithZeroOffsetToSize =
        getNullEdgesMapOffsetToSize(object, newSmg);

    boolean applyUpdate =
        oldEdgesWithZeroOffsetToSize.entrySet().stream()
            .anyMatch(
                entry -> {
                  // if newSize == null the offset was shortened
                  // if !newSize.equals(entry.getValue()) the length was shortened
                  return !newEdgesWithZeroOffsetToSize.containsKey(entry.getKey())
                      || !newEdgesWithZeroOffsetToSize.get(entry.getKey()).equals(entry.getValue());
                });
    if (applyUpdate) {
      status = status.updateWith(pNewStatus);
    }
  }

  /**
   * Implementation of Algorithm 3 step 2.
   *
   * @param obj1 - SMGObject of smg1
   * @param obj2 - SMGObject of smg2
   */
  @VisibleForTesting
  PersistentSet<SMGHasValueEdge> processHasValueEdgeSet(
      SMGObject obj1, SMGObject obj2, SMG pSmg1, SMG pSmg2) {
    // H1 and H2

    // 2a)
    FluentIterable<SMGHasValueEdge> edgesObj1Without0Address =
        pSmg1.getHasValueEdgesByPredicate(obj1, edge -> !edge.hasValue().isZero());

    // 2b)
    PersistentSortedMap<BigInteger, BigInteger> obj1EdgesWithZeroOffsetToSize =
        getNullEdgesMapOffsetToSize(obj1, pSmg1);
    PersistentSortedMap<BigInteger, BigInteger> map2 = getNullEdgesMapOffsetToSize(obj2, pSmg2);

    FluentIterable<SMGHasValueEdge> commonNullValueEdgeSet =
        FluentIterable.from(obj1EdgesWithZeroOffsetToSize.entrySet())
            .transformAndConcat(entry -> getNullEdgesIntersection(entry, map2));

    // 2c)
    FluentIterable<SMGHasValueEdge> obj2EdgesWithoutZero =
        pSmg2
            .getHasValueEdgesByPredicate(
                obj2,
                edge -> {
                  if (edge.hasValue().isZero()) {
                    return false;
                  }
                  // find offset wise matching edges
                  BigInteger floor = edge.getOffset();
                  BigInteger celing = edge.getOffset().add(edge.getSizeInBits());
                  Entry<BigInteger, BigInteger> entry =
                      obj1EdgesWithZeroOffsetToSize.floorEntry(floor);
                  return entry != null
                      && entry.getKey().add(entry.getValue()).compareTo(celing) >= 0;
                })
            .transform(
                edge ->
                    new SMGHasValueEdge(
                        SMGValue.zeroValue(), edge.getOffset(), edge.getSizeInBits()));

    return PersistentSet.copyOf(
        Iterables.concat(edgesObj1Without0Address, commonNullValueEdgeSet, obj2EdgesWithoutZero));
  }

  @VisibleForTesting
  FluentIterable<SMGHasValueEdge> getNullEdgesIntersection(
      Entry<BigInteger, BigInteger> pEntry, PersistentSortedMap<BigInteger, BigInteger> pMap) {
    return FluentIterable.from(
            pMap.subMap(pEntry.getKey(), true, pEntry.getKey().add(pEntry.getValue()), false)
                .entrySet())
        .transform(
            next -> {
              BigInteger resultOffset = pEntry.getKey().max(next.getKey());
              BigInteger resultSize =
                  pEntry.getKey().add(pEntry.getValue()).min(next.getKey().add(next.getValue()));
              return new SMGHasValueEdge(SMGValue.zeroValue(), resultOffset, resultSize);
            });
  }

  private PersistentSortedMap<BigInteger, BigInteger> getNullEdgesMapOffsetToSize(
      SMGObject pObj, SMG pSMG) {
    return pSMG.getEdges(pObj).stream()
        .filter(createEqNullValuePredicate(true))
        .collect(mapOffsetToSizeCollector());
  }

  private Predicate<SMGHasValueEdge> createEqNullValuePredicate(boolean equals) {
    return edge -> equals == edge.hasValue().isZero();
  }

  private Collector<SMGHasValueEdge, ?, PersistentSortedMap<BigInteger, BigInteger>>
      mapOffsetToSizeCollector() {
    return PathCopyingPersistentTreeMap.toPathCopyingPersistentTreeMap(
        SMGHasValueEdge::getOffset, SMGHasValueEdge::getSizeInBits);
  }

  private Collector<SMGHasValueEdge, ?, PersistentSortedMap<BigInteger, SMGHasValueEdge>>
      mapOffsetToEdgeCollector() {
    return PathCopyingPersistentTreeMap.toPathCopyingPersistentTreeMap(
        SMGHasValueEdge::getOffset, e -> e);
  }

  public SMG getSmg1() {
    return smg1;
  }

  public SMG getSmg2() {
    return smg2;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }
}
