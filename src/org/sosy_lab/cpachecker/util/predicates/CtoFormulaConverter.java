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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType.ElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeUtils;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
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

  //names for special variables needed to deal with functions
  private static final String VAR_RETURN_NAME = "__retval__";
  private static final String OP_ADDRESSOF_NAME = "__ptrAmp__";
  private static final String OP_STAR_NAME = "__ptrStar__";
  private static final String OP_ARRAY_SUBSCRIPT = "__array__";
  public static final String NONDET_VARIABLE = "__nondet__";
  public static final String EXPAND_VARIABLE = "__expandVariable__";
  public static final String NONDET_FLAG_VARIABLE = NONDET_VARIABLE + "flag__";

  private static final String POINTER_VARIABLE = "__content_of__";
  private static final Predicate<CharSequence> IS_POINTER_VARIABLE
    = Predicates.containsPattern("\\Q" + POINTER_VARIABLE + "\\E.*\\Q__end\\E");


  /** The prefix used for variables representing memory locations. */
  private static final String MEMORY_ADDRESS_VARIABLE_PREFIX = "__address_of__";
  private static final Predicate<String> IS_MEMORY_ADDRESS_VARIABLE = new Predicate<String>() {
      @Override
      public boolean apply(String pVariable) {
        return pVariable.startsWith(MEMORY_ADDRESS_VARIABLE_PREFIX);
      }
    };

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

  @Option(description = "Handle implicit and explicit casts.")
  private boolean ignoreCasts = true;

  private final Set<String> printedWarnings = new HashSet<>();

  private final Map<String, BitvectorFormula> stringLitToFormula = new HashMap<>();
  private int nextStringLitIndex = 0;

  private final MachineModel machineModel;

  protected final FormulaManagerView fmgr;
  protected final  BooleanFormulaManagerView bfmgr;
  protected final  RationalFormulaManagerView nfmgr;
  protected final  BitvectorFormulaManagerView efmgr;
  protected final  FunctionFormulaManagerView ffmgr;
  protected final LogManager logger;

  private static final int                 VARIABLE_UNSET          = -1;
  private static final int                 VARIABLE_UNINITIALIZED  = 2;

  public CtoFormulaConverter(Configuration config, FormulaManagerView fmgr,
      MachineModel pMachineModel, LogManager logger)
          throws InvalidConfigurationException {
    config.inject(this, CtoFormulaConverter.class);

    this.fmgr = fmgr;
    this.machineModel = pMachineModel;

    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.nfmgr = fmgr.getRationalFormulaManager();
    this.efmgr = fmgr.getBitvectorFormulaManager();
    this.ffmgr = fmgr.getFunctionFormulaManager();
    this.logger = logger;
    nondetFunctionsPattern = Pattern.compile(nondetFunctionsRegexp);
  }

  private void warnUnsafeVar(CExpression exp) {
    logDebug("Unhandled expression treated as free variable", exp);
  }

  private void warnUnsafeAssignment() {
    log(Level.WARNING, "Program contains array, pointer, or field access; analysis is imprecise in case of aliasing.");
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
  /** Looks up the variable name in the current namespace. */
  private Variable<CType> scopedIfNecessary(CIdExpression var, String function) {
    CSimpleDeclaration decl = var.getDeclaration();
    boolean isGlobal = false;
    if (decl instanceof CDeclaration) {
      isGlobal = ((CDeclaration)decl).isGlobal();
    }
    String name;
    if (isGlobal) {
      name = var.getName();
    } else {
      name = scoped(var.getName(), function);
    }

    return Variable.create(name, var.getExpressionType());
  }

  @SuppressWarnings("unchecked")
  protected <T extends Formula> FormulaType<T> getFormulaTypeFromCType(CType type) {
    int byteSize = machineModel.getSizeof(type);

    // byte to bits
    FormulaType<T> t = (FormulaType<T>) efmgr.getFormulaType(byteSize * 8);
    return t;
  }

  @SuppressWarnings("unused")
  private boolean isSignedType(CType pType) {
    if (pType instanceof CSimpleType){
      return !((CSimpleType)pType).isUnsigned();
    }
    // Default behaviour, structs for example
    return false;
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
  private static String exprToVarName(CExpression e) {
    return e.toASTString().replaceAll("[ \n\t]", "");
  }

  private String getTypeName(final CType tp) {

    if (tp instanceof CPointerType) {
      return getTypeName(((CPointerType)tp).getType());

    } else if (tp instanceof CTypedefType) {
      return getTypeName(((CTypedefType)tp).getRealType());

    } else {
      throw new AssertionError("wrong type");
    }
  }

  /**
   * Produces a fresh new SSA index for an assignment
   * and updates the SSA map.
   */
  private int makeFreshIndex(Variable<CType> varName, SSAMapBuilder ssa) {
    int idx = ssa.getIndex(varName);
    if (idx > 0) {
      idx = idx+1;
    } else {
      idx = VARIABLE_UNINITIALIZED; // AG - IMPORTANT!!! We must start from 2 and
      // not from 1, because this is an assignment,
      // so the SSA index must be fresh.
    }
    ssa.setIndex(varName, idx);
    return idx;
  }

  /**
   * This method returns the index of the given variable in the ssa map, if there
   * is none, it creates one with the value 1.
   *
   * @return the index of the variable
   */
  private int getIndex(Variable<CType> var, SSAMapBuilder ssa) {
    int idx = ssa.getIndex(var);
    if (idx <= 0) {
      logger.log(Level.ALL, "WARNING: Auto-instantiating variable:", var);
      idx = 1;
      ssa.setIndex(var, idx);
    }
    return idx;
  }

  /**
   * Produces a fresh new SSA index for the left-hand side of an assignment
   * and updates the SSA map.
   */
  private int makeLvalIndex(Variable<CType> varName, FormulaList args, SSAMapBuilder ssa) {
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
    ssa.setIndex(varName, args, idx);
    return idx;
  }

  /**
   * Create a formula for a given variable, which is assumed to be constant.
   * This method does not handle scoping!
   */
  private <T extends Formula> T makeConstant(Variable<CType> var, SSAMapBuilder ssa) {
    // TODO better use variables without index (this piece of code prevents
    // SSAMapBuilder from checking for strict monotony)
    int idx = ssa.getIndex(var);
    assert idx <= 1 : var + " is assumed to be constant there was an assignment to it";
    if (idx != 1) {
      ssa.setIndex(var, 1); // set index so that predicates will be instantiated correctly
    }

    return fmgr.makeVariable(this.<T>getFormulaTypeFromCType(var.getType()), var.getName(), 1);
  }

  /**
   * Create a formula for a given variable.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   *
   * This method does not update the index of the variable.
   */
  private <T extends Formula> T makeVariable(Variable<CType> var, SSAMapBuilder ssa) {
    int idx = getIndex(var, ssa);
    return fmgr.makeVariable(this.<T>getFormulaTypeFromCType(var.getType()), var.getName(), idx);
  }

  /**
   * Create a formula for a given variable with a fresh index for the left-hand
   * side of an assignment.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   */
  private  <T extends Formula> T makeFreshVariable(Variable<CType> var, SSAMapBuilder ssa) {
    int idx = makeFreshIndex(var, ssa);
    return fmgr.makeVariable(this.<T>getFormulaTypeFromCType(var.getType()), var.getName(), idx);
  }

  /** Returns the pointer variable belonging to a given IdExpression */
  private <T extends Formula> T makePointerVariable(CIdExpression expr, String function,
      SSAMapBuilder ssa) {
    Variable<CType> ptrVarName = makePointerVariableName(expr, function, ssa);
    return makeVariable(ptrVarName, ssa);
  }

  /** Takes a (scoped) variable name and returns the pointer variable name. */
  private static String makePointerMask(String scopedId, SSAMapBuilder ssa) {
    return POINTER_VARIABLE + scopedId + "__at__" + ssa.getIndex(scopedId) + "__end";
  }

  /**
   * Takes a pointer variable name and returns the name of the associated
   * variable.
   */
  private static String removePointerMask(String pointerVariable) {
    assert (IS_POINTER_VARIABLE.apply(pointerVariable));

    return pointerVariable.substring(POINTER_VARIABLE.length(), pointerVariable.indexOf("__at__"));
  }

  private Variable<CType> makePointerMaskVariable(Variable<CType> pointerVar, SSAMapBuilder ssa){
    return Variable.create(makePointerMask(pointerVar.getName(), ssa), dereferencedType(pointerVar.getType()));
  }


  private Variable<CType> removePointerMaskVariable(Variable<CType> pointerVar){
    return Variable.create(removePointerMask(pointerVar.getName()), makePointerType(pointerVar.getType()));
  }
  private CType makePointerType(CType pType) {
    return new CPointerType(false, false, pType);
  }

  /** Returns the pointer variable name corresponding to a given IdExpression */
  private Variable<CType> makePointerVariableName(CIdExpression expr,
      String function, SSAMapBuilder ssa) {

    Variable<CType> scopedId = scopedIfNecessary(expr, function);
    return makePointerMaskVariable(scopedId, ssa);
  }

  private CType dereferencedType(CType t) {
    t = CTypeUtils.simplifyType(t);
    if (t instanceof CPointerType) {
      t = ((CPointerType)t).getType();
    } else if (t instanceof CArrayType) {
      t = ((CArrayType)t).getType();
    } else {
      log(Level.INFO, "No pointer type: " + t.getClass() + ", " + t.toString());
    }
    return t;
  }

  /**Returns the concatenation of MEMORY_ADDRESS_VARIABLE_PREFIX and varName */
  private String makeMemoryLocationVariableName(String varName) {
    return MEMORY_ADDRESS_VARIABLE_PREFIX + varName;
  }

  // name has to be scoped already
  /**
   * makes a fresh variable out of the varName and assigns the rightHandSide to it
   * @param varName has to be scoped already
   * @param rightHandSide
   * @param ssa
   * @return the new Formula (lhs = rhs)
   */
  private <T extends Formula> BooleanFormula makeAssignment(
      Variable<CType> lVar, CType right, Formula rightHandSide, SSAMapBuilder ssa) {
    Formula rhs = makeCast(right, lVar.getType(), rightHandSide);
    T lhs = makeFreshVariable(lVar, ssa);

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

    if (CTypeUtils.equals(fromType, toType)){
      return formula; // No cast required;
    }

    if (ignoreCasts) {
      log(Level.ALL, "IGNORING CAST from " + fromType + " to " + toType + "!");
      return formula;
    }

    fromType = CTypeUtils.simplifyType(fromType);
    toType = CTypeUtils.simplifyType(toType);

    if (fromType instanceof CSimpleType){
      CSimpleType sfromType = (CSimpleType)fromType;
      if (toType instanceof CSimpleType){
        CSimpleType stoType = (CSimpleType)toType;
        return makeSimpleCast(sfromType, stoType, formula);
      }
    }

    if (fromType instanceof CPointerType ||
        toType instanceof CPointerType){
      // Ignore casts between Pointer and right sized types
      if (getFormulaTypeFromCType(toType) == getFormulaTypeFromCType(fromType)){
        return formula;
      }
    }

    if (fromType instanceof CEnumType ||
        toType instanceof CEnumType){
      // Ignore casts between Enums and right sized types
      if (getFormulaTypeFromCType(toType) == getFormulaTypeFromCType(fromType)){
        return formula;
      }
    }

    if (fromType instanceof CElaboratedType && ((CElaboratedType)fromType).getKind() == ElaboratedType.ENUM ||
        toType instanceof CElaboratedType && ((CElaboratedType)toType).getKind() == ElaboratedType.ENUM ){
      // Ignore casts between Enums and right sized types
      if (getFormulaTypeFromCType(toType) == getFormulaTypeFromCType(fromType)){
        return formula;
      }
    }
    log(Level.WARNING, "WARNING: Could not handle cast from " + fromType + " to " + toType + "!");
    FunctionFormulaType<Formula> func = getCastFunc(fromType, toType);
    return ffmgr.createUninterpretedFunctionCall(func, Arrays.asList(formula));
  }

  private Formula makeSimpleCast(CSimpleType pfromType, CSimpleType ptoType, Formula pFormula) {
    int sfrom = machineModel.getSizeof(pfromType);
    int sto = machineModel.getSizeof(ptoType);

    // Currently everything is a bitvector
    // TODO: this is currently not always correct
    // (for example if we expand a signed variable with negative value)
    Formula ret;
    if (sfrom > sto){
      ret = fmgr.makeExtract(pFormula, sto * 8 - 1, 0);
    }else if (sfrom < sto){
      ret = fmgr.makeConcat(efmgr.makeBitvector((sto - sfrom) * 8, 0) , pFormula);
    }else{
      ret = pFormula;
    }

    assert fmgr.getFormulaType(ret) == getFormulaTypeFromCType(ptoType);
    return ret;
  }

  Map<CType, Map<CType, FunctionFormulaType<Formula>>> castMap = new Hashtable<>();

  private FunctionFormulaType<Formula> getCastFunc(CType fromType, CType toType){
    Map<CType, FunctionFormulaType<Formula>> toTable;
    if (!castMap.containsKey(fromType)){
      toTable = new Hashtable<>();
      castMap.put(fromType, toTable);
    }else{
      toTable = castMap.get(fromType);
    }
    FunctionFormulaType<Formula> func;
    if (!toTable.containsKey(toType)){
      FormulaType<Formula> intoFormulaType = getFormulaTypeFromCType(toType);
      FormulaType<Formula> fromFormulaType = getFormulaTypeFromCType(fromType);
      func = ffmgr.createFunction("CAST_FROM_"+fromType+"_TO_"+toType, intoFormulaType, Arrays.<FormulaType<?>>asList(fromFormulaType));
      toTable.put(toType, func);
    }else{
      func = toTable.get(toType);
    }

    return func;
  }
  /**
   * Gets the Type of the given two, which C would implicitly cast to.
   * @param pT1
   * @param pT2
   * @return
   */
  private CType getImplicitCType(CType pT1, CType pT2) {
    pT1 = CTypeUtils.simplifyType(pT1);
    pT2 = CTypeUtils.simplifyType(pT2);

    if (pT1 instanceof CSimpleType){
      CSimpleType s1 = (CSimpleType)pT1;
      if (pT2 instanceof CSimpleType){
        CSimpleType s2 = (CSimpleType)pT2;
        CSimpleType resolved = getImplicitSimpleCType(s1, s2);

        if (!CTypeUtils.equals(s1, s2)){
          log(Level.INFO, "Implicit Cast: " + s1 + " and " + s2 + " to " + resolved);
        }

        return resolved;
      }
    }

    int s1 = machineModel.getSizeof(pT1);
    int s2 = machineModel.getSizeof(pT2);
    CType res;
    if (s1 > s2){
      res = pT1;
    } else if (s2 > s1){
      res = pT2;
    } else {
      res = pT1;
    }
    log(Level.WARNING, "Could not get implicit Type of " + pT1 + " and " + pT2+ ", using " + res);
    return res;
  }

  private CSimpleType getImplicitSimpleCType(CSimpleType pT1, CSimpleType pT2) {
    // From http://msdn.microsoft.com/en-us/library/3t4w2bkb%28v=vs.80%29.aspx
    // If either operand is of type long double, the other operand is converted to type long double.
    CBasicType b1 = pT1.getType();
    CBasicType b2 = pT2.getType();
    if (pT1.isLong() && b1.equals(CBasicType.DOUBLE)){
      return pT1;
    }
    if (pT2.isLong() && b2.equals(CBasicType.DOUBLE)){
      return pT2;
    }

    // If the above condition is not met and either operand is of type double, the other operand is converted to type double.
    if (b1.equals(CBasicType.DOUBLE)){
      return pT1;
    }
    if (b2.equals(CBasicType.DOUBLE)){
      return pT2;
    }
    // If the above two conditions are not met and either operand is of type float, the other operand is converted to type float.
    if (b1.equals(CBasicType.FLOAT)){
      return pT1;
    }
    if (b2.equals(CBasicType.FLOAT)){
      return pT2;
    }

    // TODO: https://www.securecoding.cert.org/confluence/display/seccode/INT02-C.+Understand+integer+conversion+rules
    // See also http://stackoverflow.com/questions/50605/signed-to-unsigned-conversion-in-c-is-it-always-safe
    if (CTypeUtils.equals(pT1, pT2)){
      return pT1; // This is a quick workaround to prevent shorts to be converted to ints
    }

    // If the above three conditions are not met (none of the operands are of floating types), then integral conversions are performed on the operands as follows:

    // NOTE: EXTENSION for long long (see below for long)
    // Convert to unsinged long long if one is unsigned long long
    if (pT1.isUnsigned() && pT1.isLongLong() && (b1.equals(CBasicType.INT) || b1.equals(CBasicType.UNSPECIFIED))){
      return pT1;
    }
    if (pT2.isUnsigned() && pT2.isLongLong() && (b2.equals(CBasicType.INT) || b2.equals(CBasicType.UNSPECIFIED))){
      return pT2;
    }

    // Convert to unsigned long long if one is long long and the other is unsigned int
    if (pT1.isLongLong() && (b1.equals(CBasicType.INT) || b1.equals(CBasicType.UNSPECIFIED))
        && pT1.isLong() && pT2.isUnsigned() && (b2.equals(CBasicType.INT) || b2.equals(CBasicType.UNSPECIFIED))){
      return new CSimpleType(false, false, CBasicType.INT, false, false, false, true, false, false, true);
    }
    if (pT2.isLongLong() && (b2.equals(CBasicType.INT) || b2.equals(CBasicType.UNSPECIFIED))
        && pT1.isLong() && pT1.isUnsigned() && (b1.equals(CBasicType.INT) || b1.equals(CBasicType.UNSPECIFIED))){
      return new CSimpleType(false, false, CBasicType.INT, false, false, false, true, false, false, true);
    }

    // Convert to long long if one is long long
    if (pT1.isLongLong() && (b1.equals(CBasicType.INT) || b1.equals(CBasicType.UNSPECIFIED))){
      return pT1;
    }
    if (pT2.isLongLong() && (b2.equals(CBasicType.INT) || b2.equals(CBasicType.UNSPECIFIED))){
      return pT2;
    }
    // NOTE: END EXTENSION

    // - If either operand is of type unsigned long, the other operand is converted to type unsigned long.
    if (pT1.isUnsigned() && pT1.isLong() && (b1.equals(CBasicType.INT) || b1.equals(CBasicType.UNSPECIFIED))){
      return pT1;
    }
    if (pT2.isUnsigned() && pT2.isLong() && (b2.equals(CBasicType.INT) || b2.equals(CBasicType.UNSPECIFIED))){
      return pT2;
    }

    // - If the above condition is not met and either operand is of type long and the other of type unsigned int, both operands are converted to type unsigned long.
    if (pT1.isLong() && (b1.equals(CBasicType.INT) || b1.equals(CBasicType.UNSPECIFIED))
        && pT2.isUnsigned() && (b2.equals(CBasicType.INT) || b2.equals(CBasicType.UNSPECIFIED))){
      return new CSimpleType(false, false, CBasicType.INT, true, false, false, true, false, false, false);
    }
    if (pT2.isLong() && (b2.equals(CBasicType.INT) || b2.equals(CBasicType.UNSPECIFIED))
        && pT1.isUnsigned() && (b1.equals(CBasicType.INT) || b1.equals(CBasicType.UNSPECIFIED))){
      return new CSimpleType(false, false, CBasicType.INT, true, false, false, true, false, false, false);
    }

    // - If the above two conditions are not met, and either operand is of type long, the other operand is converted to type long.
    if (pT1.isLong() && (b1.equals(CBasicType.INT) || b1.equals(CBasicType.UNSPECIFIED))){
      return pT1;
    }
    if (pT2.isLong() && (b2.equals(CBasicType.INT) || b2.equals(CBasicType.UNSPECIFIED))){
      return pT2;
    }

    // - If the above three conditions are not met, and either operand is of type unsigned int, the other operand is converted to type unsigned int.
    if (pT1.isUnsigned() && (b1.equals(CBasicType.INT) || b1.equals(CBasicType.UNSPECIFIED))){
      return pT1;
    }
    if (pT2.isUnsigned() && (b2.equals(CBasicType.INT) || b2.equals(CBasicType.UNSPECIFIED))){
      return pT2;
    }

    // - If none of the above conditions are met, both operands are converted to type int.
    return new CSimpleType(false, false, CBasicType.INT, false, false, true, false, false, false, false);
  }

  @SuppressWarnings("unchecked")
  protected <T extends Formula> FormulaType<T> getNondetType() {
    return (FormulaType<T>) efmgr.getFormulaType(machineModel.getSizeofInt() * 8);
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
          Formula nondetVar = fmgr.makeVariable(getNondetType(), NONDET_FLAG_VARIABLE, lIndex);
          BooleanFormula lAssignment = fmgr.assignment(nondetVar, fmgr.makeNumber(getNondetType(), 1));
          edgeFormula = bfmgr.and(edgeFormula, lAssignment);
        }

        // update ssa index of nondet flag
        ssa.setIndex(Variable.create(NONDET_FLAG_VARIABLE, getNondetType()), lNondetIndex);
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
    CType declType = decl.getType();
    //FormulaType<Formula> t= getFormulaTypeFromCType(declType);
    Variable<CType> var = Variable.create(varName, declType);
    // TODO get the type of the variable, and act accordingly

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
    makeFreshIndex(var, ssa);

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

    CType initType = init.getExpressionType();

    // initializer value present

    Formula minit = buildTerm(init, edge, function, ssa, constraints);
    BooleanFormula assignments = makeAssignment(var, initType, minit, ssa);

    if (handlePointerAliasing) {
      // we need to add the pointer alias
      BooleanFormula pAssign = buildDirectSecondLevelAssignment(var,
          removeCast(init), function, constraints, ssa);
      assignments = bfmgr.and(pAssign, assignments);

      // no need to add pointer updates:
      // the left hand variable cannot yet be aliased
      // because it is newly created
    }

    return assignments;
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
      CType retType = exp.getRightHandSide().getDeclaration().getType().getReturnType();

      Variable<CType> var = Variable.create(retVarName, retType);
      Formula retVar = makeVariable(var, ssa);
      CExpression e = exp.getLeftHandSide();

      function = ce.getSuccessor().getFunctionName();
      Formula outvarFormula = buildLvalueTerm(e, ce, function, ssa, constraints);
      retVar = makeCast(retType, e.getExpressionType(), retVar);
      BooleanFormula assignments = fmgr.assignment(outvarFormula, retVar);

      if (handlePointerAliasing) {
        CExpression left = removeCast(e);
        if (left instanceof CIdExpression) {
          BooleanFormula ptrAssignment = buildDirectReturnSecondLevelAssignment(
              (CIdExpression) left, var, function, ssa);
          assignments = bfmgr.and(assignments, ptrAssignment);
        }
      }

       return assignments;
    } else {
      throw new UnrecognizedCCodeException("Unknown function exit expression", ce, retExp.asStatement());
    }
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
        warnUnsafeAssignment();
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
              Variable.create(varName, paramType),
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
      Variable<CType> ret = Variable.create(retVarName, returnType);
      BooleanFormula assignments = makeAssignment(
          ret, expressionType, retval, ssa);

      if (handlePointerAliasing) {
        // if the value to be returned may be a pointer, act accordingly
        BooleanFormula rightAssignment = buildDirectSecondLevelAssignment(
            ret, rightExp, function, constraints, ssa);
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

  private BooleanFormula buildDirectReturnSecondLevelAssignment(CIdExpression leftId,
      Variable<CType> retVarName, String function, SSAMapBuilder ssa) {

    // include aliases if the left or right side may be a pointer a pointer
    if (maybePointer(leftId, function, ssa)
        || maybePointer((CType) null, retVarName.getName(), ssa)) {
      // we assume that either the left or the right hand side is a pointer
      // so we add the equality: *l = *r
      Formula lPtrVar = makePointerVariable(leftId, function, ssa);
      Variable<CType> retPtrVarName = makePointerMaskVariable(retVarName, ssa);

      Formula retPtrVar = makeVariable(retPtrVarName, ssa);
      return makeNondetAssignment(lPtrVar, retPtrVar, ssa);

    } else {
      // we can assume, that no pointers are affected in this assignment
      return bfmgr.makeBoolean(true);
    }
  }

  private int expands = 0;
  private BooleanFormula makeNondetAssignment(Formula left, Formula right, SSAMapBuilder ssa) {
    BitvectorFormulaManagerView bitvectorFormulaManager = efmgr;
    FormulaType<Formula> tl = fmgr.getFormulaType(left);
    FormulaType<Formula> tr = fmgr.getFormulaType(right);
    if (tl == tr){
      return fmgr.assignment(left, right);
    }

    if ((Class<?>)tl.getInterfaceType() == BitvectorFormula.class && (Class<?>)tr.getInterfaceType() == BitvectorFormula.class){

      BitvectorFormula leftBv = (BitvectorFormula) left;
      BitvectorFormula rightBv = (BitvectorFormula) right;
      int leftSize = bitvectorFormulaManager.getLength(leftBv);
      int rightSize = bitvectorFormulaManager.getLength(rightBv);

      // Expand the smaller one with nondet-bits
      int dif = Math.abs(rightSize - leftSize);
      FormulaType<BitvectorFormula> t = bitvectorFormulaManager.getFormulaType(dif);
      //Variable<CType> var = Variable.create(CtoFormulaConverter.NONDET_VARIABLE, null);
      //int idx = makeFreshIndex(var, ssa);
      BitvectorFormula nonDet = fmgr.makeVariable(t, CtoFormulaConverter.EXPAND_VARIABLE + expands++, 0); // for every call a new variable
      if (leftSize < rightSize) {
        leftBv = bitvectorFormulaManager.concat(nonDet,leftBv);
      } else {
        rightBv = bitvectorFormulaManager.concat(nonDet,rightBv);
      }
      return bitvectorFormulaManager.equal(leftBv, rightBv);
    }

    throw new IllegalArgumentException("Assignment between different types");
  }

  private BooleanFormula buildDirectSecondLevelAssignment(
      Variable<CType> lVarName,
      CRightHandSide pRight, String function,
      Constraints constraints, SSAMapBuilder ssa) {

    CRightHandSide right = removeCast(pRight);
    Formula lVar = makeVariable(lVarName, ssa);

    if (isVariable(right)) {
      // C statement like: s1 = s2;

      // include aliases if the left or right side may be a pointer a pointer
      CIdExpression rIdExp = (CIdExpression) right;
      if (maybePointer(lVarName, ssa) || maybePointer(rIdExp, function, ssa)) {
        // we assume that either the left or the right hand side is a pointer
        // so we add the equality: *l = *r
        Variable<CType> lPVarName = makePointerMaskVariable(lVarName, ssa);
        Formula lPtrVar = makeVariable(lPVarName, ssa);

        Variable<CType> rPtrVarName = makePointerVariableName(rIdExp, function, ssa);
        Formula rPtrVar = makeVariable(rPtrVarName, ssa);
        return makeNondetAssignment(lPtrVar, rPtrVar, ssa);

      } else {
        // we can assume, that no pointers are affected in this assignment
        return bfmgr.makeBoolean(true);
      }

    } else if (isPointerDereferencing(right)) {
      // C statement like: s1 = *s2;

      Variable<CType> lPtrVarName = makePointerMaskVariable(lVarName, ssa);
      makeFreshIndex(lPtrVarName, ssa);
      removeOldPointerVariablesFromSsaMap(lPtrVarName, ssa);
      Formula lPtrVar = makeVariable(lPtrVarName, ssa);

      CExpression rExpr = removeCast(((CUnaryExpression) right).getOperand());
      if (!(rExpr instanceof CIdExpression)) {
        // these are statements like s1 = *(s2.f)
        // TODO check whether doing nothing is correct
        return bfmgr.makeBoolean(true);
      }

      CIdExpression rIdExpr = (CIdExpression)rExpr;
      Formula rPtrVar = makePointerVariable(rIdExpr, function, ssa);

      // the dealiased address of the right hand side may be a pointer itself.
      // to ensure tracking, we need to set the left side
      // equal to the dealiased right side or update the pointer
      // r is the right hand side variable, l is the left hand side variable
      // ∀p ∈ maybePointer: (p = *r) ⇒ (l = p ∧ *l = *p)
      List<Variable<CType>> ptrVars = getAllPointerVariablesFromSsaMap(ssa);
      for (Variable<CType> ptrVarName : ptrVars) {
        Variable<CType> varName = removePointerMaskVariable(ptrVarName);

        if (!varName.equals(lVarName)) {

          Formula var = makeVariable(varName, ssa);
          Formula ptrVar = makeVariable(ptrVarName, ssa);

          BooleanFormula ptr = fmgr.makeEqual(rPtrVar, var);

          BooleanFormula dirEq = fmgr.makeEqual(lVar, var);
          BooleanFormula indirEq = fmgr.makeEqual(lPtrVar, ptrVar);
          BooleanFormula consequence = bfmgr.and(dirEq, indirEq);

          BooleanFormula constraint = bfmgr.implication(ptr, consequence);

          constraints.addConstraint(constraint);
        }
      }

      // no need to add a second level assignment
      return bfmgr.makeBoolean(true);

    } else if (isMemoryLocation(right)) {
      // s = &x
      // need to update the pointer on the left hand side
      if (right instanceof CUnaryExpression
          && ((CUnaryExpression) right).getOperator() == UnaryOperator.AMPER){

        CExpression rOperand =
            removeCast(((CUnaryExpression) right).getOperand());
        if (rOperand instanceof CIdExpression) {
          Variable<CType> rVarName = scopedIfNecessary((CIdExpression) rOperand, function);
          Formula rVar = makeVariable(rVarName, ssa);
          Formula lPtrVar = makeVariable(makePointerMaskVariable(rVarName, ssa), ssa);

          return makeNondetAssignment(lPtrVar, rVar, ssa);
        }
      }

      // s = malloc()
      // has been handled already
      return bfmgr.makeBoolean(true);

    } else {
      // s = 1
      // s = someFunction()
      // s = a + b
      // s = x.f
      // s = x->f
      // s = a[i]

      // no second level assignment necessary
      return bfmgr.makeBoolean(true);
    }
  }

  private BooleanFormula makePredicate(CExpression exp, boolean isTrue, CFAEdge edge,
      String function, SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {

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


  private static boolean isVariable(CAstNode exp) {
    return exp instanceof CIdExpression;
  }

  private static boolean isPointerDereferencing(CAstNode exp) {
    return (exp instanceof CUnaryExpression
        && ((CUnaryExpression) exp).getOperator() == UnaryOperator.STAR);
  }

  private static boolean isStaticallyDeclaredPointer(CType expr) {
    return expr instanceof CPointerType;
  }

  private boolean isMemoryLocation(CAstNode exp) {

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
    }

    return false;
  }



  /** Returns if a given expression may be a pointer. */
  private boolean maybePointer(CExpression pExp, String function, SSAMapBuilder ssa) {
    CExpression exp = removeCast(pExp);
    if (exp instanceof CIdExpression) {
      CIdExpression idExp = (CIdExpression) exp;
      CType type = exp.getExpressionType();
      return maybePointer(type, scopedIfNecessary(idExp, function).getName(), ssa);
    }

    return false;
  }

  private static boolean maybePointer(Variable<CType> varName, SSAMapBuilder ssa){
    return maybePointer(varName.getType(), varName.getName(), ssa);
  }

  private static boolean maybePointer(CType type, String varName, SSAMapBuilder ssa) {
    if (type != null && isStaticallyDeclaredPointer(type)) {
      return true;
    }

    // check if it has been used as a pointer before
    List<Variable<CType>> ptrVarNames = getAllPointerVariablesFromSsaMap(ssa);
    String expPtrVarName = makePointerMask(varName, ssa);
    return ptrVarNames.contains(Variable.create(expPtrVarName, FormulaType.BooleanType));
  }

  /**
   * Returns a list of all variable names representing memory locations in
   * the SSAMap. These memory locations are those previously used.
   *
   * Stored memory locations are prefixed with
   * {@link #MEMORY_ADDRESS_VARIABLE_PREFIX}.
   */
  private static ImmutableList<Variable<CType>> getAllMemoryLocationsFromSsaMap(SSAMapBuilder ssa) {
    return from(ssa.allVariables())
              .transform(new Function<Variable<?>, Variable<CType>>() {
                  @SuppressWarnings("unchecked")
                  @Override
                  public Variable<CType> apply(Variable<?> pInput) {
                    return (Variable<CType>) pInput;
                  }})
              .filter(liftToVariable(IS_MEMORY_ADDRESS_VARIABLE))
              .toImmutableList();
  }

  private static Predicate<? super Variable<CType>> liftToVariable(final Predicate<String> stringPred) {
    return new Predicate<Variable<?>>() {
      @Override
      public boolean apply(Variable<?> pInput) {
        return stringPred.apply(pInput.getName());
      }};
  }

  /**
   * Returns a list of all pointer variables stored in the SSAMap.
   */
  private static List<Variable<CType>> getAllPointerVariablesFromSsaMap(SSAMapBuilder ssa) {
    return from(ssa.allVariables())
              .transform(new Function<Variable<?>, Variable<CType>>() {
                @SuppressWarnings("unchecked")
                @Override
                public Variable<CType> apply(Variable<?> pInput) {
                  return (Variable<CType>) pInput;
                }})
              .filter(liftToVariable(toStringPred(IS_POINTER_VARIABLE)))
              .toImmutableList();
  }

  private static Predicate<String> toStringPred(final Predicate<CharSequence> charSeqPred) {
    return new Predicate<String>() {
      @Override
      public boolean apply(String pInput) {
        return charSeqPred.apply(pInput);
      }};
  }

  /**
   * Removes all pointer variables belonging to a given variable from a given
   * SSAMapBuilderthat are no longer valid. Validity of an entry expires,
   * when the pointer variable belongs to a variable with an old index.
   *
   * @param newPVar The variable name of the new pointer variable.
   * @param ssa The SSAMapBuilder from which the variables are to be deleted
   */
  private static void removeOldPointerVariablesFromSsaMap(Variable<?> newPVar,
      SSAMapBuilder ssa) {

    String newVar = removePointerMask(newPVar.getName());

    List<Variable<CType>> pointerVariables = getAllPointerVariablesFromSsaMap(ssa);
    for (Variable<CType> ptrVar : pointerVariables) {
      String ptrVarName = ptrVar.getName();
      String oldVar = removePointerMask(ptrVarName);
      if (!ptrVarName.equals(newPVar.getName()) && oldVar.equals(newVar)) {
        ssa.deleteVariable(ptrVar);
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
      warnUnsafeVar(exp);
      return makeVariable(Variable.create(scoped(exprToVarName(exp), function), exp.getExpressionType()), ssa);
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
      CType implicitType = getImplicitCType(t1, t2);
      Formula f1 = e1.accept(this);
      Formula f2 = e2.accept(this);
      boolean signed = isSignedType(implicitType);
      f1 = makeCast(t1, implicitType, f1);
      f2 = makeCast(t2, implicitType, f2);
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
        ret =  fmgr.makeShiftLeft(f1, f2);
        break;
      case SHIFT_RIGHT:
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
      if (returnFormulaType != fmgr.getFormulaType(ret)){
        System.out.println();
      }

      assert returnFormulaType == fmgr.getFormulaType(ret);
      return ret;
    }



    @Override
    public Formula visit(CCastExpression cexp) throws UnrecognizedCCodeException {
      Formula operand = cexp.getOperand().accept(this);
      CType expType = cexp.getExpressionType();
      CType operandType = cexp.getOperand().getExpressionType();
      return makeCast(operandType, expType, operand);
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
          return makeConstant(Variable.create(enumerator.getName(), t), ssa);
        }
      }

      return makeVariable(scopedIfNecessary(idExp, function), ssa);
    }

    @Override
    public Formula visit(CFieldReference fExp) throws UnrecognizedCCodeException {
      CExpression fieldRef = fExp.getFieldOwner();
      if (fieldRef instanceof CIdExpression) {
        CSimpleDeclaration decl = ((CIdExpression) fieldRef).getDeclaration();
        if (decl instanceof CDeclaration && ((CDeclaration)decl).isGlobal()) {
          // this is the reference to a global field variable

          // we can omit the warning (no pointers involved),
          // and we don't need to scope the variable reference
          return makeVariable(Variable.create(exprToVarName(fExp),fieldRef.getExpressionType()), ssa);
        }
      }

      // else do the default
      return super.visit(fExp);
    }

    @Override
    public Formula visit(CCharLiteralExpression cExp) throws UnrecognizedCCodeException {
      // we just take the byte value
      FormulaType<Formula> t = getFormulaTypeFromCType(cExp.getExpressionType());
      return fmgr.makeNumber(t, cExp.getCharacter());
    }

    @Override
    public Formula visit(CIntegerLiteralExpression iExp) throws UnrecognizedCCodeException {
      FormulaType<Formula> t = getFormulaTypeFromCType(iExp.getExpressionType());
      return fmgr.makeNumber(t, iExp.getValue().longValue());
    }

    @Override
    public Formula visit(CFloatLiteralExpression fExp) throws UnrecognizedCCodeException {
      FormulaType<Formula> t = getFormulaTypeFromCType(fExp.getExpressionType());
      // TODO: Check if this is actually correct
      return fmgr.makeNumber(t,fExp.getValue().longValue());
    }

    @Override
    public Formula visit(CStringLiteralExpression lexp) throws UnrecognizedCCodeException {
      // we create a string constant representing the given
      // string literal
      String literal = lexp.getValue();
      BitvectorFormula result = stringLitToFormula.get(literal);

      if (result == null) {
        // generate a new string literal. We generate a new UIf
        int n = nextStringLitIndex++;
        result =  fmgr.makeString(n);
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
        return operand.accept(this);

      case MINUS: {
        Formula formula = operand.accept(this);
        return fmgr.makeNegate(formula);
      }

      case TILDE: {
        Formula formula = operand.accept(this);
        return fmgr.makeNot(formula);
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
          machineModel.getSizeof(pCType));
    }
  }

  private class ExpressionToFormulaVisitorUIF extends ExpressionToFormulaVisitor {

    public ExpressionToFormulaVisitorUIF(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(pEdge, pFunction, pSsa, pCo);
    }

    private Formula makeUIF(Variable<CType> var, SSAMapBuilder ssa, Formula... args) {
      String name = var.getName();
      FormulaList l = new AbstractFormulaList(args);
      int idx = ssa.getIndex(name, l);
      if (idx <= 0) {
        logger.log(Level.ALL, "DEBUG_3",
            "WARNING: Auto-instantiating lval: ", name, "(", l, ")");
        idx = 1;
        ssa.setIndex(var, l, idx);
      }
      return ffmgr.createFuncAndCall(name, idx, getFormulaTypeFromCType(var.getType()), Arrays.asList( args) );
    }

    @Override
    public Formula visit(CArraySubscriptExpression aexp) throws UnrecognizedCCodeException {
      CExpression arrexp = aexp.getArrayExpression();
      CExpression subexp = aexp.getSubscriptExpression();
      Formula aterm = arrexp.accept(this);
      Formula sterm = subexp.accept(this);

      String ufname = OP_ARRAY_SUBSCRIPT;
      return makeUIF(Variable.create(ufname,aexp.getExpressionType()), ssa, aterm, sterm);
    }

    @Override
    public Formula visit(CFieldReference fexp) throws UnrecognizedCCodeException {
      String field = fexp.getFieldName();
      CExpression owner = fexp.getFieldOwner();
      Formula term = owner.accept(this);

      String tpname = getTypeName(owner.getExpressionType());
      String ufname =
          (fexp.isPointerDereference() ? "->{" : ".{") + tpname + "," + field
              + "}";

      // see above for the case of &x and *x
      return makeUIF(Variable.create(ufname, fexp.getExpressionType()), ssa, term);
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
        int idx = getIndex(Variable.create(opname, expType), ssa);
        //int idx = getIndex(
        //    opname, term, ssa, absoluteSSAIndices);

        // build the  function corresponding to this operation.

        return ffmgr.createFuncAndCall(
            opname, idx, getFormulaTypeFromCType(expType), Arrays.asList( term ));

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
      CExpression opExp = removeCast(exp.getOperand());
      UnaryOperator op = exp.getOperator();

      switch (op) {
      case AMPER:
        return makeAddressVariable(exp, function);

      case STAR:
        if (opExp instanceof CIdExpression) {
          return makePointerVariable((CIdExpression) opExp, function, ssa);
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

        if (op != UnaryOperator.AMPER || !(operand instanceof CIdExpression)) {
          return super.visitDefault(exp);
        }

        return makeMemoryLocationVariable(exp.getExpressionType(), (CIdExpression) operand, function);
    }

    /**
     * Returns a Formula representing the memory location of a given IdExpression.
     * Ensures that the location is unique and not 0.
     *
     * @param function The scope of the variable.
     */
    private Formula makeMemoryLocationVariable(CType pointerType, CIdExpression exp, String function) {
      Variable<CType> v =
          scopedIfNecessary(exp, function)
            .withType(pointerType);
      String addressVariable = makeMemoryLocationVariableName(v.getName());

      // a variable address is always initialized, not 0 and cannot change
      if (ssa.getIndex(addressVariable) == VARIABLE_UNSET) {
        List<Variable<CType>> oldMemoryLocations = getAllMemoryLocationsFromSsaMap(ssa);
        Formula newMemoryLocation = makeConstant(v.withName(addressVariable), ssa);

        // a variable address that is unknown is different from all previously known addresses
        for (Variable<CType> memoryLocation : oldMemoryLocations) {
          Formula oldMemoryLocation = makeConstant( memoryLocation, ssa);
          BooleanFormula addressInequality = bfmgr.not(fmgr.makeEqual(newMemoryLocation, oldMemoryLocation));

          constraints.addConstraint(addressInequality);
        }

        // a variable address is not 0
        BooleanFormula notZero = bfmgr.not(fmgr.makeEqual(newMemoryLocation, fmgr.makeNumber(getFormulaTypeFromCType(v.getType()), 0)));
        constraints.addConstraint(notZero);
      }

      return makeConstant(v.withName(addressVariable), ssa);
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
      FormulaType<BitvectorFormula> t = getFormulaTypeFromCType(expType);
      if (fn instanceof CIdExpression) {
        func = ((CIdExpression)fn).getName();
        if (func.equals(ASSUME_FUNCTION_NAME) && pexps.size() == 1) {

          BooleanFormula condition = fmgr.toBooleanFormula(pexps.get(0).accept(this));
          constraints.addConstraint(condition);

          return makeFreshVariable(Variable.create(func,expType), ssa);

        } else if (nondetFunctions.contains(func)
            || nondetFunctionsPattern.matcher(func).matches()) {
          // function call like "random()"
          // ignore parameters and just create a fresh variable for it
          return makeFreshVariable(Variable.create(func,expType), ssa);

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
        func = "<func>{" + fn.toASTString() + "}";
      }

      if (pexps.isEmpty()) {
        // This is a function of arity 0 and we assume its constant.
        return makeConstant(Variable.create(func,expType), ssa);

      } else {
        CFunctionDeclaration declaration = fexp.getDeclaration();
        if (declaration == null){
          log(Level.WARNING, "Cant get declaration of function. Ignoring the call (" + fexp.toASTString() + ").");
          return makeFreshVariable(Variable.create(func,expType), ssa);
        }

        List<CType> paramTypes =
          from (declaration.getType().getParameters())
            .transform(new Function<CParameterDeclaration, CType>() {
              @Override
              public CType apply(CParameterDeclaration pInput) {
                return pInput.getType();
              }}).toImmutableList();
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

        if (it2.hasNext()){
          log(Level.WARNING, "Ignoring call to " + declaration.toASTString() + " because of varargs");
          return makeFreshVariable(Variable.create(func,expType), ssa);
        }

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

    public BooleanFormula visit(CAssignment assignment) throws UnrecognizedCCodeException {
      Formula r = assignment.getRightHandSide().accept(this);
      Formula l = buildLvalueTerm(assignment.getLeftHandSide(), edge, function, ssa, constraints);
      r = makeCast(
          assignment.getRightHandSide().getExpressionType(),
          assignment.getLeftHandSide().getExpressionType(),
          r);

      return fmgr.assignment(l, r);
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
      CType expType = fexp.getExpressionType();
      FormulaType<Formula> t = getFormulaTypeFromCType(expType);
      if (fn instanceof CIdExpression) {
        String fName = ((CIdExpression)fn).getName();

        if (memoryAllocationFunctions.contains(fName)) {
          // for now all parameters are ignored

          List<Variable<CType>> memoryLocations = getAllMemoryLocationsFromSsaMap(ssa);

          Variable<CType> mallocVarName = makeFreshMallocVariableName(expType);
          Formula mallocVar = makeConstant(mallocVarName, ssa);

          // we must distinguish between two cases:
          // either the result is 0 or it is different from all other memory locations
          // (m != 0) => for all memory locations n: m != n
          BooleanFormula ineq = bfmgr.makeBoolean(true);
          for (Variable<CType> ml : memoryLocations) {
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

      } else {
        return super.visit(assignment);
      }
    }

    /**
     * An indirect assignment does not change the value of the variable on the
     * left hand side. Instead it changes the value stored in the memory location
     * aliased on the left hand side.
     */
    private BooleanFormula handleIndirectAssignment(CAssignment pAssignment)
        throws UnrecognizedCCodeException {
      CExpression lExp = removeCast(pAssignment.getLeftHandSide());
      assert (lExp instanceof CUnaryExpression);

      // the following expressions are supported by cil:
      // *p = a;
      // *p = 1;
      // *p = a | b; (or any other binary statement)
      // *p = function();

      CUnaryExpression l = (CUnaryExpression) lExp;
      assert (l.getOperator() == UnaryOperator.STAR);

      CExpression lOperand = removeCast(l.getOperand());
      if (!(lOperand instanceof CIdExpression)) {
        // TODO: *(a + 2) = b
        return super.visit(pAssignment);
      }

      CRightHandSide r = pAssignment.getRightHandSide();

      Variable<CType> lVarName = scopedIfNecessary((CIdExpression) lOperand, function);
      Formula lVar = makeVariable(lVarName, ssa);

      //TODO: check this
      Variable<CType> rVarName = Variable.create(null, pAssignment.getRightHandSide().getExpressionType());
      Formula rPtrVar = null;
      if (r instanceof CIdExpression) {
        rVarName = scopedIfNecessary((CIdExpression) r, function);
        rPtrVar = makePointerVariable((CIdExpression) r, function, ssa);
      }

      Formula rightVariable = pAssignment.getRightHandSide().accept(this);

      Formula lPtrVar = buildLvalueTerm(pAssignment.getLeftHandSide(), edge, function, ssa, constraints);
      BooleanFormula assignments = fmgr.assignment(lPtrVar, rightVariable);

      updateAllPointers(lVarName, lVar, rVarName, rightVariable);

      boolean doDeepUpdate = (r instanceof CIdExpression);
      updateAllMemoryLocations(lVarName, rPtrVar, rightVariable, rVarName, doDeepUpdate);

      return assignments;
    }

    private void updateAllMemoryLocations(
        Variable<CType> lVarName, Formula rPtrVar, Formula rVar, Variable<CType> rVarName, boolean deepUpdate) {

      Formula lVar = makeVariable(lVarName, ssa);

      // for all memory addresses also update the aliasing
      // if the left variable is an alias for an address,
      // then the left side is (deep) equal to the right side
      // otherwise update the variables
      List<Variable<CType>> memAddresses = getAllMemoryLocationsFromSsaMap(ssa);
      if (deepUpdate) {
        for (Variable<CType> memAddress : memAddresses) {
          Variable<CType> varName = getVariableFromMemoryAddress(memAddress);
          //String varName = getVariableNameFromMemoryAddress(memAddress.getName());

          if (!varName.equals(lVarName)) {
            // we assume that cases like the following are illegal and do not occur
            // (gcc 4.6 gives an error):
            // p = &p;
            // *p = &a;

            Formula memAddressVar = makeVariable(memAddress, ssa);

            Formula oldVar = makeVariable(varName, ssa);
            //String oldPtrVarName = makePointerMask(varName, ssa);
            Variable<CType> oldPtrVarName = makePointerMaskVariable(varName, ssa);
            Formula oldPtrVar = makeVariable(oldPtrVarName, ssa);

            makeFreshIndex(varName, ssa);

            Formula newVar = makeVariable(varName, ssa);
            //String newPtrVarName = makePointerMask(varName, ssa);
            Variable<CType> newPtrVarName = makePointerMaskVariable(varName, ssa);
            Formula newPtrVar = makeVariable(varName, ssa);
            removeOldPointerVariablesFromSsaMap(newPtrVarName, ssa);

            BooleanFormula varEquality = fmgr.assignment(newVar, rVar);
            BooleanFormula ptrVarEquality = fmgr.assignment(newPtrVar, rPtrVar);
            BooleanFormula varUpdate = fmgr.assignment(newVar, oldVar);
            BooleanFormula ptrVarUpdate = fmgr.assignment(newPtrVar, oldPtrVar);

            BooleanFormula condition = fmgr.makeEqual(lVar, memAddressVar);
            BooleanFormula equality = bfmgr.and(varEquality, ptrVarEquality);
            BooleanFormula update = bfmgr.and(varUpdate, ptrVarUpdate);

            BooleanFormula variableUpdate = bfmgr.ifThenElse(condition, equality, update);
            constraints.addConstraint(variableUpdate);
          }
        }

      } else {
        // no deep update of pointers required

        for (Variable<CType> memAddress : memAddresses) {
          Variable<CType> varName = getVariableFromMemoryAddress(memAddress);

          if (!varName.equals(lVarName)) {

            Formula oldVar = makeVariable(varName, ssa);
            makeFreshIndex(varName, ssa);

            Formula newVar = makeVariable(varName, ssa);
            Variable<CType> newPtrVarName = makePointerMaskVariable(varName, ssa);
            removeOldPointerVariablesFromSsaMap(newPtrVarName, ssa);

            Formula memAddressVar = makeVariable(memAddress, ssa);

            BooleanFormula condition = fmgr.makeEqual(lVar, memAddressVar);
            BooleanFormula equality = makeNondetAssignment(newVar, rVar, ssa);
            BooleanFormula update = makeNondetAssignment(newVar, oldVar, ssa);

            BooleanFormula variableUpdate = bfmgr.ifThenElse(condition, equality, update);
            constraints.addConstraint(variableUpdate);
          }
        }
      }
    }

    private Variable<CType> getVariableFromMemoryAddress(Variable<CType> pMemAddress) {
      return Variable.create(
          getVariableNameFromMemoryAddress(pMemAddress.getName()),
          dereferencedType(pMemAddress.getType()));
    }

    /**
     * Call this method if you need to update all pointers
     */
    private void updateAllPointers(Variable<CType> leftVarName, Formula leftVar,
        Variable<CType> rightVarName, Formula rightVariable) {

      // update all pointer variables (they might have a new value)
      // every variable aliased to the left hand side,
      // has its pointer set to the right hand side,
      // for all other pointer variables, the index is updated
      List<Variable<CType>> ptrVarNames = getAllPointerVariablesFromSsaMap(ssa);
      for (Variable<CType> ptrVarName : ptrVarNames) {
        Variable<CType> varName = removePointerMaskVariable(ptrVarName);

        if (!varName.equals(leftVarName) && !varName.equals(rightVarName)) {
          Formula var = makeVariable(varName, ssa);

          Formula oldPtrVar = makeVariable(ptrVarName, ssa);
          makeFreshIndex(ptrVarName, ssa);
          Formula newPtrVar = makeVariable(ptrVarName, ssa);

          BooleanFormula condition = fmgr.makeEqual(var, leftVar);
          BooleanFormula equality = makeNondetAssignment(newPtrVar, rightVariable, ssa);
          BooleanFormula indexUpdate = makeNondetAssignment(newPtrVar, oldPtrVar, ssa);

          BooleanFormula variableUpdate = bfmgr.ifThenElse(condition, equality, indexUpdate);
          constraints.addConstraint(variableUpdate);
        }
      }
    }

    /** A direct assignment changes the value of the variable on the left side. */
    private BooleanFormula handleDirectAssignment(CAssignment assignment)
        throws UnrecognizedCCodeException {
      CExpression lExpr = removeCast(assignment.getLeftHandSide());
      assert(lExpr instanceof CIdExpression);

      CIdExpression left = (CIdExpression) lExpr;
      CRightHandSide right = removeCast(assignment.getRightHandSide());

      Variable<CType> leftVarName = scopedIfNecessary(left, function);

      // assignment (first level) -- uses superclass
      Formula rightVariable = assignment.getRightHandSide().accept(this);
      Formula leftVariable = buildLvalueTerm(assignment.getLeftHandSide(), edge, function, ssa, constraints);
      rightVariable =
          makeCast(
              assignment.getRightHandSide().getExpressionType(),
              assignment.getLeftHandSide().getExpressionType(),
              rightVariable);
      BooleanFormula firstLevelFormula = fmgr.assignment(leftVariable, rightVariable);

      // assignment (second level)
      Variable<CType> lVarName = scopedIfNecessary(left, function);
      BooleanFormula secondLevelFormula = buildDirectSecondLevelAssignment(
          lVarName, right, function, constraints, ssa);

      BooleanFormula assignmentFormula = bfmgr.and(firstLevelFormula, secondLevelFormula);

      // updates
      if (isKnownMemoryLocation(leftVarName.getName())) {
        String leftMemLocationName = makeMemoryLocationVariableName(leftVarName.getName());
        Formula leftMemLocation = makeConstant(leftVarName.withName(leftMemLocationName), ssa);

        // update all pointers:
        // if a pointer is aliased to the assigned variable,
        // update that pointer to reflect the new aliasing,
        // otherwise only update the index
        List<Variable<CType>> ptrVarNames = getAllPointerVariablesFromSsaMap(ssa);
        Formula newLeftVar = leftVariable;
        for (Variable<CType> ptrVarName : ptrVarNames) {
          String varName = removePointerMask(ptrVarName.getName());
          if (!varName.equals(leftVarName)) {
            Formula var = makeVariable(leftVarName.withName(varName), ssa);
            Formula oldPtrVar = makeVariable(ptrVarName, ssa);
            makeFreshIndex(ptrVarName, ssa);
            Formula newPtrVar = makeVariable(ptrVarName, ssa);

            BooleanFormula condition = fmgr.makeEqual(var, leftMemLocation);
            BooleanFormula equivalence = fmgr.assignment(newPtrVar, newLeftVar);
            BooleanFormula update = fmgr.assignment(newPtrVar, oldPtrVar);

            BooleanFormula constraint = bfmgr.ifThenElse(condition, equivalence, update);
            constraints.addConstraint(constraint);
          }
        }

      }
      return assignmentFormula;
    }

    /** Returns whether the address of a given variable has been used before. */
    private boolean isKnownMemoryLocation(String varName) {
      assert varName != null;
      List<Variable<CType>> memLocations = getAllMemoryLocationsFromSsaMap(ssa);
      String memVarName = makeMemoryLocationVariableName(varName);
      return memLocations.contains(Variable.create(memVarName, null));
    }

    /** Returns a new variable name for every malloc call.
     * @param pT */
    private Variable<CType> makeFreshMallocVariableName(CType pT) {
      int idx = ssa.getIndex(MALLOC_COUNTER_VARIABLE_NAME);

      if (idx == VARIABLE_UNSET) {
        idx = VARIABLE_UNINITIALIZED;
      }

      ssa.setIndex(Variable.create(MALLOC_COUNTER_VARIABLE_NAME, pT), idx + 1);
      return Variable.create(MALLOC_VARIABLE_PREFIX + idx, pT);
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
      Variable<CType> var = scopedIfNecessary(idExp, function);
      return makeFreshVariable(var, ssa);
    }

    protected Formula makeUIF(CExpression exp) {
      warnUnsafeAssignment();
      logDebug("Assigning to ", exp);

      String var = scoped(exprToVarName(exp), function);
      Variable<CType> v = Variable.create(var, exp.getExpressionType());
      return makeFreshVariable(v, ssa);
    }

    @Override
    public Formula visit(CUnaryExpression pE) throws UnrecognizedCCodeException {
      return makeUIF(pE);
    }

    @Override
    public Formula visit(CFieldReference fExp) throws UnrecognizedCCodeException {

      CExpression fieldRef = fExp.getFieldOwner();
      if (fieldRef instanceof CIdExpression) {
        CSimpleDeclaration decl = ((CIdExpression) fieldRef).getDeclaration();
        if (decl instanceof CDeclaration && ((CDeclaration)decl).isGlobal()) {
          // this is the reference to a global field variable

          // we don't need to scope the variable reference
          String var = exprToVarName(fExp);

          Variable<CType> v = Variable.create(var, decl.getType());
          return makeFreshVariable(v, ssa);
        }
      }

      // else do the default
      return makeUIF(fExp);
    }

    @Override
    public Formula visit(CArraySubscriptExpression pE) throws UnrecognizedCCodeException {
      return makeUIF(pE);
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

      FormulaType<Formula> formulaType = getFormulaTypeFromCType(result);
      // PW make SSA index of * independent from argument
      int idx = makeFreshIndex(Variable.create(opname,result), ssa);
      //int idx = makeLvalIndex(opname, term, ssa, absoluteSSAIndices);

      // build the "updated" function corresponding to this operation.
      // what we do is the following:
      // C            |     MathSAT
      // *x = 1       |     <ptr_*>::2(x) = 1
      // ...
      // &(*x) = 2    |     <ptr_&>::2(<ptr_*>::1(x)) = 2
      return ffmgr.createFuncAndCall(opname, idx, formulaType, Arrays.asList( term ));
    }

    @Override
    public Formula visit(CFieldReference fexp) throws UnrecognizedCCodeException {
      String field = fexp.getFieldName();
      CExpression owner = fexp.getFieldOwner();
      Formula term = buildTerm(owner, edge, function, ssa, constraints);

      String tpname = getTypeName(owner.getExpressionType());
      String ufname =
        (fexp.isPointerDereference() ? "->{" : ".{") +
        tpname + "," + field + "}";
      FormulaList args = new AbstractFormulaList(term);


      CType expType = fexp.getExpressionType();
      FormulaType<Formula> formulaType = getFormulaTypeFromCType(expType);
      int idx = makeLvalIndex(Variable.create(ufname, expType), args, ssa);

      // see above for the case of &x and *x
      return ffmgr.createFuncAndCall(
          ufname, idx, formulaType, Arrays.asList( term ));
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
      FormulaType<Formula> formulaType = getFormulaTypeFromCType(expType);
      int idx = makeLvalIndex(Variable.create(ufname,expType), args, ssa);

      return ffmgr.createFuncAndCall(
          ufname, idx, formulaType, Arrays.asList( aterm, sterm ));
    }
  }

  private class LvalueVisitorPointers extends LvalueVisitor {
    public LvalueVisitorPointers(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(pEdge, pFunction, pSsa, pCo);
    }

    @Override
    public Formula visit(CCastExpression e) throws UnrecognizedCCodeException {
      return e.getOperand().accept(this);
    }

    @Override
    public Formula visit(CUnaryExpression pE) throws UnrecognizedCCodeException {
      if (pE.getOperator() == UnaryOperator.STAR) {
        CExpression exp = removeCast(pE.getOperand());

        if (exp instanceof CIdExpression) {
          // *a = ...
          // *((int*) a) = ...
          CIdExpression ptrId = (CIdExpression) exp;
          Variable<CType> ptrVarName = makePointerVariableName(ptrId, function, ssa);
          makeFreshIndex(ptrVarName, ssa);
          return makePointerVariable(ptrId, function, ssa);

        } else {
          // apparently valid cil output:
          // *(&s.f) = ...
          // *(s->f) = ...
          // *(a+b) = ...

          return makeUIF(pE);
        }
      } else {
        return super.visit(pE);
      }
    }
  }
}
