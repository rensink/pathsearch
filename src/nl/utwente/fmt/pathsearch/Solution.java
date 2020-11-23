package nl.utwente.fmt.pathsearch;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Solution extends ArrayList<Edge> {
    private static final String FILE_SEP = System.getProperty("file.separator");
    private static final String DOT_DIR = System.getProperty("user.dir") + FILE_SEP + "dots";
    private static int id_count;
    private int stepCount;
    private final Graph graph;
    private final Node target;
    private final int id;

    public Solution(Graph graph, Node target) {
        this.graph = graph;
        this.target = target;
        this.id = id_count;
        id_count++;
    }

    public int getId() {
        return this.id;
    }

    public int getStepCount() {
        return this.stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    /**
     * Checks whether this is a valid solution leading to a given target
     * 
     * @param target
     * @return
     */
	public boolean validate(Node target) {
        var nodes = this.stream().map(Edge::target).collect(Collectors.toSet());
		if (nodes.size() != this.size()) {
			return false;
		}
		var produced = new HashSet<>();
        var unusedNodes = new HashSet<>(nodes);
        var unusedEdges = new HashSet<>(this);
		Map<Node, Set<Edge>> outEdges = new HashMap<>();
		nodes.stream().forEach(n -> outEdges.put(n, new HashSet<>()));
		try {
			this.stream().forEach(e -> e.source().stream().forEach(n -> outEdges.get(n).add(e)));
		} catch (NullPointerException exc) {
			return false;
		}
        var fresh = new LinkedList<Node>();
        for (Edge e : this) {
            if (e.source().isEmpty()) {
                unusedEdges.remove(e);
                fresh.add(e.target());
            }
        }
        while (!fresh.isEmpty()) {
            var next = fresh.poll();
            produced.add(next);
            for (Edge e : outEdges.get(next)) {
                if (produced.containsAll(e.source())) {
                    var oldEdge = unusedEdges.remove(e);
                    assert oldEdge;
                    unusedNodes.removeAll(e.source());
                    fresh.add(e.target());
                }
            }
		}
		if (!produced.equals(nodes)) {
			return false;
		}
        if (!unusedEdges.isEmpty()) {
            return false;
        }
        if (unusedNodes.size() != 1) {
			return false;
		}
        if (!unusedNodes.iterator().next().equals(target)) {
			return false;
		}
		return true;
	}

    public String getName() {
        return this.graph.getName() + "-" + this.target.name() + "-" + this.id;
    }

    public String toDot() {
        var result = new StringBuilder();
        var nodes = new HashSet<Node>();
        stream().map(Edge::source).forEach(nodes::addAll);
        stream().map(Edge::target).forEach(nodes::add);
        result.append("digraph \"" + getName() + "\" {\n");
        result.append("node [style=filled,color=gold3,fillcolor=gold1,shape=box] \n");
        result.append("edge [color=gold3] \n");
        nodes.stream().map(Solution::nodeLine).forEach(result::append);
        stream().map(Solution::edgeLines).forEach(result::append);
        result.append("}");
        return result.toString();
    }

    public void saveDot() {
        File temp;
        try {
            temp = new File(DOT_DIR + FILE_SEP + getName() + ".dot");
            var fout = new FileWriter(temp);
            fout.write(toDot());
            fout.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    static private String nodeLine(Node node) {
        return String.format("\"%s\" [color=deepskyblue3,fillcolor=deepskyblue1,shape=ellipse]%n", node.name());
    }

    static private String edgeLines(Edge edge) {
        var result = new StringBuilder();
        result.append(String.format("\"%s\" [height=0,width=0] %n", edge.name()));
        for (var i = 0; i < edge.source().size(); i++) {
            result.append(
                    String.format("\"%s\" -> \"%s\" [headlabel=\"%s\"]%n", edge.source().get(i).name(),
                    edge.name(), i));
        }
        result.append(String.format("\"%s\" -> \"%s\"%n", edge.name(), edge.target().name()));
        return result.toString();
    }
}
