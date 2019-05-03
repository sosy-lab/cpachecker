import json

from lib.metadata import get_meta_data
from lib.paths import get_test262_test_dir
from lib.print import eprint


def main():
    for file in get_test262_test_dir().glob('language/asi/S7.9_A6.3_T7.js'):
        file_content = file.read_text()
        meta_data = get_meta_data(file, file_content)
        eprint(json.dumps(meta_data, indent=4, sort_keys=False))


if __name__ == '__main__':
    main()
