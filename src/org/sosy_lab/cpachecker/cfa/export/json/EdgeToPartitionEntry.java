// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json;

import com.google.common.collect.Table;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.EdgeToPartitionsDeserializer;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.PartitionsDeserializer;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.EdgeToPartitionsTableToListConverter;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/**
 * Represents an entry in a {@link Table}<CFAEdge, Integer, Partition>.
 *
 * <p>This record encapsulates information about a CFAEdge, an index, and a Partition.
 *
 * @see EdgeToPartitionsTableToListConverter
 * @see PartitionsDeserializer
 * @see EdgeToPartitionsDeserializer
 */
public record EdgeToPartitionEntry(CFAEdge edge, Integer index, Partition partition) {}
