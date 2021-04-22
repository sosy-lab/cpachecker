// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.alternative_error_witness;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class LocationAwareBlockFormulas extends BlockFormulas {

  private static final String SMT = ".smt";
  public static final String PREFIX_FILENAME = "formula_";
  public  static final String SUFFIX_BRANCHING_FORMULA = "branching";
  private List<CFANode> locationList;


  public LocationAwareBlockFormulas(List<BooleanFormula> pFormulas, List<CFANode> pList) {
    super(pFormulas);
    this.locationList = pList;
    assert pList.size() == pFormulas.size();
  }

  public LocationAwareBlockFormulas(
      ImmutableList<BooleanFormula> pFormulas,
      BooleanFormula pBranchingFormula,
      List<CFANode> pLocationList) {
    super(pFormulas, pBranchingFormula);
    this.locationList = pLocationList;
  }

  @Override
  public LocationAwareBlockFormulas withBranchingFormula(BooleanFormula pBranchingFormula) {
    checkState(super.getBranchingFormula() == null);
    return new LocationAwareBlockFormulas(
        super.getFormulas(), pBranchingFormula, this.locationList);
  }

  public boolean dumpToFolder(Path dir, LogManager logger, FormulaManagerView fmgr) {

    try {

        ImmutableList<BooleanFormula> formulaList = super.getFormulas();
        for (int cntFormula = 0; cntFormula < formulaList.size(); cntFormula++) {
        Path f =
            Files.createFile(
                Paths.get(
                    dir.toAbsolutePath()
                        + "/"
                        + PREFIX_FILENAME
                        + locationList.get(cntFormula).toString()
                        + SMT));
          fmgr.dumpFormulaToFile(formulaList.get(cntFormula), f);
        }
      if (super.hasBranchingFormula()) {
        Path f =
            Files.createFile(
                Paths.get(
                    dir.toAbsolutePath()
                        + "/"
                        + PREFIX_FILENAME
                        + SUFFIX_BRANCHING_FORMULA
                        + SMT));
        fmgr.dumpFormulaToFile(super.getBranchingFormula(), f);
        }

    } catch (IOException e) {
      logger.log(
          Level.SEVERE,
          String.format(
              "An error occured while stroring the output-files. Errro: %s",
              Throwables.getStackTraceAsString(e)));
      return false;
    }
    return true;
  }

  public LocationAwareBlockFormulas constructFromDump(Path dir, FormulaManagerView fmgr, CFA pCfa)
      throws IllegalArgumentException, IOException {
    List<CFANode> nodes = new ArrayList<>();
    List<BooleanFormula> formulas = new ArrayList<>();
    Optional<BooleanFormula> branchingFormula = Optional.empty();
    Map<String, CFANode> nodeNamesToNodes = new HashMap<>();
    pCfa.getAllNodes().stream().forEach(n -> nodeNamesToNodes.put(n.toString(), n));
    for (File file : dir.toFile().listFiles()) {
      String name = file.getName();
      if (name.equals(PREFIX_FILENAME + SUFFIX_BRANCHING_FORMULA + SMT)) {
        branchingFormula = Optional.of(fmgr.parse(Files.readString(file.toPath())));
      } else {
        BooleanFormula formula = fmgr.parse(Files.readString(file.toPath()));
        String nodenumber =
            name.substring(name.lastIndexOf("N"), name.length() - 4); // -4 to remove ".smt"
        formulas.add(formula);
        nodes.add(nodeNamesToNodes.get(nodenumber));
      }
    }
    LocationAwareBlockFormulas loc = new LocationAwareBlockFormulas(formulas, nodes);
    if (branchingFormula.isPresent()) {
      return loc.withBranchingFormula(branchingFormula.get());
    }
    return loc;
  }
}
