#!/usr/bin/env python

import os, re, subprocess, sys, time

from optparse import OptionParser

config_dir = '../test/config/'
log_dir = '../test/output/'
test_set_dir = '../test/test-sets/' 

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

footer = '''\
</body>
</html>
'''

table_row_offset = 2

table_columns = 7

newline = '\n'

instance_column = 0
outcome_column = 2

def list_of_files_in(dir):
    result = []
    for dirpath, dirnames, filenames in os.walk(dir):
        for file in filenames:
            result.append(file)
    return result

def logfile_to(config):
    isodate = time.strftime('%Y-%m-%d')
    filename = 'test_%s.%s.log' % (isodate, config.replace('.properties', ''))
    return log_dir + filename 

def main():
    parser = OptionParser()
    parser.add_option("-c", "--config",
                      action="store", type="string", dest="config_re",
                      default="^.+\.properties$",
                      help="include the config files matching PCRE",
                      metavar="PCRE")
    parser.add_option("-s", "--set",
                      action="store", type="string", dest="set_re",
                      default="^(?!benchmark-).+\.set$",
                      help="include the test-set files matching PCRE",
                      metavar="PCRE")
    (options, args) = parser.parse_args()
    with open('index.html', 'w') as out:
        out.write(header % 'index.html')
        out.write(newline)
        out.write('<ul>\n')
    test_sets = filter(lambda str: re.match(options.set_re, str),
                       list_of_files_in(test_set_dir))
    configs = filter(lambda str: re.match(options.config_re, str),
                     list_of_files_in(config_dir))
    for test_set in test_sets:
        with open(test_set.replace('.set', '.html'), 'w') as out:
            out.write(header % test_set.replace('.set', '.html'))
            out.write(newline)
            out.write('<a href="index.html"> back</a>\n')
            for config in configs:
                logfile = logfile_to(config)
                subprocess.call(['./run_test_set.sh', test_set, config])
                out.write(newline)
                out.write('<h1>%s</h1>\n' % config.replace('.properties', ''))
                out.write(newline)
                out.write('<table rules="groups">\n')
                out.write(table_head)
                out.write('  <tbody>\n')      
                with open(logfile) as log:
                    for line in log.readlines()[table_row_offset:]:
                        data = re.sub('\s\s+', '\t', line).split('\t')
                        data[len(data) - 1] =\
                                data[len(data) - 1].replace(newline, '')
                        for i in range(table_columns - len(data)):
                            data.append('')
                        if data[outcome_column] == "SAFE":
                            if data[instance_column].find("BUG") == -1:
                                style = "green"
                            else:
                                style = "red"
                        elif data[outcome_column] == "UNSAFE":
                            if data[instance_column].find("BUG") == -1:
                                style = "red"
                            else:
                                style = "green"
                        else:
                            style = "violet"
                        data.insert(0, style)
                        out.write(table_row % tuple(data))
                out.write('  </tbody>\n')
                out.write('</table>\n')
            out.write(newline)
            out.write('<a href="index.html"> back</a>\n')
            out.write(newline)
            out.write(footer)
        with open('index.html', 'a') as out:
            out.write('  <li><a href="%s">%s</a></li>\n' %
                      (test_set.replace('.set', '.html'),
                       test_set.replace('.set', '')))
    with open('index.html', 'a') as out:
        out.write('</ul>\n')
        out.write(newline)
        out.write(footer)

if __name__ == '__main__':
    main()