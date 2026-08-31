import { useEffect, useState } from 'react';
import Card from '../../components/Card';
import ErrorBanner from '../../components/ErrorBanner';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import SearchInput from '../../components/SearchInput';
import FilterSelect from '../../components/FilterSelect';
import Table from '../../components/Table';
import { createUser, deleteUser, getUsersPage } from '../../api/resources';
import type { UtilisateurDto } from '../../api/types';
import { useI18n } from '../../i18n/I18nContext';

const STAFF_ROLES = ['MANAGER_RH', 'ADMIN_CODING_CHALLENGE', 'ADMIN_CODEPULSE'];

export default function AdminUsers() {
  const { t } = useI18n();
  const [items, setItems] = useState<UtilisateurDto[]>([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('');

  const [nom, setNom] = useState('');
  const [prenom, setPrenom] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('MANAGER_RH');

  function reload() {
    setLoading(true);
    getUsersPage(page, size, search, roleFilter)
      .then((data) => {
        setItems(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err) => setError(err.response?.data?.message ?? 'Chargement impossible.'))
      .finally(() => setLoading(false));
  }

  useEffect(reload, [page, size, search, roleFilter]);

  function onSearchChange(value: string) {
    setPage(1);
    setSearch(value);
  }

  function onRoleFilterChange(value: string) {
    setPage(1);
    setRoleFilter(value);
  }

  async function onCreate(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      await createUser({ nom, prenom, email, rawPassword: password, role });
      setMessage(`Utilisateur ${email} créé.`);
      setNom('');
      setPrenom('');
      setEmail('');
      setPassword('');
      reload();
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Création impossible.'
      );
    }
  }

  async function onDelete(u: UtilisateurDto) {
    if (!confirm(`Supprimer ${u.email} ?`)) return;
    await deleteUser(u.id);
    reload();
  }

  return (
    <div>
      <PageHeader title="Utilisateurs" />
      {error && <ErrorBanner message={error} />}
      {message && (
        <div className="mb-4 border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
          {message}
        </div>
      )}

      <Card padded className="mb-5">
        <h3 className="mb-4 text-lg font-semibold text-slate-800 dark:text-slate-100">Créer un compte</h3>
        <form onSubmit={onCreate} className="grid gap-3 sm:grid-cols-2">
          <input
            className="rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            placeholder="Nom"
            value={nom}
            onChange={(e) => setNom(e.target.value)}
            required
          />
          <input
            className="rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            placeholder="Prénom"
            value={prenom}
            onChange={(e) => setPrenom(e.target.value)}
            required
          />
          <input
            className="rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            placeholder="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <input
            className="rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            placeholder="Mot de passe"
            type="password"
            name="new-user-password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
          />
          <select
            className="rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            value={role}
            onChange={(e) => setRole(e.target.value)}
          >
            {STAFF_ROLES.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
          <button
            type="submit"
            className="rounded-lg bg-brand px-4 py-2 font-semibold text-white hover:bg-brand-dark"
          >
            Créer
          </button>
        </form>
      </Card>

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-4 py-2.5 dark:border-slate-700">
          <SearchInput
            value={search}
            onChange={onSearchChange}
            placeholder={t('common.searchPlaceholder')}
            className="max-w-xs"
          />
          <FilterSelect
            value={roleFilter}
            onChange={onRoleFilterChange}
            allLabel={t('common.allRoles')}
            options={STAFF_ROLES.concat('USER').map((r) => ({ value: r, label: r }))}
          />
        </div>
        <Table
          columns={['Nom', 'Email', 'Rôle', 'Action']}
          isEmpty={loading || items.length === 0}
          emptyLabel={loading ? 'Chargement…' : search || roleFilter ? t('common.noResults') : 'Aucun utilisateur.'}
        >
          {items.map((u) => (
            <tr key={u.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/60">
              <td className="px-4 py-3 font-medium">
                {u.prenom} {u.nom}
              </td>
              <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{u.email}</td>
              <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{u.role}</td>
              <td className="px-4 py-3">
                <button
                  type="button"
                  onClick={() => onDelete(u)}
                  className="text-xs font-semibold text-red-600 dark:text-red-400 hover:text-red-700"
                >
                  Supprimer
                </button>
              </td>
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
