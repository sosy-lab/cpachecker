// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.GenericAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.GenericAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.GenericAbstractionCandidateTemplate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.MaterlisationStep;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.MaterlisationStep.FieldsOfTemplate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.SMGEdgeHasValueTemplate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.SMGEdgeHasValueTemplateWithConcreteValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.SMGEdgePointsToTemplate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.SMGObjectTemplate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

@SuppressWarnings("unused")
public class SMGJoinSubSMGsIntoGenericAbstraction {

  private final MachineModel machineModel;
  private final GenericAbstractionCandidateTemplate template;
  private final UnmodifiableSMG inputSMG1;
  private final UnmodifiableSMG inputSMG2;
  private final SMGObject rootObject1;
  private final SMGObject rootObject2;
  private final SMGNodeMapping mapping1;
  private final SMGNodeMapping mapping2;
  private final Map<SMGValue, List<GenericAbstractionCandidate>> previouslyMatched;

  public SMGJoinSubSMGsIntoGenericAbstraction(
      MachineModel pMachineModel,
      UnmodifiableSMG pInputSMG1,
      UnmodifiableSMG pInputSMG2,
      GenericAbstractionCandidateTemplate pTemplate,
      SMGObject pRootObject1,
      SMGObject pRootObject2,
      SMGNodeMapping pMapping1,
      SMGNodeMapping pMapping2,
      Map<SMGValue, List<GenericAbstractionCandidate>> pValueAbstractionCandidates) {
    machineModel = pMachineModel;
    template = pTemplate;
    inputSMG1 = pInputSMG1;
    inputSMG2 = pInputSMG2;
    rootObject1 = pRootObject1;
    rootObject2 = pRootObject2;
    mapping1 = pMapping1;
    mapping2 = pMapping2;
    previouslyMatched = pValueAbstractionCandidates;
  }

  public Optional<GenericAbstractionCandidate> joinSubSmgsIntoGenericAbstractionCandidate() {

    MatchResult matchResult =
        subSMGmatchSpecificShape(inputSMG1, rootObject1, mapping1, template, new HashSet<>());

    if (!matchResult.isMatch()) {
      return Optional.empty();
    }

    MatchResult matchResult2 =
        subSMGmatchSpecificShape(inputSMG2, rootObject2, mapping2, template, new HashSet<>());

    if (!matchResult.isMatch()) {
      return Optional.empty();
    }

    return createGenericAbstractionCandidate(matchResult, matchResult2);
  }

  private Optional<GenericAbstractionCandidate> createGenericAbstractionCandidate(
      MatchResult pMatchResult, MatchResult pMatchResult2) {

    MatchResultBuilder builder = new MatchResultBuilder(template);

    builder
        .getAbstractToConcretePointerMap()
        .putAll(pMatchResult.getAbstractToConcretePointerMap());

    for (Entry<SMGValue, SMGValue> entry :
        pMatchResult.getAbstractToConcretePointerMapInputSMG().entrySet()) {

      SMGValue absVal = entry.getKey();
      SMGValue val = entry.getValue();

      if (mapping1.containsKey(val)) {
        builder.putAbstractToConcretePointerMap(absVal, mapping1.get(val));
      } else {
        return Optional.empty();
      }
    }

    builder
        .getObjectsToBeRemovedForAbstraction()
        .addAll(pMatchResult.getObjectsToBeRemovedForAbstraction());

    for (SMGObject obj : pMatchResult.getObjectsToBeRemovedForAbstractionInputSMG()) {
      if (mapping1.containsKey(obj)) {
        builder.addObjectsToBeRemovedForAbstraction(mapping1.get(obj));
      }
    }

    for (SMGObject obj : pMatchResult2.getObjectsToBeRemovedForAbstractionInputSMG()) {
      if (mapping2.containsKey(obj)
          && !builder.getObjectsToBeRemovedForAbstraction().contains(mapping2.get(obj))) {
        return Optional.empty();
      }
    }

    int score = Math.max(pMatchResult.getScore(), pMatchResult2.getScore());

    MatchResult destres = builder.build();

    return Optional.of(
        GenericAbstractionCandidate.valueOf(
            machineModel,
            destres.getObjectsToBeRemovedForAbstraction(),
            destres.getAbstractToConcretePointerMap(),
            template.getMaterlisationStepMap(),
            score));
  }

  private MatchResult subSMGmatchSpecificShape(
      UnmodifiableSMG pInputSMG,
      SMGObject pRootObject,
      SMGNodeMapping pMapping,
      GenericAbstractionCandidateTemplate pTemplate,
      Set<SMGObject> pAlreadyVisited) {

    if (pRootObject instanceof GenericAbstraction) {
      return subSMGmatchSpecificShape((GenericAbstraction) pRootObject, pTemplate, pAlreadyVisited);
    } else if (pRootObject instanceof SMGRegion) {
      return subSMGmatchSpecificShape(pInputSMG, pRootObject, pMapping, pTemplate, pAlreadyVisited);
    } else {
      return MatchResult.getUnknownInstance();
    }
  }

  private MatchResult subSMGmatchSpecificShape(
      GenericAbstraction pRootObject,
      GenericAbstractionCandidateTemplate pTemplate,
      Set<SMGObject> pAlreadyVisited) {

    GenericAbstractionCandidateTemplate rootObjectTemaplate =
        pRootObject.createCandidateTemplate(machineModel);

    if (rootObjectTemaplate.isSpecificShape(pTemplate)) {
      return MatchResult.valueOf(machineModel, pRootObject);
    } else {
      return MatchResult.getUnknownInstance();
    }
  }

  private MatchResult subSMGmatchSpecificShape(
      UnmodifiableSMG pInputSMG,
      SMGRegion pRootObject,
      SMGNodeMapping pMapping,
      GenericAbstractionCandidateTemplate pTemplate,
      Set<SMGObject> pAlreadyVisited) {

    MatchResult matchedPreviously =
        wasMatchedPreviously(pInputSMG, pRootObject, pTemplate, pMapping);

    if (matchedPreviously.isMatch()) {
      return matchedPreviously;
    }

    Set<MaterlisationStep> toBeChecked = pTemplate.getMaterlisationSteps();
    Set<MaterlisationStep> stopStep = new HashSet<>();

    MatchResult result = MatchResult.getUnknownInstance();

    /*First, match for continue step, then match for stop step to
     * join as much of the smg into the generic abstraction as possible.*/
    for (MaterlisationStep matStep : toBeChecked) {
      if (!matStep.isStopStep()) {
        result =
            subSMGmatchStep(matStep, pInputSMG, pRootObject, pMapping, pAlreadyVisited, pTemplate);

        if (result.isMatch()) {
          return result;
        }
      } else {
        stopStep.add(matStep);
      }
    }

    for (MaterlisationStep matStep : stopStep) {
      result =
          subSMGmatchStep(matStep, pInputSMG, pRootObject, pMapping, pAlreadyVisited, pTemplate);

      if (result.isMatch()) {
        return result;
      }
    }

    return result;
  }

  private MatchResult subSMGmatchStep(
      MaterlisationStep pMatStep,
      UnmodifiableSMG pInputSMG,
      SMGRegion pRootObject,
      SMGNodeMapping pMapping,
      Set<SMGObject> pAlreadyVisited,
      GenericAbstractionCandidateTemplate pTemplate) {

    Set<SMGRegion> entryRegions = pMatStep.getEntryRegions();
    Set<SMGObject> alreadyVisited = new HashSet<>(pAlreadyVisited);

    MatchResult result;

    for (SMGObjectTemplate entryRegion : entryRegions) {

      result =
          subSMGmatchStep(
              pMatStep, pInputSMG, pRootObject, entryRegion, pMapping, alreadyVisited, pTemplate);

      if (result.isMatch()) {
        return result;
      }
    }

    return MatchResult.getUnknownInstance();
  }

  private MatchResult subSMGmatchStep(
      MaterlisationStep pMatStep,
      UnmodifiableSMG pInputSMG,
      SMGRegion pRootObject,
      SMGObjectTemplate pEntryRegion,
      SMGNodeMapping pMapping,
      Set<SMGObject> pAlreadyVisited,
      GenericAbstractionCandidateTemplate pTemplate) {

    /*If entry  region is abstraction, use different matstep to avoid endless loop*/
    if (pEntryRegion instanceof GenericAbstractionCandidateTemplate) {
      return MatchResult.getUnknownInstance();
    }

    MaterilisationStepToSubSMGMap templateToInputSmgMapping = new MaterilisationStepToSubSMGMap();
    templateToInputSmgMapping.put(pEntryRegion, pRootObject);
    pAlreadyVisited.add(pRootObject);
    Set<SMGNodeTemplateAndNode> toBeMatchedLater = new HashSet<>();
    toBeMatchedLater.add(
        new SMGNodeTemplateAndNode(new SMGNodeTemplate(pEntryRegion), new SMGNode(pRootObject)));

    while (!toBeMatchedLater.isEmpty()) {
      Set<SMGNodeTemplateAndNode> toBeMatched = ImmutableSet.copyOf(toBeMatchedLater);
      toBeMatchedLater = new HashSet<>();

      for (SMGNodeTemplateAndNode templateNodeAndConcreteNode : toBeMatched) {

        SMGNode node = templateNodeAndConcreteNode.getNode();
        SMGNodeTemplate nodeTemplate = templateNodeAndConcreteNode.getNodeTemplate();

        boolean match =
            matchNodeTemplateWithNode(
                node,
                nodeTemplate,
                pMatStep,
                templateToInputSmgMapping,
                pInputSMG,
                toBeMatchedLater,
                pAlreadyVisited,
                pMapping);

        if (!match) {
          return MatchResult.getUnknownInstance();
        }
      }
    }

    MatchResult result = constructAbstraction(pTemplate, templateToInputSmgMapping, pMatStep);

    return result;
  }

  private MatchResult constructAbstraction(
      GenericAbstractionCandidateTemplate pTemplate,
      MaterilisationStepToSubSMGMap pTemplateToInputSmgMapping,
      MaterlisationStep pMatStep) {

    MatchResultBuilder matchResultBuilder = new MatchResultBuilder(pTemplate);

    matchResultBuilder.setMatchTrue();

    for (SMGObject newObjects : pTemplateToInputSmgMapping.objectTemplateToObject.values()) {
      matchResultBuilder.addObjectsToBeRemovedForAbstractionInputSMG(newObjects);
    }

    for (Entry<SMGValue, SMGValue> values :
        pTemplateToInputSmgMapping.getValueTemplateToValue().entrySet()) {
      SMGValue absVal = values.getKey();
      SMGValue conVal = values.getValue();

      if (pMatStep.abstractInterfaceContains(absVal)) {
        matchResultBuilder.putAbstractToConcretePointerMapInputSMG(absVal, conVal);
      }
    }

    int score = 0;

    for (MatchResult prevAbs : pTemplateToInputSmgMapping.getAbstractionToMatchMap().values()) {

      matchResultBuilder
          .getObjectsToBeRemovedForAbstraction()
          .addAll(prevAbs.getObjectsToBeRemovedForAbstraction());
      matchResultBuilder
          .getObjectsToBeRemovedForAbstractionInputSMG()
          .addAll(prevAbs.getObjectsToBeRemovedForAbstractionInputSMG());

      if (prevAbs.getScore() > score) {
        score = prevAbs.getScore();
      }
    }

    score++;
    matchResultBuilder.setScore(score);

    return matchResultBuilder.build();
  }

  private boolean matchNodeTemplateWithNode(
      SMGNode pNode,
      SMGNodeTemplate pNodeTemplate,
      MaterlisationStep pMatStep,
      MaterilisationStepToSubSMGMap pTemplateToInputSmgMapping,
      UnmodifiableSMG pInputSMG,
      Set<SMGNodeTemplateAndNode> pToBeMatchedLater,
      Set<SMGObject> pAlreadyVisited,
      SMGNodeMapping pMapping) {

    if (pNode.isValue() && pNodeTemplate.isValueTemplate()) {
      return matchValueTemplateWithValue(
          pNode.getValue(),
          pNodeTemplate.getValueTemplate(),
          pMatStep,
          pTemplateToInputSmgMapping,
          pInputSMG,
          pToBeMatchedLater,
          pAlreadyVisited,
          pMapping);
    } else if (pNode.isObject() && pNodeTemplate.isObjectTemplate()) {

      SMGObject object = pNode.getObject();
      SMGObjectTemplate objectTemplate = pNodeTemplate.getObjectTemplate();

      if (objectTemplate instanceof GenericAbstractionCandidateTemplate) {
        GenericAbstractionCandidateTemplate genAbsTmp =
            (GenericAbstractionCandidateTemplate) objectTemplate;

        return matchGenericAbstractionTemplateWithGenericAbstraction(
            genAbsTmp, pInputSMG, pToBeMatchedLater, pTemplateToInputSmgMapping, pMatStep);
      } else if (objectTemplate instanceof SMGRegion) {
        SMGRegion regionTemplate = (SMGRegion) objectTemplate;
        return matchRegionTemplateWithObject(
            regionTemplate,
            object,
            pInputSMG,
            pToBeMatchedLater,
            pTemplateToInputSmgMapping,
            pMatStep);
      }
    }

    return false;
  }

  private boolean matchGenericAbstractionTemplateWithGenericAbstraction(
      GenericAbstractionCandidateTemplate pGenAbsTmp,
      UnmodifiableSMG pInputSMG,
      Set<SMGNodeTemplateAndNode> pToBeMatchedLater,
      MaterilisationStepToSubSMGMap pTemplateToInputSmgMapping,
      MaterlisationStep pMatStep) {

    if (!pTemplateToInputSmgMapping.containsAbstraction(pGenAbsTmp)) {
      return false;
    }

    MatchResult genAbsMatchResult = pTemplateToInputSmgMapping.getAbstraction(pGenAbsTmp);

    if (!genAbsMatchResult.isMatch()) {
      return false;
    }

    Set<SMGEdgePointsTo> pointerToAbstraction =
        getPointerToThisAbstraction(genAbsMatchResult, pInputSMG);
    Set<SMGEdgePointsToTemplate> pointerToRegionTemplate =
        pMatStep.getPointerToThisTemplate(pGenAbsTmp);

    if (pointerToAbstraction.size() != pointerToRegionTemplate.size()) {
      return false;
    }

    Map<Long, SMGEdgePointsTo> pointerToRegionMap =
        Maps.uniqueIndex(pointerToAbstraction, SMGEdgePointsTo::getOffset);

    Map<Long, SMGEdgePointsToTemplate> pointerToRegionTemplateMap =
        Maps.uniqueIndex(pointerToRegionTemplate, SMGEdgePointsToTemplate::getOffset);

    for (Entry<Long, SMGEdgePointsToTemplate> ptEntry : pointerToRegionTemplateMap.entrySet()) {
      long offset = ptEntry.getKey();
      SMGEdgePointsToTemplate pointerTemplateEdge = ptEntry.getValue();

      if (!pointerToRegionMap.containsKey(offset)) {
        return false;
      }

      SMGValue pointerTemplate = pointerTemplateEdge.getAbstractValue();
      SMGValue pointer = pointerToRegionMap.get(offset).getValue();

      if (pTemplateToInputSmgMapping.contains(pointerTemplate)) {
        if (!pTemplateToInputSmgMapping.get(pointerTemplate).equals(pointer)) {
          return false;
        }
      } else {
        pTemplateToInputSmgMapping.put(pointerTemplate, pointer);
        if (pMatStep.getAbstractPointers().contains(pointerTemplateEdge)) {
          pToBeMatchedLater.add(new SMGNodeTemplateAndNode(pointerTemplate, pointer));
        }
      }
    }

    Set<SMGEdgeHasValue> fieldsOfRegion = getFieldsOfGenAbs(genAbsMatchResult, pInputSMG);
    FieldsOfTemplate fieldsOfTemplate = pMatStep.getFieldsOfThisTemplate(pGenAbsTmp);

    if (fieldsOfRegion.size() != fieldsOfTemplate.size()) {
      return false;
    }

    Map<Long, SMGEdgeHasValue> fieldsOfRegionMap =
        Maps.uniqueIndex(fieldsOfRegion, SMGEdgeHasValue::getOffset);

    Set<SMGEdgeHasValueTemplate> fieldsOfTemplateSet =
        new HashSet<>(fieldsOfTemplate.getFieldTemplateContainingPointer());
    fieldsOfTemplateSet.addAll(fieldsOfTemplate.getFieldTemplateContainingPointerTemplate());

    Map<Long, SMGEdgeHasValueTemplate> fieldsOfRegionTemplateMap =
        Maps.uniqueIndex(fieldsOfTemplateSet, SMGEdgeHasValueTemplate::getOffset);

    Map<Long, SMGEdgeHasValueTemplateWithConcreteValue> fieldsOfRegionTemplateCVMap =
        Maps.uniqueIndex(
            fieldsOfTemplate.getFieldTemplateContainingValue(),
            SMGEdgeHasValueTemplateWithConcreteValue::getOffset);

    for (Entry<Long, SMGEdgeHasValue> hveEntry : fieldsOfRegionMap.entrySet()) {

      long offset = hveEntry.getKey();
      SMGEdgeHasValue hve = hveEntry.getValue();

      if (fieldsOfRegionTemplateMap.containsKey(offset)) {

        SMGValue pointerTemplate = fieldsOfRegionTemplateMap.get(offset).getAbstractValue();
        SMGValue pointer = hve.getValue();

        if (pTemplateToInputSmgMapping.contains(pointerTemplate)) {
          if (pTemplateToInputSmgMapping.get(pointerTemplate) != pointer) {
            return false;
          }
        } else {
          pTemplateToInputSmgMapping.put(pointerTemplate, pointer);
          if (pMatStep
              .getAbstractFieldsToIPointer()
              .contains(fieldsOfRegionTemplateMap.get(offset))) {
            pToBeMatchedLater.add(new SMGNodeTemplateAndNode(pointerTemplate, pointer));
          }
        }

      } else if (fieldsOfRegionTemplateCVMap.containsKey(offset)) {

        SMGValue value = hve.getValue();
        SMGValue valueInTemplate = fieldsOfRegionTemplateCVMap.get(offset).getValue();

        if (value != valueInTemplate) {
          return false;
        }

      } else {
        return false;
      }
    }

    return true;
  }

  private Set<SMGEdgeHasValue> getFieldsOfGenAbs(
      MatchResult pGenAbsMatchResult, UnmodifiableSMG pInputSMG) {

    GenericAbstractionCandidateTemplate abstractionTemplate =
        pGenAbsMatchResult.getGenAbsTemplate();

    Map<SMGValue, SMGValue> abstractToConcreteMap =
        pGenAbsMatchResult.getAbstractToConcretePointerMapInputSMG();

    Set<SMGEdgeHasValueTemplate> pointerToThisAbstraction =
        abstractionTemplate.getMaterlisationSteps().iterator().next().getAbstractFieldsToOPointer();

    Set<SMGEdgeHasValue> result = new HashSet<>();
    for (SMGEdgeHasValueTemplate fieldTmp : pointerToThisAbstraction) {
      SMGValue absVal = fieldTmp.getAbstractValue();
      SMGValue concreteValue = abstractToConcreteMap.get(absVal);
      Iterables.addAll(result, SMGUtils.getFieldsofThisValue(concreteValue, pInputSMG));
    }
    return result;
  }

  private Set<SMGEdgePointsTo> getPointerToThisAbstraction(
      MatchResult pGenAbsMatchResult, UnmodifiableSMG pInputSMG) {

    GenericAbstractionCandidateTemplate abstractionTemplate =
        pGenAbsMatchResult.getGenAbsTemplate();

    Map<SMGValue, SMGValue> abstractToConcreteMap =
        pGenAbsMatchResult.getAbstractToConcretePointerMapInputSMG();

    Set<SMGEdgePointsToTemplate> pointerToThisAbstraction =
        abstractionTemplate
            .getMaterlisationSteps()
            .iterator()
            .next()
            .getAbstractAdressesToOPointer();

    Set<SMGEdgePointsTo> result = new HashSet<>();
    for (SMGEdgePointsToTemplate pointerTmp : pointerToThisAbstraction) {
      SMGValue absVal = pointerTmp.getAbstractValue();
      SMGValue concreteValue = abstractToConcreteMap.get(absVal);
      result.add(pInputSMG.getPointer(concreteValue));
    }
    return result;
  }

  private boolean matchValueTemplateWithValue(
      SMGValue pValue,
      SMGValue pValueTemplate,
      MaterlisationStep pMatStep,
      MaterilisationStepToSubSMGMap pTemplateToInputSmgMapping,
      UnmodifiableSMG pInputSMG,
      Set<SMGNodeTemplateAndNode> pToBeMatchedLater,
      Set<SMGObject> pAlreadyVisited,
      SMGNodeMapping pMapping) {

    if (!pTemplateToInputSmgMapping.contains(pValueTemplate)
        || pTemplateToInputSmgMapping.get(pValueTemplate) != pValue) {
      return false;
    }

    if (pInputSMG.isPointer(pValue)) {

      SMGEdgePointsTo pointerEdge = pInputSMG.getPointer(pValue);
      Optional<SMGEdgePointsToTemplate> pointerEdgeTemplateOpt =
          pMatStep.getPointer(pValueTemplate);

      if (!pointerEdgeTemplateOpt.isPresent()) {
        return false;
      }

      SMGEdgePointsToTemplate pointerEdgeTemplate = pointerEdgeTemplateOpt.orElseThrow();

      if (pointerEdgeTemplate.getOffset() != pointerEdge.getOffset()) {
        return false;
      }

      SMGObjectTemplate targetTemplate = pointerEdgeTemplate.getObjectTemplate();
      SMGObject target = pointerEdge.getObject();

      if (pTemplateToInputSmgMapping.contains(targetTemplate)) {
        SMGObject mappedTarget = pTemplateToInputSmgMapping.get(targetTemplate);

        if (mappedTarget != target) {
          return false;
        }
      } else {
        if (targetTemplate instanceof GenericAbstractionCandidateTemplate) {
          GenericAbstractionCandidateTemplate genAbs =
              (GenericAbstractionCandidateTemplate) targetTemplate;

          MatchResult result;
          if (pTemplateToInputSmgMapping.containsAbstraction(genAbs)) {
            result = pTemplateToInputSmgMapping.getAbstraction(genAbs);
          } else {
            result = subSMGmatchSpecificShape(pInputSMG, target, pMapping, genAbs, pAlreadyVisited);
            pTemplateToInputSmgMapping.putAbstraction(genAbs, result);
            pToBeMatchedLater.add(new SMGNodeTemplateAndNode(targetTemplate, target));
          }

          if (!result.isMatch()) {
            return false;
          }

          // matched when matching generic abstraction

        } else {
          pAlreadyVisited.add(target);
          pToBeMatchedLater.add(new SMGNodeTemplateAndNode(targetTemplate, target));
          pTemplateToInputSmgMapping.put(targetTemplate, target);
        }
      }

    } else {
      if (pMatStep.getPointer(pValueTemplate).isPresent()) {
        return false;
      }
    }

    Iterable<SMGEdgeHasValue> fields = SMGUtils.getFieldsofThisValue(pValue, pInputSMG);

    Set<SMGEdgeHasValueTemplate> fieldsTemplate = pMatStep.getFieldsOfValue(pValueTemplate);

    if (Iterables.size(fields) != fieldsTemplate.size()) {
      return false;
    }

    Map<Long, SMGEdgeHasValue> fieldOffsetMap =
        Maps.uniqueIndex(fields, SMGEdgeHasValue::getOffset);

    Map<Long, SMGEdgeHasValueTemplate> fieldOffsetTemplateMap =
        Maps.uniqueIndex(fieldsTemplate, SMGEdgeHasValueTemplate::getOffset);

    for (Entry<Long, SMGEdgeHasValueTemplate> hveTmpEntry : fieldOffsetTemplateMap.entrySet()) {

      long offset = hveTmpEntry.getKey();
      SMGEdgeHasValueTemplate pointerEdgeTemplate = hveTmpEntry.getValue();

      if (!fieldOffsetMap.containsKey(offset)) {
        return false;
      }

      SMGEdgeHasValue pointerEdge = fieldOffsetMap.get(offset);
      SMGObjectTemplate targetTemplate = pointerEdgeTemplate.getObjectTemplate();
      SMGObject target = pointerEdge.getObject();

      if (pTemplateToInputSmgMapping.contains(targetTemplate)) {
        SMGObject mappedTarget = pTemplateToInputSmgMapping.get(targetTemplate);

        if (mappedTarget != target) {
          return false;
        }
      } else {
        if (targetTemplate instanceof GenericAbstractionCandidateTemplate) {
          GenericAbstractionCandidateTemplate genAbs =
              (GenericAbstractionCandidateTemplate) targetTemplate;

          MatchResult result;
          if (pTemplateToInputSmgMapping.containsAbstraction(genAbs)) {
            result = pTemplateToInputSmgMapping.getAbstraction(genAbs);
          } else {
            result = subSMGmatchSpecificShape(pInputSMG, target, pMapping, genAbs, pAlreadyVisited);
            pTemplateToInputSmgMapping.putAbstraction(genAbs, result);
            pToBeMatchedLater.add(new SMGNodeTemplateAndNode(targetTemplate, target));
          }

          if (!result.isMatch()) {
            return false;
          }

          // matched when matching generic abstraction

        } else {
          pAlreadyVisited.add(target);
          pToBeMatchedLater.add(new SMGNodeTemplateAndNode(targetTemplate, target));
          pTemplateToInputSmgMapping.put(targetTemplate, target);
        }
      }
    }

    return true;
  }

  private boolean matchRegionTemplateWithObject(
      SMGRegion pTemplate,
      SMGObject pObject,
      UnmodifiableSMG pInputSMG,
      Set<SMGNodeTemplateAndNode> pToBeMatchedLater,
      MaterilisationStepToSubSMGMap pMatStepToSubSMGMapping,
      MaterlisationStep matStep) {

    if (!(pObject instanceof SMGRegion)) {
      return false;
    }

    SMGRegion region = (SMGRegion) pObject;

    if (region.getSize() != pTemplate.getSize()) {
      return false;
    }

    Set<SMGEdgePointsTo> pointerToRegion = SMGUtils.getPointerToThisObject(region, pInputSMG);
    Set<SMGEdgePointsToTemplate> pointerToRegionTemplate =
        matStep.getPointerToThisTemplate(pTemplate);

    if (pointerToRegion.size() != pointerToRegionTemplate.size()) {
      return false;
    }

    Map<Long, SMGEdgePointsTo> pointerToRegionMap =
        Maps.uniqueIndex(pointerToRegion, SMGEdgePointsTo::getOffset);

    Map<Long, SMGEdgePointsToTemplate> pointerToRegionTemplateMap =
        Maps.uniqueIndex(pointerToRegionTemplate, SMGEdgePointsToTemplate::getOffset);

    for (Entry<Long, SMGEdgePointsToTemplate> pteTmp : pointerToRegionTemplateMap.entrySet()) {

      long offset = pteTmp.getKey();
      SMGEdgePointsToTemplate ptTmp = pteTmp.getValue();

      if (!pointerToRegionMap.containsKey(offset)) {
        return false;
      }

      SMGValue pointerTemplate = ptTmp.getAbstractValue();
      SMGValue pointer = pointerToRegionMap.get(offset).getValue();

      if (pMatStepToSubSMGMapping.contains(pointerTemplate)) {
        if (!pMatStepToSubSMGMapping.get(pointerTemplate).equals(pointer)) {
          return false;
        }
      } else {
        pMatStepToSubSMGMapping.put(pointerTemplate, pointer);
        if (matStep.getAbstractPointers().contains(ptTmp)) {
          pToBeMatchedLater.add(new SMGNodeTemplateAndNode(pointerTemplate, pointer));
        }
      }
    }

    SMGHasValueEdges fieldsOfRegion = SMGUtils.getFieldsOfObject(region, pInputSMG);
    FieldsOfTemplate fieldsOfTemplate = matStep.getFieldsOfThisTemplate(pTemplate);

    if (fieldsOfRegion.size() != fieldsOfTemplate.size()) {
      return false;
    }

    Map<Long, SMGEdgeHasValue> fieldsOfRegionMap =
        Maps.uniqueIndex(fieldsOfRegion, SMGEdgeHasValue::getOffset);

    Set<SMGEdgeHasValueTemplate> fieldsOfTemplateSet =
        new HashSet<>(fieldsOfTemplate.getFieldTemplateContainingPointer());
    fieldsOfTemplateSet.addAll(fieldsOfTemplate.getFieldTemplateContainingPointerTemplate());

    Map<Long, SMGEdgeHasValueTemplate> fieldsOfRegionTemplateMap =
        Maps.uniqueIndex(fieldsOfTemplateSet, SMGEdgeHasValueTemplate::getOffset);

    Map<Long, SMGEdgeHasValueTemplateWithConcreteValue> fieldsOfRegionTemplateCVMap =
        Maps.uniqueIndex(
            fieldsOfTemplate.getFieldTemplateContainingValue(),
            SMGEdgeHasValueTemplateWithConcreteValue::getOffset);

    for (Entry<Long, SMGEdgeHasValue> hveEntry : fieldsOfRegionMap.entrySet()) {

      long offset = hveEntry.getKey();
      SMGEdgeHasValue hve = hveEntry.getValue();

      if (fieldsOfRegionTemplateMap.containsKey(offset)) {

        SMGValue pointerTemplate = fieldsOfRegionTemplateMap.get(offset).getAbstractValue();
        SMGValue pointer = hve.getValue();

        if (pMatStepToSubSMGMapping.contains(pointerTemplate)) {
          if (!pMatStepToSubSMGMapping.get(pointerTemplate).equals(pointer)) {
            return false;
          }
        } else {
          pMatStepToSubSMGMapping.put(pointerTemplate, pointer);
          if (matStep
              .getAbstractFieldsToIPointer()
              .contains(fieldsOfRegionTemplateMap.get(offset))) {
            pToBeMatchedLater.add(new SMGNodeTemplateAndNode(pointerTemplate, pointer));
          }
        }

      } else if (fieldsOfRegionTemplateCVMap.containsKey(offset)) {

        SMGValue value = hve.getValue();
        SMGValue valueInTemplate = fieldsOfRegionTemplateCVMap.get(offset).getValue();

        if (value != valueInTemplate) {
          return false;
        }

      } else {
        return false;
      }
    }

    return true;
  }

  private MatchResult wasMatchedPreviously(
      UnmodifiableSMG pInputSMG,
      SMGObject pRootObject,
      GenericAbstractionCandidateTemplate pTemplate,
      SMGNodeMapping pMapping) {

    Set<SMGEdgePointsTo> pointsToThisObject =
        SMGUtils.getPointerToThisObject(pRootObject, pInputSMG);

    for (SMGEdgePointsTo pointer : pointsToThisObject) {
      if (pMapping.containsKey(pointer.getValue())) {
        SMGValue destPointerValue = pMapping.get(pointer.getValue());

        if (previouslyMatched.containsKey(destPointerValue)) {
          for (GenericAbstractionCandidate abstractionCandidate :
              previouslyMatched.get(destPointerValue)) {

            if (pTemplate.equals(abstractionCandidate.createTemplate(machineModel))) {
              return MatchResult.valueOf(machineModel, abstractionCandidate);
            }
          }
        }
      }
    }

    return MatchResult.getUnknownInstance();
  }

  private static class MatchResultBuilder {

    private boolean match = false;
    private Set<SMGObject> objectsToBeRemovedForAbstraction = new HashSet<>();
    private Set<SMGObject> objectsToBeRemovedForAbstractionInputSMG = new HashSet<>();
    private int score = 0;
    private Map<SMGValue, SMGValue> abstractToConcretePointerMap = new HashMap<>();
    private Map<SMGValue, SMGValue> abstractToConcretePointerMapInputSMG = new HashMap<>();
    private final GenericAbstractionCandidateTemplate template;

    public MatchResultBuilder(GenericAbstractionCandidateTemplate pTemplate) {
      template = pTemplate;
    }

    public Set<SMGObject> getObjectsToBeRemovedForAbstraction() {
      return objectsToBeRemovedForAbstraction;
    }

    public void addObjectsToBeRemovedForAbstraction(SMGObject object) {
      objectsToBeRemovedForAbstraction.add(object);
    }

    public void setMatchTrue() {
      match = true;
    }

    public Set<SMGObject> getObjectsToBeRemovedForAbstractionInputSMG() {
      return objectsToBeRemovedForAbstractionInputSMG;
    }

    public void addObjectsToBeRemovedForAbstractionInputSMG(SMGObject object) {
      objectsToBeRemovedForAbstractionInputSMG.add(object);
    }

    public int getScore() {
      return score;
    }

    public void setScore(int pScore) {
      score = pScore;
    }

    public Map<SMGValue, SMGValue> getAbstractToConcretePointerMap() {
      return abstractToConcretePointerMap;
    }

    public void putAbstractToConcretePointerMap(SMGValue abstractValue, SMGValue concreteValue) {
      abstractToConcretePointerMap.put(abstractValue, concreteValue);
    }

    public Map<SMGValue, SMGValue> getAbstractToConcretePointerMapInputSMG() {
      return abstractToConcretePointerMapInputSMG;
    }

    public void putAbstractToConcretePointerMapInputSMG(
        SMGValue abstractValue, SMGValue concreteValue) {
      abstractToConcretePointerMapInputSMG.put(abstractValue, concreteValue);
    }

    public MatchResult build() {
      assert match;
      return new MatchResult(this);
    }

    public GenericAbstractionCandidateTemplate getGenAbsTemplate() {
      return template;
    }
  }

  private static class MatchResult {

    private static final MatchResult UNKNOWN =
        new MatchResult(false, null, 0, null, null, null, null);

    private final boolean match;
    private final Set<SMGObject> objectsToBeRemovedForAbstraction;
    private final Set<SMGObject> objectsToBeRemovedForAbstractionInputSMG;
    private final int score;
    private final Map<SMGValue, SMGValue> abstractToConcretePointerMap;
    private final Map<SMGValue, SMGValue> abstractToConcretePointerMapInputSMG;
    private final GenericAbstractionCandidateTemplate template;

    public MatchResult(
        boolean pMatche,
        Set<SMGObject> pObjectsToBeRemovedForAbstraction,
        int pScore,
        Map<SMGValue, SMGValue> pAbstractToConcretePointerMap,
        Set<SMGObject> pObjectsToBeRemovedForAbstractionInputSMG,
        Map<SMGValue, SMGValue> pAbstractToConcretePointerMapInputSMG,
        GenericAbstractionCandidateTemplate pTemplate) {
      match = pMatche;
      objectsToBeRemovedForAbstraction = pObjectsToBeRemovedForAbstraction;
      objectsToBeRemovedForAbstractionInputSMG = pObjectsToBeRemovedForAbstractionInputSMG;
      score = pScore;
      abstractToConcretePointerMap = pAbstractToConcretePointerMap;
      abstractToConcretePointerMapInputSMG = pAbstractToConcretePointerMapInputSMG;
      template = pTemplate;
    }

    public GenericAbstractionCandidateTemplate getGenAbsTemplate() {
      return template;
    }

    public MatchResult(MatchResultBuilder pMatchResultBuilder) {
      match = true;
      objectsToBeRemovedForAbstraction = pMatchResultBuilder.getObjectsToBeRemovedForAbstraction();
      objectsToBeRemovedForAbstractionInputSMG =
          pMatchResultBuilder.getObjectsToBeRemovedForAbstractionInputSMG();
      score = pMatchResultBuilder.getScore();
      abstractToConcretePointerMap = pMatchResultBuilder.getAbstractToConcretePointerMap();
      abstractToConcretePointerMapInputSMG =
          pMatchResultBuilder.getAbstractToConcretePointerMapInputSMG();
      template = pMatchResultBuilder.getGenAbsTemplate();
    }

    public static MatchResult valueOf(
        MachineModel pMachineModel, GenericAbstractionCandidate pAbstractionCandidate) {

      Map<SMGValue, SMGValue> abstractToConcretePointerMapInputSMG = ImmutableMap.of();
      Set<SMGObject> objectsToBeRemovedForAbstractionInputSMG = ImmutableSet.of();
      return new MatchResult(
          true,
          pAbstractionCandidate.getObjectsToBeRemoved(),
          pAbstractionCandidate.getScore(),
          pAbstractionCandidate.getAbstractToConcretePointerMap(),
          objectsToBeRemovedForAbstractionInputSMG,
          abstractToConcretePointerMapInputSMG,
          pAbstractionCandidate.createTemplate(pMachineModel));
    }

    public static MatchResult valueOf(MachineModel pMachineModel, GenericAbstraction pRootObject) {

      Set<SMGObject> toBeRemoved = new HashSet<>();
      toBeRemoved.add(pRootObject);
      Set<SMGObject> emptySet = ImmutableSet.of();
      Map<SMGValue, SMGValue> emptyMap = ImmutableMap.of();

      return new MatchResult(
          true,
          emptySet,
          100,
          emptyMap,
          toBeRemoved,
          pRootObject.getAbstractToConcretePointerMap(),
          pRootObject.createCandidateTemplate(pMachineModel));
    }

    public static MatchResult getUnknownInstance() {
      return UNKNOWN;
    }

    public Map<SMGValue, SMGValue> getAbstractToConcretePointerMap() {
      return abstractToConcretePointerMap;
    }

    public Set<SMGObject> getObjectsToBeRemovedForAbstraction() {
      return objectsToBeRemovedForAbstraction;
    }

    public Map<SMGValue, SMGValue> getAbstractToConcretePointerMapInputSMG() {
      return abstractToConcretePointerMapInputSMG;
    }

    public Set<SMGObject> getObjectsToBeRemovedForAbstractionInputSMG() {
      return objectsToBeRemovedForAbstractionInputSMG;
    }

    public boolean isMatch() {
      return match;
    }

    public int getScore() {
      return score;
    }
  }

  private static class SMGNodeTemplateAndNode {

    private final SMGNode node;
    private final SMGNodeTemplate nodeTemplate;

    public SMGNodeTemplateAndNode(SMGNodeTemplate pNodeTemplate, SMGNode pNode) {
      node = pNode;
      nodeTemplate = pNodeTemplate;
    }

    public SMGNodeTemplateAndNode(SMGValue pointerTemplate, SMGValue pointer) {
      node = new SMGNode(pointer);
      nodeTemplate = new SMGNodeTemplate(pointerTemplate);
    }

    public SMGNodeTemplateAndNode(SMGObjectTemplate objectTemplate, SMGObject object) {
      node = new SMGNode(object);
      nodeTemplate = new SMGNodeTemplate(objectTemplate);
    }

    public SMGNode getNode() {
      return node;
    }

    public SMGNodeTemplate getNodeTemplate() {
      return nodeTemplate;
    }
  }

  private static class SMGNode {

    @Nullable private final SMGValue value;
    private final SMGObject object;

    public SMGNode(SMGValue pValue) {
      value = Preconditions.checkNotNull(pValue);
      object = null;
    }

    public SMGNode(SMGObject pObject) {
      value = null;
      object = Preconditions.checkNotNull(pObject);
    }

    public boolean isValue() {
      return value != null;
    }

    public SMGValue getValue() {
      return value;
    }

    public boolean isObject() {
      return object != null;
    }

    public SMGObject getObject() {
      return object;
    }
  }

  private static class SMGNodeTemplate {

    @Nullable private final SMGValue valueTemplate;
    private final SMGObjectTemplate objectTemplate;

    public SMGNodeTemplate(SMGValue pValueTemplate) {
      valueTemplate = Preconditions.checkNotNull(pValueTemplate);
      objectTemplate = null;
    }

    public SMGNodeTemplate(SMGObjectTemplate pObject) {
      valueTemplate = null;
      objectTemplate = Preconditions.checkNotNull(pObject);
    }

    public boolean isValueTemplate() {
      return valueTemplate != null;
    }

    public SMGValue getValueTemplate() {
      return valueTemplate;
    }

    public boolean isObjectTemplate() {
      return objectTemplate != null;
    }

    public SMGObjectTemplate getObjectTemplate() {
      return objectTemplate;
    }
  }

  private static class MaterilisationStepToSubSMGMap {

    private final Map<SMGObjectTemplate, SMGObject> objectTemplateToObject = new HashMap<>();
    private final Map<SMGValue, SMGValue> valueTemplateToValue = new HashMap<>();
    private final Map<GenericAbstractionCandidateTemplate, MatchResult> abstractionToMatchMap =
        new HashMap<>();

    public MaterilisationStepToSubSMGMap() {
      // default
    }

    public void putAbstraction(GenericAbstractionCandidateTemplate pGenAbs, MatchResult pResult) {
      getAbstractionToMatchMap().put(pGenAbs, pResult);
    }

    public void put(SMGObjectTemplate objectTemplate, SMGObject object) {
      getObjectTemplateToObject().put(objectTemplate, object);
    }

    public void put(SMGValue abstractValue, SMGValue value) {
      getValueTemplateToValue().put(abstractValue, value);
    }

    public boolean contains(SMGObjectTemplate object) {
      return getObjectTemplateToObject().containsKey(object);
    }

    public boolean contains(SMGValue abstractValue) {
      return getValueTemplateToValue().containsKey(abstractValue);
    }

    public boolean containsAbstraction(GenericAbstractionCandidateTemplate abstractionTemplate) {
      return getAbstractionToMatchMap().containsKey(abstractionTemplate);
    }

    public MatchResult getAbstraction(GenericAbstractionCandidateTemplate template) {
      return getAbstractionToMatchMap().get(template);
    }

    public SMGObject get(SMGObjectTemplate object) {
      return getObjectTemplateToObject().get(object);
    }

    public SMGValue get(SMGValue abstractValue) {
      return getValueTemplateToValue().get(abstractValue);
    }

    public Map<SMGObjectTemplate, SMGObject> getObjectTemplateToObject() {
      return objectTemplateToObject;
    }

    public Map<SMGValue, SMGValue> getValueTemplateToValue() {
      return valueTemplateToValue;
    }

    public Map<GenericAbstractionCandidateTemplate, MatchResult> getAbstractionToMatchMap() {
      return abstractionToMatchMap;
    }
  }
}
