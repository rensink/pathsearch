package nl.utwente.fmt.pathsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class MySearchInstance implements Iterator<Solution> {
    private final GraphFacade gf;
	private final Node product;
    /** List of nodes already made. */
    private final Deque<Node> made;
    /** List of nodes yet to be made. */
    private final Deque<Node> frontier;
	/** Mapping from found nodes to their (currently known) downstream nodes. */
    private final Map<Node, Set<Node>> downstreamMap;
    /** Mapping from made nodes to the index of their maker. */
    private final Map<Node, Integer> makerIxMap;
	/**
	 * Mapping from producing edges to the downstream delta of their early
	 * producers.
	 */
	private final Map<Edge, Map<Node, Set<Node>>> deltasMap;
	/** Flag indicating that the search space is exhausted. */
	private boolean exhausted;
	/**
	 * Flag indicating that the next solution has been found but not yet delivered.
	 */
	private boolean nextValid;
    /** Counts the number of steps taken during search. */
    private int stepCount;
	
    public MySearchInstance(GraphFacade gf, Node product) {
        this.gf = gf;
		this.product = product;
        this.made = new LinkedList<>();
        this.frontier = new LinkedList<>();
        this.makerIxMap = new HashMap<>();
        this.downstreamMap = new HashMap<>();
		this.deltasMap = new HashMap<>();
        this.frontier.add(product);
        this.downstreamMap.put(product, Collections.emptySet());
		this.exhausted = false;
		this.nextValid = false;
	}

	public Graph getGraph() {
        return this.gf.getGraph();
	}

	public Node getProduct() {
		return this.product;
	}

    public int getStepCount() {
        return this.stepCount;
    }
	
	@Override
	public boolean hasNext() {
		if (!this.exhausted && !this.nextValid) {
			findNext();
		}
        return !this.exhausted;
	}

	@Override
	public Solution next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		var result = computeSolution();
		this.nextValid = false;
		return result;
	}

	private Solution computeSolution() {
        var result = new Solution(getGraph(), getProduct());
        this.made.stream().map(this::getMaker).forEach(result::add);
        result.setStepCount(getStepCount());
		return result;
	}

	private void findNext() {
		assert !this.nextValid;
        var forward = !this.frontier.isEmpty();
		while (!this.exhausted && !this.nextValid) {
			if (forward) {
				forward = nextNode();
			} else {
                forward = nextMaker();
			}
            log();
			if (forward) {
                this.nextValid = this.frontier.isEmpty();
			} else {
                this.exhausted = this.made.isEmpty();
			}
		}
	}

	/** Find a production for the next found, unproduced node. */
	private boolean nextNode() {
        var next = this.frontier.poll();
		return nextStep(next, 0);
	}

	/** Find the next production for the most recently produced node. */
    private boolean nextMaker() {
        var next = this.made.removeLast();
        var makerIx = this.makerIxMap.remove(next);
        removeMaker(getInEdge(next, makerIx));
        return nextStep(next, makerIx + 1);
	}

	private boolean nextStep(Node next, int from) {
		var success = false;
		var inEdges = getInEdges(next);
        var downstream = this.downstreamMap.get(next);
        var makerIx = from;
        while (!success && makerIx < inEdges.size()) {
            var e = inEdges.get(makerIx);
            if (this.gf.getPre(e).stream().anyMatch(downstream::contains)) {
                makerIx++;
			} else {
				success = true;
			}
		}
		if (success) {
            addMaker(next, makerIx);
        } else {
            this.frontier.push(next);
		}
        this.stepCount++;
		return success;
	}

	/**
	 * Adds the source nodes of a given edge to the found nodes, and sets or updates
	 * the upstream nodes.
	 */
    /**
     * @param made
     * @param makerIx
     */
    private void addMaker(Node made, int makerIx) {
        this.made.add(made);
        this.makerIxMap.put(made, makerIx);
        var maker = getInEdge(made, makerIx);
        var makerDeltas = new HashMap<Node, Set<Node>>();
        this.deltasMap.put(maker, makerDeltas);
        // compute the resulting (additional) downstream
        var newDownstream = new ArrayList<>(this.downstreamMap.get(made));
        newDownstream.add(made);
        for (var pred : maker.source()) {
            if (this.downstreamMap.containsKey(pred)) {
                // this is a previously found node
                // add the new downstream to it and all its upstream
                var upstream = new LinkedList<Node>();
                upstream.add(pred);
                while (!upstream.isEmpty()) {
                    var next = upstream.remove();
                    if (makerDeltas.containsKey(next)) {
                        continue;
                    }
                    var nextDelta = new HashSet<Node>();
                    makerDeltas.put(next, nextDelta);
                    var oldDownstream = this.downstreamMap.get(next);
                    newDownstream.stream().filter(oldDownstream::add).forEach(nextDelta::add);
                    if (isMade(next)) {
                        upstream.addAll(getMaker(next).source());
                    }
                }
            } else {
                // this is a newly found node
                this.frontier.push(pred);
                this.downstreamMap.put(pred, new HashSet<>(newDownstream));
            }
        }
    }

    private void removeMaker(Edge edge) {
        // Iterate over the maker's source nodes in reverse order
        var predIter = edge.source().listIterator(edge.source().size());
        var delta = this.deltasMap.remove(edge);
		while (predIter.hasPrevious()) {
			var pred = predIter.previous();
            if (!delta.containsKey(pred)) {
                // this predecessor was found later; it must be the last in the frontier
                assert pred == this.frontier.peek()
                        : String.format("Source %s of %s is not at front of %s", pred, edge, this.frontier);
                this.frontier.remove();
                this.downstreamMap.remove(pred);
			}
		}
		// restore the downstream of the early predecessors
        delta.entrySet().stream().forEach(e -> this.downstreamMap.get(e.getKey()).removeAll(e.getValue()));
	}
	
    private boolean isMade(Node n) {
        return this.makerIxMap.containsKey(n);
    }

    private int getMakerIx(Node n) {
        return this.makerIxMap.get(n);
    }

    private Edge getMaker(Node n) {
        return getInEdge(n, getMakerIx(n));
    }

    private Edge getInEdge(Node n, int i) {
        return getInEdges(n).get(i);
    }

	private List<Edge> getInEdges(Node node) {
        return this.gf.getInEdges(node);
	}
	
    static private final boolean LOG = false;

    private void log() {
        if (LOG) {
            var b = new StringBuilder();
            var i = 0;
            for (var n : this.made) {
                b.append("" + i + ":");
                b.append(this.makerIxMap.get(n));
                b.append('/');
                b.append(getInEdges(n).size());
                b.append(' ');
                i++;
            }
            for (var n : this.frontier) {
                b.append("" + i + ":. ");
                i++;
            }
            System.out.println(b);
        }
    }
}
