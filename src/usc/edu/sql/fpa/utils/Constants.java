package usc.edu.sql.fpa.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {
	public static final int MAX_LOOPS = 2;
	public static final String ALL_EXTRA_KEY = "@@ALL@@";
	public static final String MAIN_ACTION = "android.intent.action.MAIN";
	public static final String MAIN_CATEGORY = "android.intent.category.LAUNCHER";
	public static final String EXTRA_BUNDLE_KEY = "&#@$KEY@ads923746aj!$&#$!#^@#";
	public static final String BOOL = "true@@";
	public static final String RANDOM_SPLITTER = "aiwdbf3q79tgfwbiuFJZO049hkcb";
	public static String NULL = "!@#$#%#$%#^%^@#$NULL";
	public static String LOG = "";
	public static String BUNDLE_NAME = null;
	public static String APP_PACKAGE = null;
	public static String ANDROID_JAR = null;
	public static String APK_PATH = null;
	public static String output = "";
	public static String SPOINT_FILE_NAME = "spoints";
	public static String RPOINT_FILE_NAME = "rpoints";
	public static String RET_POINT_FILE_NAME = "opoints";
	public static String PUT_PARCEL_POINT_FILE_NAME = "ppoints";
	public static Map<String, String> APP_EXTRA_INFO = new HashMap<String, String>();
	public static Map<String, String> BUNDLE_EXTRA_INFO = new HashMap<String, String>();
//	public enum ATTRIBUTE {
//		CLASS_CONSTANT, CLASSNAME, SCHEME, PACKAGE, IDENTIFIER, ACTION, CATEGOR, EXTRA, DATA, TYPE, COMPONENT, FLAG,
//		INIT1_CLONE, INIT1_ACTION, INIT2_EXPLICIT, INIT2_IMPLICIT, INIT4, CHOOSER, SOURCE_BOUND, CLIP_DATA, OTHER,
//		GETINTENT, DATA_TYPE, RET_INTENT, DEF
//	}

	public enum ATTRIBUTE {
		SCHEME, PACKAGE, IDENTIFIER, ACTION, CATEGOR, EXTRA, DATA, TYPE, COMPONENT, FLAG, DATA_TYPE, DEF, INIT1_CLONE,
		INIT1_ACTION, INIT2_EXPLICIT, INIT2_IMPLICIT, INIT4, CLASS, CAST, RES
	}

	public enum API_TYPE {
		SET, GET, HAS, INIT, OTHER, ADD, RET_INTENT
	}

	public enum COMPONENT {
		ACTIVITY, BROADCAST_RECEIVER, SERVICE, CONTENT_PROVIDER, INTENT, OTHER, NONE, VIEW, FRAGMENT,
		ACTIVITY_RECEIVE_CALLBACK, FRAGEMENT, CONTENTPROVIDER, APPLICATION, BASIC_LISTENER, OTHER_LISTENER,
		ACTIVITY_RECEIVE_API, SERVICE_RECEIVE_CALLBACK, BROADCAST_RECEIVER_CALLBACK, PARCELABLE

	}

	public enum INVOCATION_TYPE {
		INITIATING_INTENT, INITIATION_URI, DEFINING, RETURNING, RECEIVING, RETREVING, INTENT_ARGUMENT, PUT_PARCLEABLE,
		GET_PARCLEABLE, INTENT_INVOKE_DEFINE
	}

	public static List<String> GET_KEYWORDS = Arrays.asList("get", "describe", "filterHashCode", "normalize", "read",
			"resolve", "write", "to");
	public static List<String> SET_KEYWORDS = Arrays.asList("put", "set", "fill", "make", "parse", "remove", "replace");
	public static List<String> ADD_KEYWORDS = Arrays.asList("add");

	public static List<String> HAS_KEYWORDS = Arrays.asList("has", "filterEquals");
	public static String BUNDLE_PATH;
	public static final boolean IS_BUNDLE_ANALYSIS = true;
	public static final boolean IS_COMPLEX_STR = true;

	public static String ANALYZED_LIST_FILE;
	public static int MAX_iter = -1;
}
