<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

# CPAchecker specification automata

The specification automata language used here is similar to the [BLAST Query Language](http://www.sosy-lab.org/~dbeyer/Publications/2004-SAS.The\_Blast\_Query\_Language\_for\_Software\_Verification.pdf).

## Syntax of the specification automata language

Each specification (_Specification_) consists of one or more automata (_Automaton_), which alternatively can be presented as an assertion (_Assertion_).

Each automaton is defined by the following attributes:
* unique automaton identifier;
* type of automaton (**OBSERVER** or **CONTROL**);
* automata variables (_LocalDefs_);
* initial automaton state identifier (_InitDef_);
* automaton states (_StateDefs_).

Each automaton state contains the following information:
* state name;
* whether it is a target (error) state or not (_StateTypeDef_);
* whether it is a nondeterministic state or not (_StateNonDetDef_);
* automaton transitions (_Transitions_).

Each automaton transition is determined by:
* trigger (_Trigger_), which is a boolean expression;
* list of assertions (_Assertions_), which are checked, if a trigger is evaluated as true;
* list of actions (_Actions_), which are executed, if assertions are held;
* assumption (_Assume_), which is a C-expression, that is passed to the other CPAs for evaluation after actions execution;
* identifier of the state, in which automaton will transfer, if assumption is evaluated as true (_Goto_).

The semantics of the operations are presented in the next section.

### The specification automata language grammar

The full grammar is presented in the [`parser.cup`](../src/org/sosy_lab/cpachecker/cpa/automaton/parser.cup) file.
Here is a simplified version without actions:

_Specification_ ::= _Automaton_ _Specification_ | _Assertion_ **;** _Specification_ |

_Automaton_ ::= **OBSERVER** _Body_ | **CONTROL** _Body_ | _Body_

_Body_ ::= **AUTOMATON** IDENTIFIER _LocalDefs_ _InitDef_ _StateDefs_ **END AUTOMATON**

_InitDef_ ::= **INITIAL STATE** IDENTIFIER **;**

_LocalDefs_ ::= _LocalDef_ _LocalDefs_ |

_LocalDef_ ::=<br>
&nbsp;&nbsp;&nbsp;&nbsp;**LOCAL** IDENTIFIER IDENTIFIER **;**<br>
&nbsp;&nbsp;| **LOCAL** IDENTIFIER **<** IDENTIFIER **>** IDENTIFIER **;**<br>
&nbsp;&nbsp;| **LOCAL** IDENTIFIER **<** IDENTIFIER **>** IDENTIFIER **=** SQUAREEXPR **;**<br>
&nbsp;&nbsp;| **LOCAL** IDENTIFIER IDENTIFIER **=** _ConstantInt_ **;**

_StateDefs_ ::= _StateDef_ _StateDefs_ |

_StateDef_ ::= _StateTypeDef_ **STATE** _StateNonDetDef_ IDENTIFIER **:** _Transitions_

_StateTypeDef_ ::= **TARGET** |

_StateNonDetDef_ ::= **USEFIRST** | **USEALL** |

_Transitions_ ::= _Transition_ _Transitions_ |

_Transition_ ::= _Trigger_ **->** _Assertions_ _Assume_ _Actions_ _Goto_ **;**

_Trigger_ ::= _Bool_

_Assertions_ ::= _Assertion_ _Assertions_ |

_Assertion_ ::= **ASSERT** _Bool_

_Assume_ ::= **ASSUME** CURLYEXPR |

_Actions_ ::= _Action_ _Actions_ |

_Goto_ ::= **GOTO** IDENTIFIER | **ERROR** | **ERROR** **(** STRING\_LITERAL **)** | **STOP** | **BREAK**

_Action_ ::=<br>
&nbsp;&nbsp;&nbsp;&nbsp;**DO** IDENTIFIER **=** _InnerInt_<br>
&nbsp;&nbsp;| **DO** IDENTIFIER SQUAREEXPR **=** _SetValue_<br>
&nbsp;&nbsp;| **PRINT** _PrintArguments_<br>
&nbsp;&nbsp;| **MODIFY** **(** IDENTIFIER **,** STRING\_LITERAL **)**

_SetValue_ ::= **TRUE** | **FALSE**

_PrintArguments_ ::= _Expression_ _PrintArguments_ |

_Int_ ::= _ConstantInt_ | **(** _Int_ **)** | IDENTIFIER | _InnerInt_ **+** _InnerInt_ | _InnerInt_ **-** _InnerInt_

_InnerInt_ ::= _Int_ | **EVAL** **(** IDENTIFIER **,** STRING\_LITERAL **)**

_ConstantInt_ ::= INTEGER\_LITERAL

_Bool_ ::=<br>
&nbsp;&nbsp;&nbsp;&nbsp;**TRUE**<br>
&nbsp;&nbsp;| **FALSE**<br>
&nbsp;&nbsp;| **!** _Bool_<br>
&nbsp;&nbsp;| **(** _Bool_ **)**<br>
&nbsp;&nbsp;| _InnerInt_ **==** _InnerInt_<br>
&nbsp;&nbsp;| _InnerInt_ **!=** _InnerInt_<br>
&nbsp;&nbsp;| _Bool_ **==** _Bool_<br>
&nbsp;&nbsp;| _Bool_ **!=** _Bool_<br>
&nbsp;&nbsp;| _Bool_ **&&** _Bool_<br>
&nbsp;&nbsp;| _Bool_ **||** _Bool_<br>
&nbsp;&nbsp;| **MATCH** STRING\_LITERAL<br>
&nbsp;&nbsp;| **MATCH** CURLYEXPR<br>
&nbsp;&nbsp;| **MATCH** SQUAREEXPR<br>
&nbsp;&nbsp;| **MATCH** **LABEL** STRING\_LITERAL<br>
&nbsp;&nbsp;| **MATCH** **LABEL** SQUAREEXPR<br>
&nbsp;&nbsp;| **MATCH** **ASSERT**<br>
&nbsp;&nbsp;| **MATCH** **EXIT**<br>
&nbsp;&nbsp;| **MATCH** **ENTRY**<br>
&nbsp;&nbsp;| **MATCH** **FUNCTIONCALL** STRING\_LITERAL<br>
&nbsp;&nbsp;| **CHECK** **(** IDENTIFIER **,** STRING\_LITERAL **)**<br>
&nbsp;&nbsp;| **CHECK** **(** STRING\_LITERAL **)**<br>
&nbsp;&nbsp;| **CHECK** **(** **IS_TARGET_STATE** **)**<br>
&nbsp;&nbsp;| **COVERS_LINES** **(** _Integers_ **)**

_Integers_ ::= _ConstantInt_ _Integers_ |

_Expression_ ::=<br>
&nbsp;&nbsp;&nbsp;&nbsp;_Int_<br>
&nbsp;&nbsp;| _Bool_<br>
&nbsp;&nbsp;| STRING\_LITERAL<br>
&nbsp;&nbsp;| **EVAL** **(** IDENTIFIER **,** STRING\_LITERAL **)**

IDENTIFIER ::= LETTER [DIGIT | LETTER\]\*<br>
LETTER ::= _$a-zA-Z<br>
DIGIT ::= 0-9<br>
STRING\_LITERAL ::= **"** \[^"\n\r\]\* **"**<br>
INTEGER\_LITERAL ::= 0 | 1-9 DIGIT\*<br>
CURLYEXPR ::= **{** \[^\{\}\n\r\]\* **}**<br>
SQUAREEXPR ::= **[** \[^\[\]\n\r\]\* **]**

## Semantics of the specification automata language

Specification automata represent a property, which can be expressed as a temporal logic formula, and thus indicate a set of correct program executions, for which this formula is evaluated as true.
A property is held in a program, if all possible program executions are included into a corresponding set of correct program executions.

CPAchecker creates a CPA for each specification automaton depending on its type.
`ObserverAutomatonCPA` is used if automaton type is **OBSERVER**, and `ControlAutomatonCPA` is used if automaton type is **CONTROL**.
Observer automaton cannot modify other CPAs (i.e. it cannot use keywords **MODIFY** and **STOP**).
By default control automaton is used.

This CPA uses _MergeSepOperator_, _StopSepOperator_, _BreakOnTargetsPrecisionAdjustment_ and _FlatLatticeDomain_.
On transfer relation computation this CPA creates a new _AutomatonState_ based on current CFA edge, automaton transitions and other CPA states.
If a new state is error (target) state or transition assertion is violated, then automaton CPA sends a _BREAK_ signal to other CPAs.
If property is represented as several specification automata, then each of them will be transformed into a separated CPA.

More information about automaton CPA can be found in the third section of the paper [On-the-Fly Decomposition of Specifications in Software Model Checking](https://www.sosy-lab.org/research/pub/2016-FSE.On-the-Fly_Decomposition_of_Specifications_in_Software_Model_Checking.pdf).

### Automata variables

Automata variables are defined in the _LocalDefs_ section, then can be read from the _Bool_ sections (which can be part of sections _Trigger_, _Assertions_ and _Actions_) and the _Assume_ sections, and can be modified in the _Actions_ section.
Supported variables types are `int` and `set`.

In order to access automata variable value from the _Assume_ section one should add "$$" as a prefix to the variable name (for example, expression `$$var` is used to read value of the automaton variable `var`).

#### Int variable

Represents an integer number.

Declaration:

`LOCAL int var_name;`

or

`LOCAL int var_name = int_const;`

where `int_const` corresponds to INTEGER\_LITERAL in grammar (which is a natural number or zero). If `int_const` is not specified, then variable value is set to zero.

Integer variable returns its value on access, and changes its value on modification.

#### Set variable

Represents a set of some elements.

Declaration:

`LOCAL set var_name <elem_type>;`

or

`LOCAL set var_name <elem_type> = set_const;`

where `elem_type` is the type of set elements, `set_const` is the initial set value.
The type of set elements can be either `int` (corresponds to INTEGER\_LITERAL in grammar) or `string` (corresponds to STRING\_LITERAL in grammar).
The initial set value is `[elem_1, ..., elem_n]`, where n>=0 and elem\_i is either INTEGER\_LITERAL or STRING\_LITERAL. Note, that type of elem\_i must correspond to `elem_type`.

Access operations:

 * `var_name[elem]` - returns true, if element `elem` is contained in the `var_name` set and false otherwise;
 * `var_name.empty` - returns true, if the `var_name` set is empty and false otherwise.

Modification operations:

`var_name[elem]=true|false` - on `true` element `elem` is added to the `var_name` set, on false element `elem` is removed from the `var_name` set. Note, that type of element `elem` must be the same as the type of the `var_name` set elements.

### Automata states

There are 3 predefined automata states:
* **ERROR** represents error (target) state;
* **STOP** represents _BOTTOM_ state in CPA (transfer relation is not calculated from this state);
* **BREAK** is used to halt the analysis without being a target state.

One automaton state must be marked as initial in _InitDef_.

Transition to the error (target) state means property violation.
Any user-defined state can be marked as a target state (with keyword **TARGET**), which then will be treated similar to the **ERROR** state.

If automaton state is marked with the **USEFIRST** keyword, then its transitions are applied until the first successful match.
Otherwise (i.e. if the **USEALL** keyword is specified) all matched for the current CFA edge automaton transitions are used.

Each checked property may contain several violation (in different automata transitions or in different automata specifications).
In order to differentiate those violations, transitions to the **ERROR** state can be marked with specific string: `ERROR ("error message")`.

Each automaton transition must have identifier of the state, in which automaton will transfer, if assumption is evaluated as true (the _Goto_ section).
If automaton should stay in the same state on some transition, then identifier of the same state should be used.

### Transition triggers

Transition trigger is a _Bool_ expression, which is evaluated on transfer relation computation based on the given CFA edge and other CPA states.
If this expression is evaluated as true, then transition is taken, otherwise it is rejected.

This expression may contain standard boolean operations (`!`, `(`, `)`, `==`, `!=`, `&&` and `||`), constants `TRUE` and `FALSE` and specific operations:
* `int_1 == int_2` or `int_1 != int_2` matches if expression is true, where `int_i` is an expression of integer constants, automata variables, addition and subtraction operations and `EVAL(cpa_name, query)` operation.
In order to evaluate this query, the CPA with name `cpa_name` name must present in a `CompositeCPA` and must implement method `evaluateProperty`, which can parse string `query`.
If these conditions are not satisfied, then the operation returns warning message as a result.
For example, operation `EVAL(location, "lineno")` sends query `lineno` to the `LocationCPA`, which returns line number of the analyzed CFA edge.
* `MATCH "stmt"` matches if string `stmt` is equal to the corresponding CFA edge C-statement.
* `MATCH {stmt}` matches if evaluated C-statement `stmt` is compared to the CFA edge C-statement with a tree comparison algorithm.
Statement `stmt` must be a correct C-statement.
For this match a set of transition variables is created, which are filled with joker expressions (`$?` and `$<number>`).
If statement `stmt` contains `CFunctionCallExpression` or `CAssignment` inside, joker expressions are substituted to assignment right-hand side or left-hand side or function return value or function arguments.
The expression `$?` is substituted to any pattern, but is not tracked, whereas each substituted name by `$<number>` is added to transition variables and can be used in this transition later.
For example:
    * pattern `x = $?` matches CFA edge `x = 5;`, transition variables are `{}`;
    * pattern `x = $1` matches CFA edge `x = 5;`, transition variables are `{$1=5}`;
    * pattern `$1 = $?` matches CFA edge `x = 5;`, transition variables are `{$1=x}`;
    * pattern `f($?)` matches CFA edges `f()` and `f(x, y)`, transition variables are `{}`;
    * pattern `f($1)` matches CFA edge `f(x)`, transition variables are `{$1=x}`;
    * pattern `$1 = f($?)` matches CFA edges `y = f(x)` and `y = f(x1, x2)`, transition variables are `{$1=y}`;
    * pattern `$1 = f($2)` matches CFA edge `y = f(x)`, transition variables are `{$1=y, $2=x}`.
* `MATCH [expr]` matches if the CFA edge C-statement matches regular expression `expr`.
* `MATCH LABEL name` matches if the CFA edge is a label with name `name`.
* `MATCH LABEL [expr]` matches if the CFA edge is a label with name, which matches regular expression `expr`.
* `MATCH ASSERT` matches special edge "assert fail" added by CPAchecker.
* `MATCH EXIT` matches if successor state has no leaving edges.
* `MATCH ENTRY` matches if predecessor state has no entering edges.
* `MATCH FUNCTIONCALL functionname` matches if the CFA edge contains a functioncall with the given `functionname`.
* `CHECK (cpa_name, query)` matches if `CompositeCPA` contains CPA with name `cpa_name`, which implements method `checkProperty`, which returns true for string `query`.
For example, `CHECK(location, "functionName==f")` returns true, if `LocationCPA` is inside function `f`.
* `CHECK (query)` matches if at least one CPA from `CompositeCPA` implements method `checkProperty`, which returns true for string `query`.
* `CHECK (IS_TARGET_STATE)` matches if at least on CPA from `CompositeCPA` is in target state.
* `COVERS_LINES (int_1 int_2 ... int_n)` matches if lines `int_1 int_2 ... int_n` are covered.

Transition trigger must present on each automaton transition.

### Transition assertions

Transition assertions are _Bool_ expressions, which are checked, if a transition trigger is evaluated as true.
This expression is similar to the transition trigger.
If this expression is evaluated as false, it is treated similar to transition to the **ERROR** state, otherwise automaton actions are executed.
The automaton assertions may not be specified on automaton transition.

### Transition actions

Transition actions are executed after successful checking of transition assertions.
The following actions are supported:
* `DO` action allows to modify values of automata variables based on its type:
    * Int variables: `DO var = expr`, where `var` is an integer automata variable name, `expr` may contain integer automata variables and transition variables (if `MATCH {expr}` expression was used in the transition).
For example, action `var = var + 1` increments integer variable `var`, action `var = $1`, where transition variables are `{$1=1}`, assigns `1` to the `var` variable.
    * Set variables: `DO var[elem] = true|false`, where `elem` should be either a constant (INTEGER\_LITERAL or STRING\_LITERAL correspondingly) or automata integer variable or transition variable (if `MATCH {expr}` expression was used in the transition).
The type of `elem` element must be the same as the type of the `var` set elements.
For example, assuming that `var` is the set of strings, action `var["elem"]=true` adds string element `elem` to the `var` set, action `var[$1]=false`, where transition variables are `{$1="x"}`, removes string element `x` from the `var` set.
* `PRINT expr` action prints the value of specified expression.
It does not affect the analysis.
The expression may contain automata variables and transition variables.
The following expressions are supported:
    * a boolean expression (the _Bool_ section);
    * an integer expression (the _Int_ section);
    * a string constant (STRING\_LITERAL);
    * `EVAL (cpa_name, query)` expression.
* `MODIFY (cpa_name, "expr")` action modifies CPA `cpa_name` with `expr`.
In order to support this operation corresponding CPA must implement method `modifyProperty`, which can parse string `expr`.
This action can only be executes inside **CONTROL** automaton.
String `expr` may contain automata variables and transition variables.
For example, action `MODIFY (ValueAnalysis, "setvalue(x==$$var)")` sets to variable `x` value of automata variable `var` in `ValueAnalysisCPA`.

The automaton actions may not be specified on automaton transition.

### Transition assumptions

If trigger is evaluated as true and assertion holds for an automaton transition, then transition to the other automaton state will be created on transfer relation computation, but it may contain additional assumption (the _Assume_ section):

`ASSUME {expr_1; expr_2; ...; expr_n}`, where `n>=1`, `expr_i` is a C-expression.

This assumption represent a conjunction of `expr_i` C-expressions, which are evaluated by other CPAs in general case.

The assumption may either restrict abstract state for the automation transition (for example, by adding assumption as a predicate) or reject the automaton transition, if it never evaluates as true.
In order to create disjunction of assumptions one should add several transition, for example:

```
TRUE -> ASSUME {expr_1} ...
TRUE -> ASSUME {expr_2} ...
```
In this case 2 automaton transitions will be created with different assumptions, which is equal to the expression `expr_1 || expr_2`.

Each C-expression `expr_i` may contain automata variables or transition variable (if `MATCH {expr}` expression was used in the transition), which are substituted before passing to the other CPAs.
It also must be successfully parsed by C-parser.
If expression `expr_i` gets a conflicting type, then it should be casted to required type.
Note, that assumptions are evaluated after execution of actions, which may modify automaton variables from assumptions.

#### Examples of assumptions

Let us consider, that there are int automaton variable `var_1` with value 0, set automaton variable of strings `var_2` with value `["arg_2"]` and transition variables `{$1="arg_1", $2="arg_2"}`, where type of `arg_1` is `int`, type of `arg_2` is `size_t`.

Then assumption:
 * `$$var_1 > 0` is evaluated to `0 > 0` - transition will be rejected unconditionally;
 * `$$var_1 == 0` is evaluated to `0 == 0` - transition will be taken unconditionally;
 * `$$var_2[$1]` is evaluated to `0` - element `arg_1` is not contained in the `var_2` set - transition will be rejected unconditionally;
 * `$$var_2[$2]` is evaluated to `1` - element `arg_2` is contained in the `var_2` set - transition will be taken unconditionally;
 * `$$var_2.empty` is evaluated to `0` - the `var_2` set is not empty - transition will be rejected unconditionally;
 * `$1 > $$var_1` is evaluated to `arg_1 > 0` - transition will be taken with additional predicate `arg_1 > 0`;
 * `$2 > $$var_1` cannot be evaluated due to different types (`size_t` and `int`);
 * `((int)$2) > $$var_1` is evaluated to `(int)arg_2 > 0` - transition will be taken with additional predicate `(int)arg_2 > 0`;
 * `$1; ((int)$2) == 0; $$var_2[$2]` is evaluated to `arg_1 != 0 && (int)arg_2 == 0 && 1` - transition will be taken with additional predicate `arg_1 != 0 && (int)arg_2`;
 * `!$1; $$var_2[$1]` is evaluated to `arg_1 == 0 && 0` &mdash; transition will be rejected unconditionally.

## Examples of specification automata

The examples of specification automata can be found in the `config/specification` and `test/config/automata` directories.
