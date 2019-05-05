import esprima

from lib.metadata import get_raw_meta_data
from lib.paths import get_test262_test_dir
from lib.print import eprint


def main():
    counter = 0
    es5_counter = 0
    es6_counter = 0
    esid_counter = 0
    no_esid_counter = 0
    meta_data_counter = 0
    for file in get_test262_test_dir().glob('language/**/*.js'):
        if file.name.endswith('_FIXTURE.js'):
            continue
        file_content = file.read_text()
        try:
            esprima.parse(file_content)
        except:
            counter += 1
            meta_data = get_raw_meta_data(file_content)
            if meta_data is None:
                meta_data_counter += 1
                eprint(file)
            elif 'es5id' in meta_data:
                es5_counter += 1
            elif 'es6id' in meta_data:
                es6_counter += 1
            elif 'esid' in meta_data:
                esid_counter += 1
            else:
                no_esid_counter += 1
                # eprint(file)
            # eprint('could not parse {} due to {}'.format(file, sys.exc_info()[0]))
    print('counter: {}'.format(counter))
    print('es5_counter: {}'.format(es5_counter))
    print('es6_counter: {}'.format(es6_counter))
    print('esid_counter: {}'.format(esid_counter))
    print('no_esid_counter: {}'.format(no_esid_counter))
    print('meta_data_counter: {}'.format(meta_data_counter))


if __name__ == '__main__':
    main()
