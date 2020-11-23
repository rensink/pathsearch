package nl.utwente.fmt.pathsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import com.opencsv.exceptions.CsvException;

public class MySearchTest {
    private static final String USER_DIR = System.getProperty("user.dir");
    private static final String FILE_SEP = System.getProperty("file.separator");
    private static final String GRAPH_HOME = USER_DIR + FILE_SEP + "graphs-0-14";

    @Test
    public void testHelloWorld() {
        System.out.println("User dir: " + System.getProperty("user.dir"));
        var g = readGraph("helloworld");
        g.addGenerator("domain.CustomerInformation.CustomerId");
        var it = new MySearch(g).search(new Node("domain.Savings.SavingsMeResponse"));
        var sol = it.next();
        assertFalse(it.hasNext());
        sol.saveDot();
        assertEquals(4, sol.size());
    }

    @Test
    public void testExtratest() {
        var g = readGraph("extratest");
        g.addGenerator("wqiaiyuUwj");
        var it = new MySearch(g).search(new Node("RTyLrWLwQv"));
        var i = 0;
        while (it.hasNext()) {
            var sol = it.next();
            i++;
            System.out.printf("#%s: size %s (%s steps)%n", i, sol.size(), sol.getStepCount());
        }
    }

    private Graph readGraph(String filename) {
        try {
            return new CSVGraphReader(GRAPH_HOME + FILE_SEP + filename + ".csv").run();
        } catch (IOException | CsvException e) {
            fail(e.getMessage());
            return null;
        }
    }
}
