import client from './client';

export type SearchSourceType = 'CHALLENGE' | 'FEEDBACK' | 'QUESTION';

export interface SearchHit {
  source_type: SearchSourceType | string;
  source_id: number;
  title: string | null;
  snippet: string;
  score: number;
}

export interface SearchResponse {
  query: string;
  results: SearchHit[];
}

export interface KpiResponse {
  question: string;
  tool: string | null;
  value: unknown;
  explanation: string | null;
}

export interface Citation {
  source_type: string;
  source_id: number;
  snippet: string;
  score: number | null;
}

export interface AssistantResponse {
  answer: string;
  citations: Citation[];
}

export async function semanticSearch(input: {
  query: string;
  top_k?: number;
  source_type?: string | null;
  tag?: string | null;
}): Promise<SearchResponse> {
  const { data } = await client.post<SearchResponse>('/search', {
    query: input.query,
    top_k: input.top_k ?? 10,
    filters: {
      source_type: input.source_type || null,
      tag: input.tag || null,
    },
  });
  return data;
}

export async function askKpi(question: string): Promise<KpiResponse> {
  const { data } = await client.post<KpiResponse>('/kpi', { question });
  return data;
}

export async function askAssistant(question: string): Promise<AssistantResponse> {
  const { data } = await client.post<AssistantResponse>('/assistant', { question });
  return data;
}

export interface KnowledgeDocument {
  id: number;
  title: string;
  body: string;
  category: string;
  tags: string | null;
  active: boolean;
}

export async function listKnowledgeDocuments(): Promise<KnowledgeDocument[]> {
  const { data } = await client.get<KnowledgeDocument[]>('/knowledge/documents');
  return data;
}

export async function createKnowledgeDocument(input: {
  title: string;
  body: string;
  category?: string;
  tags?: string;
}): Promise<KnowledgeDocument> {
  const { data } = await client.post<KnowledgeDocument>('/knowledge/documents', input);
  return data;
}

export async function deleteKnowledgeDocument(id: number): Promise<void> {
  await client.delete(`/knowledge/documents/${id}`);
}

export async function triggerIngestionSync(full = false): Promise<Record<string, unknown>> {
  const { data } = await client.post('/ingestion/sync', { full });
  return data;
}

export async function getIngestionStatus(): Promise<Record<string, unknown>> {
  const { data } = await client.get('/ingestion/status');
  return data;
}
