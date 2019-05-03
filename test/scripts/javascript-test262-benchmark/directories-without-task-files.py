from lib.paths import get_test262_test_dir
from lib.print import eprint


def main():
    for dir in get_test262_test_dir().glob('language/*/'):
        if len(list(dir.glob('**/*.yml'))) > 0:
            for d in dir.iterdir():
                if not d.is_dir():
                    continue
                print(d)
            # for d in dir.glob('*/'):
            #     print(d)
            #     if len(list(d.glob('**/*.yml'))) > 0:
            #         print(dir)
            #     else:
            #         eprint(dir)
            # return
        else:
            eprint(dir)



if __name__ == '__main__':
    main()
