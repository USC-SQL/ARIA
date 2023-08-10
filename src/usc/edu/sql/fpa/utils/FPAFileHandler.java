package usc.edu.sql.fpa.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;

import usc.edu.sql.fpa.analysis.bundle.INode;
import usc.edu.sql.fpa.analysis.interpret.IntentInterpreter;
import usc.edu.sql.fpa.model.IC3Intent;
import usc.edu.sql.fpa.model.IntentIR;

public class FPAFileHandler {

	public static void createAppAnalysisOutputFileStructure() {
		String basePath = Constants.output + File.separator + extractAppName();
		String[] files = { Constants.SPOINT_FILE_NAME, Constants.RPOINT_FILE_NAME, Constants.RET_POINT_FILE_NAME, "" };
		for (String filename : files) {
			String filePath = basePath + File.separator + filename;
			File theDir = new File(filePath);
			if (!theDir.exists()) {
				theDir.mkdirs();
			}
		}
	}

	public static String getCurrentAppOutputPathFor(String name) {
		return Constants.output + File.separator + extractAppName() + File.separator + name;
	}

	private static String extractAppName() {
		String[] temp = Constants.APK_PATH.split(File.separator);
		return temp[temp.length - 1];
	}

	public static void serialize(Object o, String filePath) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(filePath);

			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(o);
			oos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static INode deserialize(String filePath) {
		FileInputStream fis = null;
		INode inode = null;
		try {
			fis = new FileInputStream(filePath);
			ObjectInputStream ois = new ObjectInputStream(fis);
			inode = (INode) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return inode;
	}

}
