To run ARIA on a bundle of apps, we use a script runner.py with the following command:

python runner.py [bundle_path] [intra component module] [inter-component module]

These modules are provided as jar files: (1) FPAApp.jar is the module for intra component analysis and (2) FPABundle.jar is the module for inter component analysis.

Example:

python3 runner.py benchmarks/B2 FPAApp.jar FPABundle.jar

The results can be found under [bundle_path]/out: 
(1) fpa_[bundle_name]_all_intents.csv contains Intents in this bundle
(2) fpa_[bundle_name]_merged_out.csv contains the ICC links in this bundle


Please make sure that the android.jar is located at the same directory as the runner.py script (for simplicity, use the structure shown here)
A bundle is a directory with a set of .apk files
You will need Java 13 (or up) and python 3 to run the tool successfully. 
