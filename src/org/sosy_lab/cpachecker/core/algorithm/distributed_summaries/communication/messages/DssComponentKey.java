// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;

public enum DssComponentKey {
  BLOCK_STATE(BlockState.class, "blockState"),
  CALLSTACK_STATE(CallstackState.class, "callstackState"),
  FUNCTION_POINTER_STATE(FunctionPointerState.class, "functionPointerState"),
  LOCATION_STATE(LocationState.class, "locationState"),
  PREDICATE_STATE(PredicateAbstractState.class, "predicateState"),
  COMPOSITE_STATE(CompositeState.class, "compositeState"),
  PREDICATE_PRECISION(PredicatePrecision.class, "predicatePrecision"),
  COMPOSITE_PRECISION(CompositePrecision.class, "compositePrecision"),
  NO_PRECISION(DssNoPrecisionPayload.class, "noPrecision");

  private static final ImmutableMap<Class<?>, DssComponentKey> BY_CLASS =
      Arrays.stream(values())
          .collect(ImmutableMap.toImmutableMap(DssComponentKey::getJavaClass, key -> key));
  private static final ImmutableMap<String, DssComponentKey> BY_SERIALIZED_NAME =
      Arrays.stream(values())
          .collect(ImmutableMap.toImmutableMap(DssComponentKey::getSerializedName, key -> key));

  private final Class<?> javaClass;
  private final String serializedName;

  DssComponentKey(Class<?> pJavaClass, String pSerializedName) {
    this.javaClass = pJavaClass;
    this.serializedName = pSerializedName;
  }

  public Class<?> getJavaClass() {
    return javaClass;
  }

  public String getSerializedName() {
    return serializedName;
  }

  public static DssComponentKey forClass(Class<?> pClass) {
    DssComponentKey key = BY_CLASS.get(pClass);
    Preconditions.checkArgument(key != null, "No DSS payload key registered for class %s", pClass);
    return key;
  }

  public static DssComponentKey forSerializedName(String pName) {
    DssComponentKey key = BY_SERIALIZED_NAME.get(pName);
    Preconditions.checkArgument(key != null, "No Class registered for serialized name %s", pName);
    return key;
  }

  public static Optional<DssComponentKey> tryForSerializedName(String pName) {
    DssComponentKey key = BY_SERIALIZED_NAME.get(pName);
    return key == null ? Optional.empty() : Optional.of(key);
  }

  public static String serializedNameFor(Class<?> pClass) {
    return forClass(pClass).getSerializedName();
  }

  public static Class<?> classFor(String pName) {
    return forSerializedName(pName).getJavaClass();
  }
}
