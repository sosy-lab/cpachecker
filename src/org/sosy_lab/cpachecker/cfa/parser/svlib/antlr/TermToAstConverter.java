// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibIndexedSymbol;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBitVectorConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFloatingPointConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRoundingModeConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ApplicationTermContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.IdentifierContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.LetTermContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.QualIdentifierTermContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.Qual_identiferContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.SpecConstantTermContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.Spec_constantContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.Var_bindingContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibAnyType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibFloatingPointType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.java_smt.api.FloatingPointNumber;
import org.sosy_lab.java_smt.api.FormulaType;

class TermToAstConverter extends AbstractAntlrToAstConverter<SvLibTerm> {

  /**
   * The currently visible let bindings, innermost frame last. Bound variables are replaced by their
   * bound term during conversion, so let expressions never appear in the resulting AST.
   */
  private final Deque<ImmutableMap<String, SvLibTerm>> letBindings = new ArrayDeque<>();

  public TermToAstConverter(SvLibScope pScope, Path pFilePath) {
    super(pScope, pFilePath);
  }

  public TermToAstConverter(SvLibScope pScope) {
    super(pScope);
  }

  @Override
  public SvLibTerm visitQualIdentifierTerm(QualIdentifierTermContext ctx) {
    Qual_identiferContext qualIdentifier = ctx.qual_identifer();
    FileLocation fileLocation = fileLocationFromContext(ctx);

    // An indexed identifier that is no application is a constant, e.g. (_ bv5 32)
    if (qualIdentifier.GRW_As() == null && qualIdentifier.identifier().GRW_Underscore() != null) {
      return indexedConstant(qualIdentifier.identifier());
    }

    String identifier = nameOf(qualIdentifier);

    // We handle the case that it is a pre-defined constant like true or false
    if (ImmutableSet.of("true", "false").contains(identifier)) {
      return new SvLibBooleanConstantTerm(identifier.equals("true"), fileLocation);
    }

    // The rounding modes of the theory of floating point numbers are constants as well
    if (SvLibRoundingModeConstantTerm.isRoundingMode(identifier)) {
      return new SvLibRoundingModeConstantTerm(
          SvLibRoundingModeConstantTerm.fromName(identifier), fileLocation);
    }

    // Let-bound variables shadow declared variables and are replaced by their bound term
    for (ImmutableMap<String, SvLibTerm> frame : letBindings) {
      SvLibTerm boundTerm = frame.get(identifier);
      if (boundTerm != null) {
        return boundTerm;
      }
    }

    // We now handle the general case of a variable
    return new SvLibIdTerm(scope.getVariable(identifier).toSimpleDeclaration(), fileLocation);
  }

  @Override
  public SvLibTerm visitLetTerm(LetTermContext ctx) {
    // All bound terms are evaluated in the environment outside the let, since in SMT-LIB the
    // bindings of a single let are simultaneous and may not refer to each other.
    ImmutableMap.Builder<String, SvLibTerm> frameBuilder = ImmutableMap.builder();
    for (Var_bindingContext binding : ctx.var_binding()) {
      String name = nameOf(Objects.requireNonNull(binding).symbol());
      frameBuilder.put(name, binding.term().accept(this));
    }
    letBindings.push(frameBuilder.buildOrThrow());
    try {
      return ctx.term().accept(this);
    } finally {
      letBindings.pop();
    }
  }

  /** The constant that an indexed identifier denotes, e.g. the bitvector (_ bv5 32). */
  private SvLibTerm indexedConstant(IdentifierContext ctx) {
    if (ctx.index().size() == 1) {
      Verify.verify(ctx.index().size() == 1, "BitVec should have exactly one index");
      BigInteger bitvectorSize = new BigInteger(ctx.index(0).getText());
      String valueString = ctx.symbol().getText();
      BigInteger bitvectorValue;
      if (valueString.startsWith("bv")) {
        bitvectorValue = new BigInteger(valueString.substring("bv".length()));
      } else if (valueString.startsWith("#b")) {
        bitvectorValue = new BigInteger(valueString.substring("#b".length()), 2);
      } else if (valueString.startsWith("#x")) {
        bitvectorValue = new BigInteger(valueString.substring("#b".length()), 16);
      } else {
        throw new UnsupportedOperationException(
            "Unsupported format for bitvector constant: " + valueString);
      }

      // In case the parsed int does not fit into an integer we want to crash explicitly
      return new SvLibBitVectorConstantTerm(
          bitvectorValue, bitvectorSize.intValueExact(), fileLocationFromContext(ctx));
    }

    throw new UnsupportedOperationException(
        "Underscore identifiers apart from bitvectors are not yet implemented");
  }

  @Override
  public SvLibTerm visitSpecConstantTerm(SpecConstantTermContext ctx) {
    Spec_constantContext specConstantContext = ctx.spec_constant();
    if (specConstantContext.numeral() != null) {
      return new SvLibIntegerConstantTerm(
          new BigInteger(specConstantContext.numeral().getText()), fileLocationFromContext(ctx));
    } else if (specConstantContext.hexadecimal() != null) {
      return new SvLibBitVectorConstantTerm(
          new BigInteger(specConstantContext.hexadecimal().getText().substring(2), 16),
          (specConstantContext.hexadecimal().getText().length() - 2) * 4,
          SvLibBitVectorConstantTerm.Notation.HEXADECIMAL,
          fileLocationFromContext(ctx));
    } else if (specConstantContext.binary() != null) {
      return new SvLibBitVectorConstantTerm(
          new BigInteger(specConstantContext.binary().getText().substring(2), 2),
          specConstantContext.binary().getText().length() - 2,
          SvLibBitVectorConstantTerm.Notation.BINARY,
          fileLocationFromContext(ctx));
    } else {
      throw new IllegalArgumentException(
          "The constant %s is currently not supported.".formatted(ctx.getText()));
    }
  }

  @Override
  public SvLibTerm visitApplicationTerm(ApplicationTermContext ctx) {
    Qual_identiferContext functionSymbolContext = ctx.qual_identifer();
    List<SvLibTerm> arguments =
        transformedImmutableListCopy(
            ctx.term(), termContext -> Objects.requireNonNull(termContext).accept(this));

    // A literal of the theory of floating point numbers is written as an application of "fp" to its
    // three parts, but it denotes a constant and not an application of an operator.
    if (functionSymbolContext.GRW_As() == null && nameOf(functionSymbolContext).equals("fp")) {
      return createFloatingPointConstant(arguments, fileLocationFromContext(ctx));
    }

    SvLibFunctionDeclaration declaration;
    if (functionSymbolContext.GRW_As() == null
        && functionSymbolContext.identifier().GRW_Underscore() != null) {
      declaration =
          getVariableDeclarationForIndexedSymbol(functionSymbolContext.identifier(), arguments);
    } else if (functionSymbolContext.GRW_As() != null
        && nameOf(functionSymbolContext.identifier()).equals("const")) {
      // The qualified identifier (as const (Array K V)) applied to a default element denotes a
      // constant array.
      SvLibType sort = new SvLibSortToAstTypeConverter(scope).visit(functionSymbolContext.sort());
      Verify.verify(
          sort instanceof SvLibSmtLibArrayType,
          "The sort of a constant array must be an array sort, but was: %s",
          sort);
      SvLibSmtLibArrayType arrayType = (SvLibSmtLibArrayType) sort;
      Verify.verify(arguments.size() == 1);
      Verify.verify(
          SvLibType.canBeCastTo(
              arguments.getFirst().getExpressionType(), arrayType.getValuesType()));
      declaration =
          SmtLibTheoryDeclarations.arrayConst(arrayType.getKeysType(), arrayType.getValuesType());
    } else {
      declaration =
          getVariableDeclarationForSymbol(
              nameOf(functionSymbolContext), scope.getLogics(), arguments);
    }

    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(declaration, fileLocationFromContext(functionSymbolContext)),
        arguments,
        fileLocationFromContext(ctx));
  }

  /**
   * Create the literal of the theory of floating point numbers that consists of the given sign,
   * exponent and significand, which are written as bitvector literals.
   */
  private SvLibTerm createFloatingPointConstant(
      List<SvLibTerm> pArguments, FileLocation pFileLocation) {
    Verify.verify(
        pArguments.size() == 3,
        "A floating point literal consists of a sign, an exponent and a significand, but %s parts"
            + " were given",
        pArguments.size());
    ImmutableList.Builder<SvLibBitVectorConstantTerm> partsCollector = ImmutableList.builder();
    for (SvLibTerm argument : pArguments) {
      Verify.verify(
          argument instanceof SvLibBitVectorConstantTerm,
          "The parts of a floating point literal have to be bitvector literals, but one was: %s",
          argument.toASTString());
      partsCollector.add((SvLibBitVectorConstantTerm) argument);
    }
    ImmutableList<SvLibBitVectorConstantTerm> parts = partsCollector.build();
    SvLibBitVectorConstantTerm sign = parts.getFirst();
    SvLibBitVectorConstantTerm exponent = parts.get(1);
    SvLibBitVectorConstantTerm significand = parts.get(2);
    Verify.verify(sign.getSize() == 1, "The sign of a floating point literal is a single bit");
    return new SvLibFloatingPointConstantTerm(
        FloatingPointNumber.of(
            FloatingPointNumber.Sign.of(!sign.getValue().equals(BigInteger.ZERO)),
            exponent.getValue(),
            significand.getValue(),
            FormulaType.getFloatingPointTypeFromSizesWithoutHiddenBit(
                exponent.getSize(), significand.getSize())),
        pFileLocation);
  }

  private SvLibFunctionDeclaration getVariableDeclarationForSymbol(
      String pSymbol, Set<SmtLibLogic> pLogics, List<SvLibTerm> pArguments) {

    // The theory of floating point numbers, whose operators that round take the rounding mode
    // as first argument
    Optional<SvLibSmtLibFloatingPointType> floatingPointOperand =
        pArguments.stream()
            .map(SvLibTerm::getExpressionType)
            .filter(SvLibSmtLibFloatingPointType.class::isInstance)
            .map(SvLibSmtLibFloatingPointType.class::cast)
            .findFirst();
    if (floatingPointOperand.isPresent()) {
      SvLibSmtLibFloatingPointType operandType = floatingPointOperand.orElseThrow();
      switch (pSymbol) {
        case "fp.add" -> {
          return SmtLibTheoryDeclarations.floatingPointAddition(operandType);
        }
        case "fp.sub" -> {
          return SmtLibTheoryDeclarations.floatingPointSubtraction(operandType);
        }
        case "fp.mul" -> {
          return SmtLibTheoryDeclarations.floatingPointMultiplication(operandType);
        }
        case "fp.div" -> {
          return SmtLibTheoryDeclarations.floatingPointDivision(operandType);
        }
        case "fp.sqrt" -> {
          return SmtLibTheoryDeclarations.floatingPointSquareRoot(operandType);
        }
        case "fp.roundToIntegral" -> {
          return SmtLibTheoryDeclarations.floatingPointRoundToIntegral(operandType);
        }
        case "fp.rem" -> {
          return SmtLibTheoryDeclarations.floatingPointRemainder(operandType);
        }
        case "fp.neg" -> {
          return SmtLibTheoryDeclarations.floatingPointNegation(operandType);
        }
        case "fp.abs" -> {
          return SmtLibTheoryDeclarations.floatingPointAbsolute(operandType);
        }
        case "fp.max" -> {
          return SmtLibTheoryDeclarations.floatingPointMaximum(operandType);
        }
        case "fp.min" -> {
          return SmtLibTheoryDeclarations.floatingPointMinimum(operandType);
        }
        case "fp.lt" -> {
          return SmtLibTheoryDeclarations.floatingPointLessThan(operandType);
        }
        case "fp.leq" -> {
          return SmtLibTheoryDeclarations.floatingPointLessEqual(operandType);
        }
        case "fp.gt" -> {
          return SmtLibTheoryDeclarations.floatingPointGreaterThan(operandType);
        }
        case "fp.geq" -> {
          return SmtLibTheoryDeclarations.floatingPointGreaterEqual(operandType);
        }
        case "fp.eq" -> {
          return SmtLibTheoryDeclarations.floatingPointEquality(operandType);
        }
        case "fp.isNaN" -> {
          return SmtLibTheoryDeclarations.floatingPointIsNaN(operandType);
        }
        case "fp.isInfinite" -> {
          return SmtLibTheoryDeclarations.floatingPointIsInfinite(operandType);
        }
        case "fp.isZero" -> {
          return SmtLibTheoryDeclarations.floatingPointIsZero(operandType);
        }
        case "fp.isNegative" -> {
          return SmtLibTheoryDeclarations.floatingPointIsNegative(operandType);
        }
        case "fp.isSubnormal" -> {
          return SmtLibTheoryDeclarations.floatingPointIsSubnormal(operandType);
        }
        case "fp.isNormal" -> {
          return SmtLibTheoryDeclarations.floatingPointIsNormal(operandType);
        }
        default -> {}
      }
    }

    // The equality of the core theory of SMT-LIB is generic over the type of its arguments, so it
    // is resolved for floating point numbers here and for the other types in their theory below.
    if (pSymbol.equals("=")
        && pArguments.size() == 2
        && pArguments.getFirst().getExpressionType()
            instanceof SvLibSmtLibFloatingPointType floatingPointType) {
      return SmtLibTheoryDeclarations.floatingPointIdentity(floatingPointType);
    }

    // The conversions between the theory of bitvectors and the one of integers, and the one that
    // reads the bits of a floating point number, are not indexed, because the size of the
    // bitvector follows from the type of the argument.
    switch (pSymbol) {
      case "sbv_to_int", "ubv_to_int" -> {
        Verify.verify(pArguments.size() == 1);
        Verify.verify(
            pArguments.getFirst().getExpressionType() instanceof SvLibSmtLibBitVectorType,
            "%s is not applied to a bitvector",
            pSymbol);
        return SmtLibTheoryDeclarations.bitVectorToInt(
            pSymbol.equals("sbv_to_int"),
            ((SvLibSmtLibBitVectorType) pArguments.getFirst().getExpressionType()).getSize());
      }
      case "fp.to_ieee_bv" -> {
        Verify.verify(pArguments.size() == 1);
        Verify.verify(
            pArguments.getFirst().getExpressionType() instanceof SvLibSmtLibFloatingPointType,
            "fp.to_ieee_bv is not applied to a floating point number");
        return SmtLibTheoryDeclarations.floatingPointAsBitVector(
            (SvLibSmtLibFloatingPointType) pArguments.getFirst().getExpressionType());
      }
      default -> {}
    }

    // A declared function is uninterpreted, so it is not part of any theory and is resolved from
    // the scope instead.
    if (scope.hasFunctionDeclaration(pSymbol)) {
      SvLibSmtFunctionDeclaration declaration = scope.getFunctionDeclaration(pSymbol);
      Verify.verify(
          declaration.getType().getParameters().size() == pArguments.size(),
          "The function %s is applied to %s arguments instead of %s",
          pSymbol,
          pArguments.size(),
          declaration.getType().getParameters().size());
      return SvLibFunctionDeclaration.uninterpretedFunction(
          declaration.getFileLocation(), declaration.getType(), pSymbol);
    }

    // Match Integer Arithmetic logic
    if (FluentIterable.from(pLogics).anyMatch(SmtLibLogic::containsIntegerArithmetic)
        && FluentIterable.from(pArguments)
            .allMatch(
                term ->
                    SvLibType.canBeCastTo(
                        term.getExpressionType(), SvLibSmtLibPredefinedType.INT))) {
      switch (pSymbol) {
        case "=" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_EQUALITY;
        }
        case "<" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_LESS_THAN;
        }
        case "<=" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN;
        }
        case ">" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_GREATER_THAN;
        }
        case ">=" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_GREATER_EQUAL_THAN;
        }
        case "-" -> {
          return SmtLibTheoryDeclarations.intSubtraction(pArguments.size());
        }
        case "+" -> {
          return SmtLibTheoryDeclarations.intAddition(pArguments.size());
        }
        case "*" -> {
          // Only a special case of multiplication is actually supported in LIA
          // but we do not care, because we either way use all theories together.
          // In case this becomes a problem, we can introduce a separate check for LIA here.
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_MULTIPLICATION;
        }
      }
    }

    // Match Non-Linear Integer Arithmetic logic
    if (FluentIterable.from(pLogics).anyMatch(SmtLibLogic::containsNonLinearIntegerArithmetic)) {
      switch (pSymbol) {
        case "mod" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_MOD;
        }
        case "div" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.INT_DIV;
        }
      }
    }

    // Match the core logic of SMT-LIB
    switch (pSymbol) {
      case "not" -> {
        Verify.verify(pArguments.size() == 1);
        return SmtLibTheoryDeclarations.BOOL_NEGATION;
      }
      case "and" -> {
        return SmtLibTheoryDeclarations.boolConjunction(pArguments.size());
      }
      case "or" -> {
        return SmtLibTheoryDeclarations.boolDisjunction(pArguments.size());
      }
      case "=>" -> {
        return SmtLibTheoryDeclarations.boolImplication(pArguments.size());
      }
      case "ite" -> {
        Verify.verify(pArguments.size() == 3);
        return SmtLibTheoryDeclarations.ite(pArguments.get(1).getExpressionType());
      }
      case "distinct" -> {
        // distinct is generic over the (shared) argument type and always returns a boolean.
        Verify.verify(pArguments.size() >= 2);
        return SmtLibTheoryDeclarations.distinct(
            pArguments.getFirst().getExpressionType(), pArguments.size());
      }
      case "=" -> {
        // Equality is part of the core theory only for booleans, for other types it is resolved
        // in their respective theory below.
        if (pArguments.size() == 2
            && FluentIterable.from(pArguments)
                .allMatch(
                    term ->
                        SvLibType.canBeCastTo(
                            term.getExpressionType(), SvLibSmtLibPredefinedType.BOOL))) {
          return SmtLibTheoryDeclarations.BOOL_EQUALITY;
        }
      }
    }

    // Match all array stuff
    switch (pSymbol) {
      case "select" -> {
        Verify.verify(pArguments.size() == 2);
        if (pArguments.getFirst().getExpressionType() instanceof SvLibSmtLibArrayType arrayType) {

          return SmtLibTheoryDeclarations.arraySelect(
              arrayType.getKeysType(), arrayType.getValuesType());
        } else if (pArguments.getFirst().getExpressionType().equals(new SvLibAnyType())) {
          return SmtLibTheoryDeclarations.arraySelect(new SvLibAnyType(), new SvLibAnyType());
        }

        throw new IllegalArgumentException(
            "The first argument of 'select' must be of array type, but was: "
                + pArguments.getFirst().getExpressionType());
      }
      case "store" -> {
        Verify.verify(pArguments.size() == 3);
        return SmtLibTheoryDeclarations.arrayStore(
            (SvLibSmtLibType) pArguments.get(1).getExpressionType(),
            (SvLibSmtLibType) pArguments.get(2).getExpressionType());
      }
      case "=" -> {
        // Equality is only handled here if the arguments are arrays, other types are resolved
        // in their respective theory.
        if (pArguments.size() == 2
            && pArguments.getFirst().getExpressionType()
                instanceof SvLibSmtLibArrayType arrayType) {
          return SmtLibTheoryDeclarations.arrayEquality(
              arrayType.getKeysType(), arrayType.getValuesType());
        }
      }
    }

    // Match all the bitvector stuff
    if (FluentIterable.from(pLogics).anyMatch(SmtLibLogic::containsBitvectorOperations)) {
      List<SvLibType> argumentTypes =
          transformedImmutableListCopy(pArguments, SvLibTerm::getExpressionType);
      switch (pSymbol) {
        case "concat" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getLast() instanceof SvLibSmtLibBitVectorType);
          return SmtLibTheoryDeclarations.bitVectorConcat(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize(),
              ((SvLibSmtLibBitVectorType) argumentTypes.getLast()).getSize());
        }
        case "bvnot" -> {
          Verify.verify(pArguments.size() == 1);
          return SmtLibTheoryDeclarations.bitVectorBitwiseNegation(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvneg" -> {
          Verify.verify(pArguments.size() == 1);
          return SmtLibTheoryDeclarations.bitVectorComplementNegation(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvand" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.bitVectorAnd(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvnand" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorNand(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvor" -> {
          Verify.verify(pArguments.size() == 2);
          return SmtLibTheoryDeclarations.bitVectorOr(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvadd" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorAddition(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvmul" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorMul(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvudiv" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorUnsignedDivision(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvurem" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorUnsignedRemainder(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvshl" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorShiftLeft(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvlshr" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorLogicalShiftRight(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }

        /* not in SMT-LIB FixedSizeBitVectors but used by solvers Z3 & MathSAT */
        case "redxor" -> {
          Verify.verify(pArguments.size() == 1);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          return SmtLibTheoryDeclarations.bitVectorReductionXor(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "redor" -> {
          Verify.verify(pArguments.size() == 1);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          return SmtLibTheoryDeclarations.bitVectorReductionOr(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "redand" -> {
          Verify.verify(pArguments.size() == 1);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          return SmtLibTheoryDeclarations.bitVectorReductionAnd(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvcomp" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorComparison(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvxor" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorXor(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvxnor" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorXnor(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvashr" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorArithmeticShiftRight(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "=" -> {
          // Equality is only handled here if the arguments are bitvectors, other types are
          // resolved in their respective theory.
          if (pArguments.size() == 2
              && argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType bitVectorType
              && argumentTypes.getFirst().equals(argumentTypes.getLast())) {
            return SmtLibTheoryDeclarations.bitVectorEquality(bitVectorType.getSize());
          }
        }
        case "bvule" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorUnsignedLessEqual(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvult" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorUnsignedLessThan(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvuge" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorUnsignedGreaterEqual(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvugt" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorUnsignedGreaterThan(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvsle" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorSignedLessEqual(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvslt" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorSignedLessThan(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvsge" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorSignedGreaterEqual(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvsgt" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorSignedGreaterThan(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvsdiv" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorSignedDivision(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvsrem" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorSignedRemainder(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
        case "bvsub" -> {
          Verify.verify(pArguments.size() == 2);
          Verify.verify(argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
          Verify.verify(argumentTypes.getFirst().equals(argumentTypes.getLast()));
          return SmtLibTheoryDeclarations.bitVectorSubstraction(
              ((SvLibSmtLibBitVectorType) argumentTypes.getFirst()).getSize());
        }
      }
    }

    throw new IllegalArgumentException(
        "Unsupported logic for the resolution of symbol: " + pSymbol);
  }

  /** Resolves indexed identifiers like {@code (_ extract 31 31)} applied to arguments. */
  private SvLibFunctionDeclaration getVariableDeclarationForIndexedSymbol(
      IdentifierContext pCtx, List<SvLibTerm> pArguments) {
    SmtLibIndexedSymbol symbol =
        new SmtLibIndexedSymbol(
            nameOf(pCtx.symbol()),
            transformedImmutableListCopy(pCtx.index(), index -> Integer.parseInt(index.getText())));
    ImmutableList<SvLibType> argumentTypes =
        transformedImmutableListCopy(pArguments, SvLibTerm::getExpressionType);
    return switch (symbol.symbol()) {
      case "extract" -> {
        Verify.verify(symbol.indices().size() == 2);
        yield SmtLibTheoryDeclarations.bitVectorExtract(
            bitVectorSizeOfOnlyArgument(argumentTypes), symbol.index(0), symbol.index(1));
      }
      case "repeat" -> {
        Verify.verify(symbol.indices().size() == 1);
        yield SmtLibTheoryDeclarations.bitVectorRepeat(
            bitVectorSizeOfOnlyArgument(argumentTypes), symbol.index(0));
      }
      case "zero_extend" -> {
        Verify.verify(symbol.indices().size() == 1);
        int sourceSize = bitVectorSizeOfOnlyArgument(argumentTypes);
        yield SmtLibTheoryDeclarations.bitVectorZeroExtend(
            sourceSize, sourceSize + symbol.index(0));
      }
      case "sign_extend" -> {
        Verify.verify(symbol.indices().size() == 1);
        int sourceSize = bitVectorSizeOfOnlyArgument(argumentTypes);
        yield SmtLibTheoryDeclarations.bitVectorSignExtend(
            sourceSize, sourceSize + symbol.index(0));
      }
      case "int_to_bv" -> {
        Verify.verify(symbol.indices().size() == 1);
        Verify.verify(argumentTypes.size() == 1);
        yield SmtLibTheoryDeclarations.intToBitVector(symbol.index(0));
      }
      case "to_fp" -> {
        Verify.verify(symbol.indices().size() == 2);
        SvLibSmtLibFloatingPointType targetType =
            new SvLibSmtLibFloatingPointType(symbol.index(0), symbol.index(1));
        if (argumentTypes.size() == 1) {
          // A conversion of the bits of the representation of IEEE 754 loses no information and
          // therefore does not round.
          Verify.verify(
              argumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType,
              "to_fp without a rounding mode reads the bits of a bitvector");
          yield SmtLibTheoryDeclarations.floatingPointFromBitVector(
              (SvLibSmtLibBitVectorType) argumentTypes.getFirst(), targetType);
        }
        Verify.verify(
            argumentTypes.size() == 2, "A conversion into a floating point number rounds");
        yield SmtLibTheoryDeclarations.toFloatingPoint(false, argumentTypes.get(1), targetType);
      }
      case "to_fp_unsigned" -> {
        Verify.verify(symbol.indices().size() == 2);
        Verify.verify(
            argumentTypes.size() == 2, "A conversion into a floating point number rounds");
        yield SmtLibTheoryDeclarations.toFloatingPoint(
            true,
            argumentTypes.get(1),
            new SvLibSmtLibFloatingPointType(symbol.index(0), symbol.index(1)));
      }
      case "fp.to_sbv", "fp.to_ubv" -> {
        Verify.verify(symbol.indices().size() == 1);
        Verify.verify(argumentTypes.size() == 2, "A conversion of a floating point number rounds");
        Verify.verify(argumentTypes.get(1) instanceof SvLibSmtLibFloatingPointType);
        yield SmtLibTheoryDeclarations.floatingPointToBitVector(
            symbol.symbol().equals("fp.to_sbv"),
            (SvLibSmtLibFloatingPointType) argumentTypes.get(1),
            new SvLibSmtLibBitVectorType(symbol.index(0)));
      }
      default -> throw new IllegalArgumentException("Unsupported indexed symbol: " + symbol);
    };
  }

  private static int bitVectorSizeOfOnlyArgument(List<SvLibType> pArgumentTypes) {
    Verify.verify(pArgumentTypes.size() == 1);
    Verify.verify(pArgumentTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
    return ((SvLibSmtLibBitVectorType) pArgumentTypes.getFirst()).getSize();
  }
}
