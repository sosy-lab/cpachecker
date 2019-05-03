from lib.metadata import get_meta_data
from lib.paths import get_test262_test_dir


def contains_assertion(file_content):
    assertion_sub_strings = [
        '$ERROR(',
        'assert(',
        'assert.',
    ]
    return any(s in file_content for s in assertion_sub_strings)


def analyze_files(name, files):
    noStrict_files = set()
    parsing_files = set()
    es6id_files = set()
    es5id_files = set()
    unknown_language_version_files = set()

    for file in files:
        file_content = file.read_text()
        meta_data = get_meta_data(file, file_content)
        if 'noStrict' in meta_data['flags']:
            noStrict_files.add(file)
        if not contains_assertion(file_content):
            parsing_files.add(file)
        if 'es6id' in meta_data:
            es6id_files.add(file)
        elif 'es5id' in meta_data:
            es5id_files.add(file)
        else:
            unknown_language_version_files.add(file)
        # if 'yield' in file_content:
        #     if not file in es6id_files:
        #         eprint('yield in non es6 file {}'.format(file))

    table_format_str = '{:<35} {:>5}'
    print(table_format_str.format(name, len(files)))
    print('-' * 40)
    print(table_format_str.format('es6id:', len(es6id_files)))
    print(table_format_str.format('es5id:', len(es5id_files)))
    print(table_format_str.format('unknown_language_version_files:',
                                  len(unknown_language_version_files)))
    print(table_format_str.format('Parsing:', len(parsing_files)))
    print(table_format_str.format('Non-strict tests:', len(noStrict_files)))
    print(table_format_str.format('Parsing and Non-strict tests:',
                                  len(parsing_files.intersection(noStrict_files))))
    print(table_format_str.format('es6 & Non-strict tests:',
                                  len(es6id_files.intersection(noStrict_files))))
    print(table_format_str.format('es? & Non-strict tests:',
                                  len(unknown_language_version_files.intersection(noStrict_files))))
    print('')
    print('')


def main():
    test262_root_dir = get_test262_test_dir()
    test_files = set(test262_root_dir.glob('**/*.js'))
    annexB_files = set(test262_root_dir.glob('annexB/**/*.js'))
    built_ins_files = set(test262_root_dir.glob('built-ins/**/*.js'))
    harness_files = set(test262_root_dir.glob('harness/**/*.js'))
    intl402_files = set(test262_root_dir.glob('intl402/**/*.js'))
    language_files = set(test262_root_dir.glob('language/**/*.js'))

    print('')
    table_format_str = '{:<20} {:>5}'
    print(table_format_str.format('test_files:', len(test_files)))
    print('-' * 25)
    print(table_format_str.format('annexB_files:', len(annexB_files)))
    print(table_format_str.format('built_ins_files:', len(built_ins_files)))
    print(table_format_str.format('harness_files:', len(harness_files)))
    print(table_format_str.format('intl402_files:', len(intl402_files)))
    print(table_format_str.format('language_files:', len(language_files)))
    print('')
    print('')

    analyze_files('built-ins', built_ins_files)
    analyze_files('harness', harness_files)
    analyze_files('language', language_files)


if __name__ == '__main__':
    main()
