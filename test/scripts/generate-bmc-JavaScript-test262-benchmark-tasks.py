import os
import textwrap
from pathlib import Path


def contains_eval(file_content):
    return 'eval(' in file_content


def contains_syntax_error(file_content):
    return 'type: SyntaxError' in file_content


def contains_try_statement(file_content):
    return 'try' in file_content


def is_skip(file):
    file_content = file.read_text()
    return (contains_eval(file_content)
            or contains_syntax_error(file_content)
            or contains_try_statement(file_content)
            or '.isPrototypeOf(' in file_content
            or 'with(' in file_content
            or 'arguments[' in file_content
            or 'Math.' in file_content
            or 'es6id:' in file_content
            or 'features: [default-parameters' in file_content
            or 'assert.throws(' in file_content
            or 'tail-call-optimization' in file_content
            or 'new Array(' in file_content
            or 'new Boolean(' in file_content
            or 'new Function(' in file_content
            or 'new Number(' in file_content
            or 'new String(' in file_content
            or 'new Object(' in file_content)


project_root_dir = Path(__file__).parent.parent.parent
property_file = project_root_dir / 'config/specification/JavaScriptAssertion.spc'
if not property_file.exists():
    print('Property file {} not found'.format(property_file))
    exit(1)
assert_lib_file = \
    project_root_dir / 'test/programs/javascript-test262-benchmark/CPAchecker-test262-assert.js'
if not assert_lib_file.exists():
    print('Assertion library file {} not found'.format(assert_lib_file))
    exit(1)

for file in project_root_dir.glob(
        'test/programs/javascript-test262-benchmark/test/language/statements/*/*.js'):
    if is_skip(file):
        print('SKIP {}'.format(file))
        continue
    else:
        print('GENERATE TASK FOR {}'.format(file))
    relative_path_to_property_file = os.path.relpath(str(property_file), str(file.parent))
    relative_path_to_assert_lib_file = os.path.relpath(str(assert_lib_file), str(file.parent))
    yml_file = file.parent / (file.stem + '.yml')
    yml_file.write_text(textwrap.dedent("""\
        format_version: "1.0"
        input_files:
            - "{assert_lib_file}"
            - "{input_file}"
        properties:
          - property_file: {property_file}
            expected_verdict: true
        """).format(assert_lib_file=relative_path_to_assert_lib_file,
                    input_file='./' + file.name,
                    property_file=relative_path_to_property_file))
