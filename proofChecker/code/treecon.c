
/* implementation of tree construction functions */

#include "node.h"

#include "nodecode.h"

#include "attrpredef.h"

#include "visitmap.h"

#include "treeact.h"

#ifdef MONITOR
#include "attr_mon_dapto.h"
#include "MONTblStack.h"
#endif

#include <stdlib.h>

#define _USE_OBSTACK 1

/* use of obstack: */

#if _USE_OBSTACK

#include "obstack.h"
static struct obstack TreeSpace;
static void *_TreeBase;

#ifdef __cplusplus
void* NODEPTR_struct::operator new(size_t size)
{
	return obstack_alloc(&TreeSpace, size);
}
#else
#if defined(__STDC__) || defined(__cplusplus)
char* TreeNodeAlloc(int size)
#else
char* TreeNodeAlloc(size) int size;
#endif
{
	return (char *)(obstack_alloc(&TreeSpace, size));
}
#endif

void InitTree()
{
	obstack_init(&TreeSpace);
	_TreeBase=obstack_alloc(&TreeSpace,0);
}

void FreeTree()
{
	obstack_free(&TreeSpace, _TreeBase);
	_TreeBase=obstack_alloc(&TreeSpace,0);
}

#else

#include <stdio.h>

#ifdef __cplusplus
void* NODEPTR_struct::operator new(size_t size)
{
	void *retval = malloc(size);
	if (retval) return retval;
	fprintf(stderr, "*** DEADLY: No more memory.\n");
	exit(1);
}
#else
#if defined(__STDC__) || defined(__cplusplus)
char* TreeNodeAlloc(int size)
#else
char* TreeNodeAlloc(size) int size;
#endif
{
	char *retval = (char *) malloc(size);
	if (retval) return retval;
	fprintf(stderr, "*** DEADLY: No more memory.\n");
	exit(1);
}
#endif

void InitTree() { }

void FreeTree() { }

#endif

#ifdef MONITOR
#define _SETCOORD(node) \
        node->_coord = _coordref ? *_coordref : NoCoord;
#define _COPYCOORD(node) \
        node->_coord = _currn->_desc1->_coord;
#else
#define _SETCOORD(node)
#define _COPYCOORD(node)
#endif
#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkunary_operator (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkunary_operator (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBunary_operator)) return (_currn);
return(NULLNODEPTR);
}/* Mkunary_operator */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkshift_operator (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkshift_operator (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBshift_operator)) return (_currn);
return(NULLNODEPTR);
}/* Mkshift_operator */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mksource (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mksource (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBsource)) return (_currn);
if (IsSymb (_currn, SYMBdeclaration)) return (Mkrule_89(_coordref, _currn));
if (IsSymb (_currn, SYMBfunction_definition)) return (Mkrule_89(_coordref, _currn));
if (IsSymb (_currn, SYMBpre_include)) return (Mkrule_89(_coordref, _currn));
if (IsSymb (_currn, SYMBfile)) return (Mkrule_89(_coordref, _currn));
if (IsSymb (_currn, SYMBexternal_declaration)) return (Mkrule_89(_coordref, _currn));
if (IsSymb (_currn, SYMBtranslation_unit)) return (Mkrule_89(_coordref, _currn));
return(NULLNODEPTR);
}/* Mksource */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkroot (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkroot (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBroot)) return (_currn);
if (IsSymb (_currn, SYMBdeclaration)) return (Mkrule_85(_coordref, _currn));
if (IsSymb (_currn, SYMBfunction_definition)) return (Mkrule_85(_coordref, _currn));
if (IsSymb (_currn, SYMBpre_include)) return (Mkrule_85(_coordref, _currn));
if (IsSymb (_currn, SYMBfile)) return (Mkrule_85(_coordref, _currn));
if (IsSymb (_currn, SYMBsource)) return (Mkrule_85(_coordref, _currn));
if (IsSymb (_currn, SYMBexternal_declaration)) return (Mkrule_85(_coordref, _currn));
if (IsSymb (_currn, SYMBtranslation_unit)) return (Mkrule_85(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkroot */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkparameter (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkparameter (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBparameter)) return (_currn);
return(NULLNODEPTR);
}/* Mkparameter */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkparameter_list (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkparameter_list (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBparameter_list)) return (_currn);
if (IsSymb (_currn, SYMBparameter)) return (Mkrule_75(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkparameter_list */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkinitializer (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkinitializer (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBinitializer)) return (_currn);
if (IsSymb (_currn, SYMBinteger_constant)) return (Mkrule_64(_coordref, _currn));
if (IsSymb (_currn, SYMBconstant)) return (Mkrule_64(_coordref, _currn));
if (IsSymb (_currn, SYMBIdUse)) return (Mkrule_64(_coordref, _currn));
if (IsSymb (_currn, SYMBExpression)) return (Mkrule_64(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkinitializer */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkinit_declarator (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkinit_declarator (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBinit_declarator)) return (_currn);
return(NULLNODEPTR);
}/* Mkinit_declarator */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkinclusive_OR_operator (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkinclusive_OR_operator (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBinclusive_OR_operator)) return (_currn);
return(NULLNODEPTR);
}/* Mkinclusive_OR_operator */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mktranslation_unit (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mktranslation_unit (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBtranslation_unit)) return (_currn);
if (IsSymb (_currn, SYMBdeclaration)) return (Mkrule_90(_coordref, _currn));
if (IsSymb (_currn, SYMBfunction_definition)) return (Mkrule_90(_coordref, _currn));
if (IsSymb (_currn, SYMBpre_include)) return (Mkrule_90(_coordref, _currn));
if (IsSymb (_currn, SYMBexternal_declaration)) return (Mkrule_90(_coordref, _currn));
return(NULLNODEPTR);
}/* Mktranslation_unit */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkfile (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkfile (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBfile)) return (_currn);
if (IsSymb (_currn, SYMBdeclaration)) return (Mkrule_55(_coordref, _currn));
if (IsSymb (_currn, SYMBfunction_definition)) return (Mkrule_55(_coordref, _currn));
if (IsSymb (_currn, SYMBpre_include)) return (Mkrule_55(_coordref, _currn));
if (IsSymb (_currn, SYMBexternal_declaration)) return (Mkrule_55(_coordref, _currn));
if (IsSymb (_currn, SYMBtranslation_unit)) return (Mkrule_55(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkfile */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkpre_include (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkpre_include (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBpre_include)) return (_currn);
return(NULLNODEPTR);
}/* Mkpre_include */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkexternal_declaration (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkexternal_declaration (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBexternal_declaration)) return (_currn);
if (IsSymb (_currn, SYMBdeclaration)) return (Mkrule_52(_coordref, _currn));
if (IsSymb (_currn, SYMBfunction_definition)) return (Mkrule_53(_coordref, _currn));
if (IsSymb (_currn, SYMBpre_include)) return (Mkrule_54(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkexternal_declaration */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkexclusive_OR_operator (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkexclusive_OR_operator (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBexclusive_OR_operator)) return (_currn);
return(NULLNODEPTR);
}/* Mkexclusive_OR_operator */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkinit_declarator_list (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkinit_declarator_list (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBinit_declarator_list)) return (_currn);
if (IsSymb (_currn, SYMBinit_declarator)) return (Mkrule_62(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkinit_declarator_list */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkdeclaration (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkdeclaration (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBdeclaration)) return (_currn);
return(NULLNODEPTR);
}/* Mkdeclaration */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkinteger_constant (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkinteger_constant (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBinteger_constant)) return (_currn);
return(NULLNODEPTR);
}/* Mkinteger_constant */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkconstant (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkconstant (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBconstant)) return (_currn);
if (IsSymb (_currn, SYMBinteger_constant)) return (Mkrule_43(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkconstant */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkargument_expression_list (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkargument_expression_list (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBargument_expression_list)) return (_currn);
if (IsSymb (_currn, SYMBinteger_constant)) return (Mkrule_33(_coordref, _currn));
if (IsSymb (_currn, SYMBconstant)) return (Mkrule_33(_coordref, _currn));
if (IsSymb (_currn, SYMBIdUse)) return (Mkrule_33(_coordref, _currn));
if (IsSymb (_currn, SYMBExpression)) return (Mkrule_33(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkargument_expression_list */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkand_operator (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkand_operator (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBand_operator)) return (_currn);
return(NULLNODEPTR);
}/* Mkand_operator */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR MkBinOp (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR MkBinOp (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBBinOp)) return (_currn);
return(NULLNODEPTR);
}/* MkBinOp */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkargument_expression_list_opt (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkargument_expression_list_opt (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBargument_expression_list_opt)) return (_currn);
if (IsSymb (_currn, SYMBargument_expression_list)) return (Mkrule_35(_coordref, _currn));
if (IsSymb (_currn, SYMBinteger_constant)) return (Mkrule_35(_coordref, _currn));
if (IsSymb (_currn, SYMBconstant)) return (Mkrule_35(_coordref, _currn));
if (IsSymb (_currn, SYMBIdUse)) return (Mkrule_35(_coordref, _currn));
if (IsSymb (_currn, SYMBExpression)) return (Mkrule_35(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkargument_expression_list_opt */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR MkExpression (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR MkExpression (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBExpression)) return (_currn);
if (IsSymb (_currn, SYMBinteger_constant)) return (Mkrule_79(_coordref, _currn));
if (IsSymb (_currn, SYMBconstant)) return (Mkrule_79(_coordref, _currn));
if (IsSymb (_currn, SYMBIdUse)) return (Mkrule_80(_coordref, _currn));
return(NULLNODEPTR);
}/* MkExpression */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkfunction_call_expression (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkfunction_call_expression (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBfunction_call_expression)) return (_currn);
if (IsSymb (_currn, SYMBinteger_constant)) return (Mkrule_2(_coordref, _currn));
if (IsSymb (_currn, SYMBconstant)) return (Mkrule_2(_coordref, _currn));
if (IsSymb (_currn, SYMBIdUse)) return (Mkrule_2(_coordref, _currn));
if (IsSymb (_currn, SYMBExpression)) return (Mkrule_2(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkfunction_call_expression */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkassignment_operator (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkassignment_operator (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBassignment_operator)) return (_currn);
return(NULLNODEPTR);
}/* Mkassignment_operator */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR MkIdUse (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR MkIdUse (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBIdUse)) return (_currn);
return(NULLNODEPTR);
}/* MkIdUse */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkassignment_expression (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkassignment_expression (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBassignment_expression)) return (_currn);
if (IsSymb (_currn, SYMBinteger_constant)) return (Mkrule_7(_coordref, _currn));
if (IsSymb (_currn, SYMBconstant)) return (Mkrule_7(_coordref, _currn));
if (IsSymb (_currn, SYMBIdUse)) return (Mkrule_7(_coordref, _currn));
if (IsSymb (_currn, SYMBExpression)) return (Mkrule_7(_coordref, _currn));
if (IsSymb (_currn, SYMBfunction_call_expression)) return (Mkrule_7(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkassignment_expression */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkexpression_opt (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkexpression_opt (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBexpression_opt)) return (_currn);
if (IsSymb (_currn, SYMBinteger_constant)) return (Mkrule_9(_coordref, _currn));
if (IsSymb (_currn, SYMBconstant)) return (Mkrule_9(_coordref, _currn));
if (IsSymb (_currn, SYMBIdUse)) return (Mkrule_9(_coordref, _currn));
if (IsSymb (_currn, SYMBassignment_expression)) return (Mkrule_9(_coordref, _currn));
if (IsSymb (_currn, SYMBExpression)) return (Mkrule_9(_coordref, _currn));
if (IsSymb (_currn, SYMBfunction_call_expression)) return (Mkrule_9(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkexpression_opt */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkselection_statement (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkselection_statement (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBselection_statement)) return (_currn);
return(NULLNODEPTR);
}/* Mkselection_statement */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkexpression_statement (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkexpression_statement (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBexpression_statement)) return (_currn);
return(NULLNODEPTR);
}/* Mkexpression_statement */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mklabeled_statement (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mklabeled_statement (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBlabeled_statement)) return (_currn);
return(NULLNODEPTR);
}/* Mklabeled_statement */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkstatement (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkstatement (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBstatement)) return (_currn);
if (IsSymb (_currn, SYMBlabeled_statement)) return (Mkrule_17(_coordref, _currn));
if (IsSymb (_currn, SYMBcompound_statement)) return (Mkrule_16(_coordref, _currn));
if (IsSymb (_currn, SYMBexpression_statement)) return (Mkrule_15(_coordref, _currn));
if (IsSymb (_currn, SYMBselection_statement)) return (Mkrule_14(_coordref, _currn));
if (IsSymb (_currn, SYMBjump_statement)) return (Mkrule_13(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkstatement */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkstatement_list (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkstatement_list (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBstatement_list)) return (_currn);
if (IsSymb (_currn, SYMBlabeled_statement)) return (Mkrule_19(_coordref, _currn));
if (IsSymb (_currn, SYMBcompound_statement)) return (Mkrule_19(_coordref, _currn));
if (IsSymb (_currn, SYMBexpression_statement)) return (Mkrule_19(_coordref, _currn));
if (IsSymb (_currn, SYMBselection_statement)) return (Mkrule_19(_coordref, _currn));
if (IsSymb (_currn, SYMBjump_statement)) return (Mkrule_19(_coordref, _currn));
if (IsSymb (_currn, SYMBstatement)) return (Mkrule_19(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkstatement_list */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkstatement_list_opt (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkstatement_list_opt (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBstatement_list_opt)) return (_currn);
if (IsSymb (_currn, SYMBstatement_list)) return (Mkrule_20(_coordref, _currn));
if (IsSymb (_currn, SYMBlabeled_statement)) return (Mkrule_20(_coordref, _currn));
if (IsSymb (_currn, SYMBcompound_statement)) return (Mkrule_20(_coordref, _currn));
if (IsSymb (_currn, SYMBexpression_statement)) return (Mkrule_20(_coordref, _currn));
if (IsSymb (_currn, SYMBselection_statement)) return (Mkrule_20(_coordref, _currn));
if (IsSymb (_currn, SYMBjump_statement)) return (Mkrule_20(_coordref, _currn));
if (IsSymb (_currn, SYMBstatement)) return (Mkrule_20(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkstatement_list_opt */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkdeclaration_list (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkdeclaration_list (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBdeclaration_list)) return (_currn);
if (IsSymb (_currn, SYMBdeclaration)) return (Mkrule_45(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkdeclaration_list */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkcompound_statement (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkcompound_statement (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBcompound_statement)) return (_currn);
return(NULLNODEPTR);
}/* Mkcompound_statement */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkjump_statement (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkjump_statement (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBjump_statement)) return (_currn);
return(NULLNODEPTR);
}/* Mkjump_statement */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkcompound_statement_opt (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkcompound_statement_opt (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBcompound_statement_opt)) return (_currn);
if (IsSymb (_currn, SYMBcompound_statement)) return (Mkrule_24(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkcompound_statement_opt */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkparameter_list_opt (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkparameter_list_opt (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBparameter_list_opt)) return (_currn);
if (IsSymb (_currn, SYMBparameter)) return (Mkrule_73(_coordref, _currn));
if (IsSymb (_currn, SYMBparameter_list)) return (Mkrule_73(_coordref, _currn));
return(NULLNODEPTR);
}/* Mkparameter_list_opt */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mktype_specifier (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mktype_specifier (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBtype_specifier)) return (_currn);
return(NULLNODEPTR);
}/* Mktype_specifier */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkfunction_definition (POSITION *_coordref, NODEPTR _currn)
#else
NODEPTR Mkfunction_definition (_coordref, _currn)
	POSITION *_coordref; NODEPTR _currn;
#endif
{
if (_currn == NULLNODEPTR) return NULLNODEPTR;
if (IsSymb (_currn, SYMBfunction_definition)) return (_currn);
return(NULLNODEPTR);
}/* Mkfunction_definition */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_1 (POSITION *_coordref, int _TERM1, NODEPTR _desc1)
#else
NODEPTR Mkrule_1 (_coordref, _TERM1,_desc1)
	POSITION *_coordref;
	int _TERM1;
	NODEPTR _desc1;
#endif
{	_TPPrule_1 _currn;
#ifdef __cplusplus
_currn = new _TPrule_1;
#else
_currn = (_TPPrule_1) TreeNodeAlloc (sizeof (struct _TPrule_1));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_1;
_currn->_desc1 = (_TSPargument_expression_list_opt) Mkargument_expression_list_opt (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_1: root of subtree no. 1 can not be made a argument_expression_list_opt node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_1;
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
_dapto_term_int((_currn)->_uid, "ident", _TERM1);
#endif

return ( (NODEPTR) _currn);
}/* Mkrule_1 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_2 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_2 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_2 _currn;
#ifdef __cplusplus
_currn = new _TPrule_2;
#else
_currn = (_TPPrule_2) TreeNodeAlloc (sizeof (struct _TPrule_2));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_2;
_currn->_desc1 = (_TSPExpression) MkExpression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_2: root of subtree no. 1 can not be made a Expression node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_2;
return ( (NODEPTR) _currn);
}/* Mkrule_2 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_3 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_3 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_3 _currn;
#ifdef __cplusplus
_currn = new _TPrule_3;
#else
_currn = (_TPPrule_3) TreeNodeAlloc (sizeof (struct _TPrule_3));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_3;
_currn->_desc1 = (_TSPexpression_opt) Mkexpression_opt (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_3: root of subtree no. 1 can not be made a expression_opt node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_3;
return ( (NODEPTR) _currn);
}/* Mkrule_3 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_4 (POSITION *_coordref, int _TERM1)
#else
NODEPTR Mkrule_4 (_coordref, _TERM1)
	POSITION *_coordref;
	int _TERM1;
#endif
{	_TPPrule_4 _currn;
#ifdef __cplusplus
_currn = new _TPrule_4;
#else
_currn = (_TPPrule_4) TreeNodeAlloc (sizeof (struct _TPrule_4));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_4;
_SETCOORD(_currn)
_TERMACT_rule_4;
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
_dapto_term_int((_currn)->_uid, "ident", _TERM1);
#endif

return ( (NODEPTR) _currn);
}/* Mkrule_4 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_5 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3)
#else
NODEPTR Mkrule_5 (_coordref,_desc1,_desc2,_desc3)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
	NODEPTR _desc3;
#endif
{	_TPPrule_5 _currn;
#ifdef __cplusplus
_currn = new _TPrule_5;
#else
_currn = (_TPPrule_5) TreeNodeAlloc (sizeof (struct _TPrule_5));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_5;
_currn->_desc1 = (_TSPExpression) MkExpression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_5: root of subtree no. 1 can not be made a Expression node ", 0, _coordref);
_currn->_desc2 = (_TSPstatement) Mkstatement (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_5: root of subtree no. 2 can not be made a statement node ", 0, _coordref);
_currn->_desc3 = (_TSPstatement) Mkstatement (_coordref, _desc3);	
if (((NODEPTR)_currn->_desc3) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_5: root of subtree no. 3 can not be made a statement node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_5;
return ( (NODEPTR) _currn);
}/* Mkrule_5 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_6 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2)
#else
NODEPTR Mkrule_6 (_coordref,_desc1,_desc2)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
#endif
{	_TPPrule_6 _currn;
#ifdef __cplusplus
_currn = new _TPrule_6;
#else
_currn = (_TPPrule_6) TreeNodeAlloc (sizeof (struct _TPrule_6));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_6;
_currn->_desc1 = (_TSPExpression) MkExpression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_6: root of subtree no. 1 can not be made a Expression node ", 0, _coordref);
_currn->_desc2 = (_TSPstatement) Mkstatement (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_6: root of subtree no. 2 can not be made a statement node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_6;
return ( (NODEPTR) _currn);
}/* Mkrule_6 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_7 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_7 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_7 _currn;
#ifdef __cplusplus
_currn = new _TPrule_7;
#else
_currn = (_TPPrule_7) TreeNodeAlloc (sizeof (struct _TPrule_7));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_7;
_currn->_desc1 = (_TSPfunction_call_expression) Mkfunction_call_expression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_7: root of subtree no. 1 can not be made a function_call_expression node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_7;
return ( (NODEPTR) _currn);
}/* Mkrule_7 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_8 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3)
#else
NODEPTR Mkrule_8 (_coordref,_desc1,_desc2,_desc3)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
	NODEPTR _desc3;
#endif
{	_TPPrule_8 _currn;
#ifdef __cplusplus
_currn = new _TPrule_8;
#else
_currn = (_TPPrule_8) TreeNodeAlloc (sizeof (struct _TPrule_8));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_8;
_currn->_desc1 = (_TSPIdUse) MkIdUse (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_8: root of subtree no. 1 can not be made a IdUse node ", 0, _coordref);
_currn->_desc2 = (_TSPassignment_operator) Mkassignment_operator (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_8: root of subtree no. 2 can not be made a assignment_operator node ", 0, _coordref);
_currn->_desc3 = (_TSPfunction_call_expression) Mkfunction_call_expression (_coordref, _desc3);	
if (((NODEPTR)_currn->_desc3) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_8: root of subtree no. 3 can not be made a function_call_expression node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_8;
return ( (NODEPTR) _currn);
}/* Mkrule_8 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_9 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_9 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_9 _currn;
#ifdef __cplusplus
_currn = new _TPrule_9;
#else
_currn = (_TPPrule_9) TreeNodeAlloc (sizeof (struct _TPrule_9));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_9;
_currn->_desc1 = (_TSPassignment_expression) Mkassignment_expression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_9: root of subtree no. 1 can not be made a assignment_expression node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_9;
return ( (NODEPTR) _currn);
}/* Mkrule_9 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_10 (POSITION *_coordref)
#else
NODEPTR Mkrule_10 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_10 _currn;
#ifdef __cplusplus
_currn = new _TPrule_10;
#else
_currn = (_TPPrule_10) TreeNodeAlloc (sizeof (struct _TPrule_10));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_10;
_SETCOORD(_currn)
_TERMACT_rule_10;
return ( (NODEPTR) _currn);
}/* Mkrule_10 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_11 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_11 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_11 _currn;
#ifdef __cplusplus
_currn = new _TPrule_11;
#else
_currn = (_TPPrule_11) TreeNodeAlloc (sizeof (struct _TPrule_11));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_11;
_currn->_desc1 = (_TSPexpression_opt) Mkexpression_opt (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_11: root of subtree no. 1 can not be made a expression_opt node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_11;
return ( (NODEPTR) _currn);
}/* Mkrule_11 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_12 (POSITION *_coordref, int _TERM1, NODEPTR _desc1)
#else
NODEPTR Mkrule_12 (_coordref, _TERM1,_desc1)
	POSITION *_coordref;
	int _TERM1;
	NODEPTR _desc1;
#endif
{	_TPPrule_12 _currn;
#ifdef __cplusplus
_currn = new _TPrule_12;
#else
_currn = (_TPPrule_12) TreeNodeAlloc (sizeof (struct _TPrule_12));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_12;
_currn->_desc1 = (_TSPstatement) Mkstatement (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_12: root of subtree no. 1 can not be made a statement node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_12;
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
_dapto_term_int((_currn)->_uid, "ident", _TERM1);
#endif

return ( (NODEPTR) _currn);
}/* Mkrule_12 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_13 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_13 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_13 _currn;
#ifdef __cplusplus
_currn = new _TPrule_13;
#else
_currn = (_TPPrule_13) TreeNodeAlloc (sizeof (struct _TPrule_13));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_13;
_currn->_desc1 = (_TSPjump_statement) Mkjump_statement (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_13: root of subtree no. 1 can not be made a jump_statement node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_13;
return ( (NODEPTR) _currn);
}/* Mkrule_13 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_14 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_14 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_14 _currn;
#ifdef __cplusplus
_currn = new _TPrule_14;
#else
_currn = (_TPPrule_14) TreeNodeAlloc (sizeof (struct _TPrule_14));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_14;
_currn->_desc1 = (_TSPselection_statement) Mkselection_statement (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_14: root of subtree no. 1 can not be made a selection_statement node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_14;
return ( (NODEPTR) _currn);
}/* Mkrule_14 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_15 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_15 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_15 _currn;
#ifdef __cplusplus
_currn = new _TPrule_15;
#else
_currn = (_TPPrule_15) TreeNodeAlloc (sizeof (struct _TPrule_15));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_15;
_currn->_desc1 = (_TSPexpression_statement) Mkexpression_statement (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_15: root of subtree no. 1 can not be made a expression_statement node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_15;
return ( (NODEPTR) _currn);
}/* Mkrule_15 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_16 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_16 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_16 _currn;
#ifdef __cplusplus
_currn = new _TPrule_16;
#else
_currn = (_TPPrule_16) TreeNodeAlloc (sizeof (struct _TPrule_16));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_16;
_currn->_desc1 = (_TSPcompound_statement) Mkcompound_statement (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_16: root of subtree no. 1 can not be made a compound_statement node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_16;
return ( (NODEPTR) _currn);
}/* Mkrule_16 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_17 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_17 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_17 _currn;
#ifdef __cplusplus
_currn = new _TPrule_17;
#else
_currn = (_TPPrule_17) TreeNodeAlloc (sizeof (struct _TPrule_17));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_17;
_currn->_desc1 = (_TSPlabeled_statement) Mklabeled_statement (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_17: root of subtree no. 1 can not be made a labeled_statement node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_17;
return ( (NODEPTR) _currn);
}/* Mkrule_17 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_18 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2)
#else
NODEPTR Mkrule_18 (_coordref,_desc1,_desc2)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
#endif
{	_TPPrule_18 _currn;
#ifdef __cplusplus
_currn = new _TPrule_18;
#else
_currn = (_TPPrule_18) TreeNodeAlloc (sizeof (struct _TPrule_18));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_18;
_currn->_desc1 = (_TSPstatement_list) Mkstatement_list (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_18: root of subtree no. 1 can not be made a statement_list node ", 0, _coordref);
_currn->_desc2 = (_TSPstatement) Mkstatement (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_18: root of subtree no. 2 can not be made a statement node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_18;
return ( (NODEPTR) _currn);
}/* Mkrule_18 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_19 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_19 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_19 _currn;
#ifdef __cplusplus
_currn = new _TPrule_19;
#else
_currn = (_TPPrule_19) TreeNodeAlloc (sizeof (struct _TPrule_19));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_19;
_currn->_desc1 = (_TSPstatement) Mkstatement (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_19: root of subtree no. 1 can not be made a statement node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_19;
return ( (NODEPTR) _currn);
}/* Mkrule_19 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_20 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_20 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_20 _currn;
#ifdef __cplusplus
_currn = new _TPrule_20;
#else
_currn = (_TPPrule_20) TreeNodeAlloc (sizeof (struct _TPrule_20));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_20;
_currn->_desc1 = (_TSPstatement_list) Mkstatement_list (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_20: root of subtree no. 1 can not be made a statement_list node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_20;
return ( (NODEPTR) _currn);
}/* Mkrule_20 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_21 (POSITION *_coordref)
#else
NODEPTR Mkrule_21 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_21 _currn;
#ifdef __cplusplus
_currn = new _TPrule_21;
#else
_currn = (_TPPrule_21) TreeNodeAlloc (sizeof (struct _TPrule_21));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_21;
_SETCOORD(_currn)
_TERMACT_rule_21;
return ( (NODEPTR) _currn);
}/* Mkrule_21 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_22 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_22 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_22 _currn;
#ifdef __cplusplus
_currn = new _TPrule_22;
#else
_currn = (_TPPrule_22) TreeNodeAlloc (sizeof (struct _TPrule_22));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_22;
_currn->_desc1 = (_TSPstatement_list_opt) Mkstatement_list_opt (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_22: root of subtree no. 1 can not be made a statement_list_opt node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_22;
return ( (NODEPTR) _currn);
}/* Mkrule_22 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_23 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2)
#else
NODEPTR Mkrule_23 (_coordref,_desc1,_desc2)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
#endif
{	_TPPrule_23 _currn;
#ifdef __cplusplus
_currn = new _TPrule_23;
#else
_currn = (_TPPrule_23) TreeNodeAlloc (sizeof (struct _TPrule_23));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_23;
_currn->_desc1 = (_TSPdeclaration_list) Mkdeclaration_list (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_23: root of subtree no. 1 can not be made a declaration_list node ", 0, _coordref);
_currn->_desc2 = (_TSPstatement_list_opt) Mkstatement_list_opt (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_23: root of subtree no. 2 can not be made a statement_list_opt node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_23;
return ( (NODEPTR) _currn);
}/* Mkrule_23 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_24 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_24 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_24 _currn;
#ifdef __cplusplus
_currn = new _TPrule_24;
#else
_currn = (_TPPrule_24) TreeNodeAlloc (sizeof (struct _TPrule_24));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_24;
_currn->_desc1 = (_TSPcompound_statement) Mkcompound_statement (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_24: root of subtree no. 1 can not be made a compound_statement node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_24;
return ( (NODEPTR) _currn);
}/* Mkrule_24 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_25 (POSITION *_coordref)
#else
NODEPTR Mkrule_25 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_25 _currn;
#ifdef __cplusplus
_currn = new _TPrule_25;
#else
_currn = (_TPPrule_25) TreeNodeAlloc (sizeof (struct _TPrule_25));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_25;
_SETCOORD(_currn)
_TERMACT_rule_25;
return ( (NODEPTR) _currn);
}/* Mkrule_25 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_26 (POSITION *_coordref, NODEPTR _desc1, int _TERM1, NODEPTR _desc2, NODEPTR _desc3)
#else
NODEPTR Mkrule_26 (_coordref,_desc1, _TERM1,_desc2,_desc3)
	POSITION *_coordref;
	NODEPTR _desc1;
	int _TERM1;
	NODEPTR _desc2;
	NODEPTR _desc3;
#endif
{	_TPPrule_26 _currn;
#ifdef __cplusplus
_currn = new _TPrule_26;
#else
_currn = (_TPPrule_26) TreeNodeAlloc (sizeof (struct _TPrule_26));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_26;
_currn->_desc1 = (_TSPtype_specifier) Mktype_specifier (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_26: root of subtree no. 1 can not be made a type_specifier node ", 0, _coordref);
_currn->_desc2 = (_TSPparameter_list_opt) Mkparameter_list_opt (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_26: root of subtree no. 2 can not be made a parameter_list_opt node ", 0, _coordref);
_currn->_desc3 = (_TSPcompound_statement_opt) Mkcompound_statement_opt (_coordref, _desc3);	
if (((NODEPTR)_currn->_desc3) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_26: root of subtree no. 3 can not be made a compound_statement_opt node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_26;
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
_dapto_term_int((_currn)->_uid, "ident", _TERM1);
#endif

return ( (NODEPTR) _currn);
}/* Mkrule_26 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_100 (POSITION *_coordref)
#else
NODEPTR Mkrule_100 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_100 _currn;
#ifdef __cplusplus
_currn = new _TPrule_100;
#else
_currn = (_TPPrule_100) TreeNodeAlloc (sizeof (struct _TPrule_100));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_100;
_SETCOORD(_currn)
_TERMACT_rule_100;
return ( (NODEPTR) _currn);
}/* Mkrule_100 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_99 (POSITION *_coordref)
#else
NODEPTR Mkrule_99 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_99 _currn;
#ifdef __cplusplus
_currn = new _TPrule_99;
#else
_currn = (_TPPrule_99) TreeNodeAlloc (sizeof (struct _TPrule_99));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_99;
_SETCOORD(_currn)
_TERMACT_rule_99;
return ( (NODEPTR) _currn);
}/* Mkrule_99 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_98 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2)
#else
NODEPTR Mkrule_98 (_coordref,_desc1,_desc2)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
#endif
{	_TPPrule_98 _currn;
#ifdef __cplusplus
_currn = new _TPrule_98;
#else
_currn = (_TPPrule_98) TreeNodeAlloc (sizeof (struct _TPrule_98));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_98;
_currn->_desc1 = (_TSPunary_operator) Mkunary_operator (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_98: root of subtree no. 1 can not be made a unary_operator node ", 0, _coordref);
_currn->_desc2 = (_TSPExpression) MkExpression (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_98: root of subtree no. 2 can not be made a Expression node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_98;
return ( (NODEPTR) _currn);
}/* Mkrule_98 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_97 (POSITION *_coordref)
#else
NODEPTR Mkrule_97 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_97 _currn;
#ifdef __cplusplus
_currn = new _TPrule_97;
#else
_currn = (_TPPrule_97) TreeNodeAlloc (sizeof (struct _TPrule_97));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_97;
_SETCOORD(_currn)
_TERMACT_rule_97;
return ( (NODEPTR) _currn);
}/* Mkrule_97 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_96 (POSITION *_coordref)
#else
NODEPTR Mkrule_96 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_96 _currn;
#ifdef __cplusplus
_currn = new _TPrule_96;
#else
_currn = (_TPPrule_96) TreeNodeAlloc (sizeof (struct _TPrule_96));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_96;
_SETCOORD(_currn)
_TERMACT_rule_96;
return ( (NODEPTR) _currn);
}/* Mkrule_96 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_95 (POSITION *_coordref)
#else
NODEPTR Mkrule_95 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_95 _currn;
#ifdef __cplusplus
_currn = new _TPrule_95;
#else
_currn = (_TPPrule_95) TreeNodeAlloc (sizeof (struct _TPrule_95));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_95;
_SETCOORD(_currn)
_TERMACT_rule_95;
return ( (NODEPTR) _currn);
}/* Mkrule_95 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_94 (POSITION *_coordref)
#else
NODEPTR Mkrule_94 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_94 _currn;
#ifdef __cplusplus
_currn = new _TPrule_94;
#else
_currn = (_TPPrule_94) TreeNodeAlloc (sizeof (struct _TPrule_94));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_94;
_SETCOORD(_currn)
_TERMACT_rule_94;
return ( (NODEPTR) _currn);
}/* Mkrule_94 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_93 (POSITION *_coordref)
#else
NODEPTR Mkrule_93 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_93 _currn;
#ifdef __cplusplus
_currn = new _TPrule_93;
#else
_currn = (_TPPrule_93) TreeNodeAlloc (sizeof (struct _TPrule_93));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_93;
_SETCOORD(_currn)
_TERMACT_rule_93;
return ( (NODEPTR) _currn);
}/* Mkrule_93 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_92 (POSITION *_coordref)
#else
NODEPTR Mkrule_92 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_92 _currn;
#ifdef __cplusplus
_currn = new _TPrule_92;
#else
_currn = (_TPPrule_92) TreeNodeAlloc (sizeof (struct _TPrule_92));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_92;
_SETCOORD(_currn)
_TERMACT_rule_92;
return ( (NODEPTR) _currn);
}/* Mkrule_92 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_91 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2)
#else
NODEPTR Mkrule_91 (_coordref,_desc1,_desc2)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
#endif
{	_TPPrule_91 _currn;
#ifdef __cplusplus
_currn = new _TPrule_91;
#else
_currn = (_TPPrule_91) TreeNodeAlloc (sizeof (struct _TPrule_91));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_91;
_currn->_desc1 = (_TSPtranslation_unit) Mktranslation_unit (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_91: root of subtree no. 1 can not be made a translation_unit node ", 0, _coordref);
_currn->_desc2 = (_TSPexternal_declaration) Mkexternal_declaration (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_91: root of subtree no. 2 can not be made a external_declaration node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_91;
return ( (NODEPTR) _currn);
}/* Mkrule_91 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_90 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_90 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_90 _currn;
#ifdef __cplusplus
_currn = new _TPrule_90;
#else
_currn = (_TPPrule_90) TreeNodeAlloc (sizeof (struct _TPrule_90));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_90;
_currn->_desc1 = (_TSPexternal_declaration) Mkexternal_declaration (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_90: root of subtree no. 1 can not be made a external_declaration node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_90;
return ( (NODEPTR) _currn);
}/* Mkrule_90 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_89 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_89 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_89 _currn;
#ifdef __cplusplus
_currn = new _TPrule_89;
#else
_currn = (_TPPrule_89) TreeNodeAlloc (sizeof (struct _TPrule_89));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_89;
_currn->_desc1 = (_TSPfile) Mkfile (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_89: root of subtree no. 1 can not be made a file node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_89;
return ( (NODEPTR) _currn);
}/* Mkrule_89 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_88 (POSITION *_coordref)
#else
NODEPTR Mkrule_88 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_88 _currn;
#ifdef __cplusplus
_currn = new _TPrule_88;
#else
_currn = (_TPPrule_88) TreeNodeAlloc (sizeof (struct _TPrule_88));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_88;
_SETCOORD(_currn)
_TERMACT_rule_88;
return ( (NODEPTR) _currn);
}/* Mkrule_88 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_87 (POSITION *_coordref)
#else
NODEPTR Mkrule_87 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_87 _currn;
#ifdef __cplusplus
_currn = new _TPrule_87;
#else
_currn = (_TPPrule_87) TreeNodeAlloc (sizeof (struct _TPrule_87));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_87;
_SETCOORD(_currn)
_TERMACT_rule_87;
return ( (NODEPTR) _currn);
}/* Mkrule_87 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_86 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3)
#else
NODEPTR Mkrule_86 (_coordref,_desc1,_desc2,_desc3)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
	NODEPTR _desc3;
#endif
{	_TPPrule_86 _currn;
#ifdef __cplusplus
_currn = new _TPrule_86;
#else
_currn = (_TPPrule_86) TreeNodeAlloc (sizeof (struct _TPrule_86));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_86;
_currn->_desc1 = (_TSPExpression) MkExpression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_86: root of subtree no. 1 can not be made a Expression node ", 0, _coordref);
_currn->_desc2 = (_TSPshift_operator) Mkshift_operator (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_86: root of subtree no. 2 can not be made a shift_operator node ", 0, _coordref);
_currn->_desc3 = (_TSPExpression) MkExpression (_coordref, _desc3);	
if (((NODEPTR)_currn->_desc3) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_86: root of subtree no. 3 can not be made a Expression node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_86;
return ( (NODEPTR) _currn);
}/* Mkrule_86 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_85 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_85 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_85 _currn;
#ifdef __cplusplus
_currn = new _TPrule_85;
#else
_currn = (_TPPrule_85) TreeNodeAlloc (sizeof (struct _TPrule_85));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_85;
_currn->_desc1 = (_TSPsource) Mksource (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_85: root of subtree no. 1 can not be made a source node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_85;
return ( (NODEPTR) _currn);
}/* Mkrule_85 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_84 (POSITION *_coordref)
#else
NODEPTR Mkrule_84 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_84 _currn;
#ifdef __cplusplus
_currn = new _TPrule_84;
#else
_currn = (_TPPrule_84) TreeNodeAlloc (sizeof (struct _TPrule_84));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_84;
_SETCOORD(_currn)
_TERMACT_rule_84;
return ( (NODEPTR) _currn);
}/* Mkrule_84 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_83 (POSITION *_coordref)
#else
NODEPTR Mkrule_83 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_83 _currn;
#ifdef __cplusplus
_currn = new _TPrule_83;
#else
_currn = (_TPPrule_83) TreeNodeAlloc (sizeof (struct _TPrule_83));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_83;
_SETCOORD(_currn)
_TERMACT_rule_83;
return ( (NODEPTR) _currn);
}/* Mkrule_83 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_82 (POSITION *_coordref)
#else
NODEPTR Mkrule_82 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_82 _currn;
#ifdef __cplusplus
_currn = new _TPrule_82;
#else
_currn = (_TPPrule_82) TreeNodeAlloc (sizeof (struct _TPrule_82));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_82;
_SETCOORD(_currn)
_TERMACT_rule_82;
return ( (NODEPTR) _currn);
}/* Mkrule_82 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_81 (POSITION *_coordref)
#else
NODEPTR Mkrule_81 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_81 _currn;
#ifdef __cplusplus
_currn = new _TPrule_81;
#else
_currn = (_TPPrule_81) TreeNodeAlloc (sizeof (struct _TPrule_81));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_81;
_SETCOORD(_currn)
_TERMACT_rule_81;
return ( (NODEPTR) _currn);
}/* Mkrule_81 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_80 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_80 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_80 _currn;
#ifdef __cplusplus
_currn = new _TPrule_80;
#else
_currn = (_TPPrule_80) TreeNodeAlloc (sizeof (struct _TPrule_80));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_80;
_currn->_desc1 = (_TSPIdUse) MkIdUse (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_80: root of subtree no. 1 can not be made a IdUse node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_80;
return ( (NODEPTR) _currn);
}/* Mkrule_80 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_79 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_79 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_79 _currn;
#ifdef __cplusplus
_currn = new _TPrule_79;
#else
_currn = (_TPPrule_79) TreeNodeAlloc (sizeof (struct _TPrule_79));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_79;
_currn->_desc1 = (_TSPconstant) Mkconstant (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_79: root of subtree no. 1 can not be made a constant node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_79;
return ( (NODEPTR) _currn);
}/* Mkrule_79 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_78 (POSITION *_coordref, int _TERM1)
#else
NODEPTR Mkrule_78 (_coordref, _TERM1)
	POSITION *_coordref;
	int _TERM1;
#endif
{	_TPPrule_78 _currn;
#ifdef __cplusplus
_currn = new _TPrule_78;
#else
_currn = (_TPPrule_78) TreeNodeAlloc (sizeof (struct _TPrule_78));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_78;
_SETCOORD(_currn)
_TERMACT_rule_78;
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
_dapto_term_int((_currn)->_uid, "include_string", _TERM1);
#endif

return ( (NODEPTR) _currn);
}/* Mkrule_78 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_77 (POSITION *_coordref, NODEPTR _desc1, int _TERM1)
#else
NODEPTR Mkrule_77 (_coordref,_desc1, _TERM1)
	POSITION *_coordref;
	NODEPTR _desc1;
	int _TERM1;
#endif
{	_TPPrule_77 _currn;
#ifdef __cplusplus
_currn = new _TPrule_77;
#else
_currn = (_TPPrule_77) TreeNodeAlloc (sizeof (struct _TPrule_77));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_77;
_currn->_desc1 = (_TSPtype_specifier) Mktype_specifier (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_77: root of subtree no. 1 can not be made a type_specifier node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_77;
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
_dapto_term_int((_currn)->_uid, "ident", _TERM1);
#endif

return ( (NODEPTR) _currn);
}/* Mkrule_77 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_76 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2)
#else
NODEPTR Mkrule_76 (_coordref,_desc1,_desc2)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
#endif
{	_TPPrule_76 _currn;
#ifdef __cplusplus
_currn = new _TPrule_76;
#else
_currn = (_TPPrule_76) TreeNodeAlloc (sizeof (struct _TPrule_76));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_76;
_currn->_desc1 = (_TSPparameter_list) Mkparameter_list (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_76: root of subtree no. 1 can not be made a parameter_list node ", 0, _coordref);
_currn->_desc2 = (_TSPparameter) Mkparameter (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_76: root of subtree no. 2 can not be made a parameter node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_76;
return ( (NODEPTR) _currn);
}/* Mkrule_76 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_75 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_75 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_75 _currn;
#ifdef __cplusplus
_currn = new _TPrule_75;
#else
_currn = (_TPPrule_75) TreeNodeAlloc (sizeof (struct _TPrule_75));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_75;
_currn->_desc1 = (_TSPparameter) Mkparameter (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_75: root of subtree no. 1 can not be made a parameter node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_75;
return ( (NODEPTR) _currn);
}/* Mkrule_75 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_74 (POSITION *_coordref)
#else
NODEPTR Mkrule_74 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_74 _currn;
#ifdef __cplusplus
_currn = new _TPrule_74;
#else
_currn = (_TPPrule_74) TreeNodeAlloc (sizeof (struct _TPrule_74));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_74;
_SETCOORD(_currn)
_TERMACT_rule_74;
return ( (NODEPTR) _currn);
}/* Mkrule_74 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_73 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_73 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_73 _currn;
#ifdef __cplusplus
_currn = new _TPrule_73;
#else
_currn = (_TPPrule_73) TreeNodeAlloc (sizeof (struct _TPrule_73));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_73;
_currn->_desc1 = (_TSPparameter_list) Mkparameter_list (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_73: root of subtree no. 1 can not be made a parameter_list node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_73;
return ( (NODEPTR) _currn);
}/* Mkrule_73 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_72 (POSITION *_coordref)
#else
NODEPTR Mkrule_72 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_72 _currn;
#ifdef __cplusplus
_currn = new _TPrule_72;
#else
_currn = (_TPPrule_72) TreeNodeAlloc (sizeof (struct _TPrule_72));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_72;
_SETCOORD(_currn)
_TERMACT_rule_72;
return ( (NODEPTR) _currn);
}/* Mkrule_72 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_71 (POSITION *_coordref)
#else
NODEPTR Mkrule_71 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_71 _currn;
#ifdef __cplusplus
_currn = new _TPrule_71;
#else
_currn = (_TPPrule_71) TreeNodeAlloc (sizeof (struct _TPrule_71));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_71;
_SETCOORD(_currn)
_TERMACT_rule_71;
return ( (NODEPTR) _currn);
}/* Mkrule_71 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_70 (POSITION *_coordref)
#else
NODEPTR Mkrule_70 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_70 _currn;
#ifdef __cplusplus
_currn = new _TPrule_70;
#else
_currn = (_TPPrule_70) TreeNodeAlloc (sizeof (struct _TPrule_70));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_70;
_SETCOORD(_currn)
_TERMACT_rule_70;
return ( (NODEPTR) _currn);
}/* Mkrule_70 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_69 (POSITION *_coordref)
#else
NODEPTR Mkrule_69 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_69 _currn;
#ifdef __cplusplus
_currn = new _TPrule_69;
#else
_currn = (_TPPrule_69) TreeNodeAlloc (sizeof (struct _TPrule_69));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_69;
_SETCOORD(_currn)
_TERMACT_rule_69;
return ( (NODEPTR) _currn);
}/* Mkrule_69 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_68 (POSITION *_coordref)
#else
NODEPTR Mkrule_68 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_68 _currn;
#ifdef __cplusplus
_currn = new _TPrule_68;
#else
_currn = (_TPPrule_68) TreeNodeAlloc (sizeof (struct _TPrule_68));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_68;
_SETCOORD(_currn)
_TERMACT_rule_68;
return ( (NODEPTR) _currn);
}/* Mkrule_68 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_67 (POSITION *_coordref)
#else
NODEPTR Mkrule_67 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_67 _currn;
#ifdef __cplusplus
_currn = new _TPrule_67;
#else
_currn = (_TPPrule_67) TreeNodeAlloc (sizeof (struct _TPrule_67));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_67;
_SETCOORD(_currn)
_TERMACT_rule_67;
return ( (NODEPTR) _currn);
}/* Mkrule_67 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_66 (POSITION *_coordref)
#else
NODEPTR Mkrule_66 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_66 _currn;
#ifdef __cplusplus
_currn = new _TPrule_66;
#else
_currn = (_TPPrule_66) TreeNodeAlloc (sizeof (struct _TPrule_66));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_66;
_SETCOORD(_currn)
_TERMACT_rule_66;
return ( (NODEPTR) _currn);
}/* Mkrule_66 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_65 (POSITION *_coordref, int _TERM1)
#else
NODEPTR Mkrule_65 (_coordref, _TERM1)
	POSITION *_coordref;
	int _TERM1;
#endif
{	_TPPrule_65 _currn;
#ifdef __cplusplus
_currn = new _TPrule_65;
#else
_currn = (_TPPrule_65) TreeNodeAlloc (sizeof (struct _TPrule_65));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_65;
_SETCOORD(_currn)
_TERMACT_rule_65;
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
_dapto_term_int((_currn)->_uid, "integer", _TERM1);
#endif

return ( (NODEPTR) _currn);
}/* Mkrule_65 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_64 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_64 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_64 _currn;
#ifdef __cplusplus
_currn = new _TPrule_64;
#else
_currn = (_TPPrule_64) TreeNodeAlloc (sizeof (struct _TPrule_64));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_64;
_currn->_desc1 = (_TSPExpression) MkExpression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_64: root of subtree no. 1 can not be made a Expression node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_64;
return ( (NODEPTR) _currn);
}/* Mkrule_64 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_63 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2)
#else
NODEPTR Mkrule_63 (_coordref,_desc1,_desc2)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
#endif
{	_TPPrule_63 _currn;
#ifdef __cplusplus
_currn = new _TPrule_63;
#else
_currn = (_TPPrule_63) TreeNodeAlloc (sizeof (struct _TPrule_63));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_63;
_currn->_desc1 = (_TSPinit_declarator_list) Mkinit_declarator_list (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_63: root of subtree no. 1 can not be made a init_declarator_list node ", 0, _coordref);
_currn->_desc2 = (_TSPinit_declarator) Mkinit_declarator (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_63: root of subtree no. 2 can not be made a init_declarator node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_63;
return ( (NODEPTR) _currn);
}/* Mkrule_63 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_62 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_62 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_62 _currn;
#ifdef __cplusplus
_currn = new _TPrule_62;
#else
_currn = (_TPPrule_62) TreeNodeAlloc (sizeof (struct _TPrule_62));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_62;
_currn->_desc1 = (_TSPinit_declarator) Mkinit_declarator (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_62: root of subtree no. 1 can not be made a init_declarator node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_62;
return ( (NODEPTR) _currn);
}/* Mkrule_62 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_61 (POSITION *_coordref, int _TERM1, NODEPTR _desc1, NODEPTR _desc2)
#else
NODEPTR Mkrule_61 (_coordref, _TERM1,_desc1,_desc2)
	POSITION *_coordref;
	int _TERM1;
	NODEPTR _desc1;
	NODEPTR _desc2;
#endif
{	_TPPrule_61 _currn;
#ifdef __cplusplus
_currn = new _TPrule_61;
#else
_currn = (_TPPrule_61) TreeNodeAlloc (sizeof (struct _TPrule_61));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_61;
_currn->_desc1 = (_TSPinteger_constant) Mkinteger_constant (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_61: root of subtree no. 1 can not be made a integer_constant node ", 0, _coordref);
_currn->_desc2 = (_TSPtype_specifier) Mktype_specifier (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_61: root of subtree no. 2 can not be made a type_specifier node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_61;
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
_dapto_term_int((_currn)->_uid, "ident", _TERM1);
#endif

return ( (NODEPTR) _currn);
}/* Mkrule_61 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_60 (POSITION *_coordref, int _TERM1, NODEPTR _desc1)
#else
NODEPTR Mkrule_60 (_coordref, _TERM1,_desc1)
	POSITION *_coordref;
	int _TERM1;
	NODEPTR _desc1;
#endif
{	_TPPrule_60 _currn;
#ifdef __cplusplus
_currn = new _TPrule_60;
#else
_currn = (_TPPrule_60) TreeNodeAlloc (sizeof (struct _TPrule_60));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_60;
_currn->_desc1 = (_TSPinitializer) Mkinitializer (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_60: root of subtree no. 1 can not be made a initializer node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_60;
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
_dapto_term_int((_currn)->_uid, "ident", _TERM1);
#endif

return ( (NODEPTR) _currn);
}/* Mkrule_60 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_59 (POSITION *_coordref, int _TERM1)
#else
NODEPTR Mkrule_59 (_coordref, _TERM1)
	POSITION *_coordref;
	int _TERM1;
#endif
{	_TPPrule_59 _currn;
#ifdef __cplusplus
_currn = new _TPrule_59;
#else
_currn = (_TPPrule_59) TreeNodeAlloc (sizeof (struct _TPrule_59));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_59;
_SETCOORD(_currn)
_TERMACT_rule_59;
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
_dapto_term_int((_currn)->_uid, "ident", _TERM1);
#endif

return ( (NODEPTR) _currn);
}/* Mkrule_59 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_58 (POSITION *_coordref)
#else
NODEPTR Mkrule_58 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_58 _currn;
#ifdef __cplusplus
_currn = new _TPrule_58;
#else
_currn = (_TPPrule_58) TreeNodeAlloc (sizeof (struct _TPrule_58));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_58;
_SETCOORD(_currn)
_TERMACT_rule_58;
return ( (NODEPTR) _currn);
}/* Mkrule_58 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_57 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3)
#else
NODEPTR Mkrule_57 (_coordref,_desc1,_desc2,_desc3)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
	NODEPTR _desc3;
#endif
{	_TPPrule_57 _currn;
#ifdef __cplusplus
_currn = new _TPrule_57;
#else
_currn = (_TPPrule_57) TreeNodeAlloc (sizeof (struct _TPrule_57));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_57;
_currn->_desc1 = (_TSPExpression) MkExpression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_57: root of subtree no. 1 can not be made a Expression node ", 0, _coordref);
_currn->_desc2 = (_TSPinclusive_OR_operator) Mkinclusive_OR_operator (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_57: root of subtree no. 2 can not be made a inclusive_OR_operator node ", 0, _coordref);
_currn->_desc3 = (_TSPExpression) MkExpression (_coordref, _desc3);	
if (((NODEPTR)_currn->_desc3) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_57: root of subtree no. 3 can not be made a Expression node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_57;
return ( (NODEPTR) _currn);
}/* Mkrule_57 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_56 (POSITION *_coordref, int _TERM1)
#else
NODEPTR Mkrule_56 (_coordref, _TERM1)
	POSITION *_coordref;
	int _TERM1;
#endif
{	_TPPrule_56 _currn;
#ifdef __cplusplus
_currn = new _TPrule_56;
#else
_currn = (_TPPrule_56) TreeNodeAlloc (sizeof (struct _TPrule_56));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_56;
_SETCOORD(_currn)
_TERMACT_rule_56;
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
_dapto_term_int((_currn)->_uid, "ident", _TERM1);
#endif

return ( (NODEPTR) _currn);
}/* Mkrule_56 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_55 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_55 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_55 _currn;
#ifdef __cplusplus
_currn = new _TPrule_55;
#else
_currn = (_TPPrule_55) TreeNodeAlloc (sizeof (struct _TPrule_55));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_55;
_currn->_desc1 = (_TSPtranslation_unit) Mktranslation_unit (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_55: root of subtree no. 1 can not be made a translation_unit node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_55;
return ( (NODEPTR) _currn);
}/* Mkrule_55 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_54 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_54 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_54 _currn;
#ifdef __cplusplus
_currn = new _TPrule_54;
#else
_currn = (_TPPrule_54) TreeNodeAlloc (sizeof (struct _TPrule_54));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_54;
_currn->_desc1 = (_TSPpre_include) Mkpre_include (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_54: root of subtree no. 1 can not be made a pre_include node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_54;
return ( (NODEPTR) _currn);
}/* Mkrule_54 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_53 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_53 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_53 _currn;
#ifdef __cplusplus
_currn = new _TPrule_53;
#else
_currn = (_TPPrule_53) TreeNodeAlloc (sizeof (struct _TPrule_53));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_53;
_currn->_desc1 = (_TSPfunction_definition) Mkfunction_definition (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_53: root of subtree no. 1 can not be made a function_definition node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_53;
return ( (NODEPTR) _currn);
}/* Mkrule_53 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_52 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_52 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_52 _currn;
#ifdef __cplusplus
_currn = new _TPrule_52;
#else
_currn = (_TPPrule_52) TreeNodeAlloc (sizeof (struct _TPrule_52));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_52;
_currn->_desc1 = (_TSPdeclaration) Mkdeclaration (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_52: root of subtree no. 1 can not be made a declaration node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_52;
return ( (NODEPTR) _currn);
}/* Mkrule_52 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_51 (POSITION *_coordref)
#else
NODEPTR Mkrule_51 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_51 _currn;
#ifdef __cplusplus
_currn = new _TPrule_51;
#else
_currn = (_TPPrule_51) TreeNodeAlloc (sizeof (struct _TPrule_51));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_51;
_SETCOORD(_currn)
_TERMACT_rule_51;
return ( (NODEPTR) _currn);
}/* Mkrule_51 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_50 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3)
#else
NODEPTR Mkrule_50 (_coordref,_desc1,_desc2,_desc3)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
	NODEPTR _desc3;
#endif
{	_TPPrule_50 _currn;
#ifdef __cplusplus
_currn = new _TPrule_50;
#else
_currn = (_TPPrule_50) TreeNodeAlloc (sizeof (struct _TPrule_50));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_50;
_currn->_desc1 = (_TSPExpression) MkExpression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_50: root of subtree no. 1 can not be made a Expression node ", 0, _coordref);
_currn->_desc2 = (_TSPexclusive_OR_operator) Mkexclusive_OR_operator (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_50: root of subtree no. 2 can not be made a exclusive_OR_operator node ", 0, _coordref);
_currn->_desc3 = (_TSPExpression) MkExpression (_coordref, _desc3);	
if (((NODEPTR)_currn->_desc3) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_50: root of subtree no. 3 can not be made a Expression node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_50;
return ( (NODEPTR) _currn);
}/* Mkrule_50 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_49 (POSITION *_coordref)
#else
NODEPTR Mkrule_49 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_49 _currn;
#ifdef __cplusplus
_currn = new _TPrule_49;
#else
_currn = (_TPPrule_49) TreeNodeAlloc (sizeof (struct _TPrule_49));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_49;
_SETCOORD(_currn)
_TERMACT_rule_49;
return ( (NODEPTR) _currn);
}/* Mkrule_49 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_48 (POSITION *_coordref)
#else
NODEPTR Mkrule_48 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_48 _currn;
#ifdef __cplusplus
_currn = new _TPrule_48;
#else
_currn = (_TPPrule_48) TreeNodeAlloc (sizeof (struct _TPrule_48));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_48;
_SETCOORD(_currn)
_TERMACT_rule_48;
return ( (NODEPTR) _currn);
}/* Mkrule_48 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_47 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2)
#else
NODEPTR Mkrule_47 (_coordref,_desc1,_desc2)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
#endif
{	_TPPrule_47 _currn;
#ifdef __cplusplus
_currn = new _TPrule_47;
#else
_currn = (_TPPrule_47) TreeNodeAlloc (sizeof (struct _TPrule_47));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_47;
_currn->_desc1 = (_TSPtype_specifier) Mktype_specifier (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_47: root of subtree no. 1 can not be made a type_specifier node ", 0, _coordref);
_currn->_desc2 = (_TSPinit_declarator_list) Mkinit_declarator_list (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_47: root of subtree no. 2 can not be made a init_declarator_list node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_47;
return ( (NODEPTR) _currn);
}/* Mkrule_47 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_46 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2)
#else
NODEPTR Mkrule_46 (_coordref,_desc1,_desc2)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
#endif
{	_TPPrule_46 _currn;
#ifdef __cplusplus
_currn = new _TPrule_46;
#else
_currn = (_TPPrule_46) TreeNodeAlloc (sizeof (struct _TPrule_46));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_46;
_currn->_desc1 = (_TSPdeclaration_list) Mkdeclaration_list (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_46: root of subtree no. 1 can not be made a declaration_list node ", 0, _coordref);
_currn->_desc2 = (_TSPdeclaration) Mkdeclaration (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_46: root of subtree no. 2 can not be made a declaration node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_46;
return ( (NODEPTR) _currn);
}/* Mkrule_46 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_45 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_45 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_45 _currn;
#ifdef __cplusplus
_currn = new _TPrule_45;
#else
_currn = (_TPPrule_45) TreeNodeAlloc (sizeof (struct _TPrule_45));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_45;
_currn->_desc1 = (_TSPdeclaration) Mkdeclaration (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_45: root of subtree no. 1 can not be made a declaration node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_45;
return ( (NODEPTR) _currn);
}/* Mkrule_45 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_44 (POSITION *_coordref)
#else
NODEPTR Mkrule_44 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_44 _currn;
#ifdef __cplusplus
_currn = new _TPrule_44;
#else
_currn = (_TPPrule_44) TreeNodeAlloc (sizeof (struct _TPrule_44));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_44;
_SETCOORD(_currn)
_TERMACT_rule_44;
return ( (NODEPTR) _currn);
}/* Mkrule_44 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_43 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_43 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_43 _currn;
#ifdef __cplusplus
_currn = new _TPrule_43;
#else
_currn = (_TPPrule_43) TreeNodeAlloc (sizeof (struct _TPrule_43));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_43;
_currn->_desc1 = (_TSPinteger_constant) Mkinteger_constant (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_43: root of subtree no. 1 can not be made a integer_constant node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_43;
return ( (NODEPTR) _currn);
}/* Mkrule_43 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_42 (POSITION *_coordref)
#else
NODEPTR Mkrule_42 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_42 _currn;
#ifdef __cplusplus
_currn = new _TPrule_42;
#else
_currn = (_TPPrule_42) TreeNodeAlloc (sizeof (struct _TPrule_42));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_42;
_SETCOORD(_currn)
_TERMACT_rule_42;
return ( (NODEPTR) _currn);
}/* Mkrule_42 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_41 (POSITION *_coordref)
#else
NODEPTR Mkrule_41 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_41 _currn;
#ifdef __cplusplus
_currn = new _TPrule_41;
#else
_currn = (_TPPrule_41) TreeNodeAlloc (sizeof (struct _TPrule_41));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_41;
_SETCOORD(_currn)
_TERMACT_rule_41;
return ( (NODEPTR) _currn);
}/* Mkrule_41 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_40 (POSITION *_coordref)
#else
NODEPTR Mkrule_40 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_40 _currn;
#ifdef __cplusplus
_currn = new _TPrule_40;
#else
_currn = (_TPPrule_40) TreeNodeAlloc (sizeof (struct _TPrule_40));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_40;
_SETCOORD(_currn)
_TERMACT_rule_40;
return ( (NODEPTR) _currn);
}/* Mkrule_40 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_39 (POSITION *_coordref)
#else
NODEPTR Mkrule_39 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_39 _currn;
#ifdef __cplusplus
_currn = new _TPrule_39;
#else
_currn = (_TPPrule_39) TreeNodeAlloc (sizeof (struct _TPrule_39));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_39;
_SETCOORD(_currn)
_TERMACT_rule_39;
return ( (NODEPTR) _currn);
}/* Mkrule_39 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_38 (POSITION *_coordref)
#else
NODEPTR Mkrule_38 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_38 _currn;
#ifdef __cplusplus
_currn = new _TPrule_38;
#else
_currn = (_TPPrule_38) TreeNodeAlloc (sizeof (struct _TPrule_38));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_38;
_SETCOORD(_currn)
_TERMACT_rule_38;
return ( (NODEPTR) _currn);
}/* Mkrule_38 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_37 (POSITION *_coordref)
#else
NODEPTR Mkrule_37 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_37 _currn;
#ifdef __cplusplus
_currn = new _TPrule_37;
#else
_currn = (_TPPrule_37) TreeNodeAlloc (sizeof (struct _TPrule_37));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_37;
_SETCOORD(_currn)
_TERMACT_rule_37;
return ( (NODEPTR) _currn);
}/* Mkrule_37 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_36 (POSITION *_coordref)
#else
NODEPTR Mkrule_36 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_36 _currn;
#ifdef __cplusplus
_currn = new _TPrule_36;
#else
_currn = (_TPPrule_36) TreeNodeAlloc (sizeof (struct _TPrule_36));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_36;
_SETCOORD(_currn)
_TERMACT_rule_36;
return ( (NODEPTR) _currn);
}/* Mkrule_36 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_35 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_35 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_35 _currn;
#ifdef __cplusplus
_currn = new _TPrule_35;
#else
_currn = (_TPPrule_35) TreeNodeAlloc (sizeof (struct _TPrule_35));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_35;
_currn->_desc1 = (_TSPargument_expression_list) Mkargument_expression_list (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_35: root of subtree no. 1 can not be made a argument_expression_list node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_35;
return ( (NODEPTR) _currn);
}/* Mkrule_35 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_34 (POSITION *_coordref)
#else
NODEPTR Mkrule_34 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_34 _currn;
#ifdef __cplusplus
_currn = new _TPrule_34;
#else
_currn = (_TPPrule_34) TreeNodeAlloc (sizeof (struct _TPrule_34));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_34;
_SETCOORD(_currn)
_TERMACT_rule_34;
return ( (NODEPTR) _currn);
}/* Mkrule_34 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_33 (POSITION *_coordref, NODEPTR _desc1)
#else
NODEPTR Mkrule_33 (_coordref,_desc1)
	POSITION *_coordref;
	NODEPTR _desc1;
#endif
{	_TPPrule_33 _currn;
#ifdef __cplusplus
_currn = new _TPrule_33;
#else
_currn = (_TPPrule_33) TreeNodeAlloc (sizeof (struct _TPrule_33));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_33;
_currn->_desc1 = (_TSPExpression) MkExpression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_33: root of subtree no. 1 can not be made a Expression node ", 0, _coordref);
_COPYCOORD(_currn)
_TERMACT_rule_33;
return ( (NODEPTR) _currn);
}/* Mkrule_33 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_32 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2)
#else
NODEPTR Mkrule_32 (_coordref,_desc1,_desc2)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
#endif
{	_TPPrule_32 _currn;
#ifdef __cplusplus
_currn = new _TPrule_32;
#else
_currn = (_TPPrule_32) TreeNodeAlloc (sizeof (struct _TPrule_32));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_32;
_currn->_desc1 = (_TSPargument_expression_list) Mkargument_expression_list (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_32: root of subtree no. 1 can not be made a argument_expression_list node ", 0, _coordref);
_currn->_desc2 = (_TSPExpression) MkExpression (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_32: root of subtree no. 2 can not be made a Expression node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_32;
return ( (NODEPTR) _currn);
}/* Mkrule_32 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_31 (POSITION *_coordref)
#else
NODEPTR Mkrule_31 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_31 _currn;
#ifdef __cplusplus
_currn = new _TPrule_31;
#else
_currn = (_TPPrule_31) TreeNodeAlloc (sizeof (struct _TPrule_31));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_31;
_SETCOORD(_currn)
_TERMACT_rule_31;
return ( (NODEPTR) _currn);
}/* Mkrule_31 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_30 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3)
#else
NODEPTR Mkrule_30 (_coordref,_desc1,_desc2,_desc3)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
	NODEPTR _desc3;
#endif
{	_TPPrule_30 _currn;
#ifdef __cplusplus
_currn = new _TPrule_30;
#else
_currn = (_TPPrule_30) TreeNodeAlloc (sizeof (struct _TPrule_30));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_30;
_currn->_desc1 = (_TSPExpression) MkExpression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_30: root of subtree no. 1 can not be made a Expression node ", 0, _coordref);
_currn->_desc2 = (_TSPand_operator) Mkand_operator (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_30: root of subtree no. 2 can not be made a and_operator node ", 0, _coordref);
_currn->_desc3 = (_TSPExpression) MkExpression (_coordref, _desc3);	
if (((NODEPTR)_currn->_desc3) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_30: root of subtree no. 3 can not be made a Expression node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_30;
return ( (NODEPTR) _currn);
}/* Mkrule_30 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_29 (POSITION *_coordref)
#else
NODEPTR Mkrule_29 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_29 _currn;
#ifdef __cplusplus
_currn = new _TPrule_29;
#else
_currn = (_TPPrule_29) TreeNodeAlloc (sizeof (struct _TPrule_29));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_29;
_SETCOORD(_currn)
_TERMACT_rule_29;
return ( (NODEPTR) _currn);
}/* Mkrule_29 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_28 (POSITION *_coordref)
#else
NODEPTR Mkrule_28 (_coordref)
	POSITION *_coordref;
#endif
{	_TPPrule_28 _currn;
#ifdef __cplusplus
_currn = new _TPrule_28;
#else
_currn = (_TPPrule_28) TreeNodeAlloc (sizeof (struct _TPrule_28));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_28;
_SETCOORD(_currn)
_TERMACT_rule_28;
return ( (NODEPTR) _currn);
}/* Mkrule_28 */

#if defined(__STDC__) || defined(__cplusplus)
NODEPTR Mkrule_27 (POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3)
#else
NODEPTR Mkrule_27 (_coordref,_desc1,_desc2,_desc3)
	POSITION *_coordref;
	NODEPTR _desc1;
	NODEPTR _desc2;
	NODEPTR _desc3;
#endif
{	_TPPrule_27 _currn;
#ifdef __cplusplus
_currn = new _TPrule_27;
#else
_currn = (_TPPrule_27) TreeNodeAlloc (sizeof (struct _TPrule_27));
#endif
#ifdef MONITOR
_currn->_uid=MONTblStackSize; MONTblStackPush(((NODEPTR)_currn));
#endif
_currn->_prod = RULErule_27;
_currn->_desc1 = (_TSPExpression) MkExpression (_coordref, _desc1);	
if (((NODEPTR)_currn->_desc1) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_27: root of subtree no. 1 can not be made a Expression node ", 0, _coordref);
_currn->_desc2 = (_TSPBinOp) MkBinOp (_coordref, _desc2);	
if (((NODEPTR)_currn->_desc2) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_27: root of subtree no. 2 can not be made a BinOp node ", 0, _coordref);
_currn->_desc3 = (_TSPExpression) MkExpression (_coordref, _desc3);	
if (((NODEPTR)_currn->_desc3) == NULLNODEPTR)	
	message (DEADLY, "RULE rule_27: root of subtree no. 3 can not be made a Expression node ", 0, _coordref);
_SETCOORD(_currn)
_TERMACT_rule_27;
return ( (NODEPTR) _currn);
}/* Mkrule_27 */
