import { useEffect, useMemo, useState } from 'react';
import Card from '../../components/Card';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import SearchInput from '../../components/SearchInput';
import StatCard from '../../components/StatCard';
import Table from '../../components/Table';
import {
  getLowestScoringTags,
  getManagerDashboardKpis,
  getScoresByTags,
  getTagStatisticsPage,
} from '../../api/resources';
import type { AverageScoreByTag, ManagerDashboardKpi, TagStatistics } from '../../api/types';
import { useI18n } from '../../i18n/I18nContext';

function fmtScore(n: number | undefined) {
  if (n == null || Number.isNaN(n)) return '—';
  return (Math.round(n * 10) / 10).toFixed(1);
}

function fmtPct(n: number | undefined) {
  if (n == null || Number.isNaN(n)) return '—';
  return `${Math.round(n * 10) / 10}%`;
}

export default function AdminAnalytics() {
  const { t } = useI18n();
  const [kpi, setKpi] = useState<ManagerDashboardKpi | null>(null);
  const [tags, setTags] = useState<AverageScoreByTag[]>([]);
  const [watchTags, setWatchTags] = useState<TagStatistics[]>([]);
  const [stats, setStats] = useState<TagStatistics[]>([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [tagPage, setTagPage] = useState(1);
  const [tagSize, setTagSize] = useState(10);
  const [tagSearch, setTagSearch] = useState('');
  const [statsSearch, setStatsSearch] = useState('');

  useEffect(() => {
    getManagerDashboardKpis().then(setKpi).catch(() => setKpi(null));
    getScoresByTags().then(setTags).catch(() => undefined);
    getLowestScoringTags(5).then(setWatchTags).catch(() => undefined);
  }, []);

  useEffect(() => {
    getTagStatisticsPage(page, size, statsSearch || undefined)
      .then((data) => {
        setStats(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch(() => {
        setStats([]);
        setTotalPages(0);
        setTotalElements(0);
      });
  }, [page, size, statsSearch]);

  function onStatsSearchChange(value: string) {
    setPage(1);
    setStatsSearch(value);
  }

  function onTagSearchChange(value: string) {
    setTagPage(1);
    setTagSearch(value);
  }

  const filteredTags = useMemo(() => {
    const needle = tagSearch.trim().toLowerCase();
    if (!needle) return tags;
    return tags.filter((row) => row.tag.toLowerCase().includes(needle));
  }, [tags, tagSearch]);

  const tagTotalPages = Math.max(1, Math.ceil(filteredTags.length / tagSize) || 1);
  const pagedTags = useMemo(() => {
    const start = (tagPage - 1) * tagSize;
    return filteredTags.slice(start, start + tagSize);
  }, [filteredTags, tagPage, tagSize]);

  useEffect(() => {
    if (tagPage > 1 && tagPage > tagTotalPages) {
      setTagPage(Math.max(1, tagTotalPages));
    }
  }, [tagPage, tagTotalPages]);

  return (
    <div>
      <PageHeader title={t('admin.analyticsTitle')} subtitle={t('admin.analyticsSubtitle')} />
      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <StatCard
          label={t('admin.participation')}
          value={fmtPct(kpi?.tauxParticipation)}
          accent="text-brand"
        />
        <StatCard label={t('admin.challengesCount')} value={kpi?.challengesTotal ?? '—'} />
        <StatCard
          label={t('admin.submitted')}
          value={kpi?.feedbacksSoumis ?? '—'}
          accent="text-emerald-600"
        />
        <StatCard label={t('admin.kpiAvgScore')} value={fmtScore(kpi?.noteMoyenneGlobale)} />
        <StatCard
          label={t('admin.kpiPending')}
          value={kpi?.notificationsEnAttente ?? '—'}
          accent="text-amber-600"
        />
        <StatCard label={t('admin.kpiTags')} value={kpi?.tagsCouverts ?? '—'} />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <div className="border-b border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-800 dark:border-slate-700 dark:text-slate-100">
            {t('admin.scoresByTag')}
          </div>
          <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-4 py-2.5 dark:border-slate-700">
            <SearchInput value={tagSearch} onChange={onTagSearchChange} className="max-w-xs" />
          </div>
          <Table
            columns={[t('admin.tag'), t('admin.average')]}
            isEmpty={pagedTags.length === 0}
            emptyLabel={tagSearch ? t('common.noResults') : undefined}
          >
            {pagedTags.map((row) => (
              <tr key={row.tag}>
                <td className="px-4 py-2.5">{row.tag}</td>
                <td className="px-4 py-2.5">{fmtScore(row.averageScore)}</td>
              </tr>
            ))}
          </Table>
          <Pagination
            page={tagPage}
            totalPages={tagTotalPages}
            totalElements={filteredTags.length}
            size={tagSize}
            onPageChange={setTagPage}
            onSizeChange={(s) => {
              setTagPage(1);
              setTagSize(s);
            }}
          />
        </Card>
        <Card>
          <div className="border-b border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-800 dark:border-slate-700 dark:text-slate-100">
            {t('admin.watchTags')}
          </div>
          <Table
            columns={[t('admin.tag'), t('admin.average')]}
            isEmpty={watchTags.length === 0}
            emptyLabel={t('common.empty')}
          >
            {watchTags.map((row) => (
              <tr key={row.tag}>
                <td className="px-4 py-2.5">{row.tag}</td>
                <td className="px-4 py-2.5">{fmtScore(row.averageScore)}</td>
              </tr>
            ))}
          </Table>
        </Card>
      </div>

      <Card className="mt-5">
        <div className="border-b border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-800 dark:border-slate-700 dark:text-slate-100">
          {t('admin.statsByTag')}
        </div>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-4 py-2.5 dark:border-slate-700">
          <SearchInput value={statsSearch} onChange={onStatsSearchChange} className="max-w-xs" />
        </div>
        <Table
          columns={[
            t('admin.tag'),
            t('admin.average'),
            t('admin.feedbackCount'),
            t('admin.challengesCount'),
            t('admin.completionRate'),
          ]}
          isEmpty={stats.length === 0}
          emptyLabel={statsSearch ? t('common.noResults') : undefined}
        >
          {stats.map((s) => (
            <tr key={s.tag}>
              <td className="px-4 py-2.5">{s.tag}</td>
              <td className="px-4 py-2.5">{fmtScore(s.averageScore)}</td>
              <td className="px-4 py-2.5">{s.feedbackCount}</td>
              <td className="px-4 py-2.5">{s.challengeCount}</td>
              <td className="px-4 py-2.5">{fmtPct(s.completionRate)}</td>
            </tr>
          ))}
        </Table>
        <Pagination
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          size={size}
          onPageChange={setPage}
          onSizeChange={(s) => {
            setPage(1);
            setSize(s);
          }}
        />
      </Card>
    </div>
  );
}
