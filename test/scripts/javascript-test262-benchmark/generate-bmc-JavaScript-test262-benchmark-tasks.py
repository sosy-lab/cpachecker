import json
import os
import sys
import textwrap
from pathlib import Path

import esprima

from lib.metadata import get_meta_data
from lib.paths import get_project_root_dir, get_test262_supported_features_files
from lib.print import eprint
from lib.skip import UnsupportedFeatureVisitor
from lib.tokenize import tokenize
from lib.util import contains_subsequence


def is_skip_directory(dir):
    """
    Return if directory should be skipped (contains only files with unsupported features)
    :type dir: Path
    :return:
    """
    root = project_root_dir / 'test/programs/javascript-test262-benchmark/test/language/'
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
    return any(dir == (root / sub_dir) or (root / sub_dir) in dir.parents for sub_dir in skipped_directories)


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
            'negative.type does not exist in meta data of {}\n{}'.format(file, json.dumps(meta_data, indent=4, sort_keys=False))
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


def create_task_file(yml_file, input_files, property_file, expected_verdict):
    input_files_formatted = "\n".join('  - "%s"' % (file_name) for file_name in input_files)
    yml_file.write_text(textwrap.dedent("""\
        format_version: "1.0"
        input_files:
        {input_files_formatted}
        properties:
          - property_file: {property_file}
            expected_verdict: {expected_verdict}
        """).format(expected_verdict=expected_verdict,
                    input_files_formatted=input_files_formatted,
                    property_file=property_file))


project_root_dir = get_project_root_dir()

delete_file_patterns = [
    'test/programs/javascript-test262-benchmark/test/language/**/*.yml',
    'test/programs/javascript-test262-benchmark/test/language/**/*.negated',
]
delete_error_occurred = False
for file_pattern in delete_file_patterns:
    for file in project_root_dir.glob(file_pattern):
        try:
            file.unlink()
        except OSError:
            eprint("Error while deleting file {}".format(file))
            delete_error_occurred = True
if delete_error_occurred:
    exit(1)

# Specification-Dateien im "config"-Ordner sind aus Sicherheitsgründen in der VerifierCloud
# verboten. Daher wird als Workaround auf ein anderes Verzeichnis verwiesen:
property_file = project_root_dir / 'test/config/specification/JavaScriptAssertion.spc'
# property_file = project_root_dir / 'config/specification/JavaScriptAssertion.spc'
if not property_file.exists():
    eprint('Property file {} not found'.format(property_file))
    exit(1)
error_lib_file = \
    project_root_dir / 'test/programs/javascript-test262-benchmark/CPAchecker-test262-error.js'
if not error_lib_file.exists():
    eprint('Assertion library file {} not found'.format(error_lib_file))
    exit(1)
assert_lib_file = \
    project_root_dir / 'test/programs/javascript-test262-benchmark/CPAchecker-test262-assert.js'
if not assert_lib_file.exists():
    eprint('Assertion library file {} not found'.format(assert_lib_file))
    exit(1)
assert_lib_negated_file = \
    project_root_dir / 'test/programs/javascript-test262-benchmark/CPAchecker-test262-assert-negated.js'
if not assert_lib_negated_file.exists():
    eprint('Negated assertion library file {} not found'.format(assert_lib_negated_file))
    exit(1)
std_lib_file = project_root_dir / 'contrib/javascript/std-lib.js'
if not std_lib_file.exists():
    eprint('Standard library file {} not found'.format(std_lib_file))
    exit(1)

yml_file_names = set()

for file in get_test262_supported_features_files():
    relpath = lambda f: os.path.relpath(str(f), str(file.parent))
    file_content = file.read_text()
    if is_skip_directory(file.parent) or 'bigint' in file.stem or is_skip(file, file_content):
        print('SKIP {}'.format(file))
        continue
    file_contains_error_assertion_call = '$ERROR(' in file_content
    file_contains_assertion = contains_assertion(file_content)
    if not(file_contains_error_assertion_call or file_contains_assertion):
        eprint('file contains no assertion {}'.format(file))
        continue
    else:
        print('GENERATE TASK FOR {}'.format(file))
    relative_path_to_property_file = relpath(property_file)
    yml_file_name = file.stem + '.yml'
    i = 0
    while yml_file_name in yml_file_names:
        yml_file_name = '{}_{}.yml'.format(file.stem, i)
        i = i + 1
    yml_file_names.add(yml_file_name)
    yml_file = file.parent / yml_file_name
    assertion_files = [relpath(error_lib_file)]
    if file_contains_assertion:
        assertion_files.append(relpath(assert_lib_file))
    create_task_file(
        yml_file=yml_file,
        input_files=assertion_files + [relpath(std_lib_file), './' + file.name],
        property_file=relative_path_to_property_file,
        expected_verdict='true')
    # uncomment to skip creation of negated tests
    continue
    negated_assertion_files = [relpath(error_lib_file)]
    if file_contains_assertion:
        negated_assertion_files.append(relpath(assert_lib_negated_file))
    # negated test
    if file_contains_error_assertion_call:
        # create negated task for each error case
        error_cases = re.finditer(r'(\sif\s*\()(.*?)(\)\s*{?\s*\$ERROR\()', file_content)
        for errorCaseIndex, m in enumerate(error_cases):
            # negate condition of if-statement directly before call of $ERROR
            file_content_negated = ''.join([
                file_content[0:m.start()],
                m.group(1),
                '!(',
                m.group(2),
                ')',
                m.group(3),
                file_content[m.end():],
            ])
            error_case_file = file.parent / ('%s.js.%d.negated' % (file.stem, errorCaseIndex))
            print('GENERATE NEGATED JS  {}'.format(error_case_file))
            error_case_file.write_text(file_content_negated)
            yml_file_name = '{}_{}.{}_false.yml'.format(error_case_file.stem, i, errorCaseIndex)
            yml_file = \
                error_case_file.parent / yml_file_name.replace('.js.%d' % errorCaseIndex, '')
            print('GENERATE NEGATED YML {}'.format(yml_file))
            create_task_file(
                yml_file=yml_file,
                input_files=negated_assertion_files + [relpath(std_lib_file), './' + error_case_file.name],
                property_file=relative_path_to_property_file,
                expected_verdict='false')
    else:
        yml_file_name = \
            '{}_false.yml'.format(file.stem) if i == 0 else '{}_{}_false.yml'.format(file.stem,
                                                                                     i)
        yml_file = file.parent / yml_file_name.replace('.js', '')
        create_task_file(
            yml_file=yml_file,
            input_files=negated_assertion_files + [relpath(std_lib_file), './' + file.name],
            property_file=relative_path_to_property_file,
            expected_verdict='false')
