package usc.edu.sql.fpa.utils;

import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.parsers.AXML20Parser;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import usc.edu.sql.fpa.model.Component;
import usc.edu.sql.fpa.model.IntentFilter;
import usc.edu.sql.fpa.output.GraphReporter;
import usc.edu.sql.fpa.utils.Constants.COMPONENT;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pxb.android.axml.AxmlVisitor;

public class Parser {
	private static Parser parser;
	private static String appName;
	private ARSCFileParser ArscFileParser;
	private final Set<String> pkgs = new HashSet<>();
	private final Map<Integer, String> Id2Name = new HashMap<>();
	private final Map<String, Integer> Name2id = new HashMap<>();
	private Map<String, Set<String>> Layout2CustomViews = new HashMap<>();
	private Set<String> CustomViews = new HashSet<>();
	private Set<String> clickMethods = new HashSet<>();
	Logger logger = LoggerFactory.getLogger(Parser.class);

	private final Set<String> activities = new HashSet<>();
	private Set<String> services = new HashSet<>();
	private Set<String> providers = new HashSet<>();
	private Set<String> receivers = new HashSet<>();
	public Map<String, Component> components = new HashMap<>();
	

	public Parser(String appName) {
		Parser.appName = appName;

	}

	public void parseARSC(InputStream input) throws IOException {
		ArscFileParser = new ARSCFileParser();
		ArscFileParser.parse(input);
		List<ARSCFileParser.ResPackage> pkgs = ArscFileParser.getPackages();
		if (pkgs != null) {
			for (ARSCFileParser.ResPackage pkg : pkgs) {
				// System.out.println("Package: " + pkg.getPackageName());
				for (ARSCFileParser.ResType type : pkg.getDeclaredTypes()) {
					// System.out.println(" Type : " + type.getTypeName());
					for (ARSCFileParser.ResConfig config : type.getConfigurations()) {
						// System.out.println(" Config: " + config.toString());
						for (ARSCFileParser.AbstractResource res : config.getResources()) {
							// System.out.println(" - Res : " + res.getResourceName() + " 0x" +
							// Integer.toHexString(res.getResourceID()));

							int id = res.getResourceID();
							if (!Id2Name.containsKey(id)) {
								String name = String.format("res/%s/%s", type.getTypeName(), res.getResourceName());
								Id2Name.put(id, name);
								Name2id.put(name, id);
							}
						}
					}
				}
			}
		}
	}

	public void parseLayout(String layoutFile, InputStream input) throws IOException {
		AXmlHandler handler = new AXmlHandler(input, new AXML20Parser());
		parseLayout(layoutFile, handler);
	}

	private void parseLayout(String layoutFile, AXmlHandler handler) {
		// remove ".xml"
		int len = layoutFile.length();
		String layoutRes = layoutFile.substring(0, len - 4);
		System.out.println("Parse Layout: " + layoutRes);
		AXmlNode node = handler.getDocument().getRootNode();
		List<AXmlNode> worklist = new LinkedList<>();
		worklist.add(node);
		while (!worklist.isEmpty()) {
			node = worklist.remove(0);
			if (node.getTag() == null || node.getTag().isEmpty()) {
				continue;
			}
			String tname = node.getTag().trim();
			if (tname.equals("include")) {
				logger.info("include layout is not handled");
				//parseIncludeAttributes(layoutRes, node);
			} else if (tname.contains(".")) {
				if (tname.startsWith("android")) {
					continue;
				}
				System.out.println("   [Custom] " + tname);
				Set<String> customViews = Layout2CustomViews.get(layoutRes);
				if (customViews == null) {
					customViews = new HashSet<>();
					Layout2CustomViews.put(layoutRes, customViews);
				}
				customViews.add(tname);
				CustomViews.add(tname);
			}
			if(node.hasAttribute("onClick")) {
				AXmlAttribute<?> attr = (AXmlAttribute)node.getAttribute("onClick");
				clickMethods.add(attr.getValue().toString());
				
			}
			for (AXmlNode childNode : node.getChildren()) {
				worklist.add(childNode);
			}
		}
	}

//	private void parseIncludeAttributes(String layoutRes, AXmlNode rootNode) {
//		for (Map.Entry<String, AXmlAttribute<?>> entry : rootNode.getAttributes().entrySet()) {
//			String attrName = entry.getKey().trim();
//			
//
//			if (attrName.equals("layout")) {
//				if ((attr.getType() == AxmlVisitor.TYPE_REFERENCE || attr.getType() == AxmlVisitor.TYPE_INT_HEX)
//						&& attr.getValue() instanceof Integer) {
//					// We need to get the target XML file from the binary manifest
//					ARSCFileParser.AbstractResource targetRes = new ARSCFileParser()
//							.findResource((Integer) attr.getValue());
//					if (targetRes == null) {
//						// System.err.println("Target resource " + attr.getValue() + " for layout
//						// include not found");
//						return;
//					}
//					if (!(targetRes instanceof ARSCFileParser.StringResource)) {
//						// System.err.println("Invalid target node for include tag in layout XML, was "
//						// +
//						// targetRes.getClass().getName());
//						return;
//					}
//					String targetFile = ((ARSCFileParser.StringResource) targetRes).getValue();
//					System.out.println("   <include> " + targetFile + "   0x"
//							+ Integer.toHexString(((Integer) attr.getValue()).intValue()));
//					Set<String> includes = LayoutIncludes.get(layoutRes);
//					if (includes == null) {
//						includes = new HashSet<>();
//						LayoutIncludes.put(layoutRes, includes);
//					}
//					includes.add(targetFile);
//				}
//			}
//		}
//	}

	public void parseManifest(InputStream input) throws IOException {
		AXmlHandler handler = new AXmlHandler(input, new AXML20Parser());
		parseManifest(handler);
	}

	public void parseManifestNoads(InputStream input) throws IOException {
		AXmlHandler handler = new AXmlHandler(input, new AXML20Parser());
		parseManifestNoads(handler);
	}

	private void parseManifestNoads(AXmlHandler handler) {

		AXmlNode node = handler.getDocument().getRootNode();

		String pkg = (String) node.getAttribute("package").getValue();
		pkgs.add(pkg);
		System.out.println("package name is: " + pkg);
		Constants.APP_PACKAGE = pkg;

		List<AXmlNode> worklist = new LinkedList<>();
		worklist.add(node);
		while (!worklist.isEmpty()) {
			node = worklist.remove(0);
			if (node.getTag() == null || node.getTag().isEmpty()) {
				continue;
			}
			String tname = node.getTag().trim();
			if (tname.equals("activity")) {

				addComponent(node, pkg, activities, COMPONENT.ACTIVITY);

			} else if (tname.equals("service")) {

				addComponent(node, pkg, services, COMPONENT.SERVICE);

			} else if (tname.equals("receiver")) {

				addComponent(node, pkg, receivers, COMPONENT.BROADCAST_RECEIVER);

			}

			for (AXmlNode childNode : node.getChildren()) {
				worklist.add(childNode);
			}
		}

	}

	private void addComponent(AXmlNode node, String pkg, Set<String> set, COMPONENT type) {
		AXmlAttribute<?> attr = node.getAttribute("name");
		if (attr == null) {
			return;
		}
		String comp = (String) node.getAttribute("name").getValue();
		if (comp.startsWith(".")) {
			comp = pkg + comp;
		}

		set.add(comp);

		Component cmp = new Component(comp, Constants.APK_PATH, type);
		AXmlAttribute<?> exported = node.getAttribute("exported");
		if (exported != null && (Boolean) exported.getValue()) {
			cmp.setExported(true);
		}
		for (AXmlNode childNode : node.getChildren()) {
			String tname = childNode.getTag().trim();
			if (tname.equals("intent-filter")) {
				extractIntentElementsFromManifest(childNode, cmp);
			}
		}
		if (isMainComp(cmp)) {
			cmp.setIsMain(true);
		}
		components.put(cmp.getName(), cmp);

	}

	private boolean isMainComp(Component cmp) {
		for (IntentFilter inf : cmp.getIntentFilters()) {
			if (inf.actions.contains(Constants.MAIN_ACTION)) {
				if (inf.categories.contains(Constants.MAIN_CATEGORY)) {
					return true;
				}
			}
		}
		return false;
	}

	private void extractIntentElementsFromManifest(AXmlNode node, Component comp) {
		IntentFilter intentFilter = new IntentFilter();
		for (AXmlNode childNode : node.getChildren()) {
			String tname = childNode.getTag().trim();

			if (tname.equals("action")) {
				AXmlAttribute<?> attr = childNode.getAttribute("name");
				intentFilter.actions.add((String) attr.getValue());
			}
			if (tname.equals("category")) {
				AXmlAttribute<?> attr = childNode.getAttribute("name");
				intentFilter.categories.add((String) attr.getValue());
			}
			if (tname.equals("data")) {
				for (Entry<String, AXmlAttribute<?>> en : childNode.getAttributes().entrySet())
					intentFilter.data.put(en.getKey(), (String) en.getValue().getValue());
			}
		}
		comp.addIntentFilter(intentFilter);
	}

	public Set<String> getServices() {
		return services;
	}

	public void setServices(Set<String> services) {
		this.services = services;
	}

	public Set<String> getProviders() {
		return providers;
	}

	public void setProviders(Set<String> providers) {
		this.providers = providers;
	}

	public Set<String> getReceivers() {
		return receivers;
	}

	public void setReceivers(Set<String> receivers) {
		this.receivers = receivers;
	}

	private void parseManifest(AXmlHandler handler) {

		AXmlNode node = handler.getDocument().getRootNode();

		String pkg = (String) node.getAttribute("package").getValue();
		pkgs.add(pkg);
		System.out.println("package name is: " + pkg);

		List<AXmlNode> worklist = new LinkedList<>();
		worklist.add(node);
		while (!worklist.isEmpty()) {
			node = worklist.remove(0);
			if (node.getTag() == null || node.getTag().isEmpty()) {
				continue;
			}
			String tname = node.getTag().trim();
			if (tname.equals("activity") || tname.equals("activity-alias")) {

				String act = (String) node.getAttribute("name").getValue();
				System.out.println("activity " + act);
				if (act.startsWith(".")) {
					act = pkg + act;
				}
				activities.add(act);
				if (isMainActivity(node)) {
					// if (!act.contains(pkgs.iterator().next())) {
					String[] temp = act.split("\\.");
					String pkg2 = temp[0];
					if (temp.length > 1)
						pkg2 = temp[0] + "." + temp[1];
					pkgs.add(pkg2);
					// }
				}
			}

			for (AXmlNode childNode : node.getChildren()) {
				worklist.add(childNode);
			}
		}
		Iterator<String> it = pkgs.iterator();
		while (it.hasNext()) {
			System.out.println("package is " + it.next());
		}
	}

	public boolean isMainActivity(AXmlNode node) {
		List<AXmlNode> list = node.getChildren();
		for (int i = 0; i < list.size(); i++) {
			AXmlNode n = list.get(i);
			if (!n.getTag().trim().equals("intent-filter")) {
				continue;
			}
			if (isMainIntent(n)) {
				return true;
			}
		}
		return false;
	}

	public boolean isMainIntent(AXmlNode node) {
		boolean isMain = false;
		boolean isLauncher = false;

		List<AXmlNode> list = node.getChildren();
		for (int i = 0; i < list.size(); i++) {
			AXmlNode currentNode = list.get(i);
			if (currentNode.getTag().trim().equals("action")) {

				String action = (String) currentNode.getAttribute("name").getValue();

				if ("android.intent.action.MAIN".equals(action)) {
					isMain = true;
				}
			} else if (currentNode.getTag().trim().equals("category")) {
				String category = (String) currentNode.getAttribute("name").getValue();
				if ("android.intent.category.LAUNCHER".equals(category)) {
					isLauncher = true;
				}
			}
		}
		return isLauncher && isMain;
	}

	public String id2Name(int id) {
		return Id2Name.get(id);
	}

	public Integer name2Id(String name) {
		return Name2id.get(name);
	}

	public Set<String> getActivities() {
		return activities;
	}

	public Set<String> getPkg() {
		return pkgs;
	}
	
	public Set<String> getOnClicks() {
		return clickMethods;
	}

	public static Parser getInstance(String appName) {
		if (parser == null || !Parser.appName.equals(appName)) {
			parser = new Parser(appName);
		}
		return parser;
	}

	public Set<Component> getComponents() {
		// TODO Auto-generated method stub
		return new HashSet<>(components.values());
	}

}
