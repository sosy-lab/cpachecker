// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <stdbool.h>

struct treenode;

typedef struct treenode {
  struct treenode *left;
  struct treenode *right;
} treenode;

extern bool __VERIFIER_nondet_bool();

void append(treenode *leaf, treenode **root) {
  treenode** current_ptr = root;
  while (*current_ptr != NULL){
    if (__VERIFIER_nondet_bool()) {
      current_ptr = &((*current_ptr)->left);
    } else {
      current_ptr = &((*current_ptr)->right);
    }
  }

  *current_ptr = leaf;
}

int main() {

  treenode* tree_root = NULL;

  treenode* node = malloc(sizeof(treenode));
  node->left = NULL;
  node->right = NULL;
  append(node, &tree_root);

  for (int i=0; i < 5; i++) {
    node = malloc(sizeof(treenode));
    node->left = NULL;
    node->right = NULL;
    append(node, &tree_root);
  }

  while (__VERIFIER_nondet_bool()) {
    node = malloc(sizeof(treenode));
    node->left = NULL;
    node->right = NULL;
    append(node, &tree_root);
  }
}
