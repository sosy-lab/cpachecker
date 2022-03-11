// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CFunctionPointerResolver.FunctionSet;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

public class TargetFunctionsProvider {
  private final MachineModel machine;
  private final LogManager logger;
  private final Collection<FunctionEntryNode> candidateFunctions;
  private final ImmutableSetMultimap<String, String> candidateFunctionsForField;
  private final ImmutableSetMultimap<String, String> globalsMatching;
  private final BiPredicate<CFunctionType, CFunctionType> matchingFunctionCall;

  public TargetFunctionsProvider(
      MachineModel pMachine,
      LogManager pLogger,
      Collection<CFunctionPointerResolver.FunctionSet> functionSets,
      Collection<FunctionEntryNode> candidateFunctions) {
    this(
        pMachine,
        pLogger,
        functionSets,
        candidateFunctions,
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of());
  }

  public TargetFunctionsProvider(
      MachineModel pMachine,
      LogManager pLogger,
      Collection<FunctionSet> functionSets,
      Collection<FunctionEntryNode> candidateFunctions,
      Multimap<String, String> candidateFunctionsForField,
      Multimap<String, String> globalsMatching) {
    machine = pMachine;
    logger = pLogger;
    matchingFunctionCall = getFunctionSetPredicate(functionSets);
    this.candidateFunctions = candidateFunctions;
    this.candidateFunctionsForField = ImmutableSetMultimap.copyOf(candidateFunctionsForField);
    this.globalsMatching = ImmutableSetMultimap.copyOf(globalsMatching);
  }

  public Set<String> getMatchedFunc(CExpression expression) {
    if (expression instanceof CFieldReference) {
      String fieldName = ((CFieldReference) expression).getFieldName();
      return candidateFunctionsForField.get(fieldName);
    } else if (expression instanceof CIdExpression) {
      String variableName = ((CIdExpression) expression).getName();
      return globalsMatching.get(variableName);
    } else {
      return ImmutableSet.of();
    }
  }

  public List<CFunctionEntryNode> getFunctionSet(CFunctionType func) {
    return from(candidateFunctions)
        .filter(CFunctionEntryNode.class)
        .filter(f -> matchingFunctionCall.test(func, f.getFunctionDefinition().getType()))
        .toList();
  }

  private BiPredicate<CFunctionType, CFunctionType> getFunctionSetPredicate(
      Collection<FunctionSet> pFunctionSets) {
    List<BiPredicate<CFunctionType, CFunctionType>> predicates = new ArrayList<>();

    // note that this set is sorted according to the declaration order of the enum
    EnumSet<FunctionSet> functionSets = EnumSet.copyOf(pFunctionSets);

    if (functionSets.contains(FunctionSet.EQ_PARAM_TYPES)
        || functionSets.contains(FunctionSet.EQ_PARAM_SIZES)) {
      functionSets.add(FunctionSet.EQ_PARAM_COUNT); // TYPES and SIZES need COUNT checked first
    }

    for (FunctionSet functionSet : functionSets) {
      switch (functionSet) {
        case ALL:
          // do nothing
          break;
        case EQ_PARAM_COUNT:
          predicates.add(this::checkParamCount);
          break;
        case EQ_PARAM_SIZES:
          predicates.add(this::checkReturnAndParamSizes);
          break;
        case EQ_PARAM_TYPES:
          predicates.add(this::checkReturnAndParamTypes);
          break;
        case RETURN_VALUE:
          predicates.add(this::checkReturnValue);
          break;
        case USED_IN_CODE:
          // Not necessary, only matching functions are in the
          // candidateFunctions set
          break;
        default:
          throw new AssertionError();
      }
    }
    return predicates.stream().reduce((a, b) -> true, BiPredicate::and);
  }

  private boolean checkParamCount(CFunctionType func, CFunctionType functionType) {
    int declaredParameters = functionType.getParameters().size();
    int actualParameters = func.getParameters().size();

    if (actualParameters < declaredParameters) {
      logger.log(
          Level.FINEST,
          "Function call",
          func.getName(),
          "does not match function",
          functionType,
          "because there are not enough actual parameters.");
      return false;
    }

    if (!functionType.takesVarArgs() && actualParameters > declaredParameters) {
      logger.log(
          Level.FINEST,
          "Function call",
          func.getName(),
          "does not match function",
          functionType,
          "because there are too many actual parameters.");
      return false;
    }
    return true;
  }

  private boolean checkReturnAndParamSizes(CFunctionType func, CFunctionType functionType) {
    CType declRet = functionType.getReturnType();
    if (!machine.getSizeof(declRet).equals(machine.getSizeof(func.getReturnType()))) {
      logger.log(
          Level.FINEST,
          "Function call",
          func.getName(),
          "with type",
          func.getReturnType(),
          "does not match function",
          functionType,
          "with return type",
          declRet,
          "because of return types with different sizes.");
      return false;
    }

    List<CType> declParams = functionType.getParameters();
    for (int i = 0; i < declParams.size(); i++) {
      CType dt = declParams.get(i);
      CType et = func.getParameters().get(i);
      if (!machine.getSizeof(dt).equals(machine.getSizeof(et))) {
        logger.log(
            Level.FINEST,
            "Function call",
            func.getName(),
            "does not match function",
            functionType,
            "because actual parameter",
            i,
            "has type",
            et,
            "instead of",
            dt,
            "(differing sizes).");
        return false;
      }
    }

    return true;
  }

  private boolean checkReturnAndParamTypes(CFunctionType func, CFunctionType functionType) {
    CType declRet = functionType.getReturnType();
    if (!isCompatibleType(declRet, func.getReturnType())) {
      logger.log(
          Level.FINEST,
          "Function call",
          func.getName(),
          "with type",
          func.getReturnType(),
          "does not match function",
          functionType,
          "with return type",
          declRet);
      return false;
    }

    List<CType> declParams = functionType.getParameters();
    for (int i = 0; i < declParams.size(); i++) {
      CType dt = declParams.get(i);
      CType et = func.getParameters().get(i);
      if (!isCompatibleType(dt, et)) {
        logger.log(
            Level.FINEST,
            "Function call",
            func.getName(),
            "does not match function",
            functionType,
            "because actual parameter",
            i,
            "has type",
            et,
            "instead of",
            dt);
        return false;
      }
    }
    return true;
  }

  /** Exclude void functions if the return value of the function is used in an assignment. */
  private boolean checkReturnValue(CFunctionType func, CFunctionType functionType) {
    CType declRet = functionType.getReturnType();
    if (!isCompatibleType(declRet, func.getReturnType())) {
      logger.log(
          Level.FINEST,
          "Function call",
          func.getName(),
          "with type",
          func.getReturnType(),
          "does not match function",
          functionType,
          "with return type",
          declRet);
      return false;
    }
    return true;
  }

  /**
   * Check whether two types are assignment compatible.
   *
   * @param pDeclaredType The type that is declared (e.g., as variable type).
   * @param pActualType The type that is actually used (e.g., as type of an expression).
   * @return {@code true} if a value of actualType may be assigned to a variable of declaredType.
   */
  private boolean isCompatibleType(CType pDeclaredType, CType pActualType) {
    // Check canonical types
    CType declaredType = pDeclaredType.getCanonicalType();
    CType actualType = pActualType.getCanonicalType();

    // If types are equal, they are trivially compatible
    if (declaredType.equals(actualType)) {
      return true;
    }

    // Implicit conversions among basic types
    if (declaredType instanceof CSimpleType && actualType instanceof CSimpleType) {
      return true;
    }

    // Void pointer can be converted to any other pointer or integer
    if (declaredType instanceof CPointerType) {
      CPointerType declaredPointerType = (CPointerType) declaredType;
      if (declaredPointerType.getType() == CVoidType.VOID) {
        if (actualType instanceof CSimpleType) {
          CSimpleType actualSimpleType = (CSimpleType) actualType;
          CBasicType actualBasicType = actualSimpleType.getType();
          if (actualBasicType.isIntegerType()) {
            return true;
          }
        } else if (actualType instanceof CPointerType) {
          return true;
        }
      }
    }

    // Any pointer or integer can be converted to a void pointer
    if (actualType instanceof CPointerType) {
      CPointerType actualPointerType = (CPointerType) actualType;
      if (actualPointerType.getType() == CVoidType.VOID) {
        if (declaredType instanceof CSimpleType) {
          CSimpleType declaredSimpleType = (CSimpleType) declaredType;
          CBasicType declaredBasicType = declaredSimpleType.getType();
          if (declaredBasicType.isIntegerType()) {
            return true;
          }
        } else if (declaredType instanceof CPointerType) {
          return true;
        }
      }
    }

    // If both types are pointers, check if the inner types are compatible
    if (declaredType instanceof CPointerType && actualType instanceof CPointerType) {
      CPointerType declaredPointerType = (CPointerType) declaredType;
      CPointerType actualPointerType = (CPointerType) actualType;
      if (isCompatibleType(declaredPointerType.getType(), actualPointerType.getType())) {
        return true;
      }
    }

    return false;
  }
}
