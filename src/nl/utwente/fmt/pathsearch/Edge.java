/**
 * 
 */
package nl.utwente.fmt.pathsearch;

import java.util.Collections;
import java.util.List;

/**
 * Hyperedge with (possibly empty) list of source nodes, name, and a single target node
 * @author Arend Rensink
 *
 */
public record Edge(List<Node> source, String name, Node target) {
	public Edge {
		this.source = Collections.unmodifiableList(source);
	}

    @Override
    public String toString() {
        var result = new StringBuilder();
        var first = true;
        for (Node n : source()) {
            if (first) {
                first = false;
            } else {
                result.append(",");
            }
            result.append(n);
        }
        result.append("--" + name() + "->");
        result.append(target());
        return result.toString();
    }
}
