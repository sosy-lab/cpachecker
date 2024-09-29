// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.PartitionIdResolver;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.PartitionsSerializer;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/**
 * This class is a mixin for {@link Partition}.
 *
 * <p>It prevents cyclic references by serializing the {@link Partition} as index.
 *
 * <p>Since the first occurrences of partitions are fully serialized by {@link
 * PartitionsSerializer}, partitions are always serialized as their index.
 */
@JsonIdentityInfo(
    generator = PropertyGenerator.class,
    resolver = PartitionIdResolver.class,
    scope = Partition.class,
    property = "index")
@JsonIdentityReference(alwaysAsId = true)
public final class PartitionMixin {}
