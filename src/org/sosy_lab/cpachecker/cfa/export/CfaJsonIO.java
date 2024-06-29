// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.NavigableMap;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/* This class provides a base for exporting and importing CFA data to and from JSON. */
public abstract class CfaJsonIO {

  /* This record represents the CFA data. */
  protected final record CfaJsonData(
      Set<CFANode> nodes,
      Set<CFAEdge> edges,
      NavigableMap<String, FunctionEntryNode> functions,
      CfaMetadata metadata
      ) {}

  /**
   * Configures and provides an instance of {@link ObjectMapper} for CFA serialization and
   * deserialization.
   *
   * @return The configured {@link ObjectMapper} instance which only maps fields and uses
   *     indentation and newlines. On top of that, it includes mixins to prevent redundant mappings.
   */
  protected static final ObjectMapper provideConfiguredCfaObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();

    /* Only map fields of objects. */
    objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
    objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

    /* Enable serialization with indentation and newlines. */
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

    /* Add mixins. */
    objectMapper.addMixIn(FunctionEntryNode.class, FunctionEntryNodeMixin.class);
    objectMapper.addMixIn(FunctionExitNode.class, FunctionExitNodeMixin.class);
    objectMapper.addMixIn(CFANode.class, CfaNodeMixin.class);
    objectMapper.addMixIn(CfaMetadata.class, CfaMetadataMixin.class);
    objectMapper.addMixIn(Partition.class, PartitionMixin.class);

    return objectMapper;
  }

  /**
   * This class is a mixin for {@link FunctionEntryNode}.
   *
   * <p>It serializes its {@link FunctionExitNode} field as number.
   */
  private static final class FunctionEntryNodeMixin {

    @JsonIdentityReference(alwaysAsId = true)
    private FunctionExitNode exitNode;
  }

  /**
   * This class is a mixin for {@link FunctionExitNode}.
   *
   * <p>It serializes its {@link FunctionEntryNode} field as number.
   */
  private static final class FunctionExitNodeMixin {

    @JsonIdentityReference(alwaysAsId = true)
    private FunctionEntryNode entryNode;
  }

  /**
   * This class is a mixin for {@link CFANode}.
   *
   * <p>It prevents cyclic references by serializing the {@link CFANode} as number if the same node
   * has already been fully serialized once.
   */
  @JsonIdentityInfo(
      generator = ObjectIdGenerators.PropertyGenerator.class,
      scope = CFANode.class,
      property = "nodeNumber")
  private static final class CfaNodeMixin {}

  /**
   * This class represents a mixin for {@link CfaMetadata}.
   *
   * <p>It serializes the main {@link FunctionEntryNode} as node number and ensures that the {@link
   * AstCfaRelation} is not serialized.
   */
  private static final class CfaMetadataMixin {

    @JsonIgnore private AstCfaRelation astCFARelation;
  }

  /**
   * This class is a mixin for {@link Partition}.
   *
   * <p>It prevents cyclic references by serializing the {@link Partition} as index if the same
   * object has already been fully serialized once.
   */
  @JsonIdentityInfo(
      generator = ObjectIdGenerators.PropertyGenerator.class,
      scope = Partition.class,
      property = "index")
  private static final class PartitionMixin {}
}
