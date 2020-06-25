/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.bdd;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.util.predicates.bdd.PJBDDRegion.unwrap;
import static org.sosy_lab.cpachecker.util.predicates.bdd.PJBDDRegion.wrap;

import com.google.common.primitives.ImmutableIntArray;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.PredicateOrderingStrategy;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.BooleanFormulaVisitor;
import org.sosy_lab.pjbdd.CreatorBuilder;
import org.sosy_lab.pjbdd.creators.Creator;
import org.sosy_lab.pjbdd.node.BDD;

public class PJBDDRegionManager implements RegionManager {

  private final Region trueFormula;
  private final Region falseFormula;
  private Creator bddCreator;

  public PJBDDRegionManager(Configuration pConfig) throws InvalidConfigurationException {
    BuildFromConfig buildFromConfig = new BuildFromConfig(pConfig);
    bddCreator = buildFromConfig.makeCreator();
    trueFormula = wrap(bddCreator.makeTrue());
    falseFormula = wrap(bddCreator.makeFalse());
  }

  @Override
  public boolean entails(Region f1, Region f2) {
    return bddCreator.entails(unwrap(f1), unwrap(f2));
  }

  @Override
  public Region createPredicate() {
    return wrap(bddCreator.makeVariable());
  }

  @Override
  public Region fromFormula(
      BooleanFormula pF, FormulaManagerView fmgr, Function<BooleanFormula, Region> atomToRegion) {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    if (bfmgr.isFalse(pF)) return makeFalse();
    if (bfmgr.isTrue(pF)) return makeTrue();

    try (FormulaToRegionConverter converter = new FormulaToRegionConverter(fmgr, atomToRegion)) {
      return wrap(bfmgr.visit(pF, converter));
    }
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region f) {
    BDD bdd = unwrap(f);
    return Triple.of(f, wrap(bdd.getHigh()), wrap(bdd.getLow()));
  }

  @Override
  public void printStatistics(PrintStream out) {
    out.println("Stats to be printed");
    // TODO    out.print(bddCreator.getCreatorStats().prettyPrint());
  }

  @Override
  public String getVersion() {
    return bddCreator.getVersion();
  }

  @Override
  public void setVarOrder(ImmutableIntArray pOrder) {
    bddCreator.setVarOrder(pOrder.asList());
  }

  @Override
  public void reorder(PredicateOrderingStrategy strategy) {
    throw new UnsupportedOperationException(
        "'reorder(PredicateOrderingStrategy)' not yet implemented");
  }

  @Override
  public RegionBuilder builder(ShutdownNotifier pShutdownNotifier) {
    return new RegionBuilder();
  }

  @Override
  public Region makeTrue() {
    return trueFormula;
  }

  @Override
  public Region makeFalse() {
    return falseFormula;
  }

  @Override
  public Region makeNot(Region f) {
    return wrap(bddCreator.makeNot(unwrap(f)));
  }

  @Override
  public Region makeAnd(Region f1, Region f2) {
    return wrap(bddCreator.makeAnd(unwrap(f1), unwrap(f2)));
  }

  @Override
  public Region makeOr(Region f1, Region f2) {
    return wrap(bddCreator.makeOr(unwrap(f1), unwrap(f2)));
  }

  @Override
  public Region makeEqual(Region f1, Region f2) {
    return wrap(bddCreator.makeEqual(unwrap(f1), unwrap(f2)));
  }

  @Override
  public Region makeUnequal(Region f1, Region f2) {
    return wrap(bddCreator.makeUnequal(unwrap(f1), unwrap(f2)));
  }

  @Override
  public Region makeIte(Region f1, Region f2, Region f3) {
    return wrap(bddCreator.makeIte(unwrap(f1), unwrap(f2), unwrap(f3)));
  }

  @Override
  public Region makeExists(Region f1, Region... f2) {
    BDD[] bddF2 = new BDD[f2.length];
    IntStream.range(0, f2.length).forEach(i -> bddF2[i] = unwrap(f2[i]));
    return wrap(bddCreator.makeExists(unwrap(f1), bddF2));
  }

  /**
   * Class for creating BDDs out of a formula. This class directly uses the BDD objects and their
   * manual reference counting, because for large formulas, the
   *
   * <p>All visit* methods from this class return methods that have not been ref'ed.
   */
  private class FormulaToRegionConverter implements BooleanFormulaVisitor<BDD>, AutoCloseable {

    private final Function<BooleanFormula, Region> atomToRegion;
    private final BooleanFormulaManager bfmgr;

    private final Map<BooleanFormula, BDD> cache = new HashMap<>();

    FormulaToRegionConverter(
        FormulaManagerView pFmgr, Function<BooleanFormula, Region> pAtomToRegion) {
      atomToRegion = pAtomToRegion;
      bfmgr = pFmgr.getBooleanFormulaManager();
    }

    @Override
    public void close() {
      cache.clear();
      PJBDDRegionManager.this.bddCreator.shutDown();
    }

    @Override
    public BDD visitConstant(boolean pB) {
      return pB ? bddCreator.makeTrue() : bddCreator.makeFalse();
    }

    @Override
    public BDD visitBoundVar(BooleanFormula pBooleanFormula, int pI) {
      throw new UnsupportedOperationException("Not yet implemented"); // TODO
    }

    @Override
    public BDD visitNot(BooleanFormula pBooleanFormula) {
      return bddCreator.makeNot(convert(pBooleanFormula));
    }

    @Override
    public BDD visitAnd(List<BooleanFormula> pList) {
      if (pList.isEmpty()) return bddCreator.makeFalse();

      BDD result = bddCreator.makeTrue();
      for (BooleanFormula bFormula : pList) {
        result = bddCreator.makeAnd(result, convert(bFormula));
      }
      return result;
    }

    @Override
    public BDD visitOr(List<BooleanFormula> pList) {
      if (pList.isEmpty()) return bddCreator.makeFalse();

      BDD result = bddCreator.makeFalse();
      for (BooleanFormula bFormula : pList) {
        result = bddCreator.makeOr(result, convert(bFormula));
      }
      return result;
    }

    @Override
    public BDD visitXor(BooleanFormula pBooleanFormula, BooleanFormula pBooleanFormula1) {
      return bddCreator.makeXor(convert(pBooleanFormula), convert(pBooleanFormula1));
    }

    @Override
    public BDD visitEquivalence(BooleanFormula pBooleanFormula, BooleanFormula pBooleanFormula1) {
      return bddCreator.makeEqual(convert(pBooleanFormula), convert(pBooleanFormula1));
    }

    @Override
    public BDD visitImplication(BooleanFormula pBooleanFormula, BooleanFormula pBooleanFormula1) {
      return bddCreator.makeImply(convert(pBooleanFormula), convert(pBooleanFormula1));
    }

    @Override
    public BDD visitIfThenElse(
        BooleanFormula pBooleanFormula1,
        BooleanFormula pBooleanFormula2,
        BooleanFormula pBooleanFormula3) {
      return bddCreator.makeIte(
          convert(pBooleanFormula1), convert(pBooleanFormula2), convert(pBooleanFormula3));
    }

    @Override
    public BDD visitQuantifier(
        Quantifier pQuantifier,
        BooleanFormula pBooleanFormula,
        List<Formula> pList,
        BooleanFormula pBooleanFormula1) {
      throw new UnsupportedOperationException("Not yet implemented"); // TODO
    }

    @Override
    public BDD visitAtom(
        BooleanFormula pBooleanFormula, FunctionDeclaration<BooleanFormula> pFunctionDeclaration) {
      return unwrap(atomToRegion.apply(pBooleanFormula));
    }

    // Convert one BooleanFormula (recursively)
    // and return a result that is also put in the cache.
    private BDD convert(BooleanFormula pOperand) {
      BDD operand = cache.get(pOperand);
      if (operand == null) {
        operand = bfmgr.visit(pOperand, this);
        cache.put(pOperand, operand);
      }
      return operand;
    }
  }

  private class RegionBuilder implements RegionCreator.RegionBuilder {

    private final List<BDD> cubes = new ArrayList<>();
    private BDD currentCube;

    @Override
    public void startNewConjunction() {
      checkState(currentCube == null);
      currentCube = bddCreator.makeTrue();
    }

    @Override
    public void addPositiveRegion(Region r) {
      checkState(currentCube != null);
      currentCube = bddCreator.makeAnd(currentCube, unwrap(r));
    }

    @Override
    public void addNegativeRegion(Region r) {
      checkState(currentCube != null);
      currentCube = bddCreator.makeAnd(currentCube, bddCreator.makeNot(unwrap(r)));
    }

    @Override
    public void finishConjunction() {
      checkState(currentCube != null);

      for (int i = 0; i < cubes.size(); i++) {
        BDD cubeAtI = cubes.get(i);

        if (cubeAtI == null) {
          cubes.set(i, currentCube);
          currentCube = null;
          return;
        } else {
          currentCube = bddCreator.makeOr(currentCube, cubeAtI);
          cubes.set(i, null);
        }
      }

      if (currentCube != null) {
        cubes.add(currentCube);
        currentCube = null;
      }
    }

    @Override
    public Region getResult() {
      checkState(currentCube == null);
      if (cubes.isEmpty()) {
        return falseFormula;
      } else {

        BDD[] clauses = cubes.stream().filter(bdd -> bdd != null).toArray(BDD[]::new);

        BDD result = bddCreator.makeTrue();

        for (BDD bdd : clauses) result = bddCreator.makeOr(result, bdd);

        cubes.clear();

        cubes.add(result);
        return wrap(result);
      }
    }

    @Override
    public void close() {
      checkState(currentCube == null);
      cubes.clear();
    }
  }

  @Options(prefix = "bdd.pjbdd")
  private static class BuildFromConfig {

    @Option(secure = true, description = "unique table's concurrency factor")
    @IntegerOption(min = 1)
    private int tableParallelism = 1000;

    @Option(secure = true, description = "initial variable count")
    @IntegerOption(min = 1)
    private int varCount = 10;

    @Option(secure = true, description = "increase factor for resizing tables")
    @IntegerOption(min = 1)
    private int increaseFactor = 1;

    @Option(secure = true, description = "initial size of the BDD node table.")
    @IntegerOption(min = 1)
    private int tableSize = 50000;

    @Option(secure = true, description = "size of the BDD cache.")
    @IntegerOption(min = 1)
    private int cacheSize = 10000;

    @Option(
        secure = true,
        description =
            "Number of worker threads, Runtime.getRuntime().availableProcessors() default")
    @IntegerOption(min = 1)
    private int threads = Runtime.getRuntime().availableProcessors();

    @Option(
        secure = true,
        description =
            "Which parallel bdd creator should be used? PJBDD only!"
                + "\n- serial-int: uses serial int-based algorithms with concurrent access"
                + "\n- serial: uses serial algorithms with concurrent access"
                + "\n- comp-fut: uses CompletableFuture based concurrent algorithms"
                + "\n- fork-join:  uses ForkJoin based concurrent algorithms",
        values = {"COMP-FUT", "FORK-JOIN", "SERIAL", "SERIAL-INT"},
        toUppercase = true)
    private String creator = "FORK-JOIN";

    @Option(
        secure = true,
        description =
            "Which uniquetable implementation should be used?"
                + "\n- array:  a concurrent resizing array"
                + "\n- map:    a concurrent hash map"
                + "\n- bucket: a concurrent hash bucket",
        values = {"ARRAY", "MAP", "BUCKET"},
        toUppercase = true)
    private String tableType = "ARRAY";

    private CreatorBuilder builder;

    private BuildFromConfig(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
      builder = CreatorBuilder.newBuilder();
    }

    private Creator makeCreator() {
      resolveProperties(builder);
      resolveTable(builder);
      switch (creator) {
        case "SERIAL-INT":
          return builder.makeSerialIntCreator();
        case "SERIAL":
          return builder.makeSerialApplyCreator();
        case "COMP-FUT":
          return builder.makeCompletableFutureApplyCreator();
        case "FORK-JOIN":
          return builder.makeConcurrentApplyCreator();
        default:
          return builder.makeConcurrentApplyCreator();
      }
    }

    private void resolveTable(CreatorBuilder pBuilder) {
      switch (tableType) {
        case "ARRAY":
          pBuilder.useConcurrentResizingArray();
          break;
        case "MAP":
          pBuilder.useConcurrentHashMap();
          break;
        case "BUCKET":
          pBuilder.useConcurrentHashBucket();
          break;
        default:
          pBuilder.useConcurrentResizingArray();
          break;
      }
    }

    private void resolveProperties(CreatorBuilder pBuilder) {
      pBuilder
          .setParallelism(tableParallelism)
          .setVarNum(varCount)
          .setSelectedCacheSize(cacheSize)
          .setNumWorkerThreads(threads)
          .setTableSize(tableSize)
          .setIncreaseFactor(increaseFactor);
    }
  }
}
