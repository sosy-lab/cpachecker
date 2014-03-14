extern void __VERIFIER_error() __attribute__ ((__noreturn__));

/*
 * This source code is licensed under the GPLv3 license.
 *
 * Taken from Forester.
 */

#include <stdlib.h>

int main() {

	struct TreeNode {
		struct TreeNode* left;
		struct TreeNode* right;
	};

	struct TreeNode* root = malloc(sizeof(*root)), *n;
	root->left = NULL;
	root->right = NULL;


        int c = 0;

	while (c < 2 && __VERIFIER_nondet_int()) {
                c++;
		n = root;
		while (n->left && n->right) {
			if (__VERIFIER_nondet_int())
				n = n->left;
			else
				n = n->right;
		}
		if (!n->left && __VERIFIER_nondet_int()) {
			n->left = malloc(sizeof(*n));
			n->left->left = NULL;
			n->left->right = NULL;
		}
		if (!n->right && __VERIFIER_nondet_int()) {
			n->right = malloc(sizeof(*n));
			n->right->left = NULL;
			n->right->right = NULL;
		}
	}

	n = NULL;

	struct TreeNode* pred;

	while (root) {
		pred = NULL;
		n = root;
		while (n->left || n->right) {
			pred = n;
			if (n->left)
				n = n->left;
			else
				n = n->right;
		}
		if (pred) {
			if (n == pred->left)
				pred->left = NULL;
			else
				pred->right = NULL;
		} else
			root = NULL;
		free(n);
	}

	return 0;

}
