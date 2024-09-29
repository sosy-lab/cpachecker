// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.serialization;

import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.export.json.TableEntry;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/**
 * A converter class that converts a {@link Table} object to a list of {@link TableEntry} objects.
 *
 * <p>The Table object represents a mapping between CFAEdges, Integers, and Partitions
 * (EdgeToPartitions).
 */
public final class EdgeToPartitionsTableToListConverter
    extends StdConverter<Table<CFAEdge, Integer, Partition>, List<TableEntry>> {
  @Override
  public List<TableEntry> convert(Table<CFAEdge, Integer, Partition> pTable) {
    return pTable.cellSet().stream()
        .filter(cell -> cell.getValue() != null)
        .map(cell -> new TableEntry(cell.getRowKey(), cell.getColumnKey(), cell.getValue()))
        .collect(ImmutableList.toImmutableList());
  }
}
