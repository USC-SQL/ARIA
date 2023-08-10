package usc.edu.sql.fpa.utils;

import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.BriefUnitGraph;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

public class Hierarchy {

	private final SootClass activityClass;
	private final SootClass serviceClass;
	private final SootClass contentProviderClass;
	private final SootClass broadCastReceiverClass;
	private final SootClass contextClass;
	public final SootClass intentClass;
	private final SootClass collectionClass;
	private final SootClass mapClass;
	private final SootClass fragmentClass;

	public Scene scene;
	public Set<SootClass> appClasses = new HashSet<SootClass>();
	public Set<SootMethod> appMethods = new HashSet<>();
	private final Map<SootClass, Set<SootClass>> classAndItsSuperTypes = new HashMap<SootClass, Set<SootClass>>();
	private final Map<SootClass, Set<SootClass>> classAndItsSubTypes = new HashMap<SootClass, Set<SootClass>>();
	private final Map<SootClass, Set<SootClass>> classAndItsConcreteSubTypes = new HashMap<SootClass, Set<SootClass>>();

	private static Hierarchy hierarchy;
	private static String appName;

	Hierarchy(String name) {
		// Utils.setUpLogger(Constants.APP_NAME);
		Hierarchy.appName = name;
		scene = Scene.v();
		intentClass = scene.getSootClass("android.content.Intent");
		activityClass = scene.getSootClass("android.app.Activity");
		contentProviderClass = scene.getSootClass("android.content.ContentProvider");
		serviceClass = scene.getSootClass("android.app.Service");
		broadCastReceiverClass = scene.getSootClass("android.content.BroadcastReceiver");
		contextClass = scene.getSootClass("android.content.Context");
		fragmentClass = scene.getSootClass("android.app.Fragment");
		collectionClass = scene.getSootClass("java.util.Collection");
		mapClass = scene.getSootClass("java.util.map");
		simpleClassStatistics();
		for (SootClass c : scene.getClasses()) {
			traverse(c, c);
		}

	}

	void simpleClassStatistics() {
		Scene scene = Scene.v();
		int numClasses = 0;
		// first, create an empty set for each class/interface
		for (SootClass c : scene.getClasses()) {
			numClasses++;
			if (c.isApplicationClass()) {
				appClasses.add(c);
				appMethods.addAll(c.getMethods());
			}
			classAndItsSuperTypes.put(c, new LinkedHashSet<SootClass>());
			classAndItsSubTypes.put(c, new LinkedHashSet<SootClass>());
			classAndItsConcreteSubTypes.put(c, new LinkedHashSet<SootClass>());
		}
		System.out.print("[HIER] All classes: " + numClasses);
		System.out.print(" [App: " + appClasses.size());
		System.out.print(", Lib : " + scene.getLibraryClasses().size());
		System.out.println(", Phantom: " + scene.getPhantomClasses().size() + "]");

		if (numClasses != appClasses.size() + scene.getLibraryClasses().size() + scene.getPhantomClasses().size()) {
			throw new Error("[HIER] Numbers do not add up");
		}
	}

	private void traverse(SootClass sub, SootClass supr) {

		// sub is a subtype of supr (or possibly supr == sub)

		// first, add sub to the all_tbl set for supr
		classAndItsSubTypes.get(supr).add(sub);

		// also, add supr to the all_super_tbl set for sub
		classAndItsSuperTypes.get(sub).add(supr);

		// next, if sub is a non-interface non-abstract class, add it
		// to the tbl set for supr
		if (sub.isConcrete()) {
			classAndItsConcreteSubTypes.get(supr).add(sub);
		}

		// traverse parent classes/interfaces of supr

		if (supr.hasSuperclass()) {
			traverse(sub, supr.getSuperclass());
		}

		for (Iterator<SootClass> it = supr.getInterfaces().iterator(); it.hasNext();) {
			traverse(sub, it.next());
		}
	}

	public boolean isCollectionClass(SootClass c) {

		return isSubclassOf(c, collectionClass);
	}

	public boolean isMapClass(SootClass c) {

		return isSubclassOf(c, mapClass);
	}

	public boolean isIntentClass(SootClass c) {
		return isSubclassOf(c, intentClass);
	}

	public boolean isServiceClass(final SootClass c) {
		return isSubclassOf(c, serviceClass);
	}

	public boolean isBroadcastReceiverClass(final SootClass c) {
		return isSubclassOf(c, broadCastReceiverClass);
	}

	public boolean isContentProviderClass(SootClass c) {
		return isSubclassOf(c, contentProviderClass);
	}

	public boolean isActivityClass(final SootClass c) {
		return isSubclassOf(c, activityClass);
	}

	public boolean isContextClass(SootClass c) {
		return isSubclassOf(c, contextClass);
	}

	public boolean isComponent(SootClass c) {

		return isActivityClass(c) || isServiceClass(c) || isBroadcastReceiverClass(c);
	}

	public boolean isFragmentClass(SootClass c) {
		return isSubclassOf(c, fragmentClass);
	}

	public boolean isSubclassOf(final SootClass child, final SootClass parent) {
		if (child == null)
			return false;
		Set<SootClass> superTypes = getSupertypes(child);

		if (superTypes != null) {
			return superTypes.contains(parent);
		}
		return isSubclassOfOnDemand(child, parent);
	}

	public Set<SootClass> getSupertypes(SootClass c) {
		Set<SootClass> set = classAndItsSuperTypes.get(c);
		if (set == null)
			return new HashSet<SootClass>();
		return set;
	}

	public Map<String, SootMethod> getAppMethodMap() {
		Map<String, SootMethod> map = new HashMap<String, SootMethod>();
		for (SootMethod sm : appMethods) {
			map.put(sm.getSignature(), sm);
		}
		return map;
	}

	public boolean isSubclassOfOnDemand(final SootClass child, final SootClass parent) {
		if (parent.getName().equals("java.lang.Object")) {
			return true;
		}
		if (child.equals(parent)) {
			return true;
		}
		if (child.hasSuperclass()) {
			return isSubclassOfOnDemand(child.getSuperclass(), parent);
		}
		return false;
	}

	public static Hierarchy getInstance(String name) {
		if (hierarchy == null || !name.equals(Hierarchy.appName)) {
			hierarchy = new Hierarchy(name);
		}
		return hierarchy;
	}

	public SootMethod getMethod(String method) {
		for (SootMethod sm : appMethods) {
			if (sm.getSignature().equals(method))
				return sm;
		}
		return null;
	}

}