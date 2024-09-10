// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.TreeMultimap;
import java.util.NavigableMap;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/* This class provides a base for exporting and importing CFA data to and from JSON. */
public final class CfaJsonIO {

  /* This record represents the CFA data. */
  final record CfaJsonData(
      TreeMultimap<String, CFANode> nodes,
      Set<CFAEdge> edges,
      NavigableMap<String, FunctionEntryNode> functions,
      Set<Partition> partitions,
      CfaMetadata metadata) {}

  /**
   * Configures and provides an instance of {@link ObjectMapper} for CFA serialization and
   * deserialization.
   *
   * @return The configured {@link ObjectMapper} instance which only maps fields and uses
   *     indentation and newlines.
   */
  protected static final ObjectMapper provideConfiguredCfaObjectMapper() {
    return JsonMapper.builder()

        /* Only map fields of objects. */
        .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

        /* Enable serialization with indentation and newlines. */
        .enable(SerializationFeature.INDENT_OUTPUT)

        /* Add modules. */
        .addModule(new CfaJsonModule())
        .addModule(new GuavaModule())
        .addModule(new Jdk8Module())
        .build();
  }
}
