import requests, os, re
from bs4 import BeautifulSoup

with open(os.path.dirname(os.path.realpath(__file__)) + '/tests_bad.txt') as f:
    lines = f.read().splitlines()
tests_bad = list(set(lines))

with open(os.path.dirname(os.path.realpath(__file__)) + '/tests_good.txt') as f:
    lines = f.read().splitlines()
tests_good = list(set(lines))

with open(os.path.dirname(os.path.realpath(__file__)) + '/NIST_template.yml') as f:
    template = f.read()

def download(tests, test_suite):
    for i, test in enumerate(tests):

        print(str(i+1)+"/"+str(len(tests)))
        source = ""
        if(test_suite == 100 and len(tests_good) < len(tests_bad)):
            r = requests.get('https://samate.nist.gov/SARD/view_testcase.php?tID='+test)
            source = r.text
            soup = BeautifulSoup(r.text, 'html.parser')
            text = soup.get_text()
            match = re.search(r'.*?Good pair:\s(\d{1,6}).*?Test suite:\s(\d{1,4})', text)
            if match and match.group(1) and match.group(2):
                testcase_good = match.group(1)
                tests_good.append(testcase_good)
            else:
                print("Not found")
        if(os.path.exists(os.path.dirname(os.path.realpath(__file__)) + "/nist/"+str(test_suite)+"/NIST_"+test+".c")):
            continue
            # print(text)
        if(source == ""):
            r = requests.get('https://samate.nist.gov/SARD/view_testcase.php?tID='+test)
            source = r.text
        r = requests.get('https://samate.nist.gov/SARD/view_testcase.php?tID='+test)
        source = r.text
        match = re.search(r'getFile\(\'(.*?)\'', source)
        if match and match.group(1):
            r = requests.get('https://samate.nist.gov/SARD/'+match.group(1))
            source = r.text
            yml = template.replace('***', "NIST_"+test+".c")
            f = open(os.path.dirname(os.path.realpath(__file__)) + "/nist/"+str(test_suite)+"/NIST_"+test+".c", "w")
            f.write(source)
            f.close()

            f = open(os.path.dirname(os.path.realpath(__file__)) + "/nist/"+str(test_suite)+"/NIST_"+test+".yml", "w")
            f.write(yml)
            f.close()
# f = open(os.path.dirname(os.path.realpath(__file__)) + "./links.txt", "w",)
# for link in links:
#     f.write(link+"\n")
# f.close()
download(tests_bad, 100)
with open(os.path.dirname(os.path.realpath(__file__)) + '/tests_good.txt', "w") as f:
    for t in tests_good:
        f.write(str(t) +"\n")
download(tests_good, 101)