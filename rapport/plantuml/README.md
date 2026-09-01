# Diagrammes PlantUML

Exporter chaque `.puml` en PNG (ou SVG) vers `../images/` avec le **même nom** que dans `main.tex`.

## En ligne

1. Ouvre https://www.plantuml.com/plantuml/uml/
2. Colle le contenu du `.puml`
3. Télécharge PNG
4. Renomme selon le tableau ci-dessous

## En local (si Java + plantuml.jar)

```bash
java -jar plantuml.jar -tpng -o ../images *.puml
```

Puis renomme si besoin pour coller exactement :

| Source | Fichier image attendu |
|--------|------------------------|
| `01_architecture_globale.puml` | `../images/architecture_globale.png` |
| `02_diagramme_classes.puml` | `../images/diagramme_classes.png` |
| `03_sequence_ingest_notification.puml` | `../images/sequence_ingest_notif.png` |
| `04_sequence_feedback.puml` | `../images/sequence_feedback.png` |
| `05_sequence_relance.puml` | `../images/sequence_relance.png` |
| `06_sequence_rag.puml` | `../images/sequence_rag.png` |
| `07_cas_utilisation.puml` | `../images/cas_utilisation.png` |
