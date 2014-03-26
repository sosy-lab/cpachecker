package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@Options(prefix = "cpa.bam")
public class BAMCache {

  @Option(description = "if enabled, cache queries also consider blocks with non-matching precision for reuse.")
  private boolean aggressiveCaching = true;

  final Timer hashingTimer = new Timer();
  final Timer equalsTimer = new Timer();
  final Timer searchingTimer = new Timer();

  int abstractionCausedMisses = 0;
  int precisionCausedMisses = 0;
  int noSimilarCausedMisses = 0;

  private final Map<AbstractStateHash, ReachedSet> preciseReachedCache = new HashMap<>();
  private final Map<AbstractStateHash, ReachedSet> unpreciseReachedCache = new HashMap<>();
  private final Map<AbstractStateHash, Collection<AbstractState>> returnCache = new HashMap<>();
  private final Map<AbstractStateHash, ARGState> blockARGCache = new HashMap<>();

  private ARGState lastAnalyzedBlock = null;
  private final Reducer reducer;

  public BAMCache(Configuration config, Reducer reducer) throws InvalidConfigurationException {
    config.inject(this);
    this.reducer = reducer;
  }

  private AbstractStateHash getHashCode(AbstractState stateKey, Precision precisionKey, Block context) {
    return new AbstractStateHash(stateKey, precisionKey, context);
  }

  public void put(AbstractState stateKey, Precision precisionKey, Block context, ReachedSet item) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    assert !preciseReachedCache.containsKey(hash);
    preciseReachedCache.put(hash, item);
  }

  public void put(AbstractState stateKey, Precision precisionKey, Block context, Collection<AbstractState> item,
                   ARGState rootOfBlock) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    assert allStatesContainedInReachedSet(item, preciseReachedCache.get(hash));
    returnCache.put(hash, item);
    blockARGCache.put(hash, rootOfBlock);
    setLastAnalyzedBlock(hash);
  }

  private boolean allStatesContainedInReachedSet(Collection<AbstractState> pElements, ReachedSet reached) {
    for (AbstractState e : pElements) {
      if (!reached.contains(e)) { return false; }
    }
    return true;
  }

  public void removeReturnEntry(AbstractState stateKey, Precision precisionKey, Block context) {
    returnCache.remove(getHashCode(stateKey, precisionKey, context));
  }

  public void removeBlockEntry(AbstractState stateKey, Precision precisionKey, Block context) {
    blockARGCache.remove(getHashCode(stateKey, precisionKey, context));
  }

  public Pair<ReachedSet, Collection<AbstractState>> get(AbstractState stateKey, Precision precisionKey, Block context) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);

    ReachedSet result = preciseReachedCache.get(hash);
    if (result != null) {
      setLastAnalyzedBlock(hash);
      return Pair.of(result, returnCache.get(hash));
    }

    if (aggressiveCaching) {
      result = unpreciseReachedCache.get(hash);
      if (result != null) {
        AbstractStateHash unpreciseHash = getHashCode(stateKey, result.getPrecision(result.getFirstState()), context);
        setLastAnalyzedBlock(unpreciseHash);
        return Pair.of(result, returnCache.get(unpreciseHash));
      }

      //search for similar entry
      Pair<ReachedSet, Collection<AbstractState>> pair = lookForSimilarState(stateKey, precisionKey, context);
      if (pair != null) {
        //found similar element, use this
        unpreciseReachedCache.put(hash, pair.getFirst());
        setLastAnalyzedBlock(getHashCode(stateKey, pair.getFirst().getPrecision(pair.getFirst().getFirstState()),
                context));
        return pair;
      }
    }

    lastAnalyzedBlock = null;
    return Pair.of(null, null);
  }

  private void setLastAnalyzedBlock(AbstractStateHash pHash) {
    if (BAMTransferRelation.PCCInformation.isPCCEnabled()) {
      lastAnalyzedBlock = blockARGCache.get(pHash);
    }
  }

  public ARGState getLastAnalyzedBlock() {
    return lastAnalyzedBlock;
  }

  private Pair<ReachedSet, Collection<AbstractState>> lookForSimilarState(AbstractState pStateKey,
                                                                          Precision pPrecisionKey, Block pContext) {
    searchingTimer.start();
    try {
      int min = Integer.MAX_VALUE;
      Pair<ReachedSet, Collection<AbstractState>> result = null;

      for (AbstractStateHash cacheKey : preciseReachedCache.keySet()) {
        //searchKey != cacheKey, check whether it is the same if we ignore the precision
        AbstractStateHash ignorePrecisionSearchKey = getHashCode(pStateKey, cacheKey.precisionKey, pContext);
        if (ignorePrecisionSearchKey.equals(cacheKey)) {
          int distance = reducer.measurePrecisionDifference(pPrecisionKey, cacheKey.precisionKey);
          if (distance < min) { //prefer similar precisions
            min = distance;
            result = Pair.of(
                    preciseReachedCache.get(ignorePrecisionSearchKey),
                    returnCache.get(ignorePrecisionSearchKey));
          }
        }
      }

      return result;
    } finally {
      searchingTimer.stop();
    }
  }

  public void findCacheMissCause(AbstractState pStateKey, Precision pPrecisionKey, Block pContext) {
    AbstractStateHash searchKey = getHashCode(pStateKey, pPrecisionKey, pContext);
    for (AbstractStateHash cacheKey : preciseReachedCache.keySet()) {
      assert !searchKey.equals(cacheKey);
      //searchKey != cacheKey, check whether it is the same if we ignore the precision
      AbstractStateHash ignorePrecisionSearchKey = getHashCode(pStateKey, cacheKey.precisionKey, pContext);
      if (ignorePrecisionSearchKey.equals(cacheKey)) {
        precisionCausedMisses++;
        return;
      }
      //precision was not the cause. Check abstraction.
      AbstractStateHash ignoreAbsSearchKey = getHashCode(cacheKey.stateKey, pPrecisionKey, pContext);
      if (ignoreAbsSearchKey.equals(cacheKey)) {
        abstractionCausedMisses++;
        return;
      }
    }
    noSimilarCausedMisses++;
  }

  public void clear() {
    preciseReachedCache.clear();
    unpreciseReachedCache.clear();
    returnCache.clear();
  }

  public boolean containsPreciseKey(AbstractState stateKey, Precision precisionKey, Block context) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    return preciseReachedCache.containsKey(hash);
  }

  public void updatePrecisionForEntry(AbstractState stateKey, Precision precisionKey, Block context,
                                      Precision newPrecisionKey) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    ReachedSet reachedSet = preciseReachedCache.get(hash);
    if (reachedSet != null) {
      preciseReachedCache.remove(hash);
      preciseReachedCache.put(getHashCode(stateKey, newPrecisionKey, context), reachedSet);
    }
  }

  public Collection<ReachedSet> getAllCachedReachedStates() {
    return preciseReachedCache.values();
  }

  private class AbstractStateHash {

    private final Object wrappedHash;
    private final Block context;
    private final AbstractState stateKey;
    private final Precision precisionKey;

    public AbstractStateHash(AbstractState pStateKey, Precision pPrecisionKey, Block pContext) {
      wrappedHash = reducer.getHashCodeForState(pStateKey, pPrecisionKey);
      context = checkNotNull(pContext);
      stateKey = pStateKey;
      precisionKey = pPrecisionKey;
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof AbstractStateHash)) { return false; }
      AbstractStateHash other = (AbstractStateHash) pObj;
      equalsTimer.start();
      try {
        return context.equals(other.context)
                && wrappedHash.equals(other.wrappedHash);
      } finally {
        equalsTimer.stop();
      }
    }

    @Override
    public int hashCode() {
      hashingTimer.start();
      try {
        return wrappedHash.hashCode() * 17 + context.hashCode();
      } finally {
        hashingTimer.stop();
      }
    }

    @Override
    public String toString() {
      return "AbstractStateHash [hash=" + hashCode() + ", wrappedHash=" + wrappedHash + ", context="
              + context + ", predicateKey=" + stateKey + ", precisionKey=" + precisionKey + "]";
    }
  }
}
