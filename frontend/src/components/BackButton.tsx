import { useNavigate } from "react-router-dom";

// Bouton retour carré, réutilisé en tête des pages secondaires.
// `to` permet de cibler une autre page que le tableau de bord si besoin.
export default function BackButton({ to = "/" }: { to?: string }) {
    const navigate = useNavigate();

    return (
        <button
            type="button"
            onClick={() => navigate(to)}
            aria-label="Retour au tableau de bord"
            className="flex h-10 w-10 flex-none items-center justify-center border border-slate-300 bg-white text-slate-500 transition hover:border-brand hover:text-brand"
        >
            <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M15 18l-6-6 6-6" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
        </button>
    );
}
