// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.serialization;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonExport;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.VariableClassificationMixin;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/**
 * A converter that transforms a set of {@link Partition} objects into a sorted list of {@link
 * Partition} objects.
 *
 * <p>The sorting is based on the hash code of the {@link Partition} objects.
 *
 * @see CfaJsonExport
 * @see VariableClassificationMixin
 */
public final class PartitionSetToSortedListConverter
    extends StdConverter<Set<Partition>, List<Partition>> {

  @Override
  public List<Partition> convert(Set<Partition> pSet) {
    List<Partition> list = new ArrayList<>(pSet);
    list.sort(Comparator.comparing(Partition::hashCode));

    return list;
  }
}
