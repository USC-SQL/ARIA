package usc.edu.sql.fpa.output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;

import soot.jimple.Constant;
import usc.edu.sql.fpa.analysis.intra.IType;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.IntentIR;
import usc.edu.sql.fpa.utils.AnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.sql.ir.Expression;
import usc.sql.ir.Variable;

public class Reporter {

	public static void writeIntentIR(Map< CodePoint, Set<IntentIR>> intentsAtSpoints,
			Map<CodePoint, Set<IType>> itypesAtSpoints) {
		List<String[]> data = new ArrayList<String[]>();
		List<String[]> otherInfo = new ArrayList<String[]>();
		for (CodePoint cp : itypesAtSpoints.keySet()) {
			String[] d = new String[5];
			d[0] = cp.getMethod().getSignature();
			d[1] = cp.getUnit().toString();
			d[2] = intentsAtSpoints.get(cp).size() + "";
			Set<IntentIR> set = intentsAtSpoints.get(cp);
			Set<IType> set2 = itypesAtSpoints.get(cp);
			try {
				d[3] = set.toString();
			} catch (Exception e) {
				d[3] = "";
			}
			try {
				d[4] = set2.toString();
			} catch (Exception e) {
				d[4] = "";
			}
			data.add(d);

		}
		for (String key : Constants.APP_EXTRA_INFO.keySet()) {
			String[] row = { key, Constants.APP_EXTRA_INFO.get(key) };
			otherInfo.add(row);
		}
		writeCSVFile(Constants.output + "/" + AnalysisUtils.getAppName() + "_intentIR.csv", data, false);
		writeCSVFile(Constants.output + "/" + AnalysisUtils.getAppName() + "_appOtherInfo.csv", otherInfo, false);

	}

	private static boolean isReused(IntentIR iir) {
		// TODO Auto-generated method stub
		return false;
	}

	public static void writeCSVFile(String filePath, List<String[]> data, boolean isAppend) {
		File file = new File(filePath);
		try {
			// create FileWriter object with file as parameter
			FileWriter outputfile = new FileWriter(file, isAppend);

			// create CSVWriter object filewriter object as parameter
			CSVWriter writer = new CSVWriter(outputfile);

//	        // adding header to csv
//	        String[] header = { "declaring method", "stmt", "IntentIR" };
//	        writer.writeNext(header);

			// add data to csv
			for (String[] d : data) {
				writer.writeNext(d);
			}

			// closing writer connection
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
