// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypes.withoutConst;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypes.withoutVolatile;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTAttribute;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFieldDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTInitializerList;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTProblemStatement;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IProblemType;
import org.eclipse.cdt.core.dom.ast.c.ICASTArrayDesignator;
import org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignatedInitializer;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignator;
import org.eclipse.cdt.core.dom.ast.c.ICASTFieldDesignator;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;
import org.eclipse.cdt.core.dom.ast.gnu.c.IGCCASTArrayRangeDesignator;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTArrayDesignator;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTArrayRangeDesignator;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCompositeTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTLiteralExpression;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Parsers.EclipseCParserOptions;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.simplification.NonRecursiveExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.BuiltinOverflowFunctions;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;

class ASTConverter {

  /**
   * All GNU C function attributes that are known by CPAchecker. The keys of this map are the names
   * of the C attributes. The value of each name is one of the following two:
   *
   * <ul>
   *   <li>a {@link FunctionAttribute} that is used within CPAchecker to represent the attribute, or
   *   <li>empty if the attribute is known by CPAchecker, but ignored.
   * </ul>
   */
  private static final ImmutableMap<String, Optional<FunctionAttribute>> KNOWN_FUNCTION_ATTRIBUTES;

  static {
    ImmutableMap.Builder<String, Optional<FunctionAttribute>> builder = ImmutableMap.builder();
    KNOWN_FUNCTION_ATTRIBUTES =
        builder
            // occurs in sv-benchmarks
            .put("ldv_model", Optional.empty())
            .put("ldv_model_inline", Optional.empty())
            // https://gcc.gnu.org/onlinedocs/gcc-12.1.0/gcc/Common-Function-Attributes.html
            .put("access", Optional.empty())
            .put("alias", Optional.empty())
            .put("aligned", Optional.empty())
            .put("alloc_align", Optional.empty())
            .put("alloc_size", Optional.empty())
            .put("always_inline", Optional.empty())
            .put("assume_aligned", Optional.empty())
            .put("cold", Optional.empty())
            .put("const", Optional.empty())
            .put("deprecated", Optional.empty())
            .put("unavailable", Optional.empty())
            .put("error", Optional.empty())
            .put("warning", Optional.empty())
            .put("externally_visible", Optional.empty())
            .put("flatten", Optional.empty())
            .put("format", Optional.empty())
            .put("format_arg", Optional.empty())
            .put("gnu_inline", Optional.empty())
            .put("hot", Optional.empty())
            .put("ifunc", Optional.empty())
            .put("interrupt", Optional.empty())
            .put("interrupt_handler", Optional.empty())
            .put("leaf", Optional.empty())
            .put("malloc", Optional.empty())
            .put("no_icf", Optional.empty())
            .put("no_instrument_function", Optional.empty())
            .put("no_profile_instrument_function", Optional.empty())
            .put("no_reorder", Optional.empty())
            .put("no_sanitize", Optional.empty())
            .put("no_sanitize_address", Optional.empty())
            .put("no_address_safety_analysis", Optional.empty())
            .put("no_sanitize_thread", Optional.empty())
            .put("no_sanitize_undefined", Optional.empty())
            .put("no_sanitize_coverage", Optional.empty())
            .put("no_split_stack", Optional.empty())
            .put("no_stack_limit", Optional.empty())
            .put("noclone", Optional.empty())
            .put("noinline", Optional.empty())
            .put("noipa", Optional.empty())
            .put("nonnull", Optional.empty())
            .put("noplt", Optional.empty())
            .put("noreturn", Optional.of(FunctionAttribute.NO_RETURN))
            .put("nothrow", Optional.empty())
            .put("optimize", Optional.empty())
            .put("patchable_function_entry", Optional.empty())
            .put("pure", Optional.empty())
            .put("returns_nonnull", Optional.empty())
            .put("returns_twice", Optional.empty())
            .put("section", Optional.empty())
            .put("sentinel", Optional.empty())
            .put("simd", Optional.empty())
            .put("stack_protect", Optional.empty())
            .put("no_stack_protector", Optional.empty())
            .put("target", Optional.empty())
            .put("symver", Optional.empty())
            .put("tainted_args", Optional.empty())
            .put("target_clones", Optional.empty())
            .put("unused", Optional.empty())
            .put("used", Optional.empty())
            .put("retain", Optional.empty())
            .put("visibility", Optional.empty())
            .put("warn_unused_result", Optional.empty())
            .put("weak", Optional.empty())
            .put("weakref", Optional.empty())
            .put("zero_call_used_regs", Optional.empty())
            // https://gcc.gnu.org/onlinedocs/gcc-12.1.0/gcc/x86-Function-Attributes.html
            .put("cdecl", Optional.empty())
            .put("fastcall", Optional.empty())
            .put("thiscall", Optional.empty())
            .put("ms_abi", Optional.empty())
            .put("sysv_abi", Optional.empty())
            .put("callee_pop_aggregate_return", Optional.empty())
            .put("ms_hook_prologue", Optional.empty())
            .put("naked", Optional.empty())
            .put("regparm", Optional.empty())
            .put("sseregparm", Optional.empty())
            .put("force_align_arg_pointer", Optional.empty())
            .put("stdcall", Optional.empty())
            .put("no_caller_saved_registers", Optional.empty())
            .put("indirect_branch", Optional.empty())
            .put("function_return", Optional.empty())
            .put("nocf_check", Optional.empty())
            .put("cf_check", Optional.empty())
            .put("indirect_return", Optional.empty())
            .put("fentry_name", Optional.empty())
            .put("fentry_section", Optional.empty())
            .put("nodirect_extern_access", Optional.empty())
            // https://gcc.gnu.org/onlinedocs/gcc/Common-Variable-Attributes.html
            // might end up as a funciton attribute when used for the return type?
            .put("mode", Optional.empty()) // handled by this class
            // https://gcc.gnu.org/onlinedocs/gcc-12.1.0/gcc/Microsoft-Windows-Function-Attributes.html
            .put("dllexport", Optional.empty())
            .put("dllimport", Optional.empty())
            .buildOrThrow();
  }

  // Calls to this functions are handled by this class and replaced with regular C code.
  private static final String FUNC_CONSTANT = "__builtin_constant_p";
  private static final String FUNC_OFFSETOF = "__builtin_offsetof";
  private static final String FUNC_EXPECT = "__builtin_expect";
  private static final String FUNC_TYPES_COMPATIBLE = "__builtin_types_compatible_p";

  private final ExpressionSimplificationVisitor expressionSimplificator;
  private final NonRecursiveExpressionSimplificationVisitor nonRecursiveExpressionSimplificator;
  private final CBinaryExpressionBuilder binExprBuilder;

  private final EclipseCParserOptions options;
  private final LogManager logger;
  private final ASTLiteralConverter literalConverter;
  private final ASTOperatorConverter operatorConverter;
  private final ASTTypeConverter typeConverter;
  private final MachineModel machinemodel;

  private final ParseContext parseContext;

  private final Scope scope;

  // this counter is static to make the replacing names for anonymous types, in
  // more than one file (which get parsed with different AstConverters, although
  // they are in the same run) unique
  private static int anonTypeCounter = 0;

  private final Sideassignments sideAssignmentStack;
  private final String staticVariablePrefix;

  private static final ContainsProblemTypeVisitor containsProblemTypeVisitor =
      new ContainsProblemTypeVisitor();

  public ASTConverter(
      EclipseCParserOptions pOptions,
      Scope pScope,
      LogManagerWithoutDuplicates pLogger,
      ParseContext pParseContext,
      MachineModel pMachineModel,
      String pStaticVariablePrefix,
      Sideassignments pSideAssignmentStack) {
    options = pOptions;
    scope = pScope;
    logger = pLogger;
    typeConverter = new ASTTypeConverter(scope, this, pStaticVariablePrefix, pParseContext);
    literalConverter = new ASTLiteralConverter(pMachineModel, pParseContext);
    operatorConverter = new ASTOperatorConverter(pParseContext);
    parseContext = pParseContext;
    machinemodel = pMachineModel;
    staticVariablePrefix = pStaticVariablePrefix;
    sideAssignmentStack = pSideAssignmentStack;

    expressionSimplificator = new ExpressionSimplificationVisitor(pMachineModel, pLogger);
    nonRecursiveExpressionSimplificator =
        new NonRecursiveExpressionSimplificationVisitor(pMachineModel, pLogger);
    binExprBuilder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
  }

  public CExpression convertExpressionWithoutSideEffects(IASTExpression e) {

    CAstNode node = convertExpressionWithSideEffects(e);
    if (node == null || node instanceof CExpression) {
      return (CExpression) node;

    } else if (node instanceof CFunctionCallExpression) {
      return addSideassignmentsForExpressionsWithoutSideEffects(node, e);

    } else if (e instanceof IASTUnaryExpression
        && (((IASTUnaryExpression) e).getOperator() == IASTUnaryExpression.op_postFixDecr
            || ((IASTUnaryExpression) e).getOperator() == IASTUnaryExpression.op_postFixIncr)) {
      return addSideAssignmentsForUnaryExpressions(
          ((CAssignment) node).getLeftHandSide(),
          node.getFileLocation(),
          ((CBinaryExpression) ((CAssignment) node).getRightHandSide()).getOperator());

    } else if (node instanceof CAssignment) {
      sideAssignmentStack.addPreSideAssignment(node);
      return ((CAssignment) node).getLeftHandSide();

    } else {
      throw new AssertionError("unknown expression " + node);
    }
  }

  /**
   * Simplify an expression as much as possible. Use this when you always want to evaluate a
   * specific expression if possible, e.g. array lengths (which should be constant if possible).
   */
  CExpression simplifyExpressionRecursively(CExpression exp) {
    return exp.accept(expressionSimplificator);
  }

  /**
   * Do a single step of expression simplification (not recursively). Use this when you do not care
   * about full evaluation, or you know the operands are already evaluated if possible.
   */
  CExpression simplifyExpressionOneStep(CExpression exp) {
    return exp.accept(nonRecursiveExpressionSimplificator);
  }

  private CExpression addSideassignmentsForExpressionsWithoutSideEffects(
      CAstNode node, IASTExpression e) {
    CIdExpression tmp;
    if (e.getExpressionType() instanceof IProblemType) {
      tmp =
          createInitializedTemporaryVariable(
              getLocation(e), ((CRightHandSide) node).getExpressionType(), (CInitializer) null);
    } else {
      tmp = createTemporaryVariable(e);
    }

    sideAssignmentStack.addPreSideAssignment(
        new CFunctionCallAssignmentStatement(getLocation(e), tmp, (CFunctionCallExpression) node));
    return tmp;
  }

  /**
   * This method builds a preSideAssignment for x=x+1 or x=x-1 and returns a tmp-variable, that has
   * the value of x before the operation.
   *
   * @param exp the "x" of x=x+1
   * @param fileLoc location of the expression
   * @param op binary operator, should be PLUS or MINUS
   */
  private CIdExpression addSideAssignmentsForUnaryExpressions(
      final CLeftHandSide exp, final FileLocation fileLoc, final BinaryOperator op) {
    final CIdExpression tmp =
        createInitializedTemporaryVariable(fileLoc, exp.getExpressionType(), exp);
    final CBinaryExpression postExp = buildBinaryExpression(exp, CIntegerLiteralExpression.ONE, op);
    sideAssignmentStack.addPreSideAssignment(
        new CExpressionAssignmentStatement(fileLoc, exp, postExp));
    return tmp;
  }

  private void addSideEffectDeclarationForType(CCompositeType type, FileLocation loc) {
    CComplexTypeDeclaration decl = new CComplexTypeDeclaration(loc, scope.isGlobalScope(), type);

    if (scope.registerTypeDeclaration(decl)) {
      sideAssignmentStack.addPreSideAssignment(decl);
    }
  }

  protected CAstNode convertExpressionWithSideEffects(IASTExpression e) {
    CAstNode converted = convertExpressionWithSideEffectsNotSimplified(e);
    if (converted == null
        || !options.simplifyConstExpressions()
        || !(converted instanceof CExpression)) {
      return converted;
    }

    return simplifyExpressionOneStep((CExpression) converted);
  }

  private CAstNode convertExpressionWithSideEffectsNotSimplified(IASTExpression e) {
    assert !(e instanceof CExpression);

    if (e == null) {
      return null;

    } else if (e instanceof IASTArraySubscriptExpression) {
      return convert((IASTArraySubscriptExpression) e);

    } else if (e instanceof IASTBinaryExpression) {
      return convert((IASTBinaryExpression) e);

    } else if (e instanceof IASTCastExpression) {
      return convert((IASTCastExpression) e);

    } else if (e instanceof IASTFieldReference) {
      return convert((IASTFieldReference) e);

    } else if (e instanceof IASTFunctionCallExpression) {
      return convert((IASTFunctionCallExpression) e);

    } else if (e instanceof IASTIdExpression) {
      CExpression exp = convert((IASTIdExpression) e);
      CType type = exp.getExpressionType();

      // this id expression is the name of a function. When there is no
      // functionCallExpressionn or unaryexpression with pointertype and operator.Amper
      // around it, we create it.
      if (type instanceof CFunctionType
          && !(isFunctionCallNameExpression(e) || isAddressOfArgument(e))) {
        exp =
            new CUnaryExpression(
                exp.getFileLocation(),
                new CPointerType(type.isConst(), type.isVolatile(), type),
                exp,
                UnaryOperator.AMPER);
      }
      return exp;

    } else if (e instanceof IASTLiteralExpression) {
      final CType type = typeConverter.convert(e.getExpressionType());
      return literalConverter.convert((IASTLiteralExpression) e, type, getLocation(e));

    } else if (e instanceof IASTUnaryExpression) {
      return convert((IASTUnaryExpression) e);

    } else if (e instanceof IASTTypeIdExpression) {
      return convert((IASTTypeIdExpression) e);

    } else if (e instanceof IASTTypeIdInitializerExpression) {
      return convert((IASTTypeIdInitializerExpression) e);

    } else if (e instanceof IASTConditionalExpression) {
      return convert((IASTConditionalExpression) e);

    } else if (e instanceof IGNUASTCompoundStatementExpression) {
      return convert((IGNUASTCompoundStatementExpression) e);

    } else if (e instanceof IASTExpressionList) {
      return convertExpressionListAsExpression((IASTExpressionList) e);

    } else {
      throw parseContext.parseError("Unknown expression type " + e.getClass().getSimpleName(), e);
    }
  }

  private boolean isFunctionCallNameExpression(IASTExpression e) {
    return e.getParent() instanceof IASTFunctionCallExpression
        && e.getPropertyInParent() == IASTFunctionCallExpression.FUNCTION_NAME;
  }

  private boolean isAddressOfArgument(IASTExpression e) {
    return e.getParent() instanceof IASTUnaryExpression
        && ((IASTUnaryExpression) e.getParent()).getOperator() == IASTUnaryExpression.op_amper;
  }

  enum CONDITION {
    NORMAL,
    ALWAYS_FALSE,
    ALWAYS_TRUE
  }

  CONDITION getConditionKind(final CExpression condition) {

    if (condition instanceof CIntegerLiteralExpression
        || condition instanceof CCharLiteralExpression) {
      // constant int value
      if (isZero(condition)) {
        return CONDITION.ALWAYS_FALSE;
      } else {
        return CONDITION.ALWAYS_TRUE;
      }
    }
    return CONDITION.NORMAL;
  }

  private CAstNode convert(IASTConditionalExpression e) {
    // check condition kind so we can eventually skip creating an unnecessary branch
    CONDITION conditionKind = getConditionKind(e.getLogicalConditionExpression());

    switch (conditionKind) {
      case ALWAYS_TRUE:
        return convertExpressionWithSideEffects(e.getPositiveResultExpression());
      case ALWAYS_FALSE:
        return convertExpressionWithSideEffects(e.getNegativeResultExpression());
      case NORMAL:

        // this means the return value (if there could be one) of the conditional
        // expression is not used
        if (convertType(e) instanceof CVoidType) {
          sideAssignmentStack.addConditionalExpression(e, null);

          // TODO we should not return a variable here, however null cannot be returned
          // perhaps we need a DummyExpression here
          return CIntegerLiteralExpression.ZERO;
        }

        CIdExpression tmp = createTemporaryVariable(e);
        assert !(tmp.getExpressionType() instanceof CVoidType);
        sideAssignmentStack.addConditionalExpression(e, tmp);
        return tmp;
      default:
        throw new AssertionError("Unhandled case statement: " + conditionKind);
    }
  }

  /**
   * Computes the condition kind of an IASTExpression, logical ors and logical ands are resolved the
   * rest works as with the condition kind method for CExpressions
   */
  private CONDITION getConditionKind(IASTExpression exp) {
    if (exp instanceof IASTBinaryExpression
        && (((IASTBinaryExpression) exp).getOperator() == IASTBinaryExpression.op_logicalAnd
            || ((IASTBinaryExpression) exp).getOperator() == IASTBinaryExpression.op_logicalOr)) {
      IASTBinaryExpression binExp = (IASTBinaryExpression) exp;

      switch (binExp.getOperator()) {
        case IASTBinaryExpression.op_logicalAnd:
          {
            CONDITION left = getConditionKind(binExp.getOperand1());
            switch (left) {
              case ALWAYS_TRUE:
                return getConditionKind(binExp.getOperand2());
              case ALWAYS_FALSE:
                return left;
              case NORMAL:
                if (getConditionKind(binExp.getOperand2()) == CONDITION.ALWAYS_FALSE) {
                  return CONDITION.ALWAYS_FALSE;
                } else {
                  return CONDITION.NORMAL;
                }
              default:
                throw new AssertionError("unhandled case statement");
            }
          }

        case IASTBinaryExpression.op_logicalOr:
          {
            CONDITION left = getConditionKind(binExp.getOperand1());
            switch (left) {
              case ALWAYS_TRUE:
                return CONDITION.ALWAYS_TRUE;
              case ALWAYS_FALSE:
                return getConditionKind(binExp.getOperand2());
              case NORMAL:
                CONDITION right = getConditionKind(binExp.getOperand2());
                if (right == CONDITION.ALWAYS_FALSE) {
                  return CONDITION.NORMAL;
                } else {
                  return right;
                }
              default:
                throw new AssertionError("unhandled case statement");
            }
          }

        default:
          throw new AssertionError("unhandled case statement");
      }

    } else {
      sideAssignmentStack.enterBlock();
      // Here we call simplify manually, because for conditional expressions
      // we always want a full evaluation because we might be able to prevent
      // a branch in the CFA.
      // In global scope, this is even required because there cannot be any branches.
      CExpression simplifiedExp =
          simplifyExpressionRecursively(convertExpressionWithoutSideEffects(exp));
      sideAssignmentStack.getAndResetConditionalExpressions();
      sideAssignmentStack.getAndResetPostSideAssignments();
      sideAssignmentStack.getAndResetPreSideAssignments();
      sideAssignmentStack.leaveBlock();
      return getConditionKind(simplifiedExp);
    }
  }

  private boolean isZero(CExpression exp) {
    if (exp instanceof CIntegerLiteralExpression) {
      BigInteger value = ((CIntegerLiteralExpression) exp).getValue();
      return value.equals(BigInteger.ZERO);
    }
    if (exp instanceof CCharLiteralExpression) {
      char value = ((CCharLiteralExpression) exp).getCharacter();
      return value == 0;
    }
    return false;
  }

  /**
   * Evaluate a constant expression into an integer literal. This method is for cases where the
   * frontend really needs to know the resulting int value, so we simplify the expression and force
   * it to be evaluated even if otherwise we would not. So call this only if necessary.
   */
  private BigInteger evaluateIntegerConstantExpression(IASTExpression exp) {
    CAstNode n = convertExpressionWithSideEffectsNotSimplified(exp);
    if (!(n instanceof CExpression)) {
      throw parseContext.parseError("Constant expression with side effect", exp);
    }

    CExpression e = simplifyExpressionRecursively((CExpression) n);
    if (e instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression) e).getValue();
    } else if (e instanceof CCharLiteralExpression) {
      return BigInteger.valueOf(((CCharLiteralExpression) e).getCharacter());
    } else {
      throw parseContext.parseError("Integer constant expression could not be evaluated", n);
    }
  }

  private CAstNode convert(IGNUASTCompoundStatementExpression e) {
    CIdExpression tmp = createTemporaryVariable(e);
    sideAssignmentStack.addConditionalExpression(e, tmp);

    return tmp;
  }

  private CAstNode convertExpressionListAsExpression(IASTExpressionList e) {
    CIdExpression tmp = createTemporaryVariable(e);
    sideAssignmentStack.addConditionalExpression(e, tmp);
    return tmp;
  }

  private CArraySubscriptExpression convert(IASTArraySubscriptExpression e) {
    CExpression arrayExpr = convertExpressionWithoutSideEffects(e.getArrayExpression());
    CExpression subscriptExpr = convertExpressionWithoutSideEffects(toExpression(e.getArgument()));

    // Eclipse CDT has a bug in determining the result type if the array type is a typedef.
    CType resultType = arrayExpr.getExpressionType();
    while (resultType instanceof CTypedefType) {
      resultType = ((CTypedefType) resultType).getRealType();
    }
    if (resultType instanceof CArrayType) {
      resultType = ((CArrayType) resultType).getType();
    } else if (resultType instanceof CPointerType) {
      resultType = ((CPointerType) resultType).getType();
    } else {
      // TODO probably we should throw exception,
      // but for now we delegate to Eclipse CDT and see whether it knows better than we do
      resultType = typeConverter.convert(e.getExpressionType());
    }

    return new CArraySubscriptExpression(getLocation(e), resultType, arrayExpr, subscriptExpr);
  }

  /**
   * creates temporary variables with increasing numbers.
   *
   * @return the idExpression of the variable for most cases, or <code>Null</code> if the
   *     return-type is <code>Void</code>.
   */
  private CIdExpression createTemporaryVariable(IASTExpression e) {
    CType type = convertType(e);

    if (type instanceof CVoidType) {
      return null;
    }

    return createInitializedTemporaryVariable(getLocation(e), type, (CInitializer) null);
  }

  /** Convert Eclipse AST type to {@link CType}. */
  private CType convertType(IASTExpression e) {
    CType type = typeConverter.convert(e.getExpressionType());
    if (type.getCanonicalType() instanceof CVoidType) {
      if (e instanceof IASTFunctionCallExpression) {
        // Void method called and return value used.
        // Possibly this is an undeclared function.
        // Default return type in C for these cases is INT.
        return CNumericTypes.INT;
      }

      // workaround for strange CDT behaviour
    } else if (type instanceof CProblemType) {
      if (e instanceof IASTConditionalExpression) {
        return typeConverter.convert(
            ((IASTConditionalExpression) e).getNegativeResultExpression().getExpressionType());
      } else if (e instanceof IGNUASTCompoundStatementExpression) {
        // manually ceck whether type of compundStatementExpression is void
        IGNUASTCompoundStatementExpression statementExpression =
            (IGNUASTCompoundStatementExpression) e;
        IASTStatement[] statements = statementExpression.getCompoundStatement().getStatements();

        if (statements.length > 0) {
          IASTStatement lastStatement = statements[statements.length - 1];

          if (lastStatement instanceof IASTExpressionStatement) {
            IASTExpression lastExpression =
                ((IASTExpressionStatement) lastStatement).getExpression();
            return convertType(lastExpression);
          } else {
            return CVoidType.create(false, false);
          }
        }
      }
    }
    return type;
  }

  private CIdExpression createInitializedTemporaryVariable(
      final FileLocation loc, final CType pType, @Nullable CExpression initializer) {
    return createInitializedTemporaryVariable(
        loc, pType, initializer == null ? null : new CInitializerExpression(loc, initializer));
  }

  /**
   * creates temporary variables with increasing numbers with a certain initializer. If the
   * initializer is 'null', no initializer will be created.
   */
  private CIdExpression createInitializedTemporaryVariable(
      final FileLocation loc, final CType pType, @Nullable CInitializer initializer) {
    String name = "__CPAchecker_TMP_";
    int i = 0;
    while (scope.variableNameInUse(name + i)) {
      i++;
    }
    name += i;

    // If there is no initializer, the variable cannot be const.
    // TODO: consider always adding a const modifier if there is an initializer
    CType type = (initializer == null) ? CTypes.withoutConst(pType) : pType;

    if (type instanceof CArrayType && !(initializer instanceof CInitializerList)) {
      // Replace with pointer type.
      // This should actually be handled by Eclipse, because the C standard says in §5.4.2.1 (3)
      // that array types of operands are converted to pointer types except in a very few
      // specific cases (for which there will never be a temporary variable).
      // However, if the initializer is for an array, then of course we need to keep the array type.
      type = new CPointerType(type.isConst(), type.isVolatile(), ((CArrayType) type).getType());
    } else if (type instanceof CFunctionType) {
      // Happens if function pointers are used in ternary expressions, for example.
      type = new CPointerType(false, false, type);
    }

    CVariableDeclaration decl =
        new CVariableDeclaration(
            loc,
            scope.isGlobalScope(),
            CStorageClass.AUTO,
            type,
            name,
            name,
            scope.createScopedNameOf(name),
            initializer);

    scope.registerDeclaration(decl);
    sideAssignmentStack.addPreSideAssignment(decl);

    return new CIdExpression(loc, decl);
  }

  private CAstNode convert(IASTBinaryExpression e) {

    int eop = e.getOperator();
    if (eop == IASTBinaryExpression.op_logicalOr || eop == IASTBinaryExpression.op_logicalAnd) {
      CONDITION o1 = getConditionKind(e.getOperand1());
      CONDITION o2 = getConditionKind(e.getOperand2());

      if (o1 == CONDITION.NORMAL || o2 == CONDITION.NORMAL) {
        CIdExpression tmp = createTemporaryVariable(e);
        sideAssignmentStack.addConditionalExpression(e, tmp);
        return tmp;
      }

      if ((eop == IASTBinaryExpression.op_logicalAnd
              && (o1 == CONDITION.ALWAYS_FALSE || o2 == CONDITION.ALWAYS_FALSE))
          || (o1 == CONDITION.ALWAYS_FALSE && o2 == CONDITION.ALWAYS_FALSE)) {
        return CIntegerLiteralExpression.ZERO;
      }
      return CIntegerLiteralExpression.ONE;
    }

    Pair<BinaryOperator, Boolean> opPair = operatorConverter.convertBinaryOperator(e);
    BinaryOperator op = opPair.getFirst();
    boolean isAssign = opPair.getSecond();

    FileLocation fileLoc = getLocation(e);
    CExpression leftHandSide = convertExpressionWithoutSideEffects(e.getOperand1());

    if (isAssign) {
      if (!(leftHandSide instanceof CLeftHandSide)) {
        throw parseContext.parseError(
            "Lefthandside of Assignment "
                + e.getRawSignature()
                + " is no CLeftHandside but should be.",
            leftHandSide);
      }
      CLeftHandSide lhs = (CLeftHandSide) leftHandSide;

      if (op == null) {
        // a = b
        CAstNode rightHandSide =
            convertExpressionWithSideEffects(
                e.getOperand2()); // right-hand side may have a function call

        if (rightHandSide instanceof CExpression) {
          // a = b
          return new CExpressionAssignmentStatement(fileLoc, lhs, (CExpression) rightHandSide);

        } else if (rightHandSide instanceof CFunctionCallExpression) {
          // a = f()
          return new CFunctionCallAssignmentStatement(
              fileLoc, lhs, (CFunctionCallExpression) rightHandSide);

        } else if (rightHandSide instanceof CAssignment) {
          sideAssignmentStack.addPreSideAssignment(rightHandSide);
          return new CExpressionAssignmentStatement(
              fileLoc, lhs, ((CAssignment) rightHandSide).getLeftHandSide());
        } else {
          throw parseContext.parseError("Expression is not free of side-effects", e);
        }

      } else {
        // a += b etc.
        CExpression rightHandSide = convertExpressionWithoutSideEffects(e.getOperand2());

        // first create expression "a + b"
        CBinaryExpression exp = buildBinaryExpression(leftHandSide, rightHandSide, op);

        // and now the assignment
        return new CExpressionAssignmentStatement(fileLoc, lhs, exp);
      }

    } else {
      CExpression rightHandSide = convertExpressionWithoutSideEffects(e.getOperand2());
      return buildBinaryExpression(leftHandSide, rightHandSide, op);
    }
  }

  private CBinaryExpression buildBinaryExpression(
      CExpression operand1, CExpression operand2, BinaryOperator op) {
    try {
      return binExprBuilder.buildBinaryExpression(operand1, operand2, op);
    } catch (UnrecognizedCodeException e) {
      throw new CFAGenerationRuntimeException(e);
    }
  }

  private static boolean isPointerToVoid(final IASTExpression e) {
    return (e.getExpressionType() instanceof IPointerType)
        && ((IPointerType) e.getExpressionType()).getType() instanceof IBasicType
        && ((IBasicType) ((IPointerType) e.getExpressionType()).getType()).getKind() == Kind.eVoid;
  }

  private static boolean isRightHandSide(final IASTExpression e) {
    return e.getParent() instanceof IASTBinaryExpression
        && ((IASTBinaryExpression) e.getParent()).getOperator() == IASTBinaryExpression.op_assign
        && ((IASTBinaryExpression) e.getParent()).getOperand2() == e;
  }

  private CAstNode convert(IASTCastExpression e) {
    final CExpression operand;
    final FileLocation loc = getLocation(e);
    /* using #typeConverter.convert(e.getExpressionType()); to recheck if our evaluated
     * castType is valid, is wrong in some cases, so we scip this check, and only
     * use our convert(IASTTypeID) method
     * a case where convert(e.getExpressionType()) fails is:
     * struct lock {
     *   unsigned int slock;
     * }
     *
     * int tmp = (*(volatile typeof(lock->slock) *)&(lock->slock));
     *
     * => the convert(IASTTypeId) method returns (unsigned int)*
     * => the convert(CType) method returns (volatile int)*
     * the second one is obviously wrong, because the unsigned is missing
     */
    final CType castType = convert(e.getTypeId());

    if (castType.equals(CVoidType.VOID)) {
      // ignore casts to void as in "(void) f();"
      return convertExpressionWithSideEffects(e.getOperand());
    }

    // To recognize and simplify constructs e.g. struct s *ps = (struct s *) malloc(.../* e.g.
    // sizeof(struct s)*/);
    if (e.getOperand() instanceof CASTFunctionCallExpression
        && castType.getCanonicalType() instanceof CPointerType
        && isRightHandSide(e)
        && isPointerToVoid(e.getOperand())) {
      return convertExpressionWithSideEffects(e.getOperand());
    } else {
      operand = convertExpressionWithoutSideEffects(e.getOperand());
    }

    if ("__imag__".equals(e.getTypeId().getRawSignature())) {
      return new CComplexCastExpression(loc, castType, operand, castType, false);
    } else if ("__real__".equals(e.getTypeId().getRawSignature())) {
      return new CComplexCastExpression(loc, castType, operand, castType, true);
    }

    if (e.getOperand() instanceof IASTFieldReference
        && ((IASTFieldReference) e.getOperand()).isPointerDereference()) {
      return createInitializedTemporaryVariable(
          loc, castType, new CCastExpression(loc, castType, operand));
    } else {
      return new CCastExpression(loc, castType, operand);
    }
  }

  private static class ContainsProblemTypeVisitor
      extends DefaultCTypeVisitor<Boolean, NoException> {

    @Override
    public Boolean visitDefault(CType pT) {
      return Boolean.FALSE;
    }

    @Override
    public Boolean visit(final CArrayType t) {
      return t.getType().accept(this);
    }

    @Override
    public Boolean visit(final CElaboratedType t) {
      final CType realType = t.getRealType();
      if (realType != null) {
        return realType.accept(this);
      } else {
        return false;
      }
    }

    @Override
    public Boolean visit(final CFunctionType t) {
      for (CType parameterType : t.getParameters()) {
        if (parameterType.accept(this)) {
          return true;
        }
      }
      return t.getReturnType().accept(this);
    }

    @Override
    public Boolean visit(final CPointerType t) {
      return t.getType().accept(this);
    }

    @Override
    public Boolean visit(final CProblemType t) {
      return true;
    }

    @Override
    public Boolean visit(CTypedefType t) {
      return t.getRealType().accept(this);
    }

    @Override
    public Boolean visit(CBitFieldType pCBitFieldType) {
      return pCBitFieldType.getType().accept(this);
    }
  }

  private static boolean containsProblemType(final CType type) {
    return type.accept(containsProblemTypeVisitor);
  }

  private CFieldReference convert(IASTFieldReference e) {
    CExpression owner = convertExpressionWithoutSideEffects(e.getFieldOwner());
    String fieldName = convert(e.getFieldName());
    final FileLocation loc = getLocation(e);

    CType ownerType = owner.getExpressionType().getCanonicalType();
    if (e.isPointerDereference()) {
      if (ownerType instanceof CPointerType) {
        ownerType = ((CPointerType) ownerType).getType();
      } else if (!(ownerType instanceof CProblemType)) {
        throw parseContext.parseError("Pointer dereference of non-pointer type " + ownerType, e);
      }
    }

    // In case of an anonymous struct, the type provided by Eclipse
    // does not match our type because we added a name.
    // So make sure to not use the Eclipse type.

    final CFieldReference fullFieldReference;
    List<Pair<String, CType>> wayToInnerField = ImmutableList.of();
    if (ownerType instanceof CElaboratedType) {
      assert ((CElaboratedType) ownerType).getRealType()
          == null; // otherwise getCanonicalType is broken
      throw parseContext.parseError(
          "Cannot access the field "
              + fieldName
              + " in type "
              + ownerType
              + " which does not have a definition",
          e);
    } else if (ownerType instanceof CProblemType) {
      fullFieldReference =
          new CFieldReference(
              loc,
              typeConverter.convert(e.getExpressionType()),
              fieldName,
              owner,
              e.isPointerDereference());
    } else if (ownerType instanceof CCompositeType) {
      wayToInnerField =
          getWayToInnerField((CCompositeType) ownerType, fieldName, loc, new ArrayList<>());
      if (!wayToInnerField.isEmpty()) {
        CExpression current = owner;
        boolean isPointerDereference = e.isPointerDereference();
        for (Pair<String, CType> field : wayToInnerField) {
          current =
              new CFieldReference(
                  loc, field.getSecond(), field.getFirst(), current, isPointerDereference);
          isPointerDereference = false;
        }
        fullFieldReference = (CFieldReference) current;
      } else {
        throw parseContext.parseError(
            "Accessing unknown field " + fieldName + " in type " + ownerType, e);
      }
    } else {
      throw parseContext.parseError(
          "Cannot access field "
              + fieldName
              + " in type "
              + ownerType
              + " which is not a composite type",
          e);
    }

    // FOLLOWING IF CLAUSE WILL ONLY BE EVALUATED WHEN THE OPTION cfa.simplifyPointerExpressions IS
    // SET TO TRUE
    // if the owner is a FieldReference itself there's the need for a temporary Variable
    // but only if we are not in global scope, otherwise there will be parsing errors
    if (options.simplifyPointerExpressions()
        && (wayToInnerField.size() > 1 || owner instanceof CFieldReference)
        && !scope.isGlobalScope()) {
      CExpression tmp = fullFieldReference;
      Deque<Pair<CType, String>> fields = new ArrayDeque<>();
      while (tmp != owner) {
        fields.push(Pair.of(tmp.getExpressionType(), ((CFieldReference) tmp).getFieldName()));
        tmp = ((CFieldReference) tmp).getFieldOwner();
      }

      boolean isFirstVisit = true;
      while (!fields.isEmpty()) {
        Pair<CType, String> actField = fields.pop();

        // base case, when there is no field access left
        if (fields.isEmpty()) {

          // in case there is only one field access we have to check here on a pointer dereference
          if (isFirstVisit && e.isPointerDereference()) {
            CPointerExpression exp = new CPointerExpression(loc, owner.getExpressionType(), owner);
            CExpression tmpOwner =
                new CFieldReference(loc, actField.getFirst(), actField.getSecond(), exp, false);
            owner = createInitializedTemporaryVariable(loc, tmpOwner.getExpressionType(), tmpOwner);
          } else {
            owner =
                new CFieldReference(loc, actField.getFirst(), actField.getSecond(), owner, false);
          }
        } else {

          // here could be a pointer dereference, in this case we create a temporary variable
          // otherwise there is nothing special to be done
          if (isFirstVisit) {
            if (e.isPointerDereference()) {
              CPointerExpression exp =
                  new CPointerExpression(loc, owner.getExpressionType(), owner);
              CExpression tmpOwner =
                  new CFieldReference(loc, actField.getFirst(), actField.getSecond(), exp, false);
              owner =
                  createInitializedTemporaryVariable(loc, tmpOwner.getExpressionType(), tmpOwner);
            } else {
              owner =
                  new CFieldReference(loc, actField.getFirst(), actField.getSecond(), owner, false);
            }
            isFirstVisit = false;

            // only first field access may be an pointer dereference so we do not have to check
            // anything
            // in this clause, just put a field reference to the next field on the actual owner
          } else {
            owner =
                new CFieldReference(loc, actField.getFirst(), actField.getSecond(), owner, false);
          }
        }
      }

      return (CFieldReference) owner;

      // FOLLOWING IF CLAUSE WILL ONLY BE EVALUATED WHEN THE OPTION cfa.simplifyPointerExpressions
      // IS SET TO TRUE
      // if there is a "var->field" convert it to (*var).field
    } else if (options.simplifyPointerExpressions()) {
      return fullFieldReference.withExplicitPointerDereference();
    }

    return fullFieldReference;
  }

  /**
   * This method creates a list of all necessary field access for finding the searched field.
   * Besides the case that the searched field is directly in the struct, there is the case that the
   * field is in an anonymous struct or union inside the "owner" struct. This anonymous structs /
   * unions are then the "way" to the searched field.
   *
   * @param allReferences an empty list
   * @return the fields (including the searched one) in the right order
   */
  private static List<Pair<String, CType>> getWayToInnerField(
      CCompositeType owner,
      String fieldName,
      FileLocation loc,
      List<Pair<String, CType>> allReferences) {
    for (CCompositeTypeMemberDeclaration member : owner.getMembers()) {
      if (member.getName().equals(fieldName)) {
        allReferences.add(Pair.of(member.getName(), member.getType()));
        return ImmutableList.copyOf(allReferences);
      }
    }

    // no field found in current struct, so proceed to the structs/unions which are
    // fields inside the current struct
    for (CCompositeTypeMemberDeclaration member : owner.getMembers()) {
      CType memberType = member.getType().getCanonicalType();
      if (memberType instanceof CCompositeType
          && member.getName().contains("__anon_type_member_")) {
        List<Pair<String, CType>> tmp = new ArrayList<>(allReferences);
        tmp.add(Pair.of(member.getName(), member.getType()));
        tmp = getWayToInnerField((CCompositeType) memberType, fieldName, loc, tmp);
        if (!tmp.isEmpty()) {
          return ImmutableList.copyOf(tmp);
        }
      }
    }

    return ImmutableList.of();
  }

  private CRightHandSide convert(IASTFunctionCallExpression e) {

    CExpression functionNameExpression =
        convertExpressionWithoutSideEffects(e.getFunctionNameExpression());
    CFunctionDeclaration declaration = null;
    final FileLocation loc = getLocation(e);

    if (functionNameExpression instanceof CIdExpression) {
      if (FUNC_TYPES_COMPATIBLE.equals(((CIdExpression) functionNameExpression).getName())) {
        sideAssignmentStack.enterBlock();
        List<CExpression> params = new ArrayList<>();
        for (IASTInitializerClause i : e.getArguments()) {
          params.add(convertExpressionWithoutSideEffects(toExpression(i)));
        }
        sideAssignmentStack.getAndResetConditionalExpressions();
        sideAssignmentStack.getAndResetPostSideAssignments();
        sideAssignmentStack.getAndResetPreSideAssignments();
        sideAssignmentStack.leaveBlock();
        if (params.size() == 2) {
          // Expression from convertExpressionWithoutSideEffects is null if type was void
          CType type1 = params.get(0) == null ? CVoidType.VOID : params.get(0).getExpressionType();
          CType type2 = params.get(1) == null ? CVoidType.VOID : params.get(1).getExpressionType();
          if (areCompatibleTypes(type1, type2)) {
            return CIntegerLiteralExpression.ONE;
          } else {
            return CIntegerLiteralExpression.ZERO;
          }
        }
      }
    }

    List<CExpression> params = new ArrayList<>();
    for (IASTInitializerClause i : e.getArguments()) {
      params.add(convertExpressionWithoutSideEffects(toExpression(i)));
    }

    if (functionNameExpression instanceof CIdExpression) {
      // this function is a gcc extension which checks if the given parameter is
      // a constant value. We can easily provide this functionality by checking
      // if the parameter is a literal expression.
      // We only do check it if the function is not declared.
      if (((CIdExpression) functionNameExpression).getName().equals(FUNC_CONSTANT)
          && params.size() == 1
          && scope.lookupFunction(FUNC_CONSTANT) == null) {
        if (params.get(0) instanceof CLiteralExpression) {
          return CIntegerLiteralExpression.ONE;
        } else {
          return CIntegerLiteralExpression.ZERO;
        }
      }
      if (((CIdExpression) functionNameExpression).getName().equals(FUNC_OFFSETOF)
          && params.size() == 1
          && params.get(0) instanceof CFieldReference) {
        CFieldReference exp = (CFieldReference) params.get(0);
        BigInteger offset = handleBuiltinOffsetOfFunction(exp, e);
        BigInteger byteInBit = new BigInteger("8");
        if (offset.remainder(byteInBit).equals(BigInteger.ZERO)) {
          return new CIntegerLiteralExpression(loc, CNumericTypes.INT, offset.divide(byteInBit));
        } else {
          throw parseContext.parseError("__builtin_offset is not applicable to bitfields ", exp);
        }
      }

      CSimpleDeclaration d = ((CIdExpression) functionNameExpression).getDeclaration();
      if (d instanceof CFunctionDeclaration) {
        // it may also be a variable declaration, when a function pointer is called
        declaration = (CFunctionDeclaration) d;
      }

      if ((declaration == null)
          && FUNC_EXPECT.equals(((CIdExpression) functionNameExpression).getName())
          && params.size() == 2) {

        // This is the GCC built-in function __builtin_expect(exp, c)
        // that behaves like (exp == c).
        // http://gcc.gnu.org/onlinedocs/gcc/Other-Builtins.html#index-g_t_005f_005fbuiltin_005fexpect-3345

        return buildBinaryExpression(params.get(0), params.get(1), BinaryOperator.EQUALS);
      }
    }

    // just unwrap typedefs, we do not want to put a canonical type into the CPointerExpression,
    // but the original type
    CType functionNameType = functionNameExpression.getExpressionType();
    while (functionNameType instanceof CTypedefType) {
      functionNameType = ((CTypedefType) functionNameType).getRealType();
    }
    if (functionNameType instanceof CPointerType
        && ((CPointerType) functionNameType).getType().getCanonicalType()
            instanceof CFunctionType) {
      // Function pointers can be called either via "*fp" or simply "fp".
      // We add the dereference operator, if it is missing.

      functionNameExpression =
          new CPointerExpression(
              functionNameExpression.getFileLocation(),
              ((CPointerType) functionNameType).getType(),
              functionNameExpression);
    }

    if (functionNameExpression instanceof CIdExpression
        && BuiltinOverflowFunctions.isBuiltinOverflowFunction(
            ((CIdExpression) functionNameExpression).getName())) {
      CType returnType = CNumericTypes.BOOL;
      return new CFunctionCallExpression(
          loc, returnType, functionNameExpression, params, declaration);
    }

    CType returnType = typeConverter.convert(e.getExpressionType());
    if (containsProblemType(returnType)) {
      // workaround for Eclipse CDT problems
      if (declaration != null) {
        returnType = declaration.getType().getReturnType();
        logger.log(
            Level.FINE,
            loc + ":",
            "Replacing return type",
            returnType,
            "of function call",
            e.getRawSignature(),
            "with",
            returnType);
      } else {
        final CType functionType = functionNameExpression.getExpressionType().getCanonicalType();
        if (functionType instanceof CFunctionType) {
          returnType = ((CFunctionType) functionType).getReturnType();
          logger.log(
              Level.FINE,
              loc + ":",
              "Replacing return type",
              returnType,
              "of function call",
              e.getRawSignature(),
              "with",
              returnType);
        }
      }
    }

    if (declaration == null
        && functionNameExpression instanceof CIdExpression
        && returnType instanceof CVoidType) {
      // Undeclared functions are a problem for analysis that need precise types.
      // We can at least set the return type to "int" as the standard says.
      logger.log(
          Level.FINE,
          loc + ":",
          "Setting return type of of undeclared function",
          functionNameExpression,
          "to int.");
      returnType = CNumericTypes.INT;
    }

    return new CFunctionCallExpression(
        loc, returnType, functionNameExpression, params, declaration);
  }

  private BigInteger handleBuiltinOffsetOfFunction(
      CFieldReference exp, IASTFunctionCallExpression e) {
    List<CFieldReference> fields = new ArrayList<>();
    fields.add(exp);
    while (exp.getFieldOwner() instanceof CFieldReference) {
      CFieldReference tmp = (CFieldReference) exp.getFieldOwner();
      exp = tmp;
      fields.add(exp);
    }

    if (!(exp.getFieldOwner() instanceof CIdExpression)) {
      throw parseContext.parseError(
          "unexpected type " + exp.getFieldOwner() + " in __builtin_offsetof argument: ", e);
    }
    final CType ownerType = exp.getFieldOwner().getExpressionType().getCanonicalType();
    if (!(ownerType instanceof CCompositeType)) {
      throw parseContext.parseError(
          "unexpected type " + ownerType + " in __builtin_offsetof argument", e);
    }
    CCompositeType structType = (CCompositeType) ownerType;

    BigInteger sumOffset = BigInteger.ZERO;
    Collections.reverse(fields);

    for (CFieldReference field : fields) {
      BigInteger offset = machinemodel.getFieldOffsetInBits(structType, field.getFieldName());
      sumOffset = sumOffset.add(offset);
      CFieldReference lastField = fields.get(fields.size() - 1);
      if (!field.equals(lastField)) {
        final CType fieldType = field.getExpressionType().getCanonicalType();
        if (!(fieldType instanceof CCompositeType)) {
          throw parseContext.parseError(
              "unexpected type " + fieldType + " in __builtin_offsetof argument", e);
        }
        structType = (CCompositeType) fieldType;
      }
    }

    return sumOffset;
  }

  private boolean areCompatibleTypes(CType a, CType b) {
    // http://gcc.gnu.org/onlinedocs/gcc/Other-Builtins.html#index-g_t_005f_005fbuiltin_005ftypes_005fcompatible_005fp-3613
    a = withoutConst(withoutVolatile(a.getCanonicalType()));
    b = withoutConst(withoutVolatile(b.getCanonicalType()));
    if (a.equals(b)) {
      return true;
    }
    if (a instanceof CArrayType && b instanceof CArrayType) {
      CArrayType arrayA = (CArrayType) a;
      CArrayType arrayB = (CArrayType) b;
      if (arrayA.getType().equals(arrayB.getType())) {
        if (arrayA.getLength() == null || arrayB.getLength() == null) {
          // The type int[] and int[5] are compatible
          return true;
        }
      }
    }
    return false;
  }

  private CIdExpression convert(IASTIdExpression e) {
    String name = convert(e.getName());

    // Try to find declaration.
    // Variables per se actually do not bind stronger than function,
    // but local variables do.
    // Furthermore, a global variable and a function with the same name
    // cannot exist, so the following code works correctly.
    // We first try to lookup static variables.
    CSimpleDeclaration declaration = scope.lookupVariable(staticVariablePrefix + name);
    if (declaration == null) {
      declaration = scope.lookupVariable(name);
    }
    if (declaration == null) {
      declaration = scope.lookupFunction(staticVariablePrefix + name);
    }
    if (declaration == null) {
      declaration = scope.lookupFunction(name);
    }

    if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(name)) {
      var parameterTypes = BuiltinOverflowFunctions.getParameterTypes(name);
      CFunctionType functionType = new CFunctionType(CNumericTypes.BOOL, parameterTypes, false);
      var parameterDeclarations =
          transformedImmutableListCopy(
              parameterTypes,
              paramType -> new CParameterDeclaration(FileLocation.DUMMY, paramType, "p"));
      declaration =
          new CFunctionDeclaration(
              FileLocation.DUMMY, functionType, name, parameterDeclarations, ImmutableSet.of());
    }

    // declaration may still be null here,
    // for example when parsing AST patterns for the AutomatonCPA.

    if (declaration != null) {
      name = declaration.getName(); // may have been renamed
    }

    CType type;
    // Use declaration type when possible to fix issues with anonymous composites, problem types
    // etc.
    if (declaration != null) {
      type = declaration.getType();
    } else {
      type = typeConverter.convert(e.getExpressionType());
    }

    if (declaration instanceof CEnumerator
        && type instanceof CElaboratedType
        && ((CElaboratedType) type).getKind() == ComplexTypeKind.ENUM
        && ((CElaboratedType) type).getRealType() == null) {

      // This is a reference to a value of an anonymous enum ("enum { e }").
      // Such types cannot be looked up, and thus the CElaboratedType misses
      // the reference to the enum type.
      CEnumType enumType = ((CEnumerator) declaration).getEnum();
      // enumType is null if an enum value is referenced inside the enum declaration,
      // e.g. like this: "enum { e1, e2 = e1 }"
      if (enumType != null) {
        type =
            new CElaboratedType(
                type.isConst(),
                type.isVolatile(),
                ComplexTypeKind.ENUM,
                enumType.getName(),
                enumType.getOrigName(),
                enumType);
      }
    }

    return new CIdExpression(getLocation(e), type, name, declaration);
  }

  private CAstNode convert(final IASTUnaryExpression e) {
    if (e.getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
      // we can have side effects here
      return convertExpressionWithSideEffects(e.getOperand());
    }

    final CExpression operand = convertExpressionWithoutSideEffects(e.getOperand());
    if (operand == null) {
      return operand;
    }
    final FileLocation fileLoc = getLocation(e);
    final CType operandType = operand.getExpressionType();

    switch (e.getOperator()) {
      case IASTUnaryExpression.op_bracketedPrimary:
        throw new AssertionError("handled above");
      case IASTUnaryExpression.op_plus:
        return operand;

      case IASTUnaryExpression.op_star:
        {

          // In case of pointers inside field references that refer to inner fields
          // the CDT type is not as we want it, thus we resolve the type on our own.
          CType type;
          if (operandType instanceof CPointerType) {
            type = ((CPointerType) operandType).getType();
          } else if (operandType instanceof CArrayType) {
            type = ((CArrayType) operandType).getType();
          } else {
            if (!(operandType instanceof CProblemType)) {
              logger.logf(
                  Level.WARNING,
                  "%s: Dereferencing of non-pointer type %s in expression %s",
                  fileLoc,
                  operandType,
                  e.getRawSignature());
            }
            type = typeConverter.convert(e.getExpressionType());
          }
          return simplifyUnaryPointerExpression(operand, fileLoc, type);
        }
      case IASTUnaryExpression.op_amper:
        {

          // FOLLOWING IF CLAUSE WILL ONLY BE EVALUATED WHEN THE OPTION
          // cfa.simplifyPointerExpressions IS SET TO TRUE
          // in case of *& both can be left out
          if (options.simplifyPointerExpressions() && operand instanceof CPointerExpression) {
            return ((CPointerExpression) operand).getOperand();
          }

          CType type = typeConverter.convert(e.getExpressionType());
          if (containsProblemType(type)) {
            type = new CPointerType(true, false, operandType);
          }

          // if none of the special cases before fits the default unaryExpression is created
          return new CUnaryExpression(fileLoc, type, operand, UnaryOperator.AMPER);
        }
      case IASTUnaryExpression.op_labelReference:
        // L: void * addressOfLabel = && L;

        if (!(operand instanceof CIdExpression)) {
          throw parseContext.parseError("Invalid operand for address-of-label operator", e);
        }
        String labelName = ((CIdExpression) operand).getName();

        // type given by CDT is problem type
        return new CAddressOfLabelExpression(fileLoc, CPointerType.POINTER_TO_VOID, labelName);

      case IASTUnaryExpression.op_prefixIncr:
      case IASTUnaryExpression.op_prefixDecr:
        // instead of ++x, create "x = x+1"

        BinaryOperator preOp;
        switch (e.getOperator()) {
          case IASTUnaryExpression.op_prefixIncr:
            preOp = BinaryOperator.PLUS;
            break;
          case IASTUnaryExpression.op_prefixDecr:
            preOp = BinaryOperator.MINUS;
            break;
          default:
            throw new AssertionError();
        }

        CBinaryExpression preExp =
            buildBinaryExpression(operand, CIntegerLiteralExpression.ONE, preOp);
        CLeftHandSide lhsPre = (CLeftHandSide) operand;

        return new CExpressionAssignmentStatement(fileLoc, lhsPre, preExp);

      case IASTUnaryExpression.op_postFixIncr:
      case IASTUnaryExpression.op_postFixDecr:
        // instead of x++ create "x = x + 1"

        BinaryOperator postOp;
        switch (e.getOperator()) {
          case IASTUnaryExpression.op_postFixIncr:
            postOp = BinaryOperator.PLUS;
            break;
          case IASTUnaryExpression.op_postFixDecr:
            postOp = BinaryOperator.MINUS;
            break;
          default:
            throw new AssertionError();
        }

        CBinaryExpression postExp =
            buildBinaryExpression(operand, CIntegerLiteralExpression.ONE, postOp);
        CLeftHandSide lhsPost = (CLeftHandSide) operand;
        CExpressionAssignmentStatement result =
            new CExpressionAssignmentStatement(fileLoc, lhsPost, postExp);

        if (e.getParent() instanceof IASTForStatement
            && e.getPropertyInParent() == IASTForStatement.ITERATION) {
          return result;
        }

        CExpression tmp =
            createInitializedTemporaryVariable(fileLoc, lhsPost.getExpressionType(), lhsPost);
        sideAssignmentStack.addPreSideAssignment(result);

        return tmp;

      case IASTUnaryExpression.op_not:
        try {
          return binExprBuilder.negateExpressionAndSimplify(operand);
        } catch (UnrecognizedCodeException ex) {
          throw new CFAGenerationRuntimeException(ex);
        }

      default:
        CType type;
        if (e.getOperator() == IASTUnaryExpression.op_alignOf
            || e.getOperator() == IASTUnaryExpression.op_sizeof) {
          // C11 §6.5.3.4 (5) type is always size_t (CDT has wrong type for _Alignof)
          type = CNumericTypes.SIZE_T;
        } else if (e.getOperator() == IASTUnaryExpression.op_minus
            && operand.getExpressionType() instanceof CSimpleType) {
          // CDT parser might get the type wrong in this case, e.g.:
          // literals that should be of type long would still be int instead of long,
          // because CDT only makes the operand long if there is a 'L' at the end
          // => we cannot use e.getExpressionType() here!
          CSimpleType innerType = (CSimpleType) operand.getExpressionType();
          // now do not forget: operand should get promoted to int if its type is smaller than int:
          type =
              CTypes.isIntegerType(innerType)
                  ? machinemodel.applyIntegerPromotion(innerType)
                  : innerType;
        } else {
          type = typeConverter.convert(e.getExpressionType());
        }
        return new CUnaryExpression(
            fileLoc, type, operand, operatorConverter.convertUnaryOperator(e));
    }
  }

  /** returns a CPointerExpression, that may be simplified. */
  private CExpression simplifyUnaryPointerExpression(
      final CExpression operand, final FileLocation fileLoc, final CType type) {

    // FOLLOWING IF CLAUSE WILL ONLY BE EVALUATED WHEN THE OPTION cfa.simplifyPointerExpressions IS
    // SET TO TRUE
    if (options.simplifyPointerExpressions()) {

      final CType operandType = operand.getExpressionType();

      // if there is a dereference on a field of a struct a temporary variable is needed
      if (operand instanceof CFieldReference) {
        CIdExpression tmpVar = createInitializedTemporaryVariable(fileLoc, operandType, operand);
        return new CPointerExpression(fileLoc, type, tmpVar);
      }

      // in case of *(a[index])
      else if (operand instanceof CArraySubscriptExpression) {
        CIdExpression tmpVar = createInitializedTemporaryVariable(fileLoc, operandType, operand);
        return new CPointerExpression(fileLoc, type, tmpVar);
      }

      // in case of *& both can be left out
      else if (operand instanceof CUnaryExpression
          && ((CUnaryExpression) operand).getOperator() == UnaryOperator.AMPER) {
        return ((CUnaryExpression) operand).getOperand();
      }

      // in case of ** a temporary variable is needed
      else if (operand instanceof CPointerExpression) {
        CIdExpression tmpVar = createInitializedTemporaryVariable(fileLoc, operandType, operand);
        return new CPointerExpression(fileLoc, type, tmpVar);
      }

      // in case of p.e. *(a+b) or *(a-b) or *(a ANY_OTHER_OPERATOR b) a temporary variable is
      // needed
      else if (operand instanceof CBinaryExpression) {
        CIdExpression tmpVar = createInitializedTemporaryVariable(fileLoc, operandType, operand);
        return new CPointerExpression(fileLoc, type, tmpVar);
      }
    }

    // if none of the special cases before fits the default unaryExpression is created
    return new CPointerExpression(fileLoc, type, operand);
  }

  private CTypeIdExpression convert(IASTTypeIdExpression e) {
    TypeIdOperator typeIdOperator = operatorConverter.convertTypeIdOperator(e);
    CType expressionType;
    CType typeId = convert(e.getTypeId());

    if ((typeIdOperator == TypeIdOperator.ALIGNOF || typeIdOperator == TypeIdOperator.SIZEOF)) {
      if (typeId.isIncomplete()) {
        // Cannot compute alignment
        throw parseContext.parseError(
            "Invalid application of "
                + typeIdOperator.getOperator()
                + " to incomplete type "
                + typeId,
            e);
      }
      // C11 §6.5.3.4 (5) type is always size_t (CDT has wrong type for _Alignof)
      expressionType = CNumericTypes.SIZE_T;
    } else {
      expressionType = typeConverter.convert(e.getExpressionType());
    }
    return new CTypeIdExpression(getLocation(e), expressionType, typeIdOperator, typeId);
  }

  private CExpression convert(IASTTypeIdInitializerExpression e) {
    // This is a "compound literal" (C11 § 6.5.2.5).
    // It is similar to Java array instantiations with "new String[]{...}".
    FileLocation fileLoc = getLocation(e);
    CType type = convert(e.getTypeId());
    CInitializer initializer = convert(e.getInitializer(), type, null);

    return createInitializedTemporaryVariable(fileLoc, type, initializer);
  }

  public CAstNode convert(final IASTStatement s) {

    if (s instanceof IASTExpressionStatement) {
      return convert((IASTExpressionStatement) s);

    } else if (s instanceof IASTReturnStatement) {
      return convert((IASTReturnStatement) s);

    } else if (s instanceof IASTProblemStatement) {
      throw parseContext.parseError((IASTProblemStatement) s);

    } else {
      throw parseContext.parseError("unknown statement: " + s.getClass(), s);
    }
  }

  public CStatement convert(final IASTExpressionStatement s) {
    return convertExpressionToStatement(s.getExpression());
  }

  public CStatement convertExpressionToStatement(final IASTExpression e) {
    CAstNode node = convertExpressionWithSideEffects(e);

    if (node instanceof CExpressionAssignmentStatement) {
      return (CExpressionAssignmentStatement) node;

    } else if (node instanceof CFunctionCallAssignmentStatement) {
      return (CFunctionCallAssignmentStatement) node;

    } else if (node instanceof CFunctionCallExpression) {
      return new CFunctionCallStatement(getLocation(e), (CFunctionCallExpression) node);

    } else if (node instanceof CExpression) {
      return new CExpressionStatement(getLocation(e), (CExpression) node);

    } else if (node == null) {
      return null;

    } else {
      throw new AssertionError();
    }
  }

  public CReturnStatement convert(final IASTReturnStatement s) {
    final FileLocation loc = getLocation(s);
    final Optional<CExpression> returnExp =
        Optional.ofNullable(convertExpressionWithoutSideEffects(s.getReturnValue()));
    final Optional<CVariableDeclaration> returnVariableDeclaration =
        ((FunctionScope) scope).getReturnVariable();

    final Optional<CAssignment> returnAssignment;
    if (returnVariableDeclaration.isPresent()) {
      CIdExpression lhs = new CIdExpression(loc, returnVariableDeclaration.orElseThrow());
      CExpression rhs = null;
      if (returnExp.isPresent()) {
        rhs = returnExp.orElseThrow();
      } else {
        logger.log(
            Level.WARNING, loc + ":", "Return statement without expression in non-void function.");
        CInitializer defaultValue =
            CDefaults.forType(returnVariableDeclaration.orElseThrow().getType(), loc);
        if (defaultValue instanceof CInitializerExpression) {
          rhs = ((CInitializerExpression) defaultValue).getExpression();
        }
      }
      if (rhs != null) {
        returnAssignment = Optional.of(new CExpressionAssignmentStatement(loc, lhs, rhs));
      } else {
        returnAssignment = Optional.empty();
      }

    } else {
      if (returnExp.isPresent()) {
        logger.log(
            Level.WARNING,
            loc + ":",
            "Return statement with expression",
            returnExp.orElseThrow(),
            "in void function.");
      }
      returnAssignment = Optional.empty();
    }

    return new CReturnStatement(loc, returnExp, returnAssignment);
  }

  public CFunctionDeclaration convert(final IASTFunctionDefinition f) {
    Pair<CStorageClass, ? extends CType> specifier = convert(f.getDeclSpecifier());

    CStorageClass cStorageClass = specifier.getFirst();
    if (!(cStorageClass == CStorageClass.AUTO
        || cStorageClass == CStorageClass.STATIC
        || cStorageClass == CStorageClass.EXTERN)) {
      // storage class static is the same as auto, just with reduced visibility to a single
      // compilation unit,
      // and as we only handle single compilation units, we can ignore it. A storage class extern
      // associated
      // with a function definition, while superfluous, unless it's an inline function, is allowed,
      // too.
      throw parseContext.parseError("Unsupported storage class for function definition", f);
    }

    Triple<CType, IASTInitializer, String> declarator =
        convert(f.getDeclarator(), specifier.getSecond(), cStorageClass == CStorageClass.STATIC);

    if (!(declarator.getFirst() instanceof CFunctionTypeWithNames)) {
      throw parseContext.parseError("Unsupported nested declarator for function definition", f);
    }
    if (declarator.getSecond() != null) {
      throw parseContext.parseError("Unsupported initializer for function definition", f);
    }
    if (declarator.getThird() == null) {
      throw parseContext.parseError("Missing name for function definition", f);
    }

    CFunctionTypeWithNames declSpec = (CFunctionTypeWithNames) declarator.getFirst();

    return new CFunctionDeclaration(
        getLocation(f),
        declSpec,
        declSpec.getName(),
        declarator.getThird(),
        declSpec.getParameterDeclarations(),
        getAttributes(f.getDeclarator()));
  }

  public List<CDeclaration> convert(final IASTSimpleDeclaration d) {

    FileLocation fileLoc = getLocation(d);

    Pair<CStorageClass, ? extends CType> specifier = convert(d.getDeclSpecifier());
    CStorageClass cStorageClass = specifier.getFirst();
    CType type = specifier.getSecond();

    IASTDeclarator[] declarators = d.getDeclarators();
    List<CDeclaration> result = new ArrayList<>();

    if (type instanceof CCompositeType || type instanceof CEnumType) {
      // struct, union, or enum declaration
      // split type definition from eventual variable declaration
      CComplexType complexType = (CComplexType) type;

      // in case of struct declarations with variable declarations we
      // need to add the struct declaration as sideeffect, so that
      // we can be sure the variable gets the correct (perhaps renamed) type
      if (declarators.length > 0 && type instanceof CCompositeType) {
        addSideEffectDeclarationForType((CCompositeType) complexType, fileLoc);
        complexType = scope.lookupType(complexType.getQualifiedName());

      } else {
        result.add(new CComplexTypeDeclaration(fileLoc, scope.isGlobalScope(), complexType));
      }

      // now replace type with an elaborated type referencing the new type
      type =
          new CElaboratedType(
              type.isConst(),
              type.isVolatile(),
              complexType.getKind(),
              complexType.getName(),
              complexType.getOrigName(),
              complexType);

    } else if (type instanceof CElaboratedType) {
      boolean typeAlreadyKnown =
          scope.lookupType(((CElaboratedType) type).getQualifiedName()) != null;
      boolean variableDeclaration = declarators != null && declarators.length > 0;
      if (!typeAlreadyKnown || !variableDeclaration) {
        CComplexTypeDeclaration newD =
            new CComplexTypeDeclaration(fileLoc, scope.isGlobalScope(), (CElaboratedType) type);
        result.add(newD);
      }
    }

    if (declarators != null) {
      for (IASTDeclarator c : declarators) {

        FileLocation declaratorLocation = getLocation(c);
        if (!declaratorLocation.isRealLocation()) {
          declaratorLocation = fileLoc;
        } else if (c == declarators[0]) {
          declaratorLocation =
              new FileLocation(
                  fileLoc.getFileName(),
                  fileLoc.getNiceFileName(),
                  fileLoc.getNodeOffset(),
                  declaratorLocation.getNodeOffset()
                      - fileLoc.getNodeOffset()
                      + declaratorLocation.getNodeLength(),
                  fileLoc.getStartingLineNumber(),
                  declaratorLocation.getEndingLineNumber(),
                  fileLoc.getStartingLineInOrigin(),
                  fileLoc.getEndingLineInOrigin(),
                  fileLoc.isOffsetRelatedToOrigin());
        }
        result.add(createDeclaration(declaratorLocation, cStorageClass, type, c));
      }
    }

    return result;
  }

  private CDeclaration createDeclaration(
      FileLocation fileLoc, CStorageClass cStorageClass, CType type, IASTDeclarator d) {
    boolean isGlobal = scope.isGlobalScope();

    if (d != null) {
      Triple<CType, IASTInitializer, String> declarator = convert(d, type);

      type = declarator.getFirst();

      IASTInitializer initializer = declarator.getSecond();

      String name = declarator.getThird();

      if (name == null) {
        throw parseContext.parseError("Declaration without name", d);
      }

      // first handle all special cases

      if (cStorageClass == CStorageClass.TYPEDEF) {
        if (initializer != null) {
          throw parseContext.parseError("Typedef with initializer", d);
        }

        name = scope.getFileSpecificTypeName(name);
        return new CTypeDefDeclaration(
            fileLoc, isGlobal, type, name, scope.createScopedNameOf(name));
      }

      // We need to resolve typedefs, but we cannot call getCanonicalType()
      // because we need to leave the parameter types unchanged.
      CType innerType = type;
      while (innerType instanceof CTypedefType) {
        innerType = ((CTypedefType) innerType).getRealType();
      }
      if (innerType instanceof CFunctionType) {
        if (initializer != null) {
          throw parseContext.parseError("Function definition with initializer", d);
        }

        // Note that this silently ignores const and volatile qualifiers
        // for typedefs of function types, but this is ok because const or volatile functions
        // are undefined behavior anyway (C11 §6.7.3 (9)).

        List<CParameterDeclaration> params;

        CFunctionType functionType = (CFunctionType) innerType;
        if (functionType instanceof CFunctionTypeWithNames) {
          params = ((CFunctionTypeWithNames) functionType).getParameterDeclarations();
        } else {
          params = new ArrayList<>(functionType.getParameters().size());
          int i = 0;
          for (CType paramType : functionType.getParameters()) {
            params.add(new CParameterDeclaration(fileLoc, paramType, "__param" + i++));
          }
        }

        final ImmutableSet<CFunctionDeclaration.FunctionAttribute> attributes;
        if (d instanceof IASTFunctionDeclarator) {
          attributes = getAttributes((IASTFunctionDeclarator) d);
        } else {
          attributes = ImmutableSet.of();
        }

        return new CFunctionDeclaration(fileLoc, functionType, name, params, attributes);
      }

      // now it should be a regular variable declaration

      if (cStorageClass == CStorageClass.EXTERN && initializer != null) {
        throw parseContext.parseError("Extern declarations cannot have initializers", d);
      }
      if (cStorageClass != CStorageClass.EXTERN && innerType instanceof CVoidType) {
        throw parseContext.parseError("Variable cannot have type void", d);
      }

      String origName = name;

      if (cStorageClass == CStorageClass.STATIC) {
        if (!isGlobal) {
          isGlobal = true;
          name = "static__" + ((FunctionScope) scope).getCurrentFunctionName() + "__" + name;
        } else {
          name = staticVariablePrefix + name;
        }
        cStorageClass = CStorageClass.AUTO;
      }

      if (!isGlobal && cStorageClass == CStorageClass.EXTERN) {
        // TODO: implement this, it "imports" the externally declared variable
        // into the scope of this block.
        throw parseContext.parseError("Local variable declared extern is unsupported", d);
      }

      if (!isGlobal && scope.variableNameInUse(name)) {
        String sep = "__";
        int index = 1;
        while (scope.variableNameInUse(name + sep + index)) {
          ++index;
        }
        name = name + sep + index;
      }

      final String scopedName = isGlobal ? name : scope.createScopedNameOf(name);
      CVariableDeclaration declaration =
          new CVariableDeclaration(
              fileLoc, isGlobal, cStorageClass, type, name, origName, scopedName, null);
      scope.registerDeclaration(declaration);

      // Now that we registered the declaration, we can parse the initializer.
      // We cannot do this before, because in the following code, the right "x"
      // actually binds to the left "x"!
      // int x = x;

      declaration.addInitializer(convert(initializer, declaration.getType(), declaration));

      return declaration;

    } else {
      throw new CFAGenerationRuntimeException(
          "Declaration without declarator, but type is unknown: " + type.toASTString(""));
    }
  }

  private List<CCompositeTypeMemberDeclaration> convertDeclarationInCompositeType(
      final IASTDeclaration d, int nofMember) {
    if (d.getParent() instanceof CASTCompositeTypeSpecifier) {
      // FIXME: remove conditional after debugging
    }
    if (d instanceof IASTProblemDeclaration) {
      throw parseContext.parseError((IASTProblemDeclaration) d);
    }

    if (!(d instanceof IASTSimpleDeclaration)) {
      throw parseContext.parseError("unknown declaration type " + d.getClass().getSimpleName(), d);
    }
    IASTSimpleDeclaration sd = (IASTSimpleDeclaration) d;

    Pair<CStorageClass, ? extends CType> specifier = convert(sd.getDeclSpecifier());
    // TODO: add knowledge about sd.DeclSpecifier.alignmentSpecifiers
    if (specifier.getFirst() != CStorageClass.AUTO) {
      throw parseContext.parseError("Unsupported storage class inside composite type", d);
    }
    CType type = specifier.getSecond();

    if (type instanceof CCompositeType) {
      // Nested struct declaration
      CCompositeType compositeType = (CCompositeType) type;
      addSideEffectDeclarationForType(compositeType, getLocation(d));
      type =
          new CElaboratedType(
              compositeType.isConst(),
              compositeType.isVolatile(),
              compositeType.getKind(),
              compositeType.getName(),
              compositeType.getOrigName(),
              compositeType);
    }

    List<CCompositeTypeMemberDeclaration> result;
    IASTDeclarator[] declarators = sd.getDeclarators();
    if (declarators == null || declarators.length == 0) {
      // declaration without declarator, anonymous struct field?
      CCompositeTypeMemberDeclaration newD =
          createDeclarationForCompositeType(type, null, nofMember);
      result = Collections.singletonList(newD);

    } else if (declarators.length == 1) {
      CCompositeTypeMemberDeclaration newD =
          createDeclarationForCompositeType(type, declarators[0], nofMember);
      result = Collections.singletonList(newD);

    } else {
      result = new ArrayList<>(declarators.length);
      for (IASTDeclarator c : declarators) {

        result.add(createDeclarationForCompositeType(type, c, nofMember));
      }
    }

    return result;
  }

  private CCompositeTypeMemberDeclaration createDeclarationForCompositeType(
      CType type, IASTDeclarator d, int nofMember) {
    String name = null;

    if (d != null) {
      Triple<CType, IASTInitializer, String> declarator = convert(d, type);

      if (declarator.getSecond() != null) {
        throw parseContext.parseError("Unsupported initializer inside composite type", d);
      }

      type = declarator.getFirst();
      name = declarator.getThird();
    }

    if (isNullOrEmpty(name)) {
      name = "__anon_type_member_" + nofMember;
    }

    return new CCompositeTypeMemberDeclaration(type, name);
  }

  private Triple<CType, IASTInitializer, String> convert(IASTDeclarator d, CType specifier) {
    while (d != null
        && d.getClass() == CASTDeclarator.class
        && d.getPointerOperators().length == 0
        && d.getAttributes().length == 0
        && d.getAttributeSpecifiers().length == 0
        && d.getInitializer() == null
        && d.getNestedDeclarator() != null) {
      // This is an "empty" declarator with nothing else but the nested declarator.
      // It comes from code like "void ((*(f))(void));"
      // (the outer unnecessary parentheses are represented by this).
      // We just ignore this declarator like we ignore parentheses in expressions.
      d = d.getNestedDeclarator();
    }

    if (d instanceof IASTFunctionDeclarator) {
      // TODO is it always right to assume that here is no static storage class
      return convert((IASTFunctionDeclarator) d, specifier, false);

    } else {
      // First, we handle __attribute__ because this can override the type of the declaration.
      if (d != null) {
        specifier = handleDeclaratorAttributes(d, specifier);
      }

      // Parsing type declarations in C is complex.
      // For example, array modifiers and pointer operators are declared in the
      // "wrong" way:
      // "int (*drives[4])[6]" is "array 4 of pointer to array 6 of int"
      // (The inner most modifiers are the highest-level ones.)
      // So we don't do this recursively, but instead collect all modifiers
      // and apply them after we have reached the inner-most declarator.

      // Collection of all modifiers (outermost modifier is first).
      List<IASTNode> modifiers = new ArrayList<>(1);

      IASTInitializer initializer = null;
      String name = null;
      Integer bitFieldSize = null;

      // Descend into the nested chain of declarators.
      // Find out the name and the initializer, and collect all modifiers.
      IASTDeclarator currentDecl = d;
      while (currentDecl != null) {
        // TODO handle bitfields by checking for instanceof IASTFieldDeclarator

        if (currentDecl instanceof IASTFieldDeclarator) {
          if (bitFieldSize != null) {
            throw parseContext.parseError(
                "Unsupported declaration with two bitfield descriptions", d);
          }

          IASTExpression bitField = ((IASTFieldDeclarator) currentDecl).getBitFieldSize();
          if (bitField instanceof CASTLiteralExpression) {
            CExpression cExpression = convertExpressionWithoutSideEffects(bitField);
            if (cExpression instanceof CIntegerLiteralExpression) {
              bitFieldSize = ((CIntegerLiteralExpression) cExpression).getValue().intValue();
            } else {
              throw parseContext.parseError("Unsupported bitfield specifier", d);
            }
          }
        }

        if (currentDecl instanceof IASTFunctionDeclarator) {
          throw parseContext.parseError("Unsupported declaration nested function declarations", d);
        }

        modifiers.addAll(Arrays.asList(currentDecl.getPointerOperators()));

        if (currentDecl instanceof IASTArrayDeclarator) {
          modifiers.addAll(Arrays.asList(((IASTArrayDeclarator) currentDecl).getArrayModifiers()));
        }

        if (currentDecl.getInitializer() != null) {
          if (initializer != null) {
            throw parseContext.parseError("Unsupported declaration with two initializers", d);
          }
          // xxx
          initializer = currentDecl.getInitializer();
        }

        if (!currentDecl.getName().toString().isEmpty()) {
          if (name != null) {
            throw parseContext.parseError("Unsupported declaration with two names", d);
          }
          name = convert(currentDecl.getName());
        }

        currentDecl = currentDecl.getNestedDeclarator();
      }

      name =
          Strings.nullToEmpty(
              name); // there may be no name at all, for example in parameter declarations

      // Add the modifiers to the type.
      CType type = specifier;
      // array modifiers have to be added backwards, otherwise the arraysize is wrong
      // with multidimensional arrays
      List<IASTArrayModifier> tmpArrMod = new ArrayList<>();
      for (IASTNode modifier : modifiers) {
        if (modifier instanceof IASTArrayModifier) {
          tmpArrMod.add((IASTArrayModifier) modifier);
        } else if (modifier instanceof IASTPointerOperator) {
          // add accumulated array modifiers before adding next pointer operator
          for (int i = tmpArrMod.size() - 1; i >= 0; i--) {
            type = convert(tmpArrMod.get(i), type);
          }
          // clear added modifiers
          tmpArrMod.clear();

          type = typeConverter.convert((IASTPointerOperator) modifier, type);

        } else {
          throw new AssertionError();
        }
      }

      // add last array modifiers if necessary
      for (int i = tmpArrMod.size() - 1; i >= 0; i--) {
        type = convert(tmpArrMod.get(i), type);
      }

      // Arrays with unknown length but an initializer
      // have their length calculated from the initializer.
      // Example: int a[] = { 1, 2 };
      // will be converted as int a[2] = { 1, 2 };
      if (type instanceof CArrayType) {
        CArrayType arrayType = (CArrayType) type;

        if (arrayType.getLength() == null && initializer instanceof IASTEqualsInitializer) {
          IASTInitializerClause initClause =
              ((IASTEqualsInitializer) initializer).getInitializerClause();
          if (initClause instanceof IASTInitializerList) {
            @Nullable BigInteger length = BigInteger.ZERO;
            BigInteger position = BigInteger.ZERO;
            for (IASTInitializerClause x : ((IASTInitializerList) initClause).getClauses()) {
              if (length == null) {
                break;
              }

              if (x instanceof ICASTDesignatedInitializer) {
                for (ICASTDesignator designator :
                    ((ICASTDesignatedInitializer) x).getDesignators()) {
                  if (designator instanceof CASTArrayRangeDesignator) {
                    BigInteger c =
                        evaluateIntegerConstantExpression(
                            ((CASTArrayRangeDesignator) designator).getRangeCeiling());
                    position = c.add(BigInteger.ONE);
                    length = Comparators.max(length, position);

                  } else if (designator instanceof CASTArrayDesignator) {
                    BigInteger s =
                        evaluateIntegerConstantExpression(
                            ((CASTArrayDesignator) designator).getSubscriptExpression());
                    position = s.add(BigInteger.ONE);
                    length = Comparators.max(length, position);

                    // we only know the length of the CASTArrayDesignator and the
                    // CASTArrayRangeDesignator, all other designators
                    // have to be ignore, if one occurs, we cannot calculate the length of the array
                    // correctly
                  } else {
                    length = null;
                    break;
                  }
                }
              } else {
                position = position.add(BigInteger.ONE);
                length = Comparators.max(position, length);
              }
            }

            // only adjust the length of the array if we definitely know it
            if (length != null) {
              CExpression lengthExp =
                  new CIntegerLiteralExpression(
                      getLocation(initializer), CNumericTypes.INT, length);

              type =
                  new CArrayType(
                      arrayType.isConst(), arrayType.isVolatile(), arrayType.getType(), lengthExp);
            }
          } else {
            // Arrays with unknown length but an string initializer
            // have their length calculated from the initializer.
            // Example: char a[] = "abc";
            // will be converted as char a[4] = "abc";
            if (initClause instanceof CASTLiteralExpression
                && (arrayType.getType().equals(CNumericTypes.CHAR)
                    || arrayType.getType().equals(CNumericTypes.SIGNED_CHAR)
                    || arrayType.getType().equals(CNumericTypes.UNSIGNED_CHAR))) {
              CASTLiteralExpression literalExpression = (CASTLiteralExpression) initClause;
              int length = literalExpression.getLength() - 1;
              CExpression lengthExp =
                  new CIntegerLiteralExpression(
                      getLocation(initializer), CNumericTypes.INT, BigInteger.valueOf(length));

              type =
                  new CArrayType(
                      arrayType.isConst(), arrayType.isVolatile(), arrayType.getType(), lengthExp);
            }
          }
        }
      }

      if (bitFieldSize != null) {
        type = typeConverter.convertBitFieldType(bitFieldSize, type);
      }
      return Triple.of(type, initializer, name);
    }
  }

  /**
   * Handle <code>__attribute__</code> attached to a declaration. Documentation:
   * https://gcc.gnu.org/onlinedocs/gcc/Common-Variable-Attributes.html
   *
   * @param d The declarator of the declaration.
   * @param type The specified type of the declaration.
   * @return The actual type of the declaration (may be changed due to attributes).
   */
  private CType handleDeclaratorAttributes(IASTDeclarator d, CType type) {
    for (IASTAttribute attribute : d.getAttributes()) {
      String name = getAttributeString(attribute.getName());
      if (name.equals("mode")) {
        type = type.getCanonicalType();
        if (!(type instanceof CSimpleType)) {
          throw parseContext.parseError("Mode attribute unsupported for type " + type, d);
        }
        String mode = getAttributeString(attribute.getArgumentClause().getTokenCharImage());
        type = handleModeAttribute((CSimpleType) type, mode, d);
      }
    }
    return type;
  }

  /** Return normalized string for a name etc. in an attribute context. */
  private static String getAttributeString(char[] chars) {
    String s = String.valueOf(chars);
    if (s.startsWith("__") && s.endsWith("__")) {
      // For attribute names and parameters, foo may also be written as __foo__.
      // Cf. https://gcc.gnu.org/onlinedocs/gcc/Attribute-Syntax.html
      return s.substring(2, s.length() - 2);
    }
    return s;
  }

  /**
   * Parses known GNU C attributes of IASTFunctionDeclarator. If any unknown attribute occurs, an
   * exception is thrown.
   */
  private ImmutableSet<CFunctionDeclaration.FunctionAttribute> getAttributes(
      IASTFunctionDeclarator d) {
    EnumSet<FunctionAttribute> attributes = EnumSet.noneOf(FunctionAttribute.class);
    for (IASTAttribute attribute : d.getAttributes()) {
      String name = getAttributeString(attribute.getName());
      if (!KNOWN_FUNCTION_ATTRIBUTES.containsKey(name)) {
        throw new CFAGenerationRuntimeException(
            "Unrecognized attribute in declaration of " + d.getName() + ": " + name);
      }
      KNOWN_FUNCTION_ATTRIBUTES.get(name).ifPresent(attributes::add);
    }
    String functionName = convert(d.getName());
    if (options.isNonReturningFunction(functionName)) {
      attributes.add(FunctionAttribute.NO_RETURN);
    }
    return Sets.immutableEnumSet(attributes);
  }

  /**
   * Handle <code>__attribute__(mode(...))</code> for declarations, which changes the type of the
   * declaration: In code like <code>
   * typedef unsigned int u_int8_t __attribute__ ((__mode__ (__QI__)));</code> the type is not
   * <code>unsigned int</code>, but actually an (unsigned) "quarter integer", cf. "mode" in the list
   * on https://gcc.gnu.org/onlinedocs/gcc/Common-Variable-Attributes.html Modes are documented at
   * https://gcc.gnu.org/onlinedocs/gccint/Machine-Modes.html
   *
   * <p>So far this handles only a minimal set of cases (a subset of modes and type needs to be
   * int), but only these seem common in headers.
   *
   * @param type The given type of the declaration.
   * @param mode The argument of <code>mode(...)</code>.
   * @return The actual type of the declaration.
   */
  private CSimpleType handleModeAttribute(CSimpleType type, String mode, IASTNode context) {
    if (type.getType() != CBasicType.INT || type.isComplex() || type.isImaginary()) {
      throw parseContext.parseError("Mode attribute unsupported for type " + type, context);
    }

    CSimpleType newType;
    switch (mode) {
      case "word": // assume that pointers have word size, which is the case on our platforms
        newType = machinemodel.getPointerEquivalentSimpleType();
        break;
      case "byte":
      case "QI": // quarter integer
        newType = CNumericTypes.CHAR;
        break;
      case "HI": // half integer
        assert machinemodel.getSizeofShort() == 2; // not guaranteed by C, but on our platforms
        newType = CNumericTypes.SHORT_INT;
        break;
      case "SI": // single integer
        assert machinemodel.getSizeofInt() == 4; // not guaranteed by C, but on our platforms
        newType = CNumericTypes.INT;
        break;
      case "DI": // double integer
        if (machinemodel.getSizeofLongInt() == 8) {
          newType = CNumericTypes.LONG_INT;
        } else if (machinemodel.getSizeofLongLongInt() == 8) {
          newType = CNumericTypes.LONG_LONG_INT;
        } else {
          // could occur, but not on our platforms
          throw new AssertionError("unexpected machine model");
        }
        break;
      default:
        throw parseContext.parseError("Unsupported mode " + mode, context);
    }

    // Copy const, volatile, and signedness from original type, rest from newType
    return new CSimpleType(
        type.isConst(),
        type.isVolatile(),
        newType.getType(),
        newType.isLong(),
        newType.isShort(),
        type.isSigned(),
        type.isUnsigned(),
        false, // checked above
        false, // checked above
        newType.isLongLong());
  }

  private CType convert(IASTArrayModifier am, CType type) {
    if (am instanceof ICASTArrayModifier) {
      ICASTArrayModifier a = (ICASTArrayModifier) am;
      CExpression lengthExp = convertExpressionWithoutSideEffects(a.getConstantExpression());
      if (lengthExp != null) {
        lengthExp = simplifyExpressionRecursively(lengthExp);
      }
      return new CArrayType(a.isConst(), a.isVolatile(), type, lengthExp);

    } else {
      throw parseContext.parseError("Unknown array modifier", am);
    }
  }

  private Triple<CType, IASTInitializer, String> convert(
      IASTFunctionDeclarator d, CType returnType, boolean isStaticFunction) {

    if (!(d instanceof IASTStandardFunctionDeclarator)) {
      throw parseContext.parseError("Unknown non-standard function definition", d);
    }
    IASTStandardFunctionDeclarator sd = (IASTStandardFunctionDeclarator) d;

    // handle return type
    returnType = typeConverter.convertPointerOperators(d.getPointerOperators(), returnType);
    if (returnType instanceof CSimpleType) {
      CSimpleType t = (CSimpleType) returnType;
      if (t.getType() == CBasicType.UNSPECIFIED) {
        // type of functions is implicitly int it not specified
        returnType =
            new CSimpleType(
                t.isConst(),
                t.isVolatile(),
                CBasicType.INT,
                t.isLong(),
                t.isShort(),
                t.isSigned(),
                t.isUnsigned(),
                t.isComplex(),
                t.isImaginary(),
                t.isLongLong());
      }
    }

    // handle parameters
    List<CParameterDeclaration> paramsList = convert(sd.getParameters());

    CFunctionTypeWithNames fType =
        new CFunctionTypeWithNames(returnType, paramsList, sd.takesVarArgs());
    CType type = fType;

    String origname;
    if (d.getNestedDeclarator() != null) {

      Triple<? extends CType, IASTInitializer, String> nestedDeclarator =
          convert(d.getNestedDeclarator(), type);

      assert d.getName().getRawSignature().isEmpty() : d;
      assert nestedDeclarator.getSecond() == null;

      type = nestedDeclarator.getFirst();
      origname = nestedDeclarator.getThird();

    } else {
      origname = convert(d.getName());
    }

    String qualifiedName = origname;
    if (isStaticFunction) {
      qualifiedName = staticVariablePrefix + origname;
    }

    fType.setName(qualifiedName);
    for (CParameterDeclaration param : paramsList) {
      param.setQualifiedName(FunctionScope.createQualifiedName(qualifiedName, param.getName()));
    }

    return Triple.of(type, d.getInitializer(), origname);
  }

  private Pair<CStorageClass, ? extends CType> convert(IASTDeclSpecifier d) {
    CStorageClass sc = typeConverter.convertCStorageClass(d);

    if (d instanceof IASTCompositeTypeSpecifier) {
      return Pair.of(sc, convert((IASTCompositeTypeSpecifier) d));

    } else if (d instanceof IASTElaboratedTypeSpecifier) {
      return Pair.of(sc, typeConverter.convert((IASTElaboratedTypeSpecifier) d));

    } else if (d instanceof IASTEnumerationSpecifier) {
      return Pair.of(sc, convert((IASTEnumerationSpecifier) d));

    } else if (d instanceof IASTNamedTypeSpecifier) {
      return Pair.of(sc, typeConverter.convert((IASTNamedTypeSpecifier) d));

    } else if (d instanceof IASTSimpleDeclSpecifier) {
      return Pair.of(sc, typeConverter.convert((IASTSimpleDeclSpecifier) d));

    } else {
      throw parseContext.parseError("unknown declSpecifier", d);
    }
  }

  private CCompositeType convert(IASTCompositeTypeSpecifier d) {
    List<CCompositeTypeMemberDeclaration> list = new ArrayList<>(d.getMembers().length);

    int nofMember = 0;
    for (IASTDeclaration c : d.getMembers()) {
      List<CCompositeTypeMemberDeclaration> newCs = convertDeclarationInCompositeType(c, nofMember);
      nofMember++;
      assert !newCs.isEmpty();
      list.addAll(newCs);
    }

    ComplexTypeKind kind;
    switch (d.getKey()) {
      case IASTCompositeTypeSpecifier.k_struct:
        kind = ComplexTypeKind.STRUCT;
        break;
      case IASTCompositeTypeSpecifier.k_union:
        kind = ComplexTypeKind.UNION;
        break;
      default:
        throw parseContext.parseError("Unknown key " + d.getKey() + " for composite type", d);
    }

    String name = convert(d.getName());
    String origName = name;
    if (name.isEmpty()) {
      name = "__anon_type_";
      if (d.getStorageClass() == IASTDeclSpecifier.sc_typedef) {
        name +=
            ((IASTSimpleDeclaration) d.getParent()).getDeclarators()[0].getName().getRawSignature();
      } else {
        name += anonTypeCounter++;
      }
    }

    for (Iterator<CCompositeTypeMemberDeclaration> it = list.iterator(); it.hasNext(); ) {
      CCompositeTypeMemberDeclaration member = it.next();
      if (member.getType().isIncomplete()) {
        if (kind != ComplexTypeKind.STRUCT) {
          parseContext.parseError("Member " + member + " has incomplete type in union " + name, d);
        }
        if (it.hasNext()) {
          parseContext.parseError(
              "Member "
                  + member
                  + " in non-last position of struct "
                  + name
                  + " has incomplete type",
              d);
        }
        if (!(member.getType().getCanonicalType() instanceof CArrayType)) {
          parseContext.parseError(
              "Member " + member + " of struct " + name + " has incomplete non-array type", d);
        }
      }
    }
    CCompositeType compositeType =
        new CCompositeType(d.isConst(), d.isVolatile(), kind, list, name, origName);

    // in cases like struct s { (struct s)* f }
    // we need to fill in the binding from the inner "struct s" type to the outer
    compositeType.accept(
        new FillInBindingVisitor(kind, scope.getFileSpecificTypeName(name), compositeType));
    return compositeType;
  }

  private CEnumType convert(IASTEnumerationSpecifier d) {
    List<CEnumerator> list = new ArrayList<>(d.getEnumerators().length);
    Long lastValue = -1L; // initialize with -1, so the first one gets value 0
    for (IASTEnumerationSpecifier.IASTEnumerator c : d.getEnumerators()) {
      CEnumerator newC = convert(c, lastValue);
      list.add(newC);
      if (newC.hasValue()) {
        lastValue = newC.getValue();
      } else {
        lastValue = null;
      }
    }

    String name = convert(d.getName());
    String origName = name;

    // when the enum has no name we create one
    // (this may be the case when the enum declaration is surrounded by a typedef)
    if (name.isEmpty()) {
      name = "__anon_type_" + anonTypeCounter++;
    }

    CEnumType enumType = new CEnumType(d.isConst(), d.isVolatile(), list, name, origName);
    CSimpleType integerType = getEnumerationType(enumType);
    for (CEnumerator enumValue : enumType.getEnumerators()) {
      enumValue.setEnum(enumType);
      enumValue.setType(integerType);
    }
    return enumType;
  }

  private static final ImmutableList<CSimpleType> ENUM_REPRESENTATION_CANDIDATE_TYPES =
      ImmutableList.of( // list of types with incrementing size
          CNumericTypes.SIGNED_INT, CNumericTypes.UNSIGNED_INT, CNumericTypes.SIGNED_LONG_LONG_INT);

  /**
   * Compute a matching integer type for an enumeration. We use SIGNED_INT and switch to larger type
   * if needed.
   *
   * <p>§6.7.2.2 (4) Each enumerated type shall be compatible with char, a signed integer type, or
   * an unsigned integer type. The choice of type is implementation-defined, but shall be capable of
   * representing the values of all the members of the enumeration.
   */
  private CSimpleType getEnumerationType(final CEnumType enumType) {
    LongSummaryStatistics enumStatistics =
        enumType.getEnumerators().stream()
            .filter(CEnumerator::hasValue) // some values might not have been simplified
            .mapToLong(CEnumerator::getValue)
            .summaryStatistics();

    Preconditions.checkState(
        enumStatistics.getCount() > 0, "enumeration does not provide any values: %s", enumType);
    final BigInteger minValue = BigInteger.valueOf(enumStatistics.getMin());
    final BigInteger maxValue = BigInteger.valueOf(enumStatistics.getMax());
    for (CSimpleType integerType : ENUM_REPRESENTATION_CANDIDATE_TYPES) {
      if (minValue.compareTo(machinemodel.getMinimalIntegerValue(integerType)) >= 0
          && maxValue.compareTo(machinemodel.getMaximalIntegerValue(integerType)) <= 0) {
        // if all enumeration values are matching into the range, we use it
        return integerType;
      }
    }
    // if nothing works, use the largest type we have: ULL
    return CNumericTypes.UNSIGNED_LONG_LONG_INT;
  }

  private CEnumerator convert(IASTEnumerationSpecifier.IASTEnumerator e, Long lastValue) {
    Long value = null;

    if (e.getValue() == null && lastValue != null) {
      value = lastValue + 1;
    } else {
      // TODO Because we fully evaluate the expression here and never add e.getValue() itself
      // to the AST, any overflows in it will not be detectable by the analysis.
      value = evaluateIntegerConstantExpression(e.getValue()).longValueExact();
    }

    String name = convert(e.getName());
    CEnumerator result =
        new CEnumerator(
            getLocation(e),
            name,
            scope.createScopedNameOf(name),
            /* dummy integer type, the correct one will be set directly afterwards */
            CNumericTypes.SIGNED_INT,
            value);
    scope.registerDeclaration(result);
    return result;
  }

  private IASTExpression toExpression(IASTInitializerClause i) {
    if (i instanceof IASTExpression) {
      return (IASTExpression) i;
    }
    throw parseContext.parseError("Initializer clause in unexpected location", i);
  }

  private CInitializer convert(
      IASTInitializerClause i, CType type, @Nullable CVariableDeclaration declaration) {
    if (i instanceof IASTExpression) {
      CExpression exp = convertExpressionWithoutSideEffects((IASTExpression) i);
      return new CInitializerExpression(exp.getFileLocation(), exp);
    } else if (i instanceof IASTInitializerList) {
      return convert((IASTInitializerList) i, type, declaration);
    } else if (i instanceof ICASTDesignatedInitializer) {
      return convert((ICASTDesignatedInitializer) i, type, declaration);
    } else {
      throw parseContext.parseError(
          "unknown initializer claus: " + i.getClass().getSimpleName(), i);
    }
  }

  private CInitializer convert(
      IASTInitializer i, CType type, @Nullable CVariableDeclaration declaration) {
    if (i == null) {
      return null;

    } else if (i instanceof IASTInitializerList) {
      return convert((IASTInitializerList) i, type, declaration);
    } else if (i instanceof IASTEqualsInitializer) {
      return convert((IASTEqualsInitializer) i, type, declaration);
    } else if (i instanceof ICASTDesignatedInitializer) {
      return convert((ICASTDesignatedInitializer) i, type, declaration);
    } else {
      throw parseContext.parseError("unknown initializer: " + i.getClass().getSimpleName(), i);
    }
  }

  private CInitializer convert(
      ICASTDesignatedInitializer init, CType type, @Nullable CVariableDeclaration declaration) {
    ICASTDesignator[] desInit = init.getDesignators();

    CInitializer cInit = convert(init.getOperand(), type, declaration);

    FileLocation fileLoc = cInit.getFileLocation();

    List<CDesignator> designators = new ArrayList<>(desInit.length);

    // convert all designators
    for (ICASTDesignator designator : desInit) {
      CDesignator r;
      if (designator instanceof ICASTFieldDesignator) {
        r = new CFieldDesignator(fileLoc, convert(((ICASTFieldDesignator) designator).getName()));

      } else if (designator instanceof ICASTArrayDesignator) {
        r =
            new CArrayDesignator(
                fileLoc,
                convertExpressionWithoutSideEffects(
                    ((ICASTArrayDesignator) designator).getSubscriptExpression()));

      } else if (designator instanceof IGCCASTArrayRangeDesignator) {
        r =
            new CArrayRangeDesignator(
                fileLoc,
                convertExpressionWithoutSideEffects(
                    ((IGCCASTArrayRangeDesignator) designator).getRangeFloor()),
                convertExpressionWithoutSideEffects(
                    ((IGCCASTArrayRangeDesignator) designator).getRangeCeiling()));

      } else {
        throw parseContext.parseError("Unsupported Designator", designator);
      }
      designators.add(r);
    }

    return new CDesignatedInitializer(fileLoc, designators, cInit);
  }

  private CInitializer convert(
      IASTInitializerList iList, CType type, @Nullable CVariableDeclaration declaration) {

    List<CInitializer> initializerList = new ArrayList<>();

    if (declaration != null && iList.getSize() == 1) {
      if (type instanceof CSimpleType || type instanceof CPointerType) {
        IASTInitializerClause result = unpackBracedInitializer(iList);
        if (result != null) {
          return convert(result, type, declaration);
        }
      }
    }

    // TODO: we might need do to something similar for more types
    if (type instanceof CArrayType) {
      type = ((CArrayType) type).getType();
    }

    for (IASTInitializerClause i : iList.getClauses()) {
      CInitializer newI = convert(i, type, declaration);
      if (newI != null) {
        initializerList.add(newI);
      }
    }

    return new CInitializerList(getLocation(iList), initializerList);
  }

  private @Nullable IASTInitializerClause unpackBracedInitializer(IASTInitializerList pIList) {
    if (pIList.getSize() == 1) {
      IASTInitializerClause clause = pIList.getClauses()[0];
      if (clause instanceof IASTInitializerList) {
        return unpackBracedInitializer((IASTInitializerList) clause);
      }
      return clause;
    }
    return null;
  }

  private CInitializer convert(
      IASTEqualsInitializer i, CType type, @Nullable CVariableDeclaration declaration) {
    IASTInitializerClause ic = i.getInitializerClause();
    if (ic instanceof IASTExpression) {
      IASTExpression e = (IASTExpression) ic;

      CAstNode initializer = convertExpressionWithSideEffects(e);
      if (initializer == null) {
        return null;
      }

      final CInitializerExpression result;
      if (initializer instanceof CAssignment) {
        sideAssignmentStack.addPreSideAssignment(initializer);
        result =
            new CInitializerExpression(
                getLocation(e), ((CAssignment) initializer).getLeftHandSide());

      } else if (initializer instanceof CFunctionCallExpression) {
        FileLocation loc = getLocation(i);

        if (declaration != null && !declaration.getType().getCanonicalType().isConst()) {
          // This is a variable declaration like "int i = f();"
          // We can replace this with "int i; i = f();"
          CIdExpression var = new CIdExpression(loc, declaration);
          sideAssignmentStack.addPostSideAssignment(
              new CFunctionCallAssignmentStatement(
                  loc, var, (CFunctionCallExpression) initializer));
          return null; // empty initializer

        } else {
          // This is something more complicated, like a function call inside an array initializer.
          // We need a temporary variable.

          CIdExpression var = createTemporaryVariable(e);
          sideAssignmentStack.addPreSideAssignment(
              new CFunctionCallAssignmentStatement(
                  loc, var, (CFunctionCallExpression) initializer));
          result = new CInitializerExpression(loc, var);
        }

      } else if (initializer instanceof CExpression) {
        result = new CInitializerExpression(getLocation(ic), (CExpression) initializer);

      } else {
        throw parseContext.parseError(
            "Initializer is not free of side-effects, it is a "
                + initializer.getClass().getSimpleName(),
            e);
      }

      if (!areInitializerAssignable(type, result.getExpression())) {
        if (type.getCanonicalType() instanceof CPointerType
            && CTypes.isIntegerType(result.getExpression().getExpressionType())) {
          if (declaration != null) {
            logger.logf(
                Level.WARNING,
                "%s: Initialization of pointer variable %s with integer expression %s.",
                result.getFileLocation(),
                type.toASTString(declaration.getName()),
                result);
          }
        } else {
          throw parseContext.parseError(
              "Type "
                  + type
                  + " of declaration and type "
                  + ((CExpression) initializer).getExpressionType()
                  + " of initializer are not assignment compatible",
              e);
        }
      }

      return result;

    } else if (ic instanceof IASTInitializerList) {
      return convert((IASTInitializerList) ic, type, declaration);
    } else {
      throw parseContext.parseError("unknown initializer: " + i.getClass().getSimpleName(), i);
    }
  }

  /** Check for legal initializer according to C11 § 6.7.9 (11), (13) */
  private boolean areInitializerAssignable(
      CType pDeclarationType, CExpression pInitializerExpression) {
    return pDeclarationType.canBeAssignedFrom(pInitializerExpression.getExpressionType())
        || isStringInitialization(pDeclarationType, pInitializerExpression)
        || ((pInitializerExpression instanceof CIntegerLiteralExpression)
            // the literal '0' is by default treated as a Null-Pointer in context
            // of pointers (C-Standard 11 §6.3.2.3 (3))
            && (((CIntegerLiteralExpression) pInitializerExpression).getValue().intValue() == 0)
            && pDeclarationType.canBeAssignedFrom(CPointerType.POINTER_TO_VOID));
  }

  /**
   * This method determines whether the initialization is valid in accordance to C11 Standard,
   * §6.7.9, (32).
   *
   * @param pDeclarationType the type of the lhs of the initialization
   * @param pInitializerExpression the rhs expression of the initialization
   * @return if the initialization assigns a {@link CStringLiteralExpression} to an array or pointer
   *     of/to <b><code>char</code></b>
   */
  private boolean isStringInitialization(
      CType pDeclarationType, CExpression pInitializerExpression) {
    // TODO: Can we somehow handle wide-char-arrays and corresponding Strings?
    // E.g., 'wchar_t a[] = L"abc";' is valid, whereas 'char a[] = L"abc";' is not.
    if (pInitializerExpression instanceof CStringLiteralExpression) {

      // Not null default that results in false without an exception if the declaration type is
      // neither array nor pointer and allows to drop a redundant second check for CArrayType or
      // CPointerType
      CType canonicalType = CPointerType.POINTER_TO_VOID;
      if (pDeclarationType instanceof CArrayType) {
        canonicalType = ((CArrayType) pDeclarationType).getType().getCanonicalType();
      }
      if (pDeclarationType instanceof CPointerType) {
        canonicalType = ((CPointerType) pDeclarationType).getType().getCanonicalType();
      }
      return CTypes.copyDequalified(canonicalType).equals(CNumericTypes.CHAR);
    }
    return false;
  }

  private List<CParameterDeclaration> convert(IASTParameterDeclaration[] ps) {
    List<CParameterDeclaration> paramsList = new ArrayList<>(ps.length);
    for (IASTParameterDeclaration c : ps) {
      if (!c.getRawSignature().equals("void")) {
        paramsList.add(convert(c));
      } else {
        // there may be a function declaration f(void), which is equal to f()
        // we don't want this dummy parameter "void"
        assert ps.length == 1;
      }
    }
    return paramsList;
  }

  private CParameterDeclaration convert(IASTParameterDeclaration p) {
    Pair<CStorageClass, ? extends CType> specifier = convert(p.getDeclSpecifier());
    if (specifier.getFirst() != CStorageClass.AUTO) {
      throw parseContext.parseError("Unsupported storage class for parameters", p);
    }

    Triple<CType, IASTInitializer, String> declarator =
        convert(p.getDeclarator(), specifier.getSecond());

    if (declarator.getSecond() != null) {
      throw parseContext.parseError("Unsupported initializer for parameters", p);
    }

    CType type = declarator.getFirst();
    if (type instanceof CFunctionTypeWithNames) {
      CFunctionTypeWithNames functionType = (CFunctionTypeWithNames) type;
      type = new CPointerType(false, false, functionType);
    }

    return new CParameterDeclaration(getLocation(p), type, declarator.getThird());
  }

  /** This function returns the converted file-location of an IASTNode. */
  FileLocation getLocation(final IASTNode n) {
    return parseContext.getLocation(n);
  }

  static String convert(IASTName n) {
    return n.toString(); // TODO verify toString() is the correct method
  }

  CType convert(IASTTypeId t) {
    Pair<CStorageClass, ? extends CType> specifier = convert(t.getDeclSpecifier());
    if (specifier.getFirst() != CStorageClass.AUTO) {
      throw parseContext.parseError("Unsupported storage class for type ids", t);
    }

    Triple<CType, IASTInitializer, String> declarator =
        convert(t.getAbstractDeclarator(), specifier.getSecond());

    if (declarator.getSecond() != null) {
      throw parseContext.parseError("Unsupported initializer for type ids", t);
    }
    if (declarator.getThird() != null && !declarator.getThird().trim().isEmpty()) {
      throw parseContext.parseError("Unsupported name for type ids", t);
    }

    return declarator.getFirst();
  }
}
