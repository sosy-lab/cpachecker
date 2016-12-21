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
package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.GenericAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.GenericAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.GenericAbstractionCandidateTemplate;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SMGJoinAbstractionManager {

  private final MachineModel machineModel;

  private final SMGObject smgObject1;
  private final SMGObject smgObject2;
  private final SMGObject destObject;

  private final SMG inputSMG1;
  private final SMG inputSMG2;
  @SuppressWarnings("unused")
  private final SMG destSMG;

  public SMGJoinAbstractionManager(MachineModel pMachineModel, SMGObject pRootInSMG1, SMGObject pRootInSMG2, SMG pInputSMG1,
      SMG pInputSMG2, SMGObject pDestObject, SMG pDestSMG) {
    machineModel = pMachineModel;
    smgObject1 = pRootInSMG1;
    smgObject2 = pRootInSMG2;
    inputSMG1 = pInputSMG1;
    inputSMG2 = pInputSMG2;
    destObject = pDestObject;
    destSMG = pDestSMG;
  }

  public List<SMGAbstractionCandidate> calculateCandidates() {
    Map<Integer, List<SMGAbstractionCandidate>> empty = ImmutableMap.of();
    return calculateCandidates(empty);
  }

  public List<SMGAbstractionCandidate> calculateCandidates(
      Map<Integer, List<SMGAbstractionCandidate>> alreadyFoundCandidates) {

    Optional<GenericAbstractionCandidateTemplate> templateAbstraction =
        calculateTemplateAbstraction(alreadyFoundCandidates);

    if (!templateAbstraction.isPresent()) {
      return ImmutableList.of();
    }

    return ImmutableList.of();
  }

  private Optional<GenericAbstractionCandidateTemplate> calculateTemplateAbstraction(
      Map<Integer, List<SMGAbstractionCandidate>> pAlreadyFoundCandidates) {

    if (destObject instanceof GenericAbstraction) {
      GenericAbstractionCandidateTemplate template =
          ((GenericAbstraction) destObject).createCandidateTemplate(machineModel);
      return Optional.of(template);
    } else if (pAlreadyFoundCandidates.isEmpty()) {
      return calculateSimpleTemplateAbstractionFromObject();
    } else {
      return calculateTemplateAbstractionFromAlreadyFoundAbstractions(pAlreadyFoundCandidates);
    }
  }

  private Optional<GenericAbstractionCandidateTemplate> calculateSimpleTemplateAbstractionFromObject() {

    if (!(destObject instanceof SMGRegion)) {
      return Optional.empty();
    }

    SMGRegion root = (SMGRegion) destObject;

    Set<SMGEdgeHasValue> fieldsOfObject1 = SMGUtils.getFieldsOfObject(smgObject1, inputSMG1);
    Set<SMGEdgeHasValue> fieldsOfObject2 = SMGUtils.getFieldsOfObject(smgObject2, inputSMG2);

    Triple<Set<Pair<SMGEdgeHasValue,SMGEdgeHasValue>>, Set<SMGEdgeHasValue>, Set<SMGEdgeHasValue>> sharedPnonSharedPsharedNP = assignToSharedPPointerAndNonSharedOPointerAndSharedNonPointer(fieldsOfObject1, fieldsOfObject2);

    Set<SMGEdgePointsTo> inboundPointers1 = SMGUtils.getPointerToThisObject(smgObject1, inputSMG1);
    Set<SMGEdgePointsTo> inboundPointers2 = SMGUtils.getPointerToThisObject(smgObject2, inputSMG2);

    Pair<Set<Pair<SMGEdgePointsTo, SMGEdgePointsTo>>, Set<SMGEdgePointsTo>> sharedIPointerNonSharedIP =
        assignToSharedIPointerAndNonSharedIPointer(inboundPointers1, inboundPointers2);

    Set<SMGEdgeHasValue> sharedFields = sharedPnonSharedPsharedNP.getThird();
    Set<Pair<SMGEdgeHasValue, SMGEdgeHasValue>> sharedOPointer =
        sharedPnonSharedPsharedNP.getFirst();
    Set<SMGEdgeHasValue> nonSharedOPointer = sharedPnonSharedPsharedNP.getSecond();
    Set<Pair<SMGEdgePointsTo, SMGEdgePointsTo>> sharedIPointer =
        sharedIPointerNonSharedIP.getFirst();
    Set<SMGEdgePointsTo> nonSharedIPointer = sharedIPointerNonSharedIP.getSecond();

    if (!nonSharedIPointer.isEmpty()) {
      return Optional.empty();
    }

    GenericAbstractionCandidateTemplate result = GenericAbstractionCandidateTemplate
        .createSimpleInductiveGenericAbstractionTemplate(machineModel, sharedFields, sharedIPointer,
            sharedOPointer, nonSharedOPointer, root);
    return Optional.of(result);
  }

  private Pair<Set<Pair<SMGEdgePointsTo, SMGEdgePointsTo>>, Set<SMGEdgePointsTo>> assignToSharedIPointerAndNonSharedIPointer(
      Set<SMGEdgePointsTo> pInboundPointers1, Set<SMGEdgePointsTo> pInboundPointers2) {

    Map<Integer, SMGEdgePointsTo> offsetToPte1Map =
        FluentIterable.from(pInboundPointers1).uniqueIndex(new Function<SMGEdgePointsTo, Integer>() {

          @Override
          public Integer apply(SMGEdgePointsTo pArg0) {
            return pArg0.getOffset();
          }

        });

    Map<Integer, SMGEdgePointsTo> offsetToPte2Map =
        FluentIterable.from(pInboundPointers2).uniqueIndex(new Function<SMGEdgePointsTo, Integer>() {

          @Override
          public Integer apply(SMGEdgePointsTo pArg0) {
            return pArg0.getOffset();
          }

        });

    Set<Integer> offsets = new HashSet<>();
    offsets.addAll(offsetToPte1Map.keySet());
    offsets.addAll(offsetToPte2Map.keySet());

    Set<Pair<SMGEdgePointsTo, SMGEdgePointsTo>> sharedIPointer = new HashSet<>();
    Set<SMGEdgePointsTo> nonSharedIPointer = new HashSet<>();

    for (Integer offset : offsets) {
      if (offsetToPte1Map.containsKey(offset) && offsetToPte2Map.containsKey(offset)) {
        SMGEdgePointsTo pte1 = offsetToPte1Map.get(offset);
        SMGEdgePointsTo pte2 = offsetToPte2Map.get(offset);
        sharedIPointer.add(Pair.of(pte1, pte2));
      } else if (offsetToPte1Map.containsKey(offset)) {
        SMGEdgePointsTo pte1 = offsetToPte1Map.get(offset);
        nonSharedIPointer.add(pte1);
      } else {
        SMGEdgePointsTo pte2 = offsetToPte2Map.get(offset);
        nonSharedIPointer.add(pte2);
      }
    }

    Pair<Set<Pair<SMGEdgePointsTo, SMGEdgePointsTo>>, Set<SMGEdgePointsTo>> result =
        Pair.of(sharedIPointer, nonSharedIPointer);
    return result;
  }

  private Triple<Set<Pair<SMGEdgeHasValue, SMGEdgeHasValue>>, Set<SMGEdgeHasValue>, Set<SMGEdgeHasValue>> assignToSharedPPointerAndNonSharedOPointerAndSharedNonPointer(
      Set<SMGEdgeHasValue> pFieldsOfObject1, Set<SMGEdgeHasValue> pFieldsOfObject2) {

    Set<Pair<SMGEdgeHasValue, SMGEdgeHasValue>> sharedOPointer = new HashSet<>();
    Set<SMGEdgeHasValue> nonSharedOPointer = new HashSet<>();
    Set<SMGEdgeHasValue> sharedNonPointer = new HashSet<>();

    Map<Integer, SMGEdgeHasValue> offsetToHve1Map =
        FluentIterable.from(pFieldsOfObject1).uniqueIndex(new Function<SMGEdgeHasValue, Integer>() {

          @Override
          public Integer apply(SMGEdgeHasValue pArg0) {
            return pArg0.getOffset();
          }

        });

    Map<Integer, SMGEdgeHasValue> offsetToHve2Map =
        FluentIterable.from(pFieldsOfObject2).uniqueIndex(new Function<SMGEdgeHasValue, Integer>() {

          @Override
          public Integer apply(SMGEdgeHasValue pArg0) {
            return pArg0.getOffset();
          }

        });

    Set<Integer> offsets = new HashSet<>();
    offsets.addAll(offsetToHve1Map.keySet());
    offsets.addAll(offsetToHve2Map.keySet());

    /*
     * Assign each pointer to shared pointer, if both smg contain this pointer,
     * non shared pointer, if only one smg contains the pointer, ans shared value, if
     * the shared value is no pointer.
     *
     */
    for (Integer offset : offsets) {

      if (offsetToHve1Map.containsKey(offset) && offsetToHve2Map.containsKey(offset)) {
        SMGEdgeHasValue hve1 = offsetToHve1Map.get(offset);
        SMGEdgeHasValue hve2 = offsetToHve2Map.get(offset);

        if (inputSMG1.isPointer(hve1.getValue()) && inputSMG2.isPointer(hve2.getValue())) {
          sharedOPointer.add(Pair.of(hve1, hve2));
        } else if(inputSMG1.isPointer(hve1.getValue())) {
          nonSharedOPointer.add(hve1);
        } else if(inputSMG2.isPointer(hve2.getValue())) {
          nonSharedOPointer.add(hve2);
        } else {
          sharedNonPointer.add(hve1);
        }
      } else if (offsetToHve1Map.containsKey(offset)) {
        SMGEdgeHasValue hve = offsetToHve1Map.get(offset);
        if (inputSMG1.isPointer(hve.getValue())) {
          nonSharedOPointer.add(hve);
        }
      } else {
        SMGEdgeHasValue hve = offsetToHve2Map.get(offset);
        if (inputSMG2.isPointer(hve.getValue())) {
          nonSharedOPointer.add(hve);
        }
      }
    }

    return Triple.of(sharedOPointer, nonSharedOPointer, sharedNonPointer);
  }

  private Optional<GenericAbstractionCandidateTemplate> calculateTemplateAbstractionFromAlreadyFoundAbstractions(
      Map<Integer, List<SMGAbstractionCandidate>> pAlreadyFoundCandidates) {

    SMGAbstractionCandidate template =
        pAlreadyFoundCandidates.values().iterator().next().iterator().next();

    if (template instanceof GenericAbstractionCandidate) {
      return Optional.of(((GenericAbstractionCandidate) template).createTemplate(machineModel));
    } else {
      return Optional.empty();
    }
  }
}