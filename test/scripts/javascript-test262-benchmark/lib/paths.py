from pathlib import Path

from lib.print import eprint


def get_project_root_dir():
    return Path(__file__).parent.parent.parent.parent.parent


def get_test262_test_dir():
    return get_project_root_dir() / 'test/programs/javascript-test262-benchmark/test/'


def get_test262_supported_features_file_patterns():
    # commented patterns contain files with unsupported features
    return [
        # 'language/arguments-object/**/*.js',
        'language/asi/**/*.js',
        # 'language/block-scope/**/*.js',
        'language/comments/**/*.js',
        # 'language/computed-property-names/**/*.js',
        # 'language/destructuring/**/*.js',
        # 'language/directive-prologue/**/*.js',
        # 'language/eval-code/**/*.js',
        # 'language/export/**/*.js',
        'language/expressions/**/*.js',
        'language/function-code/**/*.js',
        'language/future-reserved-words/**/*.js',
        # 'language/global-code/**/*.js',
        'language/identifier-resolution/**/*.js',
        'language/identifiers/**/*.js',
        # 'language/import/**/*.js',
        'language/keywords/**/*.js',
        'language/line-terminators/**/*.js',
        'language/literals/**/*.js',
        # 'language/module-code/**/*.js',
        'language/punctuators/**/*.js',
        'language/reserved-words/**/*.js',
        # 'language/rest-parameters/**/*.js',
        'language/source-text/**/*.js',
        'language/statements/**/*.js',
        'language/types/**/*.js',
        'language/white-space/**/*.js',
    ]


def get_test262_supported_features_files():
    test_dir = get_test262_test_dir()
    for file_pattern in get_test262_supported_features_file_patterns():
        if len(list(test_dir.glob(file_pattern))) < 1:
            eprint('pattern matches no files {}'.format(file_pattern))
        for file in test_dir.glob(file_pattern):
            yield file

