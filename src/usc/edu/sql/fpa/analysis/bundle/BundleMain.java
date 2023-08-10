package usc.edu.sql.fpa.analysis.bundle;

import java.io.File;
import java.util.*;

import usc.edu.sql.fpa.utils.BundleAnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.FPAFileHandler;

public class BundleMain {

	public static void main(String[] args) {
		Constants.BUNDLE_NAME = args[0];
		if (args.length > 1)
			Constants.output = args[1];
		if (args.length > 2)
			Constants.MAX_iter = Integer.parseInt(args[2]);
		// read data
		long start = System.currentTimeMillis();
		Map<String, Set<INode>> snodes = new HashMap<>();
		BundleAnalysisUtils.readData(Constants.output, Constants.SPOINT_FILE_NAME, snodes);
		Map<String, Set<INode>> rnodes = new HashMap<>();
		BundleAnalysisUtils.readData(Constants.output, Constants.RPOINT_FILE_NAME, rnodes);
		Map<String, Set<INode>> onodes = new HashMap<>();
		BundleAnalysisUtils.readData(Constants.output, Constants.RET_POINT_FILE_NAME, onodes);
		long runtime = System.currentTimeMillis() - start;
		Constants.BUNDLE_EXTRA_INFO.put("READ_TIME", runtime + "");
		// cleanUpData(new File(Constants.output));
		// create init graph
		System.out.println("analyzing " + snodes.size() + " apps with sending points");
		System.out.println("analyzing " + rnodes.size() + " apps with receiving points");
		System.out.println("Total " + BundleAnalysisUtils.getNodesSize(snodes) + " sending points");
		System.out.println("Total " + BundleAnalysisUtils.getNodesSize(rnodes) + " receiving points");
		Map<String, Set<INode>> rnodes2 = new HashMap<>();
		Map<String, Set<INode>> onodes2 = new HashMap<>();
		Map<String, Set<INode>> rnodesOncreate = new HashMap<>();
		Set<String> rcomponents = new HashSet<>(); 
		int count = 0;
		for (String s : rnodes.keySet()) {
			for (INode node : rnodes.get(s)) {
				if (node.getMethod().contains("onCreate")) {
					if (node.getUnitStr().contains("@this:")) {
						addToMap(s, node, rnodesOncreate);
						continue;
					}
				}
				addToMap(s, node, rnodes2);
				if (null != node.getComponent())
					rcomponents.add(node.getComponent().getName());
			}
		}
		for (String s : rnodesOncreate.keySet()) {
			for (INode node : rnodes.get(s)) {
				if (node.getComponent() != null) {
					if (!rcomponents.contains(node.getComponent().getName())) {
						addToMap(s, node, rnodes2);
					}
				}
			}
		}

		for (String s : onodes.keySet()) {
			for (INode node : onodes.get(s)) {

				if (node.getUnitStr().contains("getExtras")) {
					count++;
					continue;
				}
				if (!onodes2.containsKey(s)) {
					onodes2.put(s, new HashSet<>());
				}
				onodes2.get(s).add(node);
			}
		}
		System.out.println(count);
		BundleAnalysis ba = new BundleAnalysis(snodes, rnodes2, onodes2);
		Constants.BUNDLE_EXTRA_INFO.put("APP_SNODE", snodes.size() + "");
		Constants.BUNDLE_EXTRA_INFO.put("APP_RNODE", rnodes2.size() + "");
		Constants.BUNDLE_EXTRA_INFO.put("SNODES", BundleAnalysisUtils.getNodesSize(snodes) + "");
		Constants.BUNDLE_EXTRA_INFO.put("RNODES", BundleAnalysisUtils.getNodesSize(rnodes2) + "");
		// update graph
		ba.run();

	}

	private static void addToMap(String s, INode n, Map<String, Set<INode>> nodes) {
		if (!nodes.containsKey(s)) {
			nodes.put(s, new HashSet<>());
		}
		nodes.get(s).add(n);
	}

}
