import json
import sys

import esprima

from lib.metadata import get_meta_data
from lib.paths import get_project_root_dir, get_test262_supported_features_files
from lib.print import eprint
from lib.skip import UnsupportedFeatureVisitor
from lib.tokenize import tokenize


def contains_assertion(file_content):
    assertion_sub_strings = [
        '$ERROR(',
        'assert(',
        'assert.',
    ]
    return any(s in file_content for s in assertion_sub_strings)


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
    files_to_skip = [
    ]
    exception_counter = 0
    file_counter = 0
    for file in get_test262_supported_features_files():
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
