package org.sosy_lab.cpachecker.util.statistics;

import java.io.Closeable;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.util.statistics.Aggregateables.AggregationInt;
import org.sosy_lab.cpachecker.util.statistics.Aggregateables.AggregationMilliSecPair;
import org.sosy_lab.cpachecker.util.statistics.Aggregateables.AggregationSet;
import org.sosy_lab.cpachecker.util.statistics.Aggregateables.AggregationTime;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.StatCpuTimer;
import org.sosy_lab.cpachecker.util.statistics.interfaces.Aggregateable;
import org.sosy_lab.cpachecker.util.statistics.interfaces.Context;
import org.sosy_lab.cpachecker.util.statistics.interfaces.ContextKey;
import org.sosy_lab.cpachecker.util.statistics.interfaces.NoStatisticsException;
import org.sosy_lab.cpachecker.util.statistics.interfaces.RetrospectiveContext;
import org.sosy_lab.cpachecker.util.statistics.interfaces.TimeMeasurementListener;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
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
    private final int cachedHashCode;

    public Key(Optional<Context> pParentContext, Object pClassIdentifier, Thread pThread) {
      this.parentContext = pParentContext;
      this.classIdentifier = pClassIdentifier;
      this.thread = pThread;

      this.cachedHashCode = computeHashCode();
    }

    @Override
    public String toString() {
      return String.format("%d %s %s", thread.getId(), classIdentifier.toString(), parentContext.toString());
    }

    @Override
    public Object getIdentifier() {
      return classIdentifier;
    }

    @Override
    public Optional<Context> getParentContext() {
      return parentContext;
    }

    @Override
    public Thread getThread() {
      return thread;
    }

    private int computeHashCode() {
      final int prime = 53;
      int result = 1;
      result = prime * result + classIdentifier.hashCode();
      result = prime * result + parentContext.hashCode();
      result = prime * result + thread.hashCode();
      return result;
    }

    @Override
    public int hashCode() {
      return cachedHashCode;
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

    private Set<? extends Context> wrappedContexts;

    public Contexts(Set<? extends Context> pContexts) {
      wrappedContexts = pContexts; // No copy (performance optimization)
    }

    @Override
    public void close() {
      for (Context x: this.wrappedContexts) {
        x.close();
      }
    }
  }

  private static class StatisticsContext implements Context {

    protected transient final Map<Object, Context> childContexts = Maps.newHashMap();
    protected final Map<Object, Aggregateable> statValues = Maps.newHashMap();
    protected AggregationTime contextWallDuration = AggregationTime.neutral();
    @Nullable protected Long contextActivationTime;
    private int timesActive = 0;

    private final ContextKey key;
    private final int cachedHashCode;

    public StatisticsContext(final Key pKey) {

      Preconditions.checkNotNull(pKey);

      this.contextActivationTime = null;
      this.key = pKey;
      this.cachedHashCode = key.hashCode();
    }

    @Override
    public int hashCode() {
      return cachedHashCode;
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
      Preconditions.checkNotNull(contextActivationTime);

      timesActive = timesActive - 1;

      if (timesActive <= 0) {
        // Measure the time
        final long contextCloseTime = System.currentTimeMillis();
        final long activationDuration = contextCloseTime - contextActivationTime;
        contextWallDuration = contextWallDuration.aggregateBy(
            AggregationTime.single(activationDuration));
        contextActivationTime = null;

        // Remove the context from the global stack
        Stats.popContext(this);
      }
    }

    void activated() {
      this.contextActivationTime = System.currentTimeMillis();
      this.timesActive = timesActive + 1;
    }

    synchronized void addToContextWallTime (final AggregationTime pTime) {
      this.contextWallDuration = this.contextWallDuration.aggregateBy(pTime);
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
    public AggregationTime getContextWallTimeMillis() {
      return contextWallDuration;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(key);
      if (contextActivationTime == null) {
        sb.append(" INACTIVE");
      } else {
        sb.append(" ACTIVATED");
      }
      return sb.toString();
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

    @Override
    public Set<Object> getStatisticKeys() {
      return statValues.keySet();
    }

  }


  public static interface ContextCloseHandler {
    public void onClose(Context pContext);
  }

  private static class AnonymousContext extends StatisticsContext {

    private ContextCloseHandler closeHandler;

    public AnonymousContext(Key pKey, ContextCloseHandler pOnClose) {
      super(pKey);
      this.closeHandler = Preconditions.checkNotNull(pOnClose);
    }

    @Override
    public void close() {
      super.close();
      closeHandler.onClose(this);
    }

  }

  private static class RetroStatisticsContext extends StatisticsContext implements RetrospectiveContext {

    private final Set<Object> retroRoots = Sets.newHashSet();
    private boolean closed = false;

    public RetroStatisticsContext(Key pKey) {
      super(pKey);
    }

    @Override
    public void putRootContexts(Set<?> pContextIdents) {
      retroRoots.addAll(pContextIdents);
    }

    @Override
    public <T extends Object>  void putRootContexts(T... pContextIdents) {
      for (Object o: pContextIdents) {
        retroRoots.add(o);
      }
    }

    private void aggregateStatsFromTo(Context pFrom, StatisticsContext pTo) throws NoStatisticsException {

      pTo.addToContextWallTime(pFrom.getContextWallTimeMillis());

      for (Object statKey: pFrom.getStatisticKeys()) {

        final Aggregateable agg = pFrom.getStatistic(statKey, Aggregateable.class);
        pTo.aggregate(statKey, agg);
      }

      for (Context fromChild: pFrom.getChildContexts()) {
        final ContextKey fromKey = fromChild.getKey();
        final StatisticsContext toChild = getContext(
            fromKey.getThread(),
            fromKey.getIdentifier(),
            Optional.<Context>of(pTo));

        aggregateStatsFromTo(pFrom, pTo);
      }
    }

    @Override
    public void close() {
      super.close();

      Preconditions.checkState(!closed);

      final Thread t = Thread.currentThread();

      for (Object retroIdent: retroRoots) {
        final StatisticsContext ctx = getContext(t, retroIdent, Optional.<Context>absent());

        // TODO: This is not yet thread-safe!!
        if (!rootContexts.containsEntry(t, ctx)) {
          rootContexts.put(t, ctx);
        }

        // Copy statistics on the root level ...
        try {
          aggregateStatsFromTo(this, ctx);

        } catch (NoStatisticsException e) {
          throw new RuntimeException(e);
        }
      }

      closed = true;
    }

  }

  public synchronized static void incCounter(final String pKey, final int pBy) {
    aggValue(pKey, AggregationInt.single(pBy));
  }

  public static void putItems(Object pIdentifier, Set<? extends Object> pItems) {
    aggValue(pIdentifier, AggregationSet.of(pItems));
  }

  private synchronized static void aggValue(final Object pKey, final Aggregateable agg) {
    final Thread t = Thread.currentThread();

    for (Context ctx: activeContexts.get(t)) {
      ctx.aggregate(pKey, agg);
      valuesInContexts.put(pKey, ctx);
    }

  }

  public synchronized static Contexts beginSubContext(final Object pIdentifier) {
    final Thread t = Thread.currentThread();

    final Set<StatisticsContext> result;

    if (activeContexts.containsKey(t)) {

      // Create new child contexts (not yet activated!)
      Collection<Context> threadContexs = activeContexts.get(t);
      result =  Sets.newHashSetWithExpectedSize(threadContexs.size());
      for (Context parentContext: threadContexs) {
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

  public synchronized static Contexts beginRootContextCollection(final Collection<?> pIdentifier) {
    final Thread t = Thread.currentThread();
    Set<StatisticsContext> result = Sets.newHashSetWithExpectedSize(pIdentifier.size());

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

  public synchronized static Contexts beginRootContext(final Object ... pIdentifier) {
    final Thread t = Thread.currentThread();
    Set<StatisticsContext> result = Sets.newHashSetWithExpectedSize(pIdentifier.length);

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

  public synchronized static RetrospectiveContext retrospectiveRootContext() {
    final Thread t = Thread.currentThread();
    final Object id = new Object();
    final Key key = new Key(Optional.<Context>absent(), id, t);
    RetroStatisticsContext ctx = new RetroStatisticsContext(key);

    activeContexts.put(t, ctx);

    // Signal the activation in each context
    //  Triggers observers, and internal statistics!
    ctx.activated();

    return ctx;
  }

  public synchronized static Context anonymousRootContext(ContextCloseHandler pOnClose) {
    final Thread t = Thread.currentThread();
    final Object id = new Object();
    final Key key = new Key(Optional.<Context>absent(), id, t);
    AnonymousContext ctx = new AnonymousContext(key, pOnClose);

    activeContexts.put(t, ctx);
    ctx.activated();

    return ctx;
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

      if (pParentContext.isPresent()) {
        pParentContext.get().registerChild(result);
      }
    }

    return result;
  }

  public synchronized static StatCpuTimer startTimer(final Object pIdentifier) {
    return new StatCpuTimer(new TimeMeasurementListener() {
      @Override
      public void onMeasurementResult(long pSpentCpuTimeMSecs, long pSpentWallTimeMSecs) {
        AggregationMilliSecPair ms = AggregationMilliSecPair.single(pSpentCpuTimeMSecs, pSpentWallTimeMSecs);
        aggValue(pIdentifier, ms);
      }
    });
  }

  public static synchronized void printContext(
      final PrintStream pOut, final Context pContext, final int pLevel)
          throws NoStatisticsException {


    StatisticsUtils.write(pOut, pLevel, 50, "+ " + pContext.getKey().getIdentifier().toString(), pContext.getContextWallTimeMillis().toString());
    for (Object k: pContext.getStatisticKeys()) {
      Aggregateable v = pContext.getStatistic(k, Aggregateable.class);
      StatisticsUtils.write(pOut, pLevel, 50, "  - " + k.toString(), v.toString());
    }

    for (Context child: pContext.getChildContexts()) {
      printContext(pOut, child, pLevel+1);
    }
  }

  public static synchronized void printStatitics(final PrintStream pOut) {

    for (Thread t: rootContexts.keySet()) {
      try {
        for (Context ctx: rootContexts.get(t)) {
          printContext(pOut, ctx, 0);
          pOut.println();
        }
      } catch (NoStatisticsException e) {
        throw new RuntimeException(e);
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
