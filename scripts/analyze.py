import pandas as pd
import sys
import os

def filter(in_file):
    data = pd.read_csv(in_file)
    mean_val=data.iloc[0]["mean_score_of_loaded_classes"]
    median_val=data.iloc[0]["median_score_of_loaded_classes"]
    print("mean = " + str(mean_val) + ",  median = " + str(median_val))
    

if __name__ == "__main__":
    in_file = sys.argv[1]
    filter(in_file)
