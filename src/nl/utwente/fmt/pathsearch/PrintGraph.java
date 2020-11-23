package nl.utwente.fmt.pathsearch;

/**
 * Simple test of graph reading functionality.
 * 
 * @author Arend Rensink
 */
public class PrintGraph {
    public static void main(String[] args) {
        try {
            var g = new CSVGraphReader(args[0]).run();
            System.out.print(g);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
