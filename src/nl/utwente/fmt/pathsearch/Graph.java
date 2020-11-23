package nl.utwente.fmt.pathsearch;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hypergraph
 * @author Arend Rensink
 *
 */
public class Graph {
	private final String name;
	private final Set<Node> nodes;
	private final Set<Edge> edges;
	
	public Graph(String name) {
		this.name = name;
		this.nodes = new LinkedHashSet<>();
		this.edges = new LinkedHashSet<>();
	}

	public String getName() {
		return this.name;
	}

	public Set<Node> getNodes() {
		return this.nodes;
	}

	public Set<Edge> getEdges() {
		return this.edges;
	}

    public boolean addNode(Node node) {
        return this.nodes.add(node);
	}

    public boolean addEdge(Edge edge) {
        return this.edges.add(edge);
	}

    public boolean addNode(String name) {
        return addNode(new Node(name));
    }

    public boolean addGenerator(String name) {
        return addEdge(Collections.emptyList(), "GEN-" + name, name);
    }

    public boolean addEdge(List<Node> source, String name, Node target) {
        return addEdge(new Edge(source, name, target));
    }

    public boolean addEdge(List<String> sourceNames, String edgeName, String targetName) {
        var source = sourceNames.stream().map(Node::new).collect(Collectors.toList());
        return addEdge(source, edgeName, new Node(targetName));
    }

    @Override
    public String toString() {
        var b = new StringBuilder();
        getEdges().stream().forEach(e -> b.append(String.format("%s --%s-> %s%n", e.source(), e.name(), e.target())));
        return b.toString();
    }
}
