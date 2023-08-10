package usc.edu.sql.fpa.utils;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import usc.edu.sql.fpa.analysis.bundle.INode;

public class BundleAnalysisUtils {
	public static int getNodesSize(Map<String, Set<INode>> snodes) {
		int count = 0;
		for (String str: snodes.keySet()) {
			count += snodes.get(str).size();
		};
		return count;
	}

	public static void cleanUpData(File folder) {
		File[] files = folder.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
			for (File f : files) {
				if (f.isDirectory()) {
					cleanUpData(f);
				} else {
					f.delete();
				}
			}
		}
		if (!folder.getAbsolutePath().equals(Constants.output))
			folder.delete();

	}

	public static void readData(String filePath, String fileName, Map<String, Set<INode>> nodes) {
		final File folder = new File(filePath);
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory() && fileEntry.getName().endsWith(".apk")) {
				nodes.put(fileEntry.getName(), new HashSet<>());
				readData(fileEntry.getAbsolutePath() + File.separator + fileName, fileName, nodes);
			} else if (fileEntry.getName().endsWith(".ser")) {
				String app = fileEntry.getParentFile().getParentFile().getName();
				nodes.get(app).add(FPAFileHandler.deserialize(fileEntry.getAbsolutePath()));
			}
		}

	}

}
