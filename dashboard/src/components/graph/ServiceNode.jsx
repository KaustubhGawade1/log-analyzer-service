import { Handle, Position } from 'reactflow';

export default function ServiceNode({ data, selected }) {
    const healthClass = data.health?.toLowerCase() || 'healthy';
    const latencyMs = data.avgLatency || 0;
    const errorRate = data.errorRate || 0;

    const getLatencyClass = () => {
        if (latencyMs > 1000) return 'bad';
        if (latencyMs > 500) return 'warning';
        return 'good';
    };

    const getErrorRateClass = () => {
        if (errorRate > 0.05) return 'bad';
        if (errorRate > 0.01) return 'warning';
        return 'good';
    };

    const getIcon = () => {
        switch (data.type) {
            case 'ENTRY':
                return '🚀';
            case 'DATABASE':
                return '🗄️';
            case 'EXTERNAL':
                return '🌐';
            case 'MESSAGING':
                return '📨';
            case 'CACHE':
                return '⚡';
            default:
                return '📦';
        }
    };

    return (
        <>
            <Handle type="target" position={Position.Left} />
            <div className={`service-node ${healthClass} ${data.selected ? 'selected' : ''}`}>
                <div className="node-header">
                    <span className="node-icon">{getIcon()}</span>
                    <span className="node-title" title={data.label}>
                        {data.label}
                    </span>
                    {data.type && (
                        <span className={`node-type-badge ${data.type.toLowerCase()}`}>
                            {data.type}
                        </span>
                    )}
                </div>

                {data.endpoint && (
                    <div className="node-endpoint" title={data.endpoint}>
                        {data.method && <span style={{ color: '#60a5fa' }}>{data.method} </span>}
                        {data.endpoint}
                    </div>
                )}

                <div className="node-metrics">
                    <span className="metric">
                        <span>⏱</span>
                        <span className={`metric-value ${getLatencyClass()}`}>
                            {latencyMs < 1000 ? `${latencyMs}ms` : `${(latencyMs / 1000).toFixed(1)}s`}
                        </span>
                    </span>
                    <span className="metric">
                        <span>❌</span>
                        <span className={`metric-value ${getErrorRateClass()}`}>
                            {(errorRate * 100).toFixed(1)}%
                        </span>
                    </span>
                    {data.requestCount && (
                        <span className="metric">
                            <span>📊</span>
                            <span className="metric-value">{data.requestCount}</span>
                        </span>
                    )}
                </div>
            </div>
            <Handle type="source" position={Position.Right} />
        </>
    );
}
