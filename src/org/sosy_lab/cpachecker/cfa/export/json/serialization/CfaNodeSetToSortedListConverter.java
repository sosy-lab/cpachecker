// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.serialization;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonExport;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.LoopMixin;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * A converter that transforms a set of {@link CFANode} objects into a sorted list of {@link
 * CFANode} objects.
 *
 * <p>The sorting is based on {@link CFANode#compareTo(CFANode)}.
 *
 * @see CfaJsonExport
 * @see LoopMixin
 */
public final class CfaNodeSetToSortedListConverter
    extends StdConverter<Set<CFANode>, List<CFANode>> {

  @Override
  public List<CFANode> convert(Set<CFANode> pSet) {
    return pSet.stream().sorted().toList();
  }
}
