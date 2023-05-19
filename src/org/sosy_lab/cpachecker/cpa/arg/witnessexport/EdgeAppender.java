// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

interface EdgeAppender {

  void appendNewEdge(
      String pFrom,
      String pTo,
      CFAEdge pEdge,
      Optional<Collection<ARGState>> pFromState,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
      CFAEdgeWithAdditionalInfo pAdditionalInfo)
      throws InterruptedException;

  void appendNewEdgeToSink(
      String pFrom,
      CFAEdge pEdge,
      Optional<Collection<ARGState>> pFromState,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
      CFAEdgeWithAdditionalInfo pAdditionalInfo)
      throws InterruptedException;
}
