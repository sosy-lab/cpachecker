// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.smg.SMGAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.smg.SMGAdditionalInfo.Level;
import org.sosy_lab.cpachecker.cpa.smg.SMGConvertingTags;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class AdditionalInfoExtractor {

  public CFAPathWithAdditionalInfo createExtendedInfo(ARGPath pPath) {
    // inject additional info for extended witness
    PathIterator rIterator = pPath.reverseFullPathIterator();
    ARGState lastArgState = rIterator.getAbstractState();
    SMGState state = AbstractStates.extractStateByType(lastArgState, SMGState.class);
    List<Object> invalidChain = null;
    List<SMGErrorInfo> errorInfos = state.getErrorInfo();
    boolean isMemoryLeakError = state.hasMemoryLeak();
    SMGState prevSMGState = state;
    Set<Object> visitedElems = new HashSet<>();
    List<CFAEdgeWithAdditionalInfo> pathWithExtendedInfo = new ArrayList<>();

    while (rIterator.hasNext()) {
      rIterator.advance();
      if (rIterator.isPositionWithState()) {
        ARGState argState = rIterator.getAbstractState();
        SMGState smgState = AbstractStates.extractStateByType(argState, SMGState.class);
        CFAEdgeWithAdditionalInfo edgeWithAdditionalInfo =
            CFAEdgeWithAdditionalInfo.of(rIterator.getOutgoingEdge());
        if (errorInfos != null && !errorInfos.isEmpty()) {
          invalidChain = errorInfos.get(0).getInvalidChain();
        }

        // Move memory leak on return edge
        if (!isMemoryLeakError && errorInfos != null && !errorInfos.isEmpty()) {
          // We assume that there is only 1 error info per state (as the SV-COMP rules dictate)
          edgeWithAdditionalInfo.addInfo(
              SMGConvertingTags.NOTE,
              SMGAdditionalInfo.of(errorInfos.get(0).getErrorDescription(), Level.ERROR));
          errorInfos = null;
        }

        isMemoryLeakError = false;
        List<Object> toCheck =
            extractAdditionalInfoFromInvalidChain(
                invalidChain, prevSMGState, visitedElems, smgState, edgeWithAdditionalInfo);
        invalidChain = toCheck;
        prevSMGState = smgState;
        pathWithExtendedInfo.add(edgeWithAdditionalInfo);
      }
    }
    return CFAPathWithAdditionalInfo.of(Lists.reverse(pathWithExtendedInfo));
  }

  /**
   * checks the given chain for invalid elements and extracts info about it.
   *
   * @return a set of more elements to be checked.
   */
  @SuppressWarnings("unused")
  private List<Object> extractAdditionalInfoFromInvalidChain(
      Collection<Object> invalidChain,
      SMGState prevSMGState,
      Collection<Object> visitedElems,
      SMGState smgState,
      CFAEdgeWithAdditionalInfo edgeWithAdditionalInfo) {
    List<Object> toCheck = new ArrayList<>();
    for (Object elem : invalidChain) {
      if (!visitedElems.contains(elem)) {
        if (!containsInvalidElement(smgState, elem)) {
          visitedElems.add(elem);
          if (!prevSMGState.getErrorInfo().isEmpty()) {
            for (Object additionalElem : prevSMGState.getErrorInfo().get(0).getInvalidChain()) {
              if (!visitedElems.contains(additionalElem)
                  && !invalidChain.contains(additionalElem)) {
                toCheck.add(additionalElem);
              }
            }
          }
          /*
          // Extract infos like "malloc in line"
          // TODO:
          edgeWithAdditionalInfo.addInfo(
              SMGConvertingTags.NOTE,
              SMGAdditionalInfo.of(
                  getNoteMessageOnElement(prevSMGState, elem), Level.NOTE));*/

        } else {
          toCheck.add(elem);
        }
      }
    }
    return toCheck;
  }

  private boolean containsInvalidElement(SMGState pSMGState, Object elem) {
    if (elem instanceof SMGObject smgObject) {
      return pSMGState.getMemoryModel().isHeapObject(smgObject)
          || pSMGState.getMemoryModel().getGlobalVariableToSmgObjectMap().containsValue(smgObject)
          || pSMGState.isSMGObjectAStackVariable(smgObject);
    } else if (elem instanceof SMGHasValueEdge hasValueEdge) {
      return pSMGState.getMemoryModel().getSmg().getHVEdges().contains(hasValueEdge);
    } else if (elem instanceof SMGPointsToEdge pointsToEdge) {
      return pSMGState.getMemoryModel().getSmg().getPTEdges().contains(pointsToEdge);
    } else if (elem instanceof SMGValue smgValue) {
      return pSMGState.getMemoryModel().getSmg().getValues().contains(smgValue);
    } else if (elem instanceof Value value) {
      return pSMGState.getMemoryModel().getSMGValueFromValue(value).isPresent()
          && containsInvalidElement(
              pSMGState, pSMGState.getMemoryModel().getSMGValueFromValue(value));
    }
    return false;
  }
}
