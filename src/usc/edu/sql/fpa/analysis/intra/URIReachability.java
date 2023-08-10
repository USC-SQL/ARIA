package usc.edu.sql.fpa.analysis.intra;

import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import usc.edu.sql.fpa.model.CodePoint;
import usc.edu.sql.fpa.utils.AnalysisUtils;
import usc.edu.sql.fpa.utils.Constants;
import usc.edu.sql.fpa.utils.Hierarchy;
import usc.edu.sql.fpa.utils.IntentUtils;
import usc.edu.sql.fpa.utils.URIUtils;
import usc.edu.sql.fpa.utils.Constants.ATTRIBUTE;

import java.util.*;

public class URIReachability {

	private SootMethod currentMethod;
	private final List<URINode> allURINode = new ArrayList<>();
	// a node mapping to the nodes it can reach
	private final Map<CodePoint, Set<CodePoint>> rTable = new HashMap<>();

	public URIReachability(UnitGraph cfg) {
		currentMethod = cfg.getBody().getMethod();
		for (Unit n : cfg.getBody().getUnits()) {
			allURINode.add(new URINode(new CodePoint(n, currentMethod, Constants.APK_PATH)));
			rTable.put(new CodePoint(n, currentMethod, Constants.APK_PATH), new HashSet<>());
		}
		try {
			computeReachability(cfg);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	private void initialize() {
		for (URINode rn : allURINode) {
			if (URIUtils.getURIDefinition(rn.getNode().getUnit()) != null) {
				rn.getGenSet().add(rn.getNode());
			}
		}
	}

	private boolean compareTwoSet(Set<CodePoint> oldset, Set<CodePoint> newset) {
		return oldset.containsAll(newset) && newset.containsAll(oldset);
	}

	private void computeReachability(UnitGraph cfg) throws CloneNotSupportedException {
		initialize();
		Map<URINode, List<URINode>> preList = new HashMap<>();
		// Create predecessor list for each node
		for (URINode node : allURINode) {
			List<URINode> nodePre = new ArrayList<>();
			for (Unit pre : cfg.getPredsOf(node.getNode().getUnit())) {
				for (URINode n : allURINode)
					if (n.getNode().getUnit().equals(pre))
						nodePre.add(n);

			}
			preList.put(node, nodePre);

		}
		Map<URINode, Set<CodePoint>> old = new HashMap<>();
		for (URINode rn : allURINode) {
			Set<CodePoint> s = new HashSet<>();
			old.put(rn, s);
		}
		boolean needtoloop = true;

		while (needtoloop) {
			needtoloop = false;
			for (URINode rn : allURINode) {
				Value currRef = URIUtils.getURIDefinition(rn.getNode().getUnit());

				// Union the out set of all the nodes in the predecessor
				// list，add to the in set of node rn
				for (URINode node : preList.get(rn)) {
					for (CodePoint outnode : node.getOutSet())
						rn.getInSet().add(outnode);
				}
				// Union the gen set and in set to the out set of node rn
				for (CodePoint gennode : rn.getGenSet()) {
					if (AnalysisUtils.isApplicationMethodInvoked(gennode.getUnit())
							&& AnalysisUtils.getInvokedMethod(gennode.getUnit()).isConcrete()) {
						SootMethod invoked = ((Stmt) gennode.getUnit()).getInvokeExpr().getMethod();
						BriefUnitGraph ug = new BriefUnitGraph(invoked.retrieveActiveBody());
						for (Unit exit : ug.getTails()) {
							Set<CodePoint> res = AppInferrer.getInstance(Constants.APK_PATH).getURIReachability(ug,
									exit);
							rn.getOutSet().addAll(res);
						}
					} else {
						rn.getOutSet().add(gennode);
					}
				}

				for (CodePoint in : rn.getInSet()) {

					boolean isKilled = false;
					Value inRef = URIUtils.getURIDefinition(in.getUnit());
					if (currRef != null && currRef.equals(inRef)) {
						isKilled = true;
					}

					if (!isKilled) {
						rn.getOutSet().add(in);
					}

				}
				if (!compareTwoSet(old.get(rn), rn.getOutSet())) {
					needtoloop = true;
					Set<CodePoint> s = new HashSet<>(rn.getOutSet());
					old.put(rn, s);

				}
			}
		}

		for (URINode rn : allURINode) {
			for (CodePoint n : rn.getInSet()) {
				rTable.get(rn.getNode()).add(n);
			}
		}
	}

	/**
	 * Return a map mapping a node to the nodes it can reach.
	 */
	public Map<CodePoint, Set<CodePoint>> getReachableTable() {
		return rTable;
	}

}

class URINode {
	private CodePoint node;
	private Set<CodePoint> inSet = new HashSet<>();
	private Set<CodePoint> outSet = new HashSet<>();
	private Set<CodePoint> genSet = new HashSet<>();

	public URINode(CodePoint node) {
		this.node = node;
	}

	public CodePoint getNode() {
		return node;
	}

	public void setNode(CodePoint node) {
		this.node = node;
	}

	public Set<CodePoint> getInSet() {
		return inSet;
	}

	public void setInSet(Set<CodePoint> inSet) {
		this.inSet = inSet;
	}

	public Set<CodePoint> getOutSet() {
		return outSet;
	}

	public void setOutSet(Set<CodePoint> outSet) {
		this.outSet = outSet;
	}

	public Set<CodePoint> getGenSet() {
		return genSet;
	}

	public void setGenSet(Set<CodePoint> genSet) {
		this.genSet = genSet;
	}

	@Override
	public String toString() {
		return node.toString();
	}

}