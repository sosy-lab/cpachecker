import json
import sys

import esprima

from lib.metadata import get_meta_data
from lib.paths import get_test262_test_dir
from lib.print import eprint
from lib.tokenize import tokenize
from lib.util import contains_subsequence


class UnsupportedFeatureVisitor(esprima.NodeVisitor):
    def __init__(self):
        self.has_unsupported_feature = False
        self.found_unsupported_features = set()
        self.node_types = set()

    def visit_Object(self, obj):
        # print(obj.type)
        self.node_types.add(obj.type)
        unsupported_node_types = [
            'ArrayPattern',
            'ArrowFunctionExpression',
            'AssignmentPattern',
            'AwaitExpression',
            'ClassDeclaration',
            'ForInStatement',
            'ForOfStatement',
            'Import',
            'ObjectPattern',
            'RestElement',
            'SpreadElement',
            'Super',
            'TemplateLiteral',
            'ThrowStatement',
            'TryStatement',
            'WithStatement',
            'YieldExpression',
        ]
        if obj.type in unsupported_node_types:
            self.found_unsupported_features.add(obj.type)
            self.has_unsupported_feature = True
        if obj.type == 'BinaryExpression' and obj.operator in ['in', 'instanceof']:
            self.found_unsupported_features.add(obj.operator + ' operator')
            self.has_unsupported_feature = True
        if obj.type == 'CallExpression' and obj.callee.type == 'Identifier':
            unsupported_callee_names = [
                # 'Array',
                # 'Boolean',
                # 'Date',
                'Function',
                # 'Number',
                # 'Object',
                # 'String',
            ]
            if obj.callee.name in unsupported_callee_names:
                self.found_unsupported_features.add('call of ' + obj.callee.name)
                self.has_unsupported_feature = True
        if (obj.type in ['FunctionDeclaration', 'FunctionExpression']
                and (obj.generator or obj.isAsync)):
            self.found_unsupported_features.add(obj.type)
            self.has_unsupported_feature = True
        # property names that indicate unsupported feature
        unsupported_identifiers = [
            'arguments',
            'eval',
            'Math',
            'Promise',
            'RegExp',
            'ReferenceError',
            'Symbol',
        ]
        if obj.type == 'Identifier' and obj.name in unsupported_identifiers:
            self.found_unsupported_features.add(obj.name)
            self.has_unsupported_feature = True
        if obj.type == 'MemberExpression' and obj.property.type == 'Identifier':
            if (obj.property.name == 'keys'
                    and obj.object.type == 'Identifier'
                    and obj.object.name == 'Object'):
                self.found_unsupported_features.add(obj.property.name)
                self.has_unsupported_feature = True
            # property names that indicate unsupported feature
            unsupported_property_names = [
                'apply',
                'bind',
                'call',
                'charCodeAt',
                'constructor',
                'defineProperty',
                'fromCharCode',
                'getOwnPropertyDescriptor',
                'hasOwnProperty',
                'isPrototypeOf',
                'setPrototypeOf',
                'throws',
                'toString',
                'valueOf',
            ]
            if obj.property.name in unsupported_property_names:
                self.found_unsupported_features.add(obj.property.name)
                self.has_unsupported_feature = True
        if obj.type == 'NewExpression' and obj.callee.type == 'Identifier':
            unsupported_callee_names = [
                'Array',
                'Boolean',
                'Date',
                'Function',
                'Number',
                'Object',
                'String',
            ]
            if obj.callee.name in unsupported_callee_names:
                self.found_unsupported_features.add('new ' + obj.callee.name)
                self.has_unsupported_feature = True
        if obj.type == 'Property':
            if obj.kind in ['get', 'set']:
                self.found_unsupported_features.add(obj.kind)
                self.has_unsupported_feature = True
            if obj.method:
                self.found_unsupported_features.add('method property')
                self.has_unsupported_feature = True
            if obj.key.type == 'Identifier' and obj.key.name in ['toString', 'valueOf']:
                self.found_unsupported_features.add(obj.key.name)
                self.has_unsupported_feature = True
        if obj.type == 'UnaryExpression' and obj.operator == 'delete':
            self.found_unsupported_features.add('delete operator')
            self.has_unsupported_feature = True
        if obj.type == 'VariableDeclaration' and obj.kind in ['const', 'let']:
            self.found_unsupported_features.add(obj.kind)
            self.has_unsupported_feature = True
        return super().visit_Object(obj)


def is_skip_directory(dir):
    """
    Return if directory should be skipped (contains only files with unsupported features)
    :type dir: Path
    :return:
    """
    root = get_test262_test_dir() / 'language/'
    skipped_directories = [
        'expressions/arrow-function',
        'expressions/async-arrow-function',
        'expressions/async-function',
        'expressions/async-generator',
        'expressions/await',
        'expressions/class',
        'expressions/concatenation',  # support of string concatenation to restricted yet
        'expressions/dynamic-import',
        'expressions/generators',
        'expressions/exponentiation',
        'expressions/in',  # TODO in operator
        'expressions/instanceof',  # TODO instanceof operator
        'expressions/template-literal',
        'expressions/tagged-template',
        'literals/regexp',
        'statements/async-function',
        'statements/async-generator',
        'statements/class',
        'statements/for-await-of',
        'statements/for-in',
        'statements/for-of',
        'statements/generators',
        'statements/with',
    ]
    return any(dir == (root / sub_dir) or (root / sub_dir) in dir.parents for sub_dir in
               skipped_directories)


def contains_assertion(file_content):
    assertion_sub_strings = [
        'assert(',
        'assert.sameValue(',
    ]
    return any(s in file_content for s in assertion_sub_strings)


def is_skip(file, file_content):
    """
    Return if file should be skipped (contains unsupported features)
    :type file_content: str
    """
    if 'String.prototype.replace' in file_content:
        return True
    # TODO includes in meta data
    meta_data = get_meta_data(file, file_content)
    if 'es6id' in meta_data:
        return True
    unsupported_flags = [
        'async',
        'generators',
        'module',
        'noStrict',
    ]
    if any(f in meta_data['flags'] for f in unsupported_flags):
        return True
    unsupported_features = [
        'BigInt',
        'Map',
        'Proxy',
        'Reflect',
        'Reflect.construct',
        'Set',
        'Symbol',
        'Symbol.asyncIterator',
        'Symbol.hasInstance',
        'Symbol.iterator',
        'Symbol.toPrimitive',
        'Symbol.toStringTag',
        'Symbol.unscopables',
        'TypedArray',
        'arrow-function',
        'async-functions',
        'async-iteration',
        'caller',
        'class',
        'class-fields-private',
        'class-fields-public',
        'class-methods-private',
        'class-static-fields-private',
        'class-static-fields-public',
        'class-static-methods-private',
        'computed-property-names',
        'const',
        'cross-realm',
        'default-parameters',
        'destructuring-assignment',
        'destructuring-binding',
        'dynamic-import',
        'export-star-as-namespace-from-module',
        'for-of',
        'generators',
        'import.meta',
        'json-superset',
        'let',
        'new.target',
        'numeric-separator-literal',
        'object-rest',
        'object-spread',
        'optional-catch-binding',
        'regexp-named-groups',
        'super',
        'tail-call-optimization',
        'template',
        'u180e'
    ]
    if any(f in meta_data['features'] for f in unsupported_features):
        return True
    if 'negative' in meta_data:
        assert 'type' in meta_data['negative'], \
            'negative.type does not exist in meta data of {}\n{}'.format(file, json.dumps(meta_data,
                                                                                          indent=4,
                                                                                          sort_keys=False))
        if 'SyntaxError' in meta_data['negative']['type']:
            return True
    try:
        v = UnsupportedFeatureVisitor()
        ast = esprima.parse(file_content, delegate=v)
        # print(json.dumps(ast, indent=4, sort_keys=False))
        # print(ast)
        # print('')
        v.visit(ast)
        if v.has_unsupported_feature:
            # print('has unsupported features {}\n\t{}'.format(file, v.found_unsupported_features))
            return True
    except (esprima.error_handler.Error, RecursionError):
        eprint('could not parse {} due to {}'.format(file, sys.exc_info()[0]))
    try:
        tokens = tokenize(file_content)
    except (esprima.error_handler.Error, RecursionError):
        eprint('could not tokenize file {} due to {}'.format(file, sys.exc_info()[0]))
        tokens = list()
    # TODO elided array elements are not parsed correctly by Eclipse parser
    # https://bugs.eclipse.org/bugs/show_bug.cgi?id=544733
    return any(contains_subsequence(tokenize(c), tokens) for c in [',]', '[,', ',,'])
