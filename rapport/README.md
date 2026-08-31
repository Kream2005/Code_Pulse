# Rapport CodePulse (template IID)

## Contenu

| Élément | Rôle |
|---------|------|
| `main.tex` | **Fichier unique** du mémoire (structure + placeholders `% <<< CLAUDE`) |
| `iid.cls` | Classe LaTeX ENSA/IID (ne pas modifier sauf besoin) |
| `glossaire_reduit.tex` | Acronymes / glossaire |
| `biblio.bib` | Bibliographie |
| `images/` | Logos + diagrammes PNG + captures |
| `screenshots/` | Dépôt brut des captures |
| `plantuml/` | Sources PlantUML à exporter en PNG |
| `CLAUDE_PROMPT.md` | **Prompt complet** à donner à Claude pour rédiger le contenu |

## Workflow recommandé

1. Exporter les diagrammes PlantUML → `images/*.png` (voir `plantuml/README.md`).
2. Copier `CLAUDE_PROMPT.md` dans Claude (éventuellement joindre `main.tex`).
3. Coller / fusionner le LaTeX produit dans `main.tex`.
4. Ajouter les screenshots UI dans `images/`.
5. Compiler : `pdflatex` (+ glossaries / bibtex selon ta chaîne habituelle).

## Compilation (indicatif)

```bash
cd rapport
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
```

(arabtex / résumé arabe peuvent nécessiter une config locale ; si problème, laisser le résumé AR en « À compléter ».)
