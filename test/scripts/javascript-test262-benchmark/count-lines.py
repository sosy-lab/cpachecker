from lib.paths import get_test262_test_dir


def main():
    files_glob = get_test262_test_dir().glob('language/**/*.js')
    for file in files_glob:
        num_lines = sum(1 for line in open(str(file)))
        if num_lines > 1000:
            print('{:<4}\t {}'.format(num_lines, file))


if __name__ == '__main__':
    main()
