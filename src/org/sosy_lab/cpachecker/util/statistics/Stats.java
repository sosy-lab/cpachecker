package org.sosy_lab.cpachecker.util.statistics;

import java.io.Closeable;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.statistics.Aggregateables.AggregationInt;
import org.sosy_lab.cpachecker.util.statistics.Aggregateables.AggregationMilliSec;
import org.sosy_lab.cpachecker.util.statistics.Aggregateables.AggregationSet;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.StatCpuTimer;
import org.sosy_lab.cpachecker.util.statistics.interfaces.Aggregateable;
import org.sosy_lab.cpachecker.util.statistics.interfaces.Context;
import org.sosy_lab.cpachecker.util.statistics.interfaces.ContextKey;
import org.sosy_lab.cpachecker.util.statistics.interfaces.NoStatisticsException;
import org.sosy_lab.cpachecker.util.statistics.interfaces.TimeMeasurementListener;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public final class Stats {

  // Aggregate the statistical data into these contexts:
  private static Multimap<Thread, Context> rootContexts = HashMultimap.create();
  private static Multimap<Thread, Context> activeContexts = HashMultimap.create();
  private static Map<ContextKey, StatisticsContext> contexts = Maps.newHashMap();
  private static Multimap<Object, Context> valuesInContexts = HashMultimap.create();

  public synchronized static void reset() {
    contexts.clear();
    rootContexts.clear();
    activeContexts.clear();
    valuesInContexts.clear();
  }

  private static class Key implements ContextKey {
    private final Optional<Context> parentContext;
    private final Object classIdentifier;
    private final Thread thread;

    public Key(Optional<Context> pParentContext, Object pClassIdentifier, Thread pThread) {
      parentContext = pParentContext;
      classIdentifier = pClassIdentifier;
      thread = pThread;
    }

    @Override
    public String toString() {
      return String.format("%d %s %s", thread.getId(), classIdentifier.toString(), parentContext.toString());
    }

    /* (non-Javadoc)
     * @see org.sosy_lab.cpachecker.util.statistics.ContextKeyIf#getIdentifier()
     */
    @Override
    public Object getIdentifier() {
      return classIdentifier;
    }

    /* (non-Javadoc)
     * @see org.sosy_lab.cpachecker.util.statistics.ContextKeyIf#getParentContext()
     */
    @Override
    public Optional<Context> getParentContext() {
      return parentContext;
    }

    /* (non-Javadoc)
     * @see org.sosy_lab.cpachecker.util.statistics.ContextKeyIf#getThread()
     */
    @Override
    public Thread getThread() {
      return thread;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((classIdentifier == null) ? 0 : classIdentifier.hashCode());
      result = prime * result + ((parentContext == null) ? 0 : parentContext.hashCode());
      result = prime * result + ((thread == null) ? 0 : thread.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Key other = (Key) obj;
      if (classIdentifier == null) {
        if (other.classIdentifier != null) {
          return false;
        }
      } else if (!classIdentifier.equals(other.classIdentifier)) {
        return false;
      }
      if (parentContext == null) {
        if (other.parentContext != null) {
          return false;
        }
      } else if (!parentContext.equals(other.parentContext)) {
        return false;
      }
      if (thread == null) {
        if (other.thread != null) {
          return false;
        }
      } else if (!thread.equals(other.thread)) {
        return false;
      }
      return true;
    }

  }

  public static class Contexts implements Closeable {

    private ImmutableSet<Context> contexts;

    public Contexts(Set<? extends Context> pContexts) {
      contexts = ImmutableSet.copyOf(pContexts);
    }

    @Override
    public void close() {
      for (Context x: this.contexts) {
        x.close();
      }
    }
  }

  private static class StatisticsContext implements Context {

    private transient final Map<Object, Context> childContexts = Maps.newHashMap();
    private final Map<Object, Aggregateable> statValues = Maps.newHashMap();
    private AggregationMilliSec contextTime = AggregationMilliSec.neutral();

    private final ContextKey key;

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      StatisticsContext other = (StatisticsContext) obj;
      if (key == null) {
        if (other.key != null) {
          return false;
        }
      } else if (!key.equals(other.key)) {
        return false;
      }
      return true;
    }

    private StatCpuTimer contextTimer;

    public StatisticsContext(Key pKey) {
      Preconditions.checkNotNull(pKey);
      this.key = pKey;
    }

    @Override
    public void registerChild(Context pChildContext) {
      childContexts.put(pChildContext.getKey().getIdentifier(), pChildContext);
    }

    @Override
    public ContextKey getKey() {
      return key;
    }

    @Override
    public void close() {
      contextTimer.stop();
      Stats.popContext(this);
    }

    private void activated() {
      final StatisticsContext self = this;
      contextTimer = new StatCpuTimer(new TimeMeasurementListener() {
        @Override
        public void onMeasurementResult(long pSpentCpuTimeMSecs, long pSpentWallTimeMSecs) {
          AggregationMilliSec ms = AggregationMilliSec.single(pSpentCpuTimeMSecs, pSpentWallTimeMSecs);
          self.contextTime = self.contextTime.aggregateBy(ms);
        }
      });
    }

    @Override
    public synchronized void aggregate(Object pKey, Aggregateable pAgg) {

      Aggregateable existing = statValues.get(pKey);

      if (existing != null) {
        statValues.put(pKey, existing.aggregateBy(pAgg));

      } else {
        statValues.put(pKey, pAgg);
      }
    }

    @Override
    public ImmutableSet<Context> getChildContexts() {
      return ImmutableSet.copyOf(childContexts.values());
    }

    @Override
    public ImmutableMap<Object, Aggregateable> getStatistics() {
      return ImmutableMap.copyOf(statValues);
    }

    @Override
    public AggregationMilliSec getContextTime() {
      return contextTime;
    }

    @Override
    public String toString() {
      return key.toString();
    }

    @Override
    public Context getChild(Object pIdent) {
      return childContexts.get(pIdent);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Aggregateable> T getStatistic(Object pIdent, Class<T> pClass)
        throws NoStatisticsException {

      Aggregateable value = statValues.get(pIdent);
      if (value == null) {
        throw new NoStatisticsException("No statistic value with ID '%s'", pIdent);
      }

      return (T) value;
    }

  }

  public synchronized static void incCounter(final String pKey, final int pBy) {
    aggValue(pKey, AggregationInt.single(pBy));
  }

  public static void putItems(Object pIdentifier, Set<Object> pItems) {
    aggValue(pIdentifier, AggregationSet.of(pItems));
  }

  private synchronized static void aggValue(final Object pKey, final Aggregateable agg) {
    final Thread t = Thread.currentThread();

    Set<Context> aggregatedTo = Sets.newHashSet();

    // Aggregate to all active contexts
    for (Context ctx: activeContexts.get(t)) {
      if (aggregatedTo.add(ctx)) {
        ctx.aggregate(pKey, agg);
        valuesInContexts.put(pKey, ctx);
      }
    }

  }

  public synchronized static Contexts beginSubContext(final Object pIdentifier) {
    final Thread t = Thread.currentThread();

    final Set<StatisticsContext> result;

    if (activeContexts.containsKey(t)) {

      // Create new child contexts (not yet activated!)
      result =  Sets.newHashSet();
      for (Context parentContext: activeContexts.get(t)) {
        StatisticsContext ctx = getContext(t, pIdentifier, Optional.<Context>of(parentContext));
        result.add(ctx);
      }

      // Deactivate the parent contexts of the new child contexts!
      for (Context newContext : result) {
        activeContexts.remove(t, newContext.getKey().getParentContext().get());
      }

      // Activate the new contexts!
      activeContexts.putAll(t, result);

    } else {

      // Statistics for threads are separated!

      StatisticsContext ctx = getContext(t, pIdentifier, Optional.<Context>absent());
      if (!rootContexts.containsEntry(t, ctx)) {
        rootContexts.put(t, ctx);
      }

      activeContexts.put(t, ctx);

      result = Sets.newHashSet(ctx);
    }

    // Signal the activation in each context
    //  Triggers observers, and internal statistics!
    for (StatisticsContext ctx: result) {
      ctx.activated();
    }

    return new Contexts(result);
  }

  private synchronized static void popContext(Context pContext) {
    final Thread t = Thread.currentThread();
    activeContexts.remove(t, pContext);

    if (pContext.getKey().getParentContext().isPresent()) {
      activeContexts.put(t, pContext.getKey().getParentContext().get());
    }
  }

  public synchronized static Contexts beginRootContext(final Object ... pIdentifier) {
    final Thread t = Thread.currentThread();
    Set<StatisticsContext> result = Sets.newHashSet();

    for (Object ident: pIdentifier) {
      StatisticsContext ctx = getContext(t, ident, Optional.<Context>absent());

      result.add(ctx);

      if (!rootContexts.containsEntry(t, ctx)) {
        rootContexts.put(t, ctx);
      }
      activeContexts.put(t, ctx);
    }

    // Signal the activation in each context
    //  Triggers observers, and internal statistics!
    for (StatisticsContext ctx: result) {
      ctx.activated();
    }

    return new Contexts(result);
  }

  private synchronized static StatisticsContext getContext(
      final Thread pThread,
      final Object pIdent,
      Optional<Context> pParentContext) {

    final Key key = new Key(pParentContext, pIdent, pThread);

    StatisticsContext result = contexts.get(key);
    if (result == null) {
      result = new StatisticsContext(key);
      contexts.put(key, result);
    }

    if (pParentContext.isPresent()) {
      pParentContext.get().registerChild(result);
    }

    return result;
  }

  public synchronized static StatCpuTimer startTimer(final Object pIdentifier) {
    return new StatCpuTimer(new TimeMeasurementListener() {
      @Override
      public void onMeasurementResult(long pSpentCpuTimeMSecs, long pSpentWallTimeMSecs) {
        AggregationMilliSec ms = AggregationMilliSec.single(pSpentCpuTimeMSecs, pSpentWallTimeMSecs);
        aggValue(pIdentifier, ms);
      }
    });
  }

  public static synchronized void printContext(
      final PrintStream pOut, final Context pContext, final int pLevel,
      final boolean pIncludeRootStats, final boolean pIncludeChildStats) {


    StatisticsUtils.write(pOut, pLevel, 50, "+ " + pContext.getKey().getIdentifier().toString(), pContext.getContextTime().toString());
    ImmutableMap<Object, Aggregateable> stats = pContext.getStatistics();
    for (Object k: stats.keySet()) {
      Aggregateable v = stats.get(k);
      StatisticsUtils.write(pOut, pLevel, 50, "  - " + k.toString(), v.toString());
    }

    for (Context child: pContext.getChildContexts()) {
      printContext(pOut, child, pLevel+1, pIncludeRootStats, pIncludeRootStats);
    }
  }

  public static synchronized void printStatitics(final PrintStream pOut,
      final boolean pIncludeRootStats, final boolean pIncludeChildStats) {

    for (Thread t: rootContexts.keySet()) {
      for (Context ctx: rootContexts.get(t)) {

        printContext(pOut, ctx, 0, pIncludeRootStats, pIncludeChildStats);
        pOut.println();
      }
    }
  }

  @SuppressWarnings("resource")
  private static synchronized boolean isSubInContextOf(
      final Context pLeafContext,
      final Object pQueriedContextIdent) {

    Context cur = pLeafContext;
    do {
      if (cur.getKey().getIdentifier().equals(pQueriedContextIdent)) {
        return true;
      }

      if (cur.getKey().getParentContext().isPresent()) {
        cur = cur.getKey().getParentContext().get();
      } else {
        cur = null;
      }

    } while (cur != null);

    return false;
  }

  public static synchronized <R extends Aggregateable> R query(
      final Thread pThread,
      final Object pValueIdent,
      final Object pInChainWithContextIdent,
      final Class<R> pResultClass)
  throws NoStatisticsException {

    // Problem: The structure of the context
    //  heavily depends on the configuration of the verifier
    //    (different algorithms might be wrapped)
    //
    // Solution:
    //  1. Check from leafs of the tree
    //  2. Check that a specific context appears along to the root

    Preconditions.checkNotNull(pThread);
    Preconditions.checkNotNull(pValueIdent);
    Preconditions.checkNotNull(pInChainWithContextIdent);
    Preconditions.checkNotNull(pResultClass);

    Collection<Context> contexs = valuesInContexts.get(pValueIdent);
    if (!contexs.isEmpty()) {
      for (Context ctx: contexs) {
        // Thread
        if (!ctx.getKey().getThread().equals(pThread)) {
          continue;
        }

        // Context
        if (!isSubInContextOf(ctx, pInChainWithContextIdent)) {
          continue;
        }

        return ctx.getStatistic(pValueIdent, pResultClass);
      }
    }

    throw new NoStatisticsException("No statistics with ID '%s' in context of '%s'",
        pValueIdent, pInChainWithContextIdent);
  }

}
