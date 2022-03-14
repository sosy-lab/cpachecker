<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Style & Coding Guide for Report Template
========================================

The style guide of the Report Template is based [Google Style for HTML/CSS](https://google.github.io/styleguide/htmlcssguide.html) and [Google Javascript Style](https://google.github.io/styleguide/jsguide.html).


Some additional information can be found in other files
in this directory, e.g. [Style Guide for Java](StyleGuide.md), [`Logging.md`](Logging.md) and [`Test.md`](Test.md).

Please read all these documents, they will let you write better code
with considerably less effort!

HTML/CSS guide
--------------

- Indent by 2 spaces at a time.Don’t use tabs or mix tabs and spaces for indentation.

- Use only lowercase. All code has to be lowercase: This applies to HTML element names, attributes, attribute values (unless text/CDATA), CSS selectors, properties, and property values (with the exception of strings).

- Remove trailing white spaces. Trailing white spaces are unnecessary and can complicate diffs.

- Use UTF-8 (no BOM). Make sure your editor uses UTF-8 as character encoding, without a byte order mark.Specify the encoding in HTML templates and documents via <meta charset="utf-8">. Do not specify the encoding of style sheets as these assume UTF-8.

- Explain code as needed, where possible. Use comments to explain code.

- Use HTML5. HTML5 (HTML syntax) is preferred for all HTML documents: <!DOCTYPE html> 

- Use valid HTML code unless that is not possible due to otherwise unattainable performance goals regarding file size.

- Use elements (sometimes incorrectly called “tags”) for what they have been created for. For example, use heading elements for headings, p elements for paragraphs, a elements for anchors, etc.

- For multimedia, such as images, videos, animated objects via canvas, make sure to offer alternative access. For images that means use of meaningful alternative text (alt) and for video and audio transcripts and captions, if available. Providing alternative contents is important for accessibility reasons: A blind user has few cues to tell what an image is about without @alt, and other users may have no way of understanding what video or audio contents are about either.

- Strictly keep structure (markup), presentation (styling), and behavior (scripting) apart, and try to keep the interaction between the three to an absolute minimum.

- Do not use entity references. There is no need to use entity references like &mdash;, &rdquo;, or &#x263a;, assuming the same encoding (UTF-8) is used for files and editors as well as among teams.

- Use a new line for every block, list, or table element, and indent every such child element. Independent of the styling of an element (as CSS allows elements to assume a different role per display property), put every block, list, or table element on a new line.Also, indent them if they are child elements of a block, list, or table element.

- Break long lines (optional). While there is no column limit recommendation for HTML, you may consider wrapping long lines if it significantly improves readability.

- When quoting attributes values, use double quotation marks. Use double ("") rather than single quotation marks ('') around attribute values.

- Use valid CSS where possible. Unless dealing with CSS validator bugs or requiring proprietary syntax, use valid CSS code.

- Use meaningful or generic ID and class names. Instead of presentational or cryptic names, always use ID and class names that reflect the purpose of the element in question, or that are otherwise generic.

- Use ID and class names that are as short as possible but as long as necessary. Try to convey what an ID or class is about while being as brief as possible.

- Avoid qualifying ID and class names with type selectors. Unless necessary (for example with helper classes), do not use element names in conjunction with IDs or classes.

- Use shorthand properties where possible. CSS offers a variety of shorthand properties (like font) that should be used whenever possible, even in cases where only one value is explicitly set.

- Omit unit specification after “0” values, unless required. Do not use units after 0 values unless they are required.

- Omit leading “0”s in values. Do not put 0s in front of values or lengths between -1 and 1.

- Alphabetize declarations. Put declarations in alphabetical order in order to achieve consistent code in a way that is easy to remember and maintain. 

- Indent all block content. Indent all block content, that is rules within rules as well as declarations, so to reflect hierarchy and improve understanding.

- Use a semicolon after every declaration. End every declaration with a semicolon for consistency and extensibility reasons.

- Use a space after a property name’s colon. Always use a single space between property and value (but no space between property and colon) for consistency reasons.

- Use a space between the last selector and the declaration block. Always use a single space between the last selector and the opening brace that begins the declaration block.

- Separate selectors and declarations by new lines. Always start a new line for each selector and declaration.

- Separate rules by new lines. Always put a blank line (two line breaks) between rules.

- Use single ('') rather than double ("") quotation marks for attribute selectors and property values. Do not use quotation marks in URI values (url()).

- Group sections by a section comment (optional). If possible, group style sheet sections together by using comments. Separate sections with new lines.


JavaScript guide
----------------

- Declarations with `let`: Always. When you fail to specify `let`, the variable gets placed in the global context, potentially clobbering existing values. Also, if there's no declaration, it's hard to tell in what scope a variable lives (e.g., it could be in the Document or Window just as easily as in the local scope). If you use `var` instead of `let`, the variable gets placed in the function scope instead of the current block scope. This is also confusing. So always declare with `let`.

- Use `NAMES_LIKE_THIS` for constant values. Use `@const` to indicate a constant (non-overwritable) pointer (a variable or property). Never use the const keyword as it's not supported in Internet Explorer.

- Semicolons should be included at the end of function expressions, but not at the end of function declarations. Always use semicolons.

- While most script engines support Function Declarations within blocks it is not part of ECMAScript (see ECMA-262, clause 13 and 14). Worse implementations are inconsistent with each other and with future EcmaScript proposals. ECMAScript only allows for Function Declarations in the root statement list of a script or function. Instead use a variable initialized with a Function Expression to define a function within a block.

- You basically can't avoid exceptions if you're doing something non-trivial (using an application development framework, etc.). Go for it.

- Without custom exceptions, returning error information from a function that also returns a value can be tricky, not to mention inelegant. Bad solutions include passing in a reference type to hold error information or always returning Objects with a potential error member. These basically amount to a primitive exception handling hack. Feel free to use custom exceptions when appropriate.

- For maximum portability and compatibility, always prefer standards features over non-standards features (e.g., `string.charAt(3)` over `string[3]` and element access with DOM functions instead of using an application-specific shorthand).

- There's no reason to use wrapper objects for primitive types, plus they're dangerous.

- Multi-level prototype hierarchies are how JavaScript implements inheritance. You have a multi-level hierarchy if you have a user-defined class D with another user-defined class B as its prototype. These hierarchies are much harder to get right than they first appear!

- In modern JavaScript engines, changing the number of properties on an object is much slower than reassigning the values. The delete keyword should be avoided except when it is necessary to remove a property from an object's iterated list of keys, or to change the result of if (`key in obj`).

- The ability to create closures is perhaps the most useful and often overlooked feature of JS. Here is a good description of how closures work. One thing to keep in mind, however, is that a closure keeps a pointer to its enclosing scope. As a result, attaching a closure to a DOM element can create a circular reference and thus, a memory leak. 

- `eval()` makes for confusing semantics and is dangerous to use if the string being eval()'d contains user input. There's usually a better, clearer, and safer way to write your code, so its use is generally not permitted. For RPC you can always use JSON and read the result using `JSON.parse()` instead of `eval()`.

- Using `with` clouds the semantics of your program. Because the object of the `with` can have properties that collide with local variables, it can drastically change the meaning of your program.

- `for-in` loops are often incorrectly used to loop over the elements in an Array. This is however very error prone because it does not loop from 0 to length - 1 but over all the present keys in the object and its prototype chain. 

- The semantics of this can be tricky. At times it refers to the global object (in most places), the scope of the caller (in eval), a node in the DOM tree (when attached using an event handler HTML attribute), a newly created object (in a constructor), or some other object (if function was call()ed or apply()ed).

- Associative Arrays are not allowed... or more precisely you are not allowed to use non number indexes for arrays. If you need a map/hash use Object instead of Array in these cases because the features that you want are actually features of Object and not of Array. Array just happens to extend Object (like any other object in JS and therefore you might as well have used `Date`, `RegExp` or `String`).

- The whitespace at the beginning of each line can't be safely stripped at compile time; whitespace after the slash will result in tricky errors. Use string concatenation instead.

- Use Array and Object literals instead of Array and Object constructors. Array constructors are error-prone due to their arguments.

- Modifying builtins like `Object.prototype` and `Array.prototype` are strictly forbidden. Modifying other builtins like `Function.prototype` is less dangerous but still leads to hard to debug issues in production and should be avoided.

- Private properties and methods should be named with a trailing underscore. Protected properties and methods should be named without a trailing underscore (like public ones).

- You can control how your objects string-ify themselves by defining a custom `toString()` method. This is fine, but you need to ensure that your method (1) always succeeds and (2) does not have side-effects. If your method doesn't meet these criteria, it's very easy to run into serious problems. For example, if toString() calls a method that does an assert, assert might try to output the name of the object in which it failed, which of course requires calling `toString()`.

- It isn't always possible to initialize variables at the point of declaration, so deferred initialization is fine.

- Always use explicit scope - doing so increases portability and clarity. For example, don't rely on window being in the scope chain. You might want to use your function in another application for which window is not the content window.

- Use sparingly and in general only where required by the syntax and semantics.

- We recommend the use of the JSDoc annotations `@private` and `@protected` to indicate visibility levels for classes, functions, and properties.

- All files, classes, methods and properties should be documented with JSDoc comments with the appropriate tags and types. Textual descriptions for properties, methods, method parameters and method return values should be included unless obvious from the property, method, or parameter name.

- All members defined on a class should be in the same file. So, only top-level classes should be provided in a file that contains multiple members defined on the same class (e.g. enums, inner classes, etc).
