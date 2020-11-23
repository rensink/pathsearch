package nl.utwente.fmt.pathsearch;

/**
 * Named graph node.
 * @author Arend Rensink
 *
 */
public record Node(String name) {
    @Override
    public String toString() {
        return "N(" + name() + ")";
    }
}
