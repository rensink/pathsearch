package nl.utwente.fmt.pathsearch;

import java.util.Iterator;

/**
 * Algorithm interface for the search
 */
public interface Search {
    Iterator<Solution> search(String name);

    Iterator<Solution> search(Node product);
}
