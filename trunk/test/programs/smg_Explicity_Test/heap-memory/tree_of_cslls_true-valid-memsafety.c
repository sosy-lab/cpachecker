extern void __VERIFIER_error() __attribute__ ((__noreturn__));

/*
 * A tree of circular singly linked lists
 *
 * This source code is licensed under the GPLv3 license.
 *
 * Taken from Forester.
 */

#include <stdlib.h>

typedef struct TListNode
{
	struct TListNode* next;
} ListNode;

typedef struct TTreeNode
{
	struct TTreeNode* left;
	struct TTreeNode* right;
	ListNode* list;
} TreeNode;

int main()
{
	TreeNode* tree = malloc(sizeof(*tree));
	TreeNode* tmp;
	ListNode* tmpList;

	tree->left  = NULL;
	tree->right = NULL;
	tree->list = malloc(sizeof(ListNode));
	tree->list->next = tree->list;

        int c = 0;

	while (c < 2)
	{
                c++;
		tmpList = malloc(sizeof(ListNode));
		tmpList->next = tree->list->next;
		tree->list->next = tmpList;
	}

        int d = 0;

	while (d < 2 && __VERIFIER_nondet_int())
	{
                d++;
		tmp = tree;

		while ((NULL != tmp->left) && (NULL != tmp->right))
		{
			if (__VERIFIER_nondet_int())
			{
				tmp = tmp->left;
			}
			else
			{
				tmp = tmp->right;
			}
		}

		TreeNode* newNode;
		if ((NULL == tmp->left) && __VERIFIER_nondet_int())
		{
			newNode = malloc(sizeof(*newNode));
			tmp->left = newNode;
		}
		else if ((NULL == tmp->right) && __VERIFIER_nondet_int())
		{
			newNode = malloc(sizeof(*newNode));
			tmp->right = newNode;
		}
		else
		{
			continue;
		}

		newNode->left = NULL;
		newNode->right = NULL;
		newNode->list = malloc(sizeof(*newNode->list));
		newNode->list->next = newNode->list;

                int e = 0;
 
		while (e < 2)
		{
                        e++;
			tmpList = malloc(sizeof(ListNode));
			tmpList->next = tree->list->next;
			tree->list->next = tmpList;
		}
	}

	while (NULL != tree)
	{	// while there are still some remains of the tree
		tmp = tree;
		TreeNode* pred = NULL;

		while ((NULL != tmp->left) || (NULL != tmp->right))
		{
			pred = tmp;
			if (NULL != tmp->left)
			{
				tmp = tmp->left;
			}
			else
			{
				tmp = tmp->right;
			}
		}

		if (NULL != pred)
		{
			if (tmp == pred->left)
			{
				pred->left = NULL;
			}
			else
			{
				pred->right = NULL;
			}
		}
		else
		{
			tree = NULL;
		}

		while (tmp->list != tmp->list->next)
		{
			tmpList = tmp->list->next;
			tmp->list->next = tmpList->next;
			free(tmpList);
		}

		free(tmp->list);
		free(tmp);
	}

	return 0;
}
