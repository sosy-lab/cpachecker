// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;

public class IntendedIndexRanking implements FaultRanking {

  @Override
  public List<Fault> rank(Set<Fault> result) {
    return result
        .stream()
        .sorted(Comparator.comparingInt(f -> f.getIntendedIndex().orElse(result.size())))
        .collect(Collectors.toList());
  }

}
