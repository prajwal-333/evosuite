import pandas as pd
import sys
import os

def filter(in_file):
    data = pd.read_csv(in_file)
    mean_val=data["mean_score_of_loaded_classes"]
    median_val=data["median_score_of_loaded_classes"]
    print(f"mean = {mean_val},  median = {median_val}")
    

if __name__ == "__main__":
    in_file = sys.argv[1]
    filter(in_file, metric_name, path_name)