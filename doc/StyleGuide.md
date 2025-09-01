<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Style & Coding Guide
====================

The style guide of this project is the [Google Java Style](https://google.github.io/styleguide/javaguide.html)
and we format all code with [google-java-format](https://github.com/google/google-java-format).
We recommend to install the google-java-format plugin for your IDE,
otherwise you need to execute `ant format-source` before each commit.

Further guidelines that are worth reading:
- Bloch: [Effective Java, 3rd Edition](https://www.amazon.com/Effective-Java-3rd-Joshua-Bloch/dp/0134685997)
- [Guava User Guide](https://github.com/google/guava/wiki)

Some additional information can be found in other files
in this directory, e.g. [`Logging.md`](Logging.md) and [`Test.md`](Test.md).

Please read all these documents, they will let you write better code
with considerably less effort!


## Additional rules and hints

### Spelling and Naming

- Try to avoid typos.
- Interfaces are not named with a leading `I`.
  In general, client code should not need to know whether
  it is using an interface or a concrete class,
  thus there should not be a naming difference.
  Furthermore, a good API should always make sure that
  the best way to do something is also the easiest way.
  When both an interface and a similarly-named class exist,
  the interface is the one that should primarily be used,
  and thus the interface gets the normal/clean/beautiful name,
  and the class the internal/ugly name.
  It is better to have one place using `FooImpl` and hundreds of places using `Foo`
  instead of one place using `Foo` and hundreds of places using `IFoo`.
- Parameters should start with `p` to avoid confusion with
  local variables or fields.
- For a set of names of concepts (e.g., of files, classes),
  the prefix order induced by the names should
  represent the semantic relation and structure of the concepts.
- Avoid negations in names, e.g., for parameters, fields etc.

### Compilation

- Never check in with compile errors.
- Avoid warnings:
  - If there is a way to fix the code, fix it (before committing).
  - Otherwise, use `@SuppressWarnings`.
- After adding/changing an `@Option` configuration,
  run `ant` to update documentation (before committing).

### Design

- Prefer immutable objects, for own classes and for collections
  (cf. [Guava's explanation of immutable collections](https://github.com/google/guava/wiki/ImmutableCollectionsExplained)).
  Do not add setters unless really required!
  For collections use Guava's types, cf. [below](#use-guavas-immutable-data-structures).
- Avoid `null`, replace it with real objects, or (at last resort) `Optional`
  (cf. [Guava's explanation of avoiding `null`](https://github.com/google/guava/wiki/UsingAndAvoidingNullExplained))
  and make your code `null`-hostile:
  actively preventing `null` by adding pre-condition checks
  finds bugs and makes the code easier to understand.
  In fields and private context, `null` is acceptable if there is no nicer solution.
- Avoid `boolean` parameters. They carry no semantics
  and make it hard to understand for the reader what effect they have
  (cf. [Martin Fowler's explanation](http://martinfowler.com/bliki/FlagArgument.html)).

### Configuration Options

- Only use the `@Option` scheme so your options are automatically type-checked
  and documented.
- Only introduce an option when its meaningful
  (do not add options that nobody would ever want to change).
- Preferably use one of the existing prefixes
  (`cpa.YOURCPA.`, `analysis.`, `parser.`, ...)
  instead of adding a new one.
- Do not use negated predicates as option name
  (use `something.enable` instead `something.disable`,
  `something.foo` instead of `something.noFoo` etc.).
- Do not forget to update the file [`ConfigurationOptions.txt`](ConfigurationOptions.txt)
  (done automatically by `ant`) and commit it together with your changes.

### Configuration Files

- All config files should have a comment explaining the goal of the respective configuration.
  In the files in the main `config/` directory
  this description should be an explanation for users of CPAchecker
  and for example refer to the relevant papers.
- Respect the grouping of files into the respective directories
  (also cf. the linked `README.md` files in each directory)
  and the individual rules:
  - [`config/`](../config/README.md): main configs for users of CPAchecker  
    These config files should be complete and usable.
    Except in special cases they should always include
    our default [resource limits](config/includes/resource-limits.properties)
    and our [default specification](config/specification/default.spc).
    In many cases it makes sense to refactor all other options out
    into a separate file in `config/includes/` to make it reusable
    also for component configs.
    It often makes sense to provide several config files
    for different high-level variants of an analysis,
    and in this case we usually want to a default such that users
    who are not experts with the respective analysis
    can use a recommendation from us
    (for example, we could have `foo-Cegar.properties`, `foo-NoCegar.properties`,
    and `foo.properties` that just includes one of the other two files).
  - [`config/cex-checks/`](../config/cex-checks/README.md): configs for analyses to be used as counterexample check  
    These can rely on the state space being finite
    (and usually consisting of only a single path) due to an additional automaton.
    Files in this directory should not include a specification nor the default resource limits
    and thus not include files that are directly in `config/`.
  - [`config/components/`](../config/components/README.md): configs for analyses that are components of a meta analysis
    (e.g., in a strategy selection or portfolio)  
    These configs may contain options that are useful only in particular cases,
    like small time limits for parts of sequential combinations.
    Files in this directory should not include a specification nor the default resource limits
    and thus not include files that are directly in `config/`.
  - [`config/includes/`](../config/includes/README.md): partial configs that make sense only as building blocks included in other files  
    Typically the files here should not contain a specification
    nor the default resource limits in order to make them useful
    in main configs as well as component configs.
    Thus the files here should only include files that are also in `config/includes/`.
  - [`config/specification/`](../config/specification/README.md): no config files, but specification files
  - [`config/unmaintained/`](../config/unmaintained/README.md): main configs for users of CPAchecker,
    but unmaintained and potentially outdated  
    All files here should not be included or referenced
    from other config files outside of this directory.
- CPAchecker configurations are usually much easier to understand
  (and less error-prone to maintain!)
  if we avoid overwriting options from one file in a different file,
  i.e., if one can simply look at all options in a config file and its included files
  without having to worry about whether the option is maybe overwritten
  and not effective in the resulting config.
  In the case where you want to include some config file
  but overwrite one of the options thereof and reset it to the default value,
  consider instead splitting the included file such that you have two files
  that can be used for the new purpose without the need to overwrite something previously set:
  one file that contains the included config but without the unwanted option,
  and one file that includes the former and adds the option.  
  This is especially relevant in cases where config file `a.properties`
  should overwrite an option from `b.properties` but where `a.properties`
  does not (directly) include `b.properties`, for example in transitive include chains
  or where a third file includes both `a.properties` and `b.properties`
  that have no further relation to each other.
  In the latter case the final value of the config option depends
  on the order of the include statements, which is highly error prone.
  This case will likely be forbidden in the future
  (cf. https://github.com/sosy-lab/java-common-lib/issues/4)
  and should already never occur.
- Make sure that configs with the same name in different directories behave similarly!
  Differences that result directly from the use case of the respective config file
  as indicated by the directory are ok.
  For example `config/foo.properties` and `config/includes/foo.properties`
  can and should differ with regards to options that we want only in main config files
  (default specification and resource limits),
  and `config/foo.properties` and `config/components/foo.properties`
  can also differ in options that make no sense as main or component config
  (like specific resource limits or options for parsing or CFA creation).
  But it should be avoided that such files for example define different algorithms,
  a different set of CPAs, or different analysis options.
  In such cases, rename one of the config files.
  Config files in `config/unmaintained/` should never have the same name
  as config files outside of this directory.

Note that the syntax of configuration files is explained in
[`Configuration.md`](Configuration.md#configuration-file-format).

### Documentation / Comments

- The following ranks several places by their importance of having comments:
  * packages (in `package-info.java`, EVERY package should have one!)
  * interfaces and public classes (at least a short note at the top of their responsibility)
  * public methods in interfaces and classes
  * non-public classes, methods and fields
- Please add comments wherever sensible,
  but make sure to add comments for the top three items!
- All command-line arguments need to be explained in [`Configuration.md`](Configuration.md).
- All `@Option` fields need to have a non-empty description
  that explains (to a user) what the option does.
- All top-level configuration files (`config/*.properties`) need to have a description
  that explains (to a user) what the configuration does (cf. above).
- Add references to external sources wherever possible,
  e.g., to papers describing implemented concepts or any relevant standard
  like C, Java, witnesses, etc.
  Use deep links and precise references to the relevant parts.
- Self-documenting code is usually better than an explicit comment describing what it does.
  This is achieved by using functions and variables with descriptive names.
  But do not take this as an excuse to omit useful comments
  that describe the reason for some particular code or background context.

### Collections and Data Structures

- Use Guava's immutable data structures as described [below in the separate section](#use-guavas-immutable-data-structures)!
- Use arrays only with primitive types (`int`, `long`, etc.)
  or when existing APIs require them.
  Otherwise, never use arrays of object types, use lists instead.
  They have a much nicer API, are equally fast,
  and allow you to use `ImmutableList` and `Collections.unmodifiableList()`
  to avoid the need for defensive copying while still guaranteeing immutability.
- When declaring variables of collection types,
  use the interface as type instead of the implementation (e.g., `List` instead of `ArrayList`).
  This is especially true for fields, parameters, and return types.
  Do use the `Immutable*` types from Guava, though, to show that your collection is immutable.
- Do not use the types `SortedMap` and `SortedSet`, use `NavigableMap` and `NavigableSet`
  (these are subinterfaces with more methods).
- If you need to iterate over a collection, make sure it has a defined order,
  i.e., use `HashMap` and `HashSet` only if iteration order is definitively irrelevant
  (all other common collection implementations have deterministic iteration order).
  If you need an unsorted mutable map/set with defined iteration order,
  make sure to use the types `SequencedMap` and `SequencedSet` to communicate and enforce this requirement,
  and the types `LinkedHashMap` and `LinkedHashSet` as implementation.
  Unfortunately, there is currently no type that guarantees deterministic iteration order
  and allows both Guava's immutable collections as well as `LinkedHashMap`/`Set`,
  so if you really need this use plain `Map` and `Set` plus additional documentation.
- There are many helpers for collections, but unfortunately in different places:
  - `org.sosy_lab.common.collect.Collections3` contains our own helper methods.
  - Guava has [utility classes such as `Lists`, `Iterables`, `Maps`, `Collections2`](https://github.com/google/guava/wiki/CollectionUtilitiesExplained),
    each of them offering utility methods for the respective type.
  - `FluentIterable` is a nice class for handling `Iterable`s with the same kind of API like `Stream`,
    and often easier to use and with more nice methods (like `filter(Class)`) than `Stream`.
  - Java itself provides the `Collections` class,
    though some parts like the singleton and immutable collections are better replaced by Guava utilities.

### Coding

- Make sure that CPAchecker remains deterministic,
  i.e., use fixed seeds and do not iterate over collections with non-deterministic order.
- Never have public fields,
  never have non-private non-final fields,
  and try to keep all other non-private fields to a minimum.
- If you use null in method parameters, return values, or fields,
  annotate them with `@Nullable`.
- Mark fields as final, if they are never modified,
  and try to make them final, if they are modified (-> immutability).
- Prefer enhanced for-loop over `List.get(int)`.
- Use try-with-resources instead of manually calling `close()`.
- For `Function`, `Predicate`, and `Optional`,
  use the JDK types instead of the Guava types where possible.
  For Optional fields in serializable classes, make them `@Nullable` instead.
- Do not over-use functional idioms!
  When an imperative version of the code is shorter and easier to read,
  it should be preferred.
- Avoid long and complex anonymous functions, especially if deeply nested.
  Turning them into a regular method and using a method reference
  makes the code easier to read and understand
  (e.g., because the function now has a name describing what it does).
- Use `Integer`, `Double`, `Long`, `Boolean` etc. only when necessary (this is, inside generics like in `List<Integer>`).
  In fields, method parameters and return values, and local parameters,
  it is better to use the corresponding primitive types like int.
- Be very careful when implementing `Comparator` or `Comparable`!
  The logic for comparison is typically very error prone.
  For example, one must not use integer subtraction or integer negation,
  because both can overflow and cause a wrong result.
  Try to avoid implementing Comparators and instead use Guava's Ordering
  or the static methods on the `Comparator` interface.
  If you implement `Comparator` or `Comparable`,
  follow our below instructions for [`compareTo` methods](#compareto-methods).
- Avoid `Cloneable` and `clone()`, use copy constructors instead if you need them
  (you don't if you use immutable classes).
- Never swallow an exception.
  If throwing another exception, add the original one as the cause.
  If logging, use the appropriate logger methods (c.f. [`Logging.md`](Logging.md)).
- Do not catch unchecked exceptions, these are used to signal bugs,
  so catching them hides bugs.
  If case it is really required, catch only the most specific type
  and only from the smallest possible part of the code.
  In particular, never catch `Exception`, only the specific exception types
  that need to be caught!
- Do not write `this.` were not necessary.
- Be careful with serialization, it has lots of unexpected pitfalls
  (read "Effective Java" before using it).
  If you have serializable classes,
  mark all serialization-related fields and methods with `@Serial`.

#### `switch`

Java 16 brought two new features for the `switch` keyword:
the possibility to use it as a *switch expression*
and *arrow labels* for cases, which use `->` instead of `:`.
These bring several improvements that we want to make use of:
Switch expressions require the compiler to prove exhaustiveness, so no case can be forgotten.
Arrow labels prevent fall through and avoid the surprising scoping issues of classic case labels.
Note that both features are orthogonal,
just because something uses arrow labels does not mean that it is a switch expression
or that the compiler would enforce exhaustiveness!
Unfortunately, there is no way to require an exhaustiveness check from the compiler for a switch statement.
But at least Eclipse and Google Error Prone check for exhaustiveness of enum switches in CI.

Thus, for `switch` the rules are:
- Whenever possible prefer switch expressions over switch statements,
  i.e., write the switch *inside an expression* (typically after `return` or an assignment).
- Always use *arrow labels* (`->` instead of `:`).

For `default` clauses the following rules apply:
- Switches over ints and Strings should always have a `default` clause
  (e.g., with `throw new AssertionError()`).
- Enum switches that intentionally not list all cases explicitly
  should always have a `default` clause (potentially empty with an explanatory comment).
  But often adding all cases explicitly is a better alternative.
- Enum switches that are exhaustive should *not* have a `default` clause,
  only where it is required to make the code compile
  (e.g., because otherwise a variable would remain uninitialized).
  In this case use `throw new AssertionError()`,
  but prefer to rewrite the switch statement into a switch expression,
  such that no default clause is necessary.

### Use Guava's immutable data structures

Java provides several immutable collections, but Guava has better replacements,
so always use [Guava's classes](https://github.com/google/guava/wiki/ImmutableCollectionsExplained).
With Guava's data structures one can see the immutability directly from the type,
and always using the same set of data structures consistently is better than mixing them.
Furthermore, only `ImmutableMap` and `ImmutableSet` guarantee order,
and we want to keep CPAchecker deterministic.
All the replacements in the following table can be used safely in all circumstances.

| Java method - NEVER USE         | Guava method - USE THIS         |
| --------------------------------|---------------------------------|
| Collections.emptyList()         | ImmutableList.of()              |
| Collections.emptyMap()          | ImmutableMap.of()               |
| Collections.emptySet()          | ImmutableSet.of()               |
| Collectors.toUnmodifiableList() | ImmutableList.toImmutableList() |
| Collectors.toUnmodifiableMap()  | ImmutableMap.toImmutableMap()   |
| Collectors.toUnmodifiableSet()  | ImmutableSet.toImmutableSet()   |
| List.of()                       | ImmutableList.of()              |
| List.copyOf()                   | ImmutableList.copyOf()          |
| Map.of()                        | ImmutableMap.of()               |
| Map.copyOf()                    | ImmutableMap.copyOf()           |
| Set.of()                        | ImmutableSet.of()               |
| Set.copyOf()                    | ImmutableSet.copyOf()           |

Several other collection methods from Java have the same disadvantage as above,
but have no direct replacement because they accept null values and Guava doesn't.
Null values in collections are typically bad design anyway,
so make sure null is avoided and replace them.
The `Collectors.toList/Map/Set()` results have the additional disadvantage
that they do not guarantee mutability,
but it is easy to accidentally mutate them and thus introduce a bug.

| Java method - AVOID         | Guava method - USE AFTER REMOVING NULL VALUES |
| ----------------------------|-----------------------------------------------|
| Collections.singletonList() | ImmutableList.of()                            |
| Collections.singletonMap()  | ImmutableMap.of()                             |
| Collections.singleton()     | ImmutableSet.of()                             |
| Collectors.toList()         | ImmutableList.toImmutableList()               |
| Collectors.toMap()          | ImmutableMap.toImmutableMap()                 |
| Collectors.toSet()          | ImmutableSet.toImmutableSet()                 |

### List of APIs to avoid

Avoid the following classes:

| Avoid                          | Replacement          | Why? |
|--------------------------------|----------------------|------|
| com.google.common.base.Optional| java.util.Optional   | only necessary for older Java, mix of types is confusing |
| java.io.**PrintStream**        | BufferedOutputStream | Swallows IOExceptions, but use for CPAchecker's statistics is ok |
| java.io.**PrintWriter**        | BufferedWriter       | Swallows IOExceptions |
| java.util.**LinkedList**       | ArrayList/ArrayDeque | inefficient, but note that ArrayDeque does not accept null and is slower when modifying in the middle |
| org.junit.**Assert**           | Truth.assertThat     | much better failure messages |

For Guava's `Optional`, usage that is hidden inside fluent method chains is ok
(Example: `FluentIterable.from(...).first().orNull()`)
but using it as a type (for declaring variables etc.) is not
as it introduces confusion with Java's `Optional`.


### equals methods

Writing a correct `equals()` implementation can be tricky.
It needs to ensure that it fulfills the contract of `equals()`
(reflexive, symmetric, transitive), returns `false` for `null`,
does not crash for unexpected types, and is consistent with `hashCode()`.
**For data classes, the recommended alternative to writing `equals()`
is to use a [`record class`](https://docs.oracle.com/en/java/javase/17/language/records.html).**
If this is not possible, please use one of the following patterns.
General notes:
- The `this == pOther` check in in `equals` is optional.
- Primitive fields need to be compared using `==`,
  object fields with `Object.equals(Object other)`
  and `@Nullable` object fields (but only those)
  with `Objects.equals(Object a, Object b)`.
- In class hierarchies, each class should check its own fields
  and delegate to `super.equals()` for the rest.

This is the preferred pattern:
```java
public void equals(@Nullable Object pOther) {
  if (this == pOther) {
    return true;
  }
  return pOther instanceof MyClass other
      && field1.equals(other.field1)
      && primitiveField2 == other.primitiveField2
      && ...;
}
```
`super.equals()` would be called as part of the conjunction if necessary.

If you must check for class identity instead of instanceof,
please make sure to read [this documentation](https://errorprone.info/bugpattern/EqualsGetClass)
and use the following pattern if required:
```java
public void equals(@Nullable Object pOther) {
  if (this == pOther) {
    return true;
  }
  if (pOther == null || getClass() != pOther.getClass()) {
    return false;
  }
  MyClass other = (MyClass) pOther;
  return field1.equals(other.field1)
      && primitiveField2 == other.field2
      && ...;
}
```
`super.equals()` would replace the null check if necessary.

If the equality logic for your class is more complex
than a series of conjunction (e.g., because it requires a disjunction),
please use the following pattern:
```java
public void equals(@Nullable Object pOther) {
  if (this == pOther) {
    return true;
  }
  if (pOther instanceof MyClass other
      && field1.equals(other.field1)
      && ...) {

    // Add comment here explaining the reason.
    if (/* condition */) {
      return field2.equals(other.field2);
    } else {
      return field3.equals(other.field3);
    }
  }
  return false;
}
```

If this still does not fit,
please refactor the implementation by extracting code into utility methods
and add comments. A comment in the beginning will also silence the CI check.

### compareTo methods

Writing a correct `compareTo()` implementation can be tricky.
It needs to ensure that it fulfills the contract of `compareTo()`
and is consistent with `equals()`.
Implementations should thus rely as much on existing utilities
as possible, for example on `compare` methods in classes
like `Arrays`, `Integer`, `Long`, etc.,
on comparators built with the static methods in `Comparator` or `Ordering`,
or use `ComparisonChain`.
In particular, do not implement a lexicographic ordering
on collections on your own!
`ComparisonChain` is the recommended standard pattern for `compareTo`
if delegation to a single utility method is not enough,
e.g., because more than one field needs to be compared:
```java
public int compareTo(MyClass other) {
  return ComparisonChain.start()
      .compare(field1, other.field1)
      .compare(field2, other.field2)
      .result();
}
```

If neither `ComparisonChain` or one of the utilities fit,
please refactor the implementation by extracting code into utility methods
and add comments. A comment in the beginning will also silence the CI check.
