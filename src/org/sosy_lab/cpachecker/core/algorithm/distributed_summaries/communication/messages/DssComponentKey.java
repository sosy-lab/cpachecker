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

/**
 * Stable serialized identifier for a distributed-CPA state or precision component.
 *
 * <p>The serialized names are part of the JSON format and should only be changed together with all
 * readers, writers, tests, and tools that consume the format.
 */
public enum DssComponentKey {
  BLOCK_STATE(BlockState.class, SerializedNames.BLOCK_STATE),
  CALLSTACK_STATE(CallstackState.class, SerializedNames.CALLSTACK_STATE),
  FUNCTION_POINTER_STATE(FunctionPointerState.class, SerializedNames.FUNCTION_POINTER_STATE),
  LOCATION_STATE(LocationState.class, SerializedNames.LOCATION_STATE),
  PREDICATE_STATE(PredicateAbstractState.class, SerializedNames.PREDICATE_STATE),
  COMPOSITE_STATE(CompositeState.class, SerializedNames.COMPOSITE_STATE),
  PREDICATE_PRECISION(PredicatePrecision.class, SerializedNames.PREDICATE_PRECISION),
  COMPOSITE_PRECISION(CompositePrecision.class, SerializedNames.COMPOSITE_PRECISION),
  NO_PRECISION(DssNoPrecisionPayload.class, SerializedNames.NO_PRECISION);

  public static final class SerializedNames {
    public static final String BLOCK_STATE = "blockState";
    public static final String CALLSTACK_STATE = "callstackState";
    public static final String FUNCTION_POINTER_STATE = "functionPointerState";
    public static final String LOCATION_STATE = "locationState";
    public static final String PREDICATE_STATE = "predicateState";
    public static final String COMPOSITE_STATE = "compositeState";
    public static final String PREDICATE_PRECISION = "predicatePrecision";
    public static final String COMPOSITE_PRECISION = "compositePrecision";
    public static final String NO_PRECISION = "noPrecision";

    private SerializedNames() {}
  }

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
