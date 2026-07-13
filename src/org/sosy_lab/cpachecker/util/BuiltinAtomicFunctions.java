// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Helper for handling the GCC atomic builtin functions (names starting with {@code __atomic_}).
 *
 * <p>The semantics of these builtins are documented at <a
 * href="https://gcc.gnu.org/onlinedocs/gcc/_005f_005fatomic-Builtins.html">GCC: Built-in Functions
 * for Memory Model Aware Atomic Operations</a>.
 *
 * <p>Besides recognizing the builtins, this class expresses their semantics as plain {@link
 * CExpression}s (cf. {@link #getPointerTarget} and {@link #getUpdatedValue}). Analyses can thus
 * reuse the encoding instead of re-implementing the semantics of each builtin.
 *
 * <p>CPAchecker assumes a sequentially consistent memory model, so the memory-order arguments carry
 * no information beyond {@code __ATOMIC_SEQ_CST}, and the fences degenerate to no-ops. Callers are
 * expected to reject any other memory order; see {@link CAtomicOperations#getMemoryOrderIndices}.
 *
 * <p>Builtins that are absent from {@link CAtomicOperations}, such as the lock-free queries whose
 * result is implementation defined, must be rejected as unsupported. Treating them as pure external
 * functions would silently discard the memory effect of an atomic store.
 */
public final class BuiltinAtomicFunctions {

  private BuiltinAtomicFunctions() {}

  /** Value of GCC's {@code __ATOMIC_SEQ_CST}, the only memory order CPAchecker supports. */
  public static final BigInteger MEMORY_ORDER_SEQ_CST = BigInteger.valueOf(5);

  /** The kind of memory effect an atomic builtin has. */
  public enum CAtomicOperationType {
    /** Reads {@code *ptr}. */
    LOAD,
    /** Writes {@code *ptr}. */
    STORE,
    /** Writes {@code *ptr} and yields its previous value. */
    EXCHANGE,
    /** Writes {@code *ptr} only if it currently equals {@code *expected}. */
    CMP_XCHG,
    /** Updates {@code *ptr} and yields the value it held <em>before</em> the update. */
    FETCH_OP,
    /** Updates {@code *ptr} and yields the value it holds <em>after</em> the update. */
    OP_FETCH,
    /** Sets {@code *ptr} and yields whether it was already set. */
    TEST_AND_SET,
    /** Clears {@code *ptr}. */
    CLEAR,
    /** Orders memory accesses; a no-op under sequential consistency. */
    FENCE
  }

  /**
   * The atomic builtins that CPAchecker understands.
   *
   * <p>The builtins come in two flavours. The {@code _n} forms take the operand <em>by value</em>,
   * while what GCC calls the <em>generic version</em> of a builtin ({@code __atomic_store}, {@code
   * __atomic_load}, ...) takes it <em>by pointer</em>; see {@link #isGeneric}.
   *
   * <p>The documentation of each constant paraphrases the semantics given by <a
   * href="https://gcc.gnu.org/onlinedocs/gcc/_005f_005fatomic-Builtins.html">GCC: Built-in
   * Functions for Memory Model Aware Atomic Operations</a>.
   */
  public enum CAtomicOperations {
    /**
     * {@code type __atomic_load_n (type *ptr, int memorder)} returns the contents of {@code *ptr}.
     */
    ATOMIC_LOAD_N("__atomic_load_n", CAtomicOperationType.LOAD, false, 1, ImmutableList.of(1)),
    /**
     * {@code void __atomic_load (type *ptr, type *ret, int memorder)} is the generic version of an
     * atomic load. It returns the contents of {@code *ptr} in {@code *ret}.
     */
    ATOMIC_LOAD("__atomic_load", CAtomicOperationType.LOAD, true, 2, ImmutableList.of(2)),
    /**
     * {@code void __atomic_store_n (type *ptr, type val, int memorder)} writes {@code val} into
     * {@code *ptr}.
     */
    ATOMIC_STORE_N("__atomic_store_n", CAtomicOperationType.STORE, false, 2, ImmutableList.of(2)),
    /**
     * {@code void __atomic_store (type *ptr, type *val, int memorder)} is the generic version of an
     * atomic store. It stores the value of {@code *val} into {@code *ptr}.
     */
    ATOMIC_STORE("__atomic_store", CAtomicOperationType.STORE, true, 2, ImmutableList.of(2)),
    /**
     * {@code type __atomic_exchange_n (type *ptr, type val, int memorder)} writes {@code val} into
     * {@code *ptr} and returns the previous contents of {@code *ptr}.
     */
    ATOMIC_EXCHANGE_N(
        "__atomic_exchange_n", CAtomicOperationType.EXCHANGE, false, 2, ImmutableList.of(2)),
    /**
     * {@code void __atomic_exchange (type *ptr, type *val, type *ret, int memorder)} is the generic
     * version of an atomic exchange. It stores the contents of {@code *val} into {@code *ptr}, and
     * the original value of {@code *ptr} into {@code *ret}.
     */
    ATOMIC_EXCHANGE(
        "__atomic_exchange", CAtomicOperationType.EXCHANGE, true, 3, ImmutableList.of(3)),
    /**
     * {@code bool __atomic_compare_exchange_n (type *ptr, type *expected, type desired, bool weak,
     * int success_memorder, int failure_memorder)} compares the contents of {@code *ptr} with the
     * contents of {@code *expected}. If equal, it writes {@code desired} into {@code *ptr}. If not,
     * the current contents of {@code *ptr} are written into {@code *expected}. The result reports
     * whether {@code desired} was written.
     */
    ATOMIC_CMP_XCHG_N(
        "__atomic_compare_exchange_n",
        CAtomicOperationType.CMP_XCHG,
        false,
        3,
        ImmutableList.of(4, 5),
        /* pWeakArgumentIndex= */ 3),
    /**
     * {@code bool __atomic_compare_exchange (type *ptr, type *expected, type *desired, bool weak,
     * int success_memorder, int failure_memorder)} is the generic version of {@link
     * #ATOMIC_CMP_XCHG_N}, except that the desired value is also a pointer.
     */
    ATOMIC_CMP_XCHG(
        "__atomic_compare_exchange",
        CAtomicOperationType.CMP_XCHG,
        true,
        3,
        ImmutableList.of(4, 5),
        /* pWeakArgumentIndex= */ 3),

    /**
     * {@code type __atomic_fetch_add (type *ptr, type val, int memorder)}, i.e. {@code tmp = *ptr;
     * *ptr += val; return tmp;}.
     */
    ATOMIC_FETCH_ADD("__atomic_fetch_add", CAtomicOperationType.FETCH_OP, BinaryOperator.PLUS),
    /**
     * {@code type __atomic_fetch_sub (type *ptr, type val, int memorder)}, cf. {@link
     * #ATOMIC_FETCH_ADD}.
     */
    ATOMIC_FETCH_SUB("__atomic_fetch_sub", CAtomicOperationType.FETCH_OP, BinaryOperator.MINUS),
    /**
     * {@code type __atomic_fetch_and (type *ptr, type val, int memorder)}, cf. {@link
     * #ATOMIC_FETCH_ADD}.
     */
    ATOMIC_FETCH_AND(
        "__atomic_fetch_and", CAtomicOperationType.FETCH_OP, BinaryOperator.BITWISE_AND),
    /**
     * {@code type __atomic_fetch_or (type *ptr, type val, int memorder)}, cf. {@link
     * #ATOMIC_FETCH_ADD}.
     */
    ATOMIC_FETCH_OR("__atomic_fetch_or", CAtomicOperationType.FETCH_OP, BinaryOperator.BITWISE_OR),
    /**
     * {@code type __atomic_fetch_xor (type *ptr, type val, int memorder)}, cf. {@link
     * #ATOMIC_FETCH_ADD}.
     */
    ATOMIC_FETCH_XOR(
        "__atomic_fetch_xor", CAtomicOperationType.FETCH_OP, BinaryOperator.BITWISE_XOR),
    /**
     * {@code type __atomic_fetch_nand (type *ptr, type val, int memorder)}, i.e. {@code tmp = *ptr;
     * *ptr = ~(*ptr & val); return tmp;}.
     */
    ATOMIC_FETCH_NAND(
        "__atomic_fetch_nand", CAtomicOperationType.FETCH_OP, BinaryOperator.BITWISE_AND, true),

    /**
     * {@code type __atomic_add_fetch (type *ptr, type val, int memorder)}, i.e. {@code *ptr += val;
     * return *ptr;}.
     */
    ATOMIC_ADD_FETCH("__atomic_add_fetch", CAtomicOperationType.OP_FETCH, BinaryOperator.PLUS),
    /**
     * {@code type __atomic_sub_fetch (type *ptr, type val, int memorder)}, cf. {@link
     * #ATOMIC_ADD_FETCH}.
     */
    ATOMIC_SUB_FETCH("__atomic_sub_fetch", CAtomicOperationType.OP_FETCH, BinaryOperator.MINUS),
    /**
     * {@code type __atomic_and_fetch (type *ptr, type val, int memorder)}, cf. {@link
     * #ATOMIC_ADD_FETCH}.
     */
    ATOMIC_AND_FETCH(
        "__atomic_and_fetch", CAtomicOperationType.OP_FETCH, BinaryOperator.BITWISE_AND),
    /**
     * {@code type __atomic_or_fetch (type *ptr, type val, int memorder)}, cf. {@link
     * #ATOMIC_ADD_FETCH}.
     */
    ATOMIC_OR_FETCH("__atomic_or_fetch", CAtomicOperationType.OP_FETCH, BinaryOperator.BITWISE_OR),
    /**
     * {@code type __atomic_xor_fetch (type *ptr, type val, int memorder)}, cf. {@link
     * #ATOMIC_ADD_FETCH}.
     */
    ATOMIC_XOR_FETCH(
        "__atomic_xor_fetch", CAtomicOperationType.OP_FETCH, BinaryOperator.BITWISE_XOR),
    /**
     * {@code type __atomic_nand_fetch (type *ptr, type val, int memorder)}, i.e. {@code *ptr =
     * ~(*ptr & val); return *ptr;}.
     */
    ATOMIC_NAND_FETCH(
        "__atomic_nand_fetch", CAtomicOperationType.OP_FETCH, BinaryOperator.BITWISE_AND, true),

    /**
     * {@code bool __atomic_test_and_set (void *ptr, int memorder)} performs a test-and-set
     * operation on the byte at {@code *ptr}. The byte is set to some implementation defined nonzero
     * "set" value, for which CPAchecker uses 1, and the result is true iff the previous contents
     * were "set". GCC states that it should only be used for operands of type {@code bool} or
     * {@code char}, because for other types only part of the value may be set.
     */
    ATOMIC_TEST_AND_SET(
        "__atomic_test_and_set", CAtomicOperationType.TEST_AND_SET, false, 1, ImmutableList.of(1)),
    /**
     * {@code void __atomic_clear (bool *ptr, int memorder)} performs a clear operation on {@code
     * *ptr}, which afterwards contains 0. As for {@link #ATOMIC_TEST_AND_SET}, GCC states that it
     * should only be used for operands of type {@code bool} or {@code char}.
     */
    ATOMIC_CLEAR("__atomic_clear", CAtomicOperationType.CLEAR, false, 1, ImmutableList.of(1)),

    /**
     * {@code void __atomic_thread_fence (int memorder)} acts as a synchronization fence between
     * threads. It has no effect under the assumed sequentially consistent memory model.
     */
    ATOMIC_THREAD_FENCE(
        "__atomic_thread_fence", CAtomicOperationType.FENCE, false, 0, ImmutableList.of(0)),
    /**
     * {@code void __atomic_signal_fence (int memorder)} acts as a synchronization fence between a
     * thread and signal handlers based in the same thread. It has no effect under the assumed
     * sequentially consistent memory model.
     */
    ATOMIC_SIGNAL_FENCE(
        "__atomic_signal_fence", CAtomicOperationType.FENCE, false, 0, ImmutableList.of(0));

    private final String representation;
    private final CAtomicOperationType operationType;
    private final boolean generic;
    private final int minimumArgumentCount;
    private final ImmutableList<Integer> memoryOrderIndices;
    private final OptionalInt weakArgumentIndex;
    private final @Nullable BinaryOperator operator;
    private final boolean negated;

    /** A builtin without a {@code weak} argument and without an arithmetic operation. */
    CAtomicOperations(
        String pRepresentation,
        CAtomicOperationType pOperationType,
        boolean pGeneric,
        int pMinimumArgumentCount,
        ImmutableList<Integer> pMemoryOrderIndices) {
      this(
          pRepresentation,
          pOperationType,
          pGeneric,
          pMinimumArgumentCount,
          pMemoryOrderIndices,
          OptionalInt.empty(),
          null,
          false);
    }

    /**
     * A compare-exchange builtin, whose {@code weak} argument sits at {@code pWeakArgumentIndex}.
     */
    CAtomicOperations(
        String pRepresentation,
        CAtomicOperationType pOperationType,
        boolean pGeneric,
        int pMinimumArgumentCount,
        ImmutableList<Integer> pMemoryOrderIndices,
        int pWeakArgumentIndex) {
      this(
          pRepresentation,
          pOperationType,
          pGeneric,
          pMinimumArgumentCount,
          pMemoryOrderIndices,
          OptionalInt.of(pWeakArgumentIndex),
          null,
          false);
    }

    /** A fetch builtin, which applies an arithmetic operation to its target. */
    CAtomicOperations(
        String pRepresentation, CAtomicOperationType pOperationType, BinaryOperator pOperator) {
      this(pRepresentation, pOperationType, pOperator, false);
    }

    /** A fetch builtin whose arithmetic operation is bit-wise negated, i.e. a NAND builtin. */
    CAtomicOperations(
        String pRepresentation,
        CAtomicOperationType pOperationType,
        BinaryOperator pOperator,
        boolean pNegated) {
      this(
          pRepresentation,
          pOperationType,
          false,
          2,
          ImmutableList.of(2),
          OptionalInt.empty(),
          pOperator,
          pNegated);
    }

    CAtomicOperations(
        String pRepresentation,
        CAtomicOperationType pOperationType,
        boolean pGeneric,
        int pMinimumArgumentCount,
        ImmutableList<Integer> pMemoryOrderIndices,
        OptionalInt pWeakArgumentIndex,
        @Nullable BinaryOperator pOperator,
        boolean pNegated) {
      representation = pRepresentation;
      operationType = pOperationType;
      generic = pGeneric;
      minimumArgumentCount = pMinimumArgumentCount;
      memoryOrderIndices = pMemoryOrderIndices;
      weakArgumentIndex = pWeakArgumentIndex;
      operator = pOperator;
      negated = pNegated;
    }

    public String getRepresentation() {
      return representation;
    }

    public CAtomicOperationType getOperationType() {
      return operationType;
    }

    /** Whether this builtin passes its value operands by pointer rather than by value. */
    public boolean isGeneric() {
      return generic;
    }

    /** The number of arguments below which a call to this builtin is malformed. */
    public int getMinimumArgumentCount() {
      return minimumArgumentCount;
    }

    /**
     * The number of arguments above which a call to this builtin is malformed, i.e. the highest
     * argument position used by this builtin (the {@code weak} flag or a memory order), plus one.
     * None of the atomic builtins are variadic.
     */
    public int getMaximumArgumentCount() {
      int max = Math.max(minimumArgumentCount, weakArgumentIndex.orElse(-1) + 1);
      for (int memoryOrderIndex : memoryOrderIndices) {
        max = Math.max(max, memoryOrderIndex + 1);
      }
      return max;
    }

    /** Argument positions that hold a memory order and must therefore be {@code SEQ_CST}. */
    public ImmutableList<Integer> getMemoryOrderIndices() {
      return memoryOrderIndices;
    }

    /**
     * The argument position that holds the {@code weak} flag, which only the compare-exchange
     * builtins have.
     */
    public OptionalInt getWeakArgumentIndex() {
      return weakArgumentIndex;
    }

    /** Whether the result of {@link #getOperator} is bit-wise negated, as for the NAND builtins. */
    public boolean isNegated() {
      return negated;
    }

    /** The arithmetic operation of a fetch builtin, or {@code null} for the other builtins. */
    public @Nullable BinaryOperator getOperator() {
      return operator;
    }

    /**
     * Whether this builtin writes to the object that one of its pointer arguments designates, and
     * thus has an effect that must not be dropped even if the result of the call is unused.
     */
    public boolean hasSideEffect() {
      return switch (operationType) {
        // __atomic_load_n returns the loaded value, while the generic __atomic_load writes it to
        // the object designated by its second argument
        case LOAD -> generic;
        // under sequential consistency a fence constrains nothing
        case FENCE -> false;
        case STORE, EXCHANGE, CMP_XCHG, FETCH_OP, OP_FETCH, TEST_AND_SET, CLEAR -> true;
      };
    }

    private static final ImmutableMap<String, CAtomicOperations> BY_REPRESENTATION =
        Maps.uniqueIndex(Arrays.asList(values()), CAtomicOperations::getRepresentation);

    public static Optional<CAtomicOperations> fromString(String s) {
      return Optional.ofNullable(BY_REPRESENTATION.get(s));
    }
  }

  private static final ImmutableSet<String> SIDE_EFFECT_FUNCTIONS =
      Arrays.stream(CAtomicOperations.values())
          .filter(CAtomicOperations::hasSideEffect)
          .map(CAtomicOperations::getRepresentation)
          .collect(ImmutableSet.toImmutableSet());

  /**
   * The names of the atomic builtins that have a side effect, cf. {@link
   * CAtomicOperations#hasSideEffect}.
   */
  public static ImmutableSet<String> getSideEffectFunctionNames() {
    return SIDE_EFFECT_FUNCTIONS;
  }

  /** Check whether a given function name identifies an atomic builtin that CPAchecker encodes. */
  public static boolean isBuiltinAtomicFunction(String pFunctionName) {
    return CAtomicOperations.fromString(checkNotNull(pFunctionName)).isPresent();
  }

  private static boolean matches(String pFunctionName, CAtomicOperationType pType) {
    return CAtomicOperations.fromString(pFunctionName)
        .map(operation -> operation.operationType == pType)
        .orElse(false);
  }

  public static boolean matchesStore(String pFunctionName) {
    return matches(pFunctionName, CAtomicOperationType.STORE);
  }

  public static boolean matchesLoad(String pFunctionName) {
    return matches(pFunctionName, CAtomicOperationType.LOAD);
  }

  public static boolean matchesExchange(String pFunctionName) {
    return matches(pFunctionName, CAtomicOperationType.EXCHANGE);
  }

  public static boolean matchesCompareExchange(String pFunctionName) {
    return matches(pFunctionName, CAtomicOperationType.CMP_XCHG);
  }

  public static boolean matchesFetchOp(String pFunctionName) {
    return matches(pFunctionName, CAtomicOperationType.FETCH_OP)
        || matches(pFunctionName, CAtomicOperationType.OP_FETCH);
  }

  public static boolean matchesFence(String pFunctionName) {
    return matches(pFunctionName, CAtomicOperationType.FENCE);
  }

  /**
   * Return the object that an atomic builtin's pointer argument designates, i.e. {@code *pPointer}.
   *
   * @throws UnrecognizedCodeException if the argument is not a pointer.
   */
  public static CLeftHandSide getPointerTarget(CExpression pPointer)
      throws UnrecognizedCodeException {
    if (pPointer instanceof CUnaryExpression unary
        && unary.getOperator() == UnaryOperator.AMPER
        && unary.getOperand() instanceof CLeftHandSide operand) {
      return operand;
    }
    CType pointerType = pPointer.getExpressionType().getCanonicalType();
    if (!(pointerType instanceof CPointerType pointer)) {
      throw new UnrecognizedCodeException(
          "Atomic builtin expects a pointer argument, but got type " + pointerType, pPointer);
    }
    return new CPointerExpression(
        pPointer.getFileLocation(), pointer.getType().getCanonicalType(), pPointer);
  }

  /**
   * Build the value that a fetch builtin writes to its target, i.e. {@code *ptr op value} (or
   * {@code ~(*ptr & value)} for the NAND builtins).
   *
   * <p>The result is cast back to the type of {@code pTarget}, because C evaluates the operation on
   * the promoted operand types, while the builtin stores a value of the pointee type.
   *
   * @param pBuilder used to derive the C conversion and promotion types of the operation.
   */
  public static CExpression getUpdatedValue(
      CBinaryExpressionBuilder pBuilder,
      CAtomicOperations pOperation,
      CLeftHandSide pTarget,
      CExpression pValue)
      throws UnrecognizedCodeException {
    BinaryOperator binaryOperator = pOperation.getOperator();
    if (binaryOperator == null) {
      throw new UnrecognizedCodeException(
          "Atomic builtin " + pOperation.getRepresentation() + " has no arithmetic operation",
          pTarget);
    }
    CExpression result = pBuilder.buildBinaryExpression(pTarget, pValue, binaryOperator);
    if (pOperation.isNegated()) {
      result =
          new CUnaryExpression(
              FileLocation.DUMMY, result.getExpressionType(), result, UnaryOperator.TILDE);
    }
    return castTo(result, pTarget.getExpressionType());
  }

  /** Cast {@code pExpression} to {@code pType} unless it already has that type. */
  private static CExpression castTo(CExpression pExpression, CType pType) {
    if (pExpression.getExpressionType().getCanonicalType().equals(pType.getCanonicalType())) {
      return pExpression;
    }
    return new CCastExpression(FileLocation.DUMMY, pType, pExpression);
  }

  /**
   * Return the statically-known return type for some atomic builtins when possible.
   *
   * <p>The builtins that yield the value that was stored, loaded or exchanged have a return type
   * that depends on the pointee type of their pointer argument, so it cannot be resolved here and
   * has to be derived from the call site.
   */
  public static Optional<CType> getType(String pFunctionName) {
    return CAtomicOperations.fromString(pFunctionName)
        .flatMap(
            operation ->
                switch (operation.getOperationType()) {
                  // the generic load and exchange write their result through a pointer instead of
                  // returning it
                  case STORE, CLEAR, FENCE -> Optional.of(CVoidType.VOID);
                  case LOAD, EXCHANGE ->
                      operation.isGeneric() ? Optional.of(CVoidType.VOID) : Optional.empty();
                  case CMP_XCHG, TEST_AND_SET -> Optional.of(CNumericTypes.BOOL);
                  case FETCH_OP, OP_FETCH -> Optional.empty();
                });
  }
}
