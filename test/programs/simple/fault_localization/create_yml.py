from os import listdir
from os.path import isfile, join
mypath="./"
onlyfiles = [f for f in listdir(mypath) if isfile(join(mypath, f)) and (".c" in f)]
for s in onlyfiles:
	name=s[:-2]+".yml"
	f = open(name, "w")
	f.write("format_version: '1.0'\ninput_files: '"+s+"'\n\nproperties:\n  - property_file: ../../../../config/properties/unreach-label.prp\n    expected_verdict: false")
	f.close()
