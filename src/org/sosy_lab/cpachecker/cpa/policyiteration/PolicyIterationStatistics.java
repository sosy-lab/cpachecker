package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

@Options(prefix="cpa.stator.policy")
public class PolicyIterationStatistics implements Statistics {

  final Multiset<Pair<CFANode, Template>> templateUpdateCounter
      = HashMultiset.create();

  final Multiset<CFANode> abstractMergeCounter = HashMultiset.create();

  private final Timer valueDeterminationTimer = new Timer();
  private final Timer abstractionTimer = new Timer();
  private final Timer checkSATTimer = new Timer();
  final Timer slicingTimer = new Timer();
  final Timer optTimer = new Timer();
  final Timer checkIndependenceTimer = new Timer();

  public void startCheckSATTimer() {
    checkSATTimer.start();
  }

  public void stopCheckSATTimer() {
    checkSATTimer.stop();
  }

  public void startOPTTimer() {
    optTimer.start();
  }

  public void stopOPTTimer() {
    optTimer.stop();
  }

  public void startAbstractionTimer() {
    abstractionTimer.start();
  }

  public void stopAbstractionTimer() {
    abstractionTimer.stop();
  }

  public void startValueDeterminationTimer() {
    valueDeterminationTimer.start();
  }

  public void stopValueDeterminationTimer() {
    valueDeterminationTimer.stop();
  }

  public PolicyIterationStatistics(Configuration config)
      throws InvalidConfigurationException {
    config.inject(this, PolicyIterationStatistics.class);
  }

  @Override
  public void printStatistics(
      PrintStream out, CPAcheckerResult.Result result, ReachedSet reached) {

    printTimer(out, valueDeterminationTimer, "value determination");
    out.printf("Number of val. det. calls: %d%n",
        valueDeterminationTimer.getNumberOfIntervals());
    printTimer(out, abstractionTimer, "abstraction");
    out.printf("Number of abstractions performed: %d%n",
        abstractionTimer.getNumberOfIntervals());
    printTimer(out, optTimer, "optimization");
    out.printf("Number of optimization queries sent: %d%n",
        optTimer.getNumberOfIntervals());
    printTimer(out, checkSATTimer, "checking satisfiability");
    if (slicingTimer.getNumberOfIntervals() > 0) {
      printTimer(out, slicingTimer, "checking inductiveness in formula slicing");
    }
    printTimer(out, checkIndependenceTimer, "checking independence");
    out.printf("Time spent in %s: %s (Max: %s)%n",
        "SMT solver",
        TimeSpan.sum(
            optTimer.getSumTime(),
            checkSATTimer.getSumTime()
        ).formatAs(TimeUnit.SECONDS),
        optTimer.getMaxTime().formatAs(TimeUnit.SECONDS));

    UpdateStats<?> updateStats =
        getUpdateStats(PolicyAbstractedState.getUpdateCounter());
    UpdateStats<?> templateUpdateStats = getUpdateStats(templateUpdateCounter);
    UpdateStats<?> mergeUpdateStats = getUpdateStats(abstractMergeCounter);

    printStats(out, updateStats, "abstractions on a given location");
    printStats(out, templateUpdateStats, "updates for given template on a given location");
    printStats(out, mergeUpdateStats, "merges of abstract states on a given location");
  }

  private void printStats(PrintStream out, UpdateStats<?> stats, String description) {
    out.printf("Max number of %s: %d, for object: %s%n",
        description, stats.max, stats.maxObject);
    out.printf("Min number of %s: %d, for object: %s%n",
        description, stats.min, stats.minObject);
    out.printf("Avg number of %s: %.1f%n",
        description, stats.avg);

  }

  private static class UpdateStats<T> {
    final T maxObject;
    final T minObject;
    final int max, min;
    final double avg;
    UpdateStats(int pMax, int pMin, double pAvg, T pMaxObject, T pMinObject) {
      max = pMax;
      min = pMin;
      avg = pAvg;
      maxObject = pMaxObject;
      minObject = pMinObject;
    }
  }

  private <T> UpdateStats<T> getUpdateStats(Multiset<T> updateStats) {
    int max = Integer.MIN_VALUE;
    int min = Integer.MAX_VALUE;
    T maxObject = null;
    T minObject = null;
    double sum = 0;
    for (T l : updateStats.elementSet()) {
      int count = updateStats.count(l);
      if (count > max) {
        max = count;
        maxObject = l;
      }
      if (count < min) {
        min = count;
        minObject = l;
      }
      sum += count;
    }
    double avg = sum / updateStats.elementSet().size();
    return new UpdateStats<>(max, min, avg, maxObject, minObject);
  }

  private void printTimer(PrintStream out, Timer t, String name) {
    out.printf("Time spent in %s: %s (Max: %s), (Avg: %s)%n",
        name, t, t.getMaxTime().formatAs(TimeUnit.SECONDS),
        t.getAvgTime().formatAs(TimeUnit.SECONDS));
  }

  @Override
  public String getName() {
    return "PolicyIterationCPA";
  }
}
