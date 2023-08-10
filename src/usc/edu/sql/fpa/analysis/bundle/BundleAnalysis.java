package usc.edu.sql.fpa.analysis.bundle;

import java.util.*;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dataflow.constantpropagation.Constant;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.NullConstant;
import soot.toolkits.scalar.Pair;
import usc.edu.sql.fpa.analysis.IRAnalysis;
import usc.edu.sql.fpa.analysis.interpret.IntentInterpreter;
import usc.edu.sql.fpa.analysis.intra.IType;
import usc.edu.sql.fpa.analysis.intra.IntraIntentAnalysis;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.Component;
import usc.edu.sql.fpa.model.IC3Intent;
import usc.edu.sql.fpa.model.ICCLink;
import usc.edu.sql.fpa.model.IntentIR;
import usc.edu.sql.fpa.output.GraphReporter;
import usc.edu.sql.fpa.utils.AnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.IntentUtils;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.sql.ir.ConstantString;
import usc.sql.ir.Expression;
import usc.sql.ir.InternalVar;
import usc.sql.ir.Variable;

public class BundleAnalysis {

	public static Map<String, Set<Component>> app2Components = new HashMap<>();
	public static Set<INode> senderPoints;

	public static Set<INode> ICDsenderPoints = new HashSet<>();
	public static Map<String, Set<INode>> receiverPoints;
	public static Map<String, Set<INode>> extractingPoints;
	
	public static Map<INode, Set<INode>> ret2rec = new HashMap<>();
	public static IGraph iGraph;
	Logger logger = LoggerFactory.getLogger(BundleAnalysis.class);

	public BundleAnalysis(Map<String, Set<INode>> snodes, Map<String, Set<INode>> rnodes,
			Map<String, Set<INode>> onodes) {
		iGraph = new IGraph();
		senderPoints = new HashSet<INode>();
		for (Set<INode> nodes : snodes.values()) {
			senderPoints.addAll(nodes);
			iGraph.sNodes.addAll(nodes);
		}

		receiverPoints = new HashMap<>();
		for (INode node : getAllNodes(rnodes)) {
			if (!receiverPoints.containsKey(node.getMethod()))
				receiverPoints.put(node.getMethod(), new HashSet<>());
			
			receiverPoints.get(node.getMethod()).add(node);
			iGraph.rNodes.add(node);
			iGraph.updateGraphForReceivingPoints(node);
		}

		extractingPoints = new HashMap<>();
		for (INode node : getAllNodes(onodes)) {
			if (!extractingPoints.containsKey(node.getMethod()))
				extractingPoints.put(node.getMethod(), new HashSet<>());
			extractingPoints.get(node.getMethod()).add(node);
			iGraph.oNodes.add(node);

		}
		setupApp2Components();
		System.out.println("Analyzing " + app2Components.size() + " apps");
		int count = 0;
		for (String str : app2Components.keySet()) {
			count += app2Components.get(str).size();
		}
		Constants.BUNDLE_EXTRA_INFO.put("APP_COUNT", app2Components.size() + "");
		Constants.BUNDLE_EXTRA_INFO.put("COMP_COUNT", count + "");
		System.out.println(count);

		initialize();

	}

	private void setupApp2Components() {
		for (INode node : getAllNodes(receiverPoints)) {
			String app = node.getApp();
			Component comp = node.getComponent();
			if (comp == null) {
				System.out.println("node does not have a reaching component");
				continue;
			}
			if (!app2Components.containsKey(app)) {
				app2Components.put(app, new HashSet<Component>());
			}
			if (!app2Components.get(app).contains(comp))
				app2Components.get(app).add(comp);

		}

	}

	private void initialize() {

		logger.info("iGraph initialization started");
		long start = System.currentTimeMillis();
		
//		for(INode rnode : getAllNodes(receiverPoints)) {
//			if (rnode.getComponent() != null && rnode.getComponent().isMainActivity()) {
//				rnode.addInterpretedIntent(IntentUtils.generateLauncherNode(rnode));
//			}
//		}

		for (INode snode : senderPoints) {
			iGraph.addIntraComponentEdge(snode);
		}
		
		for (INode snode : senderPoints) {
			for (IntentIR intentIR : snode.getIrIntents()) {
				Set<ATTRIBUTE> set = intentIR.dependsOnIncomingIntent();
				if (!set.isEmpty()) {
					ICDsenderPoints.add(snode);
					continue;
				}
				Set<IC3Intent> intents = new IntentInterpreter().interpret(snode, intentIR, Constants.MAX_LOOPS);
				snode.addAllInterpreted(intents);
				snode.addAllMappings(intents, intentIR);
				for (INode rnode : getAllNodes(receiverPoints))
					iGraph.canICCHappen(snode, rnode, intentIR, intents);
			}
		}
		
		for (INode onode : getAllNodes(extractingPoints)) {
			iGraph.addIntraRetreivingEdge(onode);
		}

		long runtime = System.currentTimeMillis() - start;
		Constants.BUNDLE_EXTRA_INFO.put("INIT_TIME", runtime + "");
		logger.info("iGraph initialization finished");

	}

	public void run() {
		logger.info("iGraph analysis started");
		long start = System.currentTimeMillis();
		int count = 0;
		List<INode> worklist = new LinkedList<>(ICDsenderPoints);
		while (!worklist.isEmpty()) {
			if(count == Constants.MAX_iter)
				break;
			count++;
			INode currNode = worklist.remove(0);
			Set<IC3Intent> old1 = new HashSet<>(currNode.getInterpretedIntents());
			
			if (ICDsenderPoints.contains(currNode)) {
				Set<String> cannotHappenCache = new HashSet<String>();
				logger.info("Updating iGraph for Sending Point: " + currNode);
				iGraph.updateGraphForSendingPoint(currNode, cannotHappenCache);
			}
			if (containsNode(receiverPoints, currNode)) {
				logger.info("Updating iGraph for receiving Point: " + currNode);
				iGraph.updateGraphForReceivingPoints(currNode);
				worklist.addAll(currNode.getSuccs());
			}
			if (containsNode(extractingPoints, currNode)) {
				logger.info("Updating iGraph for extracting Point: " + currNode);
				iGraph.updateGraphForOtherPoints(currNode);

			}
			if (!old1.equals(currNode.getInterpretedIntents())) {
				worklist.addAll(currNode.getSuccs());
			}

		}
		long runtime = System.currentTimeMillis() - start;
		Constants.BUNDLE_EXTRA_INFO.put("PROP_TIME", runtime + "");
		logger.info("iGraph reached a fixed point");
		logger.info("Generating output..");
		GraphReporter.reportICCOuts(iGraph.getICCOuts());
		GraphReporter.reportIntents(iGraph.sNodes, "all");
		
	}

	private boolean containsNode(Map<String, Set<INode>> map, INode currNode) {
		if (map.containsKey(currNode.getMethod()))
			return map.get(currNode.getMethod()).contains(currNode);
		return false;
	}

	public static Set<INode> getRelevantPoints(INode sn, ATTRIBUTE attr, IntentIR ir, Set<INode> nodes) {
		Set<INode> res = new HashSet<>();

		if (attr.equals(ATTRIBUTE.EXTRA)) {
			for (Variable vk : ir.extraMap.keySet()) {
				res.addAll(getReachingNodes(vk, nodes));
				for (Variable vv : ir.extraMap.get(vk)) {
					res.addAll(getReachingNodes(vv, nodes));
				}
			}

		} else {
			for (Variable v : ir.iattrsMap.get(attr)) {
				res.addAll(getReachingNodes(v, nodes));
			}

		}
		return res;
	}

	private static Set<INode> getReachingNodes(Variable v, Set<INode> nodes) {
		Set<INode> res = new HashSet<>();
		if (IRAnalysis.isIntentRelatedOp(v) != null) {
			Expression expr = (Expression) v;
			for (INode n : nodes) {
				if (n.getMethod().equals(expr.getMethod()) && n.getOffset() == expr.getUnitOffset()) {
					res.add(n);
				}
			}
		} else if (v instanceof Expression) {
			for (List<Variable> vv : ((Expression) v).getOperands()) {
				for (Variable vvv : vv) {
					res.addAll(getReachingNodes(vvv, nodes));
				}
			}
		}
		return res;
	}

	public static Set<INode> getRelevantReceivingPoints(INode sn, ATTRIBUTE attr, IntentIR ir) {
		return getRelevantPoints(sn, attr, ir, getAllNodes(receiverPoints));
	}

	public static Set<INode> getRelevantRetreivingPoints(INode sn, ATTRIBUTE attr, IntentIR ir) {
		return getRelevantPoints(sn, attr, ir, getAllNodes(extractingPoints));
	}

	public static Set<Set<Variable>> findValuesForAttrFromReceivedIntents(ATTRIBUTE attr, CodePoint definitionPoint) {
		Set<Set<Variable>> set = new HashSet<>();
		for (INode snode : iGraph.getNode(definitionPoint).getPreds()) {
			for (ICCLink link : iGraph.getLinksFromSendingPoint(snode)) {
				set.add(link.intentIR.iattrsMap.get(attr));
			}
		}
		return set;
	}

//	private static void checkReceivingPoints(ATTRIBUTE attr, String nodeMethodSig, int nodeUnitOffset, Set<String> res,
//			String keyvar) {
//		for (INode n : receiverPoints.get(nodeMethodSig)) {
//			if (n.getMethod().equals(nodeMethodSig) && n.getOffset() == nodeUnitOffset) {
//				res.addAll(findAttrFromReceivedIntents(attr, n, keyvar));
//				break;
//			}
//		}
//	}
//
//	private static Set<String> findAttrFromReceivedIntents(ATTRIBUTE attr, INode n, String key) {
//		Set<String> res = new HashSet<>();
//		res.addAll(getRevelantAttrValFromIC3Intents(iGraph.getReceivedIntentsAt(n), attr, key));
//		return res;
//
//	}

//	private static void checkRetreivingPoints(ATTRIBUTE attr, String nodeMethodSig, int nodeUnitOffset, Set<String> res,
//			String keyvar) {
//		for (INode n : retreivingPoints.get(nodeMethodSig)) {
//			if (n.getOffset() == nodeUnitOffset) {
//				Set<String> vars = getValsOfAttrOfReachedIntents(attr, n, keyvar, null);
//				for (String var : vars) {
//					res.add(var);
//
//				}
//			}
//		}
//
//	}

//	private static Set<String> findAttrFromReachedIntents(ATTRIBUTE attr, String nodeMethodSig, int nodeUnitOffset,
//			INode retreivedNode, String key) {
//		Set<String> res = new HashSet<>();
//		for (INode rNode : findReceivedPointLeadingToRetreivePoint(nodeMethodSig, nodeUnitOffset, retreivedNode)) {
//			res.addAll(findAttrFromReceivedIntents(attr, rNode, key));
//		}
////		if (res.isEmpty()) {
////			res.add(new ConstantString(Constants.NULL));
////		}
//		return res;
//	}

	private static Set<INode> findReceivedPointLeadingToRetreivePoint(String nodeMethodSig, int nodeUnitOffset,
			INode retreivedNode) {
		if (!ret2rec.containsKey(retreivedNode)) {
			ret2rec.put(retreivedNode, new HashSet<INode>());
			for (INode rev : receiverPoints.get(nodeMethodSig)) {
				if (rev.getMethod().equals(nodeMethodSig) && rev.getOffset() == nodeUnitOffset) {
					ret2rec.get(retreivedNode).add(rev);
				}
			}
		}
		return ret2rec.get(retreivedNode);
	}

//	private static Set<String> getValsOfAttrOfReachedIntents(ATTRIBUTE attr, INode n, String keyvar,
//			Set<Map<String, String>> extras) {
//		Set<String> set = new HashSet<>();
//		IntentInterpreter iin = new IntentInterpreter();
//		for (IntentIR iir : n.getIrIntents()) {
//			Set<IC3Intent> types = iin.interpret(n, iir, n.getApp(), Constants.MAX_LOOPS);
//			if (iir.iattrsMap.containsKey(attr)) {
//				for (Variable v : iir.iattrsMap.get(attr)) {
//					if (IRAnalysis.isIntentRelatedOp(v) != null) {
//						set.addAll(getAttrValsFromReceivedNodes((Expression) v, keyvar, attr, n));
//					} else {
//						set.addAll(getRevelantAttrValFromIC3Intents(types, attr, keyvar));
//					}
//				}
//			}
//			if (ATTRIBUTE.EXTRA.equals(attr)) {
//				Variable val = IRAnalysis.getIntentRelatedExtra(iir.extraMap);
//				if (val != null) {
//					set.addAll(getAttrValsFromReceivedNodes((Expression) val, keyvar, attr, n));
//				}
//				set.addAll(getRevelantAttrValFromIC3Intents(types, attr, keyvar));
//			}
//		}
//		return set;
//	}

//	private static Set<String> getAttrValsFromReceivedNodes(Expression exp, String keyvar, ATTRIBUTE attr, INode n) {
//		Set<String> set = new HashSet<>();
//		Set<INode> receivedNodes = findReceivedPointLeadingToRetreivePoint(exp.getMethod(), exp.getUnitOffset(), n);
//		for (INode rnode : receivedNodes) {
//			set.addAll(findAttrFromReceivedIntents(attr, rnode, keyvar));
//		}
//
//		return set;
//	}

	private static Set<INode> getAllNodes(Map<String, Set<INode>> map) {
		Set<INode> res = new HashSet<>();
		for (Set<INode> nodes : map.values()) {

			res.addAll(nodes);

		}
		return res;
	}

}
