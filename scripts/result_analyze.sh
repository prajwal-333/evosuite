basepath=$1
projectname=$2
iteration=$3

echo "project = $projectname, iteration=$iteration"
bugno=0

if [ $1 == "Chart" ]; then
 bugno=26
elif [ $1 == "Closure" ]; then 
 bugno=133
elif [ $1 == "Lang" ]; then 
 bugno=65
elif [ $1 == "Math" ]; then 
 bugno=106
elif [ $1 == "Mockito" ]; then 
 bugno=38
elif [ $1 == "Time" ]; then 
 bugno=27
fi

for i in {1..$bugno}
do
    bugid="$i"
    echo "INFO: checking bug $bugid"
    matrixfile=$basepath/bug"$bugid"/$iteration/fl/fault_localization_log/Chart/evosuite-VCMDDU2/"$bugid"b."$bugid".sfl/txt/matrix.txt
    if [ ! -f $matrixfile ]; then
        echo "ERR: no data found for bug $bugid"
        continue
    fi 

    failingtests=$(grep -c "-" $matrixfile)
    if [ $failingtests -eq 0 ]; then 
        echo "INFO: no failing tests found for bug $bugid"
        continue
    fi

    faultcsv="$basepath/bug"$bugid"/$iteration/fl/fault_localization"
    python3 analyze.py $faultcsv

done