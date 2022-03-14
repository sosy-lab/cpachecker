// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class MaterlisationStep {

  /** Indicates, if no further abstraction remains in this step. */
  private final boolean stop;

  /**
   * These addresses templates represent concrete target addresses of pointers from outside of the
   * abstraction, that point to a region in this abstraction. They are used to create the concrete
   * addresses while materializing a concrete region from this step using existing pointer in the
   * smg.
   */
  private final Set<SMGEdgePointsToTemplate> targetAdressTemplateOfPointer;

  /**
   * Abstract pointers representing concrete pointers that lead to an smgObject within this
   * abstraction generated when materializing this step.
   */
  private final Set<SMGEdgePointsToTemplate> pointerTemplate;

  /**
   * These abstract fields with abstract values represent pointers leading from this abstraction to
   * the outside smg.
   */
  private final Set<SMGEdgeHasValueTemplate> fieldTemplateContainingPointer;

  /**
   * These abstract fields with abstract values represent pointers leading from a region within this
   * abstraction to another region within this abstraction.
   */
  private final Set<SMGEdgeHasValueTemplate> fieldTemplateContainingPointerTemplate;

  /**
   * Abstract fields represent concrete fields that are generated when materializing the fields and
   * values of smgObjects with this step.
   */
  private final Set<SMGEdgeHasValueTemplateWithConcreteValue> fieldTemplateContainingValue;

  /**
   * These abstract smgObjects represent concrete regions or abstractions that are generated when
   * materializing smgObjects with this step.
   */
  private final Set<SMGObjectTemplate> objectTemplates;

  private final Map<SMGValue, SMGValue> uniquePointerTemplateToInterfacePointerTemplate;

  public MaterlisationStep(
      Set<SMGObjectTemplate> pAbstractObjects,
      Set<SMGEdgePointsToTemplate> pAbstractPointer,
      Set<SMGEdgeHasValueTemplateWithConcreteValue> pAbstractFields,
      Set<SMGEdgeHasValueTemplate> pAbstractFieldsToIPointer,
      Set<SMGEdgePointsToTemplate> pAbstractAdressesToOPointer,
      Set<SMGEdgeHasValueTemplate> pAbstractFieldsToOPointer,
      Map<SMGValue, SMGValue> pUniquePointerTemplateToInterfacePointerTemplate,
      boolean pStop) {
    targetAdressTemplateOfPointer = ImmutableSet.copyOf(pAbstractAdressesToOPointer);
    objectTemplates = ImmutableSet.copyOf(pAbstractObjects);
    pointerTemplate = ImmutableSet.copyOf(pAbstractPointer);
    fieldTemplateContainingValue = ImmutableSet.copyOf(pAbstractFields);
    fieldTemplateContainingPointerTemplate = ImmutableSet.copyOf(pAbstractFieldsToIPointer);
    fieldTemplateContainingPointer = ImmutableSet.copyOf(pAbstractFieldsToOPointer);
    uniquePointerTemplateToInterfacePointerTemplate =
        pUniquePointerTemplateToInterfacePointerTemplate;
    stop = pStop;
  }

  public boolean isStopStep() {
    return stop;
  }

  public Set<SMGObjectTemplate> getAbstractObjects() {
    return objectTemplates;
  }

  public Set<SMGEdgeHasValueTemplateWithConcreteValue> getAbstractFields() {
    return fieldTemplateContainingValue;
  }

  public Set<SMGEdgeHasValueTemplate> getAbstractFieldsToOPointer() {
    return fieldTemplateContainingPointer;
  }

  public Set<SMGEdgePointsToTemplate> getAbstractAdressesToOPointer() {
    return targetAdressTemplateOfPointer;
  }

  public Set<SMGEdgeHasValueTemplate> getAbstractFieldsToIPointer() {
    return fieldTemplateContainingPointerTemplate;
  }

  public Set<SMGEdgePointsToTemplate> getAbstractPointers() {
    return pointerTemplate;
  }

  public SMG materialize(SMG pSMG, Map<SMGValue, SMGValue> pAbstractToConcretePointerMap) {

    // TODO throw Inconsistent Abstraction Exception

    /*First, create all new pointerValues from the abstract pointers and map them to their abstract target.*/
    Map<SMGObjectTemplate, Map<SMGValue, SMGValue>> abstractObjectToPointersMap = new HashMap<>();

    for (SMGEdgePointsToTemplate edge : pointerTemplate) {
      assignAbstractToConcretePointer(
          edge, abstractObjectToPointersMap, pAbstractToConcretePointerMap, true);
    }

    for (SMGEdgePointsToTemplate edge : targetAdressTemplateOfPointer) {
      assignAbstractToConcretePointer(
          edge, abstractObjectToPointersMap, pAbstractToConcretePointerMap, false);
    }

    for (SMGEdgeHasValueTemplate edge : fieldTemplateContainingPointerTemplate) {
      assignAbstractToConcretePointer(
          edge, abstractObjectToPointersMap, pAbstractToConcretePointerMap, true);
    }

    for (SMGEdgeHasValueTemplate edge : fieldTemplateContainingPointer) {
      assignAbstractToConcretePointer(
          edge, abstractObjectToPointersMap, pAbstractToConcretePointerMap, false);
    }

    /*Second, create the new concrete Objects from abstract smgObjects.*/
    Map<SMGObjectTemplate, SMGObject> concreteObjectMap = new HashMap<>(objectTemplates.size());

    for (SMGObjectTemplate abstractObject : objectTemplates) {
      Map<SMGValue, SMGValue> abstractToConcretePointerForObject =
          new HashMap<>(abstractObjectToPointersMap.get(abstractObject));

      prepareForGenericAbstraction(abstractToConcretePointerForObject);

      SMGObject concreteObject =
          abstractObject.createConcreteObject(abstractToConcretePointerForObject);
      concreteObjectMap.put(abstractObject, concreteObject);
    }

    Set<SMGEdgePointsTo> concretePointer = new HashSet<>();

    /*Third, create all pointers that point to this abstraction and from this abstraction.*/
    for (SMGEdgePointsToTemplate abstractPointer : pointerTemplate) {
      createPointer(
          abstractPointer, abstractObjectToPointersMap, concreteObjectMap, concretePointer);
    }

    for (SMGEdgePointsToTemplate abstractPointer : targetAdressTemplateOfPointer) {
      createPointer(
          abstractPointer, abstractObjectToPointersMap, concreteObjectMap, concretePointer);
    }

    Set<SMGEdgeHasValue> concreteHves = new HashSet<>();

    for (SMGEdgeHasValueTemplate aHve : fieldTemplateContainingPointerTemplate) {
      createFieldToPointer(aHve, concreteObjectMap, abstractObjectToPointersMap, concreteHves);
    }

    for (SMGEdgeHasValueTemplate aHve : fieldTemplateContainingPointer) {
      createFieldToPointer(aHve, concreteObjectMap, abstractObjectToPointersMap, concreteHves);
    }

    /*Fourth, create all values that are contained in this abstraction*/
    for (SMGEdgeHasValueTemplateWithConcreteValue aField : fieldTemplateContainingValue) {
      SMGObjectTemplate templateObject = aField.getObjectTemplate();
      long offset = aField.getOffset();
      SMGValue value = aField.getValue();
      SMGObject concreteObject = concreteObjectMap.get(templateObject);
      SMGEdgeHasValue concreteHve =
          new SMGEdgeHasValue(aField.getSizeInBits(), offset, concreteObject, value);
      concreteHves.add(concreteHve);
    }

    /*Finally, add all to the SMG.*/
    for (SMGObject object : concreteObjectMap.values()) {
      pSMG.addObject(object);
    }

    for (SMGEdgeHasValue hve : concreteHves) {
      pSMG.addValue(hve.getValue());
      pSMG.addHasValueEdge(hve);
    }

    for (SMGEdgePointsTo pte : concretePointer) {
      pSMG.addValue(pte.getValue());
      pSMG.addPointsToEdge(pte);
    }

    return pSMG;
  }

  private void prepareForGenericAbstraction(
      Map<SMGValue, SMGValue> pAbstractToConcretePointerForObject) {

    for (SMGValue abstractPointer :
        ImmutableSet.copyOf(pAbstractToConcretePointerForObject.keySet())) {
      if (uniquePointerTemplateToInterfacePointerTemplate.containsKey(abstractPointer)) {
        SMGValue value = pAbstractToConcretePointerForObject.get(abstractPointer);
        SMGValue pointerTmp = uniquePointerTemplateToInterfacePointerTemplate.get(abstractPointer);
        pAbstractToConcretePointerForObject.remove(abstractPointer);
        pAbstractToConcretePointerForObject.put(pointerTmp, value);
      }
    }
  }

  private void createFieldToPointer(
      SMGEdgeHasValueTemplate pAbstractField,
      Map<SMGObjectTemplate, SMGObject> pConcreteObjectMap,
      Map<SMGObjectTemplate, Map<SMGValue, SMGValue>> pAbstractObjectToPointersMap,
      Set<SMGEdgeHasValue> concreteHves) {
    SMGObjectTemplate templateObject = pAbstractField.getObjectTemplate();
    SMGObject object = pConcreteObjectMap.get(templateObject);
    long offset = pAbstractField.getOffset();
    SMGValue abstractValue = pAbstractField.getAbstractValue();
    SMGValue value = pAbstractObjectToPointersMap.get(templateObject).get(abstractValue);
    concreteHves.add(new SMGEdgeHasValue(pAbstractField.getSizeInBits(), offset, object, value));
  }

  private void createPointer(
      SMGEdgeTemplate pAbstractPointer,
      Map<SMGObjectTemplate, Map<SMGValue, SMGValue>> pAbstractObjectToPointersMap,
      Map<SMGObjectTemplate, SMGObject> pConcreteObjectMap,
      Set<SMGEdgePointsTo> concretePointer) {

    SMGObjectTemplate templateTarget = pAbstractPointer.getObjectTemplate();
    long offset = pAbstractPointer.getOffset();
    SMGValue abstractPointerValue = pAbstractPointer.getAbstractValue();

    SMGValue concretePointerValue =
        pAbstractObjectToPointersMap.get(templateTarget).get(abstractPointerValue);
    SMGObject concreteObjectTarget = pConcreteObjectMap.get(templateTarget);
    SMGEdgePointsTo edge = new SMGEdgePointsTo(concretePointerValue, concreteObjectTarget, offset);
    concretePointer.add(edge);
  }

  private void assignAbstractToConcretePointer(
      SMGEdgeTemplate edgeOfPointerTemplate,
      Map<SMGObjectTemplate, Map<SMGValue, SMGValue>> pAbstractObjectToPointersMap,
      Map<SMGValue, SMGValue> pAbstractToConcretePointerMap,
      boolean createNewConcreteValue) {
    SMGObjectTemplate objectTemplate = edgeOfPointerTemplate.getObjectTemplate();

    if (!pAbstractObjectToPointersMap.containsKey(objectTemplate)) {
      pAbstractObjectToPointersMap.put(objectTemplate, new HashMap<>());
    }

    SMGValue abstractValue = edgeOfPointerTemplate.getAbstractValue();
    SMGValue value;

    if (createNewConcreteValue) {
      value = SMGKnownSymValue.of();
    } else {
      value = pAbstractToConcretePointerMap.get(abstractValue);
    }

    pAbstractObjectToPointersMap.get(objectTemplate).put(abstractValue, value);
  }

  public Set<SMGRegion> getEntryRegions() {
    return FluentIterable.from(targetAdressTemplateOfPointer)
        .transform(edge -> edge.getObjectTemplate())
        .filter(SMGRegion.class)
        .toSet();
  }

  public Set<SMGEdgePointsToTemplate> getPointerToThisTemplate(SMGObjectTemplate pTemplate) {
    assert objectTemplates.contains(pTemplate);

    return FluentIterable.from(pointerTemplate)
        .append(targetAdressTemplateOfPointer)
        .filter(ptEdge -> ptEdge.getObjectTemplate() == pTemplate)
        .toSet();
  }

  public FieldsOfTemplate getFieldsOfThisTemplate(SMGObjectTemplate pTemplate) {
    assert objectTemplates.contains(pTemplate);

    Set<SMGEdgeHasValueTemplateWithConcreteValue> lFieldTemplateContainingValue =
        FluentIterable.from(fieldTemplateContainingValue)
            .filter(ptEdge -> ptEdge.getObjectTemplate() == pTemplate)
            .toSet();
    Set<SMGEdgeHasValueTemplate> lFieldTemplateContainingPointerTemplate =
        FluentIterable.from(fieldTemplateContainingPointerTemplate)
            .filter(ptEdge -> ptEdge.getObjectTemplate() == pTemplate)
            .toSet();
    Set<SMGEdgeHasValueTemplate> lFieldTemplateContainingPointer =
        FluentIterable.from(fieldTemplateContainingPointer)
            .filter(ptEdge -> ptEdge.getObjectTemplate() == pTemplate)
            .toSet();

    return new FieldsOfTemplate(
        lFieldTemplateContainingValue,
        lFieldTemplateContainingPointerTemplate,
        lFieldTemplateContainingPointer,
        pTemplate);
  }

  public Set<SMGEdgeHasValueTemplate> getFieldsOfValue(SMGValue value) {
    return FluentIterable.from(fieldTemplateContainingValue)
        .filter(SMGEdgeHasValueTemplate.class)
        .append(fieldTemplateContainingPointerTemplate)
        .append(fieldTemplateContainingPointer)
        .filter(edge -> value.equals(edge.getAbstractValue()))
        .toSet();
  }

  public Optional<SMGEdgePointsToTemplate> getPointer(SMGValue value) {

    for (SMGEdgePointsToTemplate edge : pointerTemplate) {
      if (edge.getAbstractValue().equals(value)) {
        return Optional.of(edge);
      }
    }

    for (SMGEdgePointsToTemplate edge : targetAdressTemplateOfPointer) {
      if (edge.getAbstractValue().equals(value)) {
        return Optional.of(edge);
      }
    }

    return Optional.empty();
  }

  public static class FieldsOfTemplate {

    private final SMGObjectTemplate template;

    /**
     * These abstract fields with abstract values represent pointers leading from this abstraction
     * to the outside smg.
     */
    private final Set<SMGEdgeHasValueTemplate> fieldTemplateContainingPointer;

    /**
     * These abstract fields with abstract values represent pointers leading from a region within
     * this abstraction to another region within this abstraction.
     */
    private final Set<SMGEdgeHasValueTemplate> fieldTemplateContainingPointerTemplate;

    /**
     * Abstract fields represent concrete fields that are generated when materializing the fields
     * and values of smgObjects with this step.
     */
    private final Set<SMGEdgeHasValueTemplateWithConcreteValue> fieldTemplateContainingValue;

    public FieldsOfTemplate(
        Set<SMGEdgeHasValueTemplateWithConcreteValue> pFieldTemplateContainingValue,
        Set<SMGEdgeHasValueTemplate> pFieldTemplateContainingPointerTemplate,
        Set<SMGEdgeHasValueTemplate> pFieldTemplateContainingPointer,
        SMGObjectTemplate pTemplate) {
      fieldTemplateContainingPointer = pFieldTemplateContainingPointer;
      fieldTemplateContainingPointerTemplate = pFieldTemplateContainingPointerTemplate;
      fieldTemplateContainingValue = pFieldTemplateContainingValue;
      template = pTemplate;
    }

    public Set<SMGEdgeHasValueTemplateWithConcreteValue> getFieldTemplateContainingValue() {
      return fieldTemplateContainingValue;
    }

    public int size() {
      return fieldTemplateContainingPointer.size()
          + fieldTemplateContainingPointerTemplate.size()
          + fieldTemplateContainingValue.size();
    }

    public Set<SMGEdgeHasValueTemplate> getFieldTemplateContainingPointerTemplate() {
      return fieldTemplateContainingPointerTemplate;
    }

    public Set<SMGEdgeHasValueTemplate> getFieldTemplateContainingPointer() {
      return fieldTemplateContainingPointer;
    }

    public SMGObjectTemplate getTemplate() {
      return template;
    }
  }

  @Override
  public String toString() {
    return targetAdressTemplateOfPointer.toString()
        + pointerTemplate
        + fieldTemplateContainingPointer
        + fieldTemplateContainingPointerTemplate
        + fieldTemplateContainingValue;
  }

  public boolean abstractInterfaceContains(SMGValue abstractValue) {

    for (SMGEdgeHasValueTemplate edge : fieldTemplateContainingPointer) {
      if (edge.getAbstractValue().equals(abstractValue)) {
        return true;
      }
    }

    for (SMGEdgePointsToTemplate edge : targetAdressTemplateOfPointer) {
      if (edge.getAbstractValue().equals(abstractValue)) {
        return true;
      }
    }

    return false;
  }
}
