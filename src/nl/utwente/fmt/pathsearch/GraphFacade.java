package nl.utwente.fmt.pathsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class GraphFacade {
    private final Graph graph;

    public GraphFacade(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return this.graph;
    }

    public String getName() {
        return getGraph().getName();
    }

    public Set<Node> getNodes() {
        return getGraph().getNodes();
    }

    public Set<Edge> getEdges() {
        return getGraph().getEdges();
    }

    public Set<Node> getPre(Node node) {
        return getNodePreMap().get(node);
    }

    public Map<Node, Set<Node>> getNodePreMap() {
        if (this.nodePreMap == null) {
            computePreMaps();
        }
        return this.nodePreMap;
    }

    public Set<Node> getPre(Edge edge) {
        return getEdgePreMap().get(edge);
    }

    public Map<Edge, Set<Node>> getEdgePreMap() {
        if (this.edgePreMap == null) {
            computePreMaps();
        }
        return this.edgePreMap;
    }

    private void computePreMaps() {
        Map<Node, Set<Node>> nodePreMap = new LinkedHashMap<>();
        Map<Edge, Set<Node>> edgePreMap = new LinkedHashMap<>();
        getEdges().stream().filter(e -> e.source().isEmpty()).forEach(e -> edgePreMap.put(e, Collections.emptySet()));
        edgePreMap.keySet().stream().map(Edge::target).forEach(n -> nodePreMap.put(n, Collections.emptySet()));
        var fresh = new LinkedList<>(nodePreMap.keySet());
        while (!fresh.isEmpty()) {
            var next = fresh.poll();
            for (Edge e : getOutEdges(next)) {
                if (nodePreMap.keySet().containsAll(e.source())) {
                    Set<Node> pre = new LinkedHashSet<>(e.source());
                    e.source().stream().map(nodePreMap::get).forEach(pre::addAll);
                    var ePre = edgePreMap.get(e);
                    if (ePre == null || ePre.size() > pre.size()) {
                        edgePreMap.put(e, pre);
                        if (!nodePreMap.containsKey(e.target())) {
                            assert !pre.contains(e.target());
                            nodePreMap.put(e.target(), new LinkedHashSet<>(pre));
                            fresh.add(e.target());
                        } else if (nodePreMap.get(e.target()).retainAll(pre)) {
                            fresh.add(e.target());
                        }
                    }
                }
            }
        }
        assert nodePreMap.keySet().equals(getNodes());
        assert edgePreMap.keySet().equals(getEdges());
        assert validPreMaps(nodePreMap, edgePreMap);
        this.nodePreMap = nodePreMap;
        this.edgePreMap = edgePreMap;
    }

    private boolean validPreMaps(Map<Node, Set<Node>> nodePreMap, Map<Edge, Set<Node>> edgePreMap) {
        var nPre = new LinkedHashMap<Node, Set<Node>>();
        for (var entry: edgePreMap.entrySet()) {
            var edge = entry.getKey();
            var pre = new LinkedHashSet<>(edge.source());
            for (var node: edge.source()) {
                pre.addAll(nodePreMap.get(node));
            }
            if (!pre.equals(entry.getValue())) {
                return false;
            }
            if (!nPre.containsKey(edge.target())) {
                nPre.put(edge.target(), new HashSet<>(entry.getValue()));
            } else {
                nPre.get(edge.target()).retainAll(entry.getValue());
            }
        }
        return nPre.equals(nodePreMap);
    }

    private Map<Node, Set<Node>> nodePreMap;
    private Map<Edge, Set<Node>> edgePreMap;

    public int getDepth(Node node) {
        var result = getNodeDepthMap().get(node);
        return result == null ? Integer.MAX_VALUE : result;
    }

    public int getDepth(Edge edge) {
        var result = getEdgeDepthMap().get(edge);
        return result == null ? Integer.MAX_VALUE : result;
    }

    public Map<Node, Integer> getNodeDepthMap() {
        if (this.nodeDepthMap == null) {
            computeDepthMaps();
        }
        return this.nodeDepthMap;
    }

    public Map<Edge, Integer> getEdgeDepthMap() {
        if (this.edgeDepthMap == null) {
            computeDepthMaps();
        }
        return this.edgeDepthMap;
    }

    private void computeDepthMaps() {
        Map<Node, Integer> nodeMap = new LinkedHashMap<>();
        Map<Edge, Integer> edgeMap = new LinkedHashMap<>();
        getEdges().stream().filter(e -> e.source().isEmpty()).forEach(e -> edgeMap.put(e, 0));
        edgeMap.keySet().stream().map(e -> e.target()).forEach(n -> nodeMap.put(n, 0));
        var fresh = new LinkedList<>(nodeMap.keySet());
        while (!fresh.isEmpty()) {
            var next = fresh.pollFirst();
            for (Edge e: getOutEdges(next)) {
                if (nodeMap.keySet().containsAll(e.source())) {
                    int depth = e.source().stream().map(n -> nodeMap.get(n)).reduce(0, Integer::max);
                    edgeMap.put(e, depth + 1);
                    if (!nodeMap.containsKey(e.target()) || nodeMap.get(e.target()) > depth + 1) {
                        nodeMap.put(e.target(), depth + 1);
                        fresh.add(e.target());
                    }
                }
            }
        }
        this.nodeDepthMap = nodeMap;
        this.edgeDepthMap = edgeMap;
    }

    private Map<Node, Integer> nodeDepthMap;
    private Map<Edge, Integer> edgeDepthMap;

    public List<Edge> getInEdges(Node node) {
        return getInEdgeMap().get(node);
    }

    public Map<Node, List<Edge>> getInEdgeMap() {
        if (this.inEdgeMap == null) {
            this.inEdgeMap = computeInEdgeMap();
        }
        return this.inEdgeMap;
    }

    private final Map<Node, List<Edge>> computeInEdgeMap() {
        Map<Node, SortedSet<Edge>> map = new HashMap<>();
        this.graph.getNodes().stream().forEach(n -> map.put(n, new TreeSet<>(GraphFacade::compareEdges)));
        this.graph.getEdges().stream().forEach(e -> map.get(e.target()).add(e));
        Map<Node, List<Edge>> result = new HashMap<>();
        map.entrySet().stream().forEach(e -> result.put(e.getKey(), new ArrayList<>(e.getValue())));
        return result;
    }

    /** Mapping from nodes to their incoming edges. */
    private Map<Node, List<Edge>> inEdgeMap;

    public List<Edge> getOutEdges(Node node) {
        return getOutEdgeMap().get(node);
    }

    public Map<Node, List<Edge>> getOutEdgeMap() {
        if (this.outEdgeMap == null) {
            this.outEdgeMap = computeOutEdgeMap();
        }
        return this.outEdgeMap;
    }

    private final Map<Node, List<Edge>> computeOutEdgeMap() {
        Map<Node, List<Edge>> result = new HashMap<>();
        this.graph.getNodes().stream().forEach(n -> result.put(n, new ArrayList<>()));
        this.graph.getEdges().stream().forEach(e -> e.source().stream().forEach(n -> result.get(n).add(e)));
        return result;
    }

    /** Mapping from nodes to their incoming edges. */
    private Map<Node, List<Edge>> outEdgeMap;

    static private int compareEdges(Edge e1, Edge e2) {
        var result = e1.source().size() - e2.source().size();
        if (result == 0) {
            result = e1.name().compareTo(e2.name());
        }
        return result;
    }
}
