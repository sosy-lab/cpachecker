/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.predicates.ctoformulahelper.CtoFormulaTypeUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.IAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.ForwardingCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.ctoformulahelper.CFieldTrackType;
import org.sosy_lab.cpachecker.util.predicates.ctoformulahelper.CtoFormulaTypeUtils;
import org.sosy_lab.cpachecker.util.predicates.ctoformulahelper.CtoFormulaTypeUtils.CtoFormulaSizeofVisitor;
import org.sosy_lab.cpachecker.util.predicates.ctoformulahelper.TooComplexVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.RationalFormulaManagerView;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Class containing all the code that converts C code into a formula.
 */
@Options(prefix="cpa.predicate")
public class CtoFormulaConverter {

  @Option(description="add special information to formulas about non-deterministic functions")
  protected boolean useNondetFlags = false;

  @Option(description="initialize all variables to 0 when they are declared")
  private boolean initAllVars = false;

  // if true, handle lvalues as *x, &x, s.x, etc. using UIFs. If false, just
  // use variables
  @Option(name="lvalsAsUIFs",
      description="use uninterpreted functions for *, & and array access")
  private boolean lvalsAsUif = false;

  @Option(description="list of functions that should be considered as giving "
    + "a non-deterministic return value\n Only predicate analysis honors this option. "
    + "If you specify this option, the default values are not added automatically "
    + "to the list, so you need to specify them explicitly if you need them. "
    + "Mentioning a function in this list has only an effect, if it is an "
    + "'external function', i.e., no source is given in the code for this function.")
  private Set<String> nondetFunctions = ImmutableSet.of(
      "malloc", "__kmalloc", "kzalloc",
      "sscanf",
      "random");

  @Option(description="Regexp pattern for functions that should be considered as giving "
    + "a non-deterministic return value (c.f. cpa.predicate.nondedFunctions)")
  private String nondetFunctionsRegexp = "^(__VERIFIER_)?nondet_[a-z]*";
  private final Pattern nondetFunctionsPattern;

  @Option(description = "Handle aliasing of pointers. "
        + "This adds disjunctions to the formulas, so be careful when using cartesian abstraction.")
  private boolean handlePointerAliasing = true;

  @Option(description = "list of functions that provide new memory on the heap."
    + " This is only used, when handling of pointers is enabled.")
  private Set<String> memoryAllocationFunctions = ImmutableSet.of(
      "malloc", "__kmalloc", "kzalloc"
      );

  private static final String ASSUME_FUNCTION_NAME = "__VERIFIER_assume";

  // list of functions that are pure (no side-effects)
  private static final Set<String> PURE_EXTERNAL_FUNCTIONS
      = ImmutableSet.of("__assert_fail", "free", "kfree",
          "fprintf", "printf", "puts", "printk", "sprintf", "swprintf",
          "strcasecmp", "strchr", "strcmp", "strlen", "strncmp", "strrchr", "strstr"
          );

  // set of functions that may not appear in the source code
  // the value of the map entry is the explanation for the user
  private static final Map<String, String> UNSUPPORTED_FUNCTIONS
      = ImmutableMap.of("pthread_create", "threads");

  private static Predicate<String> startsWith(final String pPrefix) {
    return new Predicate<String>() {
        @Override
        public boolean apply(String pVariable) {
          return pVariable.startsWith(pPrefix);
        }
      };
  }

  //names for special variables needed to deal with functions
  private static final String VAR_RETURN_NAME = "__retval__";
  private static final String OP_ADDRESSOF_NAME = "__ptrAmp__";
  private static final String OP_STAR_NAME = "__ptrStar__";
  private static final String OP_ARRAY_SUBSCRIPT = "__array__";
  static final String NONDET_VARIABLE = "__nondet__";
  static final String EXPAND_VARIABLE = "__expandVariable__";
  static final String NONDET_FLAG_VARIABLE = NONDET_VARIABLE + "flag__";
  private static final CType NONDET_TYPE = CNumericTypes.INT;
  final FormulaType<?> NONDET_FORMULA_TYPE;

  private static final String POINTER_VARIABLE = "__content_of__";
  static final Predicate<String> IS_POINTER_VARIABLE = startsWith(POINTER_VARIABLE);

  private static final String FIELD_VARIABLE = "__field_of__";
  static final Predicate<String> IS_FIELD_VARIABLE = startsWith(FIELD_VARIABLE);

  /** The prefix used for variables representing memory locations. */
  private static final String MEMORY_ADDRESS_VARIABLE_PREFIX = "__address_of__";
  private static final Predicate<String> IS_MEMORY_ADDRESS_VARIABLE = startsWith(MEMORY_ADDRESS_VARIABLE_PREFIX);

  /**
   * The prefix used for memory locations derived from malloc calls.
   * (Must start with {@link #MEMORY_ADDRESS_VARIABLE_PREFIX}.)
   */
  private static final String MALLOC_VARIABLE_PREFIX =
      MEMORY_ADDRESS_VARIABLE_PREFIX + "#";

  /** The variable name that's used to store the malloc counter in the SSAMap. */
  private static final String MALLOC_COUNTER_VARIABLE_NAME = "#malloc";

  private static final Set<String> SAFE_VAR_ARG_FUNCTIONS = ImmutableSet.of(
      "printf", "printk"
      );

  @Option(description = "Handle field access via extract and concat instead of new variables.")
  private boolean handleFieldAccess = false;

  @Option(description = "Handle field aliasing formulas.")
  private boolean handleFieldAliasing = false;

  @Option(description = "Handle field aliasing formulas.")
  private boolean omitNonPointerInFieldAliasing = true;

  private final Set<String> printedWarnings = new HashSet<>();

  private final Map<String, BitvectorFormula> stringLitToFormula = new HashMap<>();
  private int nextStringLitIndex = 0;

  private final MachineModel machineModel;
  private final CtoFormulaSizeofVisitor sizeofVisitor;

  protected final FormulaManagerView fmgr;
  protected final  BooleanFormulaManagerView bfmgr;
  protected final  RationalFormulaManagerView nfmgr;
  protected final  BitvectorFormulaManagerView efmgr;
  protected final  FunctionFormulaManagerView ffmgr;
  protected final LogManager logger;

  private static final int                 VARIABLE_UNSET          = -1;
  private static final int                 VARIABLE_UNINITIALIZED  = 2;

  private final TooComplexVisitor tooComplexVisitor;

  public CtoFormulaConverter(Configuration config, FormulaManagerView fmgr,
      MachineModel pMachineModel, LogManager logger)
          throws InvalidConfigurationException {
    config.inject(this, CtoFormulaConverter.class);

    if (handleFieldAliasing && !handleFieldAccess) {
      throw new InvalidConfigurationException("Enabling field-aliasing when field-access is disabled is unsupported!");
    }

    this.fmgr = fmgr;
    this.machineModel = pMachineModel;
    this.sizeofVisitor = new CtoFormulaSizeofVisitor(pMachineModel);
    this.tooComplexVisitor = new TooComplexVisitor(handleFieldAccess);

    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.nfmgr = fmgr.getRationalFormulaManager();
    this.efmgr = fmgr.getBitvectorFormulaManager();
    this.ffmgr = fmgr.getFunctionFormulaManager();
    this.logger = logger;
    nondetFunctionsPattern = Pattern.compile(nondetFunctionsRegexp);
    NONDET_FORMULA_TYPE = getFormulaTypeFromCType(NONDET_TYPE);
  }

  private void warnUnsafeVar(CExpression exp) {
    logDebug("Unhandled expression treated as free variable", exp);
  }

  private String getLogMessage(String msg, CAstNode astNode) {
    return "Line " + astNode.getFileLocation().getStartingLineNumber()
            + ": " + msg
            + ": " + astNode.toASTString();
  }

  private String getLogMessage(String msg, CFAEdge edge) {
    return "Line " + edge.getLineNumber()
            + ": " + msg
            + ": " + edge.getDescription();
  }

  private void logDebug(String msg, CAstNode astNode) {
    if (logger.wouldBeLogged(Level.ALL)) {
      logger.log(Level.ALL, getLogMessage(msg, astNode));
    }
  }

  private void logDebug(String msg, CFAEdge edge) {
    if (logger.wouldBeLogged(Level.ALL)) {
      logger.log(Level.ALL, getLogMessage(msg, edge));
    }
  }

  private void log(Level level, String msg) {
    if (logger.wouldBeLogged(level)
        && printedWarnings.add(msg)) {

      logger.log(level, msg);
    }
  }

  /**
   * Returns the size in bytes of the given type.
   * Always use this method instead of machineModel.getSizeOf,
   * because this method can handle dereference-types.
   * @param pType the type to calculate the size of.
   * @return the size in bytes of the given type.
   */
  private int getSizeof(CType pType) {
    int size = pType.accept(sizeofVisitor);
    if (size == 0) {
      // UNDEFINED: http://stackoverflow.com/questions/1626446/what-is-the-size-of-an-empty-struct-in-c
      log(Level.WARNING, "NOTE: Empty structs are UNDEFINED! (" + pType.toString() + ")");
    }
    return size;
  }

  /** Looks up the variable name in the current namespace. */
  private Variable scopedIfNecessary(CExpression exp, SSAMapBuilder ssa, String function) {
    assert
        isSupportedExpression(exp)
        : "Can only handle supported expressions";
    Variable name;
    if (exp instanceof CIdExpression) {
      CIdExpression var = (CIdExpression) exp;
      CSimpleDeclaration decl = var.getDeclaration();
      boolean isGlobal = false;
      if (decl instanceof CDeclaration) {
        isGlobal = ((CDeclaration)decl).isGlobal();
      }
      String simpleName;
      if (isGlobal) {
        simpleName = var.getName();
      } else {
        simpleName = scoped(var.getName(), function);
      }
      name = Variable.create(simpleName, exp.getExpressionType());
    } else if (exp instanceof CFieldReference) {
      CFieldReference fExp = (CFieldReference) exp;
      CExpression owner = getRealFieldOwner(fExp);

      // Now we have the struct
      name = scopedIfNecessary(owner, ssa, function);

      // name is now the struct:
      // for example for a pointer to a struct __content_of__ptr__at__2__end

      // construct the field
      name = makeFieldVariable(name, fExp, ssa);
    } else if (exp instanceof CUnaryExpression) {
      CUnaryExpression unary = (CUnaryExpression) exp;
      name = scopedIfNecessary(unary.getOperand(), ssa, function);
      switch (unary.getOperator()){
      case STAR:
        name = makePointerMask(name, ssa);
        break;
      case AMPER:
        name = makeMemoryLocationVariable(name);
        break;
      case PLUS:
      case MINUS:
      case TILDE:
      case NOT:
      case SIZEOF:
        default:
          throw new AssertionError("Operator not supported in scopedIfNecessary");
      }
    } else if (exp instanceof CCastExpression) {
      // Just ignore
      CCastExpression cast = (CCastExpression) exp;
      name = scopedIfNecessary(cast.getOperand(), ssa, function);
    } else {
      throw new AssertionError("Can't create more complex Variables for Fieldaccess");
    }
    return name;
  }

  private Variable makeFieldVariable(Variable pName, CFieldReference fExp, SSAMapBuilder ssa) {
    Pair<Integer,Integer> msb_lsb = getFieldOffsetMsbLsb(fExp);
    // NOTE: ALWAYS use pName.getType(),
    // because pName.getType() could be an instance of CFieldTrackType
    return Variable.create(
        makeFieldVariableName(pName.getName(), msb_lsb, ssa),
        new CFieldTrackType(fExp.getExpressionType(), pName.getType(), getRealFieldOwner(fExp).getExpressionType()));
  }

  protected FormulaType<?> getFormulaTypeFromCType(CType type) {
    int byteSize = getSizeof(type);

    int bitsPerByte = machineModel.getSizeofCharInBits();
    // byte to bits
    return efmgr.getFormulaType(byteSize * bitsPerByte);
  }

  @SuppressWarnings("unused")
  private boolean isSignedType(CType pType) {
    if (pType instanceof CSimpleType){
      return !((CSimpleType)pType).isUnsigned();
    }
    // Default behaviour, structs for example
    return false;
  }

  private boolean hasRepresentableDereference(Variable v) {
    return isRepresentableType(dereferencedType(v.getType()));
  }

  private boolean hasRepresentableDereference(CExpression e) {
    return isRepresentableType(dereferencedType(e.getExpressionType()));
  }

  /** prefixes function to variable name
  * Call only if you are sure you have a local variable!
  */
  private static String scoped(String var, String function) {
    return function + "::" + var;
  }

  /**
   * This method eleminates all spaces from an expression's ASTString and returns
   * the new String.
   *
   * @param e the expression which should be named
   * @return the name of the expression
   */
  private static String exprToVarName(IAstNode e) {
    return e.toASTString().replaceAll("[ \n\t]", "");
  }

  private String getTypeName(final CType tp) {

    if (tp instanceof CPointerType) {
      return getTypeName(((CPointerType)tp).getType());

    } else if (tp instanceof CTypedefType) {
      return getTypeName(((CTypedefType)tp).getRealType());

    } else if (tp instanceof CCompositeType) {
      CCompositeType compositeType = ((CCompositeType)tp);
      return compositeType.getKind().toASTString() + " " + compositeType.getName();

    } else if (tp instanceof CSimpleType) {
      return tp.toASTString("");

    } else {
      throw new AssertionError("Unknown type " + tp.getClass().getName());
    }
  }

  /**
   * Produces a fresh new SSA index for an assignment
   * and updates the SSA map.
   */
  private int makeFreshIndex(String name, CType type, SSAMapBuilder ssa) {
    return getIndex(name, type, ssa, true);
  }

  /**
   * This method returns the index of the given variable in the ssa map, if there
   * is none, it creates one with the value 1.
   *
   * @return the index of the variable
   */
  private int getIndex(String name, CType type, SSAMapBuilder ssa) {
    return getIndex(name, type, ssa, false);
  }

  private int getIndex(String name, CType type, SSAMapBuilder ssa, boolean makeFresh) {
    int idx = ssa.getIndex(name);
    if (makeFresh) {
      if (idx > 0) {
        idx = idx+1;
      } else {
        idx = VARIABLE_UNINITIALIZED; // AG - IMPORTANT!!! We must start from 2 and
        // not from 1, because this is an assignment,
        // so the SSA index must be fresh.
      }
      setSsaIndex(ssa, name, type, idx);
    } else {
      if (idx <= 0) {
        logger.log(Level.ALL, "WARNING: Auto-instantiating variable:", name);
        idx = 1;
        setSsaIndex(ssa, name, type, idx);
      } else {
        checkSsaSavedType(name, type, ssa);
      }
    }

    return idx;
  }

  private void checkSsaSavedType(String name, CType type, SSAMapBuilder ssa) {

    // Check if types match

    // Assert when a variable already exists, that it has the same type
    // TODO: Un-comment when parser and code-base is stable enough
//    Variable t;
//    assert
//         (t = ssa.getType(name)) == null
//      || CTypeUtils.equals(t, type)
//      : "Saving variables with mutliple types is not possible!";
    CType t = ssa.getType(name);
    if (t != null) {
      if (!areEqual(t, type)) {
        log(Level.WARNING,
            "Variable " + name + " was found with multiple types!"
                + " Analysis with bitvectors could fail! "
                + "(Type1: " + t + ", Type2: " + type + ")");
      }

      if (getFormulaTypeFromCType(t) != getFormulaTypeFromCType(type)){
        throw new UnsupportedOperationException(
            "Variable " + name + " used with types of different sizes! " +
                "(Type1: " + t + ", Type2: " + type + ")");
      }
    }
  }

  private void setSsaIndex(SSAMapBuilder ssa, String name, CType type, int idx) {
    if (isDereferenceType(type)) {
      CType guess = getGuessedType(type);
      if (guess == null) {
        // This should not happen when guessing aliasing types would always work
        log(Level.FINE, "No Type-Guess for " + name);
        CType oneByte = CNumericTypes.CHAR;
        type = setGuessedType(type, oneByte);
      }
    }

    assert
      !isDereferenceType(type) ||
      getGuessedType(type) != null
      : "The guess should be resolved now!";

    checkSsaSavedType(name, type, ssa);

    ssa.setIndex(name, type, idx);
  }

  /**
   * Produces a fresh new SSA index for the left-hand side of an assignment
   * and updates the SSA map.
   */
  private int makeLvalIndex(String varName, CType type, FormulaList args, SSAMapBuilder ssa) {
    int idx = ssa.getIndex(varName, args);
    if (idx > 0) {
      idx = idx+1;
    } else {
      idx = VARIABLE_UNINITIALIZED; // AG - IMPORTANT!!! We must start from 2 and
      // not from 1, because this is an assignment,
      // so the SSA index must be fresh. If we use 1
      // here, we will have troubles later when
      // shifting indices
    }
    ssa.setIndex(varName, args, type, idx);
    return idx;
  }

  /**
   * Create a formula for a given variable, which is assumed to be constant.
   * This method does not handle scoping!
   */
  private Formula makeConstant(String name, CType type, SSAMapBuilder ssa) {
    // TODO better use variables without index (this piece of code prevents
    // SSAMapBuilder from checking for strict monotony)
    int idx = ssa.getIndex(name);
    assert idx <= 1 : name + " is assumed to be constant there was an assignment to it";
    if (idx != 1) {
      setSsaIndex(ssa, name, type, 1); // set index so that predicates will be instantiated correctly
    }

    return fmgr.makeVariable(this.getFormulaTypeFromCType(type), name, 1);
  }
  private Formula makeConstant(Variable var, SSAMapBuilder ssa) {
    return makeConstant(var.getName(), var.getType(), ssa);
  }

  /**
   * Create a formula for a given variable with a fresh index for the left-hand if needed
   * side of an assignment.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   */
  private Formula resolveFields(String name, CType type, SSAMapBuilder ssa, boolean makeFreshIndex) {
    // Resolve Fields

    if (!IS_FIELD_VARIABLE.apply(name)) {
      int idx = getIndex(name, type, ssa, makeFreshIndex);

      assert !IS_FIELD_VARIABLE.apply(name)
        : "Never make variables for field! Always use the underlaying bitvector! Fieldvariable-Names are only used as intermediate step!";
      return fmgr.makeVariable(this.getFormulaTypeFromCType(type), name, idx);
    }

    assert handleFieldAccess : "Field Variables are only allowed with handleFieldAccess";

    Pair<String, Pair<Integer, Integer>> data = removeFieldVariable(name);
    String structName = data.getFirst();
    Pair<Integer, Integer> msb_lsb = data.getSecond();
    // With this we are able to track the types properly
    assert type instanceof CFieldTrackType
     : "Was not able to track types of Field-references";

    CFieldTrackType trackType = (CFieldTrackType)type;
    CType staticType = trackType.getStructType();
    Formula struct = resolveFields(structName, staticType, ssa, makeFreshIndex);
    // At this point it is possible, because of casts, that we have a weird type
    // Because we are currently unable to create dynamic sized bitvectors
    // We can't save in content_of variables the real bitvector
    // For example: void* ptr = malloc( i * sizeof(int))
    // So we can only use the static Type which is given to us
    // Now imagine a field in a struct with type void* and saving another struct in it
    // Now you access it like this: ((otherstruct*)(struct1->voidField))->otherfield
    // Than we would get a 8 bit struct here, because we can't handle the cast properly anywhere else.
    // The only thing we can do at this point is to expand the variable with nondet bits.
    CType runtimeType = trackType.getStructTypeRepectingCasts();

    if (!areEqual(staticType, runtimeType)) {
      log(Level.WARNING, "staticType and runtimeType do not match for " + name + " so analysis could be imprecise");
    }

    BitvectorFormula realStruct = makeExtractOrConcatNondet(staticType, runtimeType, struct);

    return accessField(msb_lsb, realStruct);
  }

  /**
   * Create a formula for a given variable.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   * But it does handles Fields.
   *
   * This method does not update the index of the variable.
   */
  private  Formula makeVariable(String name, CType type, SSAMapBuilder ssa) {
    return resolveFields(name, type, ssa, false);
  }
  private  Formula makeVariable(Variable var, SSAMapBuilder ssa) {
    return makeVariable(var.getName(), var.getType(), ssa);
  }

  /**
   * Create a formula for a given variable with a fresh index if needed.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   * But it does handles Fields.
   *
   * This method does not update the index of the variable.
   */
  private Formula makeVariable(String name, CType type, SSAMapBuilder ssa, boolean makeFreshIndex) {
    return resolveFields(name, type, ssa, makeFreshIndex);
  }

  /**
   * Create a formula for a given variable with a fresh index for the left-hand
   * side of an assignment.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   * But it does handles Fields.
   */
  private Formula makeFreshVariable(String name, CType type, SSAMapBuilder ssa) {
    return resolveFields(name, type, ssa, true);
  }

  /** Takes a (scoped) variable name and returns the pointer variable name. */
  static String makePointerMaskName(String scopedId, SSAMapBuilder ssa) {
    return POINTER_VARIABLE + scopedId + "__at__" + ssa.getIndex(scopedId) + "__end";
  }

  private static Variable makePointerMask(Variable pointerVar, SSAMapBuilder ssa) {
    Variable ptrMask = Variable.create(makePointerMaskName(pointerVar.getName(), ssa), dereferencedType(pointerVar.getType()));
    if (isDereferenceType(ptrMask.getType())) {
      // lookup in ssa map: Maybe we assigned a size to this current variable
      CType savedVarType = ssa.getType(ptrMask.getName());
      if (savedVarType != null) {
        ptrMask = ptrMask.withType(savedVarType);
        assert isDereferenceType(savedVarType)
            : "The savedVar should also be a DereferenceType!";
      }
    }
    return ptrMask;
  }
  /**
   * Takes a pointer variable name and returns the name of the associated
   * variable.
   */
  static String removePointerMask(String pointerVariable) {
    assert (IS_POINTER_VARIABLE.apply(pointerVariable));

    return pointerVariable.substring(POINTER_VARIABLE.length(), pointerVariable.lastIndexOf("__at__"));
  }

  /** Takes a (scoped) struct variable name and returns the field variable name. */
  static String makeFieldVariableName(String scopedId, Pair<Integer,Integer> msb_lsb, SSAMapBuilder ssa) {
    return FIELD_VARIABLE + scopedId +
          "__in__" + String.format("[%d:%d]", msb_lsb.getFirst(), msb_lsb.getSecond()) +
          "__at__" + ssa.getIndex(scopedId) +
          "__end";
  }

  /**
   * Takes a field variable name and returns the name and offset of the associated
   * struct variable.
   */
  static Pair<String, Pair<Integer, Integer>> removeFieldVariable(String fieldVariable) {
    assert (IS_FIELD_VARIABLE.apply(fieldVariable));

    String name = fieldVariable.substring(FIELD_VARIABLE.length(), fieldVariable.lastIndexOf("__in__"));
    String msbLsbString =
        fieldVariable.substring(
            fieldVariable.lastIndexOf("__in__") + "__in__".length(),
            fieldVariable.lastIndexOf("__at__"));
    // Remove []
    msbLsbString = msbLsbString.substring(1, msbLsbString.length() - 1);
    String[] splits = msbLsbString.split(":");
    assert splits.length == 2 : "Expect msb and lsb part";
    return Pair.of(name, Pair.of(Integer.parseInt(splits[0]), Integer.parseInt(splits[1])));
  }



  private static Variable removePointerMaskVariable(Variable pointerVar) {
    return Variable.create(removePointerMask(pointerVar.getName()), makePointerType(pointerVar.getType()));
  }

  /**Returns the concatenation of MEMORY_ADDRESS_VARIABLE_PREFIX and varName */
  private static String makeMemoryLocationVariableName(String varName) {
    return MEMORY_ADDRESS_VARIABLE_PREFIX + varName;
  }

  /**Returns the concatenation of MEMORY_ADDRESS_VARIABLE_PREFIX and varName */
  private static Variable makeMemoryLocationVariable(Variable varName) {
    return Variable.create(makeMemoryLocationVariableName(varName.getName()), makePointerType(varName.getType()));
  }

  /**
   * makes a fresh variable out of the varName and assigns the rightHandSide to it
   * @param varName has to be scoped already
   * @param rightHandSide
   * @param ssa
   * @return the new Formula (lhs = rhs)
   */
  private BooleanFormula makeAssignment(String leftName, CType leftType,
      CType right, Formula rightHandSide, SSAMapBuilder ssa) {
    Formula rhs = makeCast(right, leftType, rightHandSide);
    Formula lhs = makeFreshVariable(leftName, leftType, ssa);

    return fmgr.assignment(lhs, rhs);
  }

  /**
   * Used for implicit and explicit type casts between CTypes.
   * @param fromType the origin Type of the expression.
   * @param toType the type to cast into.
   * @param formula the formula of the expression.
   * @return the new formula after the cast.
   */
  private Formula makeCast(CType fromType, CType toType, Formula formula) {
    // UNDEFINED: Casting a numeric value into a value that can't be represented by the target type (either directly or via static_cast)
    if (areEqual(fromType, toType)) {
      return formula; // No cast required;
    }

    fromType = simplifyType(fromType);
    toType = simplifyType(toType);

    if (fromType instanceof CFunctionType) {
      // references to functions can be seen as function pointers
      fromType = new CPointerType(false, false, fromType);
    }

    boolean fromCanBeHandledAsInt, toCanBeHandledAsInt;
    boolean fromIsPointer, toIsPointer;
    if ((fromCanBeHandledAsInt =
          ((fromIsPointer = fromType instanceof CPointerType) ||
           fromType instanceof CEnumType ||
          (fromType instanceof CElaboratedType &&
              ((CElaboratedType)fromType).getKind() == ComplexTypeKind.ENUM))) |
        (toCanBeHandledAsInt =
          ((toIsPointer = toType instanceof CPointerType) ||
           toType instanceof CEnumType ||
          (toType instanceof CElaboratedType &&
              ((CElaboratedType)toType).getKind() == ComplexTypeKind.ENUM)))) {

      // See Enums/Pointers as Integers
      if (fromCanBeHandledAsInt) {
        fromType = fromIsPointer ? machineModel.getPointerEquivalentSimpleType() : CNumericTypes.INT;
      }
      if (toCanBeHandledAsInt) {
        toType = toIsPointer ? machineModel.getPointerEquivalentSimpleType() : CNumericTypes.INT;
      }
    }



    if (fromType instanceof CSimpleType) {
      CSimpleType sfromType = (CSimpleType)fromType;
      if (toType instanceof CSimpleType) {
        CSimpleType stoType = (CSimpleType)toType;
        return makeSimpleCast(sfromType, stoType, formula);
      }
    }

    if (fromType instanceof CPointerType ||
        toType instanceof CPointerType) {
      // Ignore casts between Pointer and right sized types
      if (getFormulaTypeFromCType(toType) == getFormulaTypeFromCType(fromType)) {
        return formula;
      }
    }

    if (getSizeof(fromType) == getSizeof(toType)) {
      // We can most likely just ignore this cast
      log(Level.WARNING, "WARNING: Ignoring cast from " + fromType + " to " + toType + "!");
      return formula;
    } else {
      throw new UnsupportedOperationException("Cast from " + fromType + " to " + toType + " not supported!");
    }
  }

  private Formula makeCast(CCastExpression e, Formula inner) {
    CType after = e.getExpressionType();
    CType before = e.getOperand().getExpressionType();
    return makeCast(before, after, inner);
  }

  /**
   * Change the size of the given formula from fromType to toType.
   * This method extracts or concats with nondet-bits.
   */
  private BitvectorFormula makeExtractOrConcatNondet(CType pFromType, CType pToType, Formula pFormula) {
    assert pFormula instanceof BitvectorFormula
      : "Can't makeExtractOrConcatNondet for something other than Bitvectors";
    int sfrom = getSizeof(pFromType);
    int sto = getSizeof(pToType);

    int bitsPerByte = machineModel.getSizeofCharInBits();
    return changeFormulaSize(sfrom * bitsPerByte, sto * bitsPerByte, (BitvectorFormula)pFormula);
  }

  /**
   * Change the given Formulasize from the given size to the new size.
   * if sfrom > sto an extract will be done.
   * if sto > sfrom an concat with nondet-bits will be done.
   * else pFormula is returned.
   * @param sfrom
   * @param sto
   * @param pFormula
   * @return the resized formula
   */
  private BitvectorFormula changeFormulaSize(int sfrombits, int stobits, BitvectorFormula pFormula) {
    assert fmgr.getFormulaType(pFormula) == efmgr.getFormulaType(sfrombits)
         : "expected to get sfrombits sized formula!";

    // Currently everything is a bitvector
    BitvectorFormula ret;
    if (sfrombits > stobits) {
      if (stobits == 0) {
        ret = efmgr.makeBitvector(0, 0);
      } else {
        ret = fmgr.makeExtract(pFormula, stobits - 1, 0);
      }
    } else if (sfrombits < stobits) {
      // Sign extend with ones when pfromType is signed and sign bit is set
      int bitsToExtend = stobits - sfrombits;
      FormulaType<BitvectorFormula> t = efmgr.getFormulaType(bitsToExtend);
      BitvectorFormula extendBits = fmgr.makeVariable(t, CtoFormulaConverter.EXPAND_VARIABLE + expands++, 0); // for every call a new variable
      ret = fmgr.makeConcat(extendBits , pFormula);
    } else {
      ret = pFormula;
    }

    assert fmgr.getFormulaType(ret) == efmgr.getFormulaType(stobits);
    return ret;
  }

  /**
   * Handles casts between simple types.
   * When the fromType is a signed type a bit-extension will be done,
   * on any other case it will be filled with 0 bits.
   */
  private Formula makeSimpleCast(CSimpleType pfromType, CSimpleType ptoType, Formula pFormula) {
    int sfrom = machineModel.getSizeof(pfromType);
    int sto = machineModel.getSizeof(ptoType);

    int bitsPerByte = machineModel.getSizeofCharInBits();

    // Currently everything is a bitvector
    Formula ret;
    if (sfrom > sto) {
      ret = fmgr.makeExtract(pFormula, sto * bitsPerByte - 1, 0);
    } else if (sfrom < sto) {
      // Sign extend with ones when pfromType is signed and sign bit is set
      BitvectorFormula extendBits;
      int bitsToExtend = (sto - sfrom) * bitsPerByte;
      if (pfromType.isUnsigned()) {
        extendBits = efmgr.makeBitvector(bitsToExtend, 0);
      } else {
        BitvectorFormula zeroes = efmgr.makeBitvector(bitsToExtend, 0);
        BitvectorFormula ones = efmgr.makeBitvector(bitsToExtend, -1);

        // Formula if sign bit is set
        Formula msb = fmgr.makeExtract(pFormula, sfrom * bitsPerByte - 1, sfrom * bitsPerByte - 1);
        BooleanFormula zeroExtend = fmgr.makeEqual(msb, efmgr.makeBitvector(1, 0));


        extendBits = bfmgr.ifThenElse(zeroExtend, zeroes, ones);
      }

      ret = fmgr.makeConcat(extendBits , pFormula);
    } else {
      ret = pFormula;
    }

    assert fmgr.getFormulaType(ret) == getFormulaTypeFromCType(ptoType);
    return ret;
  }

  /**
   * Gets the Type of the given two, which C would implicitly cast to.
   * @param pT1
   * @param pT2
   * @return
   */
  private CType getImplicitCType(CType pT1, CType pT2) {
    pT1 = simplifyType(pT1);
    pT2 = simplifyType(pT2);

    // UNDEFINED: What should happen when we have two pointer?
    // For example when two pointers get multiplied or added
    // This is always weird.
    if (pT1 instanceof CSimpleType) {
      CSimpleType s1 = (CSimpleType)pT1;
      if (pT2 instanceof CSimpleType) {
        CSimpleType s2 = (CSimpleType)pT2;
        CSimpleType resolved = getImplicitSimpleCType(s1, s2);

        if (!areEqual(s1, s2)) {
          log(Level.FINEST, "Implicit Cast: " + s1 + " and " + s2 + " to " + resolved);
        }

        return resolved;
      } else if (pT2 instanceof CPointerType) {
        return pT2;
      }
    } else if (pT1 instanceof CPointerType) {
      if (pT2 instanceof CSimpleType) {
        return pT1;
      }
    }

    if (pT1.equals(pT2)) {
      return pT1;
    }

    int s1 = getSizeof(pT1);
    int s2 = getSizeof(pT2);
    CType res = pT1;
    if (s1 > s2) {
      res = pT1;
    } else if (s2 > s1) {
      res = pT2;
    } else {
      res = pT1;
    }
    log(Level.WARNING, "Could not get implicit Type of " + pT1 + " and " + pT2 + ", using " + res);
    return res;
  }

  private static int getConversionRank(CSimpleType t) {
    // From https://www.securecoding.cert.org/confluence/display/seccode/INT02-C.+Understand+integer+conversion+rules
    CBasicType type = t.getType();

    assert type == CBasicType.UNSPECIFIED || type == CBasicType.INT || type == CBasicType.BOOL || type == CBasicType.CHAR;
    // For all integer types T1, T2, and T3, if T1 has greater rank than T2 and T2 has greater rank than T3, then T1 has greater rank than T3.

    // The rank of _Bool shall be less than the rank of all other standard integer types.
    if (type == CBasicType.BOOL) {
      return 10;
    }

    // The rank of char shall equal the rank of signed char and unsigned char.
    if (type == CBasicType.CHAR) {
      return 20;
    }

    // The rank of any unsigned integer type shall equal the rank of the corresponding signed integer type, if any.
    // The rank of long long int shall be greater than the rank of long int, which shall be greater than the rank of int, which shall be greater than the rank of short int, which shall be greater than the rank of signed char.
    if (type == CBasicType.INT || type == CBasicType.UNSPECIFIED) {
      if (t.isShort()) {
        return 30;
      }

      if (!t.isLong() && !t.isLongLong()) {
        return 40;
      }

      if (t.isLong()) {
        return 50;
      }

      if (t.isLongLong()) {
        return 60;
      }
    }

    // Notes: The following is not important, because we simplify all types.
    // I did add them only for the sake of completeness.

    // The rank of any standard integer type shall be greater than the rank of any extended integer type with the same width.
    // The rank of any extended signed integer type relative to another extended signed integer type with the same precision is implementation-defined, but still subject to the other rules for determining the integer conversion rank.
    // The rank of a signed integer type shall be greater than the rank of any signed integer type with less precision.
    // No two signed integer types shall have the same rank, even if they have the same representation.
    // The rank of any enumerated type shall equal the rank of the compatible integer type.
    throw new IllegalArgumentException("Unknown type to rank: " + t.toString());
  }

  private CType getPromotedCType(CType t) {
    t = simplifyType(t);
    if (t instanceof CSimpleType) {
      // Integer types smaller than int are promoted when an operation is performed on them.
      // If all values of the original type can be represented as an int, the value of the smaller type is converted to an int;
      // otherwise, it is converted to an unsigned int.
      CSimpleType s = (CSimpleType) t;
      if (machineModel.getSizeof(s) < machineModel.getSizeofInt()) {
        return CNumericTypes.INT;
      }
    }
    return t;
  }

  private CSimpleType getImplicitSimpleCType(CSimpleType pT1, CSimpleType pT2) {
    // From http://msdn.microsoft.com/en-us/library/3t4w2bkb%28v=vs.80%29.aspx
    // If either operand is of type long double, the other operand is converted to type long double.
    CBasicType b1 = pT1.getType();
    CBasicType b2 = pT2.getType();
    if (pT1.isLong() && b1.equals(CBasicType.DOUBLE)) {
      return pT1;
    }

    if (pT2.isLong() && b2.equals(CBasicType.DOUBLE)) {
      return pT2;
    }

    // If the above condition is not met and either operand is of type double, the other operand is converted to type double.
    if (b1.equals(CBasicType.DOUBLE)) {
      return pT1;
    }

    if (b2.equals(CBasicType.DOUBLE)) {
      return pT2;
    }

    // If the above two conditions are not met and either operand is of type float, the other operand is converted to type float.
    if (b1.equals(CBasicType.FLOAT)) {
      return pT1;
    }

    if (b2.equals(CBasicType.FLOAT)) {
      return pT2;
    }

    // See https://www.securecoding.cert.org/confluence/display/seccode/INT02-C.+Understand+integer+conversion+rules
    // See also http://stackoverflow.com/questions/50605/signed-to-unsigned-conversion-in-c-is-it-always-safe

    // If both operands have the same type, no further conversion is needed.
    if (areEqual(pT1, pT2)) {
      return pT1;
    }

    int r1 = getConversionRank(pT1);
    int r2 = getConversionRank(pT2);
    // If both operands are of the same integer type (signed or unsigned), the operand with the type of lesser integer conversion rank is converted to the type of the operand with greater rank.
    if (pT1.isUnsigned() == pT2.isUnsigned()) {
      if (r1 >= r2) {
        return pT1;
      } else {
        return pT2;
      }
    }

    // If the operand that has unsigned integer type has rank greater than or equal to the rank of the type of the other operand, the operand with signed integer type is converted to the type of the operand with unsigned integer type.
    if (pT1.isUnsigned() && r1 >= r2) {
      return pT1;
    }

    if (pT2.isUnsigned() && r2 >= r1) {
      return pT2;
    }

    // If the type of the operand with signed integer type can represent all of the values of the type of the operand with unsigned integer type,
    // the operand with unsigned integer type is converted to the type of the operand with signed integer type.

    int bitsPerByte = machineModel.getSizeofCharInBits();
    int s1 = machineModel.getSizeof(pT1) * bitsPerByte;
    int s2 = machineModel.getSizeof(pT2) * bitsPerByte;

    // When pT1 is signed then it can represent - 2^(s1-1) to 2^(s1-1) - 1 and pT2 can represent 0 to 2^(s2) -1
    if (!pT1.isUnsigned() && s1 > s2) {
      return pT1;
    }
    if (!pT2.isUnsigned() && s2 > s1) {
      return pT2;
    }

    // Otherwise, both operands are converted to the unsigned integer type corresponding to the type of the operand with signed integer type. Specific operations can add to or modify the semantics of the usual arithmetic operations.
    if (pT1.isUnsigned()) {
      return pT1;
    } else {
      assert pT2.isUnsigned();
      return pT2;
    }
  }

//  @Override
  public PathFormula makeAnd(PathFormula oldFormula, CFAEdge edge)
      throws CPATransferException {
    // this is where the "meat" is... We have to parse the statement
    // attached to the edge, and convert it to the appropriate formula

    if (edge.getEdgeType() == CFAEdgeType.BlankEdge) {

      // in this case there's absolutely nothing to do, so take a shortcut
      return oldFormula;
    }

    String function = (edge.getPredecessor() != null)
                          ? edge.getPredecessor().getFunctionName() : null;

    SSAMapBuilder ssa = oldFormula.getSsa().builder();
    Constraints constraints = new Constraints();

    BooleanFormula edgeFormula = createFormulaForEdge(edge, function, ssa, constraints);

    if (useNondetFlags) {
      int lNondetIndex = ssa.getIndex(NONDET_VARIABLE);
      int lFlagIndex = ssa.getIndex(NONDET_FLAG_VARIABLE);

      if (lNondetIndex != lFlagIndex) {
        if (lFlagIndex < 0) {
          lFlagIndex = 1; // ssa indices start with 2, so next flag that is generated also uses index 2
        }

        for (int lIndex = lFlagIndex + 1; lIndex <= lNondetIndex; lIndex++) {
          Formula nondetVar = fmgr.makeVariable(NONDET_FORMULA_TYPE, NONDET_FLAG_VARIABLE, lIndex);
          BooleanFormula lAssignment = fmgr.assignment(nondetVar, fmgr.makeNumber(NONDET_FORMULA_TYPE, 1));
          edgeFormula = bfmgr.and(edgeFormula, lAssignment);
        }

        // update ssa index of nondet flag
        //setSsaIndex(ssa, Variable.create(NONDET_FLAG_VARIABLE, getNondetType()), lNondetIndex);
        ssa.setIndex(NONDET_FLAG_VARIABLE, NONDET_TYPE, lNondetIndex);
      }
    }

    edgeFormula = bfmgr.and(edgeFormula, constraints.get());

    SSAMap newSsa = ssa.build();
    if (bfmgr.isTrue(edgeFormula) && (newSsa == oldFormula.getSsa())) {
      // formula is just "true" and SSAMap is identical
      // i.e. no writes to SSAMap, no branching and length should stay the same
      return oldFormula;
    }

    BooleanFormula newFormula = bfmgr.and(oldFormula.getFormula(), edgeFormula);
    int newLength = oldFormula.getLength() + 1;
    return new PathFormula(newFormula, newSsa, newLength);
  }

  /**
   * This helper method creates a formula for an CFA edge, given the current function, SSA map and constraints.
   *
   * @param edge the edge for which to create the formula
   * @param function the current scope
   * @param ssa the current SSA map
   * @param constraints the current constraints
   * @return the formula for the edge
   * @throws CPATransferException
   */
  private BooleanFormula createFormulaForEdge(CFAEdge edge, String function, SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {
    switch (edge.getEdgeType()) {
    case StatementEdge: {
      CStatementEdge statementEdge = (CStatementEdge) edge;
      StatementToFormulaVisitor v;
      if (handlePointerAliasing) {
        v = new StatementToFormulaVisitorPointers(function, ssa, constraints, edge);
      } else {
        v = new StatementToFormulaVisitor(function, ssa, constraints, edge);
      }
      return statementEdge.getStatement().accept(v);
    }

    case ReturnStatementEdge: {
      CReturnStatementEdge returnEdge = (CReturnStatementEdge)edge;
      return makeReturn(returnEdge.getExpression(), returnEdge, function, ssa, constraints);
    }

    case DeclarationEdge: {
      CDeclarationEdge d = (CDeclarationEdge)edge;
      return makeDeclaration(d, function, ssa, constraints);
    }

    case AssumeEdge: {
      return makeAssume((CAssumeEdge)edge, function, ssa, constraints);
    }

    case BlankEdge: {
      assert false : "Handled above";
      return bfmgr.makeBoolean(true);
    }

    case FunctionCallEdge: {
      return makeFunctionCall((CFunctionCallEdge)edge, function, ssa, constraints);
    }

    case FunctionReturnEdge: {
      // get the expression from the summary edge
      CFunctionSummaryEdge ce = ((CFunctionReturnEdge)edge).getSummaryEdge();
      return makeExitFunction(ce, function, ssa, constraints);
    }

    case MultiEdge: {
      BooleanFormula multiEdgeFormula = bfmgr.makeBoolean(true);

      // unroll the MultiEdge
      for (CFAEdge singleEdge : (MultiEdge)edge) {
        if (singleEdge instanceof BlankEdge) {
          continue;
        }
        multiEdgeFormula = bfmgr.and(multiEdgeFormula, createFormulaForEdge(singleEdge, function, ssa, constraints));
      }

      return multiEdgeFormula;
    }

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }
  }

  private BooleanFormula makeDeclaration(
      CDeclarationEdge edge, String function, SSAMapBuilder ssa,
      Constraints constraints) throws CPATransferException {

    if (!(edge.getDeclaration() instanceof CVariableDeclaration)) {
      // struct prototype, function declaration, typedef etc.
      logDebug("Ignoring declaration", edge);
      return bfmgr.makeBoolean(true);
    }

    CVariableDeclaration decl = (CVariableDeclaration)edge.getDeclaration();

    String varNameWithoutFunction = decl.getName();
    String varName;
    if (decl.isGlobal()) {
      varName = varNameWithoutFunction;
    } else {
      varName = scoped(varNameWithoutFunction, function);
    }

    // if the var is unsigned, add the constraint that it should
    // be > 0
    //    if (((CSimpleType)spec).isUnsigned()) {
    //    long z = mathsat.api.msat_make_number(msatEnv, "0");
    //    long mvar = buildMsatVariable(var, idx);
    //    long t = mathsat.api.msat_make_gt(msatEnv, mvar, z);
    //    t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
    //    m1 = new MathsatFormula(t);
    //    }

    // just increment index of variable in SSAMap
    // (a declaration contains an implicit assignment, even without initializer)
    // In case of an existing initializer, we increment the index twice
    // (here and below) so that the index 2 only occurs for uninitialized variables.
    // DO NOT OMIT THIS CALL, even without an initializer!
    makeFreshIndex(varName, decl.getType(), ssa);

    // if there is an initializer associated to this variable,
    // take it into account
    CInitializer initializer = decl.getInitializer();
    CExpression init = null;

    if (initializer == null) {
      if (initAllVars) {
        // auto-initialize variables to zero
        logDebug("AUTO-INITIALIZING VAR: ", edge);
        init = CDefaults.forType(decl.getType(), null);
      }

    } else if (initializer instanceof CInitializerExpression) {
      init = ((CInitializerExpression)initializer).getExpression();

    } else {
      logDebug("Ignoring unsupported initializer", initializer);
    }

    if (init == null) {
      return bfmgr.makeBoolean(true);
    }

    // initializer value present
    // Do a regular assignment
    CExpressionAssignmentStatement assign =
        new CExpressionAssignmentStatement(
            decl.getFileLocation(),
            new CIdExpression(decl.getFileLocation(), decl.getType(), decl.getName(), decl),
            init);
    StatementToFormulaVisitor v;
    if (handlePointerAliasing) {
      v = new StatementToFormulaVisitorPointers(function, ssa, constraints, edge);
    } else {
      v = new StatementToFormulaVisitor(function, ssa, constraints, edge);
    }
    return assign.accept(v);
  }


  private BooleanFormula makeExitFunction(CFunctionSummaryEdge ce, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {

    CFunctionCall retExp = ce.getExpression();
    if (retExp instanceof CFunctionCallStatement) {
      // this should be a void return, just do nothing...
      return bfmgr.makeBoolean(true);

    } else if (retExp instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement exp = (CFunctionCallAssignmentStatement)retExp;
      String retVarName = scoped(VAR_RETURN_NAME, function);

      CFunctionCallExpression funcCallExp = exp.getRightHandSide();
      CType retType = getReturnType(funcCallExp, ce);

      Formula retVar = makeVariable(retVarName, retType, ssa);
      CExpression e = exp.getLeftHandSide();

      function = ce.getSuccessor().getFunctionName();
      Formula outvarFormula = buildLvalueTerm(e, ce, function, ssa, constraints);
      retVar = makeCast(retType, e.getExpressionType(), retVar);
      BooleanFormula assignments = fmgr.assignment(outvarFormula, retVar);

      if (handlePointerAliasing) {
        CExpression left = removeCast(e);
        if (left instanceof CIdExpression) {
          BooleanFormula ptrAssignment = buildDirectReturnSecondLevelAssignment(
              e, Variable.create(retVarName, retType), function, ssa);
          assignments = bfmgr.and(assignments, ptrAssignment);
        }
      }

       return assignments;
    } else {
      throw new UnrecognizedCCodeException("Unknown function exit expression", ce, retExp.asStatement());
    }
  }

  private CType getReturnType(CFunctionCallExpression funcCallExp, CFAEdge edge) throws UnrecognizedCCodeException {
    // NOTE: When funCallExp.getExpressionType() does always return the return type of the function we don't
    // need this function. However I'm not sure because there can be implicit casts. Just to be safe.
    CType retType;
    CFunctionDeclaration funcDecl = funcCallExp.getDeclaration();
    if (funcDecl == null) {
      // Check if we have a function pointer here.
      CExpression functionNameExpression = funcCallExp.getFunctionNameExpression();
      CType expressionType = simplifyType(functionNameExpression.getExpressionType());
      if (expressionType instanceof CFunctionType) {
        CFunctionType funcPtrType = (CFunctionType)expressionType;
        retType = funcPtrType.getReturnType();
      } else {
        throw new UnrecognizedCCodeException("Cannot handle function pointer call with unknown type " + expressionType, edge, funcCallExp);
      }
      assert retType != null;
    } else {
      retType = funcDecl.getType().getReturnType();
    }

    CType expType = funcCallExp.getExpressionType();
    if (!areEqual(expType, retType)) {
      // Bit ignore for now because we sometimes just get ElaboratedType instead of CompositeType
      log(
          Level.SEVERE,
          "Returntype and ExpressionType are not equal: "
              + expType.toString() +  ", " + retType.toString());
    }
    return expType;
  }


  private BooleanFormula makeFunctionCall(CFunctionCallEdge edge,
      String callerFunction, SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {

    List<CExpression> actualParams = edge.getArguments();

    CFunctionEntryNode fn = edge.getSuccessor();
    List<CParameterDeclaration> formalParams = fn.getFunctionParameters();

    String calledFunction = fn.getFunctionName();

    if (fn.getFunctionDefinition().getType().takesVarArgs()) {
      if (formalParams.size() > actualParams.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", edge);
      }

      if (!SAFE_VAR_ARG_FUNCTIONS.contains(calledFunction)) {
        log(Level.WARNING, "Ignoring parameters passed as varargs to function "
                           + calledFunction + " in line " + edge.getLineNumber());
      }

    } else {
      if (formalParams.size() != actualParams.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", edge);
      }
    }

    int i = 0;
    BooleanFormula result = bfmgr.makeBoolean(true);
    for (CParameterDeclaration formalParam : formalParams) {
      // get formal parameter name
      String formalParamName = formalParam.getName();
      assert (!formalParamName.isEmpty()) : edge;

      if (formalParam.getType() instanceof CPointerType) {
        log(Level.WARNING, "Program contains pointer parameter; analysis is imprecise in case of aliasing.");
        logDebug("Ignoring the semantics of pointer for parameter "
            + formalParamName, fn.getFunctionDefinition());
      }
      CExpression paramExpression = actualParams.get(i++);
      // get value of actual parameter
      Formula actualParam = buildTerm(paramExpression, edge, callerFunction, ssa, constraints);

      String varName = scoped(formalParamName, calledFunction);
      CType paramType = formalParam.getType();
      BooleanFormula eq =
          makeAssignment(
              varName, paramType,
              paramExpression.getExpressionType(),
              actualParam, ssa);

      result = bfmgr.and(result, eq);
    }

    return result;
  }

  private BooleanFormula makeReturn(CExpression rightExp, CReturnStatementEdge edge, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {
    if (rightExp == null) {
      // this is a return from a void function, do nothing
      return bfmgr.makeBoolean(true);
    } else {

      // we have to save the information about the return value,
      // so that we can use it later on, if it is assigned to
      // a variable. We create a function::__retval__ variable
      // that will hold the return value
      Formula retval = buildTerm(rightExp, edge, function, ssa, constraints);
      String retVarName = scoped(VAR_RETURN_NAME, function);

      CType expressionType = rightExp.getExpressionType();
      CType returnType =
          ((CFunctionEntryNode)edge.getSuccessor().getEntryNode())
            .getFunctionDefinition()
            .getType()
            .getReturnType();
      BooleanFormula assignments = makeAssignment(retVarName, returnType,
          expressionType, retval, ssa);

      if (handlePointerAliasing) {
        // if the value to be returned may be a pointer, act accordingly
        BooleanFormula rightAssignment = buildDirectSecondLevelAssignment(
            Variable.create(retVarName, returnType), rightExp, function, constraints, ssa);
        assignments = bfmgr.and(assignments, rightAssignment);
      }

      return assignments;
    }
  }

  private BooleanFormula makeAssume(CAssumeEdge assume, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {

    return makePredicate(assume.getExpression(), assume.getTruthAssumption(),
        assume, function, ssa, constraints);
  }

  private Formula buildTerm(CExpression exp, CFAEdge edge, String function,
      SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {
    return exp.accept(getCExpressionVisitor(edge, function, ssa, constraints));
  }

  private Formula buildLvalueTerm(CExpression exp, CFAEdge edge, String function,
      SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {
    return exp.accept(getLvalueVisitor(edge, function, ssa, constraints));
  }

  private BooleanFormula buildDirectReturnSecondLevelAssignment(CExpression leftId,
      Variable retVarName, String function, SSAMapBuilder ssa) {

    // include aliases if the left or right side may be a pointer a pointer
    // We only can write *l = *r if the types after dereference have some formula
    // type correspondence (e.g. boolean, real, bit vector etc.)
    // Types of unknown sizes, e.g. CProblemType, can't be represented and would
    // otherwise cause throwing exceptions
    Variable leftVar = scopedIfNecessary(leftId, ssa, function);
    if ((maybePointer(leftVar, ssa)
        || maybePointer(retVarName, ssa)) &&
        hasRepresentableDereference(leftId) &&
        hasRepresentableDereference(retVarName)) {
      // we assume that either the left or the right hand side is a pointer
      // so we add the equality: *l = *r
      Variable leftPtrMask = makePointerMask(leftVar, ssa);
      Formula lPtrVar =  makeVariable(leftPtrMask, ssa);
      Variable retPtrVarName = makePointerMask(retVarName, ssa);

      Formula retPtrVar = makeVariable(retPtrVarName, ssa);
      return makeNondetAssignment(lPtrVar, retPtrVar);

    } else {
      // we can assume, that no pointers are affected in this assignment
      return bfmgr.makeBoolean(true);
    }
  }

  private int expands = 0;
  private BooleanFormula makeNondetAssignment(Formula left, Formula right) {
    BitvectorFormulaManagerView bitvectorFormulaManager = efmgr;
    FormulaType<Formula> tl = fmgr.getFormulaType(left);
    FormulaType<Formula> tr = fmgr.getFormulaType(right);
    if (tl == tr){
      return fmgr.assignment(left, right);
    }

    if ((Class<?>)tl.getInterfaceType() == BitvectorFormula.class &&
        (Class<?>)tr.getInterfaceType() == BitvectorFormula.class) {

      BitvectorFormula leftBv = (BitvectorFormula) left;
      BitvectorFormula rightBv = (BitvectorFormula) right;
      int leftSize = bitvectorFormulaManager.getLength(leftBv);
      int rightSize = bitvectorFormulaManager.getLength(rightBv);

      // Expand the smaller one with nondet-bits
      if (leftSize < rightSize) {
        leftBv =
            changeFormulaSize(leftSize, rightSize, leftBv);
      } else {
        rightBv =
            changeFormulaSize(rightSize, leftSize, rightBv);
      }
      return bitvectorFormulaManager.equal(leftBv, rightBv);
    }

    throw new IllegalArgumentException("Assignment between different types");
  }

  private void warnToComplex(IAstNode node) {
    if (handleFieldAccess) {
      log(Level.WARNING, "Ignoring pointer aliasing, because statement is too complex, please simplify: " + node.toASTString() + " (Line: " + node.getFileLocation().getStartingLineNumber() + ")");
    } else {
      log(Level.WARNING, "Ignoring pointer aliasing, because statement is too complex, please simplify or enable handleFieldAccess and handleFieldAliasing: " + node.toASTString() + " (Line: " + node.getFileLocation().getStartingLineNumber() + ")");
    }
  }

  private BooleanFormula buildDirectSecondLevelAssignment(
      Variable lVarName,
      CRightHandSide pRight, String function,
      Constraints constraints, SSAMapBuilder ssa) {

    if (!hasRepresentableDereference(lVarName)) {
      // The left side is a type that should not be dereferenced, so no 2nd level assignment
      return bfmgr.makeBoolean(true);
    }

    if (!(pRight instanceof CExpression)) {
      // The right side is something strange
      log(Level.WARNING, "Not a CExpression on the right side, ignoring aliasing: " + pRight.toASTString());
      return bfmgr.makeBoolean(true);
    }

    CExpression right = (CExpression)pRight;
    if (!isSupportedExpression(right)) {
      if (isTooComplexExpression(right)) {
        // The right side is too complex
        warnToComplex(right);
      }
      // If it is not too complex we probably need no 2nd level
      // TODO: Check the statement above.
      return bfmgr.makeBoolean(true);
    }

    Formula lVar = makeVariable(lVarName, ssa);

    Variable lPtrVarName = makePointerMask(lVarName, ssa);
    CType leftPtrType = lPtrVarName.getType();
    if (right instanceof CIdExpression ||
        (handleFieldAccess && right instanceof CFieldReference && !isIndirectFieldReference((CFieldReference)right))) {
      // C statement like: s1 = s2; OR s1 = s2.d;

      // include aliases if the left or right side may be a pointer a pointer
      // Assume no pointers affected if unrepresentable values are assigned
      Variable rightVar = scopedIfNecessary(right, ssa, function);
      if ((maybePointer(lVarName, ssa) || maybePointer(rightVar, ssa)) &&
          hasRepresentableDereference(right)) {
        // we assume that either the left or the right hand side is a pointer
        // so we add the equality: *l = *r
        Variable rPtrVarName = makePointerMask(rightVar, ssa);

        boolean leftT, rightT;
        if ((leftT = isDereferenceType(leftPtrType)) |
            (rightT = isDereferenceType(rPtrVarName.getType()))) {
          // One of those types is no pointer so try to guess dereferenced type.

          if (leftT) {
            if (rightT) {
              // Right is actually no pointer but was used as pointer before, so we can use its type.
            }

            // OK left is no pointer, but right is, for example:
            // l = r; // l is unsigned int and r is *long
            // now we assign *l to the type of *r if *l was not assigned before
            CType currentGuess = getGuessedType(leftPtrType);
            if (currentGuess == null) {
              lPtrVarName =
                  lPtrVarName.withType(setGuessedType(leftPtrType, rPtrVarName.getType()));
            } else {
              if (getSizeof(rPtrVarName.getType()) != getSizeof(currentGuess)) {
                log(Level.WARNING, "Second assignment of an variable that is no pointer with different size");
              }
            }
          } else {
            assert rightT : "left and right side are no pointers, however maybePointer was true for one side!";
            // OK right is no pointer, but left is, for example:
            // l = r; // l is unsigned long* and r is unsigned int

            // r was probably assigned with a pointer before and should have a size
            if (!(right.getExpressionType() instanceof CFunctionType)) {
              // ignore function pointer assignments
              CType currentGuess = getGuessedType(rPtrVarName.getType());
              if (currentGuess == null) {
                // TODO: This currently happens when assigning a function to a function pointer.
                // NOTE: Should we set the size of r in this case?
                log(Level.WARNING, "Pointer " + lVarName.getName() + " is assigned the value of variable " +
                    right.toASTString() + " which contains a non-pointer value in line " +
                    right.getFileLocation().getStartingLineNumber());
              } else {
                if (getSizeof(rPtrVarName.getType()) != getSizeof(currentGuess)) {
                  log(Level.WARNING, "Assignment of a pointer from a variable that was assigned by a pointer with different size!");
                }
              }
            }
          }
        }

        Formula lPtrVar = makeVariable(lPtrVarName, ssa);
        Formula rPtrVar = makeVariable(rPtrVarName, ssa);

        return makeNondetAssignment(lPtrVar, rPtrVar);
      } else {
        // we can assume, that no pointers are affected in this assignment
        return bfmgr.makeBoolean(true);
      }

    } else if ((right instanceof CUnaryExpression &&
                   ((CUnaryExpression) right).getOperator() == UnaryOperator.STAR) ||
               (handleFieldAccess && right instanceof CFieldReference && isIndirectFieldReference((CFieldReference)right))) {
      // C statement like: s1 = *s2;
      // OR s1 = *(s.b)
      // OR s1 = s->b

      if (isDereferenceType(leftPtrType)) {
        // We have an assignment to a non-pointer type
        CType guess = getGuessedType(leftPtrType);
        if (guess == null) {
          // We have to guess the size of the dereferenced type here
          // but there is no good guess.
          // TODO: if right side is a **(pointer of a pointer) type use it.
        }
      }

      makeFreshIndex(lPtrVarName.getName(), lPtrVarName.getType(), ssa);
      removeOldPointerVariablesFromSsaMap(lPtrVarName.getName(), ssa);

      Formula lPtrVar = makeVariable(lPtrVarName, ssa);

      if (!(isSupportedExpression(right)) ||
          !(hasRepresentableDereference(right))) {
        // these are statements like s1 = *(s2->f)
        warnToComplex(right);
        return bfmgr.makeBoolean(true);
      }

      Variable rPtrVarName = scopedIfNecessary(right, ssa, function);
      Formula rPtrVar = makeVariable(rPtrVarName, ssa);
      //Formula rPtrVar = makePointerVariable(rRawExpr, function, ssa);

      // the dealiased address of the right hand side may be a pointer itself.
      // to ensure tracking, we need to set the left side
      // equal to the dealiased right side or update the pointer
      // r is the right hand side variable, l is the left hand side variable
      // ∀p ∈ maybePointer: (p = *r) ⇒ (l = p ∧ *l = *p)
      // Note: l = *r holds because of current statement
      List<Variable> ptrVars = getAllPointerVariablesFromSsaMap(ssa);
      for (Variable ptrVarName : ptrVars) {
        Variable varName = removePointerMaskVariable(ptrVarName);

        if (!varName.equals(lVarName)) {

          Formula var = makeVariable(varName, ssa);
          Formula ptrVar = makeVariable(ptrVarName, ssa);

          // p = *r. p is a pointer but *r can be anything
          BooleanFormula ptr = makeNondetAssignment(rPtrVar, var);
          // l = p. p is a pointer but l can be anything.
          BooleanFormula dirEq = makeNondetAssignment(lVar, var);

          // *l = *p. Both can be anything.
          BooleanFormula indirEq =
              makeNondetAssignment(lPtrVar, ptrVar);

          BooleanFormula consequence = bfmgr.and(dirEq, indirEq);
          BooleanFormula constraint = bfmgr.implication(ptr, consequence);
          constraints.addConstraint(constraint);
        }
      }

      // no need to add a second level assignment
      return bfmgr.makeBoolean(true);

    } else if (isMemoryLocation(right)) {
      // s = &x
      // OR s = (&x).t
      // need to update the pointer on the left hand side
      if (right instanceof CUnaryExpression
          && ((CUnaryExpression) right).getOperator() == UnaryOperator.AMPER) {

        CExpression rOperand =
            removeCast(((CUnaryExpression) right).getOperand());
        if (rOperand instanceof CIdExpression &&
            hasRepresentableDereference(lVarName)) {
          Variable rVarName = scopedIfNecessary(rOperand, ssa, function);
          Formula rVar = makeVariable(rVarName, ssa);

          if (isDereferenceType(leftPtrType)) {
            // s is no pointer and if *s was not guessed jet we can use the type of x.
            CType guess = getGuessedType(leftPtrType);
            if (guess == null) {
              lPtrVarName =
                  lPtrVarName.withType(setGuessedType(leftPtrType, rOperand.getExpressionType()));
            } else {
              if (getSizeof(guess) != getSizeof(rOperand.getExpressionType())) {
                log(Level.WARNING, "Size of an old guess doesn't match with the current guess: " + lPtrVarName.getName());
              }
            }
          }
          Formula lPtrVar = makeVariable(lPtrVarName, ssa);

          return makeNondetAssignment(lPtrVar, rVar);
        }
      } else if (right instanceof CFieldReference) {
        // Weird Case
        log(Level.WARNING, "Strange MemoryLocation: " + right.toASTString());
      }

      // s = malloc()
      // has been handled already
      return bfmgr.makeBoolean(true);

    } else {
      // s = someFunction()
      // s = a + b
      // s = a[i]

      // no second level assignment necessary
      return bfmgr.makeBoolean(true);
    }
  }


  private BooleanFormula makePredicate(CExpression exp, boolean isTrue, CFAEdge edge,
      String function, SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {

    if (getIndirectionLevel(exp) > supportedIndirectionLevel) {
      warnToComplex(exp);
    }

    Formula f = exp.accept(getCExpressionVisitor(edge, function, ssa, constraints));
    BooleanFormula result = fmgr.toBooleanFormula(f);

    if (!isTrue) {
      result = bfmgr.not(result);
    }
    return result;
  }

  public BooleanFormula makePredicate(CExpression exp, CFAEdge edge, String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    Constraints constraints = new Constraints();
    BooleanFormula f = makePredicate(exp, true, edge, function, ssa, constraints);
    return bfmgr.and(f, constraints.get());
  }

  private ExpressionToFormulaVisitor getCExpressionVisitor(CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, Constraints pCo) {
    if (lvalsAsUif) {
      return new ExpressionToFormulaVisitorUIF(pEdge, pFunction, pSsa, pCo);
    } else if (handlePointerAliasing) {
      return new ExpressionToFormulaVisitorPointers(pEdge, pFunction, pSsa, pCo);
    } else {
      return new ExpressionToFormulaVisitor(pEdge, pFunction, pSsa, pCo);
    }
  }

  private LvalueVisitor getLvalueVisitor(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
    if (lvalsAsUif) {
      return new LvalueVisitorUIF(pEdge, pFunction, pSsa, pCo);
    } else if (handlePointerAliasing) {
      return new LvalueVisitorPointers(pEdge, pFunction, pSsa, pCo);
    } else {
      return new LvalueVisitor(pEdge, pFunction, pSsa, pCo);
    }
  }

  private boolean isMemoryLocation(CAstNode exp) {
    exp = removeCast(exp);

    // memory allocating function?
    if (exp instanceof CFunctionCall) {
      CExpression fn =
          ((CFunctionCall) exp).getFunctionCallExpression().getFunctionNameExpression();

      if (fn instanceof CIdExpression) {
        String functionName = ((CIdExpression) fn).getName();
        if (memoryAllocationFunctions.contains(functionName)) {
          return true;
        }
      }

    // explicit heap/stack address?
    } else if (exp instanceof CUnaryExpression
        && ((CUnaryExpression) exp).getOperator() == UnaryOperator.AMPER) {
      return true;
    } else if (exp instanceof CFieldReference && !isIndirectFieldReference((CFieldReference)exp)) {
      return isMemoryLocation(((CFieldReference)exp).getFieldOwner());
    }


    return false;
  }

  /**
   * Creates a Formula which accesses the given bits.
   */
  private BitvectorFormula accessField(Pair<Integer, Integer> msb_Lsb, BitvectorFormula f) {
    return fmgr.makeExtract(f, msb_Lsb.getFirst(), msb_Lsb.getSecond());
  }

  /**
   * Creates a Formula which accesses the given Field
   */
  private BitvectorFormula accessField(CFieldReference fExp, Formula f) {
    assert handleFieldAccess : "Fieldaccess if only allowed with handleFieldAccess";
    assert f instanceof BitvectorFormula : "Fields need to be represented with bitvectors";
    // Get the underlaying structure
    Pair<Integer, Integer> msb_Lsb = getFieldOffsetMsbLsb(fExp);
    return accessField(msb_Lsb, (BitvectorFormula)f);
  }

  private Formula replaceField(Pair<Integer, Integer> msb_Lsb, Formula f, Formula newField) {

    assert efmgr.getLength((BitvectorFormula) newField) == msb_Lsb.getFirst() + 1 - msb_Lsb.getSecond()
        : "The new formula has not the right size";

    assert handleFieldAccess : "Fieldaccess if only allowed with handleFieldAccess";

    Pair<Formula, Formula> pre_after = getPreAfterFormulas(f, msb_Lsb);

    return fmgr.makeConcat(ImmutableList.of(pre_after.getFirst(), newField, pre_after.getSecond()));
  }

  private Formula replaceField(CFieldReference fExp, Formula pLVar, Formula pRightVariable) {
    Pair<Integer, Integer> msb_Lsb = getFieldOffsetMsbLsb(fExp);

    return replaceField(msb_Lsb, pLVar, pRightVariable);
  }


  /**
   * Returns the given struct but without the bits indicated by the given
   * CFieldReference.
   */
  private Formula withoutField(CFieldReference fExp, Formula f) {
    assert handleFieldAccess : "Fieldaccess if only allowed with handleFieldAccess";
    Pair<Integer, Integer> msb_Lsb = getFieldOffsetMsbLsb(fExp);
    Pair<Formula, Formula> pre_after = getPreAfterFormulas(f, msb_Lsb);
    return fmgr.makeConcat(pre_after.getFirst(), pre_after.getSecond());
  }

  private Pair<Formula, Formula> getPreAfterFormulas(Formula f, Pair<Integer, Integer> msb_Lsb) {
    int size = efmgr.getLength((BitvectorFormula) f);
    assert size > msb_Lsb.getFirst() : "f is too small";
    assert 0 <= msb_Lsb.getSecond() && msb_Lsb.getFirst() >= msb_Lsb.getSecond() : "msb_Lsb is invalid";

    Formula pre = efmgr.makeBitvector(0, 0);
    if (msb_Lsb.getFirst() + 1 < size) {
      pre = fmgr.makeExtract(f, size - 1, msb_Lsb.getFirst() + 1);
    }
    Formula after = efmgr.makeBitvector(0, 0);
    if (msb_Lsb.getSecond() > 0) {
      after = fmgr.makeExtract(f, msb_Lsb.getSecond() - 1, 0);
    }

    Pair<Formula, Formula> pre_after = Pair.of(pre, after);
    return pre_after;
  }

  /**
   * Returns the offset of the given CFieldReference within the structure in bits.
   */
  private Pair<Integer, Integer> getFieldOffsetMsbLsb(CFieldReference fExp) {
    CExpression fieldRef = getRealFieldOwner(fExp);
    CType structType = simplifyType(fieldRef.getExpressionType());

    assert structType instanceof CCompositeType :
        "expecting CCompositeType on structs!";
    // f is now the structure, access it:
    int bitsPerByte = machineModel.getSizeofCharInBits();
    int offset = getFieldOffset((CCompositeType) structType, fExp.getFieldName(), fExp.getExpressionType()) * bitsPerByte;
    int fieldSize = getSizeof(fExp.getExpressionType()) * bitsPerByte;
    int lsb = offset;
    int msb = offset + fieldSize - 1;
    Pair<Integer, Integer> msb_Lsb = Pair.of(msb, lsb);
    return msb_Lsb;
  }

  /**
   * Returns the offset of the given field in the given struct in bytes
   */
  private int getFieldOffset(CCompositeType structType, String fieldName, CType assertFieldType) {
      int off = 0;
      for (CCompositeTypeMemberDeclaration member : structType.getMembers() ) {
        if (member.getName().equals(fieldName)) {
          if (assertFieldType != null) {
            if (!areEqual(assertFieldType, member.getType())){
              log(Level.SEVERE,
                  "Expected the same type for member (Ignore it for function pointer): " +
                      assertFieldType.toString() + ", " + member.getType().toString());
            }
          }

          return off;
        }

        off += getSizeof(member.getType());
      }

      throw new AssertionError("field " + fieldName + " was not found in " + structType);
  }

  private static boolean maybePointer(Variable var, SSAMapBuilder ssa) {
    if (var.getType() instanceof CPointerType) {
      return true;
    }

    // check if it has been used as a pointer before
    String expPtrVarName = makePointerMaskName(var.getName(), ssa);
    return ssa.getType(expPtrVarName) != null;
  }

  /**
   * Returns a list of all variable names representing memory locations in
   * the SSAMap. These memory locations are those previously used.
   *
   * Stored memory locations are prefixed with
   * {@link #MEMORY_ADDRESS_VARIABLE_PREFIX}.
   */
  private static ImmutableList<Variable> getAllMemoryLocationsFromSsaMap(SSAMapBuilder ssa) {
    return from(ssa.allVariables())
              .filter(liftToVariable(IS_MEMORY_ADDRESS_VARIABLE))
              .toImmutableList();
  }

  private static Predicate<? super Variable> liftToVariable(final Predicate<? super String> stringPred) {
    return new Predicate<Variable>() {
      @Override
      public boolean apply(Variable pInput) {
        return stringPred.apply(pInput.getName());
      }};
  }

  /**
   * Returns a list of all pointer variables stored in the SSAMap.
   */
  private static List<Variable> getAllPointerVariablesFromSsaMap(SSAMapBuilder ssa) {
    return from(ssa.allVariables())
              .filter(liftToVariable(IS_POINTER_VARIABLE))
              .toImmutableList();
  }

  /**
   * Removes all pointer variables belonging to a given variable from a given
   * SSAMapBuilderthat are no longer valid. Validity of an entry expires,
   * when the pointer variable belongs to a variable with an old index.
   *
   * @param newPVar The variable name of the new pointer variable.
   * @param ssa The SSAMapBuilder from which the variables are to be deleted
   */
  private static void removeOldPointerVariablesFromSsaMap(String newPVar,
      SSAMapBuilder ssa) {

    String newVar = removePointerMask(newPVar);

    List<Variable> pointerVariables = getAllPointerVariablesFromSsaMap(ssa);
    for (Variable ptrVar : pointerVariables) {
      String ptrVarName = ptrVar.getName();
      String oldVar = removePointerMask(ptrVarName);
      if (!ptrVarName.equals(newPVar) && oldVar.equals(newVar)) {
        ssa.deleteVariable(ptrVarName);
      }
    }
  }

  private static CExpression removeCast(CExpression exp) {
    if (exp instanceof CCastExpression) {
      return removeCast(((CCastExpression) exp).getOperand());
    }
    return exp;
  }

  private static CRightHandSide removeCast(CRightHandSide exp) {
    if (exp instanceof CCastExpression) {
      return removeCast(((CCastExpression) exp).getOperand());
    }
    return exp;
  }

  private CAstNode removeCast(CAstNode exp) {
    if (exp instanceof CCastExpression) {
      return removeCast(((CCastExpression) exp).getOperand());
    }
    return exp;
  }


  /**
   * Indicates which level of indirection is supported.
   * This should stay 1 unless you know what you are doing.
   * The main reason for this limit is that we would have to emit a lot more formulas for every additional level.
   */
  private int supportedIndirectionLevel = 1;

  /**
   * Returns true when we are able to produce a variable<CType> from this expression.
   * With this method we are able to control which expressions we handle and
   * which we just create variables for.
   * @param exp the expression.
   * @param level the current level of indirection.
   * @return true if we can create a variable from this expression.
   */
  private boolean isSupportedExpression (CExpression exp, int level) {
    if (level > supportedIndirectionLevel) {
      return false;
    }

    if (exp instanceof CIdExpression) {
      return true;
    } else if (handleFieldAccess && exp instanceof CFieldReference) {
      CFieldReference fexp = (CFieldReference)exp;
      return isSupportedExpression(getRealFieldOwner(fexp), level);
    } else if (exp instanceof CCastExpression) {
      CCastExpression cexp = (CCastExpression)exp;
      return isSupportedExpression(cexp.getOperand(), level);
    } else if (exp instanceof CUnaryExpression) {
      CUnaryExpression uexp = (CUnaryExpression)exp;
      UnaryOperator op = uexp.getOperator();
      return
          (op == UnaryOperator.AMPER || op == UnaryOperator.STAR) &&
          isSupportedExpression(uexp.getOperand(), level + 1);
    }

    return false;
  }

  /**
   * Returns true when we are able to produce a variable<CType> from this expression.
   * With this method we are able to control which expressions we handle and
   * which we just create variables for.
   * @param exp the expression
   * @return true if we can create a variable from this expression.
   */
  private boolean isSupportedExpression(CExpression exp) {
    return isSupportedExpression(exp, 0);
  }

  /**
   * We call this method for unsupported Expressions and just make a new Variable.
   */
  private Formula makeVariableUnsafe(CExpression exp, String function, SSAMapBuilder ssa, boolean makeFresh) {

    // We actually support this expression?
    assert (!isSupportedExpression(exp))
       : "A supported Expression is handled as unsupported!";

    if (makeFresh) {
      log(Level.WARNING, "Program contains array, or pointer (multiple level of indirection), or field (enable handleFieldAccess and handleFieldAliasing) access; analysis is imprecise in case of aliasing.");
      logDebug("Assigning to ", exp);
    } else {
      warnUnsafeVar(exp);
    }

    String var = scoped(exprToVarName(exp), function);
    return makeVariable(var, exp.getExpressionType(), ssa, makeFresh);
  }


  /**
   * This class tracks constraints which are created during AST traversal but
   * cannot be applied at the time of creation.
   */
  private class Constraints {

    private BooleanFormula constraints = bfmgr.makeBoolean(true);

    private void addConstraint(BooleanFormula pCo) {
      constraints = bfmgr.and(constraints, pCo);
    }

    public BooleanFormula get() {
      return constraints;
    }
  }

  private boolean isTooComplexExpression(CExpression c) {
    try {
      if (!c.accept(tooComplexVisitor)) {
        return false;
      }

      return CtoFormulaTypeUtils.getIndirectionLevel(c) > supportedIndirectionLevel;
    } catch (Exception e) {
      e.printStackTrace();
      throw new AssertionError("No idea what happened", e);
    }
  }

  private class ExpressionToFormulaVisitor extends DefaultCExpressionVisitor<Formula, UnrecognizedCCodeException> {

    protected final CFAEdge       edge;
    protected final String        function;
    protected final SSAMapBuilder ssa;
    protected final Constraints   constraints;

    public ExpressionToFormulaVisitor(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      edge = pEdge;
      function = pFunction;
      ssa = pSsa;
      constraints = pCo;
    }

    @Override
    protected Formula visitDefault(CExpression exp)
        throws UnrecognizedCCodeException {
      return makeVariableUnsafe(exp, function, ssa, false);
    }

    @Override
    public Formula visit(CBinaryExpression exp) throws UnrecognizedCCodeException {
      BinaryOperator op = exp.getOperator();
      CExpression e1 = exp.getOperand1();
      CExpression e2 = exp.getOperand2();

      // these operators expect numeric arguments
      CType t1 = e1.getExpressionType();
      CType t2 = e2.getExpressionType();
      CType returnType = exp.getExpressionType();
      FormulaType<?> returnFormulaType = getFormulaTypeFromCType(returnType);

      Formula f1 = e1.accept(this);
      Formula f2 = e2.accept(this);
      CType promT1 = getPromotedCType(t1);
      f1 = makeCast(t1, promT1, f1);
      CType promT2 = getPromotedCType(t2);
      f2 = makeCast(t2, promT2, f2);

      CType implicitType;
      // FOR SHIFTS: The type of the result is that of the promoted left operand. (6.5.7 3)
      if (op == BinaryOperator.SHIFT_LEFT || op == BinaryOperator.SHIFT_RIGHT) {
        implicitType = promT1;

        // TODO: This is probably not correct as we only need the right formula-type but not a cast
        f2 = makeCast(promT2, promT1, f2);

        // UNDEFINED: When the right side is negative the result is not defined
      } else {
        implicitType = getImplicitCType(promT1, promT2);
        f1 = makeCast(promT1, implicitType, f1);
        f2 = makeCast(promT2, implicitType, f2);
      }

      boolean signed = isSignedType(implicitType);

      Formula ret;
      switch (op) {
      case PLUS:
        ret = fmgr.makePlus(f1, f2);
        break;
      case MINUS:
        ret =  fmgr.makeMinus(f1, f2);
        break;
      case MULTIPLY:
        ret =  fmgr.makeMultiply(f1, f2);
        break;
      case DIVIDE:
        ret =  fmgr.makeDivide(f1, f2, signed);
        break;
      case MODULO:
        ret =  fmgr.makeModulo(f1, f2, signed);
        break;
      case BINARY_AND:
        ret =  fmgr.makeAnd(f1, f2);
        break;
      case BINARY_OR:
        ret =  fmgr.makeOr(f1, f2);
        break;
      case BINARY_XOR:
        ret =  fmgr.makeXor(f1, f2);
        break;
      case SHIFT_LEFT:

        // NOTE: The type of the result is that of the promoted left operand. (6.5.7 3)
        ret =  fmgr.makeShiftLeft(f1, f2);
        break;
      case SHIFT_RIGHT:
        // NOTE: The type of the result is that of the promoted left operand. (6.5.7 3)
        ret =  fmgr.makeShiftRight(f1, f2, signed);
        break;

      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL:
      case EQUALS:
      case NOT_EQUALS:
      {
        BooleanFormula result;
        switch (op){
          case GREATER_THAN:
            result= fmgr.makeGreaterThan(f1, f2, signed);
            break;
          case GREATER_EQUAL:
            result= fmgr.makeGreaterOrEqual(f1, f2, signed);
            break;
          case LESS_THAN:
            result= fmgr.makeLessThan(f1, f2, signed);
            break;
          case LESS_EQUAL:
            result= fmgr.makeLessOrEqual(f1, f2, signed);
            break;
          case EQUALS:
            result= fmgr.makeEqual(f1, f2);
            break;
          case NOT_EQUALS:
            result= bfmgr.not(fmgr.makeEqual(f1, f2));
            break;
          default:
            throw new AssertionError();
        }
        ret = bfmgr.ifTrueThenOneElseZero(returnFormulaType, result);
        break;
      }
      default:
        throw new UnrecognizedCCodeException("Unknown binary operator", edge, exp);

      }

      if (returnFormulaType != fmgr.getFormulaType(ret)) {
        // Could be because both types got promoted
        if (!areEqual(promT1, t1) && !areEqual(promT2, t2)) {
          // We have to cast back to the return type
          ret = makeCast(implicitType, returnType, ret);
        }
      }

      assert returnFormulaType == fmgr.getFormulaType(ret)
           : "Returntype and Formulatype do not match in visit(CBinaryExpression)";
      return ret;
    }




    @Override
    public Formula visit(CCastExpression cexp) throws UnrecognizedCCodeException {
      Formula operand = cexp.getOperand().accept(this);
      return makeCast(cexp, operand);
    }

    @Override
    public Formula visit(CIdExpression idExp) {

      if (idExp.getDeclaration() instanceof CEnumerator) {
        CEnumerator enumerator = (CEnumerator)idExp.getDeclaration();
        CType t = idExp.getExpressionType();
        if (enumerator.hasValue()) {
          return fmgr.makeNumber(getFormulaTypeFromCType(t), enumerator.getValue());
        } else {
          // We don't know the value here, but we know it is constant.
          return makeConstant(enumerator.getName(), t, ssa);
        }
      }

      return makeVariable(scopedIfNecessary(idExp, ssa, function), ssa);
    }

    @Override
    public Formula visit(CFieldReference fExp) throws UnrecognizedCCodeException {
      if (handleFieldAccess) {
        CExpression fieldOwner = getRealFieldOwner(fExp);
        Formula f = fieldOwner.accept(this);
        return accessField(fExp, f);
      }

      CExpression fieldRef = fExp.getFieldOwner();
      if (fieldRef instanceof CIdExpression) {
        CSimpleDeclaration decl = ((CIdExpression) fieldRef).getDeclaration();
        if (decl instanceof CDeclaration && ((CDeclaration)decl).isGlobal()) {
          // this is the reference to a global field variable

          // we can omit the warning (no pointers involved),
          // and we don't need to scope the variable reference
          return makeVariable(exprToVarName(fExp), fExp.getExpressionType(), ssa);
        }
      }

      return super.visit(fExp);
    }


    @Override
    public Formula visit(CCharLiteralExpression cExp) throws UnrecognizedCCodeException {
      // we just take the byte value
      FormulaType<?> t = getFormulaTypeFromCType(cExp.getExpressionType());
      return fmgr.makeNumber(t, cExp.getCharacter());
    }

    @Override
    public Formula visit(CIntegerLiteralExpression iExp) throws UnrecognizedCCodeException {
      FormulaType<?> t = getFormulaTypeFromCType(iExp.getExpressionType());
      return fmgr.makeNumber(t, iExp.getValue().longValue());
    }

    @Override
    public Formula visit(CFloatLiteralExpression fExp) throws UnrecognizedCCodeException {
      FormulaType<?> t = getFormulaTypeFromCType(fExp.getExpressionType());
      // TODO: Check if this is actually correct
      return fmgr.makeNumber(t,fExp.getValue().longValue());
    }

    private FunctionFormulaType<BitvectorFormula> stringUfDecl;
    @Override
    public Formula visit(CStringLiteralExpression lexp) throws UnrecognizedCCodeException {
      // we create a string constant representing the given
      // string literal
      String literal = lexp.getValue();
      BitvectorFormula result = stringLitToFormula.get(literal);

      if (result == null) {
        if (stringUfDecl == null) {
          FormulaType<BitvectorFormula> pointerType =
              efmgr.getFormulaType(machineModel.getSizeofPtr() * machineModel.getSizeofCharInBits());
          stringUfDecl =
              ffmgr.createFunction(
                  "__string__", pointerType, FormulaType.RationalType);
        }

        // generate a new string literal. We generate a new UIf
        int n = nextStringLitIndex++;
        result = ffmgr.createUninterpretedFunctionCall(
            stringUfDecl, ImmutableList.of(nfmgr.makeNumber(n)));
        stringLitToFormula.put(literal, result);
      }

      return result;
    }

    @Override
    public Formula visit(CUnaryExpression exp) throws UnrecognizedCCodeException {
      CExpression operand = exp.getOperand();
      UnaryOperator op = exp.getOperator();
      switch (op) {
      case PLUS:
      case MINUS:
      case TILDE: {
        // Handle Integer Promotion
        CType t = operand.getExpressionType();
        CType promoted = getPromotedCType(t);
        Formula operandFormula = operand.accept(this);
        operandFormula = makeCast(t, promoted, operandFormula);
        Formula ret;
        if (op == UnaryOperator.PLUS) {
          ret = operandFormula;
        } else if (op == UnaryOperator.MINUS) {
          ret = fmgr.makeNegate(operandFormula);
        } else {
          assert op == UnaryOperator.TILDE
                : "This case should be impossible because of switch";
          ret = fmgr.makeNot(operandFormula);
        }

        CType returnType = exp.getExpressionType();
        FormulaType<?> returnFormulaType = getFormulaTypeFromCType(returnType);
        assert returnFormulaType == fmgr.getFormulaType(ret)
              : "Returntype and Formulatype do not match in visit(CUnaryExpression)";
        return ret;
      }

      case NOT: {
        Formula f = operand.accept(this);
        BooleanFormula term = fmgr.toBooleanFormula(f);
        return bfmgr.ifTrueThenOneElseZero(getFormulaTypeFromCType(exp.getExpressionType()), bfmgr.not(term));
      }

      case AMPER:
      case STAR:
        return visitDefault(exp);

      case SIZEOF:
        if (exp.getOperand() instanceof CIdExpression) {
          CType lCType =
              ((CIdExpression) exp.getOperand()).getExpressionType();
          return handleSizeof(exp, lCType);
        } else {
          return visitDefault(exp);
        }

      default:
        throw new UnrecognizedCCodeException("Unknown unary operator", edge, exp);
      }
    }

    @Override
    public Formula visit(CTypeIdExpression tIdExp)
        throws UnrecognizedCCodeException {

      if (tIdExp.getOperator() == TypeIdOperator.SIZEOF) {
        CType lCType = tIdExp.getType();
        return handleSizeof(tIdExp, lCType);
      } else {
        return visitDefault(tIdExp);
      }
    }

    private Formula handleSizeof(CExpression pExp, CType pCType)
        throws UnrecognizedCCodeException {
      return fmgr.makeNumber(
          CtoFormulaConverter.this
            .getFormulaTypeFromCType(pExp.getExpressionType()),
          getSizeof(pCType));
    }
  }

  private class ExpressionToFormulaVisitorUIF extends ExpressionToFormulaVisitor {

    public ExpressionToFormulaVisitorUIF(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(pEdge, pFunction, pSsa, pCo);
    }

    private Formula makeUIF(String name, CType type, SSAMapBuilder ssa, Formula... args) {
      FormulaList l = new AbstractFormulaList(args);
      int idx = ssa.getIndex(name, l);
      if (idx <= 0) {
        logger.log(Level.ALL, "DEBUG_3",
            "WARNING: Auto-instantiating lval: ", name, "(", l, ")");
        idx = 1;
        ssa.setIndex(name, l, type, idx);
      }
      return ffmgr.createFuncAndCall(name, idx, getFormulaTypeFromCType(type), Arrays.asList( args) );
    }

    @Override
    public Formula visit(CArraySubscriptExpression aexp) throws UnrecognizedCCodeException {
      CExpression arrexp = aexp.getArrayExpression();
      CExpression subexp = aexp.getSubscriptExpression();
      Formula aterm = arrexp.accept(this);
      Formula sterm = subexp.accept(this);

      String ufname = OP_ARRAY_SUBSCRIPT;
      return makeUIF(ufname, aexp.getExpressionType(), ssa, aterm, sterm);
    }

    @Override
    public Formula visit(CFieldReference fexp) throws UnrecognizedCCodeException {
      String field = fexp.getFieldName();
      CExpression owner = getRealFieldOwner(fexp);
      Formula term = owner.accept(this);

      String tpname = getTypeName(owner.getExpressionType());
      String ufname = ".{" + tpname + "," + field + "}";

      // see above for the case of &x and *x
      return makeUIF(ufname, fexp.getExpressionType(), ssa, term);
    }

    @Override
    public Formula visit(CUnaryExpression exp) throws UnrecognizedCCodeException {
      UnaryOperator op = exp.getOperator();
      switch (op) {
      case AMPER:
      case STAR:
        String opname;
        if (op == UnaryOperator.AMPER) {
          opname = OP_ADDRESSOF_NAME;
        } else {
          opname = OP_STAR_NAME;
        }
        Formula term = exp.getOperand().accept(this);

        CType expType = exp.getExpressionType();

        // PW make SSA index of * independent from argument
        int idx = getIndex(opname, expType, ssa);
        //int idx = getIndex(
        //    opname, term, ssa, absoluteSSAIndices);

        // build the  function corresponding to this operation.

        return ffmgr.createFuncAndCall(
            opname, idx, getFormulaTypeFromCType(expType), ImmutableList.of( term ));

      default:
        return super.visit(exp);
      }
    }
  }

  private class ExpressionToFormulaVisitorPointers extends
      ExpressionToFormulaVisitor {

    public ExpressionToFormulaVisitorPointers(CFAEdge pEdge, String pFunction,
        SSAMapBuilder pSsa, Constraints pCo) {
      super(pEdge, pFunction, pSsa, pCo);
    }

    @Override
    public Formula visit(CUnaryExpression exp)
        throws UnrecognizedCCodeException {
      UnaryOperator op = exp.getOperator();

      switch (op) {
      case AMPER:
        return makeAddressVariable(exp, function);

      case STAR:
        // *tmp or *(tmp->field) or *(s.a)
        if (isSupportedExpression(exp)) {
          Variable fieldPtrMask  = scopedIfNecessary(exp, ssa, function);
          Formula f = makeVariable(fieldPtrMask, ssa);

          // *((type*)tmp) or *((type*)(tmp->field)) or *((type*)(s.a))
          if (exp.getOperand() instanceof CCastExpression) {
            CCastExpression cast = (CCastExpression) exp.getOperand();
            // Use fieldPtrMask.getType because of possible type guessing.
            f = makeExtractOrConcatNondet(fieldPtrMask.getType(), dereferencedType(cast.getExpressionType()), f);
          }
          return f;
        }

        //$FALL-THROUGH$
      default:
        return super.visit(exp);
      }
    }

    private Formula makeAddressVariable(CUnaryExpression exp, String function)
        throws UnrecognizedCCodeException {

        CExpression operand = removeCast(exp.getOperand());
        UnaryOperator op = exp.getOperator();

        if (op != UnaryOperator.AMPER || !isSupportedExpression(operand)) {
          return super.visitDefault(exp);
        }

        return makeMemLocationVariable(operand, function);
    }

    /**
     * Returns a Formula representing the memory location of a given IdExpression.
     * Ensures that the location is unique and not 0.
     *
     * @param function The scope of the variable.
     */
    private Formula makeMemLocationVariable(CExpression exp, String function) {
      Variable v =
          scopedIfNecessary(exp, ssa, function);
      Variable addressVariable = makeMemoryLocationVariable(v);

      // a variable address is always initialized, not 0 and cannot change
      if (ssa.getIndex(addressVariable.getName()) == VARIABLE_UNSET) {
        List<Variable> oldMemoryLocations = getAllMemoryLocationsFromSsaMap(ssa);
        Formula newMemoryLocation = makeConstant(addressVariable, ssa);

        // a variable address that is unknown is different from all previously known addresses
        for (Variable memoryLocation : oldMemoryLocations) {
          Formula oldMemoryLocation = makeConstant( memoryLocation, ssa);
          BooleanFormula addressInequality = bfmgr.not(fmgr.makeEqual(newMemoryLocation, oldMemoryLocation));

          constraints.addConstraint(addressInequality);
        }

        // a variable address is not 0
        BooleanFormula notZero = bfmgr.not(fmgr.makeEqual(newMemoryLocation, fmgr.makeNumber(getFormulaTypeFromCType(addressVariable.getType()), 0)));
        constraints.addConstraint(notZero);
      }

      return makeConstant(addressVariable, ssa);
    }
  }

  private class RightHandSideToFormulaVisitor extends
      ForwardingCExpressionVisitor<Formula, UnrecognizedCCodeException>
      implements CRightHandSideVisitor<Formula, UnrecognizedCCodeException> {

    protected final CFAEdge       edge;
    protected final String        function;
    protected final SSAMapBuilder ssa;
    protected final Constraints   constraints;

    public RightHandSideToFormulaVisitor(String pFunction, SSAMapBuilder pSsa, Constraints pCo, CFAEdge pEdge) {
      super(getCExpressionVisitor(pEdge, pFunction, pSsa, pCo));
      edge = pEdge;
      function = pFunction;
      ssa = pSsa;
      constraints = pCo;
    }

    @Override
    public Formula visit(CFunctionCallExpression fexp) throws UnrecognizedCCodeException {

      CExpression fn = fexp.getFunctionNameExpression();
      List<CExpression> pexps = fexp.getParameterExpressions();
      String func;
      CType expType = fexp.getExpressionType();
      if (fn instanceof CIdExpression) {
        func = ((CIdExpression)fn).getName();
        if (func.equals(ASSUME_FUNCTION_NAME) && pexps.size() == 1) {

          BooleanFormula condition = fmgr.toBooleanFormula(pexps.get(0).accept(this));
          constraints.addConstraint(condition);

          return makeFreshVariable(func, expType, ssa);

        } else if (nondetFunctions.contains(func)
            || nondetFunctionsPattern.matcher(func).matches()) {
          // function call like "random()"
          // ignore parameters and just create a fresh variable for it
          return makeFreshVariable(func, expType, ssa);

        } else if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), edge, fexp);

        } else if (!PURE_EXTERNAL_FUNCTIONS.contains(func)) {
          if (pexps.isEmpty()) {
            // function of arity 0
            log(Level.INFO, "Assuming external function " + func + " to be a constant function.");
          } else {
            log(Level.INFO, "Assuming external function " + func + " to be a pure function.");
          }
        }
      } else {
        log(Level.WARNING, getLogMessage("Ignoring function call through function pointer", fexp));
        func = "<func>{" + function + "::" + fn.toASTString() + "}";
      }

      if (pexps.isEmpty()) {
        // This is a function of arity 0 and we assume its constant.
        return makeConstant(func, expType, ssa);

      } else {
        CFunctionDeclaration declaration = fexp.getDeclaration();
        if (declaration == null) {
          // This should not happen
          log(Level.WARNING, "Cant get declaration of function. Ignoring the call (" + fexp.toASTString() + ").");
          return makeFreshVariable(func, expType, ssa); // BUG when expType = void
        }

        List<CType> paramTypes = declaration.getType().getParameters();
        func += "{" + paramTypes.size() + "}"; // add #arguments to function name to cope with varargs functions

        List<Formula> args = new ArrayList<>(pexps.size());
        Iterator<CType> it1 = paramTypes.iterator();
        Iterator<CExpression> it2 = pexps.iterator();
        while (it1.hasNext()) {

          CType paramType= it1.next();
          CExpression pexp;
          if (it2.hasNext()) {
            pexp  = it2.next();
          } else {
            throw new IllegalArgumentException("To the function " + declaration.toASTString() + " were given less Arguments than it has in its declaration!");
          }

           Formula arg = pexp.accept(this);
           args.add(makeCast(pexp.getExpressionType(), paramType, arg));
        }

        if (it2.hasNext()) {
          log(Level.WARNING, "Ignoring call to " + declaration.toASTString() + " because of varargs");
          return makeFreshVariable(func, expType, ssa);
        }

        CType returnType = getReturnType(fexp, edge);
        FormulaType<?> t = getFormulaTypeFromCType(returnType);
        return ffmgr.createFuncAndCall(func, t, args);
      }
    }
  }

  private class StatementToFormulaVisitor extends RightHandSideToFormulaVisitor implements CStatementVisitor<BooleanFormula, UnrecognizedCCodeException> {

    public StatementToFormulaVisitor(String pFunction, SSAMapBuilder pSsa, Constraints pConstraints, CFAEdge edge) {
      super(pFunction, pSsa, pConstraints, edge);
    }

    @Override
    public BooleanFormula visit(CExpressionStatement pIastExpressionStatement) {
      // side-effect free statement, ignore
      return bfmgr.makeBoolean(true);
    }

    /**
     * Creates formulas for the given assignment (left and right side).
     * And returns all formulas to be able to create aliasing formulas.
     * @param assignment the assignment to process
     * @return a triple of right, left and assignment formula (in this order which is chronological)
     * @throws UnrecognizedCCodeException
     */
    public Triple<Formula, Formula, BooleanFormula> visitAssignment(CAssignment assignment) throws UnrecognizedCCodeException {
      Formula r = assignment.getRightHandSide().accept(this);
      Formula l = buildLvalueTerm(assignment.getLeftHandSide(), edge, function, ssa, constraints);
      r = makeCast(
            assignment.getRightHandSide().getExpressionType(),
            assignment.getLeftHandSide().getExpressionType(),
            r);

      BooleanFormula a = fmgr.assignment(l, r);
      return Triple.of(r, l, a);
    }

    public BooleanFormula visit(CAssignment assignment) throws UnrecognizedCCodeException {
      // No need to alias anything so just return the assignment
      return
            visitAssignment(assignment).getThird();
    }

    @Override
    public BooleanFormula visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) throws UnrecognizedCCodeException {
      return visit((CAssignment)pIastExpressionAssignmentStatement);
    }

    @Override
    public BooleanFormula visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) throws UnrecognizedCCodeException {
      return visit((CAssignment)pIastFunctionCallAssignmentStatement);
    }

    @Override
    public BooleanFormula visit(CFunctionCallStatement fexp) throws UnrecognizedCCodeException {
      // this is an external call
      // visit expression in order to print warnings if necessary
      visit(fexp.getFunctionCallExpression());
      return bfmgr.makeBoolean(true);
    }
  }

  private class StatementToFormulaVisitorPointers extends StatementToFormulaVisitor {

    public StatementToFormulaVisitorPointers(String pFunction,
        SSAMapBuilder pSsa, Constraints pConstraints, CFAEdge edge) {
      super(pFunction, pSsa, pConstraints, edge);
    }

    @Override
    public Formula visit(CFunctionCallExpression fexp) throws UnrecognizedCCodeException {
      // handle malloc
      CExpression fn = fexp.getFunctionNameExpression();
      if (fn instanceof CIdExpression) {
        String fName = ((CIdExpression)fn).getName();

        if (memoryAllocationFunctions.contains(fName)) {

          CType expType = fexp.getExpressionType();
          if (!(expType instanceof CPointerType)) {
            log(Level.WARNING, "Memory allocation function ("+fName+") with invalid return type (" + expType +"). Missing includes or file not preprocessed?");
          }

          FormulaType<?> t = getFormulaTypeFromCType(expType);

          // for now all parameters are ignored
          List<Variable> memoryLocations = getAllMemoryLocationsFromSsaMap(ssa);

          String mallocVarName = makeFreshMallocVariableName(expType);
          Formula mallocVar = makeConstant(mallocVarName, expType, ssa);

          // we must distinguish between two cases:
          // either the result is 0 or it is different from all other memory locations
          // (m != 0) => for all memory locations n: m != n
          BooleanFormula ineq = bfmgr.makeBoolean(true);
          for (Variable ml : memoryLocations) {
            Formula n = makeConstant(ml, ssa);

            BooleanFormula notEqual = bfmgr.not(fmgr.makeEqual(n, mallocVar));
            ineq = bfmgr.and(notEqual, ineq);
          }

          Formula nullFormula = fmgr.makeNumber(t, 0);
          BooleanFormula notEqual = bfmgr.not(fmgr.makeEqual(mallocVar, nullFormula));
          BooleanFormula implication = bfmgr.implication(notEqual, ineq);

          constraints.addConstraint(implication);
          return mallocVar;
        }
      }

      return super.visit(fexp);
    }


    @Override
    public BooleanFormula visit(CAssignment assignment)
        throws UnrecognizedCCodeException {
      CExpression left = removeCast(assignment.getLeftHandSide());

      if (left instanceof CIdExpression) {
        // p = ...
        return handleDirectAssignment(assignment);

      } else if (left instanceof CUnaryExpression
          && ((CUnaryExpression) left).getOperator() == UnaryOperator.STAR) {
        // *p = ...
        return handleIndirectAssignment(assignment);

      } else if (handleFieldAccess && left instanceof CFieldReference) {
        // p->t = ...
        // p.s = ...

        CFieldReference fieldRef = (CFieldReference)left;
        if (isSupportedExpression(left)) {
          if (!isIndirectFieldReference(fieldRef)) {
            // p.s = ... which we handle quite similar to the p = ... case
            return handleDirectAssignment(assignment);
          } else {
            // p->s = ... which we handle quite similar to the *p = ... case
            return handleIndirectAssignment(assignment);
          }
        }
      }

      warnToComplex(assignment);
      return super.visit(assignment);
    }

    /**
     * An indirect assignment does not change the value of the variable on the
     * left hand side. Instead it changes the value stored in the memory location
     * aliased on the left hand side.
     */
    private BooleanFormula handleIndirectAssignment(CAssignment pAssignment)
        throws UnrecognizedCCodeException {
      CExpression lExpr = removeCast(pAssignment.getLeftHandSide());

      assert (lExpr instanceof CUnaryExpression || (lExpr instanceof CFieldReference && isIndirectFieldReference((CFieldReference)lExpr)))
          : "Unsupported leftHandSide in Indirect Assignment";


      CUnaryExpression leftSide;
      if (lExpr instanceof CUnaryExpression) {
        // the following expressions are supported by cil:
        // *p = a;
        // *p = 1;
        // *p = a | b; (or any other binary statement)
        // *p = function();
        // *s.t = ...
        leftSide = (CUnaryExpression) lExpr;
      } else {
        // p->s = ... is the same as (*p).s = ... which we see as *p = ... (because the bitvector was changed)
        CFieldReference l = (CFieldReference) lExpr;
        assert isIndirectFieldReference(l) : "No pointer-dereferencing in handleIndirectFieldAssignment";

        leftSide = (CUnaryExpression)getRealFieldOwner(l);
      }

      assert leftSide.getOperator() == UnaryOperator.STAR  : "Expected pointer dereferencing";

      if (!isSupportedExpression(leftSide)) {
        // TODO: *(a + 2) = b
        // NOTE: We do not support multiple levels of indirection (**t)
        // *(p->t) = ...
        warnToComplex(leftSide);
        return super.visit(pAssignment);
      }

      //SSAMapBuilder oldssa = new SSAMapBuilder(ssa.build());
      Variable lVarName = scopedIfNecessary(leftSide.getOperand(), ssa, function);
      Variable lPtrVarName = makePointerMask(lVarName, ssa);
      Formula lVar = makeVariable(lVarName, ssa);
      Formula lPtrVar = makeVariable(lPtrVarName, ssa);

      // It could be that we have to fill the structure with nondet bits, because of a cast
      if (leftSide.getOperand() instanceof CCastExpression) {
        CCastExpression ptrCast = (CCastExpression)leftSide.getOperand();

        lPtrVar = makeExtractOrConcatNondet(
            lPtrVarName.getType(), // Possible type guessing
            dereferencedType(ptrCast.getExpressionType()),
            lPtrVar);
      }

      CRightHandSide r = pAssignment.getRightHandSide();

      // NOTE: When doDeepUpdate is false rVarName will only be used for comparison in updateAllPointers
      // So this is OK.
      Variable rVarName = Variable.create(null, r.getExpressionType());
      Formula rPtrVar = null;
      boolean doDeepUpdate = false;
      // We need r to be one level lower than we support, as we need its pointer mask for a deep update
      if (r instanceof CExpression &&
          isSupportedExpression((CExpression) r, 1) &&
          hasRepresentableDereference((CExpression) r)) {
        rVarName = scopedIfNecessary((CExpression) r, ssa, function);
        rPtrVar = makeVariable(makePointerMask(rVarName, ssa), ssa);
        doDeepUpdate = true;
      }

      // assignment (first level) -- uses superclass
      Triple<Formula, Formula, BooleanFormula> assignmentFormulas = visitAssignment(pAssignment);
      Formula rightVariable = assignmentFormulas.getFirst();
      BooleanFormula assignments = assignmentFormulas.getThird();

      if (!(lExpr instanceof CUnaryExpression)) {
        // NOTE: rightVariable is only the changed field, set it to the complete bitvector
        rightVariable = replaceField((CFieldReference) lExpr, lPtrVar, rightVariable);

        // We can't make a pointermask from the right side.
        doDeepUpdate = false;
        rVarName = Variable.create(null, r.getExpressionType()); // Only for comparison
      }

      // To ensure tracking we have to update all pointers which are equal to p
      // (now they point to the right side)
      // update all pointer variables (they might have a new value)
      // every variable aliased to the left hand side,
      // has its pointer set to the right hand side,
      // for all other pointer variables, the index is updated
      List<Variable> ptrVarNames = getAllPointerVariablesFromSsaMap(ssa);
      for (Variable ptrVarName : ptrVarNames) {
        Variable varName = removePointerMaskVariable(ptrVarName);

        if (!varName.equals(lVarName) && !varName.equals(rVarName)) {
          Formula var = makeVariable(varName, ssa);

          Formula oldPtrVar = makeVariable(ptrVarName, ssa);
          makeFreshIndex(ptrVarName.getName(), ptrVarName.getType(), ssa);
          Formula newPtrVar = makeVariable(ptrVarName, ssa);
          BooleanFormula condition;
          if (isDereferenceType(ptrVarName.getType())) {
            // Variable from a aliasing formula, they are always to small, so fill up with nondet bits to make a pointer.
            condition = makeNondetAssignment(var, lVar);
          } else {
            assert fmgr.getFormulaType(var) == fmgr.getFormulaType(lVar)
                : "Make sure all memory variables are pointers! (Did you forget to process your file with cil first or are you missing some includes?)";
            condition = fmgr.makeEqual(var, lVar);
          }
          //BooleanFormula condition = fmgr.makeEqual(var, leftVar);
          BooleanFormula equality = makeNondetAssignment(newPtrVar, rightVariable);

          BooleanFormula indexUpdate = fmgr.assignment(newPtrVar, oldPtrVar);

          BooleanFormula variableUpdate = bfmgr.ifThenElse(condition, equality, indexUpdate);
          constraints.addConstraint(variableUpdate);
        }
      }

      // for all memory addresses also update the aliasing
      // if the left variable is an alias for an address,
      // then the left side is (deep) equal to the right side
      // otherwise update the variables
      List<Variable> memAddresses = getAllMemoryLocationsFromSsaMap(ssa);
      if (doDeepUpdate) {
        for (Variable memAddress : memAddresses) {
          Variable varName = getVariableFromMemoryAddress(memAddress);
          //String varName = getVariableNameFromMemoryAddress(memAddress.getName());

          if (!varName.equals(lVarName) && hasRepresentableDereference(varName)) {
            // we assume that cases like the following are illegal and do not occur
            // (gcc 4.6 gives an error):
            // p = &p;
            // *p = &a;

            Formula memAddressVar = makeVariable(memAddress, ssa);

            // *m_old
            Formula oldVar = makeVariable(varName, ssa);
            Variable oldPtrVarName = makePointerMask(varName, ssa);
            // **m_old
            Formula oldPtrVar = makeVariable(oldPtrVarName, ssa);

            makeFreshIndex(varName.getName(), varName.getType(), ssa);

            // *m_new
            Formula newVar = makeVariable(varName, ssa);
            Variable newPtrVarName = makePointerMask(varName, ssa);
            // **m_new
            Formula newPtrVar = makeVariable(newPtrVarName, ssa);
            removeOldPointerVariablesFromSsaMap(newPtrVarName.getName(), ssa);

            // Let right be r and left *p and m the current memory-address
            // We create the formula of the comments above.

            // *m_new = r (they don't need to have the same types so use makeNondetAssignment)
            BooleanFormula varEquality = makeNondetAssignment(newVar, rightVariable);
            // **m_new = *r
            BooleanFormula ptrVarEquality = makeNondetAssignment(newPtrVar, rPtrVar);
            // *m_new = *m_old
            BooleanFormula varUpdate = fmgr.assignment(newVar, oldVar);
            // **m_new = **m_old
            BooleanFormula ptrVarUpdate = fmgr.assignment(newPtrVar, oldPtrVar);

            // p = m
            assert fmgr.getFormulaType(lVar) == fmgr.getFormulaType(memAddressVar)
                : "Make sure all memory variables are pointers! (Did you forget to process your file with cil first or are you missing some includes?)";
            BooleanFormula condition = fmgr.makeEqual(lVar, memAddressVar);

            // **m_new = *r && *m_new = r
            BooleanFormula equality = bfmgr.and(varEquality, ptrVarEquality);
            // *m_new = *m_old && **m_new = **m_old
            BooleanFormula update = bfmgr.and(varUpdate, ptrVarUpdate);

            // if p = m then *m_new = r && **m_new = *r else *m_new = *m_old && **m_new = **m_old
            // means when the pointer equals to our current memory address we
            // know that this memory address contains the right side.
            // If not we know that this memory address was unchanged (same as before).
            BooleanFormula variableUpdate = bfmgr.ifThenElse(condition, equality, update);
            constraints.addConstraint(variableUpdate);
          }
        }

      } else {
        // no deep update of pointers required
        Map<String, Formula> memberMaskMap = null;
        CType expType = simplifyType(leftSide.getExpressionType());
        if (handleFieldAliasing) {
          // Read comment below.
          if (expType instanceof CCompositeType) {
            CCompositeType structType = (CCompositeType)expType;
            memberMaskMap = new Hashtable<>();
            for (CCompositeTypeMemberDeclaration member : structType.getMembers()) {
              // TODO: check if we can omit member with a maybePointer call.
              // I think we can't because even when the current member was not used as pointer
              // the same member could be used as pointer on an other variable.
              if (omitNonPointerInFieldAliasing && !CtoFormulaTypeUtils.isPointerType(member.getType())) {
                continue;
              }

              CFieldReference leftField =
                  new CFieldReference(null, member.getType(), member.getName(), leftSide, false);

              Formula g_s = accessField(leftField, rightVariable);
              // From g->s we search *(g->s)
              // Start with nondet bits
              CType maskType = dereferencedType(member.getType());
              int fieldMaskSize = getSizeof(maskType) * machineModel.getSizeofCharInBits();
              Formula content_of_g_s =
                  changeFormulaSize(0, fieldMaskSize, efmgr.makeBitvector(0, 0));

              for (Variable inner_ptrVarName : ptrVarNames) {
                Variable inner_varName = removePointerMaskVariable(inner_ptrVarName);
                if (inner_varName.equals(lVarName) || inner_varName.equals(rVarName)) {
                  continue;
                }

                Formula k = makeVariable(inner_varName, ssa);
                BooleanFormula cond = makeNondetAssignment(k, g_s);

                Formula found = makeVariable(inner_ptrVarName, ssa);
                found = changeFormulaSize(efmgr.getLength((BitvectorFormula) found), fieldMaskSize, (BitvectorFormula) found);

                content_of_g_s = bfmgr.ifThenElse(cond, found, content_of_g_s);
              }

              memberMaskMap.put(member.getName(), content_of_g_s);
            }
          }
        } else {
          warnFieldAliasing();
        }


        for (Variable memAddress : memAddresses) {
          Variable varName = getVariableFromMemoryAddress(memAddress);

          if (varName.equals(lVarName) || !hasRepresentableDereference(varName)) {
            continue;
          }
          // *m_old
          Formula oldVar = makeVariable(varName, ssa);
          makeFreshIndex(varName.getName(), varName.getType(), ssa);
          // *m_new
          Formula newVar = makeVariable(varName, ssa);
          // **m_new
          Variable newPtrVarName = makePointerMask(varName, ssa);
          removeOldPointerVariablesFromSsaMap(newPtrVarName.getName(), ssa);

          // m_new
          Formula memAddressVar = makeVariable(memAddress, ssa);

          assert fmgr.getFormulaType(lVar) == fmgr.getFormulaType(memAddressVar)
              : "Make sure all memory variables are pointers! (Did you forget to process your file with cil first or are you missing some includes?)";
          // p = m_new
          BooleanFormula condition = fmgr.makeEqual(lVar, memAddressVar);
          // *m_new = r
          BooleanFormula equality = makeNondetAssignment(newVar, rightVariable);

          // *m_new = *m_old
          BooleanFormula update = makeNondetAssignment(newVar, oldVar);

          if (memberMaskMap != null) {
            // When we found a address which was changed
            // and if this is a structure, we also have to update
            // the pointer masks of the fields.

            // The content_of_g_s formula doesn't have to be generated for all mem addresses
            // we can do this above for all

            // Currently we are in the statement *g = ...
            // Now say the condition is met and we found a p with p = m_new
            // This basically means we have a variable f with &f = m_new
            // For every changed field s we have to set the pointer mask
            // *(f.s) = *(g->s)
            // the *(g->s) part is the tricky one.
            // To get the content we basically have to search all pointers again.
            // If one pointer k is equal to g->s we can use *k
            // We build the following formula: Let {k_1,...,k_n} = maybepointer
            // if (k_1 = g->s) then *k_1 else
            //    if (k_2 = g->s) then *k_2 else
            //        ... else nondetbits

            // Ok now we have to do something if varname is actually
            CCompositeType structType = (CCompositeType)expType;

            // Note we only handle aliasing for memory addresses which make sense.
            // varname should be the same structure
            if (areEqual(varName.getType(), structType)) {
              for (CCompositeTypeMemberDeclaration member : structType.getMembers()) {
                Formula content_of_g_s = memberMaskMap.get(member.getName());
                if (content_of_g_s == null) {
                  continue;
                }

                CFieldReference leftField =
                    new CFieldReference(null, member.getType(), member.getName(), leftSide, false);

                Variable f_s = makeFieldVariable(varName, leftField, ssa);
                Variable content_of_f_s_Name = makePointerMask(f_s, ssa);

                Formula content_of_f_s_old = makeVariable(content_of_f_s_Name, ssa);
                makeFreshIndex(content_of_f_s_Name.getName(), content_of_f_s_Name.getType(), ssa);
                Formula content_of_f_s_new = makeVariable(content_of_f_s_Name, ssa);
                equality =
                    bfmgr.and(equality, fmgr.makeEqual(content_of_g_s, content_of_f_s_new));
                update =
                    bfmgr.and(update, fmgr.makeEqual(content_of_f_s_old, content_of_f_s_new));
              }
            }
          }

          // if p = m then *m_new = r else *m_new = *m_old
          // means when the pointer equals to our current memory address we
          // know that this memory address contains the right side.
          // If not we know that this memory address was unchanged (same as before).
          BooleanFormula variableUpdate = bfmgr.ifThenElse(condition, equality, update);
          constraints.addConstraint(variableUpdate);
        }
      }

      return assignments;
    }



    private Variable getVariableFromMemoryAddress(Variable pMemAddress) {
      return Variable.create(
          getVariableNameFromMemoryAddress(pMemAddress.getName()),
          dereferencedType(pMemAddress.getType()));
    }

    /** A direct assignment changes the value of the variable on the left side. */
    private BooleanFormula handleDirectAssignment(CAssignment assignment)
        throws UnrecognizedCCodeException {
      CExpression lRawExpr = assignment.getLeftHandSide();
      CExpression lExpr = removeCast(lRawExpr);
      assert(lExpr instanceof CIdExpression
          || (lExpr instanceof CFieldReference && !isIndirectFieldReference((CFieldReference)lExpr)))
          : "We currently can't handle too complex lefthandside-Expressions";

      CRightHandSide right = removeCast(assignment.getRightHandSide());

      Variable leftVarName = scopedIfNecessary(lRawExpr, ssa, function);

      // assignment (first level)
      // Just the assignment formula p = t
      Triple<Formula, Formula, BooleanFormula> assignmentFormulas = visitAssignment(assignment);
      Formula leftVariable = assignmentFormulas.getSecond();
      BooleanFormula firstLevelFormula = assignmentFormulas.getThird();

      // assignment (second level) *p = *t if nessesary
      Variable lVarName = scopedIfNecessary(lExpr, ssa, function);
      BooleanFormula secondLevelFormula = buildDirectSecondLevelAssignment(
          lVarName, right, function, constraints, ssa);

      BooleanFormula assignmentFormula = bfmgr.and(firstLevelFormula, secondLevelFormula);

      updatePointerAliasedTo(leftVarName, leftVariable);


      if (handleFieldAliasing) {
        if (lExpr instanceof CIdExpression) {
          // If the left side is a simple CIdExpression "l = ..." than we have to see if
          // l is a structure and handle pointer aliasing for all fields.

          CType simpleTypes = simplifyType(lRawExpr.getExpressionType());
          if (simpleTypes instanceof CCompositeType) {
            // There are 3 cases:
            // - the right side is of the form *r
            //     for every member we have to emit
            //     ∀p ∈ maybePointer: (p = r->t) ⇒ (l.t = p ∧ *(l.t) = *p)
            // - the right side is of the form r
            //     for every member we have to emit *(s.t) = *(r.t)
            // - the right side is of the form &r, this should not happen
            //     for every member we have to emit *(s.t) = *((&r).t)
            // -> Exactly this does the buildDirectSecondLevelAssignment method for us

            // Note (TODO?): No 2nd level assignment for statements like t = call();
            if (areEqual(right.getExpressionType(), simpleTypes) && right instanceof CExpression) {
              CCompositeType structureType = (CCompositeType) simpleTypes;
              for (CCompositeTypeMemberDeclaration member : structureType.getMembers()) {
                // We pretend to have a assignment of the form
                // l.t = (r).t
                CFieldReference leftField =
                    new CFieldReference(null, member.getType(), member.getName(), lExpr, false);

                CFieldReference rightField =
                    new CFieldReference(null, member.getType(), member.getName(), (CExpression)right, false);
                Variable leftFieldVar = scopedIfNecessary(leftField, ssa, function);
                BooleanFormula secondLevelFormulaForMember = buildDirectSecondLevelAssignment(
                    leftFieldVar, rightField, function, constraints, ssa);
                assignmentFormula = bfmgr.and(assignmentFormula, secondLevelFormulaForMember);

                // Also update all pointers aliased to the field.
                Formula leftFieldFormula = makeVariable(leftFieldVar, ssa);
                updatePointerAliasedTo(leftFieldVar, leftFieldFormula);
              }
            } else {
              log(Level.SEVERE, "Can't handle aliasing of a strange assignment to a structure: " + assignment.toASTString());
            }
          }
        } else {
          CFieldReference fexp = (CFieldReference)lExpr;
          assert !isIndirectFieldReference(fexp)
            : "direct assignment but indirect expression!";
          // If we have a CFieldReference on the left "l.t = ..." than we only have to update
          // the single field.
          // This is what we have done with the above buildDirectSecondLevelAssignment-Call.
          // And from the imaginary right side will exist no pointer so there is nothing to do.

          // Well we have to update all pointers aliased to the structure.
          // Because above we updated the field.
          CExpression owner = getRealFieldOwner(fexp);
          Variable structVar = scopedIfNecessary(owner, ssa, function);
          Formula structFormula = makeVariable(structVar, ssa);
          updatePointerAliasedTo(structVar, structFormula);
        }
      } else {
        warnFieldAliasing();
      }

      return assignmentFormula;
    }

    private void warnFieldAliasing() {
      if (handleFieldAccess) {
        log(Level.WARNING, "You should enable handleFieldAliasing if possible.");
      }
    }

    private void updatePointerAliasedTo(Variable leftVarName, Formula leftVariable) {
      // updates
      if (isKnownMemoryLocation(leftVarName)) {
        Variable leftMemLocationName = makeMemoryLocationVariable(leftVarName);
        Formula leftMemLocation = makeConstant(leftMemLocationName, ssa);

        // update all pointers:
        // if a pointer is aliased to the assigned variable,
        // update that pointer to reflect the new aliasing,
        // otherwise only update the index
        List<Variable> ptrVarNames = getAllPointerVariablesFromSsaMap(ssa);
        for (Variable ptrVarName : ptrVarNames) {
          Variable varName = removePointerMaskVariable(ptrVarName);
          if (!varName.equals(leftVarName)) {
            Formula var = makeVariable(varName, ssa);
            Formula oldPtrVar = makeVariable(ptrVarName, ssa);
            makeFreshIndex(ptrVarName.getName(), ptrVarName.getType(), ssa);
            Formula newPtrVar = makeVariable(ptrVarName, ssa);
            BooleanFormula condition;
            if (isDereferenceType(ptrVarName.getType())) {
              condition = makeNondetAssignment(var, leftMemLocation);
            } else {
              assert fmgr.getFormulaType(var) == fmgr.getFormulaType(leftMemLocation)
                  : "Make sure all memory variables are pointers! (Did you forget to process your file with cil first or are you missing some includes?)";
              condition = fmgr.makeEqual(var, leftMemLocation);
            }
            // leftVariable can be anything
            BooleanFormula equivalence = makeNondetAssignment(newPtrVar, leftVariable);
            BooleanFormula update = fmgr.assignment(newPtrVar, oldPtrVar);

            BooleanFormula constraint = bfmgr.ifThenElse(condition, equivalence, update);
            constraints.addConstraint(constraint);
          }
        }
      }
    }

    /** Returns whether the address of a given variable has been used before. */
    private boolean isKnownMemoryLocation(Variable varName) {
      assert varName.getName() != null;
      List<Variable> memLocations = getAllMemoryLocationsFromSsaMap(ssa);
      Variable memVarName = makeMemoryLocationVariable(varName);
      return memLocations.contains(memVarName);
    }

    /** Returns a new variable name for every malloc call.
     * @param pT */
    private String makeFreshMallocVariableName(CType pT) {
      int idx = ssa.getIndex(MALLOC_COUNTER_VARIABLE_NAME);

      if (idx == VARIABLE_UNSET) {
        idx = VARIABLE_UNINITIALIZED;
      }

      ssa.setIndex(MALLOC_COUNTER_VARIABLE_NAME, pT, idx + 1);
      return MALLOC_VARIABLE_PREFIX + idx;
    }

    /** Returns the variable name of a memory address variable */
    private String getVariableNameFromMemoryAddress(String memoryAddress) {
      assert(memoryAddress.startsWith(MEMORY_ADDRESS_VARIABLE_PREFIX));

      return memoryAddress.substring(MEMORY_ADDRESS_VARIABLE_PREFIX.length());
    }
  }

  private class LvalueVisitor extends
      DefaultCExpressionVisitor<Formula, UnrecognizedCCodeException> {

    protected final CFAEdge       edge;
    protected final String        function;
    protected final SSAMapBuilder ssa;
    protected final Constraints   constraints;

    public LvalueVisitor(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      edge = pEdge;
      function = pFunction;
      ssa = pSsa;
      constraints = pCo;
    }

    @Override
    protected BitvectorFormula visitDefault(CExpression exp) throws UnrecognizedCCodeException {
      throw new UnrecognizedCCodeException("Unknown lvalue", edge, exp);
    }

    @Override
    public Formula visit(CIdExpression idExp) {
      Variable var = scopedIfNecessary(idExp, ssa, function);
      return makeFreshVariable(var.getName(), var.getType(), ssa);
    }

    /**  This method is called when we don't know what else to do. */
    protected Formula giveUpAndJustMakeVariable(CExpression exp) {
      return makeVariableUnsafe(exp, function, ssa, true);
    }


    @Override
    public Formula visit(CUnaryExpression pE) throws UnrecognizedCCodeException {
      return giveUpAndJustMakeVariable(pE);
    }

    @Override
    public Formula visit(CFieldReference fexp) throws UnrecognizedCCodeException {
      if (!handleFieldAccess) {
        CExpression fieldRef = fexp.getFieldOwner();
        if (fieldRef instanceof CIdExpression) {
          CSimpleDeclaration decl = ((CIdExpression) fieldRef).getDeclaration();
          if (decl instanceof CDeclaration && ((CDeclaration)decl).isGlobal()) {
            // this is the reference to a global field variable
            // we don't need to scope the variable reference
            String var = exprToVarName(fexp);

            return makeFreshVariable(var, fexp.getExpressionType(), ssa);
          }
        }
        return giveUpAndJustMakeVariable(fexp);
      }

      // s.a = ...
      // s->b = ...
      // make a new s and return the formula accessing the field
      // as constraint add that all other fields (the rest of the bitvector) remains the same.
      CExpression owner = getRealFieldOwner(fexp);
      // This will just create the formula with the current ssa-index.
      Formula oldStructure = buildTerm(owner, edge, function, ssa, constraints);
      // This will eventually increment the ssa-index and return the new formula.
      Formula newStructure = owner.accept(this);

      // Other fields did not change.
      Formula oldRestS = withoutField(fexp, oldStructure);
      Formula newRestS = withoutField(fexp, newStructure);
      constraints.addConstraint(fmgr.makeEqual(oldRestS, newRestS));

      Formula fieldFormula = accessField(fexp, newStructure);
      return fieldFormula;
    }

    @Override
    public Formula visit(CArraySubscriptExpression pE) throws UnrecognizedCCodeException {
      return giveUpAndJustMakeVariable(pE);
    }
  }

  private class LvalueVisitorUIF extends LvalueVisitor {

    public LvalueVisitorUIF(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(pEdge, pFunction, pSsa, pCo);
    }

    @Override
    public Formula visit(CUnaryExpression uExp) throws UnrecognizedCCodeException {
      UnaryOperator op = uExp.getOperator();
      CExpression operand = uExp.getOperand();
      String opname;
      CType opType = operand.getExpressionType();
      CType result;
      switch (op) {
      case AMPER:
        opname = OP_ADDRESSOF_NAME;
        result = new CPointerType(false, false, opType);
        break;
      case STAR:
        opname = OP_STAR_NAME;
        CPointerType opTypeP = (CPointerType)opType;
        result = opTypeP.getType();
        break;
      default:
        throw new UnrecognizedCCodeException("Invalid unary operator for lvalue", edge, uExp);
      }
      Formula term = buildTerm(operand, edge, function, ssa, constraints);

      FormulaType<?> formulaType = getFormulaTypeFromCType(result);
      // PW make SSA index of * independent from argument
      int idx = makeFreshIndex(opname, result, ssa);
      //int idx = makeLvalIndex(opname, term, ssa, absoluteSSAIndices);

      // build the "updated" function corresponding to this operation.
      // what we do is the following:
      // C            |     MathSAT
      // *x = 1       |     <ptr_*>::2(x) = 1
      // ...
      // &(*x) = 2    |     <ptr_&>::2(<ptr_*>::1(x)) = 2
      return ffmgr.createFuncAndCall(opname, idx, formulaType, ImmutableList.of(term));
    }

    @Override
    public Formula visit(CFieldReference fexp) throws UnrecognizedCCodeException {
      if (!handleFieldAccess) {
        String field = fexp.getFieldName();
        CExpression owner = getRealFieldOwner(fexp);
        Formula term = buildTerm(owner, edge, function, ssa, constraints);

        String tpname = getTypeName(owner.getExpressionType());
        String ufname = ".{" + tpname + "," + field + "}";
        FormulaList args = new AbstractFormulaList(term);


        CType expType = fexp.getExpressionType();
        FormulaType<?> formulaType = getFormulaTypeFromCType(expType);
        int idx = makeLvalIndex(ufname, expType, args, ssa);

        // see above for the case of &x and *x
        return ffmgr.createFuncAndCall(
           ufname, idx, formulaType, ImmutableList.of(term));
      }

      // When handleFieldAccess is true we can handle this case already
      return super.visit(fexp);
    }

    @Override
    public Formula visit(CArraySubscriptExpression aexp) throws UnrecognizedCCodeException {
      CExpression arrexp = aexp.getArrayExpression();
      CExpression subexp = aexp.getSubscriptExpression();
      Formula aterm = buildTerm(arrexp, edge, function, ssa, constraints);
      Formula sterm = buildTerm(subexp, edge, function, ssa, constraints);

      String ufname = OP_ARRAY_SUBSCRIPT;
      FormulaList args = new AbstractFormulaList(aterm, sterm);
      CType expType = aexp.getExpressionType();
      FormulaType<?> formulaType = getFormulaTypeFromCType(expType);
      int idx = makeLvalIndex(ufname, expType, args, ssa);

      return ffmgr.createFuncAndCall(
          ufname, idx, formulaType, ImmutableList.of(aterm, sterm));
    }
  }

  private class LvalueVisitorPointers extends LvalueVisitor {
    public LvalueVisitorPointers(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(pEdge, pFunction, pSsa, pCo);
    }

    @Override
    public Formula visit(CCastExpression e) throws UnrecognizedCCodeException {
      Formula inner = e.getOperand().accept(this);
      return makeCast(e, inner);
    }


    @Override
    public Formula visit(CUnaryExpression pE) throws UnrecognizedCCodeException {
      if (pE.getOperator() == UnaryOperator.STAR) {
        // When the expression is supported we can create a Variable.
        if (isSupportedExpression(pE)) {
          // *a = ...
          Variable ptrVarName = scopedIfNecessary(pE, ssa, function);
          makeFreshIndex(ptrVarName.getName(), ptrVarName.getType(), ssa);
          Formula f = makeVariable(ptrVarName, ssa);

          // *((int*) a) = ...
          if (pE.getOperand() instanceof CCastExpression) {
            CCastExpression cast = (CCastExpression) pE.getOperand();
            // Use ptrVarName.getType() because of possible type guessing
            f = makeExtractOrConcatNondet(ptrVarName.getType(), dereferencedType(cast.getExpressionType()), f);
          }
          return f;
        } else {
          // apparently valid cil output:
          // *(&s.f) = ...
          // *(s->f) = ...
          // *(a+b) = ...

          return giveUpAndJustMakeVariable(pE);
        }
      } else {
        // &f = ... which doesn't make much sense.
        log(Level.WARNING, "Strange addressof operator on the left side:" + pE.toString());
        return super.visit(pE);
      }
    }
  }
}
