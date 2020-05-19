/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdge;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
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

    while (rIterator.hasNext()) {
      rIterator.advance();
      ARGState argState = rIterator.getAbstractState();
      UnmodifiableSMGState smgState = AbstractStates.extractStateByType(argState, SMGState.class);
      CFAEdgeWithAdditionalInfo edgeWithAdditionalInfo =
          CFAEdgeWithAdditionalInfo.of(rIterator.getOutgoingEdge());
      // Move memory leak on return edge
      if (!isMemoryLeakError && description != null && !description.isEmpty()) {
        edgeWithAdditionalInfo.addInfo(SMGConvertingTags.WARNING, description);
        description = null;
      }

      isMemoryLeakError = false;
      Set<Object> toCheck =
          extractAdditionalInfoFromInvalidChain(
              invalidChain, prevSMGState, visitedElems, smgState, edgeWithAdditionalInfo);
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
        if (!containsInvalidElement(smgState.getHeap(), elem)) {
          visitedElems.add(elem);
          for (Object additionalElem : prevSMGState.getCurrentChain()) {
            if (!visitedElems.contains(additionalElem) && !invalidChain.contains(additionalElem)) {
              toCheck.add(additionalElem);
            }
          }
          edgeWithAdditionalInfo.addInfo(
              SMGConvertingTags.NOTE, getNoteMessageOnElement(prevSMGState.getHeap(), elem));

        } else {
          toCheck.add(elem);
        }
      }
    }
    return toCheck;
  }

  private boolean containsInvalidElement(UnmodifiableCLangSMG smg, Object elem) {
    if (elem instanceof SMGObject) {
      SMGObject smgObject = (SMGObject) elem;
      return smg.isHeapObject(smgObject)
          || smg.getGlobalObjects().containsValue(smgObject)
          || isStackObject(smg, smgObject);
    } else if (elem instanceof SMGEdgeHasValue) {
      SMGEdgeHasValue edgeHasValue = (SMGEdgeHasValue) elem;
      SMGEdgeHasValueFilter filter =
          SMGEdgeHasValueFilter.objectFilter(edgeHasValue.getObject())
              .filterAtOffset(edgeHasValue.getOffset())
              .filterHavingValue(edgeHasValue.getValue());
      SMGHasValueEdges edges = smg.getHVEdges(filter);
      return edges.size() != 0;
    } else if (elem instanceof SMGEdgePointsTo) {
      SMGEdgePointsTo edgePointsTo = (SMGEdgePointsTo) elem;
      SMGEdgePointsToFilter filter =
          SMGEdgePointsToFilter.targetObjectFilter(edgePointsTo.getObject())
              .filterAtTargetOffset(edgePointsTo.getOffset())
              .filterHavingValue(edgePointsTo.getValue());
      Set<SMGEdgePointsTo> edges = smg.getPtEdges(filter);
      return !edges.isEmpty();
    } else if (elem instanceof SMGValue) {
      SMGValue smgValue = (SMGValue) elem;
      return smg.getValues().contains(smgValue);
    }
    return false;
  }

  private boolean isStackObject(UnmodifiableCLangSMG smg, SMGObject pObject) {
    String regionLabel = pObject.getLabel();
    for (CLangStackFrame frame : smg.getStackFrames()) {
      if ((frame.containsVariable(regionLabel) && frame.getVariable(regionLabel) == pObject)
          || pObject == frame.getReturnObject()) {

        return true;
      }
    }
    return false;
  }

  private String getNoteMessageOnElement(UnmodifiableCLangSMG smg, Object elem) {
    if (elem instanceof SMGEdge) {
      return "Assign edge";
    } else if (elem instanceof Integer || elem instanceof SMGValue) {
      return "Assign value";
    } else if (elem instanceof SMGObject) {
      SMGObject smgObject = (SMGObject) elem;
      if (isFunctionParameter(smg, smgObject)) {
        return "Function parameter";
      }
      return "Object creation";
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
