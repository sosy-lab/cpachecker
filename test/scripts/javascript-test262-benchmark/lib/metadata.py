import re

import yaml

try:
    from StringIO import StringIO
except ImportError:
    from io import StringIO


def parse_yaml_string(ys):
    fd = StringIO(ys)
    dct = yaml.safe_load(fd)
    return dct


def get_meta_data(file, file_content):
    meta_data_match = re.search(r'/\*---(.+?)---\*/', file_content, re.DOTALL)
    if meta_data_match is None:
        # eprint('meta data not found in file {}'.format(file))
        return {'flags': [], 'features': []}
    meta_data = parse_yaml_string(meta_data_match.group(1))
    if 'flags' not in meta_data:
        meta_data['flags'] = []
    if 'features' not in meta_data:
        meta_data['features'] = []
    return meta_data


def get_raw_meta_data(file_content):
    meta_data_match = re.search(r'/\*---(.+?)---\*/', file_content, re.DOTALL)
    if meta_data_match is None:
        return None
    return parse_yaml_string(meta_data_match.group(1))
