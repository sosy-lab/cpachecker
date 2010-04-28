#!/usr/bin/env python

from subprocess import PIPE, Popen

import operator
import optparse
import os
import re
import subprocess
import time


class TestResult(object):
    pass


project_dir = '..'
config_dir = project_dir + '/test/config'
log_dir = project_dir + '/test/output'
test_set_dir = project_dir + '/test/test-sets' 

header = '''\
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
    "http://www.w3.org/TR/html4/strict.dtd"
<html>
<head>
<title>%s</title>
<style type="text/css">
td, th {
  border: 1px solid black;
  padding-left: 10px;
  padding-right: 10px;
  text-align: center;
}
th {
  background-color: lightgray;
}
tr.green {
  background-color: green;
}
tr.orange {
  background-color: orange;
}
tr.red {
  background-color: red;
}
tr.violet {
  background-color: violet;
}
</style>
</head>
<body>
'''

table_head = '''\
  <thead>
    <tr>
      <th>INSTANCE</th>
      <th>TIME</th>
      <th>OUTCOME</th>
      <th>Reached</th>
      <th>Refinements</th>
      <th>Abstractions</th>
      <th>Blocksize</th>    
    </tr>
  </thead>
'''

table_row = '''\
  <tr class="%s">
    <td>%s</td>
    <td>%s</td>
    <td>%s</td>
    <td>%s</td>
    <td>%s</td>
    <td>%s</td>
    <td>%s</td>
  </tr>
'''

def svn_revision():
    output = Popen(['svn', 'info', project_dir], stdout=PIPE).communicate()[0]
    svn_info = dict(map(lambda str: tuple(str.split(': ')),
                        output.strip('\n').split('\n')))
    return svn_info['Revision']

def os_name():
    return ' '.join(os.uname())

footer = '''\
<p>Host: %s</p>
<p>Revision: %s</p>
</body>
</html>
''' % (os_name(), svn_revision())

table_row_offset = 2
table_columns = 7
instance_column = 0
outcome_column = 2

def list_of_files_in(dir):
    result = []
    for dirpath, dirnames, filenames in os.walk(dir):
        for file in filenames:
            result.append(file)
    return result

def run_tests(test_sets, configs):
    test_results = []
    for test_set in test_sets:
        test_set_name = test_set.replace('.set', '')
        for config in configs:
            config_name = config.replace('.properties', '')
            isodate = time.strftime('%Y-%m-%d')
            subprocess.call(['./run_test_set.sh', test_set, config])
            logfile = '%s/test_%s.%s.log' % (log_dir, isodate, config_name)
            data = []
    
            with open(logfile) as log:
                for line in log.readlines()[table_row_offset:]:
                    row = re.sub('\s\s+', '\t', line).split('\t')
                    row[len(row) - 1] = row[len(row) - 1].replace('\n', '')
                    for i in range(table_columns - len(row)):
                        row.append('')
                    data.append(row)
           
            test_result = TestResult()
            test_result.test_set_name = test_set_name
            test_result.config_name = config_name
            test_result.data = data
            test_results.append(test_result)
    
    return test_results

def generate_html(test_results, key_attr, value_attr, write_footer):
    key_attrgetter = operator.attrgetter(key_attr)
    value_attrgetter = operator.attrgetter(value_attr)
    
    test_result_map = {}
    for test_result in test_results:
        key = key_attrgetter(test_result)
        if key in test_result_map:
            test_result_map[key].append(test_result)
        else:
            test_result_map[key] = [test_result]
    
    for key in test_result_map.iterkeys():
        results = test_result_map[key]
        results.sort(key=value_attrgetter)
        html_file = key + '.html'
        with open(html_file, 'a') as out:
            out.write(header % html_file)
            out.write('\n')
            
            out.write('<a href="index.html"> back</a>\n')
            out.write('\n')
            
            out.write('<ol>\n')
            for test_result in results:
                name = value_attrgetter(test_result)
                out.write('  <li><a href="#%s">%s</a></li>\n' % (name, name))
            out.write('</ol>\n')
            out.write('\n')
            
            for test_result in results:
                name = value_attrgetter(test_result)
                data = test_result.data
                
                out.write('<a name="%s"><h1>%s</h1></a>\n' % (name, name))
                out.write('\n')
                
                out.write('<table rules="groups">\n')
                out.write(table_head)
                out.write('  <tbody>\n')
                for row in data:
                    if row[outcome_column] == 'SAFE':
                        if row[instance_column].find('BUG') == -1:
                            color = 'green'
                        else:
                            color = 'red'
                    elif row[outcome_column] == 'UNSAFE':
                        if row[instance_column].find('BUG') == -1:
                            color = 'red'
                        else:
                            color = 'green'
                    else:
                        color = 'violet'
                    out.write(table_row % tuple([color] + row))
                out.write('  </tbody>\n')
                out.write('</table>\n')
                out.write('\n')
            
            out.write('<a href="index.html"> back</a>\n')
            out.write('\n')
            
            out.write(footer)
            
    with open('index.html', 'a') as out:
        out.write(header % 'index.html')
        out.write('\n')
        
        out.write('<ul>\n')
        for key in test_result_map.iterkeys():
            html_file = key + '.html'
            out.write('  <li><a href="%s">%s</a></li>\n' % (html_file, key))
        out.write('</ul>\n')
        out.write('\n')
        
        if write_footer:
            out.write(footer)

def main():
    parser = optparse.OptionParser()
    parser.add_option('-c', '--config',
                      action='store', type='string', dest='config_re',
                      default=r'^.+\.properties$',
                      help='include the config files matching PCRE',
                      metavar='PCRE')
    parser.add_option('-s', '--set',
                      action='store', type='string', dest='set_re',
                      default=r'^(?!benchmark-).+\.set$',
                      help='include the test-set files matching PCRE',
                      metavar='PCRE')
    (options, args) = parser.parse_args()
    
    test_sets = filter(lambda str: re.match(options.set_re, str),
                       list_of_files_in(test_set_dir))
    configs = filter(lambda str: re.match(options.config_re, str),
                     list_of_files_in(config_dir))

    test_results = run_tests(test_sets, configs)

    generate_html(test_results, 'test_set_name', 'config_name', False)        
    generate_html(test_results, 'config_name', 'test_set_name', True)
            

if __name__ == '__main__':
    main()