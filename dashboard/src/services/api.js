const API_BASE = 'http://localhost:8080/api';

export async function fetchIncidents(filters = {}) {
    const params = new URLSearchParams();
    if (filters.status) params.append('status', filters.status);
    if (filters.serviceName) params.append('serviceName', filters.serviceName);
    if (filters.type) params.append('type', filters.type);

    const response = await fetch(`${API_BASE}/incidents?${params}`);
    if (!response.ok) throw new Error('Failed to fetch incidents');
    return response.json();
}

export async function fetchIncident(id) {
    const response = await fetch(`${API_BASE}/incidents/${id}`);
    if (!response.ok) throw new Error('Failed to fetch incident');
    return response.json();
}

export async function fetchIncidentStats() {
    const response = await fetch(`${API_BASE}/incidents/stats`);
    if (!response.ok) throw new Error('Failed to fetch stats');
    return response.json();
}

export async function analyzeIncident(id) {
    const response = await fetch(`${API_BASE}/incidents/${id}/analyze`, {
        method: 'POST'
    });
    if (!response.ok) throw new Error('Failed to analyze incident');
    return response.json();
}

export async function fetchLogs(filters = {}) {
    const params = new URLSearchParams();
    if (filters.level) params.append('level', filters.level);
    if (filters.serviceName) params.append('serviceName', filters.serviceName);
    if (filters.search) params.append('search', filters.search);
    if (filters.limit) params.append('limit', filters.limit);

    const response = await fetch(`${API_BASE}/logs?${params}`);
    if (!response.ok) throw new Error('Failed to fetch logs');
    return response.json();
}

export async function fetchClusters() {
    const response = await fetch(`${API_BASE}/logs/clusters`);
    if (!response.ok) throw new Error('Failed to fetch clusters');
    return response.json();
}

export async function fetchTimeline(minutes = 60) {
    const response = await fetch(`${API_BASE}/logs/timeline?minutes=${minutes}`);
    if (!response.ok) throw new Error('Failed to fetch timeline');
    return response.json();
}

export async function fetchServices() {
    const response = await fetch(`${API_BASE}/services`);
    if (!response.ok) throw new Error('Failed to fetch services');
    return response.json();
}
