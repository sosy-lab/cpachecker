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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.types.CtoFormulaTypeUtils.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.IAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
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
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.RationalFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.types.CFieldTrackType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.types.CtoFormulaTypeUtils.CtoFormulaSizeofVisitor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Class containing all the code that converts C code into a formula.
 */
@Options(prefix="cpa.predicate")
public class CtoFormulaConverter {

  @Options(prefix="cpa.predicate")
  private static class CtoFormulaConverterSelector {
    @Option(description = "Handle aliasing of pointers. "
          + "This adds disjunctions to the formulas, so be careful when using cartesian abstraction.")
    private boolean handlePointerAliasing = true;
  }

  public static CtoFormulaConverter create(Configuration config,
      FormulaManagerView pFmgr, MachineModel pMachineModel, LogManager pLogger)
          throws InvalidConfigurationException {

    CtoFormulaConverterSelector options = new CtoFormulaConverterSelector();
    config.inject(options);

    if (options.handlePointerAliasing) {
      return new PointerAliasHandling(config, pFmgr, pMachineModel, pLogger);
    } else {
      return new CtoFormulaConverter(config, pFmgr, pMachineModel, pLogger);
    }
  }

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
  Set<String> nondetFunctions = ImmutableSet.of(
      "malloc", "__kmalloc", "kzalloc",
      "sscanf",
      "random");

  @Option(description="Regexp pattern for functions that should be considered as giving "
    + "a non-deterministic return value (c.f. cpa.predicate.nondedFunctions)")
  private String nondetFunctionsRegexp = "^(__VERIFIER_)?nondet_[a-z]*";
  final Pattern nondetFunctionsPattern;

  @Option(description="Name of an external function that will be interpreted as if the function "
     + "call would be replaced by an externally defined expression over the program variables."
     + " This will only work when all variables referenced by the dimacs file are global and declared before this function is called.")
  String externModelFunctionName = "__VERIFIER_externModelSatisfied";

  @Option(description = "list of functions that provide new memory on the heap."
    + " This is only used, when handling of pointers is enabled.")
  Set<String> memoryAllocationFunctions = ImmutableSet.of(
      "malloc", "__kmalloc", "kzalloc"
      );

  static final String ASSUME_FUNCTION_NAME = "__VERIFIER_assume";

  // list of functions that are pure (no side-effects)
  static final Set<String> PURE_EXTERNAL_FUNCTIONS
      = ImmutableSet.of("__assert_fail", "free", "kfree",
          "fprintf", "printf", "puts", "printk", "sprintf", "swprintf",
          "strcasecmp", "strchr", "strcmp", "strlen", "strncmp", "strrchr", "strstr"
          );

  // set of functions that may not appear in the source code
  // the value of the map entry is the explanation for the user
  static final Map<String, String> UNSUPPORTED_FUNCTIONS
      = ImmutableMap.of("pthread_create", "threads");

  static Predicate<String> startsWith(final String pPrefix) {
    return new Predicate<String>() {
        @Override
        public boolean apply(String pVariable) {
          return pVariable.startsWith(pPrefix);
        }
      };
  }

  //names for special variables needed to deal with functions
  private static final String VAR_RETURN_NAME = "__retval__";

  private static final String EXPAND_VARIABLE = "__expandVariable__";
  private int expands = 0;

  private static final String FIELD_VARIABLE = "__field_of__";
  @VisibleForTesting
  static final Predicate<String> IS_FIELD_VARIABLE = startsWith(FIELD_VARIABLE);

  private static final Set<String> SAFE_VAR_ARG_FUNCTIONS = ImmutableSet.of(
      "printf", "printk"
      );

  @Option(description = "Handle field access via extract and concat instead of new variables.")
  boolean handleFieldAccess = false;

  private final Set<String> printedWarnings = new HashSet<>();

  private final Map<String, BitvectorFormula> stringLitToFormula = new HashMap<>();
  private int nextStringLitIndex = 0;

  final MachineModel machineModel;
  private final CtoFormulaSizeofVisitor sizeofVisitor;

  final FormulaManagerView fmgr;
  final BooleanFormulaManagerView bfmgr;
  private final RationalFormulaManagerView nfmgr;
  final BitvectorFormulaManagerView efmgr;
  final FunctionFormulaManagerView ffmgr;
  final LogManager logger;

  static final int                 VARIABLE_UNSET          = -1;
  static final int                 VARIABLE_UNINITIALIZED  = 2;

  private final FunctionFormulaType<BitvectorFormula> stringUfDecl;

  CtoFormulaConverter(Configuration config, FormulaManagerView fmgr,
      MachineModel pMachineModel, LogManager logger)
          throws InvalidConfigurationException {
    config.inject(this, CtoFormulaConverter.class);

    this.fmgr = fmgr;
    this.machineModel = pMachineModel;
    this.sizeofVisitor = new CtoFormulaSizeofVisitor(pMachineModel);

    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.nfmgr = fmgr.getRationalFormulaManager();
    this.efmgr = fmgr.getBitvectorFormulaManager();
    this.ffmgr = fmgr.getFunctionFormulaManager();
    this.logger = logger;
    nondetFunctionsPattern = Pattern.compile(nondetFunctionsRegexp);

    FormulaType<BitvectorFormula> pointerType =
        efmgr.getFormulaType(machineModel.getSizeofPtr() * machineModel.getSizeofCharInBits());
    stringUfDecl = ffmgr.createFunction(
            "__string__", pointerType, FormulaType.RationalType);
  }

  private void warnUnsafeVar(CExpression exp) {
    logDebug("Unhandled expression treated as free variable", exp);
  }

  static String getLogMessage(String msg, CAstNode astNode) {
    return "Line " + astNode.getFileLocation().getStartingLineNumber()
            + ": " + msg
            + ": " + astNode.toASTString();
  }

  private static String getLogMessage(String msg, CFAEdge edge) {
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

  void log(Level level, String msg) {
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
  int getSizeof(CType pType) {
    int size = pType.accept(sizeofVisitor);
    if (size == 0) {
      // UNDEFINED: http://stackoverflow.com/questions/1626446/what-is-the-size-of-an-empty-struct-in-c
      log(Level.WARNING, "NOTE: Empty structs are UNDEFINED! (" + pType.toString() + ")");
    }
    return size;
  }

  Variable scopedIfNecessary(CIdExpression var, SSAMapBuilder ssa, String function) {
    return Variable.create(var.getDeclaration().getQualifiedName(), var.getExpressionType());
  }

  Variable makeFieldVariable(Variable pName, CFieldReference fExp, SSAMapBuilder ssa) {
    Pair<Integer, Integer> msb_lsb = getFieldOffsetMsbLsb(fExp);
    // NOTE: ALWAYS use pName.getType(),
    // because pName.getType() could be an instance of CFieldTrackType
    return Variable.create(
        makeFieldVariableName(pName.getName(), msb_lsb, ssa),
        new CFieldTrackType(fExp.getExpressionType(), pName.getType(), getRealFieldOwner(fExp).getExpressionType()));
  }

  public FormulaType<?> getFormulaTypeFromCType(CType type) {
    int byteSize = getSizeof(type);

    int bitsPerByte = machineModel.getSizeofCharInBits();
    // byte to bits
    return efmgr.getFormulaType(byteSize * bitsPerByte);
  }

  static boolean hasRepresentableDereference(Variable v) {
    return isRepresentableType(dereferencedType(v.getType()));
  }

  static boolean hasRepresentableDereference(CExpression e) {
    return isRepresentableType(dereferencedType(e.getExpressionType()));
  }

  /** prefixes function to variable name
  * Call only if you are sure you have a local variable!
  */
  static String scoped(String var, String function) {
    return function + "::" + var;
  }

  /**
   * Create a variable name that is used to store the return value of a function
   * temporarily (between the return statement and the re-entrance in the caller function).
   */
  static String getReturnVarName(String function) {
    return scoped(VAR_RETURN_NAME, function);
  }

  /**
   * This method eleminates all spaces from an expression's ASTString and returns
   * the new String.
   *
   * @param e the expression which should be named
   * @return the name of the expression
   */
  static String exprToVarName(IAstNode e) {
    return e.toASTString().replaceAll("[ \n\t]", "");
  }

  static String getTypeName(final CType tp) {

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
  int makeFreshIndex(String name, CType type, SSAMapBuilder ssa) {
    return getIndex(name, type, ssa, true);
  }

  /**
   * This method returns the index of the given variable in the ssa map, if there
   * is none, it creates one with the value 1.
   *
   * @return the index of the variable
   */
  int getIndex(String name, CType type, SSAMapBuilder ssa) {
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
    if (t != null && !areEqual(t, type)) {

      if (getFormulaTypeFromCType(t) != getFormulaTypeFromCType(type)) {
        throw new UnsupportedOperationException(
            "Variable " + name + " used with types of different sizes! " +
                "(Type1: " + t + ", Type2: " + type + ")");
      } else {
        log(Level.WARNING,
            "Variable " + name + " was found with multiple types!"
                + " Analysis with bitvectors could fail! "
                + "(Type1: " + t + ", Type2: " + type + ")");
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
  static int makeLvalIndex(String varName, CType type, FormulaList args, SSAMapBuilder ssa) {
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
  Formula makeConstant(String name, CType type, SSAMapBuilder ssa) {
    // TODO better use variables without index (this piece of code prevents
    // SSAMapBuilder from checking for strict monotony)
    int idx = ssa.getIndex(name);
    assert idx <= 1 : name + " is assumed to be constant there was an assignment to it";
    if (idx != 1) {
      setSsaIndex(ssa, name, type, 1); // set index so that predicates will be instantiated correctly
    }

    return fmgr.makeVariable(this.getFormulaTypeFromCType(type), name, 1);
  }
  Formula makeConstant(Variable var, SSAMapBuilder ssa) {
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
  Formula makeVariable(String name, CType type, SSAMapBuilder ssa) {
    return resolveFields(name, type, ssa, false);
  }
  Formula makeVariable(Variable var, SSAMapBuilder ssa) {
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
  Formula makeFreshVariable(String name, CType type, SSAMapBuilder ssa) {
    return resolveFields(name, type, ssa, true);
  }


  /** Takes a (scoped) struct variable name and returns the field variable name. */
  static String makeFieldVariableName(String scopedId, Pair<Integer, Integer> msb_lsb, SSAMapBuilder ssa) {
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


  BitvectorFormula makeStringLiteral(String literal) {
    BitvectorFormula result = stringLitToFormula.get(literal);

    if (result == null) {
      // generate a new string literal. We generate a new UIf
      int n = nextStringLitIndex++;
      result = ffmgr.createUninterpretedFunctionCall(
          stringUfDecl, ImmutableList.of(nfmgr.makeNumber(n)));
      stringLitToFormula.put(literal, result);
    }

    return result;
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
  Formula makeCast(CType fromType, CType toType, Formula formula) {
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

  Formula makeCast(CCastExpression e, Formula inner) {
    CType after = e.getExpressionType();
    CType before = e.getOperand().getExpressionType();
    return makeCast(before, after, inner);
  }

  /**
   * Change the size of the given formula from fromType to toType.
   * This method extracts or concats with nondet-bits.
   */
  BitvectorFormula makeExtractOrConcatNondet(CType pFromType, CType pToType, Formula pFormula) {
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
  BitvectorFormula changeFormulaSize(int sfrombits, int stobits, BitvectorFormula pFormula) {
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
      ret = fmgr.makeConcat(extendBits, pFormula);
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

      ret = fmgr.makeConcat(extendBits, pFormula);
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
  CType getImplicitCType(CType pT1, CType pT2) {
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

    if (pT1 instanceof CPointerType && pT2 instanceof CFunctionType) {
      if (((CPointerType)pT1).getType() instanceof CFunctionType) {
        return pT1;
      }
    } else if (pT2 instanceof CPointerType && pT1 instanceof CPointerType) {
      if (((CPointerType)pT2).getType() instanceof CFunctionType) {
        return pT2;
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

  CType getPromotedCType(CType t) {
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
    Constraints constraints = new Constraints(bfmgr);

    BooleanFormula edgeFormula = createFormulaForEdge(edge, function, ssa, constraints);

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
      StatementToFormulaVisitor v = getStatementVisitor(edge, function, ssa, constraints);
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
    final String varName = decl.getQualifiedName();

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
    StatementToFormulaVisitor v = getStatementVisitor(edge, function, ssa, constraints);
    return assign.accept(v);
  }


  protected BooleanFormula makeExitFunction(CFunctionSummaryEdge ce, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {

    CFunctionCall retExp = ce.getExpression();
    if (retExp instanceof CFunctionCallStatement) {
      // this should be a void return, just do nothing...
      return bfmgr.makeBoolean(true);

    } else if (retExp instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement exp = (CFunctionCallAssignmentStatement)retExp;
      String retVarName = getReturnVarName(function);

      CFunctionCallExpression funcCallExp = exp.getRightHandSide();
      CType retType = getReturnType(funcCallExp, ce);

      Formula retVar = makeVariable(retVarName, retType, ssa);
      CExpression e = exp.getLeftHandSide();

      function = ce.getSuccessor().getFunctionName();
      Formula outvarFormula = buildLvalueTerm(e, ce, function, ssa, constraints);
      retVar = makeCast(retType, e.getExpressionType(), retVar);
      BooleanFormula assignments = fmgr.assignment(outvarFormula, retVar);

      return assignments;
    } else {
      throw new UnrecognizedCCodeException("Unknown function exit expression", ce, retExp.asStatement());
    }
  }

  CType getReturnType(CFunctionCallExpression funcCallExp, CFAEdge edge) throws UnrecognizedCCodeException {
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

    if (fn.getFunctionDefinition().getType().takesVarArgs()) {
      if (formalParams.size() > actualParams.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", edge);
      }

      if (!SAFE_VAR_ARG_FUNCTIONS.contains(fn.getFunctionName())) {
        log(Level.WARNING, "Ignoring parameters passed as varargs to function "
                           + fn.getFunctionName() + " in line " + edge.getLineNumber());
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
      if (formalParam.getType() instanceof CPointerType) {
        log(Level.WARNING, "Program contains pointer parameter; analysis is imprecise in case of aliasing.");
        logDebug("Ignoring the semantics of pointer for parameter "
            + formalParam.getName(), fn.getFunctionDefinition());
      }
      CExpression paramExpression = actualParams.get(i++);
      // get value of actual parameter
      Formula actualParam = buildTerm(paramExpression, edge, callerFunction, ssa, constraints);

      final String varName = formalParam.getQualifiedName();
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

  protected BooleanFormula makeReturn(CExpression rightExp, CReturnStatementEdge edge, String function,
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
      String retVarName = getReturnVarName(function);

      CType expressionType = rightExp.getExpressionType();
      CType returnType =
          ((CFunctionEntryNode)edge.getSuccessor().getEntryNode())
            .getFunctionDefinition()
            .getType()
            .getReturnType();
      BooleanFormula assignments = makeAssignment(retVarName, returnType,
          expressionType, retval, ssa);

      return assignments;
    }
  }

  private BooleanFormula makeAssume(CAssumeEdge assume, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {

    return makePredicate(assume.getExpression(), assume.getTruthAssumption(),
        assume, function, ssa, constraints);
  }

  Formula buildTerm(CExpression exp, CFAEdge edge, String function,
      SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {
    return exp.accept(getCExpressionVisitor(edge, function, ssa, constraints));
  }

  Formula buildLvalueTerm(CExpression exp, CFAEdge edge, String function,
      SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {
    return exp.accept(getLvalueVisitor(edge, function, ssa, constraints));
  }

  BooleanFormula makeNondetAssignment(Formula left, Formula right) {
    BitvectorFormulaManagerView bitvectorFormulaManager = efmgr;
    FormulaType<Formula> tl = fmgr.getFormulaType(left);
    FormulaType<Formula> tr = fmgr.getFormulaType(right);
    if (tl == tr) {
      return fmgr.assignment(left, right);
    }

    if (tl.isBitvectorType() && tr.isBitvectorType()) {

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

  void warnToComplex(IAstNode node) {
    if (logger.wouldBeLogged(Level.FINEST)) {
      if (handleFieldAccess) {
        log(Level.FINEST, "Ignoring pointer aliasing, because statement is too complex, please simplify: " + node.toASTString() + " (Line: " + node.getFileLocation().getStartingLineNumber() + ")");
      } else {
        log(Level.FINEST, "Ignoring pointer aliasing, because statement is too complex, please simplify or enable handleFieldAccess and handleFieldAliasing: " + node.toASTString() + " (Line: " + node.getFileLocation().getStartingLineNumber() + ")");
      }
    }
  }

  <T extends Formula> T ifTrueThenOneElseZero(FormulaType<T> type, BooleanFormula pCond) {
    T one = fmgr.makeNumber(type, 1);
    T zero = fmgr.makeNumber(type, 0);
    return bfmgr.ifThenElse(pCond, one, zero);
  }

  <T extends Formula> BooleanFormula toBooleanFormula(T pF) {
    // If this is not a predicate, make it a predicate by adding a "!= 0"
    T zero = fmgr.makeNumber(fmgr.getFormulaType(pF), 0);
    return bfmgr.not(fmgr.makeEqual(pF, zero));
  }

  private BooleanFormula makePredicate(CExpression exp, boolean isTrue, CFAEdge edge,
      String function, SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {

    if (IndirectionVisitor.getIndirectionLevel(exp) > supportedIndirectionLevel) {
      warnToComplex(exp);
    }

    Formula f = exp.accept(getCExpressionVisitor(edge, function, ssa, constraints));
    BooleanFormula result = toBooleanFormula(f);

    if (!isTrue) {
      result = bfmgr.not(result);
    }
    return result;
  }

  public BooleanFormula makePredicate(CExpression exp, CFAEdge edge, String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    Constraints constraints = new Constraints(bfmgr);
    BooleanFormula f = makePredicate(exp, true, edge, function, ssa, constraints);
    return bfmgr.and(f, constraints.get());
  }

  protected StatementToFormulaVisitor getStatementVisitor(CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, Constraints pConstraints) {
    ExpressionToFormulaVisitor ev = getCExpressionVisitor(pEdge, pFunction, pSsa, pConstraints);
    return new StatementToFormulaVisitor(ev);
  }

  protected ExpressionToFormulaVisitor getCExpressionVisitor(CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, Constraints pCo) {
    if (lvalsAsUif) {
      return new ExpressionToFormulaVisitorUIF(this, pEdge, pFunction, pSsa, pCo);
    } else {
      return new ExpressionToFormulaVisitor(this, pEdge, pFunction, pSsa, pCo);
    }
  }

  protected LvalueVisitor getLvalueVisitor(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
    if (lvalsAsUif) {
      return new LvalueVisitorUIF(this, pEdge, pFunction, pSsa, pCo);
    } else {
      return new LvalueVisitor(this, pEdge, pFunction, pSsa, pCo);
    }
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
  BitvectorFormula accessField(CFieldReference fExp, Formula f) {
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

  Formula replaceField(CFieldReference fExp, Formula pLVar, Formula pRightVariable) {
    Pair<Integer, Integer> msb_Lsb = getFieldOffsetMsbLsb(fExp);

    return replaceField(msb_Lsb, pLVar, pRightVariable);
  }


  /**
   * Returns the given struct but without the bits indicated by the given
   * CFieldReference.
   */
  Formula withoutField(CFieldReference fExp, Formula f) {
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
    CCompositeType structType = (CCompositeType)simplifyType(fieldRef.getExpressionType());

    // f is now the structure, access it:
    int bitsPerByte = machineModel.getSizeofCharInBits();

    // call getFieldOffset in all cases to check whether the field actually exists
    int offset = getFieldOffset(structType, fExp.getFieldName(), fExp.getExpressionType())
        * bitsPerByte;

    if (structType.getKind() == ComplexTypeKind.UNION) {
      offset = 0;
    } else {
      checkArgument(structType.getKind() == ComplexTypeKind.STRUCT,
          "Illegal field access in expression %s", fExp);
    }

    int fieldSize = getSizeof(fExp.getExpressionType()) * bitsPerByte;
    int lsb = offset;
    int msb = offset + fieldSize - 1;
    Pair<Integer, Integer> msb_Lsb = Pair.of(msb, lsb);
    return msb_Lsb;
  }

  /**
   * Returns the offset of the given field in the given struct in bytes.
   *
   * This function does not handle UNIONs or ENUMs!
   */
  private int getFieldOffset(CCompositeType structType, String fieldName, CType assertFieldType) {
      int off = 0;
      for (CCompositeTypeMemberDeclaration member : structType.getMembers()) {
        if (member.getName().equals(fieldName)) {
          if (assertFieldType != null) {
            if (!areEqual(assertFieldType, member.getType())) {
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

  static CExpression removeCast(CExpression exp) {
    if (exp instanceof CCastExpression) {
      return removeCast(((CCastExpression) exp).getOperand());
    }
    return exp;
  }

  static CRightHandSide removeCast(CRightHandSide exp) {
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
  protected final int supportedIndirectionLevel = 1;

  /**
   * Returns true when we are able to produce a variable<CType> from this expression.
   * With this method we are able to control which expressions we handle and
   * which we just create variables for.
   * @param exp the expression.
   * @param level the current level of indirection.
   * @return true if we can create a variable from this expression.
   */
  boolean isSupportedExpression(CExpression exp, int level) {
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
  boolean isSupportedExpression(CExpression exp) {
    return isSupportedExpression(exp, 0);
  }

  /**
   * We call this method for unsupported Expressions and just make a new Variable.
   */
  Formula makeVariableUnsafe(CExpression exp, String function, SSAMapBuilder ssa, boolean makeFresh) {

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
}
