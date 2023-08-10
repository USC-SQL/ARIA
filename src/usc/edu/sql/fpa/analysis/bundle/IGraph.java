package usc.edu.sql.fpa.analysis.bundle;

import java.util.*;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import usc.edu.sql.fpa.analysis.IRAnalysis;
import usc.edu.sql.fpa.analysis.interpret.IntentInterpreter;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.Component;
import usc.edu.sql.fpa.model.IC3Intent;
import usc.edu.sql.fpa.model.ICCLink;
import usc.edu.sql.fpa.model.IntentFilter;
import usc.edu.sql.fpa.model.IntentIR;
import usc.edu.sql.fpa.output.GraphReporter;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.IntentUtils;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.sql.ir.ConstantString;
import usc.sql.ir.Variable;

public class IGraph {
	Logger logger = LoggerFactory.getLogger(GraphReporter.class);
	public Set<INode> sNodes = new HashSet<>();
	public Set<INode> rNodes = new HashSet<>();
	public Set<INode> oNodes = new HashSet<>();
	private Map<INode, Set<ICCLink>> ICCsOut = new HashMap<>();
	private Map<INode, Set<ICCLink>> ICCsIn1 = new HashMap<>();

	public IGraph() {

	}

	public void canICCHappen(INode sn, INode rn, IntentIR intentIR, Set<IC3Intent> intents) {

		if (ICCFinder.isReceivedAndSentComponentTypeMatched(sn, rn)) {
			if (ICCFinder.isCallerAndResponderTypeMatched(sn, rn)) {

				for (IC3Intent i3i : intents) {
					ICCLink link = new ICCLink(sn, rn, intentIR, i3i);
					if (ICCFinder.isLinkPossible(link))
						addLink(rn, sn, intentIR, i3i);

				}

			}
		}
	}

	public void addIntraComponentEdge(INode sn) {
		for (IntentIR ir : sn.getIrIntents()) {
			for (ATTRIBUTE attr : ir.dependsOnIncomingIntent()) {
				Set<INode> res = BundleAnalysis.getRelevantRetreivingPoints(sn, attr, ir);
				res.addAll(BundleAnalysis.getRelevantReceivingPoints(sn, attr, ir));
				for (INode node : res) {
					sn.addPred(node);
					node.addSucc(sn);
				}
			}
		}

	}

	public void addIntraRetreivingEdge(INode on) {
		oNodes.add(on);
		for (IntentIR ir : on.getIrIntents()) {
			for (ATTRIBUTE attr : ir.dependsOnIncomingIntent()) {
				Set<INode> res = BundleAnalysis.getRelevantReceivingPoints(on, attr, ir);
				for (INode node : res) {
					on.addPred(node);
					node.addSucc(on);
				}
			}
			on.addAllInterpreted(new IntentInterpreter().interpret(on, ir, Constants.MAX_LOOPS));
		}

	}

	public void addLink(INode rn, INode sn, IntentIR ir, IC3Intent intent) {
		ICCLink link = new ICCLink(sn, rn, ir, intent);
		sn.addInterpretedIntent(intent);
		sn.addSucc(rn);
		rn.addPred(sn);
		rn.addInterpretedIntent(intent.clone());
		updateIntentsAtReceivingPoints(rn, sn.getComponent(), true);

		// sending side
		if (!ICCsOut.containsKey(sn)) {
			ICCsOut.put(sn, new HashSet<ICCLink>());
		}
		ICCsOut.get(sn).add(link);

//		// receiving side
//		if (!ICCsIn.containsKey(rn)) {
//			ICCsIn.put(rn, new HashSet<ICCLink>());
//		}
//		ICCsIn.get(rn).add(link);
	}

//	public void removeLink(INode sn, INode rn, IntentIR ir) {
//		sn.removeSucc(rn);
//		rn.removePred(sn);
//		if (ICCsOut.containsKey(sn)) {
//			for (ICCLink link : new HashSet<>(ICCsOut.get(sn))) {
//				if (link.from.equals(sn) && link.to.equals(rn) && link.intentIR.equals(ir)) {
//					ICCsOut.get(sn).remove(link);
//					sn.removeInterpretedIntent(link.intentInterpreted);
//				}
//			}
//		}
//
//		if (ICCsIn.containsKey(rn)) {
//			for (ICCLink link : new HashSet<>(ICCsIn.get(rn))) {
//				if (link.from.equals(sn) && link.to.equals(rn) && link.intentIR.equals(ir)) {
//					ICCsIn.get(rn).remove(link);
//					rn.removeInterpretedIntent(link.intentInterpreted);
//				}
//			}
//		}
//	}

	public void updateGraphForOtherPoints(INode currNode) {
		for (IntentIR ir : currNode.getIrIntents()) {
			if (!ir.dependsOnIncomingIntent().isEmpty()) {
				currNode.clearInterpretedIntents();
				Set<IC3Intent> interepted = new IntentInterpreter().interpret(currNode, ir, Constants.MAX_LOOPS);
				currNode.addAllInterpreted(interepted);
			}
		}

	}

	public void updateGraphForReceivingPoints(INode currNode) {
//		currNode.clearInterpretedIntents();
//		for (IntentIR ir : currNode.getIrIntents()) {
//			Set<IC3Intent> interepted = new IntentInterpreter().interpret(currNode, ir, Constants.MAX_LOOPS);
//			currNode.addAllInterpreted(interepted);
//		}

		updateIntentsAtReceivingPoints(currNode, null, false);
		Set<IC3Intent> set = new HashSet<>();
		if (currNode.getComponent() != null && currNode.getComponent().isMainActivity()) {
			if (currNode.getInvokedExpr() != null && currNode.getInvokedExpr().contains("getIntent"))
				set.add(IntentUtils.generateLauncherNode(currNode));
		}
		currNode.addAllInterpreted(set);
	}

	private void updateIntentsAtReceivingPoints(INode currNode, Component component, boolean isSetRes) {
		Set<IC3Intent> set = new HashSet<>();
		for (IC3Intent i : new HashSet<>(currNode.getInterpretedIntents())) {
			if (i.getComponent() != null && i.getComponent().contains(".*")) {
				currNode.removeInterpretedIntent(i);
				i.setComponent(currNode.getComponent().getName());
				set.add(i);
			}
			if (currNode.getComponent() == null) {
				if (i.getAction() != null && i.getAction().contains(".*") && currNode.getComponent() != null) {
					for (IntentFilter filter : currNode.getComponent().getIntentFilters()) {
						currNode.removeInterpretedIntent(i);
						for (String action : filter.actions) {
							IC3Intent ii = i.clone();
							ii.setAction(action);
							set.add(ii);
						}
					}
				}
			}
			if (currNode.getUnitStr().contains("setResult(") && isSetRes) {
				if (component != null) {
					i.setRES(component.getName());
				} else {
					i.setRES(".*");
				}
			}

		}
		currNode.addAllInterpreted(set);
	}

	public void updateGraphForSendingPoint(INode currNode, Set<String> cannotHappenCache) {
		currNode.clearInterpretedIntents();

		for (IntentIR iir : currNode.getIrIntents()) {
			if (iir.dependsOnIncomingIntent().isEmpty())
				continue;
			Set<IC3Intent> interepted = new IntentInterpreter().interpret(currNode, iir, Constants.MAX_LOOPS);
			for (INode recevingNode : rNodes) {
				if (recevingNode.getComponent() != null
						&& cannotHappenCache.contains(recevingNode.getComponent().getName()))
					continue;
				logger.info("Updating iGraph for Sending Point: " + currNode + " " + recevingNode);
				updateGraph(currNode, recevingNode, iir, interepted, cannotHappenCache);
			}
		}
	}

	private void updateGraph(INode sNode, INode rNode, IntentIR iir, Set<IC3Intent> interepted,
			Set<String> cannotHappenCache) {
		logger.debug("check if updating graph possible: ");
		logger.debug("sending node = " + sNode.toString());
		logger.debug("receiving node = " + rNode.toString());

		if (ICCFinder.isReceivedAndSentComponentTypeMatched(sNode, rNode)) {
			if (ICCFinder.isCallerAndResponderTypeMatched(sNode, rNode)) {
				canICCHappen(sNode, rNode, iir, interepted);
				sNode.addAllMappings(interepted, iir);
			}
		}

	}

	public Set<ICCLink> getLinksFromSendingPoint(INode node) {
		return ICCsOut.get(node);
	}

//	public Set<INode> getInitNodes() {
//		Set<INode> set = new HashSet<INode>();
//		// set.addAll(sNodes);
//		set.addAll(rNodes);
//		return set;
//	}

	public INode getNode(CodePoint definitionPoint) {
		for (INode rNode : this.rNodes) {
			if (rNode.containsCodePoint(definitionPoint)) {
				return rNode;
			}
		}
		return null;
	}

//	public Set<IC3Intent> getReceivedIntentsAt(INode rNode) {
//		Set<IC3Intent> set = new HashSet<>();
//		if (rNode.getComponent() != null && rNode.getComponent().isMainActivity()) {
//			set.add(IntentUtils.generateLauncherNode(rNode));
//		}
//		if (!ICCsIn.containsKey(rNode)) {
//			return set;
//		}
//		for (ICCLink link : ICCsIn.get(rNode)) {
//			IC3Intent ii = link.intentInterpreted;
////			if (link.intentIR.iattrsMap.get(ATTRIBUTE.COMPONENT) != null) {
////				ii.setComponent(rNode.getComponent().getName());
////			}
//			if (ii.getComponent() != null && ii.getComponent().contains(".*"))
//				ii.setComponent(rNode.getComponent().getName());
//			set.add(ii);
//		}
//
//		return set;
//	}

	public Map<INode, Set<ICCLink>> getICCOuts() {
		return ICCsOut;
	}

}
