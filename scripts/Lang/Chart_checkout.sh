#!/bin/bash

projectname="Lang"

for i in {1..65}
#for (( i=$1; i<=$2; i++ ))
do
bugid="$i"
# checkout project source
mkdir -p /tmp/sources/$projectname/"$bugid"f
defects4j checkout -p $projectname -v "$bugid"f -w /tmp/sources/$projectname/"$bugid"f

done