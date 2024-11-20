// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.abstraction;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAStatistics;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState.EqualityCache;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGCPAAbstractionManager {

  private final SMGState state;

  private EqualityCache<Value> equalityCache;

  private EqualityCache<SMGObject> objectCache;

  private final int minimumLengthForListsForAbstraction;

  private final SMGCPAStatistics statistics;

  private enum ListType {
    SLL,
    LOOPINGSLL,
    DLL,
    LOOPINGDLL,
    NONE,
    NONE_NOT_SLL
  }

  public SMGCPAAbstractionManager(
      SMGState pState, int pMinimumLengthForListsForAbstraction, SMGCPAStatistics pStatistics) {
    state = pState;
    minimumLengthForListsForAbstraction = pMinimumLengthForListsForAbstraction;
    statistics = pStatistics;
    // Set caches for tests
    equalityCache = EqualityCache.of();
    objectCache = EqualityCache.of();
  }

  /*
   * Info on how lists are abstracted: a sequence of memory regions linked by at least 1 pointer
   * (pfo) may be detected as a list to abstract if they are equal in shape and values
   * (besides the pointers forward and backwards). An exception to the equal values are pointers,
   * as those need to point to memory with the same shape and values again
   * (for example nested lists).
   * When 2 memory regions are merged, we create a new singly or doubly linked list segment with
   * the number of concrete list segments as the minimum length
   * (e.g. 2 concrete regions form a 2+, but a 2+ and a concrete region form a 3+).
   * Pointers pointing to the original segments (everywhere in the SMG) now point
   * to the new SLS or DLS, but remember their original nesting level in the list.
   * They also retain special behavior for the first and last segments through their pointer specifier.
   * If there was a nested list, those are also merged. First a single nested list is abstracted
   * (for example into an X+ SLL).
   * Then, when the upper list is merged, the nested abstracted linked lists are also merged.
   * This just changes all pointers towards the nested lists to the new merged segments.
   * Example: a list has a nested list of length 5. The first nested list is merged into a 5+,
   * then the second, and so on. Then, the top-list is merged,
   * and all pointers towards the first 5+ nested list are changed to point to
   * the second nested 5+ list, not remembering the nesting level of nested lists.
   * This is continued until all is merged.
   * Note: we don't use nesting level as described in the paper
   * "Byte-precise Verification of low level list manipulation"!
   * They use nesting level for merges performed in joins. They use the all pointer specifier and repeat actions for all segments.
   * We remember concrete locations of pointers in abstracted memory.
   * Future idea: remember level of nested lists, for pointers towards them.
   * No nesting level as we do it now, but a list of nesting levels. With each segment corresponding
   * to a level. E.g. {1, 2} -> a list nested in a list, with the upper list nesting level 1,
   * the lower level 2. This would also give us the nesting level of the original paper back.
   *
   */
  public SMGState findAndAbstractLists() throws SMGException {
    SMGState currentState = state;
    statistics.startTotalListSearchTime();

    // Sort in DLL and SLL candidates and also order by nesting
    List<Set<SMGCandidate>> orderedListCandidatesByNesting = getListCandidates();

    assert currentState.getMemoryModel().getSmg().checkSMGSanity();
    statistics.stopTotalListSearchTime();
    statistics.startTotalAbstractionTime();
    // Abstract top level nesting first
    for (Set<SMGCandidate> candidates : orderedListCandidatesByNesting) {
      for (SMGCandidate candidate : candidates) {
        // Not valid means kicked out by abstraction
        // TODO: the nesting update might invalidate (nested) objects that should be abstracted now,
        //    think of a solution without searching the entire SMG again
        if (!currentState.getMemoryModel().isObjectValid(candidate.getObject())) {
          continue;
        }
        int nestingLvl = getNewNestingLvl(candidate, currentState);
        if (candidate.isDLL()) {
          currentState =
              currentState.abstractIntoDLL(
                  candidate.getObject(),
                  candidate.getSuspectedNfo(),
                  candidate.getSuspectedNfoTargetOffset(),
                  candidate.getSuspectedPfo().orElseThrow(),
                  candidate.getSuspectedPfoTargetPointerOffset().orElseThrow(),
                  ImmutableSet.of(),
                  nestingLvl);

        } else {
          currentState =
              currentState.abstractIntoSLL(
                  candidate.getObject(),
                  candidate.getSuspectedNfo(),
                  candidate.getSuspectedNfoTargetOffset(),
                  ImmutableSet.of(),
                  nestingLvl);
        }
      }
    }
    currentState = currentState.removeUnusedValues();
    statistics.stopTotalAbstractionTime();
    assert candidatesHaveBeenAbstracted(orderedListCandidatesByNesting, currentState);
    assert checkNestingLevel(currentState);
    return currentState;
  }

  private static int getNewNestingLvl(SMGCandidate candidate, SMGState currentState) {
    int nestingLvl = 0;
    SMGObject root = candidate.getObject();
    SMG curSMG = currentState.getMemoryModel().getSmg();
    Set<SMGValue> ptrsTowards =
        currentState.getMemoryModel().getSmg().getPointerValuesForTarget(root);
    ImmutableSet.Builder<SMGObject> objsPointingTowards = ImmutableSet.builder();
    for (SMGValue ptrTowards : ptrsTowards) {
      objsPointingTowards.addAll(
          currentState.getMemoryModel().getSmg().getAllObjectsWithValueInThem(ptrTowards));
    }

    for (SMGObject objPointingTowards : objsPointingTowards.build()) {
      if (objPointingTowards != root
          && objPointingTowards instanceof SMGSinglyLinkedListSegment sll) {
        if (!curSMG
            .getHasValueEdgesByPredicate(
                objPointingTowards,
                h ->
                    !sll.getNextOffset().equals(h.getOffset())
                        && (!(sll instanceof SMGDoublyLinkedListSegment dll)
                            || !dll.getPrevOffset().equals(h.getOffset()))
                        && curSMG.isPointer(h.hasValue())
                        && curSMG.getPTEdge(h.hasValue()).orElseThrow().pointsTo().equals(root))
            .isEmpty()) {
          Preconditions.checkArgument(nestingLvl == 0); // Found two abstr. lists
          nestingLvl = sll.getNestingLevel() + 1;
        }
      } else {
        // Found two ptrs from abstr. and not abstr. elements
        Preconditions.checkArgument(nestingLvl == 0);
      }
    }
    return nestingLvl;
  }

  private boolean candidatesHaveBeenAbstracted(
      List<Set<SMGCandidate>> orderedListCandidatesByNesting, SMGState stateAfterAbstraction)
      throws SMGException {
    PersistentSet<SMGObject> objectsAfterAbstr =
        stateAfterAbstraction.getMemoryModel().getHeapObjects();
    for (Set<SMGCandidate> set : orderedListCandidatesByNesting) {
      for (SMGCandidate candidate : set) {
        // Check that the old root is gone
        if (objectsAfterAbstr.contains(candidate.getObject())) {
          if (!stateAfterAbstraction.getMemoryModel().getSmg().isValid(candidate.getObject())) {
            throw new SMGException(
                "Internal error; Memory has not been cleaned correctly after abstraction. This is"
                    + " not a critical error and can be ignored.");
          } else {
            return false;
          }
        }
        // Check that the new abstracted lists have the correct length
        Set<SMGValue> shouldPointToAbstrList =
            state.getMemoryModel().getSmg().getPointerValuesForTarget(candidate.getObject());

        for (SMGValue ptrToAbstr : shouldPointToAbstrList) {
          Optional<SMGPointsToEdge> pte =
              stateAfterAbstraction.getMemoryModel().getSmg().getPTEdge(ptrToAbstr);
          PersistentMap<SMGObject, Integer> sourceObjMap =
              state.getMemoryModel().getSmg().getValuesToRegionsTheyAreSavedIn().get(ptrToAbstr);
          if (sourceObjMap != null) {
            for (SMGObject sourceObj : sourceObjMap.keySet()) {
              // Only stack obj pointers
              if (!state.getMemoryModel().isHeapObject(sourceObj)) {
                if (pte.isEmpty()) {
                  return false;
                } else if (!(pte.orElseThrow().pointsTo() instanceof SMGSinglyLinkedListSegment sll)
                    || sll.getMinLength() != candidate.maximalSizeOfList) {
                  return false;
                }
              }
            }
          }
        }
      }
    }
    return true;
  }

  List<Set<SMGCandidate>> getListCandidates() throws SMGException {
    equalityCache = EqualityCache.of();
    objectCache = EqualityCache.of();

    SymbolicProgramConfiguration memModel = state.getMemoryModel();
    SMG smg = memModel.getSmg();
    PersistentSet<SMGObject> heapObjs = memModel.getHeapObjects();
    // Lists -> heap objs
    // Top level lists have ptrs from non heap objs towards them and no ptrs from inside the list
    // except for ptrs from their own chain
    // All other lists are pointed to by other lists (and maybe ptrs from outside)
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> objsAndHVEs =
        smg.getSMGObjectsWithSMGHasValueEdges();

    Set<SMGObject> alreadySeen = new HashSet<>();
    ImmutableSet.Builder<SMGCandidate> foundChains = ImmutableSet.builder();
    for (SMGObject heapObj : heapObjs) {
      if (!smg.isValid(heapObj)
          || !heapObj.getSize().isNumericValue()
          || alreadySeen.contains(heapObj)) {
        continue;
      }
      if (heapObj instanceof SMGSinglyLinkedListSegment sllHeapObj) {
        // We know the nfo already concretely
        List<SMGHasValueEdge> readNfoEdges =
            smg.readValue(heapObj, sllHeapObj.getNextOffset(), memModel.getSizeOfPointer(), false)
                .getHvEdges();
        if (readNfoEdges.isEmpty()) {
          // No next element, maybe a prev exists though, don't put current in seen list, and it
          // will be found via the prev object
          continue;
        }
        SMGValue nextPointerValue = readNfoEdges.get(0).hasValue();
        if (!smg.isPointer(nextPointerValue)
            || !smg.getPTEdge(nextPointerValue).orElseThrow().getOffset().isNumericValue()
            || !smg.getPTEdge(nextPointerValue)
                .orElseThrow()
                .getOffset()
                .asNumericValue()
                .bigIntegerValue()
                .equals(sllHeapObj.getNextPointerTargetOffset())) {
          // Incorrect or not usable next element, maybe a prev exists though, don't put current in
          // seen list, and it will be found via the prev object
          continue;
        }
        SMGObject target = smg.getPTEdge(nextPointerValue).orElseThrow().pointsTo();
        if (target != heapObj
            && smg.isValid(target)
            && target.getSize().isNumericValue()
            && target
                .getSize()
                .asNumericValue()
                .bigIntegerValue()
                .equals(sllHeapObj.getSize().asNumericValue().bigIntegerValue())) {

          SMGCandidateOrRejectedObject maybeCandidate =
              lookThroughObject(
                  heapObj,
                  target,
                  sllHeapObj.getNextOffset(),
                  sllHeapObj.getNextPointerTargetOffset(),
                  alreadySeen);
          if (maybeCandidate.isListCandidate()) {
            SMGCandidate candidate = maybeCandidate.getCandidate();
            if (candidate.suspectedElements.size() > 1) {
              foundChains.add(candidate);
            }
            alreadySeen.addAll(candidate.suspectedElements);
            continue;
          }
        }
        // Incorrect or not usable next element, maybe a prev exists though,
        //   don't put current in seen list, and it will be found via the prev object
        continue;

      } else {
        List<SMGHasValueEdge> valuesInHeapObj =
            ImmutableList.sortedCopyOf(
                Comparator.comparing(SMGHasValueEdge::getOffset),
                objsAndHVEs.getOrDefault(heapObj, PersistentSet.of()));
        if (valuesInHeapObj.isEmpty()) {
          // Can't be a list if there is no pointers
          continue;
        }
        BigInteger heapObjSize = heapObj.getSize().asNumericValue().bigIntegerValue();
        SMGCandidateOrRejectedObject maybeCandidate = null;
        // Search through all possible values of the object first, remember to reject the obj if
        // nothing is found
        for (SMGHasValueEdge valueEdge : valuesInHeapObj) {
          Optional<SMGPointsToEdge> maybePTE = smg.getPTEdge(valueEdge.hasValue());

          if (maybePTE.isPresent()) {
            SMGPointsToEdge pointsToEdge = maybePTE.orElseThrow();
            // if there is a pointer and that pointer points to another valid heap obj of the same
            // size, this might be a list. If that other obj is not equal to this obj and has a
            // chain
            // at the same offset as this one at least threshold long, it's a list.
            SMGObject target = pointsToEdge.pointsTo();
            BigInteger ptrValueOffsetInHeapObj = valueEdge.getOffset();
            // pointerTargetOffset is the offset of the pointer towards to target, not the offset
            // of any value! This has to be the same for all ptrs between list elements!
            Value pointerTargetOffset = pointsToEdge.getOffset();
            Preconditions.checkArgument(pointerTargetOffset.isNumericValue());

            if (target != heapObj
                && smg.isValid(target)
                && target.getSize().isNumericValue()
                && target.getSize().asNumericValue().bigIntegerValue().equals(heapObjSize)) {

              maybeCandidate =
                  lookThroughObject(
                      heapObj,
                      target,
                      ptrValueOffsetInHeapObj,
                      pointerTargetOffset.asNumericValue().bigIntegerValue(),
                      alreadySeen);
              if (maybeCandidate.isListCandidate()) {
                SMGCandidate candidate = maybeCandidate.getCandidate();
                if (candidate.suspectedElements.size() > 1) {
                  foundChains.add(candidate);
                }
                alreadySeen.addAll(candidate.suspectedElements);
                break;
              }
            }
          }
        }
        if (maybeCandidate == null) {
          alreadySeen.add(heapObj);
        } else if (!maybeCandidate.isListCandidate()) {
          alreadySeen.add(maybeCandidate.getRejectedObject());
        }
      }
    }

    ImmutableSet.Builder<SMGCandidate> foundChainsWRoot = ImmutableSet.builder();
    // Find good roots
    for (SMGCandidate candidate : foundChains.build()) {
      foundChainsWRoot.add(searchAndSetRootForCandidate(candidate));
    }

    // Order the candidates and check for equality of non-linking pointers
    return findNestingOfCandidates(foundChainsWRoot.build());
  }

  /**
   * Candidates might have "bad" roots. We want the leftmost object in the list (assuming next goes
   * right) or roots with external pointers for looping lists.
   */
  private SMGCandidate searchAndSetRootForCandidate(SMGCandidate pCandidate) throws SMGException {
    SMG smg = state.getMemoryModel().getSmg();
    if (pCandidate.isLooping()) {
      for (SMGObject maybeRoot : pCandidate.suspectedElements) {
        Set<SMGValue> ptrsTowardsHeapObj = smg.getPointerValuesForTarget(maybeRoot);
        for (SMGValue ptrValue : ptrsTowardsHeapObj) {
          Set<SMGObject> objsWithPtrsTowardsHeapObj =
              smg.getValuesToRegionsTheyAreSavedIn().get(ptrValue).keySet();
          if (!pCandidate.suspectedElements.containsAll(objsWithPtrsTowardsHeapObj)) {
            return SMGCandidate.moveCandidateTo(maybeRoot, pCandidate);
          }
        }
      }
    }

    // Sanity check/change that the "first" candidate is really the first for the nfo found
    Map<SMGObject, Integer> lengthOfList = new HashMap<>();
    for (SMGObject canObj : pCandidate.getMaximalListElements()) {
      int len =
          getLengthOfList(
              canObj, pCandidate.suspectedNfo, pCandidate.suspectedElements, lengthOfList);

      if (len == pCandidate.maximalSizeOfList) {
        // Correct object for len
        return SMGCandidate.moveCandidateTo(canObj, pCandidate);
      }
    }
    // Should never happen
    throw new SMGException("Could not determine correct root for list abstraction.");
  }

  private int getLengthOfList(
      SMGObject currObj,
      BigInteger suspectedNfo,
      Set<SMGObject> allowedObjects,
      Map<SMGObject, Integer> knownLength)
      throws SMGException {
    int currentLength = currObj instanceof SMGSinglyLinkedListSegment sll ? sll.getMinLength() : 1;
    int nestedLength = 0;
    SMGValue nextOfCandidate =
        state
            .readSMGValue(currObj, suspectedNfo, state.getMemoryModel().getSizeOfPointer())
            .getSMGValue();
    if (!nextOfCandidate.isZero() && state.getMemoryModel().getSmg().isPointer(nextOfCandidate)) {
      SMGObject target =
          state.getMemoryModel().getSmg().getPTEdge(nextOfCandidate).orElseThrow().pointsTo();
      if (allowedObjects.contains(target)) {
        // target is a part of the list
        if (knownLength.containsKey(target)) {
          // Rest is known
          nestedLength = knownLength.get(target);
        } else {
          // Continue traverse
          nestedLength = getLengthOfList(target, suspectedNfo, allowedObjects, knownLength);
        }
      }
    }
    knownLength.put(currObj, nestedLength + currentLength);
    return nestedLength + currentLength;
  }

  private SMGCandidateOrRejectedObject lookThroughObject(
      SMGObject currentObj,
      SMGObject nextObj,
      BigInteger suspectedNfo,
      BigInteger nextPointerTargetOffset,
      Set<SMGObject> alreadySeen) {
    SMG smg = state.getMemoryModel().getSmg();
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> objsAndHVEs =
        smg.getSMGObjectsWithSMGHasValueEdges();
    Set<SMGObject> alreadySeenInChain = new HashSet<>(alreadySeen);

    Optional<BigInteger> maybePfo = Optional.empty();
    Optional<BigInteger> maybePrevPointerTargetOffset = Optional.empty();
    // Find pfo first
    if (currentObj instanceof SMGDoublyLinkedListSegment dllTarget) {
      maybePfo = Optional.of(dllTarget.getPrevOffset());
      maybePrevPointerTargetOffset = Optional.of(dllTarget.getPrevPointerTargetOffset());
    } else {
      List<SMGHasValueEdge> valuesInTargetObj =
          ImmutableList.sortedCopyOf(
              Comparator.comparing(SMGHasValueEdge::getOffset),
              objsAndHVEs.getOrDefault(nextObj, PersistentSet.of()));
      int j = 0;
      for (SMGHasValueEdge targetHVE : valuesInTargetObj) {
        if (targetHVE.getOffset().equals(suspectedNfo)) {
          break;
        }
        j++;
      }
      if (j + 1 < valuesInTargetObj.size()) {
        SMGHasValueEdge nextValueEdge = valuesInTargetObj.get(j + 1);
        Optional<SMGPointsToEdge> maybePrevPTE = smg.getPTEdge(nextValueEdge.hasValue());
        if (maybePrevPTE.isPresent() && maybePrevPTE.orElseThrow().pointsTo().equals(currentObj)) {
          maybePfo = Optional.of(nextValueEdge.getOffset());
          Value pteTargetOffsetValue = maybePrevPTE.orElseThrow().getOffset();
          Preconditions.checkArgument(pteTargetOffsetValue.isNumericValue());
          maybePrevPointerTargetOffset =
              Optional.of(pteTargetOffsetValue.asNumericValue().bigIntegerValue());
        }
      }
    }
    return lookThroughPrevAndThenSearchForList(
        currentObj,
        suspectedNfo,
        nextPointerTargetOffset,
        maybePfo,
        maybePrevPointerTargetOffset,
        alreadySeenInChain,
        new HashSet<>());
  }

  private SMGCandidateOrRejectedObject lookThroughPrevAndThenSearchForList(
      SMGObject currentObj,
      BigInteger suspectedNfo,
      BigInteger nextPointerTargetOffset,
      Optional<BigInteger> maybePfo,
      Optional<BigInteger> maybePrevPointerTargetOffset,
      Set<SMGObject> alreadySeenInChain,
      Set<SMGObject> alreadySeenLeftMost) {
    SMG smg = state.getMemoryModel().getSmg();
    int minimumLengthForLists = minimumLengthForListsForAbstraction - 1;
    // We count the currentObj as being the first valid candidate
    if (currentObj instanceof SMGSinglyLinkedListSegment sllHeapObj) {
      minimumLengthForLists = minimumLengthForListsForAbstraction - sllHeapObj.getMinLength() + 1;
    }
    // Also collect all list segments to the left, as otherwise we might use the prev
    // pointer instead as the next pointer (as we might be at the end of the list for next
    // or the list might be longer than the threshold for prev but not for next)
    SMGObject leftMostObj =
        lookThroughPrev(
            currentObj,
            suspectedNfo,
            nextPointerTargetOffset,
            maybePfo,
            maybePrevPointerTargetOffset,
            alreadySeenInChain);
    if (alreadySeenLeftMost.contains(leftMostObj)) {
      return SMGCandidateOrRejectedObject.ofRejectedObject(leftMostObj);
    } else {
      alreadySeenLeftMost.add(leftMostObj);
    }
    Preconditions.checkArgument(state.getMemoryModel().isHeapObject(leftMostObj));
    // Leftmost might not be a list obj, but a following obj might be a list start.
    // If we find leftmost not to be a list obj, we exclude it from the overall list and start w
    // next
    SMGHasValueEdge nextPtrOfLeftmost =
        smg.readValue(leftMostObj, suspectedNfo, state.getMemoryModel().getSizeOfPointer(), false)
            .getHvEdges()
            .get(0);
    Preconditions.checkArgument(smg.isPointer(nextPtrOfLeftmost.hasValue()));
    SMGObject nextOfLeftmost = smg.getPTEdge(nextPtrOfLeftmost.hasValue()).orElseThrow().pointsTo();
    // TODO: use read instead of looping through the offsets in both lookThroughs
    alreadySeenInChain = new HashSet<>();
    ListType listType =
        lookThroughNext(
            leftMostObj,
            nextOfLeftmost,
            suspectedNfo,
            nextPointerTargetOffset,
            maybePfo,
            maybePrevPointerTargetOffset,
            alreadySeenInChain,
            minimumLengthForLists);
    if (!listType.equals(ListType.NONE) && !listType.equals(ListType.NONE_NOT_SLL)) {
      Preconditions.checkArgument(ListType.DLL != listType || maybePfo.isPresent());
      boolean isDll = listType.equals(ListType.DLL) || listType.equals(ListType.LOOPINGDLL);
      maybePfo = isDll ? maybePfo : Optional.empty();
      maybePrevPointerTargetOffset = isDll ? maybePrevPointerTargetOffset : Optional.empty();
      // found list. Remember chain (to not search for it again) and find a good root later
      int size = 0;
      // TODO: extract this into lookThroughNext as we have the info there already
      for (SMGObject seenInChain : alreadySeenInChain) {
        if (seenInChain instanceof SMGSinglyLinkedListSegment sllSeen) {
          size += sllSeen.getMinLength();
        } else {
          size++;
        }
      }

      SMGCandidate newCandidate =
          new SMGCandidate(
              leftMostObj,
              suspectedNfo,
              nextPointerTargetOffset,
              maybePfo,
              maybePrevPointerTargetOffset,
              alreadySeenInChain,
              size);

      if (listType.equals(ListType.LOOPINGDLL) || listType.equals(ListType.LOOPINGSLL)) {
        newCandidate = SMGCandidate.setIsLooping(newCandidate);
      }
      return SMGCandidateOrRejectedObject.ofSMGCandidate(newCandidate);
    } else {
      // The current chain has been rejected, e.g. because of an outside pointer.
      // However, it might be that the list is being reversed currently and the "next" element is in
      // another shape.
      // Restarting here would loop us endlessly between the current list shape and the other.
      // Hence, we check that this is not the case
      if (!currentObj.equals(leftMostObj)
          && !(maybePfo.isEmpty() && listType.equals(ListType.NONE_NOT_SLL))) {
        return lookThroughPrevAndThenSearchForList(
            currentObj,
            suspectedNfo,
            nextPointerTargetOffset,
            maybePfo,
            maybePrevPointerTargetOffset,
            alreadySeenInChain,
            alreadySeenLeftMost);
      }
      // NONE case, add leftMostObj to seen, restart the chain w next
      return SMGCandidateOrRejectedObject.ofRejectedObject(leftMostObj);
    }
  }

  private ListType lookThroughNext(
      SMGObject prevObj,
      SMGObject potentialNextObj,
      BigInteger maybeNfo,
      BigInteger nextPointerTargetOffset,
      Optional<BigInteger> maybePfo,
      Optional<BigInteger> maybePrevPointerTargetOffset,
      Set<SMGObject> alreadySeenInChain,
      int remainingMinLength) {
    // We know entering into this that prevObj is a possible list start
    // (either because it's the first list segment, there is an external non-list pointer etc.)
    alreadySeenInChain.add(prevObj);

    SMG smg = state.getMemoryModel().getSmg();
    boolean looping = alreadySeenInChain.contains(potentialNextObj);

    if (!looping
        && state.areTwoObjectsPartOfList(
            potentialNextObj, prevObj, maybeNfo, nextPointerTargetOffset)
        && state.nestedMemoryHasEqualOutsidePointers(prevObj, potentialNextObj, maybeNfo, maybePfo)
        && !state.listElementsHaveOutsidePointerInBetween(
            prevObj,
            potentialNextObj,
            maybeNfo,
            nextPointerTargetOffset,
            maybePfo,
            maybePrevPointerTargetOffset)) {
      // We check the next pointer in areTwoObjectsPartOfList and the prev below to filter it later
      ImmutableList<BigInteger> exemptOffsetsOfList = ImmutableList.of(maybeNfo);
      if (maybePfo.isPresent()) {
        exemptOffsetsOfList = ImmutableList.of(maybeNfo, maybePfo.orElseThrow());
      }

      if (state.checkEqualValuesForTwoStatesWithExemptions(
          prevObj,
          potentialNextObj,
          ImmutableMap.of(prevObj, exemptOffsetsOfList, potentialNextObj, exemptOffsetsOfList),
          state,
          state,
          equalityCache,
          objectCache,
          true,
          true)) {

        // filter out DLLs where we accidentally used the pfo as nfo and are at the "end"
        if (maybeNfo.subtract(smg.getSizeOfPointer()).compareTo(BigInteger.ZERO) >= 0) {
          // We assume pfo to follow nfo directly
          SMGHasValueEdge maybeRealNext =
              smg.readValue(
                      potentialNextObj,
                      maybeNfo.subtract(smg.getSizeOfPointer()),
                      smg.getSizeOfPointer(),
                      false)
                  .getHvEdges()
                  .get(0);
          if (smg.isPointer(maybeRealNext.hasValue())
              && smg.getPTEdge(maybeRealNext.hasValue()).orElseThrow().pointsTo().equals(prevObj)) {
            return ListType.NONE;
          }
        }

        Set<SMGHasValueEdge> valuesInPotentialNextObj =
            smg.getSMGObjectsWithSMGHasValueEdges()
                .getOrDefault(potentialNextObj, PersistentSet.of());

        // Prev checking
        if (maybePfo.isPresent()) {
          for (SMGHasValueEdge hve : valuesInPotentialNextObj) {
            if (hve.getOffset().equals(maybePfo.orElseThrow())) {
              Optional<SMGPointsToEdge> maybePTE = smg.getPTEdge(hve.hasValue());

              if (maybePTE.isPresent()) {
                SMGPointsToEdge pointsToEdge = maybePTE.orElseThrow();
                // if there is a pointer and that pointer points to another valid heap obj of the
                // same
                // size, this might be a list. If that other obj is not equal to this obj and has a
                // chain at the same offset as this one at least threshold long, it's a list.
                SMGObject targetOfPtrInNextObj = pointsToEdge.pointsTo();
                if (targetOfPtrInNextObj != prevObj) {
                  // TODO: this is bad. Might allow sll interpretation of dlls
                  maybePfo = Optional.empty();
                  maybePrevPointerTargetOffset = Optional.empty();
                  break;
                }
              }
            }
          }
        }

        // potentialNextObj is a valid list segment
        int reduce = 1;
        if (potentialNextObj instanceof SMGSinglyLinkedListSegment targetSLL) {
          reduce = targetSLL.getMinLength();
        }
        remainingMinLength = remainingMinLength - reduce;
        alreadySeenInChain.add(potentialNextObj);

        // Next checking
        for (SMGHasValueEdge hve : valuesInPotentialNextObj) {
          if (hve.getOffset().equals(maybeNfo)) {
            Optional<SMGPointsToEdge> maybePTE = smg.getPTEdge(hve.hasValue());

            if (maybePTE.isPresent()) {
              SMGPointsToEdge pointsToEdge = maybePTE.orElseThrow();
              // if there is a pointer and that pointer points to another valid heap obj of the same
              // size, this might be a list. If that other obj is not equal to this obj and has a
              // chain
              // at the same offset as this one at least threshold long, it's a list.
              SMGObject targetOfPtrInNextObj = pointsToEdge.pointsTo();
              // pointerTargetOffset is the offset of the pointer towards to target, not the offset
              // of any value! This has to be the same for all ptrs between list elements!
              Value pteTargetOffset = pointsToEdge.getOffset();
              Preconditions.checkArgument(pteTargetOffset.isNumericValue());
              BigInteger pointerTargetOffset = pteTargetOffset.asNumericValue().bigIntegerValue();
              if (nextPointerTargetOffset.equals(pointerTargetOffset)) {
                // viable next pointer and possibly viable next obj
                return lookThroughNext(
                    potentialNextObj,
                    targetOfPtrInNextObj,
                    maybeNfo,
                    nextPointerTargetOffset,
                    maybePfo,
                    maybePrevPointerTargetOffset,
                    alreadySeenInChain,
                    remainingMinLength);
              }
            }
          }
        }
      }
    }
    // We do this to get the complete list chain in alreadySeenInChain
    if (remainingMinLength <= 0) {
      if (looping) {
        return maybePfo.isPresent() ? ListType.LOOPINGDLL : ListType.LOOPINGSLL;
      }
      return maybePfo.isPresent() ? ListType.DLL : ListType.SLL;
    }
    if (maybePfo.isEmpty()
        && state
            .getMemoryModel()
            .getSmg()
            .getAllSourcesForPointersPointingTowards(prevObj)
            .contains(potentialNextObj)
        && state
            .getMemoryModel()
            .getSmg()
            .getAllSourcesForPointersPointingTowards(potentialNextObj)
            .contains(prevObj)) {
      return ListType.NONE_NOT_SLL;
    }
    return ListType.NONE;
  }

  // Searches for list elements to the left of currentObj with currentObj suspected of being a list
  // segment. Fills alreadySeenInChain so that all suspected list elements to the left are present
  // in the end. This method only checks that the size, validity, nfo, pfo and pointer offsets
  // match. Does not check values inside the lists! Also does not check the length of the list!
  private SMGObject lookThroughPrev(
      SMGObject currentObj,
      BigInteger maybeNfo,
      BigInteger nextPointerTargetOffset,
      Optional<BigInteger> maybePfo,
      Optional<BigInteger> maybePrevPointerTargetOffset,
      Set<SMGObject> alreadySeenInChain) {
    alreadySeenInChain.add(currentObj);
    SMG smg = state.getMemoryModel().getSmg();
    if (state.objectHasOutsidePointerTowards(
        currentObj, maybeNfo, nextPointerTargetOffset, maybePfo, maybePrevPointerTargetOffset)) {
      // We only abstract sections with no outside pointers towards them, except for ALL ptrs
      return currentObj;
    }
    if (maybePfo.isPresent()) {
      Preconditions.checkArgument(maybePrevPointerTargetOffset.isPresent());
      // We suspect it's a DLL, so we use the prev pointers as far as possible
      List<SMGHasValueEdge> maybeReadBackPtrs =
          smg.readValue(
                  currentObj,
                  maybePfo.orElseThrow(),
                  state.getMemoryModel().getSizeOfPointer(),
                  false)
              .getHvEdges();
      Preconditions.checkArgument(maybeReadBackPtrs.size() == 1);
      SMGHasValueEdge maybeReadBackPtr = maybeReadBackPtrs.get(0);
      if (smg.isPointer(maybeReadBackPtr.hasValue())) {
        // possible back-pointer
        SMGPointsToEdge maybeBackPointerEdge =
            smg.getPTEdge(maybeReadBackPtr.hasValue()).orElseThrow();
        SMGObject maybePrevObj = maybeBackPointerEdge.pointsTo();
        // TODO: return optional empty for reasons that disqualify this list
        //  (invalid objs that are non 0 in chain etc.)
        // Use the more advanced equality check that checks pointers via memory shape
        if (alreadySeenInChain.contains(maybePrevObj)
            || !state.areTwoObjectsPartOfList(
                currentObj, maybePrevObj, maybeNfo, nextPointerTargetOffset)) {
          return currentObj;
        }

        // maybePrevObj is a list element to the left
        return lookThroughPrev(
            maybePrevObj,
            maybeNfo,
            nextPointerTargetOffset,
            maybePfo,
            maybePrevPointerTargetOffset,
            alreadySeenInChain);
      }

    } else {
      Set<SMGValue> ptrsTowardsHeapObj = smg.getPointerValuesForTarget(currentObj);
      for (SMGValue ptrValue : ptrsTowardsHeapObj) {
        Set<SMGObject> objsWithPtrsTowardsHeapObj =
            smg.getValuesToRegionsTheyAreSavedIn().get(ptrValue).keySet();
        SMGPointsToEdge pte = smg.getPTEdge(ptrValue).orElseThrow();
        if (!pte.getOffset().isNumericValue()
            || !pte.getOffset()
                .asNumericValue()
                .bigIntegerValue()
                .equals(nextPointerTargetOffset)) {
          continue;
        }

        for (SMGObject maybePrevObj : objsWithPtrsTowardsHeapObj) {
          // We search for segments to the left here
          if (alreadySeenInChain.contains(maybePrevObj)
              || !state.areTwoObjectsPartOfList(
                  currentObj, maybePrevObj, maybeNfo, nextPointerTargetOffset)) {
            continue;
          }

          // maybePrevObj is a list element to the left
          return lookThroughPrev(
              maybePrevObj,
              maybeNfo,
              nextPointerTargetOffset,
              maybePfo,
              maybePrevPointerTargetOffset,
              alreadySeenInChain);
        }
      }
    }
    return currentObj;
  }

  /*
   *  TODO: modify that it always represents the nesting and finds the correct nesting level
   */
  List<Set<SMGCandidate>> findNestingOfCandidates(Set<SMGCandidate> allListCandidates)
      throws SMGException {
    // Search by traversing the SMG starting from non-heap objs
    Map<SMGCandidate, Integer> nestingMap = new HashMap<>();
    List<Set<SMGCandidate>> currentOrdering = new ArrayList<>();
    Map<SMGObject, SMGCandidate> objToCandidateMap = new HashMap<>();
    Set<SMGObject> allPossibleListObjects = new HashSet<>();
    SymbolicProgramConfiguration memModel = state.getMemoryModel();
    SMG smg = memModel.getSmg();

    for (SMGCandidate candidate : allListCandidates) {
      nestingMap.put(candidate, 0);
      objToCandidateMap.put(candidate.getObject(), candidate);
      allPossibleListObjects.addAll(candidate.getMaximalListElements());
      for (SMGObject candidateListObj : candidate.getMaximalListElements()) {
        objToCandidateMap.put(candidateListObj, candidate);
      }
    }

    Set<SMGObject> oldChain = new HashSet<>();
    for (SMGObject obj : smg.getObjects()) {
      if (smg.isValid(obj) && !state.getMemoryModel().isHeapObject(obj)) {
        for (SMGHasValueEdge edge : smg.getEdges(obj)) {
          if (smg.isPointer(edge.hasValue())) {
            SMGObject target = smg.getPTEdge(edge.hasValue()).orElseThrow().pointsTo();
            // Non-Heap "root" from which we search
            // Get all pointers in this and traverse them until we either end or find a loop
            int level = 0;
            recursiveNestingSearch(
                target,
                oldChain,
                new HashSet<>(),
                nestingMap,
                allPossibleListObjects,
                objToCandidateMap,
                level);
          }
        }
      }
    }

    for (Entry<SMGCandidate, Integer> objToNestingLevel : nestingMap.entrySet()) {
      SMGCandidate obj = objToNestingLevel.getKey();
      int nesting = objToNestingLevel.getValue();
      while (currentOrdering.size() <= nesting) {
        currentOrdering.add(new HashSet<>());
      }
      // Add to correct
      currentOrdering.get(nesting).add(obj);
    }
    return currentOrdering;
  }

  private void recursiveNestingSearch(
      SMGObject currentObj,
      Set<SMGObject> oldChain,
      Set<SMGCandidate> alreadyFoundCandidates,
      Map<SMGCandidate, Integer> nestingMap,
      Set<SMGObject> allPossibleListObjects,
      Map<SMGObject, SMGCandidate> objToCandidateMap,
      int level)
      throws SMGException {
    SMG smg = state.getMemoryModel().getSmg();
    // Reject stack objs, invalid objs and already visited in this chain
    if (smg.isValid(currentObj)
        && state.getMemoryModel().isHeapObject(currentObj)
        && !oldChain.contains(currentObj)) {
      if (allPossibleListObjects.contains(currentObj)) {
        // list candidate found, determine which one, set nesting level and remember level
        SMGCandidate correctCandidate = objToCandidateMap.get(currentObj);
        Preconditions.checkNotNull(correctCandidate);
        // Don't count this candidate twice
        if (!alreadyFoundCandidates.contains(correctCandidate)) {
          if (level == 0 && correctCandidate.getObject().toString().contains("SMGObject99")) {
            alreadyFoundCandidates.add(correctCandidate);
          }
          nestingMap.put(
              correctCandidate,
              Integer.max(nestingMap.getOrDefault(correctCandidate, level), level));
          alreadyFoundCandidates.add(correctCandidate);
          level++;
        }

      } else {
        // Reset level when non-heap objs point to the current only if it's not currently in a list
        for (SMGObject possibleOutsideObj :
            smg.getAllSourcesForPointersPointingTowards(currentObj)) {
          if (smg.isValid(possibleOutsideObj)) {
            if (!state.getMemoryModel().isHeapObject(possibleOutsideObj)) {
              // Stack obj points to it, reset nesting level
              level = 0;
              break;
            } else {
              // Heap obj, if a found list is connected to this, we decrement the nesting by 1,
              // as we are still on the level of an already found nested list,
              // but not in the abstracted list
              if (allPossibleListObjects.contains(possibleOutsideObj)) {
                SMGCandidate maybeConnectedCandidate = objToCandidateMap.get(possibleOutsideObj);
                if (maybeConnectedCandidate != null
                    && alreadyFoundCandidates.contains(maybeConnectedCandidate)) {
                  // NFO or PFO for the pointers have to match in either direction
                  if (connectedAsList(currentObj, maybeConnectedCandidate)) {
                    alreadyFoundCandidates.remove(maybeConnectedCandidate);
                    level--;
                  }
                }
              }
            }
          }
        }
      }

      for (SMGHasValueEdge edge : smg.getEdges(currentObj)) {
        if (smg.isPointer(edge.hasValue())) {
          SMGObject target = smg.getPTEdge(edge.hasValue()).orElseThrow().pointsTo();
          // Repeat with targets
          Set<SMGObject> newChain = new HashSet<>(oldChain);

          newChain.add(currentObj);
          recursiveNestingSearch(
              target,
              newChain,
              new HashSet<>(alreadyFoundCandidates),
              nestingMap,
              allPossibleListObjects,
              objToCandidateMap,
              level);
        }
      }
    }
  }

  private boolean connectedAsList(SMGObject pCurrentObj, SMGCandidate pMaybeConnectedCandidate)
      throws SMGException {
    BigInteger nfo = pMaybeConnectedCandidate.getSuspectedNfo();

    if (!state.getMemoryModel().getSmg().isValid(pCurrentObj)
        || !pCurrentObj.getSize().isNumericValue()
        || !pMaybeConnectedCandidate.object.isSizeEqual(pCurrentObj)) {
      return false;
    }

    // "Easy" direction first, pCurrentObj -> pMaybeConnectedCandidate via nfo or pfo
    SMGValue nfoOfCurrent =
        state
            .readSMGValue(pCurrentObj, nfo, state.getMemoryModel().getSizeOfPointer())
            .getSMGValue();
    if (!nfoOfCurrent.isZero() && state.getMemoryModel().getSmg().isPointer(nfoOfCurrent)) {
      SMGObject nfoTarget =
          state.getMemoryModel().getSmg().getPTEdge(nfoOfCurrent).orElseThrow().pointsTo();
      if (pMaybeConnectedCandidate.getMaximalListElements().contains(nfoTarget)) {
        return true;
      }
    }

    if (pMaybeConnectedCandidate.isDLL()) {
      SMGValue pfoOfCurrent =
          state
              .readSMGValue(
                  pCurrentObj,
                  pMaybeConnectedCandidate.getSuspectedPfo().orElseThrow(),
                  state.getMemoryModel().getSizeOfPointer())
              .getSMGValue();
      if (!pfoOfCurrent.isZero() && state.getMemoryModel().getSmg().isPointer(pfoOfCurrent)) {
        SMGObject pfoTarget =
            state.getMemoryModel().getSmg().getPTEdge(pfoOfCurrent).orElseThrow().pointsTo();
        if (pMaybeConnectedCandidate.getMaximalListElements().contains(pfoTarget)) {
          return true;
        }
      }
    }

    // Now we need to check every list element the same way
    for (SMGObject candidate : pMaybeConnectedCandidate.getMaximalListElements()) {
      SMGValue nfoOfCurrentCandidate =
          state
              .readSMGValue(candidate, nfo, state.getMemoryModel().getSizeOfPointer())
              .getSMGValue();
      if (!nfoOfCurrentCandidate.isZero()
          && state.getMemoryModel().getSmg().isPointer(nfoOfCurrentCandidate)) {
        SMGObject nfoOfCurrentTarget =
            state
                .getMemoryModel()
                .getSmg()
                .getPTEdge(nfoOfCurrentCandidate)
                .orElseThrow()
                .pointsTo();
        if (pCurrentObj.equals(nfoOfCurrentTarget)) {
          return true;
        }
      }

      if (pMaybeConnectedCandidate.isDLL()) {
        SMGValue pfoOfCurrent =
            state
                .readSMGValue(
                    candidate,
                    pMaybeConnectedCandidate.getSuspectedPfo().orElseThrow(),
                    state.getMemoryModel().getSizeOfPointer())
                .getSMGValue();
        if (!pfoOfCurrent.isZero() && state.getMemoryModel().getSmg().isPointer(pfoOfCurrent)) {
          SMGObject pfoTarget =
              state.getMemoryModel().getSmg().getPTEdge(pfoOfCurrent).orElseThrow().pointsTo();
          if (pCurrentObj.equals(pfoTarget)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private SMGCandidate getLinkedCandidateLength(
      SMGObject root,
      BigInteger nfo,
      int size,
      Set<SMGObject> alreadySeen,
      SMGCandidate candidate) {

    SMG smg = state.getMemoryModel().getSmg();
    if (!smg.isValid(root) || alreadySeen.contains(root)) {
      return SMGCandidate.withFoundListElements(alreadySeen, size, candidate);
    }

    int addSize = 1;
    if (root instanceof SMGSinglyLinkedListSegment) {
      addSize = ((SMGSinglyLinkedListSegment) root).getMinLength();
    }

    for (SMGHasValueEdge hve : smg.getEdges(root)) {
      SMGValue value = hve.hasValue();

      if (hve.getOffset().compareTo(nfo) == 0 && smg.isPointer(value)) {
        SMGPointsToEdge pointsToEdge = smg.getPTEdge(value).orElseThrow();
        // We only add list elements to already seen
        // In theory this does not prevent all possible loops. But who would program such a thing?
        alreadySeen.add(root);
        return getLinkedCandidateLength(
            pointsToEdge.pointsTo(), nfo, addSize + size, alreadySeen, candidate);
      }
    }
    // We don't count the "last" element this way if it does not have a pointer at nfo
    // This is completely fine! If the list extends further it will break the threshold and be
    // abstracted eventually.
    return SMGCandidate.withFoundListElements(alreadySeen, size, candidate);
  }

  /**
   * Finds all possible linked list candidates from the states heap memory. Checks that the
   * candidates have the same size and nfo and are valid and have at least
   * minimumLengthForListsForAbstraction length in total. Does not check DLL or value equality! You
   * can expect the candidates to have the suspected root, nfo, max size independent of value
   * equality and the objects.
   *
   * @return an unsorted list of candidates.
   */
  ImmutableList<SMGCandidate> getRefinedLinkedCandidates() throws SMGException {
    ImmutableList<SMGCandidate> sortedCandiList =
        ImmutableList.sortedCopyOf(
            Comparator.comparing(SMGCandidate::getSuspectedNfo),
            refineCandidates(getLinkedCandidates(), state));

    ImmutableList.Builder<SMGCandidate> refinedLinkedCandidatesBuilder = ImmutableList.builder();
    for (SMGCandidate candidate : sortedCandiList) {
      SMGCandidate candidateWithListInfo =
          getLinkedCandidateLength(
              candidate.getObject(), candidate.getSuspectedNfo(), 0, new HashSet<>(), candidate);
      if (minimumLengthForListsForAbstraction <= candidateWithListInfo.maximalSizeOfList) {
        refinedLinkedCandidatesBuilder.add(candidateWithListInfo);
      }
    }
    return refinedLinkedCandidatesBuilder.build();
  }

  @VisibleForTesting
  private Set<SMGCandidate> getLinkedCandidates() throws SMGException {
    SMG smg = state.getMemoryModel().getSmg();
    Set<SMGCandidate> candidates = new HashSet<>();
    Set<SMGObject> alreadyVisited = new HashSet<>();
    for (SMGObject heapObj : state.getMemoryModel().getHeapObjects()) {
      if (!smg.isValid(heapObj) || !heapObj.getSize().isNumericValue()) {
        continue;
      }
      Optional<SMGCandidate> possibleCandidate =
          getSLLinkedCandidatesForObject(
              heapObj, smg, alreadyVisited, state.getMemoryModel().getHeapObjects());
      if (possibleCandidate.isPresent()) {
        candidates.add(possibleCandidate.orElseThrow());
      }
    }
    // Refine candidates (DLL tend to produce both ends as candidates,
    // we chose the smallest offset and traverse the list and if we git another list,
    // we kick it out)
    return candidates;
  }

  /**
   * Determines if a candidate is a DLL and returns the suspected PFO if it is. Protected for tests
   * only.
   *
   * @param candidate the {@link SMGCandidate} for a list.
   * @param smg the {@link SMG}
   * @return Optional.empty iff not a DLL. Suspected PFO else.
   */
  @VisibleForTesting
  protected Optional<BigInteger> isDLL(SMGCandidate candidate, SMG smg) throws SMGException {
    BigInteger nfo = candidate.getSuspectedNfo();
    SMGObject root = candidate.getObject();
    // Go to the next element, search for a pointer back and conform this for the following
    // The first element might not have a prev pointer
    // In theory the first prev pointer of a list may not have been set, we check the next
    SMGObject nextObject = null;
    if (root instanceof SMGDoublyLinkedListSegment dll) {
      if (dll.getNextOffset().compareTo(nfo) == 0) {
        return Optional.of(dll.getPrevOffset());
      } else if (dll.getPrevOffset().compareTo(nfo) == 0) {
        // DLL got turned, turn back
        throw new AssertionError("Error during shape refinement.");
      } else {
        throw new AssertionError("Error during shape refinement.");
      }

    } else if (root instanceof SMGSinglyLinkedListSegment) {
      return Optional.empty();
    }
    for (SMGHasValueEdge hve : smg.getEdges(root)) {
      SMGValue value = hve.hasValue();

      if (hve.getOffset().compareTo(nfo) == 0 && smg.isPointer(value)) {
        SMGPointsToEdge pointsToEdge = smg.getPTEdge(value).orElseThrow();
        nextObject = pointsToEdge.pointsTo();
        break;
      }
    }
    return findBackPointerOffsetForListObject(nextObject, root, smg, nfo, 1);
  }

  Set<SMGCandidate> refineCandidates(Set<SMGCandidate> candidates, SMGState pState)
      throws SMGException {
    Set<SMGCandidate> finalCandidates = candidates;
    ImmutableList<SMGCandidate> sortedCandiList =
        ImmutableList.sortedCopyOf(Comparator.comparing(SMGCandidate::getSuspectedNfo), candidates);
    for (SMGCandidate candi : sortedCandiList) {
      // now kick all found other candidates out
      if (finalCandidates.contains(candi)) {
        finalCandidates =
            traverseAndRemoveEqual(
                candi.getObject(),
                candi.getSuspectedNfo(),
                finalCandidates,
                pState,
                new HashSet<>());
      }
    }
    return finalCandidates;
  }

  private Set<SMGCandidate> traverseAndRemoveEqual(
      SMGObject candidate,
      BigInteger nfo,
      Set<SMGCandidate> candidates,
      SMGState pState,
      Set<SMGObject> alreadyVisited)
      throws SMGException {
    if (alreadyVisited.contains(candidate)) {
      return candidates;
    }
    alreadyVisited.add(candidate);
    // SLL is fine to traverse
    Optional<SMGObject> maybeNext = getValidNextSLL(candidate, nfo, pState);
    if (maybeNext.isPresent()) {
      SMGObject nextObject = maybeNext.orElseThrow();
      Set<SMGCandidate> newCandidates =
          candidates.stream()
              .filter(can -> !can.getObject().equals(nextObject))
              .collect(ImmutableSet.toImmutableSet());
      return traverseAndRemoveEqual(nextObject, nfo, newCandidates, pState, alreadyVisited);
    } else {
      return candidates;
    }
  }

  private Optional<SMGObject> getValidNextSLL(SMGObject root, BigInteger nfo, SMGState pState)
      throws SMGException {
    SMGValueAndSMGState valueAndState =
        pState.readSMGValue(root, nfo, pState.getMemoryModel().getSizeOfPointer());
    SMGValue value = valueAndState.getSMGValue();
    if (!state.getMemoryModel().getSmg().isPointer(value)) {
      return Optional.empty();
    }
    SMGObject nextObject =
        pState.getMemoryModel().getSmg().getPTEdge(value).orElseThrow().pointsTo();
    Value rootObjSize = root.getSize();
    Value nextObjSize = nextObject.getSize();
    if (!rootObjSize.isNumericValue() || !nextObjSize.isNumericValue()) {
      throw new SMGException(
          "Symbolic memory size in linked list abstraction not supported at the moment.");
    }
    BigInteger rootObjConcreteSize = rootObjSize.asNumericValue().bigIntegerValue();
    BigInteger nextObjConcreteSize = nextObjSize.asNumericValue().bigIntegerValue();
    if (!pState.getMemoryModel().getSmg().isValid(nextObject)
        || rootObjConcreteSize.compareTo(nextObjConcreteSize) != 0) {
      return Optional.empty();
    }
    // Same object size, same content expect for the pointers, its valid -> ok
    // We don't need the state as it would only change for unknown reads
    return Optional.of(nextObject);
  }

  private Optional<BigInteger> nextHaveBackPointersWithAssumedPFO(
      SMGObject root,
      SMGObject previous,
      SMG smg,
      BigInteger nfo,
      BigInteger pfo,
      int lengthToCheck,
      Set<SMGObject> alreadyVisited)
      throws SMGException {
    if (lengthToCheck <= 0) {
      return Optional.of(pfo);
    }

    ImmutableSet<SMGHasValueEdge> setOfPointers =
        getPointersToSameSizeObjectsWithoutOffset(root, smg, alreadyVisited, nfo);
    if (setOfPointers.isEmpty()) {
      return Optional.empty();
    }

    @Nullable SMGObject nextObject = findNextObjectForListWithNFO(root, nfo, smg);

    // Check all others for validity
    alreadyVisited.add(previous);
    for (SMGHasValueEdge hve : setOfPointers) {
      if (hve.getOffset().compareTo(pfo) != 0) {
        continue;
      }
      SMGValue value = hve.hasValue();
      SMGPointsToEdge pte = smg.getPTEdge(value).orElseThrow();
      SMGObject prevInCurrent = pte.pointsTo();
      if (prevInCurrent.equals(previous)) {
        // assume that the pfo is correct, test further
        if (nextObject == null) {
          return Optional.empty();
        }

        Optional<BigInteger> maybePFO =
            nextHaveBackPointersWithAssumedPFO(
                nextObject, root, smg, nfo, pfo, lengthToCheck - 1, alreadyVisited);

        if (maybePFO.isPresent()) {
          return maybePFO;
        }
      }
    }
    return Optional.empty();
  }

  private Optional<BigInteger> findBackPointerOffsetForListObject(
      SMGObject root, SMGObject previous, SMG smg, BigInteger nfo, int lengthToCheck)
      throws SMGException {
    Set<SMGObject> alreadyVisited = new HashSet<>();
    // pfo unknown, try to find it

    ImmutableSet<SMGHasValueEdge> setOfPointers =
        getPointersToSameSizeObjectsWithoutOffset(root, smg, alreadyVisited, nfo);
    if (setOfPointers.isEmpty()) {
      return Optional.empty();
    }

    @Nullable SMGObject nextObject = findNextObjectForListWithNFO(root, nfo, smg);

    // Check all others for validity
    alreadyVisited.add(previous);
    for (SMGHasValueEdge hve : setOfPointers) {
      BigInteger pfo = hve.getOffset();
      SMGValue value = hve.hasValue();
      SMGPointsToEdge pte = smg.getPTEdge(value).orElseThrow();
      SMGObject prevInCurrent = pte.pointsTo();
      if (prevInCurrent.equals(previous)) {
        // assume that the pfo is correct, test further
        Optional<BigInteger> maybePFO =
            nextHaveBackPointersWithAssumedPFO(
                nextObject, root, smg, nfo, pfo, lengthToCheck - 1, alreadyVisited);

        if (maybePFO.isPresent()) {
          return maybePFO;
        }
      }
    }
    return Optional.empty();
  }

  private @Nullable SMGObject findNextObjectForListWithNFO(
      SMGObject root, BigInteger nfo, SMG smg) {
    SMGObject nextObject = null;
    for (SMGHasValueEdge hve : smg.getEdges(root)) {
      SMGValue value = hve.hasValue();

      if (hve.getOffset().compareTo(nfo) == 0 && smg.isPointer(value)) {
        Optional<SMGPointsToEdge> pointsToEdge = smg.getPTEdge(value);
        if (pointsToEdge.isEmpty()) {
          break;
        }
        nextObject = pointsToEdge.orElseThrow().pointsTo();
        break;
      }
    }
    return nextObject;
  }

  /* Search for followup segments based on potential root. If we find an object that is already
  processed in the candidatesMap, we take and remove it from the map,
  link it to the current segment, and return the new segment. */
  private Optional<SMGCandidate> getSLLinkedCandidatesForObject(
      SMGObject potentialRoot,
      SMG pInputSmg,
      Set<SMGObject> pAlreadyVisited,
      Collection<SMGObject> heapObjects)
      throws SMGException {
    Set<SMGObject> thisAlreadyVisited = new HashSet<>(pAlreadyVisited);
    if (thisAlreadyVisited.contains(potentialRoot) || !pInputSmg.isValid(potentialRoot)) {
      return Optional.empty();
    }
    thisAlreadyVisited.add(potentialRoot);
    if (potentialRoot instanceof SMGSinglyLinkedListSegment sll) {
      pAlreadyVisited.add(potentialRoot);
      // BigInteger.ZERO is potentially wrong, but this code is about to be removed
      return Optional.of(new SMGCandidate(potentialRoot, sll.getNextOffset(), BigInteger.ZERO));
    }

    ImmutableSet<SMGHasValueEdge> setOfPointers =
        getPointersToSameSizeObjects(potentialRoot, pInputSmg, thisAlreadyVisited);
    // Sort by offset of pointers and beginn with the smallest
    // Lists usually have smth like a next, then prev pointer ordering
    // We abort after we find 1 valid pointer for the candidate
    ImmutableList<SMGHasValueEdge> sortedPointersList =
        ImmutableList.sortedCopyOf(Comparator.comparing(SMGHasValueEdge::getOffset), setOfPointers);
    // If there are no targets that match this one, this is either a end part or not a list
    if (setOfPointers.isEmpty()) {
      return Optional.empty();
    }
    for (SMGHasValueEdge hve : sortedPointersList) {
      SMGValue value = hve.hasValue();
      BigInteger nfo = hve.getOffset();
      SMGPointsToEdge pointsToEdge = pInputSmg.getPTEdge(value).orElseThrow();
      SMGObject reachedObject = pointsToEdge.pointsTo();
      if ((reachedObject instanceof SMGSinglyLinkedListSegment sll)
          && (sll.getNextOffset().compareTo(nfo) == 0)) {
        pAlreadyVisited.add(potentialRoot);
        // BigInteger.ZERO is potentially wrong, but this code is about to be removed
        return Optional.of(new SMGCandidate(potentialRoot, nfo, BigInteger.ZERO));
      }

      // Check that reached object has a pointer at the same offset
      if (followupHasNextPointerToValid(reachedObject, nfo, pInputSmg, thisAlreadyVisited)) {
        // Valid candidate found!
        // Make sure it's a "root" by checking all pointers towards this root
        // The only valid pointers towards this root are from the followup or non heap objects
        if (pInputSmg.hasPotentialListObjectsWithPointersToObject(
            potentialRoot, nfo, heapObjects)) {
          pAlreadyVisited.add(potentialRoot);
          // BigInteger.ZERO is potentially wrong, but this code is about to be removed
          return Optional.of(new SMGCandidate(potentialRoot, nfo, BigInteger.ZERO));
        } else {
          // TODO: check that there is a "external" pointer pointing towards this (a pointer that is
          // not inside the list)
          pAlreadyVisited.add(potentialRoot);
          // BigInteger.ZERO is potentially wrong, but this code is about to be removed
          return Optional.of(new SMGCandidate(potentialRoot, nfo, BigInteger.ZERO));
        }
      }
    }
    // We might have missed looping lists
    return Optional.empty();
  }

  private boolean followupHasNextPointerToValid(
      SMGObject potentialFollowup,
      BigInteger nfoOfPrev,
      SMG pInputSmg,
      Set<SMGObject> alreadyVisited)
      throws SMGException {
    alreadyVisited.add(potentialFollowup);
    ImmutableSet<SMGHasValueEdge> pointers =
        getPointersToSameSizeObjects(potentialFollowup, pInputSmg, alreadyVisited);
    if (pointers.isEmpty()) {
      alreadyVisited.remove(potentialFollowup);
      return false;
    }
    // Only valid pointers are left
    for (SMGHasValueEdge hve : pointers) {
      if (hve.getOffset().compareTo(nfoOfPrev) == 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns set of HVE whoes values are pointers pointing to valid same size objects that have not
   * yet been visited.
   */
  private ImmutableSet<SMGHasValueEdge> getPointersToSameSizeObjects(
      SMGObject root, SMG pInputSmg, Set<SMGObject> alreadyVisited) throws SMGException {
    Value rootObjSize = root.getSize();
    if (!rootObjSize.isNumericValue()) {
      throw new SMGException(
          "Symbolic memory size in linked list abstraction not supported at the moment.");
    }
    BigInteger rootObjConcreteSize = rootObjSize.asNumericValue().bigIntegerValue();
    ImmutableSet.Builder<SMGHasValueEdge> res = ImmutableSet.builder();
    for (SMGHasValueEdge hve : pInputSmg.getEdges(root)) {
      SMGValue value = hve.hasValue();

      if (pInputSmg.isPointer(value)) {
        SMGPointsToEdge pointsToEdge = pInputSmg.getPTEdge(value).orElseThrow();
        SMGObject reachedObject = pointsToEdge.pointsTo();
        if (alreadyVisited.contains(reachedObject)) {
          continue;
        }
        if (!reachedObject.getSize().isNumericValue()) {
          throw new SMGException(
              "Symbolic memory size in linked list abstraction not supported at the moment.");
        }
        // If the followup is invalid or size does not match, next
        if (!pInputSmg.isValid(reachedObject)
            || reachedObject
                    .getSize()
                    .asNumericValue()
                    .bigIntegerValue()
                    .compareTo(rootObjConcreteSize)
                != 0) {
          continue;
        }

        if (pointsToEdge.targetSpecifier() != SMGTargetSpecifier.IS_ALL_POINTER
            && pointsToEdge.targetSpecifier() != SMGTargetSpecifier.IS_REGION) {
          continue;
        }
        res.add(hve);
      }
    }
    return res.build();
  }

  private ImmutableSet<SMGHasValueEdge> getPointersToSameSizeObjectsWithoutOffset(
      SMGObject root, SMG pInputSmg, Set<SMGObject> alreadyVisted, BigInteger offsetToAvoid)
      throws SMGException {
    Value rootObjSize = root.getSize();
    if (!rootObjSize.isNumericValue()) {
      throw new SMGException(
          "Symbolic memory size in linked list abstraction not supported at the moment.");
    }
    BigInteger rootObjConcreteSize = rootObjSize.asNumericValue().bigIntegerValue();
    ImmutableSet.Builder<SMGHasValueEdge> res = ImmutableSet.builder();
    for (SMGHasValueEdge hve : pInputSmg.getEdges(root)) {
      SMGValue value = hve.hasValue();
      if (hve.getOffset().compareTo(offsetToAvoid) == 0) {
        continue;
      }

      if (pInputSmg.isPointer(value)) {
        SMGPointsToEdge pointsToEdge = pInputSmg.getPTEdge(value).orElseThrow();
        SMGObject reachedObject = pointsToEdge.pointsTo();
        if (alreadyVisted.contains(reachedObject)) {
          continue;
        }
        if (!reachedObject.getSize().isNumericValue()) {
          throw new SMGException(
              "Symbolic memory size in linked list abstraction not supported at the moment.");
        }
        // If the followup is invalid or size does not match, next
        if (!pInputSmg.isValid(reachedObject)
            || reachedObject
                    .getSize()
                    .asNumericValue()
                    .bigIntegerValue()
                    .compareTo(rootObjConcreteSize)
                != 0) {
          continue;
        }

        if (pointsToEdge.targetSpecifier() != SMGTargetSpecifier.IS_ALL_POINTER
            && pointsToEdge.targetSpecifier() != SMGTargetSpecifier.IS_REGION) {
          continue;
        }
        res.add(hve);
      }
    }
    return res.build();
  }

  private boolean checkNestingLevel(SMGState pCurrentState) {
    for (SMGSinglyLinkedListSegment nestedSll :
        pCurrentState.getMemoryModel().getSmg().getAllValidAbstractedObjects()) {
      Set<SMGValue> ptrsTowards =
          pCurrentState.getMemoryModel().getSmg().getPointerValuesForTarget(nestedSll);
      for (SMGValue ptrTowards : ptrsTowards) {
        for (SMGObject objPointingToWards :
            pCurrentState.getMemoryModel().getSmg().getAllObjectsWithValueInThem(ptrTowards)) {
          // If an object that is NOT in the same list points towards a nested abstr. list, the
          // nesting level must be different
          if (objPointingToWards != nestedSll
              && objPointingToWards instanceof SMGSinglyLinkedListSegment sll2) {
            // Get location of the ptr in sll2 and check that it's not a next or prev of both
            BigInteger ptrOffset = null;
            for (SMGHasValueEdge hveSll2 :
                pCurrentState.getMemoryModel().getSmg().getEdges(objPointingToWards)) {
              if (hveSll2.hasValue().equals(ptrTowards)) {
                ptrOffset = hveSll2.getOffset();
                break;
              }
            }
            if (!sll2.getNextOffset().equals(ptrOffset)
                && (!(sll2 instanceof SMGDoublyLinkedListSegment dll2)
                    || !dll2.getPrevOffset().equals(ptrOffset))) {
              if (sll2.getNestingLevel() + 1 != nestedSll.getNestingLevel()) {
                return false;
              }
            }
          }
        }
      }
    }
    return true;
  }

  protected static class SMGCandidateOrRejectedObject {

    private final SMGObject nonListObj;

    @Nullable private final SMGCandidate possibleCandidate;

    private SMGCandidateOrRejectedObject(
        SMGObject pNonListObj, @Nullable SMGCandidate pPossibleCandidate) {
      nonListObj = pNonListObj;
      possibleCandidate = pPossibleCandidate;
    }

    public static SMGCandidateOrRejectedObject ofRejectedObject(SMGObject rejectedObject) {
      return new SMGCandidateOrRejectedObject(rejectedObject, null);
    }

    public static SMGCandidateOrRejectedObject ofSMGCandidate(SMGCandidate candidate) {
      return new SMGCandidateOrRejectedObject(null, candidate);
    }

    public boolean isListCandidate() {
      return nonListObj == null;
    }

    public SMGObject getRejectedObject() {
      return nonListObj;
    }

    public SMGCandidate getCandidate() {
      return possibleCandidate;
    }
  }

  @VisibleForTesting
  protected static class SMGCandidate {
    private final SMGObject object;
    private final BigInteger suspectedNfo;

    // If not present -> SLL
    private final Optional<BigInteger> suspectedPfo;

    /*
     * The offset of the nfo pointer in the target.
     */
    private final BigInteger suspectedNfoTargetPointerOffset;

    private final Optional<BigInteger> suspectedPfoTargetOffset;

    /*
     * Other suspected list elements. This is the maximal possible over approximation for the same size/nfo!
     * Equalities are not checked and this might not reflect the final abstracted list.
     * Used to find accurate nesting.
     */
    private final Set<SMGObject> suspectedElements;

    // Max size. Not checked for abstractable length!
    private final int maximalSizeOfList;

    private boolean looping = false;

    public SMGCandidate(
        SMGObject pObject, BigInteger pSuspectedNfo, BigInteger pSuspectedNfoTargetPointerOffset) {
      object = pObject;
      suspectedNfo = pSuspectedNfo;
      suspectedNfoTargetPointerOffset = pSuspectedNfoTargetPointerOffset;
      suspectedPfo = Optional.empty();
      suspectedPfoTargetOffset = Optional.empty();
      suspectedElements = ImmutableSet.of();
      maximalSizeOfList = 1;
    }

    private SMGCandidate(
        SMGObject pObject,
        BigInteger pSuspectedNfo,
        BigInteger pSuspectedNfoTargetOffset,
        Optional<BigInteger> pSuspectedPfo,
        Optional<BigInteger> pSuspectedPfoTargetOffset,
        Set<SMGObject> pSuspectedElements,
        int maxSize) {
      object = pObject;
      suspectedNfo = pSuspectedNfo;
      suspectedNfoTargetPointerOffset = pSuspectedNfoTargetOffset;
      suspectedPfoTargetOffset = pSuspectedPfoTargetOffset;
      suspectedPfo = pSuspectedPfo;
      suspectedElements = pSuspectedElements;
      maximalSizeOfList = maxSize;
    }

    private SMGCandidate(
        SMGObject pObject,
        BigInteger pSuspectedNfo,
        BigInteger pSuspectedNfoTargetOffset,
        Optional<BigInteger> pSuspectedPfo,
        Optional<BigInteger> pSuspectedPfoTargetOffset,
        Set<SMGObject> pSuspectedElements,
        int maxSize,
        boolean pLooping) {
      object = pObject;
      suspectedNfo = pSuspectedNfo;
      suspectedPfo = pSuspectedPfo;
      suspectedNfoTargetPointerOffset = pSuspectedNfoTargetOffset;
      suspectedPfoTargetOffset = pSuspectedPfoTargetOffset;
      suspectedElements = pSuspectedElements;
      maximalSizeOfList = maxSize;
      looping = pLooping;
    }

    public static SMGCandidate moveCandidateTo(
        SMGObject newCandidateInSameList, SMGCandidate oldCandidateOnSameList) {
      return new SMGCandidate(
          newCandidateInSameList,
          oldCandidateOnSameList.suspectedNfo,
          oldCandidateOnSameList.suspectedNfoTargetPointerOffset,
          oldCandidateOnSameList.suspectedPfo,
          oldCandidateOnSameList.suspectedPfoTargetOffset,
          oldCandidateOnSameList.suspectedElements,
          oldCandidateOnSameList.maximalSizeOfList,
          oldCandidateOnSameList.looping);
    }

    public static SMGCandidate withPfo(
        BigInteger pSuspectedPfo, BigInteger pSuspectedPfoTargetOffset, SMGCandidate candidate) {
      return new SMGCandidate(
          candidate.object,
          candidate.suspectedNfo,
          candidate.suspectedNfoTargetPointerOffset,
          Optional.of(pSuspectedPfo),
          Optional.of(pSuspectedPfoTargetOffset),
          candidate.suspectedElements,
          candidate.maximalSizeOfList,
          candidate.looping);
    }

    public static SMGCandidate setIsLooping(SMGCandidate candidate) {
      return new SMGCandidate(
          candidate.object,
          candidate.suspectedNfo,
          candidate.suspectedNfoTargetPointerOffset,
          candidate.suspectedPfo,
          candidate.suspectedPfoTargetOffset,
          candidate.suspectedElements,
          candidate.maximalSizeOfList,
          true);
    }

    public static SMGCandidate withFoundListElements(
        Set<SMGObject> pSuspectedElements, int lengthOfList, SMGCandidate candidate) {
      return new SMGCandidate(
          candidate.object,
          candidate.suspectedNfo,
          candidate.suspectedNfoTargetPointerOffset,
          candidate.suspectedPfo,
          candidate.suspectedPfoTargetOffset,
          pSuspectedElements,
          lengthOfList,
          candidate.looping);
    }

    public SMGObject getObject() {
      return object;
    }

    public BigInteger getSuspectedNfo() {
      return suspectedNfo;
    }

    public BigInteger getSuspectedNfoTargetOffset() {
      return suspectedNfoTargetPointerOffset;
    }

    public boolean isDLL() {
      return suspectedPfo.isPresent();
    }

    public Optional<BigInteger> getSuspectedPfo() {
      return suspectedPfo;
    }

    public Optional<BigInteger> getSuspectedPfoTargetPointerOffset() {
      return suspectedPfoTargetOffset;
    }

    /**
     * Other suspected list elements. This is the maximal possible over approximation for the same
     * size/nfo! Equalities are not checked and this might not reflect the final abstracted list.
     * The candidate is part of that list, but it is unknown where. Used to find accurate nesting.
     *
     * @return the found list for the candidate.
     */
    public Set<SMGObject> getMaximalListElements() {
      return suspectedElements;
    }

    public boolean isLooping() {
      return looping;
    }

    @Override
    public String toString() {
      return "SMGCandidate with root "
          + object
          + ", length "
          + getMaximalListElements()
          + ", is looping: "
          + looping;
    }
  }
}
