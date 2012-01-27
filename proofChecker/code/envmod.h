#ifndef ENVMOD_H
#define ENVMOD_H

/* $Id: envmod.h,v 5.11 2000/08/24 13:56:32 uwe Exp $ */
/* Copyright 1991, The Regents of the University of Colorado */

/* This file is part of the Eli Module Library.

The Eli Module Library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public License as
published by the Free Software Foundation; either version 2 of the
License, or (at your option) any later version.

The Eli Module Library is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with the Eli Module Library; see the file COPYING.LIB.
If not, write to the Free Software Foundation, Inc., 59 Temple Place -
Suite 330, Boston, MA 02111-1307, USA.  */

/* As a special exception, when this file is copied by Eli into the
   directory resulting from a :source derivation, you may use that
   created file as a part of that directory without restriction. */

#include "deftbl.h"
#include "obstack.h"
#include "BitSet.h"
#include "eliproto.h"

typedef struct _EnvImpl *Environment;	/* Set of regions */
typedef struct _RelElt *Scope;		/* Single region */
typedef struct _RelElt *Binding;	/* Handle for binding data */
typedef struct _AccessMechanism *_Access;/* Constant-time access data */
typedef struct _ClassDescriptor *_Class;/* Description of a class Environment */

struct _EnvImpl {		/* Addressing environment */
   int nested;			   /* 1 if currently in this environment */
   _Access access;		   /* Constant-time access data */
   Environment parent;		   /* Enclosing environment */
   Scope relate;		   /* Current region */
   DefTableKey key;		   /* a key may be set to this env */
   int level;			   /* nesting level */
   _Class classdescr;		   /* inheritance properties if this is a class */
   int haveusedbindings;	   /* lookups have been done */
};

struct _RelElt {		/* Binding data */
   Binding nxt;			   /* Next binding for the current region */
   Environment env;		   /* Region containing this binding */
   DefTableKey key;		   /* Definition */
   int idn;			   /* Identifier */
};

typedef struct StkElt *StkPtr;	/* List implementing a definition stack */
struct StkElt {
   StkPtr out;			   /* Superseded definitions */
   Binding binding;		   /* Binding for this definition */
};

struct _AccessMechanism {	/* Constant-time access data */
   ObstackP IdnTbl; int MaxIdn;	   /* Stacks of definitions */
   ObstackP ClassIdnTbl;	   /* inheritance access structure */
   int MaxClassIdn;
   Environment CurrEnv;		   /* Environment for the array nested */
   _Class Classes;		   /* list of all class descriptors */
   int NextClassNo;		   /* number of next class to be entered */
};

typedef struct _Inheritance *InheritPtr;
struct _ClassDescriptor {	/* Description of a class Environment */
   int classno;			   /* number of this class */
   InheritPtr inhlist;		   /* list of classes inherited from */
   BitSet inhset;		   /* transitive inheritance set */
   Environment env;		   /* this class Environment */
   _Class nxt;			   /* next in list of all class descriptors */
};

struct _Inheritance {		/* element in list of classes inherited from */
   _Class fromcl;		   /* inheritance from class */
   InheritPtr nxt;		   /* next in list of classes inherited from */
};

#define NoEnv ((Environment)0)
#define NoScope ((Scope)0)
#define NoBinding ((Binding)0)
#define NoInherit ((InheritPtr)0)

extern Binding     DefinitionsOf ELI_ARG((Environment env));
  /* the sequence of bindings of the environment env */
extern Binding     NextDefinition ELI_ARG((Binding b));
  /* the next binding within the sequence of an environment */

extern int         IdnOf ELI_ARG((Binding b));
  /* the identifier of the binding b */
extern DefTableKey KeyOf ELI_ARG((Binding b));
  /* the key of the binding b */
extern Environment EnvOf ELI_ARG((Binding b));
  /* the environment of the binding b*/

extern Environment ParentOf ELI_ARG((Environment env));
  /* the parent environment of env */
extern DefTableKey KeyOfEnv ELI_ARG((Environment env));
  /* the key of the environment env*/
extern DefTableKey SetKeyOfEnv ELI_ARG((Environment env, DefTableKey k));
  /* the key of the environment env is set to k */
extern int         IsClass ELI_ARG((Environment env));
  /* 0 iff the environment env does not participate in 
     an inheritance relation */

extern InheritPtr  DirectInherits ELI_ARG((Environment env));
  /* the sequence of environments which env directly inherits from */
extern Environment EnvOfInherit ELI_ARG((InheritPtr inh));
  /* the first environment of the inheritance sequence */
extern InheritPtr  NextInherit ELI_ARG((InheritPtr inh));
  /* the next element of the inheritance sequence */

extern Environment NewEnv ELI_ARG((void));
/* Establish a new environment
 *    On exit-
 *       NewEnv=new environment
 ***/
 
extern Environment NewScope ELI_ARG((Environment env));
/* Establish a new scope within an environment
 *    On exit-
 *       NewScope=new environment that is a child of env
 ***/
 
extern int InheritClass  ELI_ARG((Environment tocl, Environment fromcl));
/* Establish an inheritance from the class fromcl to tocl
 *    On entry-
 *       tocl and fromcl are Environments
 *       bindings must not yet have been sought in tocl
 *    On exit-
 *       the inheritance from fromcl to tocl is established and
 *       1 is returned if tocl and fromcl are both nested in
 *       the same Environment hierarchy and if
 *       tocl != fromcl and there is no inheritance path from
 *       tocl to fromcl and if
 *       tocl has not been searched before;
 *       otherwise no new inheritance is established and
 *       0 is returned.
 ***/

extern int Inheritsfrom  ELI_ARG((Environment tocl, Environment fromcl));
/* Checks the completed inheritance DAG
 * for existance of an inheritance path
 *    On entry-
 *       tocl and fromcl are Environments
 *    On exit-
 *       1 is returned if tocl == fromcl or if there is an
 *       inheritance path from fromcl to tocl;
 *       otherwise 0 is returned.
 *       No further inheritance may be established to tocl or fromcl
 *       after this call.
 ***/

#define AddIdn(env,idn,key) (BindKey(env,idn,key)!=NoBinding)

extern Binding BindKey ELI_ARG((Environment env, int idn, DefTableKey key));
/* Bind an identifier to a given key in a scope, entering that scope
 *    If env is not the current environment then enter it
 *    If idn is bound in the innermost scope of env then on exit-
 *       BindKey=NoBinding
 *    Else on exit-
 *       BindKey=pointer to a new binding (idn,key)
 *          in the innermost scope of env
 ***/

extern Binding
BindKeyInScope ELI_ARG((Environment env, int idn, DefTableKey key));
/* Bind an identifier to a given key in a scope without entering that scope
 *    If idn is bound in the innermost scope of env then on exit-
 *       BindKey=NoBinding
 *    Else on exit-
 *       BindKey=pointer to a new binding (idn,key)
 *          in the innermost scope of env
 ***/

#define DefineIdn(env,idn) KeyOf(BindIdn(env,idn))

extern Binding BindIdn ELI_ARG((Environment env, int idn));
/* Bind an identifier in a scope, entering that scope
 *    If env is not the current environment then enter it
 *    If idn is bound in the innermost scope of env then on exit-
 *       BindIdn=pointer to the binding for idn in the innermost scope of env
 *    Else let n be a previously-unused definition table key
 *    Then on exit-
 *       BindIdn=pointer to a new binding (idn,n) in the innermost scope of env
 ***/

extern Binding BindInScope ELI_ARG((Environment env, int idn));
/* Bind an identifier in a scope without entering that scope
 *    If idn is defined in the innermost scope of env then on exit-
 *       BindingInScope=pointer to the binding for idn
 *          in the innermost scope of env
 *    Else let n be a previously-unused definition table key
 *    Then on exit-
 *       BindingInScope=pointer to a new binding (idn,n)
 *          in the innermost scope of env
 ***/

#define KeyInEnv(env,idn) KeyOf(BindingInEnv(env,idn))

extern Binding BindingInEnv ELI_ARG((Environment env, int idn));
/* Find the binding for an identifier in an environment
 *    If idn is bound in the innermost scope of env then on exit-
 *       BindingInEnv=pointer to the binding for idn in env
 *    Else if idn is bound in some ancestor of env then on exit-   
 *       BindingInEnv=BindingInEnv(Parent(env),idn)
 *    Else on exit-
 *       BindingInEnv=NoBinding
 *    Any class environment on the path from env to surrounding
 *       environments is checked for inherited definitions of idn
 ***/

#define KeyInScope(env,idn) KeyOf(BindingInScope(env,idn))

extern Binding BindingInScope ELI_ARG((Environment env, int idn));
/* Find the binding for an identifier in a scope
 *    If idn is bound in the innermost scope of env then on exit-   
 *       BindingInScope=pointer to the binding for idn
 *          in the innermost scope of env
 *    Else on exit- 
 *       BindingInEnv=NoBinding
 *    If env is a class environment inherited bindings are considered
 ***/

extern DefTableKey
  NextInhKey ELI_ARG((Environment env, int idn, DefTableKey lastkey));
/* Find another inherited definition for an identifier
 * On entry:
 *   lastkey is a key bound to identifier idn in an
 *   environment e which is inherited to an environment tocl
 *   that is env or is the next ancestor of env which
 *   inherits e.
 * On exit:
 *     NextInhKey=key that represents the next definition bound to
 *     identifier idn in an environment ep which is also
 *     inherited to tocl but not to e,
 *     if any such definition exists;
 *     otherwise NextInhKey=NoKey
 ***/

extern Binding
  NextInhBinding ELI_ARG((Environment env, Binding lastbinding));
/* On entry:
 *   lastbinding is a binding of an identifier idn in an
 *   environment e which is inherited to an environment tocl
 *   that is env or is the next ancestor of env which
 *   inherits e.
 * On exit:
 *     NextInhBinding=bdg is the next binding of
 *     identifier idn in an environment ep which is also
 *     inherited to tocl but not to e,
 *     if any such definition exists;
 *     otherwise NextInhBinding=NoBinding
 ***/

extern Binding
  OverridesBinding ELI_ARG((Binding bind));
/* On entry:
 *   bind is a binding of an identifier idn in an
 *   environment env
 * On exit:
 *     OverridesBinding=bdg is the binding of
 *     identifier idn in an environment which is
 *     inherited to env,
 *     if any such definition exists;
 *     otherwise OverridesBinding=NoBinding
 ***/

#ifdef MONITOR
/* Monitoring support for structured values */

#define DAPTO_RESULTEnvironment(e) DAPTO_RESULT_PTR (e)
#define DAPTO_ARGEnvironment(e)    DAPTO_ARG_PTR (e, Environment)

#define DAPTO_RESULTBinding(e)     DAPTO_RESULT_PTR (e)
#define DAPTO_ARGBinding(e)        DAPTO_ARG_PTR (e, Binding)

#define DAPTO_RESULT_Class(e)      DAPTO_RESULT_PTR (e)
#define DAPTO_ARG_Class(e)         DAPTO_ARG_PTR (e, _Class)

#endif

#endif
