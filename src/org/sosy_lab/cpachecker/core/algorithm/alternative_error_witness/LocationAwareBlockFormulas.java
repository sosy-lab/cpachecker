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
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

  public static final String SMT = ".smt";
  public static final String PREFIX_FILENAME = "formula_";
  public static final String SUFFIX_BRANCHING_FORMULA = "branching";
  public static final String PATH = "path";
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

  public List<CFANode> getLocationList() {
    return locationList;
  }

  public boolean dumpToFolder(Path dir, LogManager logger, FormulaManagerView fmgr) {
    // As there mgth be more than one block formula for abstraction nodes, we used indices
    Map<CFANode, Integer> nodeNamesToIndices = new HashMap<>();
    List<String> fileOrdering = new ArrayList<>();

    this.locationList.stream().forEach(l -> nodeNamesToIndices.putIfAbsent(l, 0));
    try {
      ImmutableList<BooleanFormula> formulaList = super.getFormulas();
      for (int cntFormula = 0; cntFormula < formulaList.size(); cntFormula++) {
        CFANode loc = locationList.get(cntFormula);
        Integer index = nodeNamesToIndices.get(loc);
        Path f =
            Files.createFile(
                Paths.get(
                    dir.toAbsolutePath()
                        + "/"
                        + PREFIX_FILENAME
                        + loc.toString()
                        + String.format("§%d", index)
                        + SMT));
        fmgr.dumpFormulaToFile(formulaList.get(cntFormula), f);
        fileOrdering.add(loc.toString() + String.format("§%d", index));
        nodeNamesToIndices.replace(loc, index + 1);
      }
      if (super.hasBranchingFormula()) {
        Path f =
            Files.createFile(
                Paths.get(
                    dir.toAbsolutePath() + "/" + PREFIX_FILENAME + SUFFIX_BRANCHING_FORMULA + SMT));
        fmgr.dumpFormulaToFile(super.getBranchingFormula(), f);
      }
      // Dump the ordering of the locations:
      Path f = Files.createFile(Paths.get(dir.toAbsolutePath() + "/" +  PATH));

      Files.write(
          f,
          String.join(
                  System.lineSeparator(),
                 fileOrdering)
              .getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.WRITE);

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

  public static LocationAwareBlockFormulas constructFromDump(
      Path dir, FormulaManagerView fmgr, CFA pCfa) throws IllegalArgumentException, IOException {
    Path f = Paths.get(dir.toAbsolutePath() + "/" + PATH);
    List<String> nodeOrderingOnPath = Files.readAllLines(f);

    CFANode[] nodes = new CFANode[nodeOrderingOnPath.size()];
    BooleanFormula[] formulas = new BooleanFormula[nodeOrderingOnPath.size()];

    Optional<BooleanFormula> branchingFormula = Optional.empty();
    Map<String, CFANode> nodeNamesToNodes = new HashMap<>();
    pCfa.getAllNodes().stream().forEach(n -> nodeNamesToNodes.put(n.toString(), n));
    for (File file : dir.toFile().listFiles()) {
      String name = file.getName();
      if (name.equals(PREFIX_FILENAME + SUFFIX_BRANCHING_FORMULA + SMT)) {
        branchingFormula = Optional.of(fmgr.parse(Files.readString(file.toPath())));
      } else if (name.startsWith(PREFIX_FILENAME)) {
        BooleanFormula formula = fmgr.parse(Files.readString(file.toPath()));
        String nodenumber =
            name.substring(name.lastIndexOf("N"), name.length() - 4); // -4 to remove ".smt"
        int position = nodeOrderingOnPath.indexOf(nodenumber);
        formulas[position] = formula;
        nodes[position] =
            nodeNamesToNodes.get(nodenumber.substring(0, nodenumber.lastIndexOf("§")));
      }
    }
    LocationAwareBlockFormulas loc =
        new LocationAwareBlockFormulas(Lists.newArrayList(formulas), Lists.newArrayList(nodes));
    if (branchingFormula.isPresent()) {
      return loc.withBranchingFormula(branchingFormula.get());
    }
    return loc;
  }
}
