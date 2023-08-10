package usc.edu.sql.fpa.output;

import java.util.ArrayList;
import java.util.*;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import usc.edu.sql.fpa.analysis.IRAnalysis;
import usc.edu.sql.fpa.analysis.bundle.INode;
import usc.edu.sql.fpa.model.IC3Intent;
import usc.edu.sql.fpa.model.ICCLink;
import usc.edu.sql.fpa.model.IntentIR;
import usc.edu.sql.fpa.utils.Constants;

public class GraphReporter {

	public static void reportICCOuts(Map<INode, Set<ICCLink>> iccOuts) {
		Set<String[]> data2 = new HashSet<>();
		Set<String[]> otherInfo = new HashSet<>();
		String pkg = null;
		for (INode sNode : iccOuts.keySet()) {
			pkg = sNode.getPackage();
			String[] spoint = { getName(sNode.getApp()), sNode.getMethod(), sNode.getUnitStr(),
					sNode.getUnitIndex() + "" };
			for (ICCLink link : iccOuts.get(sNode)) {
				String[] rpoint = { link.to.getComponent() + "", link.to.getApp() };
				String[] intent = generateIntent(link.intentInterpreted);
				String[] extraInfo = { sNode.getInvokedExpr(), getStringValue(link.intentIR), link.to.getPackage(),
						link.to.getApp() };
				if("{} {}".equalsIgnoreCase(getStringValue(link.intentIR)))
					continue;
				String[] row = concatAll(spoint, rpoint, intent, extraInfo);
				boolean shouldAdd = true;
				for (String[] d : data2) {
					if (Arrays.equals(d, row)) {
						shouldAdd = false;

					}
				}
				if (shouldAdd)
					data2.add(row);

			}
		}
		for (String key : Constants.BUNDLE_EXTRA_INFO.keySet()) {
			String[] row = { key, Constants.BUNDLE_EXTRA_INFO.get(key) };
			otherInfo.add(row);
		}
		Reporter.writeCSVFile(Constants.output + "/fpa_" + Constants.BUNDLE_NAME + "_merged_out.csv",
				new ArrayList<>(data2), false);
		Reporter.writeCSVFile(Constants.output + "/fpa_" + Constants.BUNDLE_NAME + "_extra_info.csv",
				new ArrayList<>(otherInfo), false);
	}

	private static String[] generateIntent(IC3Intent intentInterpreted) {
		List<String> cats = new ArrayList<String>();
		String[] res = { intentInterpreted.getComponent(), intentInterpreted.getComponentPackage(),
				intentInterpreted.getComponentClass(), intentInterpreted.getAction(),
				intentInterpreted.getCategories().toString(), intentInterpreted.getExtras().toString(),
				intentInterpreted.getDataScheme(), intentInterpreted.getDataHost(), intentInterpreted.getDataPath(),
				intentInterpreted.getData(), intentInterpreted.getType() };
		return res;

	}

	private static String[] concatAll(String[]... arrays) {
		String[] result = null;

		for (String[] array : arrays) {
			result = ArrayUtils.addAll(result, array);
		}

		return result;
	}

	private static String getName(String app) {
		String[] temp = app.split("/");

		return temp[temp.length - 1];
	}

	public static void reportICCOuts2(Map<INode, Set<ICCLink>> iccOuts) {
		Set<String[]> data = new HashSet<>();
		Set<String[]> data2 = new HashSet<>();
		String pkg = null;
		for (INode sNode : iccOuts.keySet()) {
			pkg = sNode.getPackage();
			String[] row = { sNode.getApp(), sNode.getPackage(), sNode.getComponent() + "", sNode.getMethod(),
					sNode.getUnitStr(), sNode.getUnitIndex() + "", sNode.getOffset() + "" };
			for (ICCLink link : iccOuts.get(sNode)) {
				String[] row2 = { getStringValue(link.intentInterpreted), getStringValue(link.intentIR),
						link.to.toString(), link.to.getComponent() + "", link.to.getPackage(), link.to.getApp() };
				String[] row3 = { getStringValue(link.intentInterpreted), getStringValue(link.intentIR),
						link.to.getComponent() + "", link.to.getPackage(), link.to.getApp() };
				data.add(ArrayUtils.addAll(row, row2));
				String[] temp = ArrayUtils.addAll(row, row3);
				boolean shouldAdd = true;
				for (String[] d : data2) {
					if (Arrays.equals(d, temp)) {
						shouldAdd = false;

					}
				}
				if (shouldAdd)
					data2.add(temp);

			}
		}
		Reporter.writeCSVFile(Constants.output + "/fpa_" + Constants.BUNDLE_NAME + "_out.csv", new ArrayList<>(data),
				false);
		Reporter.writeCSVFile(Constants.output + "/fpa_" + Constants.BUNDLE_NAME + "_merged_out.csv",
				new ArrayList<>(data2), false);

	}

	private static String getStringValue(Object object) {
		if (object == null)
			return "!!NULL!!";
		return object.toString();
	}

	public static void reportIntents(Set<INode> sNodes, String type) {
		List<String[]> data = new ArrayList<>();
		for (INode s : sNodes) {
			for (IntentIR iir : s.getAllMappings().keySet()) {
				String[] row = { s.getApp(), s.getPackage(), s.getComponent() + "", s.getMethod(), s.getUnitStr(),
						s.getUnitIndex() + "", s.getOffset() + "" };
				for (IC3Intent iic : s.getAllMappings().get(iir)) {
					String[] row2 = { getStringValue(iic), getStringValue(iir), iir.dependsOnIncomingIntent().size()+"", iir.dependsOnIncomingIntent()+"" };
					data.add(ArrayUtils.addAll(row, row2));
				}

			}

		}
		Reporter.writeCSVFile(Constants.output + "/fpa_" + Constants.BUNDLE_NAME + "_" + type + "_intents.csv",
				new ArrayList<>(data), false);

	}
}
