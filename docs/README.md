# Documentazione GATHORAPP

## Struttura

```
docs/
├── source/                # Sorgenti (.md, .mermaid, immagini, LaTeX)
├── pdf/                   # PDF generati (GATHORAPP.pdf + diagrammi)
├── generate_pdf.py        # Script generazione PDF (Python 3.7+, cross-platform)
├── generate-pdf.bat       # Wrapper Windows (delega a generate_pdf.py)
├── generate-pdf.sh        # Wrapper Unix/Linux/macOS (delega a generate_pdf.py)
└── README.md              # Questo file
```

## Requisiti

### Obbligatori

- **Python** 3.7+
- **Pandoc** 3.8+: [https://pandoc.org/installing.html](https://pandoc.org/installing.html)
- **XeLaTeX** (per Pandoc → PDF):
  - Windows: MiKTeX ([https://miktex.org](https://miktex.org))
  - macOS: MacTeX ([http://tug.org/mactex/](http://tug.org/mactex/))
  - Linux: `sudo apt install texlive-xetex texlive-fonts-recommended`

### Opzionali (per rigenerare diagrammi Mermaid)

- **Mermaid CLI**: installare uno dei due:
  - `npm install -g @mermaid-js/mermaid-cli` (globale)
  - oppure usare `npx @mermaid-js/mermaid-cli` (scarica al primo avvio)

> **Nota**: Se i diagrammi sono già stati generati in precedenza, lo script li recupera da `source/pdf/` anche se `mmdc` non è disponibile.

## Uso

### Genera tutti i PDF (principale + diagrammi)

**Opzione 1: Diretto (tutti i sistemi)**

```bash
python generate_pdf.py
```

**Opzione 2: Wrapper (Windows)**

```cmd
generate-pdf.bat
```

**Opzione 3: Wrapper (Unix/Linux/macOS)**

```bash
./generate-pdf.sh
```

### Output

Tutti i PDF vengono generati nella cartella `pdf/`:

```
pdf/
├── GATHORAPP.pdf                               # Documento principale
├── class-diagram.pdf
├── sequence-diagram.pdf
├── sequence-diagram-chat-websocket.pdf
├── sequence-diagram-voucher-redemption.pdf
├── use-case-diagram-1-events-outings.pdf
├── use-case-diagram-2-communication-rewards.pdf
└── use-case-diagram-3-administration.pdf
```

## Note tecniche

- **Cross-platform**: lo script Python `generate_pdf.py` funziona su Windows, macOS, Linux.
- **Fallback automatico**: se `mmdc` non è disponibile, lo script recupera i PDF dei diagrammi da `source/pdf/` (se già generati).
- **Visualizzazione online**: i file `.mermaid` possono essere visualizzati su [https://mermaid.live](https://mermaid.live).
- **MiKTeX updates**: su Windows, MiKTeX potrebbe chiedere di controllare aggiornamenti al primo avvio (warning ignorabili).
- **Font glifi**: se il PDF mostra caratteri vuoti per i gliffi (es. ✓), aggiornare il font in `generate_pdf.py` (linea ~38, `mainfont` e `monofont`).

---
