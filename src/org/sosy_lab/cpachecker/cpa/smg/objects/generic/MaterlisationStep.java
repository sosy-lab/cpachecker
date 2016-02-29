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
package org.sosy_lab.cpachecker.cpa.smg.objects.generic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;


public class MaterlisationStep {

  /**
   * Indicates, if no further abstraction remains in this step.
   */
  private final boolean stop;

  /**
   * These addresses templates represent concrete target addresses of pointers from outside of the
   * abstraction, that point to a region in this abstraction. They are used to
   * create the concrete addresses while materializing a concrete region from this step
   * using existing pointer in the smg.
   */
  private final Set<SMGEdgePointsToTemplate> targetAdressTemplateOfPointer;

  /**
   * Abstract pointers representing concrete pointers that lead to
   * an smgObject within this abstraction generated when materializing this step.
   */
  final private Set<SMGEdgePointsToTemplate> pointerTemplate;

  /**
   * These abstract fields with abstract values represent pointers leading from this
   * abstraction to the outside smg.
   */
  final private Set<SMGEdgeHasValueTemplate> abstractFieldsToOPointer;

  /**
   * These abstract fields with abstract values represent pointers leading from a region within
   * this abstraction to another region within this abstraction.
   */
  private final Set<SMGEdgeHasValueTemplate> abstractFieldsToIPointer;

  /**
   * Abstract fields represent concrete fields that are generated
   * when materializing the fields and values of smgObjects with this step.
   */
  final private Set<SMGEdgeHasValueTemplateWithConcreteValue> abstractFields;

  /**
   * These abstract smgObjects represent concrete regions or abstractions
   *  that are generated when materializing smgObjects with this step.
   */
  final private Set<SMGObjectTemplate> objectTemplates;

  public MaterlisationStep(Set<SMGObjectTemplate> pAbstractObjects,
      Set<SMGEdgePointsToTemplate> pAbstractPointer,
      Set<SMGEdgeHasValueTemplateWithConcreteValue> pAbstractFields,
      Set<SMGEdgeHasValueTemplate> pAbstractFieldsToIPointer,
      Set<SMGEdgePointsToTemplate> pAbstractAdressesToOPointer,
      Set<SMGEdgeHasValueTemplate> pAbstractFieldsToOPointer,
      boolean pStop) {
    targetAdressTemplateOfPointer = ImmutableSet.copyOf(pAbstractAdressesToOPointer);
    objectTemplates = ImmutableSet.copyOf(pAbstractObjects);
    pointerTemplate = ImmutableSet.copyOf(pAbstractPointer);
    abstractFields = ImmutableSet.copyOf(pAbstractFields);
    abstractFieldsToIPointer = ImmutableSet.copyOf(pAbstractFieldsToIPointer);
    abstractFieldsToOPointer = ImmutableSet.copyOf(pAbstractFieldsToOPointer);
    stop = pStop;
  }

  public boolean isStopStep() {
    return stop;
  }

  public Set<SMGObjectTemplate> getAbstractObjects() {
    return objectTemplates;
  }

  public Set<SMGEdgeHasValueTemplateWithConcreteValue> getAbstractFields() {
    return abstractFields;
  }


  public Set<SMGEdgeHasValueTemplate> getAbstractFieldsToOPointer() {
    return abstractFieldsToOPointer;
  }

  public Set<SMGEdgePointsToTemplate> getAbstractAdressesToOPointer() {
    return targetAdressTemplateOfPointer;
  }

  public Set<SMGEdgeHasValueTemplate> getAbstractFieldsToIPointer() {
    return abstractFieldsToIPointer;
  }

  public Set<SMGEdgePointsToTemplate> getAbstractPointers() {
    return pointerTemplate;
  }

  public SMG materialize(SMG pSMG, Map<Integer, Integer> pAbstractToConcretePointerMap) {

    //TODO throw Inconsistent Abstraction Exception

    /*First, create all new pointerValues from the abstract pointers and map them to their abstract target.*/
    Map<SMGObjectTemplate, Map<Integer, Integer>> abstractObjectToPointersMap =
        new HashMap<>();

    for (SMGEdgePointsToTemplate edge : pointerTemplate) {
      assignAbstractToConcretePointer(edge, abstractObjectToPointersMap, pAbstractToConcretePointerMap, true);
    }

    for(SMGEdgePointsToTemplate edge : targetAdressTemplateOfPointer) {
      assignAbstractToConcretePointer(edge, abstractObjectToPointersMap, pAbstractToConcretePointerMap, false);
    }

    for(SMGEdgeHasValueTemplate edge : abstractFieldsToIPointer) {
      assignAbstractToConcretePointer(edge, abstractObjectToPointersMap, pAbstractToConcretePointerMap, true);
    }

    for (SMGEdgeHasValueTemplate edge : abstractFieldsToOPointer) {
      assignAbstractToConcretePointer(edge, abstractObjectToPointersMap, pAbstractToConcretePointerMap, false);
    }

    /*Second, create the new concrete Objects from abstract smgObjects.*/
    Map<SMGObjectTemplate, SMGObject> concreteObjectMap = new HashMap<>(objectTemplates.size());

    for (SMGObjectTemplate abstractObject : objectTemplates) {
      Map<Integer, Integer> abstractToConcretePointerForObject =
          abstractObjectToPointersMap.get(abstractObject);
      SMGObject concreteObject =
          abstractObject.createConcreteObject(abstractToConcretePointerForObject);
      concreteObjectMap.put(abstractObject, concreteObject);
    }

    Set<SMGEdgePointsTo> concretePointer = new HashSet<>();

    /*Third, create all pointers that point to this abstraction and from this abstraction.*/
    for (SMGEdgePointsToTemplate abstractPointer : pointerTemplate) {
      createPointer(abstractPointer, abstractObjectToPointersMap, concreteObjectMap, concretePointer);
    }

    for (SMGEdgePointsToTemplate abstractPointer : targetAdressTemplateOfPointer) {
      createPointer(abstractPointer, abstractObjectToPointersMap, concreteObjectMap,
          concretePointer);
    }

    Set<SMGEdgeHasValue> concreteHves = new HashSet<>();

    for (SMGEdgeHasValueTemplate aHve : abstractFieldsToIPointer) {
      createFieldToPointer(aHve, concreteObjectMap, abstractObjectToPointersMap, concreteHves);
    }

    for (SMGEdgeHasValueTemplate aHve : abstractFieldsToOPointer) {
      createFieldToPointer(aHve, concreteObjectMap, abstractObjectToPointersMap, concreteHves);
    }

    /*Fourth, create all values that are contained in this abstraction*/
    for (SMGEdgeHasValueTemplateWithConcreteValue aField : abstractFields) {
      SMGObjectTemplate templateObject = aField.getObjectTemplate();
      int offset = aField.getOffset();
      int value = aField.getValue();
      SMGObject concreteObject = concreteObjectMap.get(templateObject);
      CType type = aField.getType();
      SMGEdgeHasValue concreteHve = new SMGEdgeHasValue(type, offset, concreteObject, value);
      concreteHves.add(concreteHve);
    }

    /*Finally, add all to the SMG.*/
    for (SMGObject object : concreteObjectMap.values()) {
      pSMG.addObject(object);
    }

    for (SMGEdgeHasValue hve : concreteHves) {
      int value = hve.getValue();

      if (!pSMG.getValues().contains(value)) {
        pSMG.addValue(value);
      }

      pSMG.addHasValueEdge(hve);
    }

    for (SMGEdgePointsTo pte : concretePointer) {
      int value = pte.getValue();

      if (!pSMG.getValues().contains(value)) {
        pSMG.addValue(value);
      }

      pSMG.addPointsToEdge(pte);
    }

    return pSMG;
  }

  private void createFieldToPointer(SMGEdgeHasValueTemplate pAbstractField,
      Map<SMGObjectTemplate, SMGObject> pConcreteObjectMap,
      Map<SMGObjectTemplate, Map<Integer, Integer>> pAbstractObjectToPointersMap,
      Set<SMGEdgeHasValue> concreteHves) {
    SMGObjectTemplate templateObject = pAbstractField.getObjectTemplate();
    SMGObject object = pConcreteObjectMap.get(templateObject);
    int offset = pAbstractField.getOffset();
    int abstractValue = pAbstractField.getAbstractValue();
    int value = pAbstractObjectToPointersMap.get(templateObject).get(abstractValue);
    CType type = pAbstractField.getType();
    concreteHves.add(new SMGEdgeHasValue(type, offset, object, value));
  }

  private void createPointer(SMGEdgeTemplate pAbstractPointer,
      Map<SMGObjectTemplate, Map<Integer, Integer>> pAbstractObjectToPointersMap,
      Map<SMGObjectTemplate, SMGObject> pConcreteObjectMap,
      Set<SMGEdgePointsTo> concretePointer) {

    SMGObjectTemplate templateTarget = pAbstractPointer.getObjectTemplate();
    int offset = pAbstractPointer.getOffset();
    int abstractPointerValue = pAbstractPointer.getAbstractValue();

    int concretePointerValue =
        pAbstractObjectToPointersMap.get(templateTarget).get(abstractPointerValue);
    SMGObject concreteObjectTarget = pConcreteObjectMap.get(templateTarget);
    SMGEdgePointsTo edge = new SMGEdgePointsTo(concretePointerValue, concreteObjectTarget, offset);
    concretePointer.add(edge);
  }

  private void assignAbstractToConcretePointer(SMGEdgeTemplate edgeOfPointerTemplate,
      Map<SMGObjectTemplate, Map<Integer, Integer>> pAbstractObjectToPointersMap,
      Map<Integer, Integer> pAbstractToConcretePointerMap,
      boolean createNewConcreteValue) {
    SMGObjectTemplate objectTemplate = edgeOfPointerTemplate.getObjectTemplate();

    if (!pAbstractObjectToPointersMap.containsKey(objectTemplate)) {
      pAbstractObjectToPointersMap.put(objectTemplate, new HashMap<Integer, Integer>());
    }

    int abstractValue = edgeOfPointerTemplate.getAbstractValue();
    int value;

    if (createNewConcreteValue) {
      value = SMGValueFactory.getNewValue();
    } else {
      value = pAbstractToConcretePointerMap.get(abstractValue);
    }

    pAbstractObjectToPointersMap.get(objectTemplate).put(abstractValue, value);
  }

  public Set<SMGObjectTemplate> getEntryRegions() {

    Set<SMGObjectTemplate> entryRegions;

    entryRegions =
        FluentIterable.from(targetAdressTemplateOfPointer).filter(
            new Predicate<SMGEdgePointsToTemplate>() {

              @Override
              public boolean apply(SMGEdgePointsToTemplate edge) {
                return edge.getObjectTemplate() instanceof SMGRegion;
              }
            }).transform(new Function<SMGEdgePointsToTemplate, SMGObjectTemplate>() {

              @Override
              public SMGObjectTemplate apply(SMGEdgePointsToTemplate edge) {
                return edge.getObjectTemplate();
              }
            }).toSet();

    return entryRegions;
  }

  public Set<SMGEdgePointsToTemplate> getPointerToThisTemplate(SMGObjectTemplate pTemplate) {

    assert  objectTemplates.contains(pTemplate);



    return null;

  }

  public FieldsOfTemplate getFieldsOfThisTemplate(SMGObjectTemplate pTemplate) {

    assert  objectTemplates.contains(pTemplate);

    @SuppressWarnings("unused")
    Predicate<SMGEdgePointsToTemplate> objectFilter = new SMGUtils.FilterTargetTemplate(pTemplate);


    return null;
  }

  public static class FieldsOfTemplate {

  }
}