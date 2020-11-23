package nl.utwente.fmt.pathsearch;

import java.io.IOException;
import java.util.Collections;

import com.opencsv.exceptions.CsvException;

/**
 * Simple test of graph reading functionality.
 * 
 * @author Arend Rensink
 */
public class SearchGraph {
    /** Flag controlling whether log messages are emitted. */
    private static boolean LOG = true;
    private static boolean DOT = false;
    private static int CRUMB_LINE_COUNT = 100_000;
    private static int CRUMB_COUNT = CRUMB_LINE_COUNT / 100;

    private static void log(String message, Object... args) {
        if (LOG) {
            System.out.printf(message, args);
            System.out.println();
        }
    }

    private static void dot(int count) {
        if (count % CRUMB_LINE_COUNT == 0) {
            System.out.printf(" (%s * %s)%n", count / CRUMB_LINE_COUNT, CRUMB_LINE_COUNT);
        } else if (count % CRUMB_COUNT == 0) {
            System.out.print(".");
        }
    }

    public static final int GIVEN_COUNT = 5;
    public static final int SEARCH_COUNT = 5;

    private static final String USER_DIR = System.getProperty("user.dir");
    private static final String FILE_SEP = System.getProperty("file.separator");
    private static final String GRAPH_HOME = USER_DIR + FILE_SEP + "graphs-0-14";

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                searchRun("veryLarge_0.csv", "73_v-t-1163");
                // Stopped progress after about 1_000_000 solutions
                searchRun("large_0.csv", "15_v-t-35");
                searchRun("large_0.csv", "5_v-t-389");
            } else {
                randomSearch(args[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void searchRun(String filename, String... targets) throws IOException, CsvException {
        var g = enhance(new CSVGraphReader(GRAPH_HOME + FILE_SEP + filename).run());
        var searcher = new MySearch(g);
        for (String target : targets) {
            searchRun(searcher, new Node(target));
        }
    }

    /** Adds a number of "givens" to the graph */
    private static Graph enhance(Graph g) {
        var nodeIter = g.getNodes().iterator();
        for (var i = 0; i < GIVEN_COUNT && nodeIter.hasNext(); i++) {
            var n = nodeIter.next();
            g.addEdge(Collections.emptyList(), n.name() + "-C", n);
        }
        return g;
    }

    private static void randomSearch(String filename) throws IOException, CsvException {
        var g = enhance(new CSVGraphReader(filename).run());
        var searcher = new MySearch(g);
        for (var i = 0; i < SEARCH_COUNT; i++) {
            randomSearchRun(searcher);
        }
    }

    private static void randomSearchRun(MySearch searcher) {
        var g = searcher.getGraph();
        var targetIx = (int) (Math.random() * g.getNodes().size());
        var nodeIter = g.getNodes().iterator();
        for (var i = 0; i < targetIx; i++) {
            nodeIter.next();
        }
        var target = nodeIter.next();
        searchRun(searcher, target);
    }

    private static void searchRun(MySearch searcher, Node target) {
        var solIter = searcher.search(target);
        System.out.printf("Solutions for %s in %s%n", target, searcher.getGraph().getName());
        var count = 0;
        var stepCount = 0;
        while (solIter.hasNext()) {
            count++;
            var sol = solIter.next();
            if (DOT && count == 1) {
                sol.saveDot();
            }
            assert sol.validate(target);
            if (LOG) {
                log("#%s: size %s (%s steps)", count, sol.size(), sol.getStepCount());
            } else {
                dot(count);
            }
            stepCount = sol.getStepCount();
        }
        System.out.printf("Done after %s solutions (%s search steps) %n", count, stepCount);
    }
}
