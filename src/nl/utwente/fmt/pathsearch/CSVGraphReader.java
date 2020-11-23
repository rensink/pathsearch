package nl.utwente.fmt.pathsearch;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderHeaderAwareBuilder;
import com.opencsv.exceptions.CsvException;

public class CSVGraphReader {
    /**
     * Flag controlling whether generators are added to nodes without incoming
     * edges.
     */
    private static boolean COMPLETE = false;
    /** Flag controlling whether circular edges are omitted. */
    private static boolean OPTIMISE = false;
    /** Flag controlling whether log messages are emitted. */
    private static boolean LOG = true;

    private final String filename;
    private final Map<String, Node> nodeMap;
    private Graph graph;

    public CSVGraphReader(String filename) {
        this.filename = filename;
        this.nodeMap = new HashMap<>();
    }

    public Graph run() throws IOException, CsvException {
        if (this.graph == null) {
            var graphName = new File(this.filename).getName();
            this.graph = new Graph(graphName);
            buildGraph();
        }
        return this.graph;
    }

    private void buildGraph() throws IOException, CsvException {
        // Build reader instance
        var parser = new CSVParserBuilder().withSeparator(';').build();
        var reader = new CSVReaderHeaderAwareBuilder(new FileReader(this.filename)).withCSVParser(parser).build();
        // Read all rows at once
        reader.readAll().stream().forEach(r -> addRow(r));
        if (COMPLETE) {
            completeGraph();
        }
    }

    private void addRow(String[] row) {
        var target = addNode(row[0]);
        List<Node> source = new ArrayList<>();
        for (var i = 2; i < row.length && row[i].length() > 0; i++) {
            source.add(addNode(row[i]));
        }
        addEdge(source, row[1], target);
    }

    private Node addNode(String name) {
        assert !name.isBlank();
        var result = this.nodeMap.get(name);
        if (result == null) {
            result = new Node(name);
            this.nodeMap.put(name, result);
            this.graph.addNode(result);
        }
        return result;
    }

    private void addEdge(List<Node> source, String name, Node target) {
        var result = new Edge(source, name, target);
        if (OPTIMISE && source.contains(target)) {
            log("Circular edge %s not added", result);
        } else {
            this.graph.addEdge(result);
        }
    }

    private void completeGraph() {
        var allNodes = new HashSet<>(this.graph.getNodes());
        this.graph.getEdges().stream().map(Edge::target).forEach(allNodes::remove);
        log("Adding %s generators to %s", allNodes.size(), this.graph.getName());
        allNodes.stream().forEach(n -> addEdge(Collections.emptyList(), n.name() + "-C", n));
    }

    private void log(String message, Object... args) {
        if (LOG) {
            System.out.printf(message, args);
            System.out.println();
        }
    }
}
