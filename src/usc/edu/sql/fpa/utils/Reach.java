package usc.edu.sql.fpa.utils;

import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.toolkits.graph.UnitGraph;

import java.util.*;

public class Reach {

    private final List<RNode> allRNode = new ArrayList<>();
    // a node mapping to the nodes it can reach
    private final Map<Unit, Set<Unit>> rTable = new HashMap<>();
    private final Map<Unit, Set<Unit>> rReverseTable = new HashMap<>();
    
    public Reach(UnitGraph cfg) {

        for (Unit n : cfg.getBody().getUnits()) {
            allRNode.add(new RNode(n));

            rTable.put(n, new HashSet<>());
            rReverseTable.put(n, new HashSet<>());
        }
        computeReachability(cfg);
    }

    private void initialize() {
        for (RNode rn : allRNode) {
            rn.getGenSet().add(rn.getNode());
            rn.getOutSet().add(rn.getNode());
        }
    }

    private boolean compareTwoSet(Set<Unit> oldset, Set<Unit> newset) {
        return oldset.containsAll(newset) && newset.containsAll(oldset);
    }

    private void computeReachability(UnitGraph cfg) {
        initialize();
        Map<RNode, List<RNode>> preList = new HashMap<>();
        // Create predecessor list for each node
        for (RNode node : allRNode) {
            List<RNode> nodePre = new ArrayList<>();
            for (Unit pre : cfg.getPredsOf(node.getNode())) {
                for (RNode n : allRNode)
                    if (n.getNode().equals(pre))
                        nodePre.add(n);

            }
            preList.put(node, nodePre);

        }
        Map<Unit, Set<Unit>> old = new HashMap<>();
        for (RNode rn : allRNode) {
            Set<Unit> s = new HashSet<>(rn.getOutSet());
            old.put(rn.getNode(), s);

        }
        boolean needtoloop = true;

        while (needtoloop) {
            needtoloop = false;
            for (RNode rn : allRNode) {
                System.out.println(rn.getNode());
                Value genVal = null;
                if (rn.getNode() instanceof DefinitionStmt) {
                    genVal = ((DefinitionStmt) rn.getNode()).getLeftOp();
                }
                // Union the out set of all the nodes in the predecessor
                // list，add to the in set of node rn
                for (RNode node : preList.get(rn)) {
                    for (Unit outnode : node.getOutSet())
                        rn.getInSet().add(outnode);
                }
                // Union the gen set and in set to the out set of node rn
                for (Unit gennode : rn.getGenSet())
                    rn.getOutSet().add(gennode);
                for (Unit innode : rn.getInSet()) {
                    boolean isKilled = false;
                    for (ValueBox vb : innode.getUseAndDefBoxes()) {
                        if (vb.getValue().equals(genVal)) {
                            isKilled = true;
                            break;
                        }
                    }

                    if (!isKilled)
                        rn.getOutSet().add(innode);

                }
                if (!compareTwoSet(old.get(rn.getNode()), rn.getOutSet())) {
                    needtoloop = true;
                    Set<Unit> s = new HashSet<>(rn.getOutSet());
                    old.put(rn.getNode(), s);

                }
            }
        }

        for (RNode rn : allRNode) {
            for (Unit n : rn.getInSet()) {
                rTable.get(n).add(rn.getNode());
            }
        }
        for (RNode rn : allRNode) {
            for (Unit n : rn.getInSet()) {
                rReverseTable.get(rn.getNode()).add(n);
            }
        }

//		System.out.println(rReverseTable); 
    }

    /**
     * Return a map mapping a node to the nodes it can reach.
     */
    public Map<Unit, Set<Unit>> getReachableTable() {
        return rTable;
    }
    
    public Map<Unit, Set<Unit>> getReverseReachableTable() {
        return rReverseTable;
    }

}

class RNode {
    private Unit node;
    private Set<Unit> inSet = new HashSet<>();
    private Set<Unit> outSet = new HashSet<>();
    private Set<Unit> genSet = new HashSet<>();

    public RNode(Unit node) {
        this.node = node;
    }

    public Unit getNode() {
        return node;
    }

    public void setNode(Unit node) {
        this.node = node;
    }

    public Set<Unit> getInSet() {
        return inSet;
    }

    public void setInSet(Set<Unit> inSet) {
        this.inSet = inSet;
    }

    public Set<Unit> getOutSet() {
        return outSet;
    }

    public void setOutSet(Set<Unit> outSet) {
        this.outSet = outSet;
    }

    public Set<Unit> getGenSet() {
        return genSet;
    }

    public void setGenSet(Set<Unit> genSet) {
        this.genSet = genSet;
    }

}
