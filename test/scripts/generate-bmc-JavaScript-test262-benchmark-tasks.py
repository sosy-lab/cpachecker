import os
import re
import textwrap
from pathlib import Path


def contains_eval(file_content):
    return 'eval(' in file_content


def contains_syntax_error(file_content):
    return 'type: SyntaxError' in file_content


def contains_try_statement(file_content):
    return 'try' in file_content


def contains_for_in_statement(file_content):
    return re.search('\\s+for\\s*\\([^)]+in', file_content)


def contains_with_statement(file_content):
    return re.search('\\s+with\\s*\\([^)]+\\)\\s*{', file_content)


def is_skip_directory(dir):
    """
    Return if directory should be skipped (contains only files with unsupported features)
    :type dir: Path
    :return:
    """
    root = project_root_dir / 'test/programs/javascript-test262-benchmark/test/language/statements/'
    skipped_directories = [
        'async-function',
        'async-generator',
        'class',
        'for-await-of',
        'for-in',
        'for-of',
        'generators',
        'with',
    ]
    return any(dir == (root / sub_dir) for sub_dir in skipped_directories)


def contains_assertion(file_content):
    assertion_sub_strings = [
        'assert(',
        'assert.sameValue(',
    ]
    return any(s in file_content for s in assertion_sub_strings)


def is_skip(file_content):
    """
    Return if file should be skipped (contains unsupported features)
    :type file_content: str
    """
    return (contains_eval(file_content)
            or contains_syntax_error(file_content)
            or contains_try_statement(file_content)
            or contains_for_in_statement(file_content)
            or contains_with_statement(file_content)
            or 'delete ' in file_content  # delete expression
            or 'delete(' in file_content  # delete expression
            or '= eval;' in file_content  # code evaluation is not supported yet
            or 'Object.getOwnPropertyDescriptor(' in file_content
            or '.hasOwnProperty(' in file_content
            or '.isPrototypeOf(' in file_content
            or 'with(' in file_content
            or 'arguments[' in file_content
            or 'arguments.length' in file_content
            or 'Math.' in file_content
            or 'es6id:' in file_content
            or 'features: [default-parameters' in file_content
            or 'assert.throws(' in file_content
            or 'tail-call-optimization' in file_content
            or 'new Array(' in file_content
            or 'new Boolean(' in file_content
            or 'new Function(' in file_content
            or 'Function(' in file_content
            or 'new Number(' in file_content
            or 'new String(' in file_content
            or 'new Object(' in file_content)


def create_task_file(yml_file, assert_lib_file, input_file_name, property_file, expected_verdict):
    yml_file.write_text(textwrap.dedent("""\
        format_version: "1.0"
        input_files:
            - "{assert_lib_file}"
            - "{input_file}"
        properties:
          - property_file: {property_file}
            expected_verdict: {expected_verdict}
        """).format(expected_verdict=expected_verdict,
                    assert_lib_file=assert_lib_file,
                    input_file='./' + input_file_name,
                    property_file=property_file))


project_root_dir = Path(__file__).parent.parent.parent

# Specification-Dateien im "config"-Ordner sind aus Sicherheitsgründen in der VerifierCloud
# verboten. Daher wird als Workaround auf ein anderes Verzeichnis verwiesen:
property_file = project_root_dir / 'test/config/specification/JavaScriptAssertion.spc'
# property_file = project_root_dir / 'config/specification/JavaScriptAssertion.spc'
if not property_file.exists():
    print('Property file {} not found'.format(property_file))
    exit(1)
assert_lib_file = \
    project_root_dir / 'test/programs/javascript-test262-benchmark/CPAchecker-test262-assert.js'
if not assert_lib_file.exists():
    print('Assertion library file {} not found'.format(assert_lib_file))
    exit(1)
assert_lib_negated_file = \
    project_root_dir / 'test/programs/javascript-test262-benchmark/CPAchecker-test262-assert-negated.js'
if not assert_lib_negated_file.exists():
    print('Negated assertion library file {} not found'.format(assert_lib_negated_file))
    exit(1)

yml_file_names = set()

for file in project_root_dir.glob(
        'test/programs/javascript-test262-benchmark/test/language/statements/*/*.js'):
    file_content = file.read_text()
    if is_skip_directory(file.parent) or is_skip(file_content):
        print('SKIP {}'.format(file))
        continue
    else:
        print('GENERATE TASK FOR {}'.format(file))
    relative_path_to_property_file = os.path.relpath(str(property_file), str(file.parent))
    yml_file_name = file.stem + '.yml'
    i = 0
    while yml_file_name in yml_file_names:
        yml_file_name = '{}_{}.yml'.format(file.stem, i)
        i = i + 1
    yml_file_names.add(yml_file_name)
    yml_file = file.parent / yml_file_name
    create_task_file(
        yml_file=yml_file,
        assert_lib_file=os.path.relpath(str(assert_lib_file), str(file.parent)),
        input_file_name=file.name,
        property_file=relative_path_to_property_file,
        expected_verdict='true')
    # negated test
    if '$ERROR(' in file_content:
        # negate condition of if-statement directly before call of $ERROR
        file_content_negated = re.sub(r'(\sif\s*\()(.*?)(\)\s*{?\s*\$ERROR\()', r'\1!(\2)\3',
                                      file_content)
        file = file.parent / (file.stem + '.js.negated')
        print('GENERATE NEGATED VERSION OF {}'.format(file))
        file.write_text(file_content_negated)
    yml_file_name =\
        '{}_false.yml'.format(file.stem) if i == 0 else '{}_{}_false.yml'.format(file.stem, i)
    yml_file = file.parent / yml_file_name.replace('.js', '')
    create_task_file(
        yml_file=yml_file,
        assert_lib_file=os.path.relpath(str(assert_lib_negated_file), str(file.parent)),
        input_file_name=file.name,
        property_file=relative_path_to_property_file,
        expected_verdict='false')
