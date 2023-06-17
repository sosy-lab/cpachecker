// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.parallelRangedConditions;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.rangedconditions.CFAPath;

abstract class Heuristic {
  public static Heuristic getHeuristic(
      Type heuristicType, CFA pCfa, Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    return switch (heuristicType) {
      case PATHS_FILE -> new PathsFileHeuristic(pCfa, pConfig);
      case FIRST_BRANCHES -> new FirstBranchesHeuristic(pCfa, pConfig);
      case LOOPS -> new LoopHeuristic(pCfa, pLogger);
      case RANDOM -> new RandomPathsHeuristic(pCfa, pConfig, pLogger);
    };
  }

  public abstract List<CFAPath> generatePaths() throws InvalidConfigurationException;

  public enum Type {
    PATHS_FILE,
    FIRST_BRANCHES,
    LOOPS,
    RANDOM
  }

  @Options(prefix = "parallelRangedConditionsAlgorithm.pathsFileHeuristic")
  private static class PathsFileHeuristic extends Heuristic {

    private final CFA cfa;

    @Option(
        description = "Path generation heuristic to use for Parallel Ranged Conditions.",
        required = true,
        name = "file")
    @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
    private Path pathsFile;

    private PathsFileHeuristic(CFA pCfa, Configuration config)
        throws InvalidConfigurationException {
      config.inject(this);
      cfa = pCfa;
    }

    @Override
    public List<CFAPath> generatePaths() throws InvalidConfigurationException {
      String fileContent;
      try {
        fileContent = Files.readString(pathsFile);
      } catch (IOException pE) {
        throw new InvalidConfigurationException(
            "Can not read paths file " + pathsFile.getFileName());
      }

      List<String> pathStrings = ImmutableList.copyOf(fileContent.split(System.lineSeparator()));
      return pathStrings.stream()
          .map(pathString -> CFAPath.fromString(cfa, pathString))
          .collect(ImmutableList.toImmutableList());
    }
  }

  @Options(prefix = "parallelRangedConditionsAlgorithm.firstBranchesHeuristic")
  private static class FirstBranchesHeuristic extends Heuristic {
    private final CFA cfa;

    @Option(
        description =
            "Used for \"FIRST_BRANCHES\" heuristic, will generate paths for all branches up to this"
                + " depth.",
        required = true)
    @IntegerOption(min = 1)
    private int branchDepth;

    public FirstBranchesHeuristic(CFA pCfa, Configuration config)
        throws InvalidConfigurationException {
      config.inject(this);
      cfa = pCfa;
    }

    @Override
    public List<CFAPath> generatePaths() {
      List<CFAPath> paths =
          recursiveFirstBranches(new CFAPath(ImmutableList.of(cfa.getMainFunction())), branchDepth);

      return paths.stream().sorted().collect(ImmutableList.toImmutableList());
    }

    private List<CFAPath> recursiveFirstBranches(CFAPath pCurrentPath, int pRemainingBranchDepth) {
      CFANode currentNode = pCurrentPath.getLast();

      if (currentNode.getNumLeavingEdges() == 0) {
        return ImmutableList.of();
      }
      if (currentNode.getNumLeavingEdges() == 1) {
        pCurrentPath.add(currentNode.getLeavingEdge(0).getSuccessor());
        return recursiveFirstBranches(pCurrentPath, pRemainingBranchDepth);
      }

      List<CFANode> successors = new ArrayList<>(currentNode.getNumLeavingEdges());
      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        successors.add(currentNode.getLeavingEdge(i).getSuccessor());
      }

      ImmutableList.Builder<CFAPath> generated = new ImmutableList.Builder<>();

      for (CFANode successor : successors) {
        CFAPath nextPath = pCurrentPath.copy();
        nextPath.add(successor);
        if (successor != Collections.min(successors)) {
          generated.add(nextPath.copy());
        }
        if (pRemainingBranchDepth > 1) {
          generated.addAll(recursiveFirstBranches(nextPath, pRemainingBranchDepth - 1));
        }
      }
      return generated.build();
    }
  }

  @Options(prefix = "parallelRangedConditionsAlgorithm.randomPathsHeuristic")
  private static class RandomPathsHeuristic extends Heuristic {
    private final CFA cfa;
    private final Random rand;
    private final LogManager logger;

    @Option(
        description =
            "The number of paths, the random heuristic should generate. The number of ranges for"
                + " the ranged conditions algorithm is one more than this.",
        required = true)
    @IntegerOption(min = 1)
    private int numPaths;

    @Option(description = "Seed for the random number generator")
    private Long seed = 0L;

    @Option(
        description =
            "Maximum length of randomly generated paths. The actual length may be shorter, when the"
                + " path encounters the end of the program.")
    @IntegerOption(min = 1)
    private int maxLength = 100;

    private RandomPathsHeuristic(CFA pCfa, Configuration config, LogManager pLogger)
        throws InvalidConfigurationException {
      config.inject(this);
      cfa = pCfa;
      this.logger = pLogger;
      rand = new Random(seed);
    }

    @Override
    public List<CFAPath> generatePaths() {
      Set<CFAPath> paths = new HashSet<>(numPaths);
      for (int i = 0; i < numPaths * 2; i++) {
        paths.add(generateSinglePath(cfa.getMainFunction(), new CFAPath()));
        if (paths.size() == numPaths) {
          break;
        }
      }
      if (paths.size() != numPaths) {
        logger.log(
            Level.WARNING,
            String.format(
                "Could not generate %s different paths (this may be due to bad luck), continuing"
                    + " with %s paths instead.",
                numPaths, paths.size()));
      }
      List<CFAPath> sortedPaths = new ArrayList<>(paths);
      Collections.sort(sortedPaths);
      return sortedPaths;
    }

    private CFAPath generateSinglePath(CFANode currentNode, CFAPath currentPath) {
      currentPath.add(currentNode);
      int numLeaving = currentNode.getNumLeavingEdges();
      if (numLeaving == 0 || currentPath.size() == maxLength) {
        return currentPath;
      } else if (numLeaving == 1) {
        return generateSinglePath(currentNode.getLeavingEdge(0).getSuccessor(), currentPath);
      } else {
        return generateSinglePath(
            currentNode.getLeavingEdge(rand.nextInt(numLeaving)).getSuccessor(), currentPath);
      }
    }
  }

  private static class LoopHeuristic extends Heuristic {
    private final CFA cfa;

    private final Set<CFANode> loopNodes = new HashSet<>();
    private final LogManager logger;

    private LoopHeuristic(CFA pCfa, LogManager pLogger) {
      logger = pLogger;
      cfa = pCfa;
    }

    @Override
    public List<CFAPath> generatePaths() throws InvalidConfigurationException {
      Optional<LoopStructure> loopHeadsOptional = cfa.getLoopStructure();
      if (loopHeadsOptional.isEmpty() || loopHeadsOptional.get().getAllLoops().isEmpty()) {
        throw new InvalidConfigurationException(
            "Loop Heuristic selected but CFA does no loop structures present in CFA.");
      }
      ImmutableCollection<Loop> loops = loopHeadsOptional.get().getAllLoops();
      for (Loop loop : loops) {
        loopNodes.addAll(loop.getLoopNodes());
      }

      Set<CFAPath> limitPaths = new HashSet<>();

      for (Loop loop : loops) {
        Set<CFAPath> incommingPaths = new HashSet<>();
        for (CFAEdge entryEdge : loop.getIncomingEdges()) {
          Set<CFAPath> pathsForEdge = getPathsWithoutLoop(entryEdge.getPredecessor());
          pathsForEdge.forEach(path -> path.add(entryEdge.getSuccessor()));
          incommingPaths.addAll(pathsForEdge);
        }

        if (incommingPaths.isEmpty()) {
          logger.log(
              Level.WARNING,
              "Found loop that is not reachable without traversing other loops. Not generating"
                  + " ranges for this."
                  + loop);
          continue;
        }

        limitPaths.add(getLowerLimitPath(loop, Collections.min(incommingPaths)));

        Optional<CFAPath> upperLimit = getUpperLimitPath(loop, Collections.max(incommingPaths));
        upperLimit.ifPresent(limitPaths::add);
      }

      return limitPaths.stream().sorted().collect(ImmutableList.toImmutableList());
    }

    private CFAPath getLowerLimitPath(Loop loop, CFAPath path) {
      Set<CFANode> visited = new HashSet<>();
      CFANode currentNode = path.getLast();
      CFAPath localPath = new CFAPath(path);
      while (true) {
        if (!loop.getLoopNodes().contains(currentNode)) {
          break;
        } else {
          if (visited.contains(currentNode)) {
            logger.log(Level.INFO, "minPath pre " + localPath);
            // The smallest path through the loop does not exit it. Return the original path, which
            // is the biggest path smaller than all paths that contain the loop.
            return path;
          }
          visited.add(currentNode);
          Set<CFANode> successorNodes = new HashSet<>();
          for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
            successorNodes.add(currentNode.getLeavingEdge(i).getSuccessor());
          }
          currentNode = Collections.min(successorNodes);
          localPath.add(currentNode);
        }
      }
      while (currentNode.getNumLeavingEdges() > 0) {
        Set<CFANode> successors = new HashSet<>();
        for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
          successors.add(currentNode.getLeavingEdge(i).getSuccessor());
        }
        if (loopNodes.contains(Collections.max(successors))) {
          break;
        }
        currentNode = Collections.max(successors);
        localPath.add(currentNode);
      }
      logger.log(Level.INFO, "minPath " + localPath);
      return localPath;
    }

    private Optional<CFAPath> getUpperLimitPath(Loop loop, CFAPath path) {
      Set<CFANode> visited = new HashSet<>();
      CFANode currentNode = path.getLast();
      CFAPath localPath = new CFAPath(path);
      while (!visited.contains(currentNode)) {
        if (!loop.getLoopNodes().contains(currentNode)) {
          return Optional.of(localPath);
        }
        visited.add(currentNode);
        Set<CFANode> successorNodes = new HashSet<>();
        for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
          successorNodes.add(currentNode.getLeavingEdge(i).getSuccessor());
        }
        currentNode = Collections.max(successorNodes);
        localPath.add(currentNode);
      }

      // The largest path within the loop does not exit it. We need to backtrack the path leading to
      // the loop to find a path, that is larger.
      for (int i = path.size() - 1; i > 0; i--) {
        CFANode current = path.get(i);
        CFANode predecessor = path.get(i - 1);
        if (predecessor.getNumLeavingEdges() == 1) {
          // The predecessor to node i has only one succesor which is node i itself.
          continue;
        }

        Set<CFANode> candidates = new HashSet<>();
        for (int j = 0; j < predecessor.getNumLeavingEdges(); j++) {
          CFANode candidate = predecessor.getLeavingEdge(j).getSuccessor();
          if (candidate.compareTo(current) > 0) {
            candidates.add(candidate);
          }
        }
        if (!candidates.isEmpty()) {
          CFAPath limitPath = new CFAPath(path.subList(0, i - 1));
          limitPath.add(Collections.min(candidates));
          return Optional.of(limitPath);
        }
      }
      return Optional.empty();
    }

    private Set<CFAPath> getPathsWithoutLoop(CFANode targetNode) {
      if (targetNode == cfa.getMainFunction()) {
        return ImmutableSet.of(new CFAPath(ImmutableList.of(targetNode)));
      }
      Set<CFAPath> paths = new HashSet<>();
      for (int i = 0; i < targetNode.getNumEnteringEdges(); i++) {
        CFAEdge enteringEdge = targetNode.getEnteringEdge(i);
        CFANode predecessor = enteringEdge.getPredecessor();
        if (loopNodes.contains(predecessor)) {
          // ignore paths leading to this loop, which traverse through other loops
          continue;
        }
        paths.addAll(getPathsWithoutLoop(predecessor));
      }
      paths.forEach(path -> path.add(targetNode));
      return paths;
    }
  }
}
