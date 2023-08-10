package usc.edu.sql.fpa.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.G;
import soot.Pack;
import soot.PackManager;
import soot.SceneTransformer;
import soot.Transform;
import usc.edu.sql.fpa.analysis.intra.AppAnalysis;
import usc.edu.sql.fpa.utils.Constants;

public class FPAMain {
	public static void main(String[] args) {
		Constants.BUNDLE_PATH = args[0];
		Constants.ANDROID_JAR = args[1];
		if (args.length > 2)
			Constants.output = args[2];
		if (args.length > 3)
			Constants.LOG = args[3];
		Constants.ANALYZED_LIST_FILE = Constants.output + "/analyzed.txt";
		Set<String> bundle = getAppsPathInBundle(Constants.BUNDLE_PATH);
		System.out.println("Start Analyzing Apps in Bundle: " + Constants.BUNDLE_PATH);
		for (String app : bundle) {
			Constants.APK_PATH = app;
			new FPAMain().process(Constants.APK_PATH, Constants.ANDROID_JAR);
			addToAnalyzedApps(app);

		}
	}

	private static void addToAnalyzedApps(String app) {
		try (FileWriter fw = new FileWriter(Constants.ANALYZED_LIST_FILE, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println(app);
		} catch (IOException e) {
			// exception handling left as an exercise for the reader
		}

	}

	public void process(String apkPath, String androidJar) {
		setupAndInvokeSoot(apkPath, androidJar);
	}

	void setupAndInvokeSoot(String apkPath, String androidJAr) {
		String packName = "wjtp";
		String phaseName = "wjtp.analysis";

		String[] sootArgs = { "-w", "-p", phaseName, "enabled:true", "-keep-line-number", "-keep-offset",
				"-allow-phantom-refs", "-process-multiple-dex", "-src-prec", "apk", "-f", "none", "-force-android-jar",
				androidJAr, "-process-dir", apkPath };
		G.reset();
		setupAndInvokeSootHelper(packName, phaseName, sootArgs, apkPath);
	}

	void setupAndInvokeSootHelper(String packName, String phaseName, String[] sootArgs, String apkPath) {
		Pack pack = PackManager.v().getPack(packName);
		pack.add(new Transform(phaseName, new SceneTransformer() {
			@Override
			protected void internalTransform(String phaseName, Map<String, String> options) {
				System.out.println("Analysis started");
				AppAnalysis ma = new AppAnalysis();
				ma.run();
				System.out.println("Analysis ended");

			}
		}));

		soot.Main.main(sootArgs);

	}

	public static Set<String> getAppsPathInBundle(String bundlePath) {
		Set<String> apps = new HashSet<>();

		File folder = new File(bundlePath);
		if (folder.isFile()) {
			apps.add(folder.getAbsolutePath());
			return apps;
		}
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				if (listOfFiles[i].getName().endsWith(".apk"))
					apps.add(listOfFiles[i].getAbsolutePath());
			}
		}
		return apps;
	}

}
