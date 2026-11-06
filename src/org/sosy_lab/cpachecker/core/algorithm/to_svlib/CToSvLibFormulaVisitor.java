// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibFloatingPointType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.DynamicMemoryHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerBase;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.svlibwitnessexport.FormulaToSvLibVisitor;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;

/**
 * Writes the formulas that the analysis of a C program builds as terms of SV-LIB.
 *
 * <p>Unlike the formulas of an SV-LIB script, these contain free variables that the program does
 * not declare, for example the addresses of the memory or nondeterministic values. This visitor
 * declares a variable of the generated script for each of them, which the transformation then adds
 * to the script.
 */
final class CToSvLibFormulaVisitor extends FormulaToSvLibVisitor {

  /**
   * The character that separates the name of a nondeterministic value from the index that makes it
   * unique, see {@code CtoFormulaConverter.makeNondet}.
   */
  private static final char NONDETERMINISTIC_VALUE_SEPARATOR = '!';

  /**
   * The character that separates the name of a structure from the one of its member, for a
   * structure that is not part of the heap.
   */
  private static final char MEMBER_NAME_SEPARATOR = '$';

  /** The uninterpreted function with which the integer encoding of C writes the remainder. */
  private static final String REMAINDER_FUNCTION = "_%_";

  private final List<SvLibParsingVariableDeclaration> variablesOfFormulas = new ArrayList<>();

  private final List<SvLibParsingVariableDeclaration> freshValuesOfFormulas = new ArrayList<>();

  private final List<SvLibParsingVariableDeclaration> allocatedAddressesOfFormulas =
      new ArrayList<>();

  /** The conditions that the fresh values of {@link #freshValuesOfFormulas} have to fulfill. */
  private final List<SvLibTerm> conditionsOfFreshValues = new ArrayList<>();

  /** The number of variables for the bits of a floating point number that were declared. */
  private int numberOfBitsOfFloatingPointNumbers = 0;

  /**
   * The variables of {@link #freshValuesOfFormulas} for the bits of a floating point number, by
   * that number, so that all occurrences of the bits of a NaN in an edge have the same value.
   */
  private final Map<String, SvLibIdTerm> bitsOfFloatingPointNumbers = new HashMap<>();

  CToSvLibFormulaVisitor(FormulaManagerView pFmgr, SvLibScope pScope) {
    super(pFmgr, pScope);
  }

  /**
   * The variables that the transformed formulas introduced, for a nondeterministic value, for the
   * address of memory that is allocated on the heap or for the memory of a type. They have to be
   * declared in the generated program.
   */
  ImmutableList<SvLibParsingVariableDeclaration> getVariablesOfFormulas() {
    return ImmutableList.copyOf(variablesOfFormulas);
  }

  /**
   * The variables that the formulas transformed since the last call of this method introduced and
   * that take a new value every time the statements of the transformed edge are executed, such as
   * the result of a call whose semantics the analysis does not know.
   */
  ImmutableList<SvLibParsingVariableDeclaration> pollFreshValuesOfFormulas() {
    ImmutableList<SvLibParsingVariableDeclaration> freshValues =
        ImmutableList.copyOf(freshValuesOfFormulas);
    freshValuesOfFormulas.clear();
    bitsOfFloatingPointNumbers.clear();
    return freshValues;
  }

  /**
   * The conditions that the variables of {@link #pollFreshValuesOfFormulas} fulfill, which have to
   * be assumed after they are havoced and before the statements of the transformed edge.
   */
  ImmutableList<SvLibTerm> pollConditionsOfFreshValues() {
    ImmutableList<SvLibTerm> conditions = ImmutableList.copyOf(conditionsOfFreshValues);
    conditionsOfFreshValues.clear();
    return conditions;
  }

  /**
   * The variables for the addresses of the memory that the formulas transformed since the last call
   * of this method allocate.
   */
  ImmutableList<SvLibParsingVariableDeclaration> pollAllocatedAddressesOfFormulas() {
    ImmutableList<SvLibParsingVariableDeclaration> addresses =
        ImmutableList.copyOf(allocatedAddressesOfFormulas);
    allocatedAddressesOfFormulas.clear();
    return addresses;
  }

  /**
   * Declare a global variable for a free variable of a formula whose name is only known while the
   * formulas are built, and return its declaration.
   *
   * @param pIsFreshValue whether every execution of the statement of the transformed edge takes a
   *     new value, which is the case for a nondeterministic value and for the address of a newly
   *     allocated block of memory, but not for the memory of the program itself.
   */
  private SvLibParsingVariableDeclaration declareVariableOfFormula(
      String pName, Formula pFormula, boolean pIsFreshValue) {
    SvLibParsingVariableDeclaration declaration =
        new SvLibParsingVariableDeclaration(
            FileLocation.DUMMY,
            true,
            false,
            formulaTypeToSvLibType(fmgr.getFormulaType(pFormula)),
            pName,
            pName,
            null);
    scope.addVariable(declaration);
    variablesOfFormulas.add(declaration);
    if (pIsFreshValue) {
      freshValuesOfFormulas.add(declaration);
    }
    return declaration;
  }

  @Override
  public SvLibTerm visitFreeVariable(Formula pFormula, String pS) {
    String name = pS.replaceAll("@$", "").replaceAll("@[0-9]+", "");

    Optional<PointerBase> base = PointerBase.fromFormulaEncoding(name);
    if (base.isPresent()) {
      if (!scope.hasVariable(name)) {
        // The name of the address of memory that is allocated on the heap contains the number of
        // the allocation, and an object of the program can become part of the memory only while
        // the formulas are built, so neither can be declared before that.
        boolean isAllocation = DynamicMemoryHandler.isAllocBase(base.orElseThrow());
        SvLibParsingVariableDeclaration address =
            declareVariableOfFormula(name, pFormula, isAllocation);
        if (isAllocation) {
          allocatedAddressesOfFormulas.add(address);
        }
        return new SvLibIdTerm(address.toSimpleDeclaration(), FileLocation.DUMMY);
      }
      return new SvLibIdTerm(scope.getVariable(name).toSimpleDeclaration(), FileLocation.DUMMY);
    }

    // A nondeterministic value that CtoFormulaConverter created for a function whose semantics it
    // does not know is only a free variable of the formula, so the generated program needs a
    // variable for it. Such a value is unconstrained in both, and its name contains the index that
    // makes it unique, so it cannot be declared before the formulas are built.
    if (name.indexOf(NONDETERMINISTIC_VALUE_SEPARATOR) >= 0 && !scope.hasVariable(name)) {
      return new SvLibIdTerm(
          declareVariableOfFormula(name, pFormula, true).toSimpleDeclaration(), FileLocation.DUMMY);
    }

    // An array models the memory of all objects of one type and is unconstrained at the start of
    // the program, so it can be declared here if a formula refers to the memory of a type whose
    // declarations the program does not contain, for example the memory of a structure without
    // members. A variable of the program is not declared here, because using an unconstrained one
    // instead of it would change the semantics of the program.
    if (!scope.hasVariable(name)
        && !scope.hasVariableForQualifiedName(name)
        && (fmgr.getFormulaType(pFormula).isArrayType()
            || name.indexOf(MEMBER_NAME_SEPARATOR) >= 0)) {
      return new SvLibIdTerm(
          declareVariableOfFormula(name, pFormula, false).toSimpleDeclaration(),
          FileLocation.DUMMY);
    }

    return super.visitFreeVariable(pFormula, pS);
  }

  @Override
  public SvLibTerm visitFunction(
      Formula pFormula, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
    String name = pFunctionDeclaration.getName().replace("`", "");
    boolean returnsInteger = pFunctionDeclaration.getType().isIntegerType();

    if (pFunctionDeclaration.getKind() == FunctionDeclarationKind.FP_AS_IEEEBV) {
      return bitsOfFloatingPointNumber(
          fmgr.visit(pArgs.getFirst(), this),
          (SvLibSmtLibFloatingPointType)
              formulaTypeToSvLibType(pFunctionDeclaration.getArgumentTypes().getFirst()),
          (SvLibSmtLibBitVectorType) formulaTypeToSvLibType(pFunctionDeclaration.getType()));
    } else if (returnsInteger && pArgs.size() == 2 && name.equals(REMAINDER_FUNCTION)) {
      return remainderOfC(fmgr.visit(pArgs.getFirst(), this), fmgr.visit(pArgs.get(1), this));
    } else if (returnsInteger && pArgs.size() == 2 && name.startsWith("Integer_")) {
      // The analysis of C replaces the non-linear arithmetic of the integers by uninterpreted
      // functions, which the analysis of the script does not need to do.
      Optional<SvLibIdTerm> operator =
          switch (name) {
            case "Integer_*_" ->
                Optional.of(
                    new SvLibIdTerm(
                        SmtLibTheoryDeclarations.INT_MULTIPLICATION, FileLocation.DUMMY));
            case "Integer_/_" ->
                Optional.of(new SvLibIdTerm(SmtLibTheoryDeclarations.INT_DIV, FileLocation.DUMMY));
            default -> Optional.empty();
          };
      if (operator.isPresent()) {
        return new SvLibSymbolApplicationTerm(
            operator.orElseThrow(),
            ImmutableList.of(fmgr.visit(pArgs.getFirst(), this), fmgr.visit(pArgs.get(1), this)),
            FileLocation.DUMMY);
      }
    }
    return super.visitFunction(pFormula, pArgs, pFunctionDeclaration);
  }

  /**
   * The remainder of the division of C, which rounds towards zero. The modulo of SMT-LIB is never
   * negative, so the remainder of a negative dividend is the negated modulo of the negated
   * dividend.
   */
  private static SvLibTerm remainderOfC(SvLibTerm pDividend, SvLibTerm pDivisor) {
    SvLibTerm dividendIsNotNegative =
        new SvLibSymbolApplicationTerm(
            new SvLibIdTerm(SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN, FileLocation.DUMMY),
            ImmutableList.of(
                new SvLibIntegerConstantTerm(BigInteger.ZERO, FileLocation.DUMMY), pDividend),
            FileLocation.DUMMY);
    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(
            SmtLibTheoryDeclarations.ite(SvLibSmtLibPredefinedType.INT), FileLocation.DUMMY),
        ImmutableList.of(
            dividendIsNotNegative,
            intModulo(pDividend, pDivisor),
            intNegation(intModulo(intNegation(pDividend), pDivisor))),
        FileLocation.DUMMY);
  }

  private static SvLibTerm intModulo(SvLibTerm pDividend, SvLibTerm pDivisor) {
    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(SmtLibTheoryDeclarations.INT_MOD, FileLocation.DUMMY),
        ImmutableList.of(pDividend, pDivisor),
        FileLocation.DUMMY);
  }

  private static SvLibTerm intNegation(SvLibTerm pTerm) {
    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(SmtLibTheoryDeclarations.intSubtraction(1), FileLocation.DUMMY),
        ImmutableList.of(pTerm),
        FileLocation.DUMMY);
  }

  /**
   * The bits of the representation of IEEE 754 of the given floating point number.
   *
   * <p>SMT-LIB has no function for them, only the conversion of bits into a floating point number.
   * They are therefore a fresh value that this conversion takes to the given number, which
   * determines the bits of every number except NaN, whose bits are then any of the ones of a NaN.
   */
  private SvLibTerm bitsOfFloatingPointNumber(
      SvLibTerm pNumber,
      SvLibSmtLibFloatingPointType pNumberType,
      SvLibSmtLibBitVectorType pBitsType) {
    SvLibIdTerm existingBits = bitsOfFloatingPointNumbers.get(pNumber.toASTString());
    if (existingBits != null) {
      return existingBits;
    }
    String name =
        "__transformationBitsOfFloatingPointNumber" + numberOfBitsOfFloatingPointNumbers++;
    SvLibParsingVariableDeclaration bits =
        new SvLibParsingVariableDeclaration(
            FileLocation.DUMMY, true, false, pBitsType, name, name, null);
    scope.addVariable(bits);
    variablesOfFormulas.add(bits);
    freshValuesOfFormulas.add(bits);
    SvLibIdTerm bitsTerm = new SvLibIdTerm(bits.toSimpleDeclaration(), FileLocation.DUMMY);
    conditionsOfFreshValues.add(
        new SvLibSymbolApplicationTerm(
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.floatingPointIdentity(pNumberType), FileLocation.DUMMY),
            ImmutableList.of(
                new SvLibSymbolApplicationTerm(
                    new SvLibIdTerm(
                        SmtLibTheoryDeclarations.floatingPointFromBitVector(pBitsType, pNumberType),
                        FileLocation.DUMMY),
                    ImmutableList.of(bitsTerm),
                    FileLocation.DUMMY),
                pNumber),
            FileLocation.DUMMY));
    bitsOfFloatingPointNumbers.put(pNumber.toASTString(), bitsTerm);
    return bitsTerm;
  }
}
