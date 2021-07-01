// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.smg.SMGAdditionalInfo.Level;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdge;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGReadParams;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.util.AbstractStates;

/** This class collects additional information about possible memory errors in an ARGPath. */
public class AdditionalInfoExtractor {

  /**
   * This method iterates over an ARGPath and returns a new ARGPath enriched with more information
   * about memory errors.
   */
  public CFAPathWithAdditionalInfo createExtendedInfo(ARGPath pPath) {
    // inject additional info for extended witness
    PathIterator rIterator = pPath.reverseFullPathIterator();
    ARGState lastArgState = rIterator.getAbstractState();
    UnmodifiableSMGState state = AbstractStates.extractStateByType(lastArgState, SMGState.class);
    Set<Object> invalidChain = new LinkedHashSet<>(state.getInvalidChain());
    String description = state.getErrorDescription();
    boolean isMemoryLeakError = state.hasMemoryLeaks();
    UnmodifiableSMGState prevSMGState = state;
    Set<Object> visitedElems = new HashSet<>();
    List<CFAEdgeWithAdditionalInfo> pathWithExtendedInfo = new ArrayList<>();
    PersistentMap<String, SMGValue> variablesToCheck = state.getInvalidReads();
    PersistentMap<String, SMGReadParams> readParamsMap = PathCopyingPersistentTreeMap.of();
    for (String s : variablesToCheck.keySet()) {
      readParamsMap = readParamsMap.putAndCopy(s, state.getReadParams().get(s));
    }

    String valueMessage = "";
    while (rIterator.hasNext()) {
      rIterator.advance();
      ARGState argState = rIterator.getAbstractState();
      UnmodifiableSMGState smgState = AbstractStates.extractStateByType(argState, SMGState.class);
      CFAEdgeWithAdditionalInfo edgeWithAdditionalInfo =
          CFAEdgeWithAdditionalInfo.of(rIterator.getOutgoingEdge());
      // Move memory leak on return edge
      if (!isMemoryLeakError && description != null && !description.isEmpty()) {
        edgeWithAdditionalInfo.addInfo(
            SMGConvertingTags.NOTE, SMGAdditionalInfo.of(description, Level.ERROR));
        description = null;
      }

      isMemoryLeakError = false;
      Set<Object> toCheck =
          extractAdditionalInfoFromInvalidChain(
              invalidChain, prevSMGState, visitedElems, smgState, edgeWithAdditionalInfo);
      if (!valueMessage.isEmpty()) {
        edgeWithAdditionalInfo.addInfo(
            SMGConvertingTags.READ_VALUES,
            SMGAdditionalInfo.of(valueMessage, Level.INFO));
      }

      boolean hasChange = false;
      Set<Entry<String, SMGValue>> variablesSet = variablesToCheck.entrySet();
      for (Entry<String, SMGValue> variableEntry : variablesSet) {
        String variableName = variableEntry.getKey();
        if (!smgState.hasEdgeCorrespondedToRead(
            readParamsMap.get(variableName), variableEntry.getValue())) {
          SMGValue oldValue = prevSMGState.getReplacedValue(variableEntry.getValue());
          if (oldValue == null
              || !smgState.hasEdgeCorrespondedToRead(readParamsMap.get(variableName), oldValue)) {
            edgeWithAdditionalInfo.addInfo(
                SMGConvertingTags.WRITE_VALUES,
                SMGAdditionalInfo.of("Write to '" + variableName + "'", Level.WARNING));
            variablesToCheck = variablesToCheck.removeAndCopy(variableName);
            readParamsMap = readParamsMap.removeAndCopy(variableName);
            hasChange = true;
          } else {
            variablesToCheck = variablesToCheck.putAndCopy(variableName, oldValue);
          }
        }
      }
      if (hasChange) {
        for (Entry<String, SMGValue> entry : smgState.getReadValues().entrySet()) {
          variablesToCheck = variablesToCheck.putAndCopy(entry.getKey(), entry.getValue());
        }
        for (Entry<String, SMGReadParams> entry : smgState.getReadParams().entrySet()) {
          readParamsMap = readParamsMap.putAndCopy(entry.getKey(), entry.getValue());
        }
      }

      valueMessage = String.join(", ", getValueMessages(smgState));
      invalidChain = toCheck;
      prevSMGState = smgState;
      pathWithExtendedInfo.add(edgeWithAdditionalInfo);
    }
    return CFAPathWithAdditionalInfo.of(Lists.reverse(pathWithExtendedInfo));
  }

  /**
   * checks the given chain for invalid elements and extracts info about it.
   *
   * @return a set of more elements to be checked.
   */
  private Set<Object> extractAdditionalInfoFromInvalidChain(
      Collection<Object> invalidChain,
      UnmodifiableSMGState prevSMGState,
      Collection<Object> visitedElems,
      UnmodifiableSMGState smgState,
      CFAEdgeWithAdditionalInfo edgeWithAdditionalInfo) {
    Set<Object> toCheck = new LinkedHashSet<>();
    for (Object elem : invalidChain) {
      if (!visitedElems.contains(elem)) {
        if (smgState.hasChangeOnElement(elem, prevSMGState)) {
          if (elem instanceof SMGObject
              && smgState.getHeap().isHeapObject((SMGObject) elem)
              && !smgState.getHeap().isObjectValid((SMGObject) elem)) {
            edgeWithAdditionalInfo.addInfo(
                SMGConvertingTags.NOTE,
                SMGAdditionalInfo.of(
                    "Deallocation of " + ((SMGObject) elem).getLabel(), Level.WARNING));
          } else {
            visitedElems.add(elem);
            edgeWithAdditionalInfo.addInfo(
                SMGConvertingTags.NOTE,
                SMGAdditionalInfo.of(getNoteMessageOnElement(prevSMGState, elem), Level.WARNING));
          }

          for (Object additionalElem : prevSMGState.getCurrentChain()) {
            if (!visitedElems.contains(additionalElem) && !invalidChain.contains(additionalElem)) {
              toCheck.add(additionalElem);
            }
          }

        } else {
          toCheck.add(elem);
        }
      }
    }
    return toCheck;
  }

  public static List<String> getValueMessages(UnmodifiableSMGState smgState) {
    List<String> result = new ArrayList<>();
    PersistentMap<String, SMGValue> readValues = smgState.getReadValues();
    for (Entry<String, SMGValue> entry : readValues.entrySet()) {
      if (smgState.isExplicit(entry.getValue())) {
        result.add(entry.getKey() + " = " + smgState.getExplicit(entry.getValue()));
      }
    }
    return result;
  }

  private boolean isReturnObject(UnmodifiableCLangSMG smg, SMGObject pObject) {
    for (CLangStackFrame frame : smg.getStackFrames()) {
      if (pObject == frame.getReturnObject()) {
        return true;
      }
    }
    return false;
  }

  private String getNoteMessageOnElement(UnmodifiableSMGState pState, Object elem) {
    if (elem instanceof SMGEdgeHasValue) {
      SMGEdgeHasValue edge = (SMGEdgeHasValue) elem;
      SMGValue value = edge.getValue();
      SMGKnownExpValue explicit = pState.getExplicit(value);
      if (explicit != null) {
        return "Assign " + explicit.getValue() + " to " + edge.getObject().getLabel();
      } else {
        return "Assign value to " + edge.getObject().getLabel();
      }
    } else if (elem instanceof SMGEdge || elem instanceof Integer) {
      return "Assign";
    } else if (elem instanceof SMGValue) {
      SMGValue value = (SMGValue) elem;
      SMGKnownExpValue explicit = pState.getExplicit(value);
      if (explicit != null) {
        return "Assign " + explicit.getValue();
      } else {
        return "Assign";
      }
    } else if (elem instanceof SMGObject) {
      SMGObject smgObject = (SMGObject) elem;
      if (isFunctionParameter(pState.getHeap(), smgObject)) {
        return "Function parameter " + smgObject.getLabel();
      } else if (smgObject.getLabel().contains("alloc")) {
        return "Allocate " + smgObject.getLabel();
      } else if (pState.getHeap().isStackObject(smgObject)) {
        return "Variable " + smgObject.getLabel();
      } else if (isReturnObject(pState.getHeap(), smgObject)) {
        return "Return value from function";
      } else {
        return "Create object for " + smgObject.getLabel();
      }
    }
    return null;
  }

  private boolean isFunctionParameter(UnmodifiableCLangSMG smg, SMGObject pObject) {
    String regionLabel = pObject.getLabel();
    for (CLangStackFrame frame : smg.getStackFrames()) {
      for (CParameterDeclaration parameter : frame.getFunctionDeclaration().getParameters()) {
        if (parameter.getName().equals(regionLabel) && frame.getVariable(regionLabel) == pObject) {
          return true;
        }
      }
    }
    return false;
  }
}
