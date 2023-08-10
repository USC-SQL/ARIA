package usc.edu.sql.fpa.analysis.intra;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.*;

import ppg.parse.Constant;
import soot.Unit;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;
import usc.edu.sql.fpa.analysis.IRAnalysis;
import usc.edu.sql.fpa.analysis.bundle.INode;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.IntentIR;
import usc.edu.sql.fpa.output.Reporter;
import usc.edu.sql.fpa.utils.AnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.FPAFileHandler;
import usc.edu.sql.fpa.utils.Hierarchy;
import usc.edu.sql.fpa.utils.Parser;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.sql.string.utils.Util;

public class AppAnalysis {

	public void run() {
		System.out.println("Starts Analyzing app: " + Constants.APK_PATH);
		long start = System.currentTimeMillis();
		Hierarchy h = Hierarchy.getInstance(Constants.APK_PATH);
		long hEnd = System.currentTimeMillis();
		IRAnalysis irAnalysis = IRAnalysis.getInstance(Constants.APK_PATH);
		long iEnd = System.currentTimeMillis();
		IntraIntentAnalysis intraAnalysis = new IntraIntentAnalysis();
		intraAnalysis.run();
		long aEnd = System.currentTimeMillis();
		saveData(intraAnalysis);
		long dEnd = System.currentTimeMillis();
		Constants.APP_EXTRA_INFO.put("Hier_TIME", (hEnd - start) + "");
		Constants.APP_EXTRA_INFO.put("IR_TIME", (iEnd - hEnd) + "");
		Constants.APP_EXTRA_INFO.put("Analysis_TIME", (aEnd - iEnd) + "");
		Constants.APP_EXTRA_INFO.put("Save_TIME", (dEnd - aEnd + ""));
		Reporter.writeIntentIR(intraAnalysis.intentsAtSpoints, intraAnalysis.itypesAtSpoints);

	}

	private void saveData(IntraIntentAnalysis intraAnalysis) {
		FPAFileHandler.createAppAnalysisOutputFileStructure();

		saveData(intraAnalysis.getIntetIRAtSpoints(), Constants.SPOINT_FILE_NAME, false);
		saveData(intraAnalysis.getIntetIRAtOtherpoints(), Constants.RET_POINT_FILE_NAME, false);
		saveData(intraAnalysis.getIntetIRAtRpoints(), Constants.RPOINT_FILE_NAME, true);

	}

//	private void saveData(Set<RPoint> set, String type) {
//		int count = 0;
//		for (CodePoint cp : set) {
//			INode in = generateINode(cp, null);
//			String path = FPAFileHandler.getCurrentAppOutputPathFor(type);
//			FPAFileHandler.serialize(in, path + File.separator + type + count + ".ser");
//			count++;
//		}
//
//	}

	private void saveData(Map<? extends CodePoint, Set<IntentIR>> map, String type, boolean isRpoint) {
		int count = 0;
		for (CodePoint cp : map.keySet()) {
			INode in = generateINode(cp, map.get(cp), isRpoint);
			String path = FPAFileHandler.getCurrentAppOutputPathFor(type);
			FPAFileHandler.serialize(in, path + File.separator + type + count + ".ser");
			count++;
		}

	}

	private INode generateINode(CodePoint cp, Set<IntentIR> set, boolean isRpoint) {
		INode node = new INode(set, isRpoint);
		node.setApp(cp.getApp());

		node.setComponentType(cp.getComponent().getType());
		node.setComponent(cp.getComponent().getComponent());
		if (AnalysisUtils.getInvokedMethod(cp.getUnit()) != null)
			node.setInvokeExpr(AnalysisUtils.getInvokedMethod(cp.getUnit()).getSubSignature());
		else {
			node.setInvokeExpr(cp.getMethod().getSubSignature());
		}
		node.setOffset(Util.getBytecodeOffset(cp.getUnit()));
		node.setUnitIndex(getUnitIndex(cp));
		node.setMethod(cp.getMethod().getSignature());
		node.setUnitStr(cp.getUnit().toString());
		node.setPackage(Constants.APP_PACKAGE);
		return node;
	}

	public int getUnitIndex(CodePoint cp) {
		int ind = 0;
		for (Unit u : cp.getMethod().retrieveActiveBody().getUnits()) {
			if (u.equals(cp.getUnit()))
				return ind;
			ind++;
		}
		return ind;
	}

}
