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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.GenericAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.GenericAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.GenericAbstractionCandidateTemplate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.MaterlisationStep;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGObjectTemplate;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class SMGJoinSubSMGsIntoGenericAbstraction {

  private final GenericAbstractionCandidateTemplate template;
  private final SMG inputSMG1;
  @SuppressWarnings("unused")
  private final SMG inputSMG2;
  @SuppressWarnings("unused")
  private final SMG destSMG;

  private final SMGObject rootObject1;
  @SuppressWarnings("unused")
  private final SMGObject rootObject2;

  private SMGNodeMapping mapping1;
  @SuppressWarnings("unused")
  private SMGNodeMapping mapping2;

  private Map<Integer, List<GenericAbstractionCandidate>> previouslyMatched;

  public SMGJoinSubSMGsIntoGenericAbstraction(SMG pInputSMG1, SMG pInputSMG2, SMG pDestSMG,
      @SuppressWarnings("unused") SMGNodeMapping mapping1, @SuppressWarnings("unused") SMGNodeMapping mapping2,
      GenericAbstractionCandidateTemplate pTemplate, SMGObject pRootObject1, SMGObject pRootObject2,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2, @SuppressWarnings("unused") SMGJoinStatus pStatus,
      Map<Integer, List<GenericAbstractionCandidate>> pValueAbstractionCandidates) {
    template = pTemplate;
    inputSMG1 = pInputSMG1;
    inputSMG2 = pInputSMG2;
    destSMG = pDestSMG;
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
      return Optional.absent();
    }

    return Optional.absent();
  }

  private MatchResult subSMGmatchSpecificShape(SMG pInputSMG, SMGObject pRootObject,
      SMGNodeMapping pMapping, GenericAbstractionCandidateTemplate pTemplate, Set<SMGObject> pAlreadyMatched) {

    if (pRootObject instanceof GenericAbstraction) {
      return subSMGmatchSpecificShape((GenericAbstraction) pRootObject, pTemplate, pAlreadyMatched);
    } else if (pRootObject instanceof SMGRegion) {
      return subSMGmatchSpecificShape(pInputSMG, (SMGRegion) pRootObject, pMapping, pTemplate, pAlreadyMatched);
    } else {
      return MatchResult.getUnknownInstance();
    }
  }

  private MatchResult subSMGmatchSpecificShape(GenericAbstraction pRootObject,
      GenericAbstractionCandidateTemplate pTemplate, @SuppressWarnings("unused") Set<SMGObject> pAlreadyMatched) {

    GenericAbstractionCandidateTemplate rootObjectTemaplate = pRootObject.createCandidateTemplate();

    if (rootObjectTemaplate.isSpecificShape(pTemplate)) {
      return MatchResult.valueOf(pRootObject);
    } else {
      return MatchResult.getUnknownInstance();
    }
  }

  private MatchResult subSMGmatchSpecificShape(SMG pInputSMG, SMGRegion pRootObject,
      SMGNodeMapping pMapping, GenericAbstractionCandidateTemplate pTemplate, Set<SMGObject> pAlreadyMatched) {

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
        result = subSMGmatchStep(matStep, pInputSMG, pRootObject, pMapping, pAlreadyMatched);

        if (result.isMatch()) {
          return result;
        }
      } else {
        stopStep.add(matStep);
      }
    }

    for (MaterlisationStep matStep : stopStep) {
      result = subSMGmatchStep(matStep, pInputSMG, pRootObject, pMapping, pAlreadyMatched);

      if (result.isMatch()) {
        return result;
      }
    }

    return result;
  }

  private MatchResult subSMGmatchStep(MaterlisationStep pMatStep, SMG pInputSMG,
      SMGRegion pRootObject, SMGNodeMapping pMapping, Set<SMGObject> pAlreadyMatched) {

    Set<SMGObjectTemplate> entryRegions = pMatStep.getEntryRegions();
    Set<SMGObject> alreadyMatched = new HashSet<>();
    alreadyMatched.addAll(pAlreadyMatched);

    MatchResult result;

    for (SMGObjectTemplate entryRegion : entryRegions) {

      result = subSMGmatchStep(pMatStep, pInputSMG, pRootObject, entryRegion, pMapping, alreadyMatched);

      if (result.isMatch()) {
        return result;
      }
    }

    return MatchResult.getUnknownInstance();
  }

  private MatchResult subSMGmatchStep(MaterlisationStep pMatStep, SMG pInputSMG,
      SMGRegion pRootObject, SMGObjectTemplate pEntryRegion, SMGNodeMapping pMapping,
      Set<SMGObject> pAlreadyMatched) {

    /*If entry  region is abstraction, use different matstep to avoid endless loop*/
    if(pEntryRegion instanceof GenericAbstractionCandidateTemplate) {
      return MatchResult.getUnknownInstance();
    }

    Map<SMGObjectTemplate, SMGObject> objectTemplateToObject = new HashMap<>();
    objectTemplateToObject.put(pEntryRegion, pRootObject);
    pAlreadyMatched.add(pRootObject);
    Set<Pair<SMGObject, SMGObjectTemplate>> toBeMatchedLater = new HashSet<>();
    toBeMatchedLater.add(Pair.of((SMGObject) pRootObject, pEntryRegion));

    Set<SMGObject> objectsToBeRemovedForAbstraction = new HashSet<>();
    Map<Integer, Integer> abstractToConcretePointerMapInputSMG = new HashMap<>();
    Set<SMGObject> objectsToBeRemovedForAbstractionInputSMG = new HashSet<>();
    Map<Integer, Integer> abstractToConcretePointerMap = new HashMap<>();
    int score = 1;

    while (!toBeMatchedLater.isEmpty()) {
      Set<Pair<SMGObject, SMGObjectTemplate>> toBeMatched = ImmutableSet.copyOf(toBeMatchedLater);
      toBeMatchedLater = new HashSet<>();

      for (Pair<SMGObject, SMGObjectTemplate> templateObjectPair : toBeMatched) {

        SMGObjectTemplate template = templateObjectPair.getSecond();
        SMGObject object = templateObjectPair.getFirst();

        Pair<Boolean, Integer> matchAndScore = matchObjectTemplateWithObject(template, object,
            pInputSMG, pAlreadyMatched, pMapping, toBeMatchedLater, objectTemplateToObject,
            objectsToBeRemovedForAbstraction, objectsToBeRemovedForAbstractionInputSMG,
            abstractToConcretePointerMap, abstractToConcretePointerMapInputSMG, pMatStep, score);
        boolean match = matchAndScore.getFirst();
        score = matchAndScore.getSecond();


        if (!match) {
          return MatchResult.getUnknownInstance();
        }
      }
    }

    return new MatchResult(true, objectsToBeRemovedForAbstraction, score, abstractToConcretePointerMap, objectsToBeRemovedForAbstractionInputSMG, abstractToConcretePointerMapInputSMG);
  }

  private Pair<Boolean, Integer> matchObjectTemplateWithObject(SMGObjectTemplate pTemplate, SMGObject pObject,
      SMG pInputSMG, Set<SMGObject> pAlreadyMatched, SMGNodeMapping pMapping,
      @SuppressWarnings("unused") Set<Pair<SMGObject, SMGObjectTemplate>> pToBeMatchedLater,
      @SuppressWarnings("unused") Map<SMGObjectTemplate, SMGObject> pObjectTemplateToObject,
      @SuppressWarnings("unused") Set<SMGObject> pObjectsToBeRemovedForAbstraction,
      Set<SMGObject> pObjectsToBeRemovedForAbstractionInputSMG,
      @SuppressWarnings("unused") Map<Integer, Integer> pAbstractToConcretePointerMap,
      @SuppressWarnings("unused") Map<Integer, Integer> pAbstractToConcretePointerMapInputSMG,
      @SuppressWarnings("unused") MaterlisationStep matStep, int pScore) {

    @SuppressWarnings("unused")
    Set<SMGEdgePointsTo> pointsToThisObject;
    @SuppressWarnings("unused")
    Set<SMGEdgeHasValue> fieldsOfObject;
    int score = pScore;

    if (pTemplate instanceof SMGRegion && pObject instanceof SMGRegion
        && ((SMGRegion) pTemplate).getSize() == pObject.getSize()) {
      pointsToThisObject = getPointerToThisObject(pObject, pInputSMG);
      fieldsOfObject = getFieldsOfObject(pObject, pInputSMG);
      pObjectsToBeRemovedForAbstractionInputSMG.add(pObject);

    } else if (pTemplate instanceof GenericAbstractionCandidateTemplate) {
      MatchResult matchOfTemplate = subSMGmatchSpecificShape(pInputSMG, pObject, pMapping,
          (GenericAbstractionCandidateTemplate) pTemplate, pAlreadyMatched);

      if (!matchOfTemplate.isMatch()) {
        return Pair.of(false, score);
      }

      score = score >= matchOfTemplate.getScore() ? score : matchOfTemplate.getScore() + 1;

      Pair<Set<SMGEdgePointsTo>, Set<SMGEdgeHasValue>> pointerAndFieldsOfFoundAbstraction =
          getPointerToAndFieldsFromFoundAbstraction(matchOfTemplate, pInputSMG);

      pointsToThisObject = pointerAndFieldsOfFoundAbstraction.getFirst();
      fieldsOfObject = pointerAndFieldsOfFoundAbstraction.getSecond();
    } else {
      return Pair.of(false, score);
    }




    return Pair.of(true, score);
  }

  private Pair<Set<SMGEdgePointsTo>, Set<SMGEdgeHasValue>> getPointerToAndFieldsFromFoundAbstraction(
      MatchResult pMatchOfTemplate, @SuppressWarnings("unused") SMG pInputSMG) {

    @SuppressWarnings("unused")
    Set<SMGEdgePointsTo> pointsToFoundAbstraction;
    @SuppressWarnings("unused")
    Set<SMGEdgeHasValue> fieldsOfFoundAbstraction;

    Map<Integer, Integer> abstractToConcreteMap =
        pMatchOfTemplate.getAbstractToConcretePointerMap();
    Map<Integer, Integer> abstractToConcreteInputSMGMap =
        pMatchOfTemplate.getAbstractToConcretePointerMapInputSMG();

    Set<Integer> valuesOfPointerOrFields =
        new HashSet<>(abstractToConcreteInputSMGMap.size() + abstractToConcreteMap.size());

    valuesOfPointerOrFields.addAll(abstractToConcreteInputSMGMap.values());

//    for(destValues : abstractToConcreteMap.values()) {

//    }


    return null;
  }

  private Set<SMGEdgeHasValue> getFieldsOfObject(SMGObject pObject, SMG pInputSMG) {

    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(pObject);
    return pInputSMG.getHVEdges(filter);
  }

  private MatchResult wasMatchedPreviously(SMG pInputSMG, SMGObject pRootObject,
      GenericAbstractionCandidateTemplate pTemplate, SMGNodeMapping pMapping) {

    Set<SMGEdgePointsTo> pointsToThisObject = getPointerToThisObject(pRootObject, pInputSMG);

    for (SMGEdgePointsTo pointer : pointsToThisObject) {
      if (pMapping.containsKey(pointer.getValue())) {
        int destPointerValue = pMapping.get(pointer.getValue());

        if (previouslyMatched.containsKey(destPointerValue)) {
          for (GenericAbstractionCandidate abstractionCandidate : previouslyMatched
              .get(destPointerValue)) {

            if (pTemplate.equals(abstractionCandidate
                .createTemplate())) {
              return MatchResult.valueOf(abstractionCandidate);
            }
          }
        }
      }
    }

    return MatchResult.getUnknownInstance();
  }

  private Set<SMGEdgePointsTo> getPointerToThisObject(SMGObject pSmgObject, SMG pInputSMG) {
    Set<SMGEdgePointsTo> result = FluentIterable.from(pInputSMG.getPTEdges().values())
        .filter(new FilterTargetObject(pSmgObject)).toSet();
    return result;
  }

  private static class FilterTargetObject implements Predicate<SMGEdgePointsTo> {

    private final SMGObject object;

    public FilterTargetObject(SMGObject pObject) {
      object = pObject;
    }

    @Override
    public boolean apply(SMGEdgePointsTo ptEdge) {
      return ptEdge.getObject() == object;
    }
  }

  private static class MatchResult {

    private static final MatchResult UNKNOWN = new MatchResult(false, null, 0, null, null, null);

    private final boolean match;
    private final Set<SMGObject> objectsToBeRemovedForAbstraction;
    private final Set<SMGObject> objectsToBeRemovedForAbstractionInputSMG;
    private final int score;
    private final Map<Integer, Integer> abstractToConcretePointerMap;
    private final Map<Integer, Integer> abstractToConcretePointerMapInputSMG;

    public MatchResult(boolean pMatche, Set<SMGObject> pObjectsToBeRemovedForAbstraction,
        int pScore, Map<Integer, Integer> pAbstractToConcretePointerMap,
        Set<SMGObject> pObjectsToBeRemovedForAbstractionInputSMG,
        Map<Integer, Integer> pAbstractToConcretePointerMapInputSMG) {
      match = pMatche;
      objectsToBeRemovedForAbstraction = pObjectsToBeRemovedForAbstraction;
      objectsToBeRemovedForAbstractionInputSMG = pObjectsToBeRemovedForAbstractionInputSMG;
      score = pScore;
      abstractToConcretePointerMap = pAbstractToConcretePointerMap;
      abstractToConcretePointerMapInputSMG = pAbstractToConcretePointerMapInputSMG;
    }

    public static MatchResult valueOf(GenericAbstractionCandidate pAbstractionCandidate) {

      Map<Integer, Integer> abstractToConcretePointerMapInputSMG = ImmutableMap.of();
      Set<SMGObject> objectsToBeRemovedForAbstractionInputSMG = ImmutableSet.of();
      return new MatchResult(true, pAbstractionCandidate.getObjectsToBeRemoved(),
          pAbstractionCandidate.getScore(), pAbstractionCandidate.getAbstractToConcretePointerMap(),
          objectsToBeRemovedForAbstractionInputSMG, abstractToConcretePointerMapInputSMG);
    }

    public static MatchResult valueOf(GenericAbstraction pRootObject) {

      Set<SMGObject> toBeRemoved = new HashSet<>();
      toBeRemoved.add(pRootObject);
      Set<SMGObject> emptySet = ImmutableSet.of();
      Map<Integer, Integer> emptyMap = ImmutableMap.of();

      return new MatchResult(true, emptySet, 100, emptyMap,
          toBeRemoved, pRootObject.getAbstractToConcretePointerMap());
    }

    public static MatchResult getUnknownInstance() {
      return UNKNOWN;
    }

    public Map<Integer, Integer> getAbstractToConcretePointerMap() {
      return abstractToConcretePointerMap;
    }

    @SuppressWarnings("unused")
    public Set<SMGObject> getObjectsToBeRemovedForAbstraction() {
      return objectsToBeRemovedForAbstraction;
    }

    public Map<Integer, Integer> getAbstractToConcretePointerMapInputSMG() {
      return abstractToConcretePointerMapInputSMG;
    }

    @SuppressWarnings("unused")
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
}