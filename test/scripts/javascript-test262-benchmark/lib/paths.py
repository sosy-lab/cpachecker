from pathlib import Path


def get_project_root_dir():
    return Path(__file__).parent.parent.parent.parent.parent


def get_test262_test_dir():
    return get_project_root_dir() / 'test/programs/javascript-test262-benchmark/test/'
