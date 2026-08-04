// Pastilles de filtre par statut, partagées par les listes de feedbacks.
const FILTERS = [
    { key: "", label: "Tous" },
    { key: "SUBMITTED", label: "Soumis" },
    { key: "NOT_SUBMITTED", label: "En attente" },
    { key: "IN_PROGRESS", label: "En cours" },
];

export default function StatusFilter({ value, onChange }: { value: string; onChange: (v: string) => void }) {
    return (
        <div className="flex gap-2">
            {FILTERS.map((f) => (
                <button key={f.key} onClick={() => onChange(f.key)}
                        className={`rounded-lg px-3.5 py-1.5 text-sm font-medium transition ${
                            value === f.key ? "bg-brand text-white" : "border border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
                        }`}>
                    {f.label}
                </button>
            ))}
        </div>
    );
}