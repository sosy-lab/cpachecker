import requests, os, re

with open(os.path.dirname(os.path.realpath(__file__)) + '/tests.txt') as f:
    lines = f.read().splitlines()
tests = list(set(lines))

with open(os.path.dirname(os.path.realpath(__file__)) + '/NIST_template.yml') as f:
    template = f.read()

for test in tests:
    if(os.path.exists(os.path.dirname(os.path.realpath(__file__)) + "/nist/NIST_"+test+".c")):
        continue
    r = requests.get('https://samate.nist.gov/SARD/view_testcase.php?tID='+test)
    source = r.text
    match = re.search(r'getFile\(\'(.*?)\'', source)
    if match and match.group(1):
        r = requests.get('https://samate.nist.gov/SARD/'+match.group(1))
        source = r.text
        yml = template.replace('***', "NIST_"+test+".c")
        f = open(os.path.dirname(os.path.realpath(__file__)) + "/nist/NIST_"+test+".c", "w")
        f.write(source)
        f.close()

        f = open(os.path.dirname(os.path.realpath(__file__)) + "/nist/NIST_"+test+".yml", "w")
        f.write(yml)
        f.close()
# f = open(os.path.dirname(os.path.realpath(__file__)) + "./links.txt", "w",)
# for link in links:
#     f.write(link+"\n")
# f.close()