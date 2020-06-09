// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

public class IdentityRanking implements FaultRanking {

  @Override
  public List<Fault> rank(
      Set<Fault> pFaults) {
    pFaults.forEach(c -> c.addInfo(FaultInfo.hint("Ranked by Identity.")));
    return new ArrayList<>(pFaults);
  }

}
