import { BaseEdge, EdgeLabelRenderer, getBezierPath } from 'reactflow';

export default function FlowEdge({
    id,
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition,
    targetPosition,
    data,
    style,
    markerEnd,
}) {
    const [edgePath, labelX, labelY] = getBezierPath({
        sourceX,
        sourceY,
        sourcePosition,
        targetX,
        targetY,
        targetPosition,
    });

    const metrics = data?.metrics || {};
    const latencyMs = metrics.avgLatency || 0;
    const errorRate = metrics.errorRate || 0;
    const status = data?.status || 'NORMAL';

    const getStatusClass = () => {
        if (status === 'FAILING' || status === 'TIMEOUT') return 'failing';
        if (status === 'SLOW') return 'slow';
        return '';
    };

    const formatLatency = (ms) => {
        if (ms < 1000) return `${ms}ms`;
        return `${(ms / 1000).toFixed(1)}s`;
    };

    return (
        <>
            <BaseEdge path={edgePath} markerEnd={markerEnd} style={style} />
            <EdgeLabelRenderer>
                <div
                    className={`flow-edge-label ${getStatusClass()}`}
                    style={{
                        position: 'absolute',
                        transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
                        pointerEvents: 'all',
                    }}
                >
                    <span style={{ marginRight: '8px' }}>
                        ⏱ {formatLatency(latencyMs)}
                    </span>
                    {errorRate > 0 && (
                        <span style={{ color: errorRate > 0.05 ? '#ef4444' : '#f59e0b' }}>
                            ❌ {(errorRate * 100).toFixed(1)}%
                        </span>
                    )}
                </div>
            </EdgeLabelRenderer>
        </>
    );
}
