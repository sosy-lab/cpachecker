// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AtomicDouble;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapWriter;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

@Options(prefix = "cpa.value.invExport")
public class ValueAnalysisResultToLoopInvariants implements AutoCloseable {

  @Option(secure = true, description = "enable if loop invariant export should consider context")
  private boolean invariantsContextSensitive = true;

  @Option(secure = true, description = "Enable to export invariants on single variables")
  private boolean exportUnary = true;

  @Option(secure = true, description = "Enable to export invariants that include two variables")
  private boolean exportBinary = true;

  @Option(
      secure = true,
      description =
          "Enable to export invariants that include three variables, "
              + "currently only effective if exportLinear is enabled, too")
  private boolean exportTernary = true;

  @Option(
      secure = true,
      description =
          "Enable to export linear equalities or inequalities over variables, e.g., ax + by + c ="
              + " 0, ax + bx + c <= 0, ax + bx + c >= 0, or ax + by + cy + d = 0")
  private boolean exportLinear = true;

  @Option(
      secure = true,
      description =
          "Enable invariants that use  an arithmetic operator"
              + "(linear invariants are enabled separately)")
  private boolean exportArithmetic = true;

  @Option(secure = true, description = "Enable invariants that use a bit operator")
  private boolean exportBitops = true;

  @Option(secure = true, description = "Enable invariants that relate (compare) two variables")
  private boolean exportRelational = true;

  @Option(
      secure = true,
      description =
          "Enable invariants that use a shift operator,"
              + " note that additionally exportBitops must be enabled")
  private boolean exportShiftops = true;

  /* TODO support?
   * @Option(
      secure = true,
      description =
          "enable if loop invariant export should remove invariants that can be derived from"
              + " others")
  private boolean removeRedundantInvariants = true;*/

  private final @Nullable ImmutableSet<CFANode> loopHeads;
  private final LogManager logger;

  private final PredicateMapWriter predExporter;
  private final Solver solver;
  private final AbstractionManager absMgr;
  private final ImmutableMap<MemoryLocation, Type> varToType;
  private final CtoFormulaConverter c2Formula;
  private final MachineModel machineModel;

  private int numBooleanInvariants = -1;
  private int numNumericInvariants = -1;

  private int numCompRelationalInvariants = -1;
  private int numArithmeticRelationalInvariants = -1;
  private int numBitRelationalInvariants = -1;

  private int numLinearInvariants = -1;

  public ValueAnalysisResultToLoopInvariants(
      final @Nullable ImmutableSet<CFANode> pLoopHeads,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {
    loopHeads = pLoopHeads;
    pConfig.inject(this);
    logger = pLogger;
    solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    predExporter = new PredicateMapWriter(pConfig, solver.getFormulaManager());
    absMgr = new AbstractionManager(new SymbolicRegionManager(solver), pConfig, pLogger, solver);
    varToType = extractVarsWithType(pCfa);
    c2Formula =
        new CtoFormulaConverter(
            new FormulaEncodingOptions(pConfig),
            solver.getFormulaManager(),
            pCfa.getMachineModel(),
            pCfa.getVarClassification(),
            pLogger,
            pShutdownNotifier,
            new CtoFormulaTypeHandler(pLogger, pCfa.getMachineModel()),
            AnalysisDirection.FORWARD);
    machineModel = pCfa.getMachineModel();
    if (!anyInvariantsConfiguredForExport()) {
      logger.log(Level.WARNING, "No invariants configured for export");
    }
  }

  private boolean anyInvariantsConfiguredForExport() {
    return exportUnary
        || (exportBinary && (exportArithmetic || exportBitops || exportLinear || exportRelational))
        || (exportTernary && exportLinear);
  }

  private ImmutableMap<MemoryLocation, Type> extractVarsWithType(final CFA pCfa) {
    ImmutableMap.Builder<MemoryLocation, Type> builder = ImmutableMap.builder();
    for (AbstractSimpleDeclaration decl :
        FluentIterable.from(pCfa.edges())
            .filter(ADeclarationEdge.class)
            .transform(ADeclarationEdge::getDeclaration)
            .filter(AbstractSimpleDeclaration.class)
            .filter(
                Predicates.or(
                    Predicates.instanceOf(AVariableDeclaration.class),
                    Predicates.instanceOf(AParameterDeclaration.class)))) {
      builder.put(MemoryLocation.forDeclaration(decl), decl.getType());
    }

    for (AParameterDeclaration decl :
        FluentIterable.from(pCfa.edges())
            .filter(FunctionCallEdge.class)
            .transform(FunctionCallEdge::getFunctionCallExpression)
            .transform(AFunctionCallExpression::getDeclaration)
            .transformAndConcat(AFunctionDeclaration::getParameters)) {
      builder.put(MemoryLocation.forDeclaration(decl), decl.getType());
    }

    return builder.buildOrThrow();
  }

  @Override
  public void close() {
    solver.close();
  }

  public void writeInvariantStatistics(final StatisticsWriter pWriter) {
    pWriter
        .beginLevel()
        .putIf(
            numBooleanInvariants >= 0,
            "Number of generated boolean value invariants",
            numBooleanInvariants)
        .putIf(
            numNumericInvariants >= 0,
            "Number of generated variable range invariants",
            numNumericInvariants)
        .putIf(
            numCompRelationalInvariants >= 0,
            "Number of generated variable comparison invariants",
            numCompRelationalInvariants)
        .putIf(
            numArithmeticRelationalInvariants >= 0,
            "Number of generated variable arithmetic computation invariants",
            numArithmeticRelationalInvariants)
        .putIf(
            numBitRelationalInvariants >= 0,
            "Number of generated variable bit computation invariants",
            numBitRelationalInvariants)
        .putIf(
            numLinearInvariants >= 0, "Number of generated linear invariants", numLinearInvariants);
  }

  public void generateAndExportLoopInvariantsAsPredicatePrecision(
      final UnmodifiableReachedSet pReached, final Appendable sb) throws IOException {
    // reset statistics
    numBooleanInvariants = -1;
    numNumericInvariants = -1;
    numCompRelationalInvariants = -1;
    numArithmeticRelationalInvariants = -1;
    numBitRelationalInvariants = -1;
    numLinearInvariants = -1;

    if (!anyInvariantsConfiguredForExport()) {
      logger.log(Level.INFO, "No invariants configured for export.");
      return;
    }

    ImmutableMultimap<Pair<CFANode, Optional<CallstackStateEqualsWrapper>>, ValueAnalysisState>
        contextLocToStates = extractAndMapRelevantValueStates(pReached);

    Map<CFANode, Collection<CandidateInvariant>> invPerLoc =
        Maps.newHashMapWithExpectedSize(contextLocToStates.keySet().size());
    Collection<CandidateInvariant> invariants;

    for (Pair<CFANode, Optional<CallstackStateEqualsWrapper>> contextLoc :
        contextLocToStates.keySet()) {
      if (!invPerLoc.containsKey(contextLoc.getFirstNotNull())) {
        invPerLoc.put(contextLoc.getFirstNotNull(), new ArrayList<>());
      }
      invariants = invPerLoc.get(contextLoc.getFirstNotNull());

      invariants.addAll(generateInvariants(contextLocToStates.get(contextLoc)));
    }

    exportAsPredicatePrecision(invPerLoc, sb);
  }

  private ImmutableMultimap<
          Pair<CFANode, Optional<CallstackStateEqualsWrapper>>, ValueAnalysisState>
      extractAndMapRelevantValueStates(final UnmodifiableReachedSet pReached) {
    CFANode loc;
    Optional<CallstackStateEqualsWrapper> callstack = Optional.empty();
    ValueAnalysisState currentValState;
    Pair<CFANode, Optional<CallstackStateEqualsWrapper>> key;
    ImmutableMultimap.Builder<
            Pair<CFANode, Optional<CallstackStateEqualsWrapper>>, ValueAnalysisState>
        multiMapBuilder = new ImmutableMultimap.Builder<>();

    for (AbstractState currentAbstractState : pReached) {
      loc = AbstractStates.extractLocation(currentAbstractState);
      if (loopHeads == null || loopHeads.contains(loc)) {
        if (invariantsContextSensitive) {
          callstack = AbstractStates.extractOptionalCallstackWraper(currentAbstractState);
        }
        currentValState =
            AbstractStates.extractStateByType(currentAbstractState, ValueAnalysisState.class);
        key = Pair.of(loc, callstack);
        multiMapBuilder.put(key, currentValState);
      }
    }

    return multiMapBuilder.build();
  }

  private void exportAsPredicatePrecision(
      final Map<CFANode, Collection<CandidateInvariant>> pInvPerLoc, final Appendable sb)
      throws IOException {

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> locInvBuilder =
        ImmutableSetMultimap.builder();

    for (Entry<CFANode, Collection<CandidateInvariant>> entry : pInvPerLoc.entrySet()) {
      locInvBuilder.putAll(
          entry.getKey(),
          FluentIterable.from(entry.getValue())
              .transformAndConcat(
                  canInv ->
                      canInv.asBooleanFormulae(
                          solver.getFormulaManager(), varToType, c2Formula, machineModel))
              .transform(bf -> absMgr.makePredicate(bf)));
    }

    AbstractionPredicate falsePredicate = absMgr.makeFalsePredicate();
    ImmutableSetMultimap<CFANode, AbstractionPredicate> locInvariants = locInvBuilder.build();

    predExporter.writePredicateMap(
        ImmutableSetMultimap.of(),
        locInvariants,
        ImmutableSetMultimap.of(),
        ImmutableSet.of(falsePredicate),
        FluentIterable.from(locInvariants.values()).append(falsePredicate).toSet(),
        sb);
  }

  private ImmutableCollection<CandidateInvariant> generateInvariants(
      final ImmutableCollection<ValueAnalysisState> pValStates) {
    if (pValStates == null || pValStates.isEmpty()) {
      return ImmutableSet.<CandidateInvariant>of();
    }

    Set<MemoryLocation> vars = pValStates.iterator().next().getTrackedMemoryLocations();
    Map<MemoryLocation, List<ValueAndType>> varsWithVals =
        Maps.newHashMapWithExpectedSize(vars.size());
    for (MemoryLocation var : vars) {
      varsWithVals.put(var, new ArrayList<>(pValStates.size()));
    }

    Entry<MemoryLocation, List<ValueAndType>> varWithVals;
    MemoryLocation var;
    Set<MemoryLocation> trackedInState;
    for (final ValueAnalysisState valueState : pValStates) {
      trackedInState = valueState.getTrackedMemoryLocations();
      for (Iterator<Entry<MemoryLocation, List<ValueAndType>>> varValsIt =
              varsWithVals.entrySet().iterator();
          varValsIt.hasNext(); ) {
        varWithVals = varValsIt.next();
        var = varWithVals.getKey();
        // restrict to variables with numerical or Boolean values
        if (!trackedInState.contains(var)
            || valueState.getValueFor(var).isUnknown()
            || (!valueState.getValueFor(var).isNumericValue()
                && !(valueState.getValueFor(var) instanceof BooleanValue))) {
          // according to API specification removes var from Map
          varValsIt.remove();
        } else {
          varWithVals.getValue().add(valueState.getValueAndTypeFor(var));
        }
      }
    }

    ImmutableCollection.Builder<CandidateInvariant> invBuilder = new ImmutableList.Builder<>();

    if (exportUnary) {
      // single variable
      addSingleVariableInvariants(varsWithVals, invBuilder);
    }

    if (exportBinary && (exportArithmetic || exportBitops || exportLinear || exportRelational)) {
      // two variables
      addInvariantsOverTwoVariables(varsWithVals, invBuilder);
    }

    if (exportTernary && exportLinear) {
      // three variables
      // ax + by + cz + d =/<=/>= 0
      addLinearInvariantsOverThreeVariables(varsWithVals, invBuilder);
    }

    // redundant invariants
    /* if (removeRedundantInvariants) {
      removeRedundantInvariants(); // TODO does not fit with builder
    }*/

    return invBuilder.build();
  }

  private void addSingleVariableInvariants(
      final Map<MemoryLocation, List<ValueAndType>> pVarsWithVals,
      final ImmutableCollection.Builder<CandidateInvariant> pInvBuilder) {
    // =value, >=, <=, != 0, == 0, even, odd,
    // not supported: set of values,  (allgemein x == a (mod b)
    // not supported bitwise negation ~x
    // not expressible: zweierpotenz
    numNumericInvariants = 0;
    numBooleanInvariants = 0;
    SingleNumericVariableInvariant numInv;
    SingleBooleanVariableInvariant boolInv;
    Value val;
    for (Entry<MemoryLocation, List<ValueAndType>> varAndVals : pVarsWithVals.entrySet()) {
      val = varAndVals.getValue().get(0).getValue();
      if (val.isExplicitlyKnown() && val.isNumericValue()) {
        Preconditions.checkState(varToType.containsKey(varAndVals.getKey()));
        numInv =
            new SingleNumericVariableInvariant(
                varAndVals.getKey(), val.asNumericValue(), exportArithmetic);
        for (ValueAndType valPlusType : varAndVals.getValue()) {
          numInv.adaptToAdditionalValue(valPlusType.getValue());
        }
        pInvBuilder.add(numInv);
        numNumericInvariants += numInv.getNumInvariants();
      } else if (val instanceof BooleanValue) {
        boolInv = new SingleBooleanVariableInvariant(varAndVals.getKey(), (BooleanValue) val);
        for (ValueAndType valPlusType : varAndVals.getValue()) {
          boolInv.adaptToAdditionalValue(valPlusType.getValue());
        }
        pInvBuilder.add(boolInv);
        numBooleanInvariants += boolInv.getNumInvariants();
      }
    }
  }

  private void addInvariantsOverTwoVariables(
      final Map<MemoryLocation, List<ValueAndType>> pVarsWithVals,
      final ImmutableCollection.Builder<CandidateInvariant> pInvBuilder) {
    numCompRelationalInvariants = 0;
    numArithmeticRelationalInvariants = 0;
    numBitRelationalInvariants = 0;
    numLinearInvariants = 0;

    // two variables
    // relational
    // x > y, x<y, x>=y,x<=y, x==y, x!=y
    // linear
    // ax + -y + c = 0
    // -x + by + c =
    // arithmetic (non-linear)
    // x%y, x*y, x/y  <=/==/>=
    // bit operations
    // x&y, x|y, x^y, x<<y, x>>y
    // not expressible: x = y**2

    TwoVariableRelationInvariant relInv;
    TwoVariableArithmeticInvariant arInv;
    TwoVariableBitOpsInvariant bitInv;
    LinearInEqualityInvariant linInv1;
    LinearInEqualityInvariant linInv2;
    CSimpleType type1;
    CSimpleType type2;
    List<ValueAndType> val1;
    List<ValueAndType> val2;

    // restricted to variable pairs that are both of CSimpleType,
    // are either both integer types or floating types and have the same sign
    Set<MemoryLocation> exploredVars = Sets.newHashSetWithExpectedSize(pVarsWithVals.size());
    for (Entry<MemoryLocation, List<ValueAndType>> varWithVals1 : pVarsWithVals.entrySet()) {
      if (!(varToType.get(varWithVals1.getKey()) instanceof CSimpleType)) {
        exploredVars.add(varWithVals1.getKey());
        continue;
      }

      type1 = (CSimpleType) varToType.get(varWithVals1.getKey());

      for (Entry<MemoryLocation, List<ValueAndType>> varWithVals2 : pVarsWithVals.entrySet()) {
        if (varWithVals1.getKey().equals(varWithVals2.getKey())
            || exploredVars.contains(varWithVals2.getKey())
            || !(varToType.get(varWithVals2.getKey()) instanceof CSimpleType)) {
          continue;
        }

        type2 = (CSimpleType) varToType.get(varWithVals2.getKey());
        // only pair integer types with integer types and floating point types with floating
        // point types due to incompatibilities in formula encodings
        if (((type1.getType().isIntegerType()
                    && type2.getType().isIntegerType()
                    && type1.getType() != CBasicType.UNSPECIFIED
                    && type2.getType() != CBasicType.UNSPECIFIED)
                || (type1.getType().isFloatingPointType() && type2.getType().isFloatingPointType()))
            && (machineModel.isSigned(type1) == machineModel.isSigned(type2))) {
          Preconditions.checkState(!type1.hasComplexSpecifier() && !type1.hasImaginarySpecifier());
          Preconditions.checkState(!type2.hasComplexSpecifier() && !type2.hasImaginarySpecifier());

          val1 = varWithVals1.getValue();
          val2 = varWithVals2.getValue();
          if (val1.isEmpty()
              || val1.size() != val2.size()
              || !val1.get(0).getValue().isNumericValue()
              || !val2.get(0).getValue().isNumericValue()) {
            continue;
          }

          if (exportLinear && val1.size() >= 2) {

            Number[] coefficients =
                computeCoefficientsForLinearEquation(
                    new ValueAndType[][] {
                      {
                        val2.get(0), val2.get(1),
                      },
                    },
                    new ValueAndType[] {
                      val1.get(0), val1.get(1),
                    },
                    0,
                    type1.getType().isFloatingPointType());

            if (coefficients != null && !isIrrelevantLinearInvariant(coefficients)) {
              linInv1 =
                  new LinearInEqualityInvariant(
                      new MemoryLocation[] {
                        varWithVals1.getKey(), varWithVals2.getKey(),
                      },
                      coefficients,
                      type1.getType().isFloatingPointType());
            } else {
              linInv1 = null;
            }

            coefficients =
                computeCoefficientsForLinearEquation(
                    new ValueAndType[][] {
                      {
                        val1.get(0), val1.get(1),
                      },
                    },
                    new ValueAndType[] {
                      val2.get(0), val2.get(1),
                    },
                    1,
                    type1.getType().isFloatingPointType());

            if (coefficients != null && !isIrrelevantLinearInvariant(coefficients)) {
              linInv2 =
                  new LinearInEqualityInvariant(
                      new MemoryLocation[] {
                        varWithVals1.getKey(), varWithVals2.getKey(),
                      },
                      coefficients,
                      type1.getType().isFloatingPointType());
            } else {
              linInv2 = null;
            }

          } else {
            linInv1 = null;
            linInv2 = null;
          }

          if (exportArithmetic) {
            arInv =
                new TwoVariableArithmeticInvariant(
                    varWithVals1.getKey(),
                    val1.get(0).getValue().asNumericValue(),
                    varWithVals2.getKey(),
                    val2.get(0).getValue().asNumericValue());
          } else {
            arInv = null;
          }

          if (exportBitops
              && type1.getType().isIntegerType()
              && type2.getType().isIntegerType()
              && (!(val1.get(0).getValue().asNumericValue().getNumber() instanceof BigInteger)
                  || containslongValue(
                      (BigInteger) val1.get(0).getValue().asNumericValue().getNumber()))
              && (!(val2.get(0).getValue().asNumericValue().getNumber() instanceof BigInteger)
                  || containslongValue(
                      (BigInteger) val2.get(0).getValue().asNumericValue().getNumber()))) {
            bitInv =
                new TwoVariableBitOpsInvariant(
                    varWithVals1.getKey(),
                    val1.get(0).getValue().asNumericValue(),
                    varWithVals2.getKey(),
                    val2.get(0).getValue().asNumericValue(),
                    exportShiftops);
          } else {
            bitInv = null;
          }

          if (exportRelational
              && (!(val1.get(0).getValue().asNumericValue().getNumber() instanceof BigInteger)
                  || containslongValue(
                      (BigInteger) val1.get(0).getValue().asNumericValue().getNumber()))
              && (!(val2.get(0).getValue().asNumericValue().getNumber() instanceof BigInteger)
                  || containslongValue(
                      (BigInteger) val2.get(0).getValue().asNumericValue().getNumber()))) {
            relInv =
                new TwoVariableRelationInvariant(
                    varWithVals1.getKey(),
                    val1.get(0).getValue().asNumericValue(),
                    varWithVals2.getKey(),
                    val2.get(0).getValue().asNumericValue());

          } else {
            relInv = null;
          }

          for (int i = 1;
              (relInv != null
                      || linInv1 != null
                      || linInv2 != null
                      || arInv != null
                      || bitInv != null)
                  && i < val1.size();
              i++) {
            if (relInv != null
                && !relInv.adaptToAdditionalValues(
                    val1.get(i).getValue(), val2.get(i).getValue())) {
              relInv = null;
            }
            if (arInv != null
                && !arInv.adaptToAdditionalValues(val1.get(i).getValue(), val2.get(i).getValue())) {
              arInv = null;
            }
            if (bitInv != null
                && !bitInv.adaptToAdditionalValues(
                    val1.get(i).getValue(), val2.get(i).getValue())) {
              bitInv = null;
            }
            if (linInv1 != null
                && !linInv1.adaptToAdditionalValues(
                    new Value[] {
                      val1.get(i).getValue(), val2.get(i).getValue(),
                    })) {
              linInv1 = null;
            }
            if (linInv2 != null
                && !linInv2.adaptToAdditionalValues(
                    new Value[] {
                      val1.get(i).getValue(), val2.get(i).getValue(),
                    })) {
              linInv2 = null;
            }
          }

          if (relInv != null) {
            pInvBuilder.add(relInv);
            numCompRelationalInvariants++;
          }
          if (arInv != null) {
            pInvBuilder.add(arInv);
            numArithmeticRelationalInvariants += arInv.getNumInvariants();
          }
          if (bitInv != null) {
            pInvBuilder.add(bitInv);
            numBitRelationalInvariants += bitInv.getNumInvariants();
          }
          if (linInv1 != null) {
            pInvBuilder.add(linInv1);
            numLinearInvariants++;
          }
          if (linInv2 != null) {
            pInvBuilder.add(linInv2);
            numLinearInvariants++;
          }
        }
      }
      exploredVars.add(varWithVals1.getKey());
    }
  }

  private void addLinearInvariantsOverThreeVariables(
      final Map<MemoryLocation, List<ValueAndType>> pVarsWithVals,
      final ImmutableCollection.Builder<CandidateInvariant> pInvBuilder) {
    Preconditions.checkState(exportLinear);
    Preconditions.checkState(exportTernary);
    numLinearInvariants = Math.max(0, numLinearInvariants);

    CSimpleType type1;
    CSimpleType type2;
    CSimpleType type3;
    LinearInEqualityInvariant linInv1;
    LinearInEqualityInvariant linInv2;
    LinearInEqualityInvariant linInv3;
    List<ValueAndType> val1;
    List<ValueAndType> val2;
    List<ValueAndType> val3;

    Set<MemoryLocation> exploredVarsOuter = Sets.newHashSetWithExpectedSize(pVarsWithVals.size());
    Set<MemoryLocation> exploredVarsInner = Sets.newHashSetWithExpectedSize(pVarsWithVals.size());
    for (Entry<MemoryLocation, List<ValueAndType>> varWithVals1 : pVarsWithVals.entrySet()) {
      if (!(varToType.get(varWithVals1.getKey()) instanceof CSimpleType)
          || varWithVals1.getValue().size() < 3) {
        exploredVarsOuter.add(varWithVals1.getKey());
        continue;
      }

      type1 = (CSimpleType) varToType.get(varWithVals1.getKey());

      for (Entry<MemoryLocation, List<ValueAndType>> varWithVals2 : pVarsWithVals.entrySet()) {
        exploredVarsInner.clear();
        if (varWithVals1.getKey().equals(varWithVals2.getKey())
            || exploredVarsOuter.contains(varWithVals2.getKey())
            || !(varToType.get(varWithVals2.getKey()) instanceof CSimpleType)
            || varWithVals2.getValue().size() < 3
            || varWithVals1.getValue().size() != varWithVals2.getValue().size()) {
          exploredVarsInner.add(varWithVals2.getKey());
          continue;
        }

        type2 = (CSimpleType) varToType.get(varWithVals2.getKey());
        // only pair integer types with integer types and floating point types with floating
        // point types due to incompatibilities in formula encodings
        if (((type1.getType().isIntegerType()
                    && type2.getType().isIntegerType()
                    && type1.getType() != CBasicType.UNSPECIFIED
                    && type2.getType() != CBasicType.UNSPECIFIED)
                || (type1.getType().isFloatingPointType() && type2.getType().isFloatingPointType()))
            && (machineModel.isSigned(type1) == machineModel.isSigned(type2))) {
          Preconditions.checkState(!type1.hasComplexSpecifier() && !type1.hasImaginarySpecifier());
          Preconditions.checkState(!type2.hasComplexSpecifier() && !type2.hasImaginarySpecifier());

          for (Entry<MemoryLocation, List<ValueAndType>> varWithVals3 : pVarsWithVals.entrySet()) {
            if (varWithVals1.getKey().equals(varWithVals3.getKey())
                || varWithVals2.getKey().equals(varWithVals3.getKey())
                || exploredVarsOuter.contains(varWithVals3.getKey())
                || exploredVarsInner.contains(varWithVals3.getKey())
                || !(varToType.get(varWithVals3.getKey()) instanceof CSimpleType)
                || varWithVals3.getValue().size() < 3
                || varWithVals1.getValue().size() != varWithVals3.getValue().size()) {
              continue;
            }

            type3 = (CSimpleType) varToType.get(varWithVals3.getKey());
            // only pair integer types with integer types and floating point types with floating
            // point types due to incompatibilities in formula encodings
            if (((type1.getType().isIntegerType()
                        && type3.getType().isIntegerType()
                        && type1.getType() != CBasicType.UNSPECIFIED
                        && type3.getType() != CBasicType.UNSPECIFIED)
                    || (type1.getType().isFloatingPointType()
                        && type3.getType().isFloatingPointType()))
                && (machineModel.isSigned(type1) == machineModel.isSigned(type3))) {
              Preconditions.checkState(
                  !type3.hasComplexSpecifier() && !type3.hasImaginarySpecifier());

              val1 = varWithVals1.getValue();
              val2 = varWithVals2.getValue();
              val3 = varWithVals3.getValue();

              Number[] coefficients =
                  computeCoefficientsForLinearEquation(
                      new ValueAndType[][] {
                        {
                          val2.get(0), val2.get(1), val2.get(2),
                        },
                        {
                          val3.get(0), val3.get(1), val3.get(2),
                        },
                      },
                      new ValueAndType[] {
                        val1.get(0), val1.get(1), val1.get(2),
                      },
                      0,
                      type1.getType().isFloatingPointType());

              if (coefficients != null) {
                linInv1 =
                    new LinearInEqualityInvariant(
                        new MemoryLocation[] {
                          varWithVals1.getKey(), varWithVals2.getKey(), varWithVals3.getKey(),
                        },
                        coefficients,
                        type1.getType().isFloatingPointType());
              } else {
                linInv1 = null;
              }

              coefficients =
                  computeCoefficientsForLinearEquation(
                      new ValueAndType[][] {
                        {
                          val1.get(0), val1.get(1), val1.get(2),
                        },
                        {
                          val3.get(0), val3.get(1), val3.get(2),
                        },
                      },
                      new ValueAndType[] {
                        val2.get(0), val2.get(1), val2.get(2),
                      },
                      1,
                      type1.getType().isFloatingPointType());

              if (coefficients != null) {
                linInv2 =
                    new LinearInEqualityInvariant(
                        new MemoryLocation[] {
                          varWithVals1.getKey(), varWithVals2.getKey(), varWithVals3.getKey(),
                        },
                        coefficients,
                        type1.getType().isFloatingPointType());
              } else {
                linInv2 = null;
              }

              coefficients =
                  computeCoefficientsForLinearEquation(
                      new ValueAndType[][] {
                        {
                          val1.get(0), val1.get(1), val1.get(2),
                        },
                        {
                          val2.get(0), val2.get(1), val2.get(2),
                        },
                      },
                      new ValueAndType[] {
                        val3.get(0), val3.get(1), val3.get(2),
                      },
                      2,
                      type1.getType().isFloatingPointType());

              if (coefficients != null) {
                linInv3 =
                    new LinearInEqualityInvariant(
                        new MemoryLocation[] {
                          varWithVals1.getKey(), varWithVals2.getKey(), varWithVals3.getKey(),
                        },
                        coefficients,
                        type1.getType().isFloatingPointType());
              } else {
                linInv3 = null;
              }

              for (int i = 1;
                  i < val1.size() && (linInv1 != null || linInv2 != null || linInv3 != null);
                  i++) {
                if (linInv1 != null
                    && !linInv1.adaptToAdditionalValues(
                        new Value[] {
                          val1.get(i).getValue(), val2.get(i).getValue(), val3.get(i).getValue(),
                        })) {
                  linInv1 = null;
                }
                if (linInv2 != null
                    && !linInv2.adaptToAdditionalValues(
                        new Value[] {
                          val1.get(i).getValue(), val2.get(i).getValue(), val3.get(i).getValue(),
                        })) {
                  linInv2 = null;
                }

                if (linInv3 != null
                    && !linInv3.adaptToAdditionalValues(
                        new Value[] {
                          val1.get(i).getValue(), val2.get(i).getValue(), val3.get(i).getValue(),
                        })) {
                  linInv3 = null;
                }
              }
              if (linInv1 != null) {
                pInvBuilder.add(linInv1);
                numLinearInvariants++;
              }

              if (linInv2 != null) {
                pInvBuilder.add(linInv2);
                numLinearInvariants++;
              }

              if (linInv3 != null) {
                pInvBuilder.add(linInv3);
                numLinearInvariants++;
              }
            }
          }
        }
        exploredVarsInner.add(varWithVals2.getKey());
      }
      exploredVarsOuter.add(varWithVals1.getKey());
    }
  }

  private Number[] computeCoefficientsForLinearEquation(
      final ValueAndType[][] varValsWithCoeff,
      final ValueAndType[] eqVarVals,
      final int posEqVar,
      final boolean isFloatType) {
    Preconditions.checkNotNull(eqVarVals);
    Preconditions.checkNotNull(varValsWithCoeff);
    Preconditions.checkArgument(posEqVar >= 0 && posEqVar <= varValsWithCoeff.length);
    Preconditions.checkArgument(
        FluentIterable.from(varValsWithCoeff)
            .allMatch(varVals -> varVals != null && varVals.length == eqVarVals.length));

    Value val;
    boolean linConstraint = true;
    Number[][] eqMatrix = new Number[varValsWithCoeff.length + 1][varValsWithCoeff.length + 2];

    for (int i = 0; i < eqMatrix.length && linConstraint; i++) {
      val = eqVarVals[i].getValue();
      if (val.isNumericValue()) {
        eqMatrix[i][eqMatrix.length] = val.asNumericValue().getNumber();
      } else {
        linConstraint = false;
        break;
      }

      if (isFloatType) {
        eqMatrix[i][eqMatrix.length - 1] = Double.valueOf(1);
      } else {
        eqMatrix[i][eqMatrix.length - 1] = Integer.valueOf(1);
      }

      for (int j = 0; j < varValsWithCoeff.length; j++) {
        val = varValsWithCoeff[j][i].getValue();
        if (val.isNumericValue()) {
          eqMatrix[i][j] = val.asNumericValue().getNumber();
        } else {
          linConstraint = false;
          break;
        }
      }
    }

    if (linConstraint) {
      Number[] coefficients = LinearInEqualityInvariant.solveLinearEquation(eqMatrix, isFloatType);
      if (coefficients != null) {
        Number[] coefficientsResult = new Number[coefficients.length + 1];
        for (int i = 0; i < posEqVar; i++) {
          coefficientsResult[i] = coefficients[i];
        }

        if (isFloatType) {
          coefficientsResult[posEqVar] = Double.valueOf(-1);
        } else {
          coefficientsResult[posEqVar] = Integer.valueOf(-1);
        }

        for (int i = posEqVar + 1; i < coefficientsResult.length; i++) {
          coefficientsResult[i] = coefficients[i - 1];
        }

        return coefficientsResult;
      }
    }

    return null;
  }

  private boolean isIrrelevantLinearInvariant(final Number[] coefficients) {
    if (coefficients != null && coefficients.length <= 3) {
      return coefficients.length < 2
          || ((coefficients[0].doubleValue() == 1
                  && coefficients[1].doubleValue() == -1
                  && coefficients[2].doubleValue() == 0)
              || (coefficients[0].doubleValue() == -1 && coefficients[1].doubleValue() == 1));
    }
    return false;
  }

  private boolean containslongValue(final BigInteger pNum) {
    try {
      pNum.longValueExact();
      return true;
    } catch (ArithmeticException e) {
      return false;
    }
  }

  /*
  private void removeRedundantInvariants() {

  }*/

  private abstract static class CandidateInvariant {

    protected enum EqualCompareType {
      EQ,
      GEQ,
      LEQ,
      NONE
    }

    protected EqualCompareType updateCompareType(
        final int compRes, final EqualCompareType currentType) {

      return switch (currentType) {
        case EQ ->
            compRes < 0 ? EqualCompareType.LEQ : (compRes > 0 ? EqualCompareType.GEQ : currentType);
        case GEQ -> compRes < 0 ? EqualCompareType.NONE : currentType;
        case LEQ -> compRes > 0 ? EqualCompareType.NONE : currentType;
        case NONE -> currentType;
      };
    }

    protected @Nullable NumericValue extractNumValue(Value pValue) {
      Preconditions.checkNotNull(pValue);
      if (pValue.isExplicitlyKnown() && pValue.isNumericValue()) {
        return pValue.asNumericValue();
      }
      return null;
    }

    protected abstract Collection<BooleanFormula> asBooleanFormulae(
        final FormulaManagerView pFormulaManagerView,
        final ImmutableMap<MemoryLocation, Type> pVarToType,
        final CtoFormulaConverter pC2Formula,
        final MachineModel pMachineModel);

    protected static boolean isIntegralType(final Number pNum) {
      return pNum instanceof java.lang.Byte
          || pNum instanceof Short
          || pNum instanceof Integer
          || pNum instanceof Long
          || pNum instanceof AtomicInteger
          || pNum instanceof AtomicLong;
    }

    protected static boolean isFloatingNumber(final Number pNum) {
      return pNum instanceof Float || pNum instanceof Double || pNum instanceof AtomicDouble;
    }

    protected int compareVals(final NumericValue pVal1, final NumericValue pVal2) {
      Number num1 = pVal1.getNumber();
      Number num2 = pVal2.getNumber();

      boolean isIntegral1 = isIntegralType(num1);
      boolean isIntegral2 = isIntegralType(num2);

      if (isIntegral1 && isIntegral2) {
        long valL1 = pVal1.longValue();
        long valL2 = pVal2.longValue();
        return Long.compare(valL1, valL2);
      }

      boolean isFloat1 = isFloatingNumber(num1);
      boolean isFloat2 = isFloatingNumber(num2);

      if ((isFloat1 && (isFloat2 || isIntegral2)) || (isFloat2 && isIntegral1)) {
        return Double.compare(pVal1.doubleValue(), pVal2.doubleValue());
      }

      boolean isBigInt1 = num1 instanceof BigInteger;
      boolean isBigInt2 = num2 instanceof BigInteger;

      if ((isBigInt1 && (isBigInt2 || isIntegral2)) || (isBigInt2 && isIntegral1)) {
        return pVal1.bigIntegerValue().compareTo(pVal2.bigIntegerValue());
      }

      if ((num1 instanceof BigDecimal || isBigInt1 || isIntegral1 || isFloat1)
          && (num2 instanceof BigDecimal || isBigInt2 || isIntegral2 || isFloat2)) {
        return pVal1.bigDecimalValue().compareTo(pVal2.bigDecimalValue());
      }

      if (num1 instanceof Rational) {
        if (num2 instanceof Rational) {
          return ((Rational) num1).compareTo((Rational) num2);
        }
        if (isIntegral2) {
          return ((Rational) num1).compareTo(Rational.of(num2.longValue()));
        }
        if (isBigInt2) {
          return ((Rational) num1).compareTo(Rational.ofBigInteger((BigInteger) num2));
        }
        if (num2 instanceof BigDecimal) {
          return ((Rational) num1).compareTo(Rational.ofBigDecimal((BigDecimal) num2));
        }
        if (isFloat2) {
          return ((Rational) num1).compareTo(Rational.ofBigDecimal(pVal2.bigDecimalValue()));
        }
      } else if (num2 instanceof Rational) {
        if (isIntegral1) {
          return Rational.of(num1.longValue()).compareTo((Rational) num2);
        }
        if (isBigInt1) {
          return Rational.ofBigInteger((BigInteger) num1).compareTo(((Rational) num2));
        }
        if (num1 instanceof BigDecimal) {
          return Rational.ofBigDecimal((BigDecimal) num1).compareTo((Rational) num2);
        }
        if (isFloat1) {
          return Rational.ofBigDecimal(pVal1.bigDecimalValue()).compareTo((Rational) num2);
        }
      }

      throw new AssertionError(
          "Comparision between " + pVal1 + " and " + pVal2 + " not supported.");
    }

    protected Formula encodeNumVal(
        final FormulaManagerView pFmgrV,
        final FormulaType<?> pFormulaType,
        final Formula pVarF,
        final Number numVal) {
      if (isIntegralType(numVal)) {
        return pFmgrV.makeNumber(pFormulaType, numVal.longValue());
      }

      if (numVal instanceof BigInteger) {
        return pFmgrV.makeNumber(pFormulaType, (BigInteger) numVal);
      }

      if (numVal instanceof Rational) {
        return pFmgrV.makeNumber(pVarF, (Rational) numVal);
      }

      if (pFormulaType instanceof FormulaType.FloatingPointType) {
        try {

          if (isFloatingNumber(numVal)) {
            return pFmgrV
                .getFloatingPointFormulaManager()
                .makeNumber(numVal.doubleValue(), (FormulaType.FloatingPointType) pFormulaType);
          }

          if (numVal instanceof BigDecimal) {
            return pFmgrV
                .getFloatingPointFormulaManager()
                .makeNumber((BigDecimal) numVal, (FormulaType.FloatingPointType) pFormulaType);
          }
        } catch (UnsupportedOperationException e) {
          throw new AssertionError("Unsupported floating point Number instance " + numVal);
        }
      }

      throw new AssertionError("Unsupported Number instance " + numVal);
    }

    protected BooleanFormula makeComparison(
        final Formula pTermF,
        final Pair<Number, EqualCompareType> pOp,
        final FormulaManagerView pFmgrV,
        final FormulaType<?> pFormulaType,
        final boolean pSigned) {
      return switch (pOp.getSecond()) {
        case EQ ->
            pFmgrV.makeEqual(pTermF, encodeNumVal(pFmgrV, pFormulaType, pTermF, pOp.getFirst()));
        case GEQ ->
            pFmgrV.makeGreaterOrEqual(
                pTermF, encodeNumVal(pFmgrV, pFormulaType, pTermF, pOp.getFirst()), pSigned);
        case LEQ ->
            pFmgrV.makeLessOrEqual(
                pTermF, encodeNumVal(pFmgrV, pFormulaType, pTermF, pOp.getFirst()), pSigned);
        default -> throw new AssertionError("Unsupported comparison");
      };
    }

    protected CSimpleType getCompareType(final CSimpleType pType1, final CSimpleType pType2) {
      CBasicType bType1 = pType1.getType();
      CBasicType bType2 = pType2.getType();

      if (bType1.isFloatingPointType()) {
        Preconditions.checkArgument(bType2.isFloatingPointType());
        if (bType1 == CBasicType.FLOAT128) {
          return pType1;
        } else if (bType2 == CBasicType.FLOAT128) {
          return pType2;
        } else if (bType1 == CBasicType.DOUBLE) {
          if (pType1.hasLongSpecifier()) {
            return pType1;
          } else if (bType2 == CBasicType.DOUBLE && pType2.hasLongSpecifier()) {
            return pType2;
          } else {
            return pType1;
          }
        } else { // pType2 is double or pType1 and pType2 are floats
          return pType2;
        }
      } else {
        Preconditions.checkArgument(bType1.isIntegerType());
        Preconditions.checkArgument(bType2.isIntegerType());
        Preconditions.checkArgument(bType1 != CBasicType.UNSPECIFIED);
        Preconditions.checkArgument(bType2 != CBasicType.UNSPECIFIED);

        if (bType1 == CBasicType.INT128) {
          return pType1;
        } else if (bType2 == CBasicType.INT128) {
          return pType2;
        } else if (bType1 == CBasicType.INT) {
          if (pType1.hasLongLongSpecifier()) {
            return pType1;
          }
          if (bType2 == CBasicType.INT) {
            if (pType2.hasLongLongSpecifier()) {
              return pType2;
            } else if (pType1.hasLongSpecifier()) {
              return pType1;
            } else if (pType2.hasLongSpecifier()) {
              return pType2;
            } else if (!pType1.hasShortSpecifier()) {
              return pType1;
            } else if (!pType2.hasShortSpecifier()) {
              return pType2;
            }
          }
          return pType1;

        } else if (bType2 == CBasicType.INT) {
          return pType2;
        } else if (bType1 == CBasicType.CHAR) {
          return pType1;
        } else if (bType2 == CBasicType.CHAR) {
          return pType2;
        } else { // both have bool type
          return pType1;
        }
      }
    }

    protected CSimpleType getCTypeFromValue(
        final boolean isSigned, final Number pNumber, final MachineModel pMachineModel) {
      if (isFloatingNumber(pNumber)
          || pNumber instanceof BigDecimal
          || pNumber instanceof Rational) {
        return CNumericTypes.DOUBLE; // TODO should we distinguish different types for more
        // precision?
      } else {
        BigInteger compareNum;
        if (pNumber instanceof BigInteger) {
          compareNum = (BigInteger) pNumber;
        } else {
          Preconditions.checkArgument(isIntegralType(pNumber));
          compareNum = BigInteger.valueOf(pNumber.longValue());
        }
        if (isSigned || compareNum.compareTo(BigInteger.ZERO) < 0) {
          if (compareNum.compareTo(pMachineModel.getMinimalIntegerValue(CNumericTypes.SIGNED_CHAR))
                  >= 0
              && compareNum.compareTo(
                      pMachineModel.getMaximalIntegerValue(CNumericTypes.SIGNED_CHAR))
                  <= 0) {
            return CNumericTypes.SIGNED_CHAR;
          } else if (compareNum.compareTo(
                      pMachineModel.getMinimalIntegerValue(CNumericTypes.SHORT_INT))
                  >= 0
              && compareNum.compareTo(pMachineModel.getMaximalIntegerValue(CNumericTypes.SHORT_INT))
                  <= 0) {
            return CNumericTypes.SHORT_INT;
          } else if (compareNum.compareTo(
                      pMachineModel.getMinimalIntegerValue(CNumericTypes.SIGNED_INT))
                  >= 0
              && compareNum.compareTo(
                      pMachineModel.getMaximalIntegerValue(CNumericTypes.SIGNED_INT))
                  <= 0) {
            return CNumericTypes.SIGNED_INT;
          } else if (compareNum.compareTo(
                      pMachineModel.getMinimalIntegerValue(CNumericTypes.SIGNED_LONG_INT))
                  >= 0
              && compareNum.compareTo(
                      pMachineModel.getMaximalIntegerValue(CNumericTypes.SIGNED_LONG_INT))
                  <= 0) {
            return CNumericTypes.SIGNED_LONG_INT;
          } else if (compareNum.compareTo(
                      pMachineModel.getMinimalIntegerValue(CNumericTypes.SIGNED_LONG_LONG_INT))
                  >= 0
              && compareNum.compareTo(
                      pMachineModel.getMaximalIntegerValue(CNumericTypes.SIGNED_LONG_LONG_INT))
                  <= 0) {
            return CNumericTypes.SIGNED_LONG_LONG_INT;
          } else {
            throw new AssertionError("Unsupported literal " + pNumber + "(value too large");
          }
        } else {
          if (compareNum.compareTo(
                      pMachineModel.getMinimalIntegerValue(CNumericTypes.UNSIGNED_CHAR))
                  >= 0
              && compareNum.compareTo(
                      pMachineModel.getMaximalIntegerValue(CNumericTypes.UNSIGNED_CHAR))
                  <= 0) {
            return CNumericTypes.UNSIGNED_CHAR;
          } else if (compareNum.compareTo(
                      pMachineModel.getMinimalIntegerValue(CNumericTypes.UNSIGNED_SHORT_INT))
                  >= 0
              && compareNum.compareTo(
                      pMachineModel.getMaximalIntegerValue(CNumericTypes.UNSIGNED_SHORT_INT))
                  <= 0) {
            return CNumericTypes.UNSIGNED_SHORT_INT;
          } else if (compareNum.compareTo(
                      pMachineModel.getMinimalIntegerValue(CNumericTypes.UNSIGNED_INT))
                  >= 0
              && compareNum.compareTo(
                      pMachineModel.getMaximalIntegerValue(CNumericTypes.UNSIGNED_INT))
                  <= 0) {
            return CNumericTypes.UNSIGNED_INT;
          } else if (compareNum.compareTo(
                      pMachineModel.getMinimalIntegerValue(CNumericTypes.UNSIGNED_LONG_INT))
                  >= 0
              && compareNum.compareTo(
                      pMachineModel.getMaximalIntegerValue(CNumericTypes.UNSIGNED_LONG_INT))
                  <= 0) {
            return CNumericTypes.UNSIGNED_LONG_INT;
          } else if (compareNum.compareTo(
                      pMachineModel.getMinimalIntegerValue(CNumericTypes.UNSIGNED_LONG_LONG_INT))
                  >= 0
              && compareNum.compareTo(
                      pMachineModel.getMaximalIntegerValue(CNumericTypes.UNSIGNED_LONG_LONG_INT))
                  <= 0) {
            return CNumericTypes.UNSIGNED_LONG_LONG_INT;
          } else {
            throw new AssertionError("Unsupported literal " + pNumber + "(value too large");
          }
        }
      }
    }

    protected Formula simpleCast(
        final Formula pFormula,
        final boolean pIsSignedOriginal,
        final CType originalType,
        final CType goalType,
        final FormulaManagerView pFmgrV,
        final CtoFormulaConverter pC2Formula) {
      final FormulaType<?> fromType = pC2Formula.getFormulaTypeFromCType(originalType);
      final FormulaType<?> toType = pC2Formula.getFormulaTypeFromCType(goalType);

      Preconditions.checkArgument(
          (fromType.isBitvectorType() && toType.isBitvectorType()) || toType.isFloatingPointType());

      final Formula ret;
      if (fromType.equals(toType)) {
        ret = pFormula;

      } else if (fromType.isBitvectorType() && toType.isBitvectorType()) {
        int toSize = ((FormulaType.BitvectorType) toType).getSize();
        int fromSize = ((FormulaType.BitvectorType) fromType).getSize();

        if (fromSize > toSize) {
          ret = pFmgrV.makeExtract(pFormula, toSize - 1, 0);
        } else if (fromSize < toSize) {
          ret = pFmgrV.makeExtend(pFormula, (toSize - fromSize), pIsSignedOriginal);
        } else {
          ret = pFormula;
        }

      } else if (toType.isFloatingPointType()) {
        ret =
            pFmgrV
                .getFloatingPointFormulaManager()
                .castFrom(pFormula, pIsSignedOriginal, (FormulaType.FloatingPointType) toType);

      } else {
        ret = null;
        Preconditions.checkState(false);
      }

      assert pFmgrV.getFormulaType(ret).equals(toType)
          : "types do not match: " + pFmgrV.getFormulaType(ret) + " vs " + toType;
      return ret;
    }
  }

  private static class SingleBooleanVariableInvariant extends CandidateInvariant {

    private final MemoryLocation var;
    private boolean alwaysTrue;
    private boolean alwaysFalse;

    private SingleBooleanVariableInvariant(
        final MemoryLocation pVar, final BooleanValue pInitialValue) {
      Preconditions.checkNotNull(pVar);
      Preconditions.checkNotNull(pInitialValue);
      var = pVar;
      if (pInitialValue.isTrue()) {
        alwaysTrue = true;
        alwaysFalse = false;
      } else {
        alwaysTrue = false;
        alwaysFalse = true;
      }
    }

    private int getNumInvariants() {
      return alwaysTrue || alwaysFalse ? 1 : 0;
    }

    private void adaptToAdditionalValue(final Value pValue) {
      Preconditions.checkNotNull(pValue);
      Preconditions.checkArgument(pValue instanceof BooleanValue);
      BooleanValue newVal = (BooleanValue) pValue;
      Preconditions.checkNotNull(newVal);
      alwaysTrue = alwaysTrue && newVal.isTrue();
      alwaysFalse = alwaysFalse && !newVal.isTrue();
    }

    @Override
    protected Collection<BooleanFormula> asBooleanFormulae(
        final FormulaManagerView pFmgrV,
        final ImmutableMap<MemoryLocation, Type> pVarToType,
        final CtoFormulaConverter pC2Formula,
        final MachineModel pMachineModel) {
      Preconditions.checkArgument(pVarToType.containsKey(var));
      Preconditions.checkArgument(pVarToType.get(var) instanceof CType);
      Preconditions.checkState(!(alwaysFalse && alwaysTrue));

      if (alwaysTrue || alwaysFalse) {
        FormulaType<?> formulaType =
            pC2Formula.getFormulaTypeFromCType((CType) pVarToType.get(var));
        Formula varF = pFmgrV.makeVariable(formulaType, var.getExtendedQualifiedName());

        if (varF instanceof BooleanFormula) {
          if (alwaysTrue) {
            return ImmutableList.of((BooleanFormula) varF);
          } else if (alwaysFalse) {
            return ImmutableList.of(pFmgrV.makeNot((BooleanFormula) varF));
          }
        } else {
          if (alwaysTrue) {
            return ImmutableList.of(
                pFmgrV.makeNot(pFmgrV.makeEqual(varF, pFmgrV.makeNumber(formulaType, 0))));
          } else if (alwaysFalse) {
            return ImmutableList.of(pFmgrV.makeEqual(varF, pFmgrV.makeNumber(formulaType, 0)));
          }
        }
      }
      return ImmutableList.of();
    }
  }

  private static class SingleNumericVariableInvariant extends CandidateInvariant {

    private final MemoryLocation var;
    private NumericValue maxVal;
    private NumericValue minVal;
    private boolean isNeverZero;
    private boolean alwaysEven;
    private boolean alwaysOdd;
    private final boolean exportEvenOdd;

    private SingleNumericVariableInvariant(
        final MemoryLocation pVar, final NumericValue pInitialValue, boolean pExportModTwo) {
      var = Preconditions.checkNotNull(pVar);
      maxVal = Preconditions.checkNotNull(pInitialValue);
      minVal = maxVal;
      isNeverZero = !isZero(maxVal);

      exportEvenOdd = pExportModTwo;
      alwaysEven = exportEvenOdd && isEven(maxVal);
      alwaysOdd = exportEvenOdd && isOdd(maxVal);
    }

    private boolean isZero(NumericValue pVal) {
      Number num = pVal.getNumber();
      if (isIntegralType(num)) {
        return num.longValue() == 0;
      } else if (num instanceof Float) {
        return num.floatValue() == 0;
      } else if (num instanceof Double) {
        return num.doubleValue() == 0;
      } else if (num instanceof BigInteger) {
        return pVal.bigIntegerValue().equals(BigInteger.ZERO);
      } else if (num instanceof BigDecimal) {
        return pVal.bigDecimalValue().compareTo(BigDecimal.ZERO) == 0;
      } else {
        return false;
      }
    }

    private boolean isEven(NumericValue pVal) {
      Number num = pVal.getNumber();
      if (isIntegralType(num)) {
        return num.longValue() % 2 == 0;
      }

      return false;
    }

    private boolean isOdd(NumericValue pVal) {
      Number num = pVal.getNumber();
      if (isIntegralType(num)) {
        return num.longValue() % 2 != 0;
      }

      return false;
    }

    private void adaptToAdditionalValue(final Value pValue) {
      Preconditions.checkNotNull(pValue);

      NumericValue newVal = extractNumValue(pValue);
      Preconditions.checkNotNull(newVal);
      if (compareVals(maxVal, newVal) < 0) {
        maxVal = newVal;
      }
      if (compareVals(minVal, newVal) > 0) {
        minVal = newVal;
      }
      isNeverZero = isNeverZero && !isZero(newVal);
      alwaysEven = alwaysEven && isEven(newVal);
      alwaysOdd = alwaysOdd && isOdd(newVal);
    }

    private boolean isZeroInMinMax() {
      NumericValue zeroVal = new NumericValue(0);
      return compareVals(minVal, zeroVal) <= 0 && compareVals(maxVal, zeroVal) >= 0;
    }

    private int getNumInvariants() {
      int num = 0;
      if (minVal.equals(maxVal)) {
        num++;
      } else {
        num += 2;
      }
      if (isNeverZero && isZeroInMinMax()) {
        num++;
      }
      if (alwaysEven || alwaysOdd) {
        num++;
      }
      return num;
    }

    @Override
    protected Collection<BooleanFormula> asBooleanFormulae(
        final FormulaManagerView pFmgrV,
        final ImmutableMap<MemoryLocation, Type> pVarToType,
        final CtoFormulaConverter pC2Formula,
        final MachineModel pMachineModel) {
      Preconditions.checkArgument(pVarToType.containsKey(var));
      Preconditions.checkArgument(pVarToType.get(var) instanceof CType);
      Preconditions.checkState(!(alwaysEven && alwaysOdd));
      Collection<BooleanFormula> result = new ArrayList<>(4);

      FormulaType<?> formulaType = pC2Formula.getFormulaTypeFromCType((CType) pVarToType.get(var));
      Formula varF = pFmgrV.makeVariable(formulaType, var.getExtendedQualifiedName());
      // assume type is signed (by default) if it is not a simple type
      boolean signed =
          pVarToType.get(var) instanceof CSimpleType
              ? pMachineModel.isSigned((CSimpleType) pVarToType.get(var))
              : true;

      if (minVal.equals(maxVal)) {
        result.add(
            pFmgrV.makeEqual(varF, encodeNumVal(pFmgrV, formulaType, varF, minVal.getNumber())));
      } else {
        result.add(
            pFmgrV.makeGreaterOrEqual(
                varF, encodeNumVal(pFmgrV, formulaType, varF, minVal.getNumber()), signed));
        result.add(
            pFmgrV.makeLessOrEqual(
                varF, encodeNumVal(pFmgrV, formulaType, varF, maxVal.getNumber()), signed));
      }

      if (isNeverZero && isZeroInMinMax()) {
        result.add(pFmgrV.makeNot(pFmgrV.makeEqual(varF, pFmgrV.makeNumber(formulaType, 0))));
      }

      if (alwaysEven) {
        result.add(
            pFmgrV.makeEqual(
                pFmgrV.makeRemainder(varF, pFmgrV.makeNumber(formulaType, 2), signed),
                pFmgrV.makeNumber(formulaType, 0)));
      } else if (alwaysOdd) {
        result.add(
            pFmgrV.makeNot(
                pFmgrV.makeEqual(
                    pFmgrV.makeRemainder(varF, pFmgrV.makeNumber(formulaType, 2), signed),
                    pFmgrV.makeNumber(formulaType, 0))));
      }

      return result;
    }
  }

  private static class TwoVariableRelationInvariant extends CandidateInvariant {
    private enum ComparisonType {
      EQ,
      GT,
      GEQ,
      LT,
      LEQ,
      UNEQ,
      NONE
    }

    private final MemoryLocation var;
    private final MemoryLocation var2;
    private ComparisonType comp;

    private TwoVariableRelationInvariant(
        final MemoryLocation pVar,
        final NumericValue pValue,
        final MemoryLocation pVar2,
        final NumericValue pValue2) {
      var = Preconditions.checkNotNull(pVar);
      var2 = Preconditions.checkNotNull(pVar2);
      Preconditions.checkNotNull(pValue);
      Preconditions.checkNotNull(pValue2);

      initComparisonType(pValue, pValue2);
    }

    private void initComparisonType(
        final NumericValue pInitValVar, final NumericValue pInitValVar2) {
      int compRes = compareVals(pInitValVar, pInitValVar2);
      if (compRes == 0) {
        comp = ComparisonType.EQ;
      } else if (compRes < 0) {
        comp = ComparisonType.LT;
      } else {
        comp = ComparisonType.GT;
      }
    }

    private boolean adaptToAdditionalValues(final Value pValL, final Value pValR) {
      Preconditions.checkNotNull(pValL);
      Preconditions.checkNotNull(pValR);

      NumericValue valL = extractNumValue(pValL);
      Preconditions.checkNotNull(valL);
      NumericValue valR = extractNumValue(pValR);
      Preconditions.checkNotNull(valR);

      int compRes = compareVals(valL, valR);

      comp =
          switch (comp) {
            case EQ -> compRes > 0 ? ComparisonType.GEQ : (compRes < 0 ? ComparisonType.LEQ : comp);
            case GT ->
                compRes == 0 ? ComparisonType.GEQ : (compRes < 0 ? ComparisonType.UNEQ : comp);
            case GEQ -> compRes < 0 ? ComparisonType.NONE : comp;
            case LT ->
                compRes > 0 ? ComparisonType.UNEQ : (compRes == 0 ? ComparisonType.LEQ : comp);
            case LEQ -> compRes > 0 ? ComparisonType.NONE : comp;
            case UNEQ -> compRes == 0 ? ComparisonType.NONE : comp;
            case NONE -> comp;
          };
      return comp != ComparisonType.NONE;
    }

    @Override
    protected Collection<BooleanFormula> asBooleanFormulae(
        final FormulaManagerView pFmgrV,
        final ImmutableMap<MemoryLocation, Type> pVarToType,
        final CtoFormulaConverter pC2Formula,
        final MachineModel pMachineModel) {

      Preconditions.checkArgument(pVarToType.containsKey(var));
      Preconditions.checkArgument(pVarToType.get(var) instanceof CSimpleType);
      Preconditions.checkArgument(pVarToType.containsKey(var2));
      Preconditions.checkArgument(pVarToType.get(var2) instanceof CSimpleType);
      Preconditions.checkArgument(
          pMachineModel.isSigned((CSimpleType) pVarToType.get(var))
              == pMachineModel.isSigned((CSimpleType) pVarToType.get(var2)));
      Preconditions.checkState(comp != ComparisonType.NONE);

      CType compareType =
          getCompareType((CSimpleType) pVarToType.get(var), (CSimpleType) pVarToType.get(var2));

      Formula varF =
          simpleCast(
              pFmgrV.makeVariable(
                  pC2Formula.getFormulaTypeFromCType((CType) pVarToType.get(var)),
                  var.getExtendedQualifiedName()),
              pMachineModel.isSigned((CSimpleType) pVarToType.get(var)),
              (CType) pVarToType.get(var),
              compareType,
              pFmgrV,
              pC2Formula);

      Formula varF2 =
          simpleCast(
              pFmgrV.makeVariable(
                  pC2Formula.getFormulaTypeFromCType((CType) pVarToType.get(var2)),
                  var2.getExtendedQualifiedName()),
              pMachineModel.isSigned((CSimpleType) pVarToType.get(var2)),
              (CType) pVarToType.get(var2),
              compareType,
              pFmgrV,
              pC2Formula);

      boolean signed = pMachineModel.isSigned((CSimpleType) pVarToType.get(var));

      BooleanFormula fEnc =
          switch (comp) {
            case EQ -> pFmgrV.makeEqual(varF, varF2);
            case GT -> pFmgrV.makeGreaterThan(varF, varF2, signed);
            case GEQ -> pFmgrV.makeGreaterOrEqual(varF, varF2, signed);
            case LT -> pFmgrV.makeLessThan(varF, varF2, signed);
            case LEQ -> pFmgrV.makeLessOrEqual(varF, varF2, signed);
            case UNEQ -> pFmgrV.makeNot(pFmgrV.makeEqual(varF, varF2));
            case NONE -> throw new AssertionError("Unsupported comparison type");
              //            default -> throw new AssertionError("Unknown comparison type");
          };

      return ImmutableList.of(fEnc);
    }
  }

  private static class TwoVariableArithmeticInvariant extends CandidateInvariant {
    // TODO currently use Java semantics during computation
    // construct C expressions and evaluate?
    private final MemoryLocation var1;
    private final MemoryLocation var2;
    private final boolean isFloatingPoint;
    private final boolean divReversed;
    private Pair<Number, EqualCompareType> opMul;
    private Pair<Number, EqualCompareType> opDiv;
    private Pair<Number, EqualCompareType> opModVar1Left;
    private Pair<Number, EqualCompareType> opModVar2Left;

    private TwoVariableArithmeticInvariant(
        final MemoryLocation pVar,
        final NumericValue pValue,
        final MemoryLocation pVar2,
        final NumericValue pValue2) {
      var1 = Preconditions.checkNotNull(pVar);
      var2 = Preconditions.checkNotNull(pVar2);
      Preconditions.checkNotNull(pValue);
      Preconditions.checkNotNull(pValue2);
      Number val1 = pValue.getNumber();
      if (val1 instanceof BigInteger) {
        val1 = ((BigInteger) val1).longValueExact();
      }
      Number val2 = pValue2.getNumber();
      if (val2 instanceof BigInteger) {
        val2 = ((BigInteger) val2).longValueExact();
      }

      Preconditions.checkArgument(
          (isFloatingNumber(val1) && isFloatingNumber(val2))
              || (isIntegralType(val1) && isIntegralType(val2)));
      isFloatingPoint = isFloatingNumber(pValue.getNumber());
      divReversed =
          isFloatingPoint
              ? Double.compare(val1.doubleValue(), val2.doubleValue()) <= 0
              : Long.compare(val1.longValue(), val2.longValue()) <= 0;

      initOperators(val1, val2);
    }

    private void initOperators(final Number pVal1, final Number pVal2) {
      if (isFloatingPoint) {
        double val1 = pVal1.doubleValue();
        double val2 = pVal2.doubleValue();
        opMul = Pair.of(val1 * val2, EqualCompareType.EQ);
        if (divReversed ? val1 == 0 : val2 == 0) {
          opDiv = Pair.of(0.0, EqualCompareType.NONE);
        } else {
          double res = divReversed ? val2 / val1 : val1 / val2;
          opDiv = Pair.of(res, EqualCompareType.EQ);
        }
        // modulo only supported for integer variables
        opModVar1Left = Pair.of(0.0, EqualCompareType.NONE);
        opModVar2Left = Pair.of(0.0, EqualCompareType.NONE);
      } else {
        long val1 = pVal1.longValue();
        long val2 = pVal2.longValue();
        opMul = Pair.of(val1 * val2, EqualCompareType.EQ);
        if (divReversed ? val1 == 0 : val2 == 0) {
          opDiv = Pair.of(0, EqualCompareType.NONE);
        } else {
          long res = divReversed ? val2 / val1 : val1 / val2;
          opDiv = Pair.of(res, EqualCompareType.EQ);
        }
        if (val2 == 0) {
          opModVar1Left = Pair.of(0, EqualCompareType.NONE);
        } else {
          opModVar1Left = Pair.of(val1 % val2, EqualCompareType.EQ);
        }
        if (val1 == 0) {
          opModVar2Left = Pair.of(0, EqualCompareType.NONE);
        } else {
          opModVar2Left = Pair.of(val2 % val1, EqualCompareType.EQ);
        }
      }
    }

    private boolean adaptToAdditionalValuesMult(final Number pValVar1, final Number pValVar2) {
      if (opMul.getSecond() == EqualCompareType.NONE) {
        return false;
      }
      int compRes;
      if (isFloatingPoint) {
        double mulRes = pValVar1.doubleValue() * pValVar2.doubleValue();
        compRes = Double.compare(mulRes, opMul.getFirst().doubleValue());
      } else {
        long mulRes = pValVar1.longValue() * pValVar2.longValue();
        compRes = Long.compare(mulRes, opMul.getFirst().longValue());
      }

      EqualCompareType newType = updateCompareType(compRes, opMul.getSecond());

      if (newType != opMul.getSecond()) {
        opMul = Pair.of(opMul.getFirst(), newType);
      }

      return newType != EqualCompareType.NONE;
    }

    private boolean adaptToAdditionalValuesDiv(final Number pValVar1, final Number pValVar2) {
      if (opDiv.getSecond() == EqualCompareType.NONE) {
        return false;
      }
      int compRes;
      if (isFloatingPoint) {
        if ((divReversed && pValVar1.doubleValue() == 0)
            || (!divReversed && pValVar2.doubleValue() == 0)) {
          opDiv = Pair.of(opDiv.getFirst(), EqualCompareType.NONE);
          return false;
        }
        double divRes =
            divReversed
                ? pValVar2.doubleValue() / pValVar1.doubleValue()
                : pValVar1.doubleValue() / pValVar2.doubleValue();
        compRes = Double.compare(divRes, opDiv.getFirst().doubleValue());
      } else {
        // use integer division
        if ((divReversed && pValVar1.longValue() == 0)
            || (!divReversed && pValVar2.longValue() == 0)) {
          opDiv = Pair.of(opDiv.getFirst(), EqualCompareType.NONE);
          return false;
        }
        long divRes =
            divReversed
                ? pValVar2.longValue() / pValVar1.longValue()
                : pValVar1.longValue() / pValVar2.longValue();
        compRes = Long.compare(divRes, opDiv.getFirst().longValue());
      }

      EqualCompareType newType = updateCompareType(compRes, opDiv.getSecond());

      if (newType != opDiv.getSecond()) {
        opDiv = Pair.of(opDiv.getFirst(), newType);
      }

      return newType != EqualCompareType.NONE;
    }

    private boolean adaptToAdditionalValuesMod(
        final Number pValVar1, final Number pValVar2, final boolean isVar1Left) {
      // only supported for integer values
      if ((isVar1Left && opModVar1Left.getSecond() == EqualCompareType.NONE)
          || (!isVar1Left && opModVar2Left.getSecond() == EqualCompareType.NONE)) {
        return false;
      }

      int compRes;
      if (isFloatingPoint) {
        // modulo not supported for floating point
        if (isVar1Left) {
          opModVar1Left = Pair.of(opModVar1Left.getFirst(), EqualCompareType.NONE);
        } else {
          opModVar2Left = Pair.of(opModVar2Left.getFirst(), EqualCompareType.NONE);
        }
        return false;
      } else {
        if (isVar1Left && pValVar2.longValue() == 0) {
          opModVar1Left = Pair.of(opModVar1Left.getFirst(), EqualCompareType.NONE);
          return false;
        }
        if (!isVar1Left && pValVar1.longValue() == 0) {
          opModVar2Left = Pair.of(opModVar2Left.getFirst(), EqualCompareType.NONE);
          return false;
        }
        if (isVar1Left) {
          long modRes = pValVar1.longValue() % pValVar2.longValue();
          compRes = Long.compare(modRes, opModVar1Left.getFirst().longValue());
          EqualCompareType newType = updateCompareType(compRes, opModVar1Left.getSecond());
          if (newType != opModVar1Left.getSecond()) {
            opModVar1Left = Pair.of(opModVar1Left.getFirst(), newType);
          }
          return newType != EqualCompareType.NONE;
        } else {
          long modRes = pValVar2.longValue() % pValVar1.longValue();
          compRes = Long.compare(modRes, opModVar2Left.getFirst().longValue());
          EqualCompareType newType = updateCompareType(compRes, opModVar2Left.getSecond());
          if (newType != opModVar2Left.getSecond()) {
            opModVar2Left = Pair.of(opModVar2Left.getFirst(), newType);
          }
          return newType != EqualCompareType.NONE;
        }
      }
    }

    private boolean adaptToAdditionalValues(final Value pValVar1, final Value pValVar2) {
      Preconditions.checkNotNull(pValVar1);
      Preconditions.checkNotNull(pValVar2);

      NumericValue numValVar1 = extractNumValue(pValVar1);
      Preconditions.checkNotNull(numValVar1);
      NumericValue numValVar2 = extractNumValue(pValVar2);
      Preconditions.checkNotNull(numValVar2);

      Number valVar1 = numValVar1.getNumber();
      Number valVar2 = numValVar2.getNumber();
      try {
        if (valVar1 instanceof BigInteger) {
          valVar1 = ((BigInteger) valVar1).longValueExact();
        }

        if (valVar2 instanceof BigInteger) {
          valVar2 = ((BigInteger) valVar2).longValueExact();
        }
      } catch (ArithmeticException e) {
        opMul = Pair.of(opMul.getFirst(), EqualCompareType.NONE);
        opDiv = Pair.of(opDiv.getFirst(), EqualCompareType.NONE);
        opModVar1Left = Pair.of(opModVar1Left.getFirst(), EqualCompareType.NONE);
        opModVar2Left = Pair.of(opModVar2Left.getFirst(), EqualCompareType.NONE);
        return false;
      }

      Preconditions.checkArgument(
          (isFloatingPoint && isFloatingNumber(valVar1))
              || (!isFloatingPoint && isIntegralType(valVar1)));
      Preconditions.checkArgument(
          (isFloatingPoint && isFloatingNumber(valVar2))
              || (!isFloatingPoint && isIntegralType(valVar2)));

      boolean adaptSuccess = false;
      if (opMul.getSecond() != EqualCompareType.NONE) {
        adaptSuccess = adaptSuccess || adaptToAdditionalValuesMult(valVar1, valVar2);
      }

      if (opDiv.getSecond() != EqualCompareType.NONE) {
        adaptSuccess = adaptSuccess || adaptToAdditionalValuesDiv(valVar1, valVar2);
      }

      if (opModVar1Left.getSecond() != EqualCompareType.NONE) {
        adaptSuccess = adaptSuccess || adaptToAdditionalValuesMod(valVar1, valVar2, true);
      }

      if (opModVar2Left.getSecond() != EqualCompareType.NONE) {
        adaptSuccess = adaptSuccess || adaptToAdditionalValuesMod(valVar1, valVar2, false);
      }

      return adaptSuccess;
    }

    private int getNumInvariants() {
      int num = 0;
      if (opMul.getSecond() != EqualCompareType.NONE) {
        num++;
      }
      if (opDiv.getSecond() != EqualCompareType.NONE) {
        num++;
      }
      if (opModVar1Left.getSecond() != EqualCompareType.NONE) {
        num++;
      }

      if (opModVar2Left.getSecond() != EqualCompareType.NONE) {
        num++;
      }
      return num;
    }

    @Override
    protected Collection<BooleanFormula> asBooleanFormulae(
        FormulaManagerView pFmgrV,
        ImmutableMap<MemoryLocation, Type> pVarToType,
        CtoFormulaConverter pC2Formula,
        MachineModel pMachineModel) {
      Collection<BooleanFormula> result = new ArrayList<>(4);

      Preconditions.checkArgument(pVarToType.containsKey(var1));
      Preconditions.checkArgument(pVarToType.get(var1) instanceof CSimpleType);
      Preconditions.checkArgument(pVarToType.containsKey(var2));
      Preconditions.checkArgument(pVarToType.get(var2) instanceof CSimpleType);
      CSimpleType type1 = (CSimpleType) pVarToType.get(var1);
      CSimpleType type2 = (CSimpleType) pVarToType.get(var2);
      Preconditions.checkArgument(
          ((isFloatingPoint
                      && type1.getType().isFloatingPointType()
                      && type2.getType().isFloatingPointType())
                  || (!isFloatingPoint
                      && type1.getType().isIntegerType()
                      && type2.getType().isIntegerType()))
              && (pMachineModel.isSigned(type1) == pMachineModel.isSigned(type2)));

      boolean signedVars = pMachineModel.isSigned(type1);
      boolean signed;
      CSimpleType compareTypeVars = getCompareType(type1, type2);
      CSimpleType formulaType;

      Formula varF =
          pFmgrV.makeVariable(
              pC2Formula.getFormulaTypeFromCType(type1), var1.getExtendedQualifiedName());
      Formula varF2 =
          pFmgrV.makeVariable(
              pC2Formula.getFormulaTypeFromCType(type2), var2.getExtendedQualifiedName());

      if (opMul.getSecond() != EqualCompareType.NONE) {
        formulaType =
            getCompareType(
                compareTypeVars, getCTypeFromValue(signedVars, opMul.getFirst(), pMachineModel));
        signed = pMachineModel.isSigned(formulaType);
        result.add(
            makeComparison(
                pFmgrV.makeMultiply(
                    simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula),
                    simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula)),
                opMul,
                pFmgrV,
                pC2Formula.getFormulaTypeFromCType(formulaType),
                signed));
      }

      if (opDiv.getSecond() != EqualCompareType.NONE) {
        formulaType =
            getCompareType(
                compareTypeVars, getCTypeFromValue(signedVars, opDiv.getFirst(), pMachineModel));
        signed = pMachineModel.isSigned(formulaType);
        if (divReversed) {
          result.add(
              makeComparison(
                  pFmgrV.makeDivide(
                      simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula),
                      simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula),
                      signed),
                  opDiv,
                  pFmgrV,
                  pC2Formula.getFormulaTypeFromCType(formulaType),
                  signed));
        } else {
          result.add(
              makeComparison(
                  pFmgrV.makeDivide(
                      simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula),
                      simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula),
                      signed),
                  opDiv,
                  pFmgrV,
                  pC2Formula.getFormulaTypeFromCType(formulaType),
                  signed));
        }
      }

      if (opModVar1Left.getSecond() != EqualCompareType.NONE) {
        formulaType =
            getCompareType(
                compareTypeVars,
                getCTypeFromValue(signedVars, opModVar1Left.getFirst(), pMachineModel));
        signed = pMachineModel.isSigned(formulaType);
        result.add(
            makeComparison(
                pFmgrV.makeRemainder(
                    simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula),
                    simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula),
                    signed),
                opModVar1Left,
                pFmgrV,
                pC2Formula.getFormulaTypeFromCType(formulaType),
                signed));
      }

      if (opModVar2Left.getSecond() != EqualCompareType.NONE) {
        formulaType =
            getCompareType(
                compareTypeVars,
                getCTypeFromValue(signedVars, opModVar1Left.getFirst(), pMachineModel));
        signed = pMachineModel.isSigned(formulaType);
        result.add(
            makeComparison(
                pFmgrV.makeRemainder(
                    simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula),
                    simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula),
                    signed),
                opModVar2Left,
                pFmgrV,
                pC2Formula.getFormulaTypeFromCType(formulaType),
                signed));
      }

      return result;
    }
  }

  private static class TwoVariableBitOpsInvariant extends CandidateInvariant {
    // bit operations only supported on integer type
    // TODO currently use Java semantics during computation
    private final MemoryLocation var1;
    private final MemoryLocation var2;
    private Pair<Number, EqualCompareType> opBitAnd;
    private Pair<Number, EqualCompareType> opBitOr;
    private Pair<Number, EqualCompareType> opBitXor;
    private final boolean includeShiftOps;
    // <<
    private Pair<Number, EqualCompareType> opVar1ShiftLeft;
    private Pair<Number, EqualCompareType> opVar2ShiftLeft;
    // >>
    private Pair<Number, EqualCompareType> opVar1ShiftRight;
    private Pair<Number, EqualCompareType> opVar2ShiftRight;

    private TwoVariableBitOpsInvariant(
        final MemoryLocation pVar,
        final NumericValue pValue,
        final MemoryLocation pVar2,
        final NumericValue pValue2,
        final boolean considerShiftOps) {
      var1 = Preconditions.checkNotNull(pVar);
      var2 = Preconditions.checkNotNull(pVar2);
      includeShiftOps = considerShiftOps;
      Preconditions.checkNotNull(pValue);
      Preconditions.checkNotNull(pValue2);
      Number val1 = pValue.getNumber();
      if (val1 instanceof BigInteger) {
        val1 = ((BigInteger) val1).longValueExact();
      }
      Number val2 = pValue2.getNumber();
      if (val2 instanceof BigInteger) {
        val2 = ((BigInteger) val2).longValueExact();
      }
      Preconditions.checkArgument(isIntegralType(val1) && isIntegralType(val2));

      initOperators(val1, val2);
    }

    private void initOperators(final Number pVal1, final Number pVal2) {
      long val1 = pVal1.longValue();
      long val2 = pVal2.longValue();
      opBitAnd = Pair.of(val1 & val2, EqualCompareType.EQ);
      opBitOr = Pair.of(val1 | val2, EqualCompareType.EQ);
      opBitXor = Pair.of(val1 ^ val2, EqualCompareType.EQ);
      if (val1 >= 0 && includeShiftOps) {
        opVar2ShiftLeft = Pair.of(val2 << val1, EqualCompareType.EQ);
        opVar2ShiftRight = Pair.of(val2 >> val1, EqualCompareType.EQ);

      } else {
        opVar2ShiftLeft = Pair.of(0, EqualCompareType.NONE);
        opVar2ShiftRight = Pair.of(0, EqualCompareType.NONE);
      }
      if (val2 >= 0 && includeShiftOps) {
        opVar1ShiftLeft = Pair.of(val1 << val2, EqualCompareType.EQ);
        opVar1ShiftRight = Pair.of(val1 >> val2, EqualCompareType.EQ);

      } else {
        opVar1ShiftLeft = Pair.of(0, EqualCompareType.NONE);
        opVar1ShiftRight = Pair.of(0, EqualCompareType.NONE);
      }
    }

    private boolean adaptToAdditionalValues(final Value pValVar1, final Value pValVar2) {
      Preconditions.checkNotNull(pValVar1);
      Preconditions.checkNotNull(pValVar2);

      NumericValue numValVar1 = extractNumValue(pValVar1);
      Preconditions.checkNotNull(numValVar1);
      NumericValue numValVar2 = extractNumValue(pValVar2);
      Preconditions.checkNotNull(numValVar2);

      Number valVar1 = numValVar1.getNumber();
      Number valVar2 = numValVar2.getNumber();
      try {
        if (valVar1 instanceof BigInteger) {
          valVar1 = ((BigInteger) valVar1).longValueExact();
        }

        if (valVar2 instanceof BigInteger) {
          valVar2 = ((BigInteger) valVar2).longValueExact();
        }
      } catch (ArithmeticException e) {
        opBitAnd = Pair.of(opBitAnd.getFirst(), EqualCompareType.NONE);
        opBitOr = Pair.of(opBitOr.getFirst(), EqualCompareType.NONE);
        opBitXor = Pair.of(opBitXor.getFirst(), EqualCompareType.NONE);
        opVar1ShiftLeft = Pair.of(opVar1ShiftLeft.getFirst(), EqualCompareType.NONE);
        opVar2ShiftLeft = Pair.of(opVar2ShiftLeft.getFirst(), EqualCompareType.NONE);
        opVar1ShiftRight = Pair.of(opVar1ShiftRight.getFirst(), EqualCompareType.NONE);
        opVar2ShiftRight = Pair.of(opVar2ShiftRight.getFirst(), EqualCompareType.NONE);
        return false;
      }

      Preconditions.checkArgument(isIntegralType(valVar1));
      Preconditions.checkArgument(isIntegralType(valVar2));

      long computeRes;
      int compRes;
      EqualCompareType newType;

      if (opBitAnd.getSecond() != EqualCompareType.NONE) {
        computeRes = valVar1.longValue() & valVar2.longValue();
        compRes = Long.compare(computeRes, opBitAnd.getFirst().longValue());
        newType = updateCompareType(compRes, opBitAnd.getSecond());

        if (newType != opBitAnd.getSecond()) {
          opBitAnd = Pair.of(opBitAnd.getFirst(), newType);
        }
      }

      if (opBitOr.getSecond() != EqualCompareType.NONE) {
        computeRes = valVar1.longValue() | valVar2.longValue();
        compRes = Long.compare(computeRes, opBitOr.getFirst().longValue());
        newType = updateCompareType(compRes, opBitOr.getSecond());

        if (newType != opBitOr.getSecond()) {
          opBitOr = Pair.of(opBitOr.getFirst(), newType);
        }
      }

      if (opBitXor.getSecond() != EqualCompareType.NONE) {
        computeRes = valVar1.longValue() ^ valVar2.longValue();
        compRes = Long.compare(computeRes, opBitXor.getFirst().longValue());
        newType = updateCompareType(compRes, opBitXor.getSecond());

        if (newType != opBitXor.getSecond()) {
          opBitXor = Pair.of(opBitXor.getFirst(), newType);
        }
      }
      if (includeShiftOps) {

        if (opVar1ShiftLeft.getSecond() != EqualCompareType.NONE) {
          if (valVar2.longValue() >= 0) {
            computeRes = valVar1.longValue() << valVar2.longValue();
            compRes = Long.compare(computeRes, opVar1ShiftLeft.getFirst().longValue());
            newType = updateCompareType(compRes, opVar1ShiftLeft.getSecond());
          } else {
            newType = EqualCompareType.NONE;
          }

          if (newType != opVar1ShiftLeft.getSecond()) {
            opVar1ShiftLeft = Pair.of(opVar1ShiftLeft.getFirst(), newType);
          }
        }

        if (opVar2ShiftLeft.getSecond() != EqualCompareType.NONE) {
          if (valVar1.longValue() >= 0) {
            computeRes = valVar2.longValue() << valVar1.longValue();
            compRes = Long.compare(computeRes, opVar2ShiftLeft.getFirst().longValue());
            newType = updateCompareType(compRes, opVar2ShiftLeft.getSecond());
          } else {
            newType = EqualCompareType.NONE;
          }

          if (newType != opVar2ShiftLeft.getSecond()) {
            opVar2ShiftLeft = Pair.of(opVar2ShiftLeft.getFirst(), newType);
          }
        }

        if (opVar1ShiftRight.getSecond() != EqualCompareType.NONE) {
          if (valVar2.longValue() >= 0) {
            computeRes = valVar1.longValue() >> valVar2.longValue();
            compRes = Long.compare(computeRes, opVar1ShiftRight.getFirst().longValue());
            newType = updateCompareType(compRes, opVar1ShiftRight.getSecond());
          } else {
            newType = EqualCompareType.NONE;
          }

          if (newType != opVar1ShiftRight.getSecond()) {
            opVar1ShiftRight = Pair.of(opVar1ShiftRight.getFirst(), newType);
          }
        }

        if (opVar2ShiftRight.getSecond() != EqualCompareType.NONE) {
          if (valVar1.longValue() >= 0) {
            computeRes = valVar2.longValue() >> valVar1.longValue();
            compRes = Long.compare(computeRes, opVar2ShiftRight.getFirst().longValue());
            newType = updateCompareType(compRes, opVar2ShiftRight.getSecond());
          } else {
            newType = EqualCompareType.NONE;
          }

          if (newType != opVar2ShiftRight.getSecond()) {
            opVar2ShiftRight = Pair.of(opVar2ShiftRight.getFirst(), newType);
          }
        }
      }

      return opBitAnd.getSecond() != EqualCompareType.NONE
          || opBitOr.getSecond() != EqualCompareType.NONE
          || opBitXor.getSecond() != EqualCompareType.NONE
          || (includeShiftOps
              && (opVar1ShiftLeft.getSecond() != EqualCompareType.NONE
                  || opVar2ShiftLeft.getSecond() != EqualCompareType.NONE
                  || opVar1ShiftRight.getSecond() != EqualCompareType.NONE
                  || opVar2ShiftRight.getSecond() != EqualCompareType.NONE));
    }

    private int getNumInvariants() {
      int num = 0;

      if (opBitAnd.getSecond() != EqualCompareType.NONE) {
        num++;
      }

      if (opBitOr.getSecond() != EqualCompareType.NONE) {
        num++;
      }

      if (opBitXor.getSecond() != EqualCompareType.NONE) {
        num++;
      }

      if (includeShiftOps) {

        if (opVar1ShiftLeft.getSecond() != EqualCompareType.NONE) {
          num++;
        }

        if (opVar2ShiftLeft.getSecond() != EqualCompareType.NONE) {
          num++;
        }

        if (opVar1ShiftRight.getSecond() != EqualCompareType.NONE) {
          num++;
        }

        if (opVar2ShiftRight.getSecond() != EqualCompareType.NONE) {
          num++;
        }
      }

      return num;
    }

    @Override
    protected Collection<BooleanFormula> asBooleanFormulae(
        final FormulaManagerView pFmgrV,
        final ImmutableMap<MemoryLocation, Type> pVarToType,
        final CtoFormulaConverter pC2Formula,
        final MachineModel pMachineModel) {
      Collection<BooleanFormula> result;
      if (includeShiftOps) {
        result = new ArrayList<>(7);
      } else {
        result = new ArrayList<>(3);
      }

      Preconditions.checkArgument(pVarToType.containsKey(var1));
      Preconditions.checkArgument(pVarToType.get(var1) instanceof CSimpleType);
      Preconditions.checkArgument(pVarToType.containsKey(var2));
      Preconditions.checkArgument(pVarToType.get(var2) instanceof CSimpleType);
      CSimpleType type1 = (CSimpleType) pVarToType.get(var1);
      CSimpleType type2 = (CSimpleType) pVarToType.get(var2);
      Preconditions.checkArgument(
          (type1.getType().isIntegerType() && type2.getType().isIntegerType())
              && (pMachineModel.isSigned(type1) == pMachineModel.isSigned(type2)));

      boolean signedVars = pMachineModel.isSigned(type1);
      boolean signed;
      CSimpleType compareTypeVars = getCompareType(type1, type2);
      CSimpleType formulaType;

      Formula varF =
          pFmgrV.makeVariable(
              pC2Formula.getFormulaTypeFromCType(type1), var1.getExtendedQualifiedName());
      Formula varF2 =
          pFmgrV.makeVariable(
              pC2Formula.getFormulaTypeFromCType(type2), var2.getExtendedQualifiedName());

      Preconditions.checkState(
          varF instanceof BitvectorFormula && varF2 instanceof BitvectorFormula);

      if (opBitAnd.getSecond() != EqualCompareType.NONE) {
        formulaType =
            getCompareType(
                compareTypeVars, getCTypeFromValue(signedVars, opBitAnd.getFirst(), pMachineModel));
        signed = pMachineModel.isSigned(formulaType);
        result.add(
            makeComparison(
                pFmgrV.makeAnd(
                    simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula),
                    simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula)),
                opBitAnd,
                pFmgrV,
                pC2Formula.getFormulaTypeFromCType(formulaType),
                signed));
      }

      if (opBitOr.getSecond() != EqualCompareType.NONE) {
        formulaType =
            getCompareType(
                compareTypeVars, getCTypeFromValue(signedVars, opBitOr.getFirst(), pMachineModel));
        signed = pMachineModel.isSigned(formulaType);
        result.add(
            makeComparison(
                pFmgrV.makeOr(
                    simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula),
                    simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula)),
                opBitOr,
                pFmgrV,
                pC2Formula.getFormulaTypeFromCType(formulaType),
                signed));
      }

      if (opBitXor.getSecond() != EqualCompareType.NONE) {
        formulaType =
            getCompareType(
                compareTypeVars, getCTypeFromValue(signedVars, opBitXor.getFirst(), pMachineModel));
        signed = pMachineModel.isSigned(formulaType);
        result.add(
            makeComparison(
                pFmgrV.makeXor(
                    simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula),
                    simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula)),
                opBitXor,
                pFmgrV,
                pC2Formula.getFormulaTypeFromCType(formulaType),
                signed));
      }

      if (includeShiftOps) {

        if (opVar1ShiftLeft.getSecond() != EqualCompareType.NONE) {
          formulaType =
              getCompareType(
                  compareTypeVars,
                  getCTypeFromValue(signedVars, opVar1ShiftLeft.getFirst(), pMachineModel));
          signed = pMachineModel.isSigned(formulaType);
          result.add(
              makeComparison(
                  pFmgrV.makeShiftLeft(
                      simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula),
                      simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula)),
                  opVar1ShiftLeft,
                  pFmgrV,
                  pC2Formula.getFormulaTypeFromCType(formulaType),
                  signed));
        }

        if (opVar2ShiftLeft.getSecond() != EqualCompareType.NONE) {
          formulaType =
              getCompareType(
                  compareTypeVars,
                  getCTypeFromValue(signedVars, opVar2ShiftLeft.getFirst(), pMachineModel));
          signed = pMachineModel.isSigned(formulaType);
          result.add(
              makeComparison(
                  pFmgrV.makeShiftLeft(
                      simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula),
                      simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula)),
                  opVar2ShiftLeft,
                  pFmgrV,
                  pC2Formula.getFormulaTypeFromCType(formulaType),
                  signed));
        }

        if (opVar1ShiftRight.getSecond() != EqualCompareType.NONE) {
          formulaType =
              getCompareType(
                  compareTypeVars,
                  getCTypeFromValue(signedVars, opVar1ShiftRight.getFirst(), pMachineModel));
          signed = pMachineModel.isSigned(formulaType);
          result.add(
              makeComparison(
                  pFmgrV.makeShiftRight(
                      simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula),
                      simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula),
                      signed),
                  opVar1ShiftRight,
                  pFmgrV,
                  pC2Formula.getFormulaTypeFromCType(formulaType),
                  signed));
        }

        if (opVar2ShiftRight.getSecond() != EqualCompareType.NONE) {
          formulaType =
              getCompareType(
                  compareTypeVars,
                  getCTypeFromValue(signedVars, opVar2ShiftRight.getFirst(), pMachineModel));
          signed = pMachineModel.isSigned(formulaType);
          result.add(
              makeComparison(
                  pFmgrV.makeShiftRight(
                      simpleCast(varF2, signed, type2, formulaType, pFmgrV, pC2Formula),
                      simpleCast(varF, signed, type1, formulaType, pFmgrV, pC2Formula),
                      signed),
                  opVar2ShiftRight,
                  pFmgrV,
                  pC2Formula.getFormulaTypeFromCType(formulaType),
                  signed));
        }
      }

      return result;
    }
  }

  private static class LinearInEqualityInvariant extends CandidateInvariant {
    // TODO currently use Java semantics

    private final MemoryLocation[] vars;
    private final Number[] coefficients;
    private final boolean isFloatingType;
    private EqualCompareType op;

    private LinearInEqualityInvariant(
        final MemoryLocation[] pVars, final Number[] pCoefficients, final boolean pFloatingType) {
      Preconditions.checkArgument(pVars != null && pCoefficients != null);
      Preconditions.checkArgument(pVars.length > 0 && pCoefficients.length == pVars.length + 1);
      Preconditions.checkArgument(
          FluentIterable.from(pCoefficients)
              .allMatch(
                  coefficient ->
                      (coefficient != null
                          && ((pFloatingType && isFloatingNumber(coefficient))
                              || (!pFloatingType && isIntegralType(coefficient))))));

      vars = pVars;
      coefficients = pCoefficients;
      op = EqualCompareType.EQ;
      isFloatingType = pFloatingType;
    }

    private boolean adaptToAdditionalValues(final Value[] newVals) {
      Preconditions.checkNotNull(newVals);
      Preconditions.checkArgument(newVals.length == vars.length);

      try {
        int compResWithZero = evaluateLinearEquation(newVals);

        op =
            switch (op) {
              case EQ ->
                  compResWithZero < 0
                      ? op = EqualCompareType.LEQ
                      : (compResWithZero > 0 ? EqualCompareType.GEQ : op);
              case GEQ -> compResWithZero >= 0 ? op : EqualCompareType.NONE;
              case LEQ -> compResWithZero <= 0 ? op : EqualCompareType.NONE;
              case NONE -> op;
            };

      } catch (ArithmeticException e) {
        // do nothing
      }
      return op != EqualCompareType.NONE;
    }

    private int evaluateLinearEquation(final Value[] pNewVals) throws ArithmeticException {
      if (isFloatingType) {
        double resComp = 0;
        NumericValue varVal;
        for (int i = 0; i < pNewVals.length; i++) {
          varVal = extractNumValue(pNewVals[i]);
          Preconditions.checkNotNull(varVal);
          Preconditions.checkArgument(isFloatingNumber(varVal.getNumber()));
          resComp += coefficients[i].doubleValue() * varVal.doubleValue();
        }
        resComp += coefficients[coefficients.length - 1].doubleValue();
        return Double.compare(resComp, 0);

      } else {
        long resComp = 0;
        NumericValue varVal;
        for (int i = 0; i < pNewVals.length; i++) {
          varVal = extractNumValue(pNewVals[i]);
          Preconditions.checkNotNull(varVal);
          Number numVal = varVal.getNumber();
          if (numVal instanceof BigInteger) {
            numVal = ((BigInteger) numVal).longValueExact();
          }
          Preconditions.checkArgument(isIntegralType(numVal));
          resComp += coefficients[i].longValue() * numVal.longValue();
        }
        resComp += coefficients[coefficients.length - 1].longValue();
        return Long.compare(resComp, 0);
      }
    }

    @Override
    protected Collection<BooleanFormula> asBooleanFormulae(
        final FormulaManagerView pFmgrV,
        final ImmutableMap<MemoryLocation, Type> pVarToType,
        final CtoFormulaConverter pC2Formula,
        final MachineModel pMachineModel) {

      Formula varAdditionTerm = null;
      Formula varTerm;
      MemoryLocation var;

      if (vars.length < 1) {
        return ImmutableList.of();
      }

      Preconditions.checkArgument(pVarToType.containsKey(vars[0]));
      Preconditions.checkArgument(pVarToType.get(vars[0]) instanceof CSimpleType);

      boolean signed = pMachineModel.isSigned((CSimpleType) pVarToType.get(vars[0]));
      CSimpleType commonType = (CSimpleType) pVarToType.get(vars[0]);

      for (MemoryLocation memLoc : FluentIterable.from(vars).skip(1)) {
        Preconditions.checkArgument(pVarToType.containsKey(memLoc));
        Preconditions.checkArgument(pVarToType.get(memLoc) instanceof CSimpleType);
        Preconditions.checkArgument(
            pMachineModel.isSigned((CSimpleType) pVarToType.get(memLoc)) == signed);

        commonType = getCompareType(commonType, (CSimpleType) pVarToType.get(memLoc));
        signed = pMachineModel.isSigned(commonType);
      }

      commonType =
          getCompareType(
              commonType,
              getCTypeFromValue(signed, coefficients[coefficients.length - 1], pMachineModel));
      signed = pMachineModel.isSigned(commonType);
      FormulaType<?> formulaType = pC2Formula.getFormulaTypeFromCType(commonType);

      for (int i = 0; i < vars.length; i++) {
        var = vars[i];

        varTerm =
            simpleCast(
                pFmgrV.makeVariable(
                    pC2Formula.getFormulaTypeFromCType((CType) pVarToType.get(var)),
                    var.getExtendedQualifiedName()),
                signed,
                (CType) pVarToType.get(var),
                commonType,
                pFmgrV,
                pC2Formula);
        varTerm =
            pFmgrV.makeMultiply(
                encodeNumVal(pFmgrV, formulaType, varTerm, coefficients[i]), varTerm);
        if (i == 0) {
          varAdditionTerm = varTerm;
        } else {
          varAdditionTerm = pFmgrV.makePlus(varAdditionTerm, varTerm);
        }
      }

      Preconditions.checkState(varAdditionTerm != null);

      varAdditionTerm =
          pFmgrV.makePlus(
              varAdditionTerm,
              encodeNumVal(
                  pFmgrV, formulaType, varAdditionTerm, coefficients[coefficients.length - 1]));

      Formula constF =
          encodeNumVal(
              pFmgrV,
              formulaType,
              varAdditionTerm,
              isFloatingType
                  ? Double.valueOf(0)
                  : (Number)
                      Long.valueOf(0)); // cast required to avoid that long object becomes double

      // export in form ax+bx+c=0 (or similar) not ax+bx=-c
      return switch (op) {
        case EQ -> ImmutableSet.of(pFmgrV.makeEqual(varAdditionTerm, constF));
        case GEQ -> ImmutableSet.of(pFmgrV.makeGreaterOrEqual(varAdditionTerm, constF, signed));
        case LEQ -> ImmutableSet.of(pFmgrV.makeLessOrEqual(varAdditionTerm, constF, signed));
        default ->
            throw new AssertionError("Unsupported relational operator in linear (in)equality.");
      };
    }

    private static Number[] solveLinearEquation(
        final Number[][] pEquationMatrix, final boolean allowFloatSolution) {
      Preconditions.checkNotNull(pEquationMatrix);
      Preconditions.checkArgument(pEquationMatrix.length >= 2);
      for (final Number[] eq : pEquationMatrix) {
        Preconditions.checkArgument(eq.length == pEquationMatrix.length + 1);

        for (int j = 0; j < eq.length; j++) {
          if (eq[j] instanceof BigInteger) {
            try {
              eq[j] = ((BigInteger) eq[j]).longValueExact();
            } catch (ArithmeticException e) {
              return null;
            }
          }

          Preconditions.checkArgument(
              (allowFloatSolution && isFloatingNumber(eq[j]))
                  || (!allowFloatSolution && isIntegralType(eq[j])));
        }
      }

      Number[] res = new Number[pEquationMatrix.length];
      if (allowFloatSolution) {
        double[][] matrix = new double[pEquationMatrix.length][pEquationMatrix.length + 1];
        // conversion
        for (int i = 0; i < pEquationMatrix.length; i++) {
          for (int j = 0; j < pEquationMatrix[i].length; j++) {
            matrix[i][j] = pEquationMatrix[i][j].doubleValue();
          }
        }

        // gauss elimination
        double[] tmp;
        // construct row echelon form
        for (int i = 0; i < matrix.length; i++) {
          if (matrix[i][i] == 0) {
            // try to change with later row
            for (int j = i + 1; j < matrix.length; j++) {
              if (matrix[j][i] != 0) {
                tmp = matrix[j];
                matrix[j] = matrix[i];
                matrix[i] = tmp;
                break;
              }
            }

            if (matrix[i][i] == 0) { // no solution
              return null;
            }
          }
          for (int j = i; j < matrix[i].length; j++) {
            matrix[i][j] = matrix[i][j] / matrix[i][i];
          }

          for (int j = i + 1; j < matrix.length; j++) {
            for (int k = i + 1; k < matrix[j].length; k++) {
              matrix[j][k] -= matrix[j][i] * matrix[i][k];
            }
            matrix[j][i] = 0;
          }
        }

        // backward substitution
        double dVal;
        for (int i = matrix.length - 1; i >= 0; i--) {
          dVal = matrix[i][matrix[i].length - 1];
          for (int j = i + 1; j < matrix.length; j++) {
            dVal -= matrix[i][j] * res[j].doubleValue();
          }
          res[i] = Double.valueOf(dVal);
        }
      } else {
        long[][] matrix = new long[pEquationMatrix.length][pEquationMatrix.length + 1];
        // conversion
        for (int i = 0; i < pEquationMatrix.length; i++) {
          for (int j = 0; j < pEquationMatrix[i].length; j++) {
            matrix[i][j] = pEquationMatrix[i][j].longValue();
          }
        }

        // gauss elimination
        long[] tmp;
        // construct row echelon form
        for (int i = 0; i < matrix.length; i++) {
          if (matrix[i][i] == 0 || !isIntegerDivision(matrix[i], i)) {
            // try to change with later row
            for (int j = i + 1; j < matrix.length; j++) {
              if (matrix[j][i] != 0 && isIntegerDivision(matrix[j], i)) {
                tmp = matrix[j];
                matrix[j] = matrix[i];
                matrix[i] = tmp;
                break;
              }
            }

            if (matrix[i][i] == 0 || !isIntegerDivision(matrix[i], i)) { // no solution
              return null;
            }
          }
          for (int j = i; j < matrix[i].length; j++) {
            matrix[i][j] = matrix[i][j] / matrix[i][i];
          }

          for (int j = i + 1; j < matrix.length; j++) {
            for (int k = i + 1; k < matrix[j].length; k++) {
              matrix[j][k] -= matrix[j][i] * matrix[i][k];
            }
            matrix[j][i] = 0;
          }
        }

        // backward substitution
        long dVal;
        for (int i = matrix.length - 1; i >= 0; i--) {
          dVal = matrix[i][matrix[i].length - 1];
          for (int j = i + 1; j < matrix.length; j++) {
            dVal -= matrix[i][j] * res[j].longValue();
          }
          res[i] = Long.valueOf(dVal);
        }
      }

      return res;
    }

    private static boolean isIntegerDivision(final long[] row, final int posDivisor) {
      Preconditions.checkArgument(row != null && posDivisor >= 0 && posDivisor < row.length);
      long divisor = row[posDivisor];
      for (long num : row) {
        if (num % divisor != 0) {
          return false;
        }
      }
      return true;
    }
  }
}
