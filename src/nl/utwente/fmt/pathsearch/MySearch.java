package nl.utwente.fmt.pathsearch;

import java.util.Iterator;
import java.util.LinkedList;

public class MySearch implements Search {
    /** Flag controlling whether log messages are emitted. */
    private static boolean LOG = true;

    private static void log(String message, Object... args) {
        if (LOG) {
            System.out.printf(message, args);
            System.out.println();
        }
    }

    private final GraphFacade gf;

    public MySearch(Graph graph) {
        this.gf = pruneGraph(new GraphFacade(graph));
    }

    public Graph getGraph() {
        return this.gf.getGraph();
    }

    @Override
    public Iterator<Solution> search(String name) {
        return search(new Node(name));
    }

    @Override
    public Iterator<Solution> search(Node product) {
        return new MySearchInstance(this.gf, product);
    }

    private GraphFacade pruneGraph(GraphFacade gf) {
        var g = new Graph(gf.getName());
        gf.getEdges().stream().filter(e -> e.source().isEmpty()).forEach(g::addEdge);
        g.getEdges().stream().map(Edge::target).forEach(g::addNode);
        final var reachable = new LinkedList<Node>(g.getNodes());
        while (!reachable.isEmpty()) {
            var next = reachable.remove();
            gf.getOutEdgeMap()
                    .get(next)
                    .stream()
                    .filter(e -> g.getNodes().containsAll(e.source()))
                    .filter(e -> !e.source().contains(e.target()))
                    .forEach(e -> {
                        if (g.getNodes().add(e.target())) {
                            reachable.add(e.target());
                        }
                        g.addEdge(e);
            });
        }
        var result = new GraphFacade(g);
        log("Result of pruning %s", gf.getName());
        log("Original graph: %s nodes, %s edges", gf.getNodes().size(), gf.getEdges().size());
        log("Pruned graph: %s nodes, %s edges", result.getNodes().size(), result.getEdges().size());
        log("Max depth: %s", result.getNodeDepthMap().values().stream().reduce(0, Integer::max));
        return result;
    }
}
