package other.diclens;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Supplier;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.filters.FilterUtils;
import edu.uci.ics.jung.algorithms.shortestpath.PrimMinimumSpanningTree;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.graph.util.TreeUtils;

/**
 * For the input Graph, creates a MinimumSpanningTree using a variation of
 * Prim's algorithm.
 *
 * @author Tom Nelson - tomnelson@dev.java.net
 *
 * @param <V> the vertex type
 * @param <E> the edge type
 */
@SuppressWarnings("unchecked")
public class ParallelMSF<V, E> {

	protected Graph<V, E> graph;
	protected Forest<V, E> forest;
	protected Function<? super E, Double> weights = Functions.<Double>constant(1.0);

	/**
	 * Create a Forest from the supplied Graph and supplied Supplier, which is used
	 * to create a new, empty Forest. If non-null, the supplied root will be used as
	 * the root of the tree/forest. If the supplied root is null, or not present in
	 * the Graph, then an arbitary Graph vertex will be selected as the root. If the
	 * Minimum Spanning Tree does not include all vertices of the Graph, then a
	 * leftover vertex is selected as a root, and another tree is created
	 *
	 * @param graph       the graph for which the minimum spanning forest will be
	 *                    generated
	 * @param supplier    a factory for the type of forest to build
	 * @param treeFactory a factory for the type of tree to build
	 * @param weights     edge weights; may be null
	 */
	public ParallelMSF(Graph<V, E> graph, Supplier<Forest<V, E>> supplier, Supplier<? extends Graph<V, E>> treeFactory,
			Function<? super E, Double> weights) {
		this(graph, supplier.get(), treeFactory, weights);
	}

	/**
	 * Create a forest from the supplied graph, populating the supplied Forest,
	 * which must be empty. If the supplied root is null, or not present in the
	 * Graph, then an arbitary Graph vertex will be selected as the root. If the
	 * Minimum Spanning Tree does not include all vertices of the Graph, then a
	 * leftover vertex is selected as a root, and another tree is created
	 *
	 * @param graph       the Graph to find MST in
	 * @param forest      the Forest to populate. Must be empty
	 * @param treeFactory a factory for the type of tree to build
	 * @param weights     edge weights, may be null
	 */
	public ParallelMSF(Graph<V, E> graph, Forest<V, E> forest, Supplier<? extends Graph<V, E>> treeFactory,
			Function<? super E, Double> weights) {

		if (forest.getVertexCount() != 0) {
			throw new IllegalArgumentException("Supplied Forest must be empty");
		}
		this.graph = graph;
		this.forest = forest;
		if (weights != null) {
			this.weights = weights;
		}

		final WeakComponentClusterer<V, E> wcc = new WeakComponentClusterer<V, E>();
		final Set<Set<V>> component_vertices = wcc.apply(graph);
		final List<Graph<V, E>> components = new ArrayList<Graph<V, E>>(
				FilterUtils.createAllInducedSubgraphs(component_vertices, graph));
		final ReentrantLock lock = new ReentrantLock();
		final Graph<V, E>[] subTrees = new Graph[components.size()];
		IntStream.range(0, components.size()).forEach(i -> {
			final Graph<V, E> component = components.get(i);
			final PrimMinimumSpanningTree<V, E> mst = new PrimMinimumSpanningTree<V, E>(treeFactory, this.weights);
			final Graph<V, E> subTree = mst.apply(component);
			if (subTree instanceof Tree) {
				lock.lock();
				try {
					subTrees[i] = subTree;
				} finally {
					lock.unlock();
				}
			}
		});
		for (int i = 0; i < subTrees.length; ++i)
			if (subTrees[i] instanceof Tree)
				TreeUtils.addSubTree(forest, (Tree<V, E>) subTrees[i], null, null);

	}

	/**
	 * @return the generated forest
	 */
	public Forest<V, E> getForest() {
		return forest;
	}
}
