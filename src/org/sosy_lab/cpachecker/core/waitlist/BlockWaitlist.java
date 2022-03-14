// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BlockWaitlist implements Waitlist {

  private static CallstackState retreiveCallstack(AbstractState pState) {
    return AbstractStates.extractStateByType(pState, CallstackState.class);
  }

  private static class Block {
    public static final String ENTRY_BLOCK_NAME = "entry_block_main";
    // function name which is the basis for the block
    @SuppressWarnings("unused")
    private String name;
    // current number of used resources
    private int countResources;
    // saved number of resources when limit is reached
    private int savedResources;
    // limit for resources
    private BlockConfiguration limits;
    // main waitlist
    private Waitlist mainWaitlist;
    // is it a block for entry function
    private boolean isEntryBlock;
    // previous block in the list

    Block(BKey key, WaitlistFactory factory, BlockConfiguration pLimits) {
      mainWaitlist = factory.createWaitlistInstance();
      limits = pLimits;
      name = key.name;
    }

    @SuppressWarnings("unused")
    public int getSavedResources() {
      return savedResources;
    }

    /** Add state to main waitlist, increment used resources */
    void addStateToMain(AbstractState e) {
      mainWaitlist.add(e);
      incResources(e);
    }

    /**
     * check resource limits
     *
     * @return true if resource limit has been reached
     */
    boolean checkResources() {
      if (isEntryBlock) {
        // check entry limit
        return countResources > limits.getEntryResourceLimit();
      } else {
        return countResources > limits.getBlockResourceLimit();
      }
    }

    @SuppressWarnings("unused")
    private void incResources(AbstractState e) {
      countResources++;
    }

    @SuppressWarnings("unused")
    private void decResources(AbstractState e) {
      countResources--;
    }

    boolean isEmpty() {
      return mainWaitlist.isEmpty();
    }

    boolean removeState(AbstractState e) {
      boolean b = mainWaitlist.remove(e);

      if (b) {
        // remove resources for e in block
        decResources(e);
      }
      return b;
    }

    AbstractState popState() {
      AbstractState res;
      if (!mainWaitlist.isEmpty()) {
        res = mainWaitlist.pop();
        // remove resources for e
        // decResources(res);
        return res;
      } else {
        throw new AssertionError("invalid pop: current block is empty");
      }
    }

    int size() {
      return mainWaitlist.size();
    }
  }

  private static final class BKey implements Comparable<BKey> {
    private final String name;
    private final int callStackDepth;

    BKey(String pName, int pDepth) {
      name = checkNotNull(pName);
      callStackDepth = pDepth;
    }

    @Override
    public int compareTo(BKey k2) {
      return ComparisonChain.start()
          .compare(callStackDepth, k2.callStackDepth)
          .compare(name, k2.name)
          .result();
    }

    @Override
    public String toString() {
      return "[" + name + ", " + callStackDepth + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + callStackDepth;
      result = prime * result + name.hashCode();
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
      BKey other = (BKey) obj;
      return (callStackDepth == other.callStackDepth) && name.equals(other.name);
    }
  }

  private final WaitlistFactory wrappedWaitlist;

  private int size = 0;
  // the map of active blocks (for efficient state removal)
  private final NavigableMap<BKey, Block> activeBlocksMap = new TreeMap<>();
  // map of inactive blocks (where resource limits are reached)
  private final Map<BKey, Block> inactiveBlocksMap = new TreeMap<>();
  // map of saved empty blocks (to count resources during)
  private final Map<BKey, Block> savedBlocksMap = new TreeMap<>();

  private BlockConfiguration config;
  private LogManager logger;

  /**
   * Constructor that needs a factory for the waitlist implementation that should be used to store
   * states with the same block.
   */
  protected BlockWaitlist(
      WaitlistFactory pSecondaryStrategy, BlockConfiguration pConfig, LogManager pLogger) {
    wrappedWaitlist = Preconditions.checkNotNull(pSecondaryStrategy);
    config = pConfig;
    logger = pLogger;
    int len = config.getBlockFunctionPatterns().size();
    int i = 0;
    ldvPattern = new Pattern[len];
    for (String p : config.getBlockFunctionPatterns()) {
      ldvPattern[i] = Pattern.compile(p.replace("%", ".*"));
      i++;
    }
  }

  /**
   * add new block as the last element in the activeList
   *
   * @param key - key of the block
   * @param pState - first state to be added
   */
  private void addNewBlock(BKey key, AbstractState pState) {
    Block b;
    if (activeBlocksMap.containsKey(key)) {
      b = activeBlocksMap.get(key);
    } else {
      if (config.shouldSaveBlockResources() && savedBlocksMap.containsKey(key)) {
        // restore saved resources
        b = savedBlocksMap.remove(key);
      } else {
        b = new Block(key, wrappedWaitlist, config);
        b.isEntryBlock = key.name.equals(Block.ENTRY_BLOCK_NAME);
      }
      activeBlocksMap.put(key, b);
    }
    b.addStateToMain(pState);
  }

  /** mark last active block as inactive */
  private void makeBlockInactive(BKey key) {
    Block b = activeBlocksMap.get(key);
    inactiveBlocksMap.put(key, b);
    // save resource count
    b.savedResources = b.countResources;
    // remove from active blocks
    activeBlocksMap.remove(key);
    logger.log(Level.INFO, "Make inactive " + key + ", resources=" + b.countResources);
  }

  private Pattern[] ldvPattern;

  /**
   * checks whether function name is a block (for example, starts with emg_control or emg_callback
   * or matches ldv_.*_instance_)
   *
   * @return true if it is a block entry
   */
  private boolean isBlock(String func) {
    for (Pattern p : ldvPattern) {
      Matcher matcher = p.matcher(func);
      if (matcher.matches()) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return key for the block
   */
  private BKey getBlockKey(AbstractState e) {
    CallstackState callStack = retreiveCallstack(e);
    String resFunc = Block.ENTRY_BLOCK_NAME;
    int resDepth = 1;
    while (callStack != null) {
      // get current function
      String func = callStack.getCurrentFunction();
      if (isBlock(func)) {
        resFunc = func;
        resDepth = callStack.getDepth();
        break;
      }
      callStack = callStack.getPreviousState();
    }
    return new BKey(resFunc, resDepth);
  }

  /**
   * get block for state e
   *
   * @param e the state for which we need a block
   * @return block for state e
   */
  private @Nullable Block getBlockForState(AbstractState e) {
    BKey key = getBlockKey(e);
    assert key != null;

    // search block in active blocks
    Block block = activeBlocksMap.get(key);
    if (block != null) {
      return block;
    }

    // search block in inactive blocks
    block = inactiveBlocksMap.get(key);
    return block;
  }

  @Override
  public Iterator<AbstractState> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(AbstractState pState) {
    BKey key = getBlockKey(pState);
    size++;

    CallstackState cs = retreiveCallstack(pState);

    logger.log(Level.FINE, "Add" + key + "[" + cs.getCurrentFunction() + "], size=" + size);
    if (inactiveBlocksMap.containsKey(key)) {
      // TODO: optimization - do not add
      Block block = inactiveBlocksMap.get(key);
      block.addStateToMain(pState);
    } else {
      Block b = activeBlocksMap.get(key);
      if (b != null) {
        b.addStateToMain(pState);
        logger.log(Level.FINE, "Resources=" + b.countResources);
        logger.log(Level.FINE, "Callstack=" + cs);
        if (b.checkResources()) {
          // stop analysis for the current block
          makeBlockInactive(key);
        }
      } else {
        addNewBlock(key, pState);
      }
    }
  }

  @Override
  public boolean contains(AbstractState pState) {
    Block block = getBlockForState(pState);
    if (block == null) {
      return false;
    }
    return block.mainWaitlist.contains(pState);
  }

  @Override
  public boolean remove(AbstractState pState) {
    // remove may be called even if the state is not in the waitlist
    Block block = getBlockForState(pState);
    if (block == null) {
      return false;
    }
    boolean b = block.removeState(pState);
    if (!b) {
      return false;
    }
    size--;
    logger.log(
        Level.FINE,
        "Remove[" + block.name + "] resources=" + block.countResources + ", size=" + size);
    return true;
  }

  boolean unknownIfHasInactive = true;

  @Override
  public AbstractState pop() {
    assert !isEmpty();
    Entry<BKey, Block> e = activeBlocksMap.lastEntry();
    while (e != null && e.getValue().isEmpty()) {
      activeBlocksMap.pollLastEntry();
      if (config.shouldSaveBlockResources() && e.getValue().countResources != 0) {
        logger.log(
            Level.INFO, "Save block=" + e.getKey() + ", resources=" + e.getValue().countResources);
        savedBlocksMap.put(e.getKey(), e.getValue());
      }
      e = activeBlocksMap.lastEntry();
    }

    if (unknownIfHasInactive && isEmptyMap()) {
      logger.log(Level.FINE, "active blocks=" + activeBlocksMap.keySet());
      throw new RuntimeException(
          "Waitlist of size="
              + size
              + " contains only inactive blocks "
              + inactiveBlocksMap.keySet());
    }
    assert !isEmpty();
    Entry<BKey, Block> highestEntry = activeBlocksMap.lastEntry();
    AbstractState state = highestEntry.getValue().popState();
    size--;
    logger.log(
        Level.FINE,
        "Pop"
            + highestEntry.getKey()
            + " resources="
            + highestEntry.getValue().countResources
            + ", size="
            + size);

    return state;
  }

  @Override
  public int size() {
    return size;
  }

  private boolean isEmptyMap() {
    if (activeBlocksMap.isEmpty()) {
      return true;
    }
    // fast detection if last block is not empty
    Entry<BKey, Block> highestEntry = activeBlocksMap.lastEntry();
    if (!highestEntry.getValue().isEmpty()) {
      return false;
    }

    // slow path

    for (BKey key : activeBlocksMap.descendingKeySet()) {
      Block b = activeBlocksMap.get(key);
      if (!b.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private int countSize() {
    int completeSize = 0;
    for (Block b : activeBlocksMap.values()) {
      completeSize += b.size();
    }
    for (Block b : inactiveBlocksMap.values()) {
      completeSize += b.size();
    }
    return completeSize;
  }

  @Override
  public boolean isEmpty() {
    assert size == countSize() : "size mismatch, size=" + size + ", countSize=" + countSize();
    if (unknownIfHasInactive) {
      return size == 0;
    }
    return isEmptyMap();
  }

  @Override
  public void clear() {
    activeBlocksMap.clear();
    inactiveBlocksMap.clear();
    size = 0;
  }

  public static WaitlistFactory factory(
      final WaitlistFactory pSecondaryStrategy, BlockConfiguration config, LogManager logger) {
    return () -> new BlockWaitlist(pSecondaryStrategy, config, logger);
  }
}
