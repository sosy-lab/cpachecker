// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

@SuppressWarnings("all")
@Options(prefix = "bmc.kinduction.reuse")
public class WitnessToInitialInvariantsConverter {

  private final Configuration config;
  private final LogManager logger;

  public WitnessToInitialInvariantsConverter(final Configuration pConfig, final LogManager pLogger)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    config.inject(this);
  }

  public ImmutableSet<CandidateInvariant> WitnessParser(Path pfilename, Solver pSolver)
      throws JsonParseException, JsonMappingException, IOException {
    FormulaManagerView formulaManager = pSolver.getFormulaManager();
    File yamlWitness = pfilename.toFile();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    JavaType entryType =
        mapper.getTypeFactory().constructCollectionType(List.class, InvariantSetEntry.class);
    List<InvariantSetEntry> entries = mapper.readValue(yamlWitness, entryType);
    ImmutableSet.Builder<CandidateInvariant> invariants = ImmutableSet.builder();
    for (InvariantSetEntry e : entries) {
      while (!e.content.isEmpty()) {
        InvariantEntry i = (InvariantEntry) e.content.remove(0);
        logger.log(Level.INFO, i.getLocation(), i.getValue(), pSolver, formulaManager);
        //        invariants.add(SingleLocationFormulaInvariant.makeLocationInvariant(
        //            i.getLocation(),
        //            i.getValue(), // .getSymbolicAtom(),
        //            formulaManager));
      }
    }

    return invariants.build();
  }
}
