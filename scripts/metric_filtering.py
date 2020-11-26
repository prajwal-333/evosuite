import pandas as pd
import sys
import os

def filter(in_file, metric_name):
    data = pd.read_csv(in_file)
    df = data.drop(data.columns.difference(['LongName',metric_name]), 1)
    df.to_csv("/tmp/Metrics.csv", header=False, index=False)
    print("done")

if __name__ == "__main__":
    in_file = sys.argv[1]
    metric_name = sys.argv[2]
    filter(in_file, metric_name)