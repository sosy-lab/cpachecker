// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import com.google.common.collect.UnmodifiableIterator;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.Var;
import com.google.errorprone.annotations.concurrent.LazyInit;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collector;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * Same as {@link PathCopyingPersistentTreeMap} with the change that the tree nodes have an
 * additional attribute "max" that is the same type as the keys. This max is used for a second
 * attribute of the values (similar to the keys) such that key and max can be interpreted as the
 * start and end of a line. The idea is that each node knows the maximum of this max value for its
 * own node and its subtrees. This way a search for these values is easier as less of the tree has
 * to be searched. But as a result, this data structure is slightly more expensive than a {@link
 * PathCopyingPersistentTreeMap} when adding/removing/copying.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
@Immutable(containerOf = {"K", "V"})
public final class PathCopyingPersistentMaxTreeMap<K extends Comparable<K>, V>
    implements Serializable {

  private static final long serialVersionUID = 1041711151457528188L;

  @SuppressWarnings("unused")
  @SuppressFBWarnings(
      value = "EQ_DOESNT_OVERRIDE_EQUALS",
      justification = "Inherits equals() according to specification.")
  @Immutable(containerOf = {"K", "V"})
  private static final class Node<K extends Comparable<K>, V> extends SimpleImmutableEntry<K, V> {

    // A secondary key maximum; the maximum of all secondary keys of this node or its subtrees
    // (left,
    // right)
    private final K max;

    // This nodes exact secondary key (i.e. offset + size)
    private final K secondaryKey;

    // Constants for isRed field
    private static final boolean RED = true;
    private static final boolean BLACK = false;

    private static final long serialVersionUID = -7393505826652634501L;

    private final @Nullable Node<K, V> left;
    private final @Nullable Node<K, V> right;
    private final boolean isRed;

    // Leaf node
    Node(K pKey, K pSecondaryKey, V pValue) {
      super(pKey, pValue);
      max = pSecondaryKey;
      secondaryKey = pSecondaryKey;
      left = null;
      right = null;
      isRed = RED;
    }

    // Any node
    Node(
        K pKey,
        K pSecondaryKey,
        K pMax,
        V pValue,
        Node<K, V> pLeft,
        Node<K, V> pRight,
        boolean pRed) {
      super(pKey, pValue);
      secondaryKey = pSecondaryKey;
      max = pMax;
      left = pLeft;
      right = pRight;
      isRed = pRed;
    }

    boolean isLeaf() {
      return left == null && right == null;
    }

    boolean getColor() {
      return isRed;
    }

    static <K extends Comparable<K>, V> boolean isRed(@Nullable Node<K, V> n) {
      return n != null && n.isRed;
    }

    static <K extends Comparable<K>, V> boolean isBlack(@Nullable Node<K, V> n) {
      return n != null && !n.isRed;
    }

    K getMax() {
      return max;
    }

    K getSecondaryKey() {
      return secondaryKey;
    }

    // Methods for creating new nodes based on current node.

    Node<K, V> withColor(boolean color) {
      if (isRed == color) {
        return this;
      } else {
        return new Node<>(getKey(), getSecondaryKey(), getMax(), getValue(), left, right, color);
      }
    }

    @SuppressWarnings("ReferenceEquality") // cannot use equals() for check whether tree is the same
    Node<K, V> withLeftChild(Node<K, V> newLeft) {
      if (newLeft == left) {
        return this;
      } else {
        K newMax = getMax();
        if (newLeft != null && newLeft.getMax().compareTo(newMax) > 0) {
          newMax = newLeft.getMax();
        }
        return new Node<>(getKey(), getSecondaryKey(), newMax, getValue(), newLeft, right, isRed);
      }
    }

    @SuppressWarnings("ReferenceEquality") // cannot use equals() for check whether tree is the same
    Node<K, V> withRightChild(Node<K, V> newRight) {
      if (newRight == right) {
        return this;
      } else {
        K newMax = getMax();
        if (newRight != null && newRight.getMax().compareTo(newMax) > 0) {
          newMax = newRight.getMax();
        }
        return new Node<>(getKey(), getSecondaryKey(), newMax, getValue(), left, newRight, isRed);
      }
    }

    static <K extends Comparable<K>, V> int countNodes(@Nullable Node<K, V> n) {
      if (n == null) {
        return 0;
      }
      return countNodes(n.left) + 1 + countNodes(n.right);
    }
  }

  // static creation methods

  private static final PathCopyingPersistentMaxTreeMap<?, ?> EMPTY_MAP =
      new PathCopyingPersistentMaxTreeMap<>(null);

  @SuppressWarnings("unchecked")
  public static <K extends Comparable<K>, V> PathCopyingPersistentMaxTreeMap<K, V> of() {
    return (PathCopyingPersistentMaxTreeMap<K, V>) EMPTY_MAP;
  }

  public static PathCopyingPersistentMaxTreeMap<?, ?> copyOf(
      PathCopyingPersistentMaxTreeMap<?, ?> map) {
    checkNotNull(map);

    // TODO: Kinda useless since i removed the inheritance^^
    return map;
  }

  /**
   * Return a {@link Collector} that accumulates elements into a {@link
   * PathCopyingPersistentMaxTreeMap}. Keys and values are the result of the respective functions.
   * If duplicate keys appear, the collector throws an {@link IllegalArgumentException}.
   */
  /*
  public static <T, K extends Comparable<? super K>, V>
      Collector<T, ?, PersistentSortedMap<K, V>> toPathCopyingPersistentMaxTreeMap(
          Function<? super T, ? extends K> keyFunction,
          Function<? super T, ? extends V> valueFunction) {
    return toPathCopyingPersistentMaxTreeMap(
        keyFunction,
        valueFunction,
        (k, v) -> {
          throw new IllegalArgumentException("Duplicate key " + k);
        });
  }*/

  /**
   * Return a {@link Collector} that accumulates elements into a {@link
   * PathCopyingPersistentMaxTreeMap}. Keys and values are the result of the respective functions.
   * Duplicate keys are resolved using the given merge function.
   */
  /*
  public static <T, K extends Comparable<? super K>, V>
      Collector<T, ?, PersistentSortedMap<K, V>> toPathCopyingPersistentMaxTreeMap(
          Function<? super T, ? extends K> keyFunction,
          Function<? super T, ? extends V> valueFunction,
          BinaryOperator<V> mergeFunction) {
    checkNotNull(keyFunction);
    checkNotNull(valueFunction);
    checkNotNull(mergeFunction);
    return Collectors.collectingAndThen(
        Collectors.toMap(
            keyFunction,
            valueFunction,
            mergeFunction,
            () -> CopyOnWriteSortedMap.copyOf(PathCopyingPersistentMaxTreeMap.<K, V>of())),
        CopyOnWriteSortedMap::getSnapshot);
  }
  */

  // state and constructor

  private final @Nullable Node<K, V> root;

  /*
  @SuppressWarnings("Immutable")
  @LazyInit
  private transient @Nullable NavigableSet<Entry<K, V>> entrySet;*/

  @LazyInit private transient int size;

  private PathCopyingPersistentMaxTreeMap(@Nullable Node<K, V> pRoot) {
    root = pRoot;
  }

  // private utility methods

  @SuppressWarnings("unchecked")
  @Nullable
  private static <K extends Comparable<K>, V> Node<K, V> findNode(K key, Node<K, V> root) {
    checkNotNull(key);
    return findNode1(key, root);
  }

  @Nullable
  private static <K extends Comparable<K>, V> Node<K, V> findNode1(K key, Node<K, V> root) {
    checkNotNull(key);

    @Var Node<K, V> current = root;
    while (current != null) {
      int comp = key.compareTo(current.getKey());

      if (comp < 0) {
        // key < current.data

        current = current.left;

      } else if (comp > 0) {
        // key > current.data

        current = current.right;

      } else {
        // key == current.data

        return current;
      }
    }
    return null;
  }

  /**
   * Find the node with the smallest key in a given non-empty subtree.
   *
   * @param root The subtree to search in.
   * @return The node with the smallest key.
   * @throws NullPointerException If tree is empty.
   */
  private static <K extends Comparable<K>, V> Node<K, V> findSmallestNode(Node<K, V> root) {
    @Var Node<K, V> current = root;
    while (current.left != null) {
      current = current.left;
    }
    return current;
  }

  /**
   * Find the node with the largest key in a given non-empty subtree.
   *
   * @param root The subtree to search in.
   * @return The node with the largest key.
   * @throws NullPointerException If tree is empty.
   */
  private static <K extends Comparable<K>, V> Node<K, V> findLargestNode(Node<K, V> root) {
    @Var Node<K, V> current = root;
    while (current.right != null) {
      current = current.right;
    }
    return current;
  }

  /**
   * Given a key and a tree, find the node in the tree with the given key, or (if {@code inclusive}
   * is false or there is no such node) the node with the smallest key that is still greater than
   * the key to look for. In terms of {@link NavigableMap} operations, this returns the node for
   * {@code map.tailMap(key, inclusive).first()}. Returns null if the tree is empty or there is no
   * node that matches (i.e., key is larger than the largest key in the map).
   *
   * @param key The key to search for.
   * @param root The tree to look in.
   * @return A node or null.
   */
  private static <K extends Comparable<K>, V> @Nullable Node<K, V> findNextGreaterNode(
      K key, Node<K, V> root, boolean inclusive) {
    checkNotNull(key);

    @Var Node<K, V> result = null; // this is always greater than key

    @Var Node<K, V> current = root;
    while (current != null) {
      int comp = key.compareTo(current.getKey());

      if (comp < 0) {
        // key < current.data
        // All nodes to the right of current are irrelevant because they are too big.
        // current is the best candidate we have found so far, so it becomes the new result
        // (current is always smaller than the previous result and still bigger than key).

        result = current;
        current = current.left;

      } else if (comp > 0) {
        // key > current.data
        // All nodes to the left of current are irrelevant because they are too small.
        // current itself is too small, too.

        current = current.right;

      } else {
        // key == current.data

        if (inclusive) {
          return current;
        } else {
          // All nodes to the left of current are irrelevant because they are too small.
          // current itself is too small, too.
          // The left-most node in the right subtree of child is the result.
          if (current.right == null) {
            // no node smaller than key in this subtree
            return result;
          } else {
            return findSmallestNode(current.right);
          }
        }
      }

      if (current == null) {
        // We have reached a leaf without finding the element.
        return result;
      }
    }
    return null;
  }

  /**
   * Given a key and a tree, find the node in the tree with the given key, or (if {@code inclusive}
   * is false or there is no such node) the node with the largest key that is still smaller than the
   * key to look for. In terms of {@link NavigableMap} operations, this returns the node for {@code
   * map.headMap(key, inclusive).last()}. Returns null if the tree is empty or there is no node that
   * matches (i.e., key is smaller than or equal to the smallest key in the map).
   *
   * @param key The key to search for.
   * @param root The tree to look in.
   * @return A node or null.
   */
  private static <K extends Comparable<K>, V> @Nullable Node<K, V> findNextSmallerNode(
      K key, Node<K, V> root, boolean inclusive) {
    checkNotNull(key);

    @Var Node<K, V> result = null; // this is always smaller than key

    @Var Node<K, V> current = root;
    while (current != null) {
      int comp = key.compareTo(current.getKey());

      if (comp < 0) {
        // key < current.data
        // All nodes to the right of current are irrelevant because they are too big.
        // current itself is too big, too.

        current = current.left;

      } else if (comp > 0) {
        // key > current.data
        // All nodes to the left of current are irrelevant because they are too small.
        // current is the best candidate we have found so far, so it becomes the new result
        // (current is always bigger than the previous result and still smaller than key).

        result = current;
        current = current.right;

      } else {
        // key == current.data

        if (inclusive) {
          return current;
        } else {
          // All nodes to the right of current are irrelevant because they are too big.
          // current itself is too big, too.
          // The right-most node in the left subtree of child is the result.
          if (current.left == null) {
            // no node smaller than key in this subtree
            return result;
          } else {
            return findLargestNode(current.left);
          }
        }
      }

      if (current == null) {
        // We have reached a leaf without finding the element.
        return result;
      }
    }
    return null;
  }

  private static <K extends Comparable<? super K>> boolean exceedsLowerBound(
      K pKey, K pLowerBound, boolean pLowerInclusive) {
    if (pLowerInclusive) {
      return pKey.compareTo(pLowerBound) < 0;
    } else {
      return pKey.compareTo(pLowerBound) <= 0;
    }
  }

  private static <K extends Comparable<? super K>> boolean exceedsUpperBound(
      K pKey, K pUpperBound, boolean pUpperInclusive) {
    if (pUpperInclusive) {
      return pKey.compareTo(pUpperBound) > 0;
    } else {
      return pKey.compareTo(pUpperBound) >= 0;
    }
  }

  private static <K extends Comparable<K>, V> int checkAssertions(Node<K, V> current) {
    if (current == null) {
      return 0;
    }

    // check property of binary search tree
    if (current.left != null) {
      checkState(
          current.getKey().compareTo(current.left.getKey()) > 0,
          "Tree has left child that is not smaller.");
    }
    if (current.right != null) {
      checkState(
          current.getKey().compareTo(current.right.getKey()) < 0,
          "Tree has right child that is not bigger.");
    }

    // Check LLRB invariants
    // No red right child.
    checkState(!Node.isRed(current.right), "LLRB has red right child");
    // No more than two consecutive red nodes.
    checkState(
        !Node.isRed(current) || !Node.isRed(current.left) || !Node.isRed(current.left.left),
        "LLRB has three red nodes in a row.");

    // Check recursively.
    int leftBlackHeight = checkAssertions(current.left);
    int rightBlackHeight = checkAssertions(current.right);

    // Check black height balancing.
    checkState(
        leftBlackHeight == rightBlackHeight,
        "Black path length on left is %s and on right is %s",
        leftBlackHeight,
        rightBlackHeight);

    @Var int blackHeight = leftBlackHeight;
    if (!current.isRed) {
      blackHeight++;
    }
    return blackHeight;
  }

  /**
   * Check the map for violation of its invariants.
   *
   * @throws IllegalStateException If any invariant is violated.
   */
  @VisibleForTesting
  @SuppressWarnings("CheckReturnValue")
  void checkAssertions() {
    checkAssertions(root);
  }

  // modifying methods

  /**
   * Create a map instance with a given root node.
   *
   * @param newRoot A node or null (meaning the empty tree).
   * @return A map instance with the given tree.
   */
  @SuppressWarnings("ReferenceEquality") // cannot use equals() for check whether tree is the same
  private PathCopyingPersistentMaxTreeMap<K, V> mapFromTree(@Var Node<K, V> newRoot) {
    if (newRoot == root) {
      return this;
    } else if (newRoot == null) {
      return of();
    } else {
      // Root is always black.
      newRoot = newRoot.withColor(Node.BLACK);
      return new PathCopyingPersistentMaxTreeMap<>(newRoot);
    }
  }

  public PathCopyingPersistentMaxTreeMap<K, V> putAndCopy(K key, K pMax, V value) {
    return mapFromTree(putAndCopy0(checkNotNull(key), checkNotNull(pMax), value, root));
  }

  private static <K extends Comparable<K>, V> Node<K, V> putAndCopy0(
      K key, K pSecondaryKey, V value, @Var Node<K, V> current) {
    // Inserting is easy:
    // We find the place where to insert,
    // and afterwards fix the invariants by some rotations or re-colorings.

    if (current == null) {
      return new Node<>(key, pSecondaryKey, value);
    }

    int comp = key.compareTo(current.getKey());
    if (comp < 0) {
      // key < current.data
      Node<K, V> newLeft = putAndCopy0(key, pSecondaryKey, value, current.left);
      current = current.withLeftChild(newLeft);

    } else if (comp > 0) {
      // key > current.data
      Node<K, V> newRight = putAndCopy0(key, pSecondaryKey, value, current.right);
      current = current.withRightChild(newRight);

    } else {
      // TODO: Check whether or not i need to calc max here or if restoreInvariants is always enough
      K max = getMaxSecondaryKey(current.left, current.right, pSecondaryKey);
      current =
          new Node<>(
              key, pSecondaryKey, max, value, current.left, current.right, current.getColor());
    }

    // restore invariants
    return restoreInvariants(current);
  }

  @SuppressWarnings("unused")
  private static <K extends Comparable<K>, V> K getMaxSecondaryKey(
      Node<K, V> node1, Node<K, V> node2, Node<K, V> node3) {
    Optional<K> temp = getMaxSecondaryKey(node1, node2);
    if (temp.isPresent() && temp.orElseThrow().compareTo(node3.getMax()) >= 0) {
      return temp.orElseThrow();
    }
    return node3.getMax();
  }

  private static <K extends Comparable<K>, V> K getMaxSecondaryKey(
      Node<K, V> node1, Node<K, V> node2, K max3) {
    Optional<K> temp = getMaxSecondaryKey(node1, node2);
    if (temp.isPresent() && temp.orElseThrow().compareTo(max3) >= 0) {
      return temp.orElseThrow();
    }
    return max3;
  }

  private static <K extends Comparable<K>, V> Optional<K> getMaxSecondaryKey(
      Node<K, V> node1, Node<K, V> node2) {
    if (node1 == null && node2 == null) {
      return Optional.empty();
    }
    if (node1 == null) {
      return Optional.of(node2.getMax());
    }
    if (node1.getMax().compareTo(node2.getMax()) >= 0) {
      return Optional.of(node1.getMax());
    }
    return Optional.of(node2.getMax());
  }

  @SuppressWarnings("unchecked")
  public PathCopyingPersistentMaxTreeMap<K, V> removeAndCopy(K key) {
    if (isEmpty()) {
      return this;
    }
    return mapFromTree(removeAndCopy0(checkNotNull(key), root));
  }

  @Nullable
  private static <K extends Comparable<K>, V> Node<K, V> removeAndCopy0(
      K key, @Var Node<K, V> current) {
    // Removing a node is more difficult.
    // We can remove a leaf if it is red.
    // So we try to always have a red node while going downwards.
    // This is accomplished by calling moveRedLeft() when going downwards to the left
    // or by calling moveRedRight() otherwise.
    // If we found the node and it is a leaf, we can then delete it
    // and do the usual adjustments for re-establishing the invariants (just like for insertion).
    // If we found the node and it is not a leaf node,
    // we can use a trick. We replace the node with the next greater node
    // (the left-most node in the right subtree),
    // and afterwards delete that node from the right subtree (otherwise it would be duplicate).

    @Var int comp = key.compareTo(current.getKey());

    if (comp < 0) {
      // key < current.data
      if (current.left == null) {
        // Target key is not in map.
        return current;
      }

      // Go down leftwards, keeping a red node.

      if (!Node.isRed(current.left) && !Node.isRed(current.left.left)) {
        // Push red to left if necessary.
        current = makeLeftRed(current);
      }

      // recursive descent
      Node<K, V> newLeft = removeAndCopy0(key, current.left);
      current = current.withLeftChild(newLeft);

    } else {
      // key >= current.data
      if ((comp > 0) && (current.right == null)) {
        // Target key is not in map.
        return current;
      }

      if (Node.isRed(current.left)) {
        // First chance to push red to right.
        current = rotateClockwise(current);

        // re-update comp
        comp = key.compareTo(current.getKey());
        assert comp >= 0;
      }

      if ((comp == 0) && (current.right == null)) {
        assert current.left == null;
        // We can delete the node easily, it's a leaf.
        return null;
      }

      if (!Node.isRed(current.right) && !Node.isRed(current.right.left)) {
        // Push red to right.
        current = makeRightRed(current);

        // re-update comp
        comp = key.compareTo(current.getKey());
        assert comp >= 0;
      }

      if (comp == 0) {
        // We have to delete current, but is has children.
        // We replace current with the smallest node in the right subtree (the "successor"),
        // and delete that (leaf) node there.

        @Var Node<K, V> successor = current.right;
        while (successor.left != null) {
          successor = successor.left;
        }

        // Delete the successor
        Node<K, V> newRight = removeMininumNodeInTree(current.right);
        // Update max; it is the max(current.right, current.left)
        K newMax = successor.getMax();
        if (successor.getMax().compareTo(current.left.getMax()) < 0) {
          newMax = current.left.getMax();
        }
        // and replace current with newRight
        current =
            new Node<>(
                successor.getKey(),
                successor.getSecondaryKey(),
                newMax,
                successor.getValue(),
                current.left,
                newRight,
                current.getColor());

      } else {
        // key > current.data
        // Go down rightwards.

        Node<K, V> newRight = removeAndCopy0(key, current.right);
        current = current.withRightChild(newRight);
      }
    }

    return restoreInvariants(current);
  }

  /**
   * Unconditionally delete the node with the smallest key in a given subtree.
   *
   * @return A new subtree reflecting the change.
   */
  @Nullable
  private static <K extends Comparable<K>, V> Node<K, V> removeMininumNodeInTree(
      @Var Node<K, V> current) {
    if (current.left == null) {
      // This is the minium node to delete
      return null;
    }

    if (!Node.isRed(current.left) && !Node.isRed(current.left.left)) {
      // Push red to left if necessary (similar to general removal strategy).
      current = makeLeftRed(current);
    }

    // recursive descent
    Node<K, V> newLeft = removeMininumNodeInTree(current.left);
    current = current.withLeftChild(newLeft);

    return restoreInvariants(current);
  }

  /**
   * Fix the LLRB invariants around a given node (regarding the node, its children, and
   * grand-children).
   *
   * @return A new subtree with the same content that is a legal LLRB.
   */
  private static <K extends Comparable<K>, V> Node<K, V> restoreInvariants(
      @Var Node<K, V> current) {
    if (Node.isRed(current.right)) {
      // Right should not be red in a left-leaning red-black tree.
      current = rotateCounterclockwise(current);
    }

    if (Node.isRed(current.left) && Node.isRed(current.left.left)) {
      // Don't have consecutive red nodes.
      current = rotateClockwise(current);
    }

    if (Node.isRed(current.left) && Node.isRed(current.right)) {
      // Again, don't have red right children.
      // We make both children black and this one red,
      // so we pass the potential problem of having a red right upwards in the tree.
      current = colorFlip(current);
    }

    return current;
  }

  /**
   * Flip the colors of current and its two children. This is an operation that keeps the "black
   * height".
   *
   * @param current A node with two children.
   * @return The same subtree, but with inverted colors for the three top nodes.
   */
  private static <K extends Comparable<K>, V> Node<K, V> colorFlip(Node<K, V> current) {
    Node<K, V> newLeft = current.left.withColor(!current.left.getColor());
    Node<K, V> newRight = current.right.withColor(!current.right.getColor());
    return new Node<>(
        current.getKey(),
        current.getSecondaryKey(),
        current.getMax(),
        current.getValue(),
        newLeft,
        newRight,
        !current.getColor());
  }

  private static <K extends Comparable<K>, V> Node<K, V> rotateCounterclockwise(
      Node<K, V> current) {
    // the node that is moved between subtrees: (Its max doesn't change)
    Node<K, V> crossoverNode = current.right.left;
    // The final node will have the same max as current
    K oldMaxTop = current.getMax();
    // The new left is now the parent of crossover node, so the max has to be recalculated
    // TODO: this can be optimized
    K newMaxnewLeft = current.getMax();
    if (current.left != null && newMaxnewLeft.compareTo(current.left.getMax()) < 0) {
      newMaxnewLeft = current.left.getMax();
    }
    if (crossoverNode != null && newMaxnewLeft.compareTo(crossoverNode.getMax()) < 0) {
      newMaxnewLeft = crossoverNode.getMax();
    }
    Node<K, V> newLeft =
        new Node<>(
            current.getKey(),
            current.getSecondaryKey(),
            newMaxnewLeft,
            current.getValue(),
            current.left,
            crossoverNode,
            Node.RED);
    return new Node<>(
        current.right.getKey(),
        current.right.getSecondaryKey(),
        oldMaxTop,
        current.right.getValue(),
        newLeft,
        current.right.right,
        current.getColor());
  }

  private static <K extends Comparable<K>, V> Node<K, V> rotateClockwise(Node<K, V> current) {
    // the node that is moved between subtrees:
    Node<K, V> crossOverNode = current.left.right;
    // The final node will have the same max as current
    K oldMaxTop = current.getMax();
    // The new right is now the parent of crossover node, so the max has to be recalculated
    // TODO: this can be optimized
    K newMaxnewRight = current.getMax();
    if (current.right != null && newMaxnewRight.compareTo(current.right.getMax()) < 0) {
      newMaxnewRight = current.left.getMax();
    }
    if (crossOverNode != null && newMaxnewRight.compareTo(crossOverNode.getMax()) < 0) {
      newMaxnewRight = crossOverNode.getMax();
    }
    Node<K, V> newRight =
        new Node<>(
            current.getKey(),
            current.getSecondaryKey(),
            newMaxnewRight,
            current.getValue(),
            crossOverNode,
            current.right,
            Node.RED);
    return new Node<>(
        current.left.getKey(),
        current.left.getSecondaryKey(),
        oldMaxTop,
        current.left.getValue(),
        current.left.left,
        newRight,
        current.getColor());
  }

  private static <K extends Comparable<K>, V> Node<K, V> makeLeftRed(@Var Node<K, V> current) {
    // Make current.left or one of its children red
    // (assuming that current is red and both current.left and current.left.left are black).

    current = colorFlip(current);
    if (Node.isRed(current.right.left)) {
      Node<K, V> newRight = rotateClockwise(current.right);
      // Because only the children get moved, the max value will never change here!
      current =
          new Node<>(
              current.getKey(),
              current.getSecondaryKey(),
              current.getMax(),
              current.getValue(),
              current.left,
              newRight,
              current.getColor());

      current = rotateCounterclockwise(current);
      current = colorFlip(current);
    }
    return current;
  }

  private static <K extends Comparable<K>, V> Node<K, V> makeRightRed(@Var Node<K, V> current) {
    // Make current.right or one of its children red
    // (assuming that current is red and both current.right and current.right.left are black).

    current = colorFlip(current);
    if (Node.isRed(current.left.left)) {
      current = rotateClockwise(current);
      current = colorFlip(current);
    }
    return current;
  }

  // read operations

  @Override
  @SuppressWarnings("ReferenceEquality") // comparing nodes with equals would not suffice
  public boolean equals(@Nullable Object pObj) {
    if (pObj instanceof PathCopyingPersistentMaxTreeMap<?, ?>
        && ((PathCopyingPersistentMaxTreeMap<?, ?>) pObj).root == root) {
      return true;
    }
    return super.equals(pObj);
  }

  @Override
  @SuppressWarnings("RedundantOverride") // to document that using super.hashCode is intended
  public int hashCode() {
    return super.hashCode();
  }

  public @Nullable Entry<K, V> getEntry(K pKey) {
    return findNode(pKey, root);
  }

  public Iterator<Entry<K, V>> entryIterator() {
    return EntryInOrderIterator.create(root);
  }

  // TODO: Is this fine or would ImmutableSet.Builder be better?
  public Set<V> getValuesSet() {
    Iterable<Entry<K, V>> iterable = () -> entryIterator();

    return Streams.stream(iterable).map(n -> n.getValue()).collect(ImmutableSet.toImmutableSet());
  }

  public Iterator<Entry<K, V>> descendingEntryIterator() {
    return DescendingEntryInOrderIterator.create(root);
  }

  @SuppressWarnings("unchecked")
  public PathCopyingPersistentMaxTreeMap<K, V> empty() {
    return (PathCopyingPersistentMaxTreeMap<K, V>) of();
  }

  public boolean containsKey(K pObj) {
    return findNode(pObj, root) != null;
  }

  /**
   * @param key The key to the value.
   * @param value The value that has key key.
   * @return True if it contains the value. False else.
   */
  public boolean contains(K key, V value) {
    @Nullable Node<K, V> node = findNode(key, root);
    return node != null && node.getValue() == value;
  }

  /**
   * @param pObj the KEY of the value searched for.
   * @return The value to the key entered or null if not present.
   */
  public V get(K pObj) {
    Node<K, V> node = findNode(pObj, root);
    return node == null ? null : node.getValue();
  }

  /* This seems wierd, but is exactly what we need in SMGs
   * Its basically:
   *   value ->
   *   value.hasValue().isZero()
   *   && offsetPlusSize.compareTo(value.getOffset()) >= 0
   *   && offset.compareTo(value.getOffset().add(value.getSizeInBits())) <= 0)
   *
   *   with offset and offsetPlusSize being the input; value being the saved values in this map.
   *   offsetPlusSize = largerEqualsKey
   *   offset = lessEqualsMax
   */
  public ImmutableSet<V> filterByOverlappingZeroEdges(K largerEqualsKey, K lessEqualsMax) {
    ImmutableSet.Builder<V> setBuilder = ImmutableSet.builder();
    filterByThingy1(largerEqualsKey, lessEqualsMax, SMGValue.zeroValue(), root, setBuilder);
    return setBuilder.build();
  }

  private void filterByThingy1(
      K offSetPlusSize,
      K offset,
      Object valueEqualityConstraint,
      Node<K, V> node,
      ImmutableSet.Builder<V> builder) {
    if (node == null) {
      return;
    }
    int compare = node.getKey().compareTo(offSetPlusSize);
    if (compare < 0) {
      // If the current node is smaller than the offSetPlusSize we have to check it further and
      // search left and right

      if (node.getMax().compareTo(offset) > 0) {
        // if the maximum offsetPlusSize in this tree is larger than the offset we have to check
        // this + the subtrees

        // check current node equality+size (again, but proper value of this node this time) and
        // equality constraint
        if (((SMGHasValueEdge) node.getValue()).hasValue().equals(valueEqualityConstraint)
            && node.getSecondaryKey().compareTo(offset) > 0) {
          builder.add(node.getValue());
        }
        filterByThingy1(offSetPlusSize, offset, valueEqualityConstraint, node.left, builder);
        filterByThingy1(offSetPlusSize, offset, valueEqualityConstraint, node.right, builder);
      }
      // If the max (offsetPlusSize of this subtree) is less than the offset,
      // none of the subtrees will ever be included -> stop search

    } else if (compare > 0 && node.getMax().compareTo(offset) > 0) {
      // In this case we have to go only left, and not include the current
      filterByThingy1(offSetPlusSize, offset, valueEqualityConstraint, node.left, builder);

    } else if (node.getMax().compareTo(offset) > 0) {
      // == case for the not checked predicate; Check second predicate;
      // We may include the current, but go only left

      if (((SMGHasValueEdge) node.getValue()).hasValue().equals(valueEqualityConstraint)
          && node.getSecondaryKey().compareTo(offset) > 0) {
        builder.add(node.getValue());
      }
      filterByThingy1(offSetPlusSize, offset, valueEqualityConstraint, node.left, builder);
    }
  }

  /* This seems wierd, but is exactly what we need in SMGs
   * Its basically:
   *   value ->
   *   !value.hasValue().isZero()
   *   && offsetPlusSize.compareTo(value.getOffset()) >= 0
   *   && offset.compareTo(value.getOffset().add(value.getSizeInBits())) <= 0)
   *   TODO: i think it should be offset.compareTo(value.getOffset().add(value.getSizeInBits())) < 0
   *
   *   with offset and offsetPlusSize being the input; value being the saved values in this map.
   *   offsetPlusSize = largerEqualsKey
   *   offset = lessEqualsMax
   */
  public ImmutableSet<V> filterByOverlappingNonZeroEdges(K largerEqualsKey, K lessEqualsMax) {
    ImmutableSet.Builder<V> setBuilder = ImmutableSet.builder();
    filterByThingy2(largerEqualsKey, lessEqualsMax, root, setBuilder);
    return setBuilder.build();
  }

  private void filterByThingy2(
      K offSetPlusSize, K offset, Node<K, V> node, ImmutableSet.Builder<V> builder) {
    if (node == null) {
      return;
    }
    int compare = node.getKey().compareTo(offSetPlusSize);
    if (compare < 0) {
      // If the current node is smaller than the offSetPlusSize we have to check it further and
      // search left and right

      if (node.getMax().compareTo(offset) > 0) {
        // if the maximum offsetPlusSize in this tree is larger than the offset we have to check
        // this + the subtrees

        // check current node equality+size (again, but proper value of this node this time) and
        // equality constraint
        if (((SMGHasValueEdge) node.getValue()).hasValue() != SMGValue.zeroValue()
            && node.getSecondaryKey().compareTo(offset) > 0) {
          builder.add(root.getValue());
        }
        filterByThingy2(offSetPlusSize, offset, node.left, builder);
        filterByThingy2(offSetPlusSize, offset, node.right, builder);
      }
      // If the max (offsetPlusSize of this subtree) is less than the offset,
      // none of the subtrees will ever be included -> stop search

    } else if (compare > 0 && node.getMax().compareTo(offset) > 0) {
      // In this case we have to go only left, and not include the current
      filterByThingy2(offSetPlusSize, offset, node.left, builder);

    } else if (node.getMax().compareTo(offset) > 0) {
      // == case for the not checked predicate; Check second predicate;
      // We may include the current, but go only left

      if (((SMGHasValueEdge) node.getValue()).hasValue() != SMGValue.zeroValue()
          && node.getSecondaryKey().compareTo(offset) > 0) {
        builder.add(root.getValue());
      }
      filterByThingy2(offSetPlusSize, offset, node.left, builder);
    }
  }

  @SuppressWarnings("unused")
  public void deleteOverlappingNonZeroEdges(
      K largerEqualsKey, K lessEqualsMax, Object valueEqualityConstraint) {
    // TODO: by doing this directly without filter -> delete, we could improve the runtime further.
  }

  public PersistentSortedMap<K, V> filterToSortedMap(
      K largerEqualsKey, K lessEqualsMax, Object valueEqualityConstraint) {
    PersistentSortedMap<K, V> map = PathCopyingPersistentTreeMap.<K, V>of();
    map = filterToSortedMap0(largerEqualsKey, lessEqualsMax, valueEqualityConstraint, root, map);
    return map;
  }

  private PersistentSortedMap<K, V> filterToSortedMap0(
      K offSetPlusSize,
      K offset,
      Object valueEqualityConstraint,
      Node<K, V> node,
      PersistentSortedMap<K, V> map) {
    if (node == null) {
      return map;
    }
    int compare = node.getKey().compareTo(offSetPlusSize);
    if (compare < 0) {
      // If the current node is smaller than the offSetPlusSize we have to check it further and
      // search left and right

      if (node.getMax().compareTo(offset) > 0) {
        // if the maximum offsetPlusSize in this tree is larger than the offset we have to check
        // this + the subtrees

        // check current node equality+size (again, but proper value of this node this time) and
        // equality constraint
        if (((SMGHasValueEdge) node.getValue()).hasValue() == valueEqualityConstraint
            && node.getSecondaryKey().compareTo(offset) > 0) {
          map = map.putAndCopy(node.getKey(), node.getValue());
        }
        map = filterToSortedMap0(offSetPlusSize, offset, valueEqualityConstraint, node.left, map);
        return filterToSortedMap0(offSetPlusSize, offset, valueEqualityConstraint, node.right, map);
      }
      // If the max (offsetPlusSize of this subtree) is less than the offset,
      // none of the subtrees will ever be included -> stop search

    } else if (compare > 0 && node.getMax().compareTo(offset) > 0) {
      // In this case we have to go only left, and not include the current
      return filterToSortedMap0(offSetPlusSize, offset, valueEqualityConstraint, node.left, map);

    } else if (node.getMax().compareTo(offset) > 0) {
      // == case for the not checked predicate; Check second predicate;
      // We may include the current, but go only left

      if (node.getValue() == valueEqualityConstraint
          && node.getSecondaryKey().compareTo(offset) > 0) {
        map = map.putAndCopy(node.getKey(), node.getValue());
      }
      return filterToSortedMap0(offSetPlusSize, offset, valueEqualityConstraint, node.left, map);
    }
    return map;
  }

  public V getOrDefault(K pKey, V pDefaultValue) {
    Node<K, V> node = findNode(pKey, root);
    return node == null ? pDefaultValue : node.getValue();
  }

  public boolean isEmpty() {
    return root == null;
  }

  public int size() {
    if (size <= 0) {
      size = Node.countNodes(root);
    }
    return size;
  }

  @Nullable
  public Entry<K, V> firstEntry() {
    if (isEmpty()) {
      return null;
    }

    return findSmallestNode(root);
  }

  @Nullable
  public Entry<K, V> lastEntry() {
    if (isEmpty()) {
      return null;
    }

    return findLargestNode(root);
  }

  public Entry<K, V> ceilingEntry(K pKey) {
    return findNextGreaterNode(pKey, root, /*inclusive=*/ true);
  }

  public Entry<K, V> floorEntry(K pKey) {
    return findNextSmallerNode(pKey, root, /*inclusive=*/ true);
  }

  public Entry<K, V> higherEntry(K pKey) {
    return findNextGreaterNode(pKey, root, /*inclusive=*/ false);
  }

  public Entry<K, V> lowerEntry(K pKey) {
    return findNextSmallerNode(pKey, root, /*inclusive=*/ false);
  }

  public Comparator<? super K> comparator() {
    return null;
  }
  /* TODO:
    public OurSortedMap<K, V> descendingMap() {
      return new DescendingSortedMap<>(this);
    }

    public NavigableSet<Entry<K, V>> entrySet() {
      if (entrySet == null) {
        entrySet = new SortedMapEntrySet<>(this);
      }
      return entrySet;
    }
  */
  public OurSortedMap<K, V> subMap(
      K pFromKey, boolean pFromInclusive, K pToKey, boolean pToInclusive) {
    checkNotNull(pFromKey);
    checkNotNull(pToKey);

    return PartialSortedMap.create(root, pFromKey, pFromInclusive, pToKey, pToInclusive);
  }

  public OurSortedMap<K, V> headMap(K pToKey, boolean pToInclusive) {
    checkNotNull(pToKey);

    return PartialSortedMap.create(
        root, null, /*pFromInclusive=*/ true, pToKey, /*pToInclusive=*/ pToInclusive);
  }

  public OurSortedMap<K, V> tailMap(K pFromKey, boolean pFromInclusive) {
    checkNotNull(pFromKey);

    return PartialSortedMap.create(
        root, pFromKey, /*pFromInclusive=*/ pFromInclusive, null, /*pToInclusive=*/ false);
  }

  /**
   * Tree iterator with in-order iteration returning node objects, with possibility for lower and
   * upper bound.
   *
   * @param <K> The type of keys.
   * @param <V> The type of values.
   */
  private static class EntryInOrderIterator<K extends Comparable<K>, V>
      extends UnmodifiableIterator<Map.Entry<K, V>> {

    // invariants:
    // stack.top is always the next element to be returned
    // (i.e., its left subtree has already been handled)

    private final Deque<Node<K, V>> stack;

    // If not null, iteration stops at this key.
    private final @Nullable K highKey;
    private final boolean highInclusive; // only relevant if highKey != null

    static <K extends Comparable<K>, V> Iterator<Map.Entry<K, V>> create(
        @Nullable Node<K, V> root) {
      if (root == null) {
        return Collections.emptyIterator();
      } else {
        return new EntryInOrderIterator<>(
            root, null, /*pLowInclusive=*/ false, null, /*pHighInclusive=*/ false);
      }
    }

    /**
     * Create a new iterator with an optional lower and upper bound.
     *
     * @param pFromKey null or inclusive lower bound
     * @param pToKey null or exclusive lower bound
     */
    static <K extends Comparable<K>, V> Iterator<Map.Entry<K, V>> createWithBounds(
        @Nullable Node<K, V> root,
        @Nullable K pFromKey,
        boolean pFromInclusive,
        @Nullable K pToKey,
        boolean pToInclusive) {
      if (root == null) {
        return Collections.emptyIterator();
      } else {
        return new EntryInOrderIterator<>(root, pFromKey, pFromInclusive, pToKey, pToInclusive);
      }
    }

    private EntryInOrderIterator(
        Node<K, V> root,
        @Nullable K pLowKey,
        boolean pLowInclusive,
        @Nullable K pHighKey,
        boolean pHighInclusive) {
      stack = new ArrayDeque<>();
      highKey = pHighKey;
      highInclusive = pHighInclusive;

      if (pLowKey == null) {
        pushLeftMostNodesOnStack(root);
      } else {
        // TODO: optimize: this iterates twice through the tree
        pushNodesToKeyOnStack(root, findNextGreaterNode(pLowKey, root, pLowInclusive).getKey());
      }
      stopFurtherIterationIfOutOfRange();
    }

    @Override
    public boolean hasNext() {
      return !stack.isEmpty();
    }

    private void pushLeftMostNodesOnStack(@Var Node<K, V> current) {
      while (current.left != null) {
        stack.push(current);
        current = current.left;
      }
      stack.push(current);
    }

    private void pushNodesToKeyOnStack(@Var Node<K, V> current, K key) {
      while (current != null) {
        int comp = key.compareTo(current.getKey());

        if (comp < 0) {
          stack.push(current);
          current = current.left;

        } else if (comp > 0) {
          // This node and it's left subtree can be ignored completely.
          current = current.right;

        } else {
          stack.push(current);
          return;
        }
      }
      throw new AssertionError(
          "PartialEntryInOrderIterator created with lower bound that is not in map");
    }

    private void stopFurtherIterationIfOutOfRange() {
      if (highKey != null
          && !stack.isEmpty()
          && exceedsUpperBound(stack.peek().getKey(), highKey, highInclusive)) {
        // We have reached the end, next element would already be too large
        stack.clear();
      }
    }

    @Override
    public Map.Entry<K, V> next() {
      Node<K, V> current = stack.pop();
      // this is the element to be returned

      // if it has a right subtree,
      // push it on stack so that it will be handled next
      if (current.right != null) {
        pushLeftMostNodesOnStack(current.right);
      }

      stopFurtherIterationIfOutOfRange();

      return current;
    }
  }

  /**
   * Reverse tree iterator with in-order iteration returning node objects, with possibility for
   * lower and upper bound. The iteration starts at the upper bound and goes to the lower bound.
   *
   * @param <K> The type of keys.
   * @param <V> The type of values.
   */
  private static class DescendingEntryInOrderIterator<K extends Comparable<K>, V>
      extends UnmodifiableIterator<Map.Entry<K, V>> {

    // invariants:
    // stack.top is always the next element to be returned
    // (i.e., its right subtree has already been handled)

    private final Deque<Node<K, V>> stack;

    // If not null, iteration stops at this key.
    private final @Nullable K lowKey;
    private final boolean lowInclusive; // only relevant if lowKey != null

    static <K extends Comparable<K>, V> Iterator<Map.Entry<K, V>> create(
        @Nullable Node<K, V> root) {
      if (root == null) {
        return Collections.emptyIterator();
      } else {
        return new DescendingEntryInOrderIterator<>(
            root, null, /*pLowInclusive=*/ false, null, /*pHighInclusive=*/ false);
      }
    }

    /**
     * Create a new iterator with an optional lower and upper bound.
     *
     * @param pFromKey null or inclusive lower bound
     * @param pToKey null or exclusive lower bound
     */
    static <K extends Comparable<K>, V> Iterator<Map.Entry<K, V>> createWithBounds(
        @Nullable Node<K, V> root,
        @Nullable K pFromKey,
        boolean pFromInclusive,
        @Nullable K pToKey,
        boolean pToInclusive) {
      if (root == null) {
        return Collections.emptyIterator();
      } else {
        return new DescendingEntryInOrderIterator<>(
            root, pFromKey, pFromInclusive, pToKey, pToInclusive);
      }
    }

    private DescendingEntryInOrderIterator(
        Node<K, V> root,
        @Nullable K pLowKey,
        boolean pLowInclusive,
        @Nullable K pHighKey,
        boolean pHighInclusive) {
      stack = new ArrayDeque<>();
      lowKey = pLowKey;
      lowInclusive = pLowInclusive;

      if (pHighKey == null) {
        pushRightMostNodesOnStack(root);
      } else {
        // TODO: optimize: this iterates twice through the tree
        pushNodesToKeyOnStack(root, findNextSmallerNode(pHighKey, root, pHighInclusive).getKey());
      }
      stopFurtherIterationIfOutOfRange();
    }

    @Override
    public boolean hasNext() {
      return !stack.isEmpty();
    }

    private void pushRightMostNodesOnStack(@Var Node<K, V> current) {
      while (current.right != null) {
        stack.push(current);
        current = current.right;
      }
      stack.push(current);
    }

    private void pushNodesToKeyOnStack(@Var Node<K, V> current, K key) {
      while (current != null) {
        int comp = key.compareTo(current.getKey());

        if (comp > 0) {
          stack.push(current);
          current = current.right;

        } else if (comp < 0) {
          // This node and it's right subtree can be ignored completely.
          current = current.left;

        } else {
          stack.push(current);
          return;
        }
      }
      throw new AssertionError(
          "PartialEntryInOrderIterator created with upper bound that is not in map");
    }

    private void stopFurtherIterationIfOutOfRange() {
      if (lowKey != null
          && !stack.isEmpty()
          && exceedsLowerBound(stack.peek().getKey(), lowKey, lowInclusive)) {
        // We have reached the end, next element would already be too small
        stack.clear();
      }
    }

    @Override
    public Map.Entry<K, V> next() {
      Node<K, V> current = stack.pop();
      // this is the element to be returned

      // if it has a left subtree,
      // push it on stack so that it will be handled next
      if (current.left != null) {
        pushRightMostNodesOnStack(current.left);
      }

      stopFurtherIterationIfOutOfRange();

      return current;
    }
  }

  /**
   * Partial map implementation for {@link SortedMap#subMap(Object, Object)} etc. At least one bound
   * (upper/lower) needs to be present. The range needs to contain at least one mapping.
   *
   * @param <K> The type of keys.
   * @param <V> The type of values.
   */
  @Immutable(containerOf = {"K", "V"})
  private static final class PartialSortedMap<K extends Comparable<K>, V>
      extends AbstractImmutableSortedMap<K, V> implements OurSortedMap<K, V>, Serializable {

    static <K extends Comparable<K>, V> OurSortedMap<K, V> create(
        Node<K, V> pRoot,
        @Nullable K pFromKey,
        boolean pFromInclusive,
        @Nullable K pToKey,
        boolean pToInclusive) {
      checkArgument(pFromKey != null || pToKey != null);

      if (pFromKey != null && pToKey != null) {
        int comp = pFromKey.compareTo(pToKey);
        if (comp == 0 && (!pFromInclusive || !pToInclusive)) {
          return EmptyImmutableOurSortedMap.<K, V>of();
        }
        checkArgument(comp <= 0, "fromKey > toKey");
      }

      Node<K, V> root = findBestRoot(pRoot, pFromKey, pFromInclusive, pToKey, pToInclusive);
      if (root == null) {
        return EmptyImmutableOurSortedMap.<K, V>of();
      }

      @Var Node<K, V> lowestNode = null;
      if (pFromKey != null) {
        lowestNode = findNextGreaterNode(pFromKey, root, pFromInclusive);
        if (lowestNode == null) {
          return EmptyImmutableOurSortedMap.<K, V>of();
        }
      }

      @Var Node<K, V> highestNode = null;
      if (pToKey != null) {
        highestNode = findNextSmallerNode(pToKey, root, pToInclusive);
        if (highestNode == null) {
          return EmptyImmutableOurSortedMap.<K, V>of();
        }
      }

      if (pFromKey != null && pToKey != null) {
        assert lowestNode != null && highestNode != null;
        if (exceedsUpperBound(lowestNode.getKey(), pToKey, pToInclusive)) {
          verify(exceedsLowerBound(highestNode.getKey(), pFromKey, pFromInclusive));

          // no mappings in in range
          return EmptyImmutableOurSortedMap.<K, V>of();
        }
      }

      return new PartialSortedMap<>(root, pFromKey, pFromInclusive, pToKey, pToInclusive);
    }

    // Find the best root for a given set of bounds
    // (the lowest node in the tree that represents the complete range).
    // Not using root directly but potentially only a subtree is more efficient.
    private static @Nullable <K extends Comparable<K>, V> Node<K, V> findBestRoot(
        @Nullable Node<K, V> pRoot,
        @Nullable K pFromKey,
        boolean pFromInclusive,
        @Nullable K pToKey,
        boolean pToInclusive) {

      @Var Node<K, V> current = pRoot;
      while (current != null) {

        if (pFromKey != null && exceedsLowerBound(current.getKey(), pFromKey, pFromInclusive)) {
          // current and left subtree can be ignored
          current = current.right;
        } else if (pToKey != null && exceedsUpperBound(current.getKey(), pToKey, pToInclusive)) {
          // current and right subtree can be ignored
          current = current.left;
        } else {
          // current is in range
          return current;
        }
      }

      return null; // no mapping in range
    }

    private static final long serialVersionUID = 5354023186935889223L;

    // Invariant: This map is never empty.

    private final Node<K, V> root;

    // null if there is no according bound, in this case the "inclusive" boolean is irrelevant
    private final @Nullable K fromKey;
    private final boolean fromInclusive;
    private final @Nullable K toKey;
    private final boolean toInclusive;

    @LazyInit private transient int size;

    @SuppressWarnings("Immutable")
    @LazyInit
    private transient @Nullable NavigableSet<Entry<K, V>> entrySet;

    private PartialSortedMap(
        Node<K, V> pRoot,
        @Nullable K pFromKey,
        boolean pFromInclusive,
        @Nullable K pToKey,
        boolean pToInclusive) {
      root = pRoot;
      fromKey = pFromKey;
      fromInclusive = pFromInclusive;
      toKey = pToKey;
      toInclusive = pToInclusive;

      // check non-emptiness invariant
      assert root != null;
      assert pFromKey == null
          || containsKey(findNextGreaterNode(pFromKey, pRoot, fromInclusive).getKey());
      assert pToKey == null
          || containsKey(findNextSmallerNode(pToKey, pRoot, toInclusive).getKey());
    }

    private boolean inRange(K key, boolean treatBoundsAsInclusive) {
      return !tooLow(key, treatBoundsAsInclusive) && !tooHigh(key, treatBoundsAsInclusive);
    }

    private boolean tooLow(K key, boolean treatBoundAsInclusive) {
      return fromKey != null
          && exceedsLowerBound(key, fromKey, treatBoundAsInclusive || fromInclusive);
    }

    private boolean tooHigh(K key, boolean treatBoundAsInclusive) {
      return toKey != null && exceedsUpperBound(key, toKey, treatBoundAsInclusive || toInclusive);
    }

    @Override
    public Iterator<Entry<K, V>> entryIterator() {
      return EntryInOrderIterator.createWithBounds(
          root, fromKey, fromInclusive, toKey, toInclusive);
    }

    @Override
    public Iterator<Entry<K, V>> descendingEntryIterator() {
      return DescendingEntryInOrderIterator.createWithBounds(
          root, fromKey, fromInclusive, toKey, toInclusive);
    }

    @Override
    @SuppressWarnings("ReferenceEquality") // comparing nodes with equals would not suffice
    public boolean equals(@Nullable Object pObj) {
      if (pObj instanceof PartialSortedMap<?, ?>) {
        PartialSortedMap<?, ?> other = (PartialSortedMap<?, ?>) pObj;
        if (root == other.root
            && Objects.equals(fromKey, other.fromKey)
            && fromInclusive == other.fromInclusive
            && Objects.equals(toKey, other.toKey)
            && toInclusive == other.toInclusive) {
          return true;
        }
      }
      return super.equals(pObj);
    }

    @Override
    @SuppressWarnings("RedundantOverride") // to document that using super.hashCode is intended
    public int hashCode() {
      return super.hashCode();
    }

    @Override
    public boolean containsKey(Object pKey) {
      @SuppressWarnings("unchecked")
      K key = (K) checkNotNull(pKey);
      return inRange(key, /*treatBoundsAsInclusive=*/ false) && findNode(key, root) != null;
    }

    @Nullable
    @Override
    public Node<K, V> getEntry(Object pKey) {
      @SuppressWarnings("unchecked")
      K key = (K) checkNotNull(pKey);
      if (!inRange(key, /*treatBoundsAsInclusive=*/ false)) {
        return null;
      }
      Node<K, V> node = findNode(key, root);
      return node;
    }

    @Override
    public V get(Object pKey) {
      Node<K, V> node = getEntry(pKey);
      return node == null ? null : node.getValue();
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public int size() {
      if (size == 0) {
        size = Iterators.size(entryIterator());
      }
      return size;
    }

    @Override
    public Entry<K, V> firstEntry() {
      if (fromKey == null) {
        return findSmallestNode(root);
      } else {
        return findNextGreaterNode(fromKey, root, fromInclusive);
      }
    }

    @Override
    public Entry<K, V> lastEntry() {
      if (toKey == null) {
        return findLargestNode(root);
      } else {
        return findNextSmallerNode(toKey, root, toInclusive);
      }
    }

    @Nullable
    @Override
    public Entry<K, V> ceilingEntry(K pKey) {
      Entry<K, V> result = findNextGreaterNode(pKey, root, /*inclusive=*/ true);
      if (result != null && !inRange(result.getKey(), /*treatBoundsAsInclusive=*/ false)) {
        return null;
      }
      return result;
    }

    @Nullable
    @Override
    public Entry<K, V> floorEntry(K pKey) {
      Entry<K, V> result = findNextSmallerNode(pKey, root, /*inclusive=*/ true);
      if (result != null && !inRange(result.getKey(), /*treatBoundsAsInclusive=*/ false)) {
        return null;
      }
      return result;
    }

    @Nullable
    @Override
    public Entry<K, V> higherEntry(K pKey) {
      Entry<K, V> result = findNextGreaterNode(pKey, root, /*inclusive=*/ false);
      if (result != null && !inRange(result.getKey(), /*treatBoundsAsInclusive=*/ false)) {
        return null;
      }
      return result;
    }

    @Nullable
    @Override
    public Entry<K, V> lowerEntry(K pKey) {
      Entry<K, V> result = findNextSmallerNode(pKey, root, /*inclusive=*/ false);
      if (result != null && !inRange(result.getKey(), /*treatBoundsAsInclusive=*/ false)) {
        return null;
      }
      return result;
    }

    @Override
    public Comparator<? super K> comparator() {
      return null;
    }

    @Override
    public OurSortedMap<K, V> descendingMap() {
      return new DescendingSortedMap<>(this);
    }

    @Override
    public NavigableSet<Entry<K, V>> entrySet() {
      if (entrySet == null) {
        entrySet = new SortedMapEntrySet<>(this);
      }
      return entrySet;
    }

    @Override
    public OurSortedMap<K, V> subMap(
        K pFromKey, boolean pFromInclusive, K pToKey, boolean pToInclusive) {
      checkNotNull(pFromKey);
      checkNotNull(pToKey);

      // If fromKey==pFromKey, we must forbid the combination of fromInclusive==false and
      // pFromInclusive==true, because this would mean that the new range exceeds the old range.
      // All other combinations of fromInclusive and pFromInclusive are allowed.
      // So we accept equal keys if (!pFromInclusive || fromInclusive) holds,
      // the latter being handled by inRange().
      checkArgument(inRange(pFromKey, !pFromInclusive));
      // Similarly for the upper bound.
      checkArgument(inRange(pToKey, !pToInclusive));

      return PartialSortedMap.create(root, pFromKey, pFromInclusive, pToKey, pToInclusive);
    }

    @Override
    public OurSortedMap<K, V> headMap(K pToKey, boolean pInclusive) {
      checkNotNull(pToKey);
      checkArgument(inRange(pToKey, /*treatBoundsAsInclusive=*/ !pInclusive));

      return PartialSortedMap.create(
          root, fromKey, /*pFromInclusive=*/ fromInclusive, pToKey, /*pToInclusive=*/ pInclusive);
    }

    @Override
    public OurSortedMap<K, V> tailMap(K pFromKey, boolean pInclusive) {
      checkNotNull(pFromKey);
      checkArgument(inRange(pFromKey, /*treatBoundsAsInclusive=*/ !pInclusive));

      return PartialSortedMap.create(
          root, pFromKey, /*pFromInclusive=*/ pInclusive, toKey, /*pToInclusive=*/ toInclusive);
    }
  }

  @Override
  public String toString() {
    return toString(root);
  }

  private String toString(Node<K, V> pCurrent) {
    if (pCurrent == null) {
      return "";
    }
    String thisNode = "(max: " + pCurrent.getMax() + "; Value: " + pCurrent.getValue() + ")";
    return toString(pCurrent.left) + thisNode + toString(pCurrent.right);
  }

  String printer(Node<?, ?> pCurrent) {
    if (pCurrent == null) {
      return "";
    }
    String thisNode = "(max: " + pCurrent.getMax() + "; Value: " + pCurrent.getValue() + ")";
    return printer(pCurrent.left) + thisNode + printer(pCurrent.right);
  }
}
