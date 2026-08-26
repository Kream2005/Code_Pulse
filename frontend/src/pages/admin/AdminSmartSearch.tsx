import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { BarChart3, BookOpen, MessageSquare, RefreshCw, Search } from 'lucide-react';
import Card from '../../components/Card';
import ErrorBanner from '../../components/ErrorBanner';
import PageHeader from '../../components/PageHeader';
import {
  askAssistant,
  askKpi,
  createKnowledgeDocument,
  deleteKnowledgeDocument,
  getIngestionStatus,
  listKnowledgeDocuments,
  semanticSearch,
  triggerIngestionSync,
  type AssistantResponse,
  type KnowledgeDocument,
  type KpiResponse,
  type SearchHit,
} from '../../api/searchApi';
import { useI18n } from '../../i18n/I18nContext';

type Tab = 'search' | 'kpi' | 'assistant' | 'knowledge';

function linkForHit(hit: SearchHit): string | null {
  if (hit.source_type === 'FEEDBACK') return `/admin/feedbacks/${hit.source_id}`;
  if (hit.source_type === 'CHALLENGE') return '/admin/challenges';
  if (hit.source_type === 'QUESTION') return '/admin/questions';
  return null;
}

function formatKpiValue(value: unknown): string {
  if (value == null) return '—';
  if (typeof value === 'number') return String(value);
  if (typeof value === 'string') return value;
  return JSON.stringify(value, null, 2);
}

export default function AdminSmartSearch() {
  const { t } = useI18n();
  const [tab, setTab] = useState<Tab>('search');
  const [query, setQuery] = useState('');
  const [sourceType, setSourceType] = useState('');
  const [tag, setTag] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hits, setHits] = useState<SearchHit[]>([]);
  const [searched, setSearched] = useState(false);
  const [kpi, setKpi] = useState<KpiResponse | null>(null);
  const [assistant, setAssistant] = useState<AssistantResponse | null>(null);
  const [docs, setDocs] = useState<KnowledgeDocument[]>([]);
  const [docTitle, setDocTitle] = useState('');
  const [docBody, setDocBody] = useState('');
  const [docTags, setDocTags] = useState('capgemini,company');
  const [syncInfo, setSyncInfo] = useState<string | null>(null);
  const [learnerInfo, setLearnerInfo] = useState<string | null>(null);

  const placeholder = useMemo(() => {
    if (tab === 'kpi') return t('smart.kpiPlaceholder');
    if (tab === 'assistant') return t('smart.assistantPlaceholder');
    return t('smart.searchPlaceholder');
  }, [tab, t]);

  async function refreshDocs() {
    const rows = await listKnowledgeDocuments();
    setDocs(rows.filter((d) => d.active));
  }

  async function refreshLearner() {
    try {
      const status = await getIngestionStatus();
      const enabled = status.enabled ? t('smart.learnerOn') : t('smart.learnerOff');
      const msg =
        typeof status.last_result === 'object' && status.last_result
          ? String((status.last_result as { message?: string }).message || '')
          : '';
      setLearnerInfo(`${enabled}${msg ? ` — ${msg}` : ''}`);
    } catch {
      setLearnerInfo(null);
    }
  }

  useEffect(() => {
    if (tab === 'knowledge') {
      refreshDocs().catch(() => setDocs([]));
      refreshLearner().catch(() => undefined);
    }
  }, [tab]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (tab === 'knowledge') return;
    const q = query.trim();
    if (!q) return;
    setLoading(true);
    setError(null);
    try {
      if (tab === 'search') {
        const res = await semanticSearch({
          query: q,
          top_k: 10,
          source_type: sourceType || null,
          tag: tag || null,
        });
        setHits(res.results);
        setSearched(true);
        setKpi(null);
        setAssistant(null);
      } else if (tab === 'kpi') {
        const res = await askKpi(q);
        setKpi(res);
        setHits([]);
        setSearched(false);
        setAssistant(null);
      } else {
        const res = await askAssistant(q);
        setAssistant(res);
        setHits([]);
        setSearched(false);
        setKpi(null);
      }
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail ||
        t('common.errorGeneric');
      setError(typeof msg === 'string' ? msg : t('common.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  async function addDocument(e: React.FormEvent) {
    e.preventDefault();
    if (!docTitle.trim() || !docBody.trim()) return;
    setLoading(true);
    setError(null);
    try {
      await createKnowledgeDocument({
        title: docTitle.trim(),
        body: docBody.trim(),
        category: 'company',
        tags: docTags.trim() || undefined,
      });
      setDocTitle('');
      setDocBody('');
      await refreshDocs();
      setSyncInfo(t('smart.docAdded'));
    } catch {
      setError(t('common.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  async function removeDoc(id: number) {
    setLoading(true);
    setError(null);
    try {
      await deleteKnowledgeDocument(id);
      await refreshDocs();
    } catch {
      setError(t('common.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  async function runSync(full: boolean) {
    setLoading(true);
    setError(null);
    try {
      const result = await triggerIngestionSync(full);
      setSyncInfo(String(result.message || t('smart.syncDone')));
      await refreshLearner();
    } catch {
      setError(t('common.errorGeneric'));
    } finally {
      setLoading(false);
    }
  }

  const tabs: { id: Tab; label: string; icon: React.ReactNode }[] = [
    { id: 'search', label: t('smart.tabSearch'), icon: <Search className="h-4 w-4" /> },
    { id: 'kpi', label: t('smart.tabKpi'), icon: <BarChart3 className="h-4 w-4" /> },
    {
      id: 'assistant',
      label: t('smart.tabAssistant'),
      icon: <MessageSquare className="h-4 w-4" />,
    },
    {
      id: 'knowledge',
      label: t('smart.tabKnowledge'),
      icon: <BookOpen className="h-4 w-4" />,
    },
  ];

  return (
    <div>
      <PageHeader title={t('smart.title')} subtitle={t('smart.subtitle')} />

      <div className="mb-4 flex flex-wrap gap-2">
        {tabs.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => {
              setTab(item.id);
              setError(null);
            }}
            className={`inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ${
              tab === item.id
                ? 'bg-brand text-white'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700'
            }`}
          >
            {item.icon}
            {item.label}
          </button>
        ))}
      </div>

      {tab !== 'knowledge' && (
        <Card padded className="mb-4">
          <form onSubmit={submit} className="space-y-3">
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              {tab === 'search' ? t('smart.queryLabel') : t('smart.questionLabel')}
              <textarea
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                rows={tab === 'search' ? 2 : 3}
                placeholder={placeholder}
                className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 outline-none ring-brand focus:ring-2 dark:border-slate-600 dark:bg-slate-950 dark:text-slate-100"
              />
            </label>

            {tab === 'search' && (
              <div className="grid gap-3 sm:grid-cols-2">
                <label className="block text-sm text-slate-600 dark:text-slate-300">
                  {t('smart.sourceType')}
                  <select
                    value={sourceType}
                    onChange={(e) => setSourceType(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-950"
                  >
                    <option value="">{t('common.allTypes')}</option>
                    <option value="CHALLENGE">CHALLENGE</option>
                    <option value="FEEDBACK">FEEDBACK</option>
                    <option value="QUESTION">QUESTION</option>
                    <option value="DOCUMENT">DOCUMENT</option>
                  </select>
                </label>
                <label className="block text-sm text-slate-600 dark:text-slate-300">
                  {t('smart.tag')}
                  <input
                    value={tag}
                    onChange={(e) => setTag(e.target.value)}
                    placeholder="arrays, trees…"
                    className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-950"
                  />
                </label>
              </div>
            )}

            <button
              type="submit"
              disabled={loading || !query.trim()}
              className="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
            >
              {loading ? t('common.loading') : t('smart.submit')}
            </button>
          </form>
        </Card>
      )}

      {tab === 'knowledge' && (
        <div className="mb-4 space-y-4">
          <Card padded>
            <div className="mb-3 flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => runSync(false)}
                disabled={loading}
                className="inline-flex items-center gap-2 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                <RefreshCw className="h-4 w-4" />
                {t('smart.syncNow')}
              </button>
              <button
                type="button"
                onClick={() => runSync(true)}
                disabled={loading}
                className="rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600"
              >
                {t('smart.fullReindex')}
              </button>
            </div>
            {learnerInfo && (
              <p className="text-xs text-slate-500 dark:text-slate-400">{learnerInfo}</p>
            )}
            {syncInfo && <p className="mt-1 text-xs text-emerald-600">{syncInfo}</p>}
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">{t('smart.knowledgeHint')}</p>
          </Card>

          <Card padded>
            <h3 className="mb-3 text-sm font-semibold text-slate-800 dark:text-slate-100">
              {t('smart.addDocument')}
            </h3>
            <form onSubmit={addDocument} className="space-y-3">
              <input
                value={docTitle}
                onChange={(e) => setDocTitle(e.target.value)}
                placeholder={t('smart.docTitle')}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-950"
              />
              <textarea
                value={docBody}
                onChange={(e) => setDocBody(e.target.value)}
                rows={8}
                placeholder={t('smart.docBody')}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-950"
              />
              <input
                value={docTags}
                onChange={(e) => setDocTags(e.target.value)}
                placeholder={t('smart.docTags')}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-950"
              />
              <button
                type="submit"
                disabled={loading || !docTitle.trim() || !docBody.trim()}
                className="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                {t('smart.saveDocument')}
              </button>
            </form>
          </Card>

          <div className="space-y-3">
            {docs.map((doc) => (
              <Card key={doc.id} padded>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-wide text-brand">
                      DOCUMENT #{doc.id} · {doc.category}
                    </p>
                    <h3 className="mt-1 font-semibold text-slate-800 dark:text-slate-100">
                      {doc.title}
                    </h3>
                    <p className="mt-2 line-clamp-4 text-sm text-slate-600 dark:text-slate-300">
                      {doc.body}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => removeDoc(doc.id)}
                    className="text-sm text-red-600 hover:underline"
                  >
                    {t('common.delete')}
                  </button>
                </div>
              </Card>
            ))}
            {docs.length === 0 && (
              <p className="text-sm text-slate-500">{t('smart.noDocuments')}</p>
            )}
          </div>
        </div>
      )}

      {error && <ErrorBanner message={error} />}

      {tab === 'search' && (
        <div className="space-y-3">
          {searched && hits.length === 0 && !loading && (
            <p className="text-sm text-slate-500">{t('common.noResults')}</p>
          )}
          {hits.map((hit) => {
            const href = linkForHit(hit);
            return (
              <Card key={`${hit.source_type}-${hit.source_id}-${hit.score}`} padded>
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-wide text-brand">
                      {hit.source_type} #{hit.source_id}
                    </p>
                    <h3 className="mt-1 text-base font-semibold text-slate-800 dark:text-slate-100">
                      {hit.title || t('smart.untitled')}
                    </h3>
                  </div>
                  <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                    score {hit.score.toFixed(4)}
                  </span>
                </div>
                <p className="mt-2 text-sm leading-relaxed text-slate-600 dark:text-slate-300">
                  {hit.snippet}
                </p>
                {href && (
                  <Link
                    to={href}
                    className="mt-3 inline-block text-sm font-medium text-brand hover:underline"
                  >
                    {t('smart.openSource')}
                  </Link>
                )}
              </Card>
            );
          })}
        </div>
      )}

      {tab === 'kpi' && kpi && (
        <Card padded>
          <p className="text-xs font-semibold uppercase tracking-wide text-brand">
            {kpi.tool || t('smart.noTool')}
          </p>
          <p className="mt-2 text-sm text-slate-500">{kpi.explanation}</p>
          <pre className="mt-4 overflow-x-auto rounded-lg bg-slate-50 p-3 text-sm text-slate-800 dark:bg-slate-950 dark:text-slate-100">
            {formatKpiValue(kpi.value)}
          </pre>
        </Card>
      )}

      {tab === 'assistant' && assistant && (
        <div className="space-y-3">
          <Card padded>
            <h3 className="text-sm font-semibold text-slate-800 dark:text-slate-100">
              {t('smart.answer')}
            </h3>
            <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-slate-700 dark:text-slate-200">
              {assistant.answer}
            </p>
          </Card>
          {assistant.citations.map((c) => (
            <Card key={`${c.source_type}-${c.source_id}-${c.snippet.slice(0, 24)}`} padded>
              <p className="text-xs font-semibold uppercase tracking-wide text-brand">
                {c.source_type} #{c.source_id}
              </p>
              <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">{c.snippet}</p>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
