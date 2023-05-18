import sys
import re
import json

assert(len(sys.argv) >= 2)

string = sys.argv[1]

outputfile = sys.argv[2] if len(sys.argv) >= 3 else None

pattern = r'^([\n|\t| ]*@include\([a-z|0-9|.|_|\/]*\)[\n|\t| ]*)*$'

files = []

if re.search("@include", string, re.IGNORECASE):
    if re.match(pattern, string):
        matches = re.findall(r'@include\((.*?)\)', string)
        files.extend(matches)
    else:
        sys.exit("Illformed string")

if outputfile is None:
    print(json.dumps(files))
else:
    with open(outputfile, 'w+') as f:
        json.dump(files, f)