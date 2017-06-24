/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.GenericAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.GenericAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.GenericAbstractionCandidateTemplate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.MaterlisationStep;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.MaterlisationStep.FieldsOfTemplate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgeHasValueTemplate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgeHasValueTemplateWithConcreteValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgePointsToTemplate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGObjectTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unused")
public class SMGJoinSubSMGsIntoGenericAbstraction {

  private final MachineModel machineModel;

  private final GenericAbstractionCandidateTemplate template;
  private final SMG inputSMG1;

  private final SMG inputSMG2;

  private final SMGObject rootObject1;

  private final SMGObject rootObject2;

  private SMGNodeMapping mapping1;

  private SMGNodeMapping mapping2;

  private Map<Integer, List<GenericAbstractionCandidate>> previouslyMatched;

  public SMGJoinSubSMGsIntoGenericAbstraction(MachineModel pMachineModel,
      SMG pInputSMG1, SMG pInputSMG2,
      GenericAbstractionCandidateTemplate pTemplate, SMGObject pRootObject1, SMGObject pRootObject2,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
      Map<Integer, List<GenericAbstractionCandidate>> pValueAbstractionCandidates) {
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

    MatchResult matchResult = subSMGmatchSpecificShape(inputSMG1, rootObject1, mapping1, template,
        new HashSet<SMGObject>());

    if (!matchResult.isMatch()) {
      return Optional.empty();
    }

    MatchResult matchResult2 = subSMGmatchSpecificShape(inputSMG2, rootObject2, mapping2, template,
        new HashSet<SMGObject>());

    if (!matchResult.isMatch()) {
      return Optional.empty();
    }


    return createGenericAbstractionCandidate(matchResult, matchResult2);
  }

  private Optional<GenericAbstractionCandidate> createGenericAbstractionCandidate(
      MatchResult pMatchResult, MatchResult pMatchResult2) {

    MatchResultBuilder builder = new MatchResultBuilder(template);

    builder.getAbstractToConcretePointerMap().putAll(pMatchResult.getAbstractToConcretePointerMap());

    for(Entry<Integer, Integer> entry : pMatchResult.getAbstractToConcretePointerMapInputSMG().entrySet()) {

      int absVal = entry.getKey();
      int val  = entry.getValue();

      if(mapping1.containsKey(val)) {
        builder.putAbstractToConcretePointerMap(absVal, mapping1.get(val));
      } else {
        return Optional.empty();
      }
    }

    builder.getObjectsToBeRemovedForAbstraction()
        .addAll(pMatchResult.getObjectsToBeRemovedForAbstraction());

    for (SMGObject obj : pMatchResult.getObjectsToBeRemovedForAbstractionInputSMG()) {
      if(mapping1.containsKey(obj)) {
        builder.addObjectsToBeRemovedForAbstraction(mapping1.get(obj));
      }
    }

    for (SMGObject obj : pMatchResult2.getObjectsToBeRemovedForAbstractionInputSMG()) {
      if(mapping2.containsKey(obj) && !builder.getObjectsToBeRemovedForAbstraction().contains(mapping2.get(obj))) {
        return Optional.empty();
      }
    }

    int score = pMatchResult.getScore() > pMatchResult2.getScore() ? pMatchResult.getScore() : pMatchResult2.getScore();

    MatchResult destres = builder.build();

    return Optional
        .of(GenericAbstractionCandidate.valueOf(machineModel, destres.getObjectsToBeRemovedForAbstraction(),
            destres.getAbstractToConcretePointerMap(), template.getMaterlisationStepMap(), score));
  }

  private MatchResult subSMGmatchSpecificShape(SMG pInputSMG, SMGObject pRootObject,
      SMGNodeMapping pMapping, GenericAbstractionCandidateTemplate pTemplate, Set<SMGObject> pAlreadyVisited) {

    if (pRootObject instanceof GenericAbstraction) {
      return subSMGmatchSpecificShape((GenericAbstraction) pRootObject, pTemplate, pAlreadyVisited);
    } else if (pRootObject instanceof SMGRegion) {
      return subSMGmatchSpecificShape(pInputSMG, (SMGRegion) pRootObject, pMapping, pTemplate, pAlreadyVisited);
    } else {
      return MatchResult.getUnknownInstance();
    }
  }

  private MatchResult subSMGmatchSpecificShape(GenericAbstraction pRootObject,
      GenericAbstractionCandidateTemplate pTemplate,  Set<SMGObject> pAlreadyVisited) {

    GenericAbstractionCandidateTemplate rootObjectTemaplate = pRootObject.createCandidateTemplate(machineModel);

    if (rootObjectTemaplate.isSpecificShape(pTemplate)) {
      return MatchResult.valueOf(machineModel, pRootObject);
    } else {
      return MatchResult.getUnknownInstance();
    }
  }

  private MatchResult subSMGmatchSpecificShape(SMG pInputSMG, SMGRegion pRootObject,
      SMGNodeMapping pMapping, GenericAbstractionCandidateTemplate pTemplate, Set<SMGObject> pAlreadyVisited) {

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
        result = subSMGmatchStep(matStep, pInputSMG, pRootObject, pMapping, pAlreadyVisited, pTemplate);

        if (result.isMatch()) {
          return result;
        }
      } else {
        stopStep.add(matStep);
      }
    }

    for (MaterlisationStep matStep : stopStep) {
      result = subSMGmatchStep(matStep, pInputSMG, pRootObject, pMapping, pAlreadyVisited, pTemplate);

      if (result.isMatch()) {
        return result;
      }
    }

    return result;
  }

  private MatchResult subSMGmatchStep(MaterlisationStep pMatStep, SMG pInputSMG,
      SMGRegion pRootObject, SMGNodeMapping pMapping, Set<SMGObject> pAlreadyVisited, GenericAbstractionCandidateTemplate pTemplate) {

    Set<SMGObjectTemplate> entryRegions = pMatStep.getEntryRegions();
    Set<SMGObject> alreadyVisited = new HashSet<>();
    alreadyVisited.addAll(pAlreadyVisited);

    MatchResult result;

    for (SMGObjectTemplate entryRegion : entryRegions) {

      result = subSMGmatchStep(pMatStep, pInputSMG, pRootObject, entryRegion, pMapping, alreadyVisited, pTemplate);

      if (result.isMatch()) {
        return result;
      }
    }

    return MatchResult.getUnknownInstance();
  }

  private MatchResult subSMGmatchStep(MaterlisationStep pMatStep, SMG pInputSMG,
      SMGRegion pRootObject, SMGObjectTemplate pEntryRegion, SMGNodeMapping pMapping,
      Set<SMGObject> pAlreadyVisited, GenericAbstractionCandidateTemplate pTemplate) {

    /*If entry  region is abstraction, use different matstep to avoid endless loop*/
    if(pEntryRegion instanceof GenericAbstractionCandidateTemplate) {
      return MatchResult.getUnknownInstance();
    }

    MaterilisationStepToSubSMGMap templateToInputSmgMapping = new MaterilisationStepToSubSMGMap();
    templateToInputSmgMapping.put(pEntryRegion, pRootObject);
    pAlreadyVisited.add(pRootObject);
    Set<SMGNodeTemplateAndNode> toBeMatchedLater = new HashSet<>();
    toBeMatchedLater.add(new SMGNodeTemplateAndNode(new SMGNodeTemplate(pEntryRegion), new SMGNode(pRootObject)));

    while (!toBeMatchedLater.isEmpty()) {
      Set<SMGNodeTemplateAndNode> toBeMatched = ImmutableSet.copyOf(toBeMatchedLater);
      toBeMatchedLater = new HashSet<>();

      for (SMGNodeTemplateAndNode templateNodeAndConcreteNode : toBeMatched) {

        SMGNode node = templateNodeAndConcreteNode.getNode();
        SMGNodeTemplate nodeTemplate = templateNodeAndConcreteNode.getNodeTemplate();

        boolean match = matchNodeTemplateWithNode(node, nodeTemplate, pMatStep, templateToInputSmgMapping, pInputSMG, toBeMatchedLater, pAlreadyVisited, pMapping);


        if (!match) {
          return MatchResult.getUnknownInstance();
        }
      }
    }

    MatchResult result = constructAbstraction(pTemplate, templateToInputSmgMapping, pMatStep);

    return result;
  }

  private MatchResult constructAbstraction(GenericAbstractionCandidateTemplate pTemplate,
      MaterilisationStepToSubSMGMap pTemplateToInputSmgMapping, MaterlisationStep pMatStep) {

    MatchResultBuilder matchResultBuilder = new MatchResultBuilder(pTemplate);

    matchResultBuilder.setMatchTrue();

    for(SMGObject newObjects : pTemplateToInputSmgMapping.objectTemplateToObject.values()) {
      matchResultBuilder.addObjectsToBeRemovedForAbstractionInputSMG(newObjects);
    }

    for (Entry<Integer, Integer> values : pTemplateToInputSmgMapping.getValueTemplateToValue()
        .entrySet()) {
      int absVal = values.getKey();
      int conVal = values.getValue();

      if(pMatStep.abstractInterfaceContains(absVal)) {
        matchResultBuilder.putAbstractToConcretePointerMapInputSMG(absVal, conVal);
      }
    }

    int score = 0;

    for (MatchResult prevAbs : pTemplateToInputSmgMapping.getAbstractionToMatchMap().values()) {

      matchResultBuilder.getObjectsToBeRemovedForAbstraction()
          .addAll(prevAbs.getObjectsToBeRemovedForAbstraction());
      matchResultBuilder.getObjectsToBeRemovedForAbstractionInputSMG()
          .addAll(prevAbs.getObjectsToBeRemovedForAbstractionInputSMG());

      if (prevAbs.getScore() > score) {
        score = prevAbs.getScore();
      }
    }

    score++;
    matchResultBuilder.setScore(score);

    return matchResultBuilder.build();
  }

  private boolean matchNodeTemplateWithNode(SMGNode pNode, SMGNodeTemplate pNodeTemplate,
      MaterlisationStep pMatStep, MaterilisationStepToSubSMGMap pTemplateToInputSmgMapping,
      SMG pInputSMG,
      Set<SMGNodeTemplateAndNode> pToBeMatchedLater,
      Set<SMGObject> pAlreadyVisited, SMGNodeMapping pMapping) {

    if (pNode.isValue() && pNodeTemplate.isValueTemplate()) {
      return matchValueTemplateWithValue(pNode.getValue(), pNodeTemplate.getValueTemplate(),
          pMatStep, pTemplateToInputSmgMapping, pInputSMG, pToBeMatchedLater, pAlreadyVisited, pMapping);
    } else if (pNode.isObject() && pNodeTemplate.isObjectTemplate()) {

      SMGObject object = pNode.getObject();
      SMGObjectTemplate template = pNodeTemplate.getObjectTemplate();

      if (template instanceof GenericAbstractionCandidateTemplate) {
        GenericAbstractionCandidateTemplate genAbsTmp = (GenericAbstractionCandidateTemplate) template;

        return matchGenericAbstractionTemplateWithGenericAbstraction(genAbsTmp, pInputSMG,
            pToBeMatchedLater, pTemplateToInputSmgMapping, pMatStep);
      } else if (template instanceof SMGRegion) {
        SMGRegion regionTemplate = (SMGRegion) template;
        return matchRegionTemplateWithObject(regionTemplate, object, pInputSMG,
            pToBeMatchedLater, pTemplateToInputSmgMapping, pMatStep);
      }
    }

    return false;
  }

  private boolean matchGenericAbstractionTemplateWithGenericAbstraction(
      GenericAbstractionCandidateTemplate pGenAbsTmp, SMG pInputSMG,
      Set<SMGNodeTemplateAndNode> pToBeMatchedLater,
      MaterilisationStepToSubSMGMap pTemplateToInputSmgMapping, MaterlisationStep pMatStep) {

    if (!pTemplateToInputSmgMapping.containsAbstraction(pGenAbsTmp)) {
      return false;
    }

    MatchResult genAbsMatchResult = pTemplateToInputSmgMapping.getAbstraction(pGenAbsTmp);

    if (!genAbsMatchResult.isMatch()) {
      return false;
    }

    Set<SMGEdgePointsTo> pointerToAbstraction = getPointerToThisAbstraction(genAbsMatchResult, pInputSMG);
    Set<SMGEdgePointsToTemplate> pointerToRegionTemplate = pMatStep.getPointerToThisTemplate(pGenAbsTmp);

    if(pointerToAbstraction.size() != pointerToRegionTemplate.size()) {
      return false;
    }

    Map<Integer, SMGEdgePointsTo> pointerToRegionMap =
        FluentIterable.from(pointerToAbstraction).uniqueIndex(new MapPointerEdgeToOffset());

    Map<Integer, SMGEdgePointsToTemplate> pointerToRegionTemplateMap =
        FluentIterable.from(pointerToRegionTemplate)
            .uniqueIndex(new MapPointerEdgeToOffsetTemplate());

    for(Entry<Integer, SMGEdgePointsToTemplate> ptEntry : pointerToRegionTemplateMap.entrySet()) {
      int offset = ptEntry.getKey();
      SMGEdgePointsToTemplate pointerTemplateEdge = ptEntry.getValue();

      if(!pointerToRegionMap.containsKey(offset)) {
        return false;
      }

      int pointerTemplate = pointerTemplateEdge.getAbstractValue();
      int pointer = pointerToRegionMap.get(offset).getValue();

      if (pTemplateToInputSmgMapping.contains(pointerTemplate)) {
        if (pTemplateToInputSmgMapping.get(pointerTemplate) != pointer) {
          return false;
        }
      } else {
        pTemplateToInputSmgMapping.put(pointerTemplate, pointer);
        if(pMatStep.getAbstractPointers().contains(pointerTemplateEdge)) {
          pToBeMatchedLater.add(new SMGNodeTemplateAndNode(pointerTemplate, pointer));
        }
      }
    }

    Set<SMGEdgeHasValue> fieldsOfRegion = getFieldsOfGenAbs(genAbsMatchResult, pInputSMG);
    FieldsOfTemplate fieldsOfTemplate = pMatStep.getFieldsOfThisTemplate(pGenAbsTmp);

    if (fieldsOfRegion.size() != fieldsOfTemplate.size()) {
      return false;
    }

    Map<Integer, SMGEdgeHasValue> fieldsOfRegionMap = FluentIterable.from(fieldsOfRegion).uniqueIndex(new MapHasValueEdgeToOffset());

    Set<SMGEdgeHasValueTemplate> fieldsOfTemplateSet = new HashSet<>(fieldsOfTemplate.getFieldTemplateContainingPointer());
    fieldsOfTemplateSet.addAll(fieldsOfTemplate.getFieldTemplateContainingPointerTemplate());

    Map<Integer, SMGEdgeHasValueTemplate> fieldsOfRegionTemplateMap =
        FluentIterable.from(fieldsOfTemplateSet)
            .uniqueIndex(new MapHasValueEdgeToOffsetTemplate());

    Map<Integer, SMGEdgeHasValueTemplateWithConcreteValue> fieldsOfRegionTemplateCVMap =
        FluentIterable.from(fieldsOfTemplate.getFieldTemplateContainingValue())
            .uniqueIndex(new MapHasValueEdgeToOffsetTemplateCV());

    for (Entry<Integer, SMGEdgeHasValue> hveEntry : fieldsOfRegionMap.entrySet()) {

      int offset = hveEntry.getKey();
      SMGEdgeHasValue hve = hveEntry.getValue();

      if (fieldsOfRegionTemplateMap.containsKey(offset)) {

        int pointerTemplate = fieldsOfRegionTemplateMap.get(offset).getAbstractValue();
        int pointer = hve.getValue();

        if (pTemplateToInputSmgMapping.contains(pointerTemplate)) {
          if (pTemplateToInputSmgMapping.get(pointerTemplate) != pointer) {
            return false;
          }
        } else {
          pTemplateToInputSmgMapping.put(pointerTemplate, pointer);
          if(pMatStep.getAbstractFieldsToIPointer().contains(fieldsOfRegionTemplateMap.get(offset))) {
            pToBeMatchedLater.add(new SMGNodeTemplateAndNode(pointerTemplate, pointer));
          }
        }

      } else if (fieldsOfRegionTemplateCVMap.containsKey(offset)) {

        int value = hve.getValue();
        int valueInTemplate = fieldsOfRegionTemplateCVMap.get(offset).getValue();

        if (value != valueInTemplate) {
          return false;
        }

      } else {
        return false;
      }
    }

    return true;
  }

  private Set<SMGEdgeHasValue> getFieldsOfGenAbs(MatchResult pGenAbsMatchResult, SMG pInputSMG) {

    GenericAbstractionCandidateTemplate template = pGenAbsMatchResult.getGenAbsTemplate();

    Map<Integer, Integer> abstractToConcreteMap =
        pGenAbsMatchResult.getAbstractToConcretePointerMapInputSMG();

    Set<SMGEdgeHasValueTemplate> pointerToThisAbstraction =
        template.getMaterlisationSteps().iterator().next().getAbstractFieldsToOPointer();

    Set<SMGEdgeHasValue> result = new HashSet<>();
    for (SMGEdgeHasValueTemplate fieldTmp : pointerToThisAbstraction) {
      int absVal = fieldTmp.getAbstractValue();
      int concreteValue = abstractToConcreteMap.get(absVal);

      result.addAll(SMGUtils.getFieldsofThisValue(concreteValue, pInputSMG));
    }
    return result;
  }

  private Set<SMGEdgePointsTo> getPointerToThisAbstraction(MatchResult pGenAbsMatchResult,
      SMG pInputSMG) {

    GenericAbstractionCandidateTemplate template = pGenAbsMatchResult.getGenAbsTemplate();

    Map<Integer, Integer> abstractToConcreteMap =
        pGenAbsMatchResult.getAbstractToConcretePointerMapInputSMG();

    Set<SMGEdgePointsToTemplate> pointerToThisAbstraction =
        template.getMaterlisationSteps().iterator().next().getAbstractAdressesToOPointer();

    Set<SMGEdgePointsTo> result = new HashSet<>();
    for (SMGEdgePointsToTemplate pointerTmp : pointerToThisAbstraction) {
      int absVal = pointerTmp.getAbstractValue();
      int concreteValue = abstractToConcreteMap.get(absVal);
      result.add(pInputSMG.getPointer(concreteValue));
    }
    return result;
  }

  private boolean matchValueTemplateWithValue(int pValue, int pValueTemplate,
      MaterlisationStep pMatStep, MaterilisationStepToSubSMGMap pTemplateToInputSmgMapping,
      SMG pInputSMG, Set<SMGNodeTemplateAndNode> pToBeMatchedLater,
      Set<SMGObject> pAlreadyVisited, SMGNodeMapping pMapping) {

    if(!pTemplateToInputSmgMapping.contains(pValueTemplate) || pTemplateToInputSmgMapping.get(pValueTemplate) != pValue) {
      return false;
    }

    if (pInputSMG.isPointer(pValue)) {

      SMGEdgePointsTo pointerEdge = pInputSMG.getPointer(pValue);
      Optional<SMGEdgePointsToTemplate> pointerEdgeTemplateOpt =
          pMatStep.getPointer(pValueTemplate);

      if (!pointerEdgeTemplateOpt.isPresent()) {
        return false;
      }

      SMGEdgePointsToTemplate pointerEdgeTemplate = pointerEdgeTemplateOpt.get();

      if(pointerEdgeTemplate.getOffset() != pointerEdge.getOffset()) {
        return false;
      }

      SMGObjectTemplate targetTemplate = pointerEdgeTemplate.getObjectTemplate();
      SMGObject target = pointerEdge.getObject();

      if (pTemplateToInputSmgMapping.contains(targetTemplate)) {
        SMGObject mappedTarget = pTemplateToInputSmgMapping.get(targetTemplate);

        if(mappedTarget != target) {
          return false;
        }
      } else {
        if(targetTemplate instanceof GenericAbstractionCandidateTemplate) {
          GenericAbstractionCandidateTemplate genAbs = (GenericAbstractionCandidateTemplate) targetTemplate;

          MatchResult result;
          if(pTemplateToInputSmgMapping.containsAbstraction(genAbs)) {
            result = pTemplateToInputSmgMapping.getAbstraction(genAbs);
          } else {
            result = subSMGmatchSpecificShape(pInputSMG, target, pMapping, genAbs, pAlreadyVisited);
            pTemplateToInputSmgMapping.putAbstraction(genAbs, result);
            pToBeMatchedLater.add(new SMGNodeTemplateAndNode(targetTemplate, target));
          }

          if(!result.isMatch()) {
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

    Set<SMGEdgeHasValue> fields = SMGUtils.getFieldsofThisValue(pValue, pInputSMG);

    Set<SMGEdgeHasValueTemplate> fieldsTemplate = pMatStep.getFieldsOfValue(pValueTemplate);

    if(fields.size() != fieldsTemplate.size()) {
      return false;
    }

    Map<Integer, SMGEdgeHasValue> fieldOffsetMap =
        FluentIterable.from(fields).uniqueIndex(new MapHasValueEdgeToOffset());

    Map<Integer, SMGEdgeHasValueTemplate> fieldOffsetTemplateMap =
        FluentIterable.from(fieldsTemplate).uniqueIndex(new MapHasValueEdgeToOffsetTemplate());

    for(Entry<Integer, SMGEdgeHasValueTemplate> hveTmpEntry : fieldOffsetTemplateMap.entrySet()) {

      int offset = hveTmpEntry.getKey();
      SMGEdgeHasValueTemplate pointerEdgeTemplate = hveTmpEntry.getValue();

      if(!fieldOffsetMap.containsKey(offset)) {
        return false;
      }

      SMGEdgeHasValue pointerEdge = fieldOffsetMap.get(offset);
      SMGObjectTemplate targetTemplate = pointerEdgeTemplate.getObjectTemplate();
      SMGObject target = pointerEdge.getObject();

      if (pTemplateToInputSmgMapping.contains(targetTemplate)) {
        SMGObject mappedTarget = pTemplateToInputSmgMapping.get(targetTemplate);

        if(mappedTarget != target) {
          return false;
        }
      } else {
        if(targetTemplate instanceof GenericAbstractionCandidateTemplate) {
          GenericAbstractionCandidateTemplate genAbs = (GenericAbstractionCandidateTemplate) targetTemplate;

          MatchResult result;
          if(pTemplateToInputSmgMapping.containsAbstraction(genAbs)) {
            result = pTemplateToInputSmgMapping.getAbstraction(genAbs);
          } else {
            result = subSMGmatchSpecificShape(pInputSMG, target, pMapping, genAbs, pAlreadyVisited);
            pTemplateToInputSmgMapping.putAbstraction(genAbs, result);
            pToBeMatchedLater.add(new SMGNodeTemplateAndNode(targetTemplate, target));
          }

          if(!result.isMatch()) {
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

  private boolean matchRegionTemplateWithObject(SMGRegion pTemplate,
      SMGObject pObject, SMG pInputSMG, Set<SMGNodeTemplateAndNode> pToBeMatchedLater,
      MaterilisationStepToSubSMGMap pMatStepToSubSMGMapping, MaterlisationStep matStep) {

    if(!(pObject instanceof SMGRegion)) {
      return false;
    }

    SMGRegion region = (SMGRegion) pObject;

    if(region.getSize() != pTemplate.getSize()) {
      return false;
    }

    Set<SMGEdgePointsTo> pointerToRegion = SMGUtils.getPointerToThisObject(region, pInputSMG);
    Set<SMGEdgePointsToTemplate> pointerToRegionTemplate = matStep.getPointerToThisTemplate(pTemplate);

    if(pointerToRegion.size() != pointerToRegionTemplate.size()) {
      return false;
    }

    Map<Integer, SMGEdgePointsTo> pointerToRegionMap =
        FluentIterable.from(pointerToRegion).uniqueIndex(new MapPointerEdgeToOffset());

    Map<Integer, SMGEdgePointsToTemplate> pointerToRegionTemplateMap =
        FluentIterable.from(pointerToRegionTemplate)
            .uniqueIndex(new MapPointerEdgeToOffsetTemplate());

    for (Entry<Integer, SMGEdgePointsToTemplate> pteTmp : pointerToRegionTemplateMap.entrySet()) {

      int offset = pteTmp.getKey();
      SMGEdgePointsToTemplate ptTmp = pteTmp.getValue();

      if (!pointerToRegionMap.containsKey(offset)) {
        return false;
      }

      int pointerTemplate = ptTmp.getAbstractValue();
      int pointer = pointerToRegionMap.get(offset).getValue();

      if (pMatStepToSubSMGMapping.contains(pointerTemplate)) {
        if (pMatStepToSubSMGMapping.get(pointerTemplate) != pointer) {
          return false;
        }
      } else {
        pMatStepToSubSMGMapping.put(pointerTemplate, pointer);
        if(matStep.getAbstractPointers().contains(ptTmp)) {
          pToBeMatchedLater.add(new SMGNodeTemplateAndNode(pointerTemplate, pointer));
        }
      }
    }

    Set<SMGEdgeHasValue> fieldsOfRegion = SMGUtils.getFieldsOfObject(region, pInputSMG);
    FieldsOfTemplate fieldsOfTemplate = matStep.getFieldsOfThisTemplate(pTemplate);

    if (fieldsOfRegion.size() != fieldsOfTemplate.size()) {
      return false;
    }

    Map<Integer, SMGEdgeHasValue> fieldsOfRegionMap = FluentIterable.from(fieldsOfRegion).uniqueIndex(new MapHasValueEdgeToOffset());

    Set<SMGEdgeHasValueTemplate> fieldsOfTemplateSet = new HashSet<>(fieldsOfTemplate.getFieldTemplateContainingPointer());
    fieldsOfTemplateSet.addAll(fieldsOfTemplate.getFieldTemplateContainingPointerTemplate());

    Map<Integer, SMGEdgeHasValueTemplate> fieldsOfRegionTemplateMap =
        FluentIterable.from(fieldsOfTemplateSet)
            .uniqueIndex(new MapHasValueEdgeToOffsetTemplate());

    Map<Integer, SMGEdgeHasValueTemplateWithConcreteValue> fieldsOfRegionTemplateCVMap =
        FluentIterable.from(fieldsOfTemplate.getFieldTemplateContainingValue())
            .uniqueIndex(new MapHasValueEdgeToOffsetTemplateCV());

    for (Entry<Integer, SMGEdgeHasValue> hveEntry : fieldsOfRegionMap.entrySet()) {

      int offset = hveEntry.getKey();
      SMGEdgeHasValue hve = hveEntry.getValue();

      if (fieldsOfRegionTemplateMap.containsKey(offset)) {

        int pointerTemplate = fieldsOfRegionTemplateMap.get(offset).getAbstractValue();
        int pointer = hve.getValue();

        if (pMatStepToSubSMGMapping.contains(pointerTemplate)) {
          if (pMatStepToSubSMGMapping.get(pointerTemplate) != pointer) {
            return false;
          }
        } else {
          pMatStepToSubSMGMapping.put(pointerTemplate, pointer);
          if(matStep.getAbstractFieldsToIPointer().contains(fieldsOfRegionTemplateMap.get(offset))) {
            pToBeMatchedLater.add(new SMGNodeTemplateAndNode(pointerTemplate, pointer));
          }
        }

      } else if (fieldsOfRegionTemplateCVMap.containsKey(offset)) {

        int value = hve.getValue();
        int valueInTemplate = fieldsOfRegionTemplateCVMap.get(offset).getValue();

        if (value != valueInTemplate) {
          return false;
        }

      } else {
        return false;
      }
    }

    return true;
  }

  private static class MapPointerEdgeToOffset implements Function<SMGEdgePointsTo, Integer> {
    @Override
    public Integer apply(SMGEdgePointsTo pEdge) {
      return pEdge.getOffset();
    }
  }

  private static class MapPointerEdgeToOffsetTemplate
      implements Function<SMGEdgePointsToTemplate, Integer> {

    @Override
    public Integer apply(SMGEdgePointsToTemplate pEdge) {
      return pEdge.getOffset();
    }
  }

  private static class MapHasValueEdgeToOffsetTemplate
      implements Function<SMGEdgeHasValueTemplate, Integer> {

    @Override
    public Integer apply(SMGEdgeHasValueTemplate pEdge) {
      return pEdge.getOffset();
    }
  }

  private static class MapHasValueEdgeToOffsetTemplateCV
      implements Function<SMGEdgeHasValueTemplateWithConcreteValue, Integer> {

    @Override
    public Integer apply(SMGEdgeHasValueTemplateWithConcreteValue pEdge) {
      return pEdge.getOffset();
    }
  }

  private static class MapHasValueEdgeToOffset
      implements Function<SMGEdgeHasValue, Integer> {

    @Override
    public Integer apply(SMGEdgeHasValue pEdge) {
      return pEdge.getOffset();
    }
  }

  private MatchResult wasMatchedPreviously(SMG pInputSMG, SMGObject pRootObject,
      GenericAbstractionCandidateTemplate pTemplate, SMGNodeMapping pMapping) {

    Set<SMGEdgePointsTo> pointsToThisObject = SMGUtils.getPointerToThisObject(pRootObject, pInputSMG);

    for (SMGEdgePointsTo pointer : pointsToThisObject) {
      if (pMapping.containsKey(pointer.getValue())) {
        int destPointerValue = pMapping.get(pointer.getValue());

        if (previouslyMatched.containsKey(destPointerValue)) {
          for (GenericAbstractionCandidate abstractionCandidate : previouslyMatched
              .get(destPointerValue)) {

            if (pTemplate.equals(abstractionCandidate
                .createTemplate(machineModel))) {
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
    private Map<Integer, Integer> abstractToConcretePointerMap = new HashMap<>();
    private Map<Integer, Integer> abstractToConcretePointerMapInputSMG = new HashMap<>();
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

    public Map<Integer, Integer> getAbstractToConcretePointerMap() {
      return abstractToConcretePointerMap;
    }

    public void putAbstractToConcretePointerMap(int abstractValue, int concreteValue) {
      abstractToConcretePointerMap.put(abstractValue, concreteValue);
    }

    public Map<Integer, Integer> getAbstractToConcretePointerMapInputSMG() {
      return abstractToConcretePointerMapInputSMG;
    }

    public void putAbstractToConcretePointerMapInputSMG(int abstractValue, int concreteValue) {
      abstractToConcretePointerMapInputSMG.put(abstractValue, concreteValue);
    }

    public MatchResult build() {
      assert match == true;
      return new MatchResult(this);
    }

    public GenericAbstractionCandidateTemplate getGenAbsTemplate() {
      return template;
    }
  }

  private static class MatchResult {

    private static final MatchResult UNKNOWN = new MatchResult(false, null, 0, null, null, null, null);

    private final boolean match;
    private final Set<SMGObject> objectsToBeRemovedForAbstraction;
    private final Set<SMGObject> objectsToBeRemovedForAbstractionInputSMG;
    private final int score;
    private final Map<Integer, Integer> abstractToConcretePointerMap;
    private final Map<Integer, Integer> abstractToConcretePointerMapInputSMG;
    private final GenericAbstractionCandidateTemplate template;

    public MatchResult(boolean pMatche, Set<SMGObject> pObjectsToBeRemovedForAbstraction,
        int pScore, Map<Integer, Integer> pAbstractToConcretePointerMap,
        Set<SMGObject> pObjectsToBeRemovedForAbstractionInputSMG,
        Map<Integer, Integer> pAbstractToConcretePointerMapInputSMG,
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

    public static MatchResult valueOf(MachineModel pMachineModel, GenericAbstractionCandidate pAbstractionCandidate) {

      Map<Integer, Integer> abstractToConcretePointerMapInputSMG = ImmutableMap.of();
      Set<SMGObject> objectsToBeRemovedForAbstractionInputSMG = ImmutableSet.of();
      return new MatchResult(true, pAbstractionCandidate.getObjectsToBeRemoved(),
          pAbstractionCandidate.getScore(), pAbstractionCandidate.getAbstractToConcretePointerMap(),
          objectsToBeRemovedForAbstractionInputSMG, abstractToConcretePointerMapInputSMG, pAbstractionCandidate.createTemplate(pMachineModel));
    }

    public static MatchResult valueOf(MachineModel pMachineModel, GenericAbstraction pRootObject) {

      Set<SMGObject> toBeRemoved = new HashSet<>();
      toBeRemoved.add(pRootObject);
      Set<SMGObject> emptySet = ImmutableSet.of();
      Map<Integer, Integer> emptyMap = ImmutableMap.of();

      return new MatchResult(true, emptySet, 100, emptyMap,
          toBeRemoved, pRootObject.getAbstractToConcretePointerMap(), pRootObject.createCandidateTemplate(pMachineModel));
    }

    public static MatchResult getUnknownInstance() {
      return UNKNOWN;
    }

    public Map<Integer, Integer> getAbstractToConcretePointerMap() {
      return abstractToConcretePointerMap;
    }

    public Set<SMGObject> getObjectsToBeRemovedForAbstraction() {
      return objectsToBeRemovedForAbstraction;
    }

    public Map<Integer, Integer> getAbstractToConcretePointerMapInputSMG() {
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

    public SMGNodeTemplateAndNode(int pointerTemplate, int pointer) {
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

    private final int value;
    private final SMGObject object;

    public SMGNode(int pValue) {
      assert pValue >= 0;
      value = pValue;
      object = null;
    }

    public SMGNode(SMGObject pObject) {
      assert pObject != null;
      value = -1;
      object = pObject;
    }

    public boolean isValue() {
      return value != -1;
    }

    public int getValue() {
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

    private final int valueTemplate;
    private final SMGObjectTemplate objectTemplate;

    public SMGNodeTemplate(int pValueTemplate) {
      assert pValueTemplate >= 0;
      valueTemplate = pValueTemplate;
      objectTemplate = null;
    }

    public SMGNodeTemplate(SMGObjectTemplate pObject) {
      assert pObject != null;
      valueTemplate = -1;
      objectTemplate = pObject;
    }

    public boolean isValueTemplate() {
      return valueTemplate != -1;
    }

    public int getValueTemplate() {
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
    private final Map<Integer, Integer> valueTemplateToValue = new HashMap<>();
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

    public void put(int abstractValue, int value) {
      getValueTemplateToValue().put(abstractValue, value);
    }

    public boolean contains(SMGObjectTemplate object) {
      return getObjectTemplateToObject().containsKey(object);
    }

    public boolean contains(int abstractValue) {
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

    public int get(int abstractValue) {
      return getValueTemplateToValue().get(abstractValue);
    }

    public Map<SMGObjectTemplate, SMGObject> getObjectTemplateToObject() {
      return objectTemplateToObject;
    }

    public Map<Integer, Integer> getValueTemplateToValue() {
      return valueTemplateToValue;
    }

    public Map<GenericAbstractionCandidateTemplate, MatchResult> getAbstractionToMatchMap() {
      return abstractionToMatchMap;
    }
  }
}