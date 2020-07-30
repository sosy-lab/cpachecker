// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.acsl.WitnessInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateGenerator;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator.KInductionInvariantGeneratorOptions;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

public class WitnessToACSLAlgorithm implements Algorithm {

  private final Configuration config;
  private final Specification specification;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownManager shutdownManager;
  private final KInductionInvariantGeneratorOptions kindOptions;
  private final TargetLocationProvider targetLocationProvider;
  private final ToCExpressionVisitor toCExpressionVisitor;
  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public WitnessToACSLAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    config = pConfig;
    specification = pSpecification;
    logger = pLogger;
    cfa = pCfa;
    shutdownManager = ShutdownManager.createWithParent(pShutdownNotifier);
    kindOptions = new KInductionInvariantGeneratorOptions();
    config.inject(kindOptions);
    targetLocationProvider = new CachingTargetLocationProvider(pShutdownNotifier, logger, cfa);
    toCExpressionVisitor = new ToCExpressionVisitor(cfa.getMachineModel(), logger);
    binaryExpressionBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    final CandidateGenerator gen;
    final Set<String> files = new LinkedHashSet<>();
    Map<CFANode, CExpression> invMap = new HashMap<>();
    try {
      gen =
          KInductionInvariantGenerator.getCandidateInvariants(
              kindOptions,
              config,
              logger,
              cfa,
              shutdownManager,
              targetLocationProvider,
              specification);
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Invalid Configuration while analyzing witness", e);
    }
    // this is important because otherwise the candidates will not be displayed!
    gen.produceMoreCandidates();
    List<ExpressionTreeLocationInvariant> cands = new ArrayList<>();
    for (CandidateInvariant inv : gen) {
      if (inv instanceof ExpressionTreeLocationInvariant) {
        cands.add((ExpressionTreeLocationInvariant) inv);
      }
    }

    // Extract invariants as CExpressions and nodes
    for (ExpressionTreeLocationInvariant c : cands) {
      CFANode loc = c.getLocation();
      if(loc instanceof FunctionExitNode) {
        continue;
      }
      Optional<? extends AAstNode> astNodeOptional = loc.getLeavingEdge(0).getRawAST();

      @SuppressWarnings("unchecked")
      CExpression exp =
          ((ExpressionTree<AExpression>) (Object) c.asExpressionTree())
              .accept(toCExpressionVisitor);
      //TODO: Do this only if astNodeOptional is present?
      invMap.put(loc, exp);
      if (astNodeOptional.isPresent()) {
        AAstNode astNode = astNodeOptional.get();
        FileLocation fileLoc = astNode.getFileLocation();
        files.add(fileLoc.getFileName());
      } else {
        //TODO
      }
    }

    for (String file : files) {
      //Sort invariants by location
      List<WitnessInvariant> sortedInvariants = new ArrayList<>(invMap.size());
      for (Entry<CFANode, CExpression> entry : invMap.entrySet()) {
        CFANode node = entry.getKey();
        if(!node.getFunction().getFileLocation().getFileName().equals(file)) {
          //Current invariant belongs to another program
          continue;
        }
        int location = 0;
        assert node.getNumLeavingEdges() > 0;
        for (int i = 0; i < node.getNumLeavingEdges(); i++) {
          CFAEdge edge = node.getLeavingEdge(i);
          while(edge.getFileLocation().equals(FileLocation.DUMMY) ||
              edge.getDescription().contains("__CPAchecker_TMP")) {
            if(!(edge.getSuccessor().getNumLeavingEdges() > 0)) {
              break;
            }
            edge = edge.getSuccessor().getLeavingEdge(0);
          }
          if(edge.getLineNumber() > 0) {
            location = edge.getLineNumber();
            break;
          }
        }
        boolean added = false;
        boolean merged = false;
        WitnessInvariant currentInvariant =
            new WitnessInvariant(node, entry.getValue(), binaryExpressionBuilder, location);
        for (int i = 0; i < sortedInvariants.size(); i++) {
          WitnessInvariant other = sortedInvariants.get(i);
          int otherLocation = other.getLocation();
          if (location < otherLocation) {
            sortedInvariants.add(i, currentInvariant);
            added = true;
            break;
          }
          if(location == otherLocation) {
            other.mergeWith(currentInvariant);
            merged = true;
            break;
          }
        }
        if (!added && !merged) {
          sortedInvariants.add(currentInvariant);
        }
      }

      String fileContent = "";
      try {
        fileContent = Files.asCharSource(new File(file), Charsets.UTF_8).read();
      } catch (IOException pE) {
        logger.logfUserException(Level.SEVERE, pE, "Could not read file %s", file);
      }

      Iterator<WitnessInvariant> iterator = sortedInvariants.iterator();
      WitnessInvariant currentInvariant = iterator.next();

      List<String> output = new ArrayList<>();

      while (currentInvariant != null && currentInvariant.getLocation() == 0) {
        String annotation = makeACSLAnnotation(currentInvariant);
        output.add(annotation);
        if (iterator.hasNext()) {
          currentInvariant = iterator.next();
        } else {
          currentInvariant = null;
        }
      }

      List<String> splitContent = Splitter.onPattern("\\r?\\n").splitToList(fileContent);
      for (int i = 0; i < splitContent.size(); i++) {
        assert currentInvariant == null || currentInvariant.getLocation() > i;
        while (currentInvariant != null && currentInvariant.getLocation() == i + 1) {
          String annotation = makeACSLAnnotation(currentInvariant);
          String indentation = getIndentation(splitContent.get(i));
          output.add(indentation.concat(annotation));
          if (iterator.hasNext()) {
            currentInvariant = iterator.next();
          } else {
            currentInvariant = null;
          }
        }
        output.add(splitContent.get(i));
      }
      try {
        writeToFile(file, output);
      } catch (IOException pE) {
        logger.logfUserException(Level.SEVERE, pE, "Could not write annotations for file %s", file);
      }
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private void writeToFile(String pathToOriginalFile, List<String> newContent) throws IOException{
    Path path = Path.of(pathToOriginalFile);
    Path directory = path.getParent();
    assert directory != null;
    Path oldFileName = path.getFileName();
    String newFileName = makeNameForAnnotatedFile(oldFileName.toString());

    File outFile = new File(Path.of(directory.toString(), newFileName).toUri());
    assert outFile.createNewFile() : String.format("File %s already exists!", outFile);
    try (Writer out = Files.newWriter(outFile, StandardCharsets.UTF_8)) {
      for(String line : newContent) {
        out.append(line.concat("\n"));
      }
      out.flush();
    }
  }

  /**
   * Creates a name for the file containing the annotated code based on the original's name. The new
   * name has the prefix "annotated" and ends in a timestamp.
   *
   * @param oldFileName The name of the original source file.
   * @return A name based on the given file name.
   */
  private String makeNameForAnnotatedFile(String oldFileName) {
    int indexOfFirstPeriod = oldFileName.indexOf('.');
    String nameWithoutExtension = oldFileName.substring(0, indexOfFirstPeriod);
    String extension = oldFileName.substring(indexOfFirstPeriod);
    String timestamp =
        LocalDateTime.now(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
    return "annotated_".concat(nameWithoutExtension).concat(timestamp).concat(extension);
  }

  /**
   * Returns a String containing only space chars the same length as the given parameters
   * indentation. Note that length refers to the length of the printed whitespace and not
   * necessarily the value returned by <code>String.length()</code>.
   *
   * @param correctlyIndented A String of which the indentation should be matched.
   * @return the longest whitespace-only prefix of the given String.
   */
  private String getIndentation(String correctlyIndented) {
    String indentation = null;
    for(int i = 0; i < correctlyIndented.length(); i++) {
      if (!Character.isSpaceChar(correctlyIndented.charAt(i))) {
        indentation = correctlyIndented.substring(0, i);
        break;
      }
    }
    return indentation == null ? correctlyIndented : indentation;
  }

  //TODO: actually transform invariant to valid ACSL annotation
  private String makeACSLAnnotation(WitnessInvariant inv) {
    if(inv.isAtLoopStart()) {
     return "/*@ loop invariant " + inv.getExpression().toASTString() + "; */";
    } else if (inv.isAtFunctionEntry()) {
      return "/*@ ensures " + inv.getExpression().toASTString() + "; */";
    } else {
      return "/*@ assert " + inv.getExpression().toASTString() + "; */";
    }
  }
}
