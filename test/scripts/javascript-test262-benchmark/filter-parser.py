import json
import re
import sys

import esprima
import yaml
from esprima.tokenizer import BufferEntry

from lib.paths import get_project_root_dir

try:
    from StringIO import StringIO
except ImportError:
    from io import StringIO


def parse_yaml_string(ys):
    fd = StringIO(ys)
    dct = yaml.safe_load(fd)
    return dct


def get_meta_data(file, file_content):
    meta_data_match = re.search(r'/\*---(.+?)---\*/', file_content, re.DOTALL)
    if meta_data_match is None:
        # eprint('meta data not found in file {}'.format(file))
        return {'flags': [], 'features': []}
    meta_data = parse_yaml_string(meta_data_match.group(1))
    if 'flags' not in meta_data:
        meta_data['flags'] = []
    if 'features' not in meta_data:
        meta_data['features'] = []
    return meta_data

class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'


def eprint(*args, **kwargs):
    print(bcolors.WARNING, *args, bcolors.ENDC, file=sys.stderr, **kwargs)


def contains_assertion(file_content):
    assertion_sub_strings = [
        '$ERROR(',
        'assert(',
        'assert.',
    ]
    return any(s in file_content for s in assertion_sub_strings)


class Token:
    def __init__(self, bufferEntry: BufferEntry):
        self.bufferEntry = bufferEntry

    def __eq__(self, other):
        return (self.bufferEntry.type == other.bufferEntry.type and
                self.bufferEntry.value == other.bufferEntry.value and
                self.bufferEntry.regex == other.bufferEntry.regex and
                self.bufferEntry.range == other.bufferEntry.range and
                self.bufferEntry.loc == other.bufferEntry.loc)

    def __str__(self):
        return str(self.bufferEntry)

    def __repr__(self):
        return str(self.bufferEntry)


def tokenize(program):
    return list(map(Token, esprima.tokenize(program)))


def contains_subsequence(subsequence: BufferEntry, sequence: BufferEntry):
    l = len(subsequence)
    return any(subsequence == sequence[i:i + l] for i in range(len(sequence) - l + 1))


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


def has_unsupported_meta_data(file, file_content):
    meta_data = get_meta_data(file, file_content)
    if 'es6id' in meta_data:
        return True
    unsupported_flags = [
        'generators',
        'module',
        'noStrict',
    ]
    if any(f in meta_data['flags'] for f in unsupported_flags):
        return True
    unsupported_features = [
        'let',
        'default-parameters',
        'numeric-separator-literal',
        'tail-call-optimization',
    ]
    if any(f in meta_data['features'] for f in unsupported_features):
        return True
    if 'negative' in meta_data:
        assert 'type' in meta_data['negative'], \
            'negative.type does not exist in meta data of {}\n{}'.format(file, json.dumps(meta_data, indent=4, sort_keys=False))
        if 'SyntaxError' in meta_data['negative']['type']:
            return True
    return False


def main():
    project_root_dir = get_project_root_dir()
    subsequence = tokenize('...')
    # print(str(subsequence))
    # for file in project_root_dir.glob('test/programs/javascript/tmp.js'):
    # for file in project_root_dir.glob('test/programs/javascript-test262-benchmark/test/language/types/string/S8.4_A7.2.js'):
    file_patterns = [
        # 'test/programs/javascript-test262-benchmark/test/language/arguments-object/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/asi/**/*.js',
        # 'test/programs/javascript-test262-benchmark/test/language/block-scope/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/comments/**/*.js',
        # 'test/programs/javascript-test262-benchmark/test/language/computed-property-names/**/*.js',
        # 'test/programs/javascript-test262-benchmark/test/language/destructuring/**/*.js',
        # 'test/programs/javascript-test262-benchmark/test/language/directive-prologue/**/*.js',
        # 'test/programs/javascript-test262-benchmark/test/language/eval-code/**/*.js',
        # 'test/programs/javascript-test262-benchmark/test/language/export/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/expressions/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/function-code/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/future-reserved-words/**/*.js',
        # 'test/programs/javascript-test262-benchmark/test/language/global-code/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/identifier-resolution/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/identifiers/**/*.js',
        # 'test/programs/javascript-test262-benchmark/test/language/import/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/keywords/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/line-terminators/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/literals/**/*.js',
        # 'test/programs/javascript-test262-benchmark/test/language/module-code/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/punctuators/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/reserved-words/**/*.js',
        # 'test/programs/javascript-test262-benchmark/test/language/rest-parameters/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/source-text/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/statements/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/types/**/*.js',
        'test/programs/javascript-test262-benchmark/test/language/white-space/**/*.js',
    ]
    files_to_skip = [
    ]
    exception_counter = 0
    file_counter = 0
    for file_pattern in file_patterns:
        if len(list(project_root_dir.glob(file_pattern))) < 1:
            eprint('pattern matches no files {}'.format(file_pattern))
        for file in project_root_dir.glob(file_pattern):
            file_counter += 1
            if str(file.absolute()) in files_to_skip:
                continue
            program = file.read_text()
            if has_unsupported_meta_data(file, program) or 'String.prototype.replace' in program:
                continue
            # if 'try' not in program or not contains_assertion(program):
            # if '...' not in program or not contains_assertion(program):
            if not contains_assertion(program):
            # if not contains_with_statement(program):
                continue
            v = UnsupportedFeatureVisitor()
            try:
                tree = esprima.parse(program, delegate=v)
                tokens = tokenize(program)
                # print(str(tokens))
                # print(contains_subsequence(subsequence, tokens))
            except :
                eprint('{} was thrown in {}'.format(sys.exc_info()[0], file))
                exception_counter += 1
                continue
            v.visit(tree)

            # if v.has_unsupported_feature:
            if not v.has_unsupported_feature:
            # if 'ForInStatement' not in v.found_unsupported_features:
                print(file)
                print('')
                print(tree)
                print('')
                print(sorted(v.node_types))
                # print('')
                # print('has unsupported features {}\n\t{}'.format(file, v.found_unsupported_features))
                # break
                exit(1)
    print('\nexceptions: {}, files: {}, file patterns: {}'.format(exception_counter, file_counter, len(file_patterns)))

if __name__ == '__main__':
    main()
