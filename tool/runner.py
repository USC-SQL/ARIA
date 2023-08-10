import os
import csv
import sys
import random
import time
import json
import shutil

all_bundle_analyzed = []
android_platforms_path = "android.jar"

def read_csv(csv_file_path):
        res = []
        with open(csv_file_path) as csv_file:
                csv_reader = csv.reader(csv_file)
                line_count = 0
                for row in csv_reader:
                        res.append(row)
        return res


def run_on_bundle(bundle_path, bundle_name, app_jar_path, bundle_jar_path):
        app_count = 0
        if not os.path.isdir(bundle_path):
                return
        bundle_out_path = os.path.join(bundle_path, "out")
        logput = os.path.join(bundle_path, "log")
        if not os.path.exists(bundle_out_path):
                os.mkdir(bundle_out_path)
        if os.path.exists(logput):
                shutil.rmtree(logput)
        os.mkdir(logput)
        print(bundle_out_path)
        for app_path in os.listdir(bundle_path):
                print(app_path)
                if app_path.endswith(".apk"):
                        app_path = os.path.join(bundle_path,app_path)
                        app_count += 1
                        print("java -jar {0} {1} {2} {3} {4}".format(app_jar_path, app_path, android_platforms_path, bundle_out_path, logput))
                        os.system("java -jar {0} {1} {2} {3} {4}".format(app_jar_path, app_path, android_platforms_path, bundle_out_path, logput))
        os.system("java -jar {0} {1} {2}".format(bundle_jar_path, bundle_name, bundle_out_path))
        print("java -jar {0} {1} {2}".format(bundle_jar_path, bundle_name, bundle_out_path))
        return app_count, bundle_out_path



def main():
        bundle_dir = sys.argv[1]
        app_jar_path = sys.argv[2]
        bundle_jar_path = sys.argv[3]

        bundle_name = bundle_dir.split("/")[-1]
        print("Computing ICCs in bundle ", bundle_name, bundle_dir)
        app_count, out_path = run_on_bundle(bundle_dir, bundle_name, app_jar_path, bundle_jar_path)
        

if __name__ == '__main__':
        main()