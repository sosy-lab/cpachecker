// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.TreeMultimap;
import java.util.NavigableMap;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.PartitionsDeserializer;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.PartitionsSerializer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/* This record represents the CFA data. */
public record CfaJsonData(
    TreeMultimap<String, CFANode> nodes,
    Set<CFAEdge> edges,
    NavigableMap<String, FunctionEntryNode> functions,
    @JsonSerialize(using = PartitionsSerializer.class)
        @JsonDeserialize(using = PartitionsDeserializer.class)
        Set<Partition> partitions,
    CfaMetadata metadata) {}
