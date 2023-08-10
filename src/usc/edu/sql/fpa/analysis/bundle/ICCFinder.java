package usc.edu.sql.fpa.analysis.bundle;

import java.util.*;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;
import usc.edu.sql.fpa.analysis.interpret.IntentInterpreter;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.model.Component;
import usc.edu.sql.fpa.model.IC3Intent;
import usc.edu.sql.fpa.model.ICCLink;
import usc.edu.sql.fpa.model.IntentIR;
import usc.edu.sql.fpa.utils.APIUtils;
import usc.edu.sql.fpa.utils.AnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;
import usc.edu.sql.fpa.utils.Constants.COMPONENT;

public class ICCFinder {

	public static boolean isLinkPossible(ICCLink link) {

		if (link.from.getUnitStr().contains("setResult")) {
			if (link.intentInterpreted.getRes() != null) {
				if (link.to.getComponent() != null)
					if (link.intentInterpreted.getRes().equals(link.to.getComponent().getName()))
						return true;
			}
		}
		Component componentDestination = link.to.getComponent();
		String app = link.to.getApp();
		IC3Intent intent = link.intentInterpreted;

		if (codePointConnectionPossible(link.from, link.to)) {
			return intent.resolve(componentDestination, app, link.from);

		}
		return false;
	}

	private static boolean codePointConnectionPossible(INode from, INode to) {
		return codePointConnectionPossible(from.getInvokedExpr(), to.getInvokedExpr(), to.getMethod());
	}

	public static boolean codePointConnectionPossible(String fromInvokedExpr, String toInvokedMethod, String toMethod) {
		COMPONENT componentOfInterest = APIUtils.getAPIType(fromInvokedExpr);

		if (toInvokedMethod == null) {// received point is callback entrypoint
			if (APIUtils.getAPIType(toMethod).toString().startsWith(componentOfInterest.toString()))
				return true;

		} else { // received point is method invocation
			if (APIUtils.getAPIType(toInvokedMethod).toString().startsWith(componentOfInterest.toString()))
				return true;
		}

		return false;
	}

	private static boolean hasComponent(List<Component> list, Component componentDestination) {
		if (componentDestination == null)
			return false;
		for (Component comp : list) {
			if (comp.getName().equals(componentDestination.getName())) {
				return true;
			}
		}
		return false;
	}

	public static boolean isReceivedAndSentComponentTypeMatched(INode sNode, INode rNode) {
		return isReceivedAndSentComponentTypeMatched(sNode.getInvokedExpr(), rNode.getComponentType());
	}

	public static boolean isReceivedAndSentComponentTypeMatched(String sNodeInvokedExpr, COMPONENT rNodeComponentType) {
		COMPONENT interestedComponent = APIUtils.getAPIType(sNodeInvokedExpr);
		if (interestedComponent.equals(rNodeComponentType))
			return true;
		return false;
	}

	public static boolean isCallerAndResponderTypeMatched(INode sNode, INode rNode) {
		if (rNode.getInvokedExpr() == null)
			return isCallerAndResponderTypeMatched(sNode.getInvokedExpr(), rNode.getMethod());
		return isCallerAndResponderTypeMatched(sNode.getInvokedExpr(), rNode.getInvokedExpr());
	}

	public static boolean isCallerAndResponderTypeMatched(String sNodeInvokedExpr, String rNodeMethod) {
		// TODO Auto-generated method stub

		if (sNodeInvokedExpr.contains("bind") && rNodeMethod.contains("onBind")) {
			return true;
		}
		if (sNodeInvokedExpr.contains("bind") || rNodeMethod.contains("onBind")) {
			return false;
		}
		if (sNodeInvokedExpr.contains("setResult") && rNodeMethod.contains("onActivityResult")) {
			return true;
		}
		if (sNodeInvokedExpr.contains("setResult") || rNodeMethod.contains("onActivityResult")) {
			return false;
		}
		if (rNodeMethod.contains("setResult")) {
			if (sNodeInvokedExpr.contains("startActivityForResult"))
				return true;
			return false;
		}

		return true;
	}

}