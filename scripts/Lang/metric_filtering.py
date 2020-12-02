import pandas as pd
import sys
import os

def filter(in_file, metric_name, path_name):
    data = pd.read_csv(in_file)
    df = data.drop(data.columns.difference(['LongName',metric_name]), 1)
    df.to_csv(path_name+"/Metrics.csv", header=False, index=False)
    print("metric created at " + path_name);

if __name__ == "__main__":
    in_file = sys.argv[1]
    metric_name = sys.argv[2]
    path_name = sys.argv[3]
    filter(in_file, metric_name, path_name)