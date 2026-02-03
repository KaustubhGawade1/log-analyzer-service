import { useCallback, useEffect, useState, useMemo } from 'react';
import ReactFlow, {
    Background,
    Controls,
    MiniMap,
    useNodesState,
    useEdgesState,
    MarkerType,
} from 'reactflow';
import ServiceNode from './ServiceNode';
import FlowEdge from './FlowEdge';
import 'reactflow/dist/style.css';
import './FlowGraph.css';

// Custom node types
const nodeTypes = {
    service: ServiceNode,
};

// Custom edge types
const edgeTypes = {
    flow: FlowEdge,
};

// Simple hierarchical layout (replaces dagre to avoid ESM issues)
const getLayoutedElements = (nodes, edges) => {
    if (!nodes.length) return { nodes: [], edges };

    const NODE_WIDTH = 200;
    const NODE_HEIGHT = 80;
    const H_SPACING = 250;
    const V_SPACING = 120;

    // Build adjacency map
    const children = new Map();
    const parents = new Map();

    nodes.forEach(node => {
        children.set(node.id, []);
        parents.set(node.id, []);
    });

    edges.forEach(edge => {
        const sourceChildren = children.get(edge.source);
        if (sourceChildren) sourceChildren.push(edge.target);
        const targetParents = parents.get(edge.target);
        if (targetParents) targetParents.push(edge.source);
    });

    // Find root nodes (no parents)
    const roots = nodes.filter(n => parents.get(n.id)?.length === 0);
    if (roots.length === 0 && nodes.length > 0) {
        roots.push(nodes[0]); // Fallback to first node
    }

    // BFS to assign levels
    const levels = new Map();
    const visited = new Set();
    const queue = roots.map(r => ({ id: r.id, level: 0 }));

    while (queue.length > 0) {
        const { id, level } = queue.shift();
        if (visited.has(id)) continue;
        visited.add(id);
        levels.set(id, level);

        const nodeChildren = children.get(id) || [];
        nodeChildren.forEach(childId => {
            if (!visited.has(childId)) {
                queue.push({ id: childId, level: level + 1 });
            }
        });
    }

    // Handle unvisited nodes (disconnected)
    nodes.forEach(node => {
        if (!levels.has(node.id)) {
            levels.set(node.id, 0);
        }
    });

    // Group by level
    const levelGroups = new Map();
    nodes.forEach(node => {
        const level = levels.get(node.id);
        if (!levelGroups.has(level)) {
            levelGroups.set(level, []);
        }
        levelGroups.get(level).push(node);
    });

    // Position nodes
    const layoutedNodes = nodes.map(node => {
        const level = levels.get(node.id);
        const group = levelGroups.get(level);
        const indexInGroup = group.indexOf(node);
        const groupSize = group.length;

        return {
            ...node,
            position: {
                x: level * H_SPACING,
                y: (indexInGroup - (groupSize - 1) / 2) * V_SPACING + 200,
            },
        };
    });

    return { nodes: layoutedNodes, edges };
};

export default function FlowGraph({
    graphData,
    onNodeClick,
    onEdgeClick,
    selectedNodeId,
    selectedEdgeId
}) {
    const [nodes, setNodes, onNodesChange] = useNodesState([]);
    const [edges, setEdges, onEdgesChange] = useEdgesState([]);

    // Convert API data to React Flow format
    useEffect(() => {
        if (!graphData) return;

        // Convert nodes
        const flowNodes = graphData.nodes.map((node) => ({
            id: node.id,
            type: 'service',
            data: {
                label: node.serviceName,
                endpoint: node.endpoint,
                method: node.method,
                type: node.type,
                health: node.health,
                avgLatency: node.avgLatency,
                errorRate: node.errorRate,
                requestCount: node.requestCount,
                selected: node.id === selectedNodeId,
            },
            position: { x: 0, y: 0 },
        }));

        // Convert edges
        const flowEdges = graphData.edges.map((edge) => ({
            id: edge.id,
            source: edge.sourceNodeId,
            target: edge.targetNodeId,
            type: 'flow',
            data: {
                metrics: edge.metrics,
                status: edge.status,
                protocol: edge.protocol,
                selected: edge.id === selectedEdgeId,
            },
            markerEnd: {
                type: MarkerType.ArrowClosed,
                color: getEdgeColor(edge.status),
            },
            style: {
                stroke: getEdgeColor(edge.status),
                strokeWidth: 2,
            },
            animated: edge.status === 'SLOW' || edge.status === 'FAILING',
        }));

        // Apply simple layout
        const { nodes: layoutedNodes, edges: layoutedEdges } = getLayoutedElements(
            flowNodes,
            flowEdges
        );

        setNodes(layoutedNodes);
        setEdges(layoutedEdges);
    }, [graphData, selectedNodeId, selectedEdgeId, setNodes, setEdges]);

    const handleNodeClick = useCallback(
        (event, node) => {
            onNodeClick?.(node);
        },
        [onNodeClick]
    );

    const handleEdgeClick = useCallback(
        (event, edge) => {
            onEdgeClick?.(edge);
        },
        [onEdgeClick]
    );

    return (
        <div className="flow-graph-container">
            <ReactFlow
                nodes={nodes}
                edges={edges}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onNodeClick={handleNodeClick}
                onEdgeClick={handleEdgeClick}
                nodeTypes={nodeTypes}
                edgeTypes={edgeTypes}
                fitView
                fitViewOptions={{ padding: 0.2 }}
                minZoom={0.1}
                maxZoom={2}
            >
                <Background color="#334155" gap={16} />
                <Controls />
                <MiniMap
                    nodeColor={(node) => getNodeColor(node.data?.health)}
                    maskColor="rgba(0, 0, 0, 0.8)"
                />
            </ReactFlow>
        </div>
    );
}

function getEdgeColor(status) {
    switch (status) {
        case 'FAILING':
            return '#ef4444'; // Red
        case 'SLOW':
            return '#f59e0b'; // Orange
        case 'TIMEOUT':
            return '#dc2626'; // Dark red
        default:
            return '#10b981'; // Green
    }
}

function getNodeColor(health) {
    switch (health) {
        case 'FAILING':
            return '#ef4444';
        case 'DEGRADED':
            return '#f59e0b';
        default:
            return '#10b981';
    }
}
