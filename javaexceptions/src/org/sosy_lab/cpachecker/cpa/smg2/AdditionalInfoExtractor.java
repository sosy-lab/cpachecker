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
import java.util.List;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;

public class AdditionalInfoExtractor {

  public CFAPathWithAdditionalInfo createExtendedInfo(ARGPath pPath) {
    // inject additional info for extended witness
    PathIterator rIterator = pPath.reverseFullPathIterator();
    // ARGState lastArgState = rIterator.getAbstractState();
    // SMGState state = AbstractStates.extractStateByType(lastArgState, SMGState.class);
    // List<SMGErrorInfo> description = state.getErrorInfo();
    // boolean isMemoryLeakError = state.hasMemoryLeaks();
    // SMGState prevSMGState = state;
    // Set<Object> visitedElems = new HashSet<>();
    List<CFAEdgeWithAdditionalInfo> pathWithExtendedInfo = new ArrayList<>();

    while (rIterator.hasNext()) {
      rIterator.advance();
      // ARGState argState = rIterator.getAbstractState();
      // SMGState smgState = AbstractStates.extractStateByType(argState, SMGState.class);
      CFAEdgeWithAdditionalInfo edgeWithAdditionalInfo =
          CFAEdgeWithAdditionalInfo.of(rIterator.getOutgoingEdge());
      /*
      // Move memory leak on return edge
      if (!isMemoryLeakError && description != null && !description.isEmpty()) {
        edgeWithAdditionalInfo.addInfo(
            SMGConvertingTags.NOTE, SMGAdditionalInfo.of(description, Level.ERROR));
        description = null;
      }
      */

      // isMemoryLeakError = false;
      // prevSMGState = smgState;
      pathWithExtendedInfo.add(edgeWithAdditionalInfo);
    }
    return CFAPathWithAdditionalInfo.of(Lists.reverse(pathWithExtendedInfo));
  }
}
