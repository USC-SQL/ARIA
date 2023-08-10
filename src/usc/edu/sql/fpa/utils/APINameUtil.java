package usc.edu.sql.fpa.utils;

import org.javatuples.Pair;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.Stmt;

import java.util.*;
import java.util.stream.Collectors;

public class APINameUtil {

	public static String getOriginalCallbackName(SootMethod sm) {
		List<List<String>> matchedCallbacks = APIsConstants.iccRespondingCallbacks.stream()
				.filter(strings -> match(sm, strings)).collect(Collectors.toList());
		if (matchedCallbacks.isEmpty()) {
			return sm.getSignature();
		}
		SootClass origCls = Scene.v().getSootClass(matchedCallbacks.get(0).get(0));
		List<SootMethod> overridenMtds = origCls.getMethods().stream()
				.filter(it -> it.getSubSignature().equals(sm.getSubSignature())).collect(Collectors.toList());
		if (overridenMtds.isEmpty()) {
			return sm.getSignature();
		} else
			return overridenMtds.get(0).getSignature();
	}

	public static boolean match(SootMethod invokeMethod, List<String> strings) {
		boolean matchClsName = isAPIinSubClsOf(invokeMethod, strings.get(0));
		boolean matchMtdName = mtdNameMatch(invokeMethod.getName(), strings.get(1));
		return matchClsName && matchMtdName;
	}

	public static boolean mtdNameMatch(String invokedMtdName, String mtdSig) {
		if (mtdSig.equals("*")) {
			return true;
		}
		return invokedMtdName.equals(mtdSig);
	}

	public static boolean isRespondingAPI(SootMethod invokedMethod) {
		return APIsConstants.iccRespondingApis.stream().anyMatch(strings -> match(invokedMethod, strings));
	}

	public static boolean isRetrievingAPI(SootMethod invokedMethod) {
		return APIsConstants.iccRetrievingApis.stream().anyMatch(strings -> match(invokedMethod, strings));
	}

	public static boolean isICCObjCls(Type type) {
		return APIsConstants.ICCObjectCls.contains(type.toString());
	}

	public static boolean isICCObjRecvAPI(SootMethod sootMethod) {
		boolean isRspdAPI = isRespondingAPI(sootMethod);
		return isRspdAPI && isICCObjCls(sootMethod.getReturnType());
	}

	public static boolean isActivatingAPI(SootMethod invokeMethod) {
		return APIsConstants.iccActivitingApis.stream().anyMatch(strings -> match(invokeMethod, strings));
	}

	public static boolean isConstructingAPI(SootMethod invokeMethod) {
		return APIsConstants.iccConstructingApis.stream().anyMatch(strings -> match(invokeMethod, strings));
	}

	public static boolean isICCRelatedCallBack(SootMethod sm) {
		return APIsConstants.iccRespondingCallbacks.stream().anyMatch(strings -> match(sm, strings));
	}

	public static boolean isDeveloperClass(SootClass sc) {
		return isDeveloperClassByString(sc.getName());
	}

	public static boolean isDeveloperClassByString(String scName) {
//        for (String pkg : Parser.getInstance(Config.apkFilePath).getPkg())
//            if (scName.startsWith(pkg))
//                return true;
		if (scName.startsWith("android.") || scName.startsWith("androidx.") || scName.startsWith("com.google")
				|| scName.startsWith("com.facebook") || scName.startsWith("com.android") || scName.startsWith("java")
				|| scName.startsWith("javax"))
			return false;
		return true;
	}

	public static boolean isActivityCallback(SootMethod sootMethod) {
		SootClass dclCls = sootMethod.getDeclaringClass();
		Hierarchy hierarchy = Hierarchy.getInstance(Constants.APK_PATH);
		boolean isActivity = hierarchy.isActivityClass(dclCls);
		boolean startsWithOn = sootMethod.getName().startsWith("on");
		return isActivity && startsWithOn;
	}

	public static boolean isServiceCallback(SootMethod sootMethod) {
		SootClass dclCls = sootMethod.getDeclaringClass();
		Hierarchy hierarchy = Hierarchy.getInstance(Constants.APK_PATH);
		boolean isService = hierarchy.isServiceClass(dclCls);
		boolean startsWithOn = sootMethod.getName().startsWith("on");
		return isService && startsWithOn;
	}

	public static boolean isFragementCallback(SootMethod sootMethod) {
		boolean isFragment = isFragmentAPI(sootMethod);
		boolean startsWithOn = sootMethod.getName().startsWith("on");
		return isFragment && startsWithOn;
	}

	public static boolean isBroadCastCallback(SootMethod sootMethod) {
		SootClass dclCls = sootMethod.getDeclaringClass();
		Hierarchy hierarchy = Hierarchy.getInstance(Constants.APK_PATH);
		boolean isBroadCast = hierarchy.isBroadcastReceiverClass(dclCls);
		boolean startsWithOn = sootMethod.getName().startsWith("on");
		return isBroadCast && startsWithOn;
	}

	public static boolean isContentProviderCallback(SootMethod sootMethod) {
		SootClass dclCls = sootMethod.getDeclaringClass();
		Hierarchy hierarchy = Hierarchy.getInstance(Constants.APK_PATH);
		boolean isContentProvider = hierarchy.isContentProviderClass(dclCls);
		boolean startsWithOn = sootMethod.getName().startsWith("on");
		return isContentProvider && startsWithOn;
	}

	public static boolean isApplicationCallback(SootMethod sootMethod) {
		boolean isAndroidApplication = isAndroidApplicationAPI(sootMethod);
		boolean startsWithOn = sootMethod.getName().startsWith("on");
		return isAndroidApplication && startsWithOn;
	}

	public static boolean isBasicListener(SootMethod sootMethod) {
		SootClass dclCls = sootMethod.getDeclaringClass();
		Hierarchy hierarchy = Hierarchy.getInstance(Constants.APK_PATH);
		Set<SootClass> superCLs = hierarchy.getSupertypes(dclCls);
		return superCLs.stream()
				.anyMatch(sootClass -> APIsConstants.Basic_Listeners.stream()
						.anyMatch(listenerName -> sootClass.getName().contains("android.")
								&& sootClass.getName().contains(listenerName)));
	}

	public static boolean isEventListener(SootMethod sootMethod) {
		SootClass dclCls = sootMethod.getDeclaringClass();
		Hierarchy hierarchy = Hierarchy.getInstance(Constants.APK_PATH);
		Set<SootClass> superCLs = hierarchy.getSupertypes(dclCls);
		boolean isEvListenerCls = superCLs.stream()
				.anyMatch(it -> it.getName().contains("Listener") && it.getName().contains("android."));
		boolean startsWithOn = sootMethod.getName().startsWith("on");
		return isEvListenerCls && startsWithOn;
	}

	private static boolean isAPIinSubClsOf(SootMethod sootMethod, String tgtCls) {
		SootClass dCl = sootMethod.getDeclaringClass();
		SootClass tgt = Scene.v().getSootClass(tgtCls);
		Boolean isSubCls = Hierarchy.getInstance(Constants.APK_PATH).isSubclassOf(dCl, tgt);
//        Boolean isAPIfromTgtCls = tgt.getMethods().stream().anyMatch(it -> it.getSubSignature().equals(sootMethod.getSubSignature()));
		return isSubCls;
	}

	private static boolean isAPIinPackageOf(SootMethod sootMethod, String pkg) {
		return sootMethod.getDeclaringClass().getPackageName().startsWith(pkg);
	}

	public static boolean isCollectionAPI(SootMethod sm) {
//        SootClass declaringClass = sm.getDeclaringClass();
//        SootClass colInterface = Scene.v().getSootClass("java.util.Collection");
//        return declaringClass.getInterfaces().contains(colInterface);
		return isAPIinSubClsOf(sm, "java.util.Collection") || isAPIinSubClsOf(sm, "java.util.ArrayList")
				|| isAPIinSubClsOf(sm, "java.util.Collections") || isAPIinSubClsOf(sm, "java.util.Iterator");
	}

	public static boolean isStringExprAPI(SootMethod sootMethod) {
		String clsName = sootMethod.getDeclaringClass().getName();
		return clsName.contains("String") && clsName.contains("java.");
	}

	public static boolean isFragmentAPI(SootMethod sootMethod) {
		return isAPIinSubClsOf(sootMethod, "android.app.Fragment");
	}

	public static boolean isViewAPI(SootMethod tgtMtd) {
		return isAPIinSubClsOf(tgtMtd, "android.view.View");
	}

	private static boolean isAndroidApplicationAPI(SootMethod sootMethod) {
		return isAPIinSubClsOf(sootMethod, "android.app.Application");
	}

	public static boolean isDialogAPI(SootMethod invokedMethod) {
		return isAPIinSubClsOf(invokedMethod, "android.app.Dialog");
	}

	public static boolean isPreferenceAPI(SootMethod invokedMethod) {
		return isAPIinSubClsOf(invokedMethod, "android.preference.Preference");
	}

	public static boolean isLogAPI(SootMethod invokedMethod) {
		return isAPIinSubClsOf(invokedMethod, "android.util.Log");
	}

	public static boolean isFileReadAPI(SootMethod invokedMethod) {
		return isAPIinSubClsOf(invokedMethod, "java.io.Reader");
	}

	public static boolean isFileWriteAPI(SootMethod invokedMethod) {
		return isAPIinSubClsOf(invokedMethod, "java.io.Writer");
	}

	public static boolean isWebViewAPI(SootMethod invokedMethod) {
		return isAPIinSubClsOf(invokedMethod, "android.webkit.WebView");
	}

	public static boolean isGraphicsAPI(SootMethod invokedMethod) {
		return isAPIinPackageOf(invokedMethod, "android.graphics");
	}

	public static boolean isWebkitAPI(SootMethod invokedMethod) {
		return isAPIinPackageOf(invokedMethod, "android.webkit");
	}

	public static boolean isDatabaseAPI(SootMethod invokedMethod) {
		return isAPIinPackageOf(invokedMethod, "android.database");
	}

	public static boolean isRandomAPI(SootMethod invokedMethod) {
		return isAPIinSubClsOf(invokedMethod, "java.util.Random");
	}

	public static boolean isDateOrTimeAPI(SootMethod invokedMethod) {
		String clsName = invokedMethod.getDeclaringClass().getName();
		String fullMtdName = invokedMethod.getSignature();
		Boolean isJavaOrAndroidAPI = clsName.startsWith("android.") || clsName.startsWith("java.");
		Boolean isDateOrTimeRelatedAPI = fullMtdName.contains("Time") || fullMtdName.contains("Date");
		return isJavaOrAndroidAPI && isDateOrTimeRelatedAPI;
	}

	public static boolean isJavaAPI(SootMethod invokedMethod) {
		return isAPIinPackageOf(invokedMethod, "java.");
	}

	public static boolean isAndroidAPI(SootMethod invokedMethod) {
		return isAPIinPackageOf(invokedMethod, "android.");
	}

	public static Pair<String, String> getAPISigAndType(Stmt stmt, SootMethod callerMethod) {
		String iccAPI;
		String iccAPIType;
		if (stmt.containsInvokeExpr()) {
			SootMethod calledMtd = stmt.getInvokeExpr().getMethod();
			iccAPI = calledMtd.getSignature();
			//iccAPIType = ICCAPITypeUtil.getAPIType(calledMtd).toString();
		} else {
			iccAPI = getOriginalCallbackName(callerMethod);
			//iccAPIType = "CALLBACK_ICC";
		}
		return new Pair<String, String>(iccAPI, "");
	}

	public static boolean isMapAPI(SootMethod invokedMethod) {
		return isAPIinSubClsOf(invokedMethod, "java.Util.Map") || isAPIinSubClsOf(invokedMethod, "java.util.HashMap");
	}

	public static boolean isActivityLifecycleCallbacks(SootMethod entry) {
		return isAPIinSubClsOf(entry, "android.app.Application.ActivityLifecycleCallbacks");
	}
}
