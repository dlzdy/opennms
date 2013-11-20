package org.opennms.features.topology.app.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opennms.core.test.MockLogAppender;
import org.opennms.features.topology.api.Graph;
import org.opennms.features.topology.api.GraphContainer;
import org.opennms.features.topology.api.GraphVisitor;
import org.opennms.features.topology.api.support.VertexHopGraphProvider;
import org.opennms.features.topology.api.support.VertexHopGraphProvider.FocusNodeHopCriteria;
import org.opennms.features.topology.api.support.VertexHopGraphProvider.VertexHopCriteria;
import org.opennms.features.topology.api.topo.AbstractEdgeRef;
import org.opennms.features.topology.api.topo.AbstractVertex;
import org.opennms.features.topology.api.topo.AbstractVertexRef;
import org.opennms.features.topology.api.topo.CollapsibleCriteria;
import org.opennms.features.topology.api.topo.Edge;
import org.opennms.features.topology.api.topo.EdgeProvider;
import org.opennms.features.topology.api.topo.EdgeRef;
import org.opennms.features.topology.api.topo.GraphProvider;
import org.opennms.features.topology.api.topo.SimpleEdgeProvider;
import org.opennms.features.topology.api.topo.Vertex;
import org.opennms.features.topology.api.topo.VertexRef;
import org.opennms.features.topology.plugins.topo.simple.SimpleGraphBuilder;

public class VEProviderGraphContainerTest {

	private GraphProvider m_graphProvider;
	private EdgeProvider m_edgeProvider;
	private GraphContainer m_graphContainer;
	private Set<VertexRef> m_expectedVertices = new HashSet<VertexRef>();
	private Map<VertexRef, String> m_expectedVertexStyles = new HashMap<VertexRef, String>();
	private Set<EdgeRef> m_expectedEdges = new HashSet<EdgeRef>();
	private Map<EdgeRef, String> m_expectedEdgeStyles = new HashMap<EdgeRef, String>();

	private static class TestCollapsibleCriteria extends VertexHopCriteria implements CollapsibleCriteria {

		@Override
		public boolean isCollapsed() {
			return true;
		}

		@Override
		public void setCollapsed(boolean collapsed) {
		}

		@Override
		public Set<VertexRef> getVertices() {
			Set<VertexRef> retval = new HashSet<VertexRef>();
			retval.add(new AbstractVertexRef("nodes", "v2", "vertex2"));
			retval.add(new AbstractVertexRef("nodes", "v4", "vertex4"));
			return retval;
		}

		@Override
		public Vertex getCollapsedRepresentation() {
			AbstractVertex retval = new AbstractVertex("nodes", "test", "Collapsed vertex");
			retval.setStyleName("test");
			return retval;
		}

		@Override
		public String getNamespace() {
			return "nodes";
		}

		@Override
		public int hashCode() {
			return getLabel().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return getLabel().equals(obj);
		}
	}

	@Before
	public void setUp() {

		MockLogAppender.setupLogging();

		m_graphProvider = new SimpleGraphBuilder("nodes")
			.vertex("g0").vLabel("group0").vIconKey("group").vTooltip("root group").vStyleName("vertex")
			.vertex("g1").parent("g0").vLabel("group1").vIconKey("group").vTooltip("group 1").vStyleName("vertex")
			.vertex("v1").parent("g1").vLabel("vertex1").vIconKey("server").vTooltip("tooltip").vStyleName("vertex")
			.vertex("v2").parent("g1").vLabel("vertex2").vIconKey("server").vTooltip("tooltip").vStyleName("vertex")
			.vertex("g2").parent("g0").vLabel("group2").vIconKey("group").vTooltip("group 2").vStyleName("vertex")
			.vertex("v3").parent("g2").vLabel("vertex3").vIconKey("server").vTooltip("tooltip").vStyleName("vertex")
			.vertex("v4").parent("g2").vLabel("vertex4").vIconKey("server").vTooltip("tooltip").vStyleName("vertex")
			.edge("e1", "v1", "v2").eStyleName("edge")
			.edge("e2", "v2", "v3").eStyleName("edge")
			.edge("e3", "v3", "v4").eStyleName("edge")
			.edge("e4", "v4", "v1").eStyleName("edge")
			.get();

		m_edgeProvider = new SimpleEdgeBuilder("ncs", "nodes")
			.edge("ncs1", "nodes", "v1", "nodes", "v3").label("ncsedge1").styleName("ncs edge")
			.edge("ncs2", "nodes", "v2", "nodes", "v4").label("ncsedge2").styleName("ncs edge")
			.edge("ncs3", "nodes", "v1", "nodes", "v2").label("ncsedge3").styleName("ncs edge")
			.get();
		
		ProviderManager providerManager = new ProviderManager();
		providerManager.onEdgeProviderBind(m_edgeProvider);

		GraphContainer graphContainer = new VEProviderGraphContainer(m_graphProvider, providerManager);
		graphContainer.setSemanticZoomLevel(0);
		
		m_graphContainer = graphContainer;
	}
	
	@Test
	public void testGraphProvider() {
		List<? extends Vertex> roots = m_graphProvider.getRootGroup();
		assertEquals(1, roots.size());
		Vertex root = roots.get(0);
		assertNotNull(root);
		
		assertEquals("nodes", root.getNamespace());
		assertEquals("g0", root.getId());
		
		List<? extends Vertex> children = m_graphProvider.getChildren(root);
		assertEquals(2, children.size());
		assertEquals(root, m_graphProvider.getParent(children.get(0)));
	}

	@Test
	public void testContainerWithHopProvider() throws Exception {
		// Wrap the test GraphProvider in a VertexHopGraphProvider
		ProviderManager providerManager = new ProviderManager();
		providerManager.onEdgeProviderBind(m_edgeProvider);
		GraphContainer graphContainer = new VEProviderGraphContainer(new VertexHopGraphProvider(m_graphProvider), providerManager);
		graphContainer.setSemanticZoomLevel(0);

		m_graphContainer = graphContainer;

		// There should be zero vertices or edges if no focus vertices are set
		Graph graph = m_graphContainer.getGraph();
		assertEquals(0, graph.getDisplayVertices().size());
		assertEquals(0, graph.getDisplayEdges().size());

		// Add one focus vertex
		FocusNodeHopCriteria focusNodes = new FocusNodeHopCriteria();
		focusNodes.add(new AbstractVertexRef("nodes", "v1"));
		m_graphContainer.addCriteria(focusNodes);
		// This needs to be 2 because there is a SemanticZoomLevelCriteria in there also
		assertEquals(2, m_graphContainer.getCriteria().length);

		// Verify that a single vertex is in the graph
		graph = m_graphContainer.getGraph();
		assertEquals(1, graph.getDisplayVertices().size());
		assertEquals(0, graph.getDisplayEdges().size());

		expectVertex("nodes", "v1", "vertex");
		graph.visit(verifier());
		verify();
		verifyConnectedness(graph);
		reset();


		// Change SZL to 1
		m_graphContainer.setSemanticZoomLevel(1);

		// Focus vertex
		expectVertex("nodes", "v1", "vertex");

		expectVertex("nodes", "v2", "vertex");
		/*
			This is a problem with the VEProviderGraphContainer... it wraps a delegate GraphProvider
			in a MergingGraphProvider like so:

			VEProviderGraphContainer { MergingGraphProvider { VertexHopGraphProvider } } }

			But for the VertexHopProvider to calculate the SZL correctly, it needs to be aware of all
			edges, including those provided by the MergingGraphProvider. So we should rearrange things
			so that they are laid out like:

			VEProviderGraphContainer { VertexHopGraphProvider { MergingGraphProvider } } }

			We should decouple the MergingGraphProvider from the VEProviderGraphContainer and then just
			inject them in the correct order. When this problem is fixed, uncomment all of the lines that
			are commented out in this test.
		*/
		//expectVertex("nodes", "v3", "vertex");
		expectVertex("nodes", "v4", "vertex");
		
		expectEdge("nodes", "e1", "edge");
		//expectEdge("nodes", "e2", "edge");
		//expectEdge("nodes", "e3", "edge");
		expectEdge("nodes", "e4", "edge");
		//expectEdge("ncs", "ncs1", "ncs edge");
		expectEdge("ncs", "ncs2", "ncs edge");
		expectEdge("ncs", "ncs3", "ncs edge");
		
		graph = m_graphContainer.getGraph();
		assertEquals(3, graph.getDisplayVertices().size());
		assertEquals(4, graph.getDisplayEdges().size());

		graph.visit(verifier());
		verify();
		verifyConnectedness(graph);
		reset();


		// Add a collapsed criteria to the container
		m_graphContainer.addCriteria(new TestCollapsibleCriteria());
		assertEquals(3, m_graphContainer.getCriteria().length);

		assertEquals(3, m_graphContainer.getGraph().getDisplayVertices().size());
		assertEquals(3, m_graphContainer.getBaseTopology().getVertices(new TestCollapsibleCriteria()).size());

		/**
		 * TODO The set of edges returned here is problematic. See SPC-787 and SPC-791.
		 */
		expectVertex("nodes", "v1", "vertex");
		// WTF why is this here
		expectVertex("nodes", "v3", "vertex");
		expectVertex("nodes", "test", "test");
		expectEdge("ncs", "ncs1", "ncs edge");
		// WTF why is this not here
		//expectEdge("nodes", "e1", "edge");
		// WTF why is this here
		//expectEdge("nodes", "e2", "edge");
		// WTF why is this here
		//expectEdge("nodes", "e3", "edge");
		// WTF why is this not here
		//expectEdge("nodes", "e4", "edge");

		graph = m_graphContainer.getGraph();

		assertEquals(3, graph.getDisplayVertices().size());
		assertEquals(1, graph.getDisplayEdges().size());

		for (Edge edge : graph.getDisplayEdges()) {
			if (edge.getId().equals("e1")) {
				assertEquals("v1", edge.getSource().getVertex().getId());
				assertEquals("test", edge.getTarget().getVertex().getId());
			} else if (edge.getId().equals("e2")) {
				assertEquals("test", edge.getSource().getVertex().getId());
				assertEquals("v3", edge.getTarget().getVertex().getId());
			} else if (edge.getId().equals("e3")) {
				assertEquals("v3", edge.getSource().getVertex().getId());
				assertEquals("test", edge.getTarget().getVertex().getId());
			} else if (edge.getId().equals("e4")) {
				assertEquals("test", edge.getSource().getVertex().getId());
				assertEquals("v1", edge.getTarget().getVertex().getId());
			}
		}

		graph.visit(verifier());
		verify();
		verifyConnectedness(graph);
		reset();
	}

	@Test
	public void testContainer() throws Exception {
			
		Graph graph = m_graphContainer.getGraph();
	
		expectVertex("nodes", "g0", "vertex");
		
		graph.visit(verifier());
		
		verify();
		verifyConnectedness(graph);
		
		reset();
		
		m_graphContainer.setSemanticZoomLevel(1);
		
		expectVertex("nodes", "g1", "vertex");
		expectVertex("nodes", "g2", "vertex");
		expectEdge("pseudo-nodes", "<nodes:g1>-<nodes:g2>", "edge");
		expectEdge("pseudo-ncs", "<nodes:g1>-<nodes:g2>", "ncs edge");
		
		graph = m_graphContainer.getGraph();
		
		graph.visit(verifier());
		
		verify();
		verifyConnectedness(graph);
		
		reset();
		
		m_graphContainer.addCriteria(SimpleEdgeProvider.labelMatches("ncs", "ncsedge."));
		
		expectVertex("nodes", "g1", "vertex");
		expectVertex("nodes", "g2", "vertex");
		expectEdge("pseudo-nodes", "<nodes:g1>-<nodes:g2>", "edge");
		expectEdge("pseudo-ncs", "<nodes:g1>-<nodes:g2>", "ncs edge");

		graph = m_graphContainer.getGraph();
		
		graph.visit(verifier());
		
		verify();
		verifyConnectedness(graph);
		
		reset();

	}

	private void verify() {
		if (!m_expectedVertices.isEmpty()) {
			fail("Expected Vertices not seen: " + m_expectedVertices);
		}
		
		if (!m_expectedEdges.isEmpty()) {
			fail("Expected Edges not seen: " + m_expectedEdges);
		}
	}
	
	private GraphVisitor verifier() {
		return new BaseGraphVisitor() {
			
			@Override
			public void visitVertex(Vertex vertex) {
				assertTrue("Unexpected vertex " + vertex + " encountered!", m_expectedVertices.contains(vertex));
				m_expectedVertices.remove(vertex);
				assertEquals("Unexpected style for vertex " + vertex, m_expectedVertexStyles.get(vertex), vertex.getStyleName());
			}
			
			@Override
			public void visitEdge(Edge edge) {
				assertTrue("Unexpected edge " + edge + " encountered!", m_expectedEdges.contains(edge));
				m_expectedEdges.remove(edge);
				assertEquals("Unexpected style for edge " + edge, m_expectedEdgeStyles.get(edge), edge.getStyleName());
			}
			
		};
	}
	
	

	private void expectVertex(String namespace, String vertexId, String styles) {
		AbstractVertexRef vertexRef = new AbstractVertexRef(namespace, vertexId);
		m_expectedVertices.add(vertexRef);
		m_expectedVertexStyles.put(vertexRef, styles);
	}
	
	private void expectEdge(String namespace, String edgeId, String styles) {
		AbstractEdgeRef edgeRef = new AbstractEdgeRef(namespace, edgeId);
		m_expectedEdges.add(edgeRef);
		m_expectedEdgeStyles.put(edgeRef, styles);
	}
	
	private static void verifyConnectedness(Graph graph) {
		Collection<Vertex> vertices = graph.getDisplayVertices();
		for (Edge edge : graph.getDisplayEdges()) {
			assertTrue(vertices.contains(edge.getSource().getVertex()));
			assertTrue(vertices.contains(edge.getTarget().getVertex()));
		}
	}
	
	private void reset() {
		m_expectedVertices.clear();
		m_expectedEdges.clear();
		m_expectedVertexStyles.clear();
		m_expectedEdgeStyles.clear();
	}
}
