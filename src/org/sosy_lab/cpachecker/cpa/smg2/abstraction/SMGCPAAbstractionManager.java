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

  private int minimumLengthForListsForAbstraction;

  private final SMGCPAStatistics statistics;

  private enum ListType {
    SLL,
    LOOPINGSLL,
    DLL,
    LOOPINGDLL,
    NONE
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
    equalityCache = EqualityCache.of();
    objectCache = EqualityCache.of();
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
        if (!currentState.getMemoryModel().isObjectValid(candidate.getObject())) {
          continue;
        }
        if (candidate.isDLL()) {
          currentState =
              currentState.abstractIntoDLL(
                  candidate.getObject(),
                  candidate.getSuspectedNfo(),
                  candidate.getSuspectedNfoTargetOffset(),
                  candidate.getSuspectedPfo().orElseThrow(),
                  candidate.getSuspectedPfoTargetPointerOffset().orElseThrow(),
                  ImmutableSet.of());

        } else {
          currentState =
              currentState.abstractIntoSLL(
                  candidate.getObject(),
                  candidate.getSuspectedNfo(),
                  candidate.getSuspectedNfoTargetOffset(),
                  ImmutableSet.of());
        }
      }
    }
    statistics.stopTotalAbstractionTime();
    assert candidatesHaveBeenAbstracted(orderedListCandidatesByNesting, currentState);
    return currentState;
  }

  private boolean candidatesHaveBeenAbstracted(
      List<Set<SMGCandidate>> orderedListCandidatesByNesting, SMGState stateAfterAbstraction)
      throws SMGException {
    PersistentSet<SMGObject> objectsAfterAbstr =
        stateAfterAbstraction.getMemoryModel().getHeapObjects();
    for (Set<SMGCandidate> set : orderedListCandidatesByNesting) {
      for (SMGCandidate candidate : set) {
        if (objectsAfterAbstr.contains(candidate.getObject())) {
          if (!stateAfterAbstraction.getMemoryModel().getSmg().isValid(candidate.getObject())) {
            throw new SMGException(
                "Internal error; Memory has not been cleaned correctly after abstraction. This is"
                    + " not a critical error and can be ignored.");
          } else {
            return false;
          }
        }
      }
    }
    return true;
  }

  private List<Set<SMGCandidate>> getListCandidates() throws SMGException {

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
    Set<SMGCandidate> foundChains = new HashSet<>();
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
            || !smg.getPTEdge(nextPointerValue)
                .orElseThrow()
                .getOffset()
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
            break;
          }
        }
        // Incorrect or not usable next element, maybe a prev exists though,
        //   don't put current in seen list, and it will be found via the prev object
        continue;

      } else {
        List<SMGHasValueEdge> valuesInHeapObj =
            objsAndHVEs.getOrDefault(heapObj, PersistentSet.of()).stream()
                .sorted(Comparator.comparing(SMGHasValueEdge::getOffset))
                .collect(ImmutableList.toImmutableList());
        if (valuesInHeapObj.isEmpty()) {
          // Can't be a list if there is no pointers
          continue;
        }
        BigInteger heapObjSize = heapObj.getSize().asNumericValue().bigIntegerValue();
        SMGCandidateOrRejectedObject maybeCandidate = null;
        // Search through all possible values of the object first, remember to reject the obj if
        // nothing is found
        for (int i = 0; i < valuesInHeapObj.size(); i++) {
          SMGHasValueEdge valueEdge = valuesInHeapObj.get(i);
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
            BigInteger pointerTargetOffset = pointsToEdge.getOffset();

            if (target != heapObj
                && smg.isValid(target)
                && target.getSize().isNumericValue()
                && target.getSize().asNumericValue().bigIntegerValue().equals(heapObjSize)) {

              maybeCandidate =
                  lookThroughObject(
                      heapObj, target, ptrValueOffsetInHeapObj, pointerTargetOffset, alreadySeen);
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
    outerLoop:
    for (SMGCandidate candidate : foundChains) {
      if (candidate.isLooping()) {
        for (SMGObject maybeRoot : candidate.suspectedElements) {
          Set<SMGValue> ptrsTowardsHeapObj = smg.getPointerValuesForTarget(maybeRoot);
          for (SMGValue ptrValue : ptrsTowardsHeapObj) {
            Set<SMGObject> objsWithPtrsTowardsHeapObj =
                smg.getValuesToRegionsTheyAreSavedIn().get(ptrValue).keySet();
            if (!candidate.suspectedElements.containsAll(objsWithPtrsTowardsHeapObj)) {
              foundChainsWRoot.add(SMGCandidate.moveCandidateTo(maybeRoot, candidate));
              continue outerLoop;
            }
          }
        }
      } else {
        foundChainsWRoot.add(candidate);
      }
    }

    // Order the candidates and check for equality of non-linking pointers
    return findNestingOfCandidates(foundChainsWRoot.build());
  }

  private SMGCandidateOrRejectedObject lookThroughObject(
      SMGObject currentObj,
      SMGObject nextObj,
      BigInteger suspectedNfo,
      BigInteger nextPointerTargetOffset,
      Set<SMGObject> alreadySeen)
      throws SMGException {
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
          objsAndHVEs.getOrDefault(nextObj, PersistentSet.of()).stream()
              .sorted(Comparator.comparing(SMGHasValueEdge::getOffset))
              .collect(ImmutableList.toImmutableList());
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
          maybePrevPointerTargetOffset = Optional.of(maybePrevPTE.orElseThrow().getOffset());
        }
      }
    }
    return lookThroughPrevAndThenSearchForList(
        currentObj,
        suspectedNfo,
        nextPointerTargetOffset,
        maybePfo,
        maybePrevPointerTargetOffset,
        alreadySeenInChain);
  }

  private SMGCandidateOrRejectedObject lookThroughPrevAndThenSearchForList(
      SMGObject currentObj,
      BigInteger suspectedNfo,
      BigInteger nextPointerTargetOffset,
      Optional<BigInteger> maybePfo,
      Optional<BigInteger> maybePrevPointerTargetOffset,
      Set<SMGObject> alreadySeenInChain)
      throws SMGException {
    SMG smg = state.getMemoryModel().getSmg();
    int minimumLengthForLists = minimumLengthForListsForAbstraction;
    if (currentObj instanceof SMGSinglyLinkedListSegment sllHeapObj) {
      minimumLengthForLists = minimumLengthForLists - sllHeapObj.getMinLength();
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
    if (!listType.equals(ListType.NONE)) {
      Preconditions.checkArgument(ListType.DLL != listType || !maybePfo.isEmpty());
      maybePfo = listType.equals(ListType.DLL) ? maybePfo : Optional.empty();
      maybePrevPointerTargetOffset =
          listType.equals(ListType.DLL) ? maybePrevPointerTargetOffset : Optional.empty();
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
      if (!currentObj.equals(leftMostObj)) {
        return lookThroughPrevAndThenSearchForList(
            currentObj,
            suspectedNfo,
            nextPointerTargetOffset,
            maybePfo,
            maybePrevPointerTargetOffset,
            alreadySeenInChain);
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
      int remainingMinLength)
      throws SMGException {
    alreadySeenInChain.add(prevObj);
    int reduce = 1;
    if (prevObj instanceof SMGSinglyLinkedListSegment targetSLL) {
      reduce = targetSLL.getMinLength();
    }
    remainingMinLength = remainingMinLength - reduce;
    SMG smg = state.getMemoryModel().getSmg();
    if (!alreadySeenInChain.contains(potentialNextObj)
        && smg.isValid(potentialNextObj)
        && state.getMemoryModel().isHeapObject(potentialNextObj)
        && state.getMemoryModel().isHeapObject(prevObj)
        && potentialNextObj.getSize().isNumericValue()
        && potentialNextObj
            .getSize()
            .asNumericValue()
            .bigIntegerValue()
            .equals(prevObj.getSize().asNumericValue().bigIntegerValue())) {
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
              // if there is a pointer and that pointer points to another valid heap obj of the same
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
            BigInteger pointerTargetOffset = pointsToEdge.getOffset();

            if (nextPointerTargetOffset.equals(pointerTargetOffset)) {
              ImmutableList<BigInteger> exemptOffsetsOfList = ImmutableList.of(maybeNfo);
              if (maybePfo.isPresent()) {
                exemptOffsetsOfList = ImmutableList.of(maybeNfo, maybePfo.orElseThrow());
              }
              if (state.checkEqualValuesForTwoStatesWithExemptions(
                  prevObj,
                  potentialNextObj,
                  exemptOffsetsOfList,
                  state,
                  state,
                  equalityCache,
                  objectCache,
                  true)) {

                // filter out DLLs where we accidentally used the pfo as nfo and are at the "end"
                if (maybeNfo.subtract(smg.getSizeOfPointer()).compareTo(BigInteger.ZERO) >= 0) {
                  // We assume nfo to follow pfo directly
                  SMGHasValueEdge maybeRealNext =
                      smg.readValue(
                              potentialNextObj,
                              maybeNfo.subtract(smg.getSizeOfPointer()),
                              smg.getSizeOfPointer(),
                              false)
                          .getHvEdges()
                          .get(0);
                  if (smg.isPointer(maybeRealNext.hasValue())
                      && smg.getPTEdge(maybeRealNext.hasValue())
                          .orElseThrow()
                          .pointsTo()
                          .equals(prevObj)) {
                    return ListType.NONE;
                  }
                }

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
      if (alreadySeenInChain.contains(potentialNextObj)) {
        return maybePfo.isPresent() ? ListType.LOOPINGDLL : ListType.LOOPINGSLL;
      }
      return maybePfo.isPresent() ? ListType.DLL : ListType.SLL;
    }
    return ListType.NONE;
  }

  // Searches for list elements to the left of currentObj with currentObj suspected of being a list
  // segment. Fills alreadySeenInChain so that all suspected list elements to the left are present
  // in the end. This method only checks that the size, validity, nfo, pfo and pointer offsets
  // match. Does not check values inside of the lists! Also does not check the length of the list!
  private SMGObject lookThroughPrev(
      SMGObject currentObj,
      BigInteger maybeNfo,
      BigInteger nextPointerTargetOffset,
      Optional<BigInteger> maybePfo,
      Optional<BigInteger> maybePrevPointerTargetOffset,
      Set<SMGObject> alreadySeenInChain)
      throws SMGException {
    alreadySeenInChain.add(currentObj);
    SMG smg = state.getMemoryModel().getSmg();
    /* TODO: either remove or re-enable eq check
    ImmutableList<BigInteger> exemptOffsetsOfList = ImmutableList.of(maybeNfo);
    if (maybePfo.isPresent()) {
      exemptOffsetsOfList = ImmutableList.of(maybeNfo, maybePfo.orElseThrow());
    }
    */
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
            || !state.getMemoryModel().isHeapObject(maybePrevObj)
            || !smg.isValid(maybePrevObj)
            || !maybeBackPointerEdge.getOffset().equals(maybePrevPointerTargetOffset.orElseThrow())
            || !maybePrevObj.getSize().isNumericValue()
            || !maybePrevObj
                .getSize()
                .asNumericValue()
                .bigIntegerValue()
                .equals(currentObj.getSize().asNumericValue().bigIntegerValue())) {
          // if (!state.checkEqualValuesForTwoStatesWithExemptions(
          //   currentObj, maybePrevObj, exemptOffsetsOfList, state, state, equalityCache, true)) {
          return currentObj;
          // }
        }
        List<SMGHasValueEdge> valuesInMaybePrevHeapObj =
            smg
                .getSMGObjectsWithSMGHasValueEdges()
                .getOrDefault(maybePrevObj, PersistentSet.of())
                .stream()
                .sorted(Comparator.comparing(SMGHasValueEdge::getOffset))
                .collect(ImmutableList.toImmutableList());

        // Next checking
        for (SMGHasValueEdge maybePrevHVE : valuesInMaybePrevHeapObj) {
          if (maybePrevHVE.getOffset().equals(maybeNfo)) {
            Optional<SMGPointsToEdge> maybePTE = smg.getPTEdge(maybePrevHVE.hasValue());

            if (maybePTE.isPresent()
                && maybePTE.orElseThrow().pointsTo().equals(currentObj)
                && maybePTE.orElseThrow().getOffset().equals(nextPointerTargetOffset)) {
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
      }

    } else {
      Set<SMGValue> ptrsTowardsHeapObj = smg.getPointerValuesForTarget(currentObj);
      for (SMGValue ptrValue : ptrsTowardsHeapObj) {
        Set<SMGObject> objsWithPtrsTowardsHeapObj =
            smg.getValuesToRegionsTheyAreSavedIn().get(ptrValue).keySet();
        SMGPointsToEdge pte = smg.getPTEdge(ptrValue).orElseThrow();
        if (!pte.getOffset().equals(nextPointerTargetOffset)) {
          continue;
        }

        for (SMGObject maybePrevObj : objsWithPtrsTowardsHeapObj) {
          if (alreadySeenInChain.contains(maybePrevObj)
              || !smg.isValid(maybePrevObj)
              || !state.getMemoryModel().isHeapObject(maybePrevObj)
              || !maybePrevObj.getSize().isNumericValue()
              || !maybePrevObj
                  .getSize()
                  .asNumericValue()
                  .bigIntegerValue()
                  .equals(currentObj.getSize().asNumericValue().bigIntegerValue())) {
            continue;
          }
          List<SMGHasValueEdge> valuesInMaybePrevHeapObj =
              smg
                  .getSMGObjectsWithSMGHasValueEdges()
                  .getOrDefault(maybePrevObj, PersistentSet.of())
                  .stream()
                  .sorted(Comparator.comparing(SMGHasValueEdge::getOffset))
                  .collect(ImmutableList.toImmutableList());

          // Next checking
          for (SMGHasValueEdge maybePrevHVE : valuesInMaybePrevHeapObj) {
            if (maybePrevHVE.getOffset().equals(maybeNfo)) {
              Optional<SMGPointsToEdge> maybePTE = smg.getPTEdge(maybePrevHVE.hasValue());

              if (maybePTE.isPresent()
                  && maybePTE
                      .orElseThrow()
                      .equals(pte) // && state.checkEqualValuesForTwoStatesWithExemptions(
              // currentObj, maybePrevObj, exemptOffsetsOfList, state, state, equalityCache, true)
              ) {
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
        }
      }
    }
    return currentObj;
  }

  private List<Set<SMGCandidate>> findNestingOfCandidates(Set<SMGCandidate> trueListStarts) {
    Map<SMGCandidate, Integer> nestingMap = new HashMap<>();
    List<Set<SMGCandidate>> currentOrdering = new ArrayList<>();
    Map<SMGObject, SMGCandidate> objToCandidateMap = new HashMap<>();

    for (SMGCandidate candidate : trueListStarts) {
      nestingMap.put(candidate, 0);
      objToCandidateMap.put(candidate.getObject(), candidate);
      for (SMGObject candidateListObj : candidate.getMaximalListElements()) {
        objToCandidateMap.put(candidateListObj, candidate);
      }
    }

    for (SMGCandidate candidate : trueListStarts) {
      findNestingOfCandidates(
          candidate,
          candidate.getObject(),
          candidate.getSuspectedNfo(),
          candidate.getSuspectedPfo(),
          new HashSet<>(),
          nestingMap,
          objToCandidateMap);
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

  // TODO: do this also for non-candidates in between candidates
  private void findNestingOfCandidates(
      SMGCandidate start,
      SMGObject currentObj,
      BigInteger nfo,
      Optional<BigInteger> maybePfo,
      Set<SMGObject> alreadySeen,
      Map<SMGCandidate, Integer> nestingMap,
      Map<SMGObject, SMGCandidate> objToCandidateMap) {
    if (alreadySeen.contains(currentObj)) {
      return;
    }
    SMG smg = state.getMemoryModel().getSmg();

    SMGPointsToEdge pointsToNext = null;
    // Get next pointer (read would do the same thing...., this way we don't care about sizes)
    for (SMGHasValueEdge hve : smg.getEdges(currentObj)) {
      SMGValue value = hve.hasValue();

      if (smg.isPointer(value)) {
        if (hve.getOffset().compareTo(nfo) == 0) {
          pointsToNext = smg.getPTEdge(value).orElseThrow();
          alreadySeen.add(currentObj);
        } else if (maybePfo.isPresent() && hve.getOffset().compareTo(maybePfo.orElseThrow()) == 0) {
          // Ignore prev pointers
        } else {
          // Points somewhere else, possibly nested list
          SMGPointsToEdge pointsToOther = smg.getPTEdge(value).orElseThrow();

          // This does take into account pointers pointing to nested lists that are not to the
          // root node or some structure that leads to a nested list below
          if (objToCandidateMap.containsKey(pointsToOther.pointsTo())) {
            SMGCandidate candidateForObj = objToCandidateMap.get(pointsToOther.pointsTo());
            if (nestingMap.containsKey(candidateForObj)) {
              // Nesting level of the source of the pointer + 1 or the old if its larger
              int nestingLevelOfNestedList = nestingMap.get(candidateForObj);
              int nestingLevelOfSource = nestingMap.get(start);
              nestingMap.put(
                  candidateForObj, Integer.max(nestingLevelOfNestedList, nestingLevelOfSource) + 1);
            }
          }
        }
      }
    }

    if (pointsToNext != null) {
      findNestingOfCandidates(
          start,
          pointsToNext.pointsTo(),
          nfo,
          maybePfo,
          alreadySeen,
          nestingMap,
          objToCandidateMap);
    }
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
    if (setOfPointers.size() < 1) {
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
    if (setOfPointers.size() < 1) {
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

  private SMGObject findNextObjectForListWithNFO(SMGObject root, BigInteger nfo, SMG smg) {
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
    if (potentialRoot instanceof SMGSinglyLinkedListSegment) {
      pAlreadyVisited.add(potentialRoot);
      SMGSinglyLinkedListSegment sll = (SMGSinglyLinkedListSegment) potentialRoot;
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
        } else if (true) {
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

  protected static class SMGCandidateOrRejectedObject {
    private final SMGObject nonListObj;
    private final SMGCandidate possibleCandidate;

    private SMGCandidateOrRejectedObject(SMGObject pNonListObj, SMGCandidate pPossibleCandidate) {
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
    private SMGObject object;
    private BigInteger suspectedNfo;

    // If not present -> SLL
    private Optional<BigInteger> suspectedPfo;

    /*
     * The offset of the nfo pointer in the target.
     */
    private BigInteger suspectedNfoTargetPointerOffset;

    private Optional<BigInteger> suspectedPfoTargetOffset;

    /*
     * Other suspected list elements. This is the maximal possible over approximation for the same size/nfo!
     * Equalities are not checked and this might not reflect the final abstracted list.
     * Used to find accurate nesting.
     */
    private Set<SMGObject> suspectedElements;

    // Max size. Not checked for abstractable length!
    private int maximalSizeOfList;

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
  }
}
