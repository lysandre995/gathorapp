#!/usr/bin/env python3
"""
generate_pdf.py

Generates the main documentation PDF (Pandoc + xelatex) and converts Mermaid
diagrams (.mermaid) to PDF using mermaid-cli (mmdc). All outputs go into
`docs/pdf/`.

Usage: run from repository root or from `docs/` folder. Example:
  python docs/generate_pdf.py

Requirements:
- pandoc in PATH
- xelatex (MiKTeX/TeX Live) in PATH
- mmdc in PATH (mermaid-cli)
- Python 3.7+
"""
from __future__ import annotations

import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent  # docs/
SOURCE = ROOT / "source"
OUT_DIR = ROOT / "pdf"
MAIN_MD = SOURCE / "GATHORAPP.md"
MAIN_PDF = OUT_DIR / "GATHORAPP.pdf"
HEADER_TEX = SOURCE / "pandoc-header.tex"

PANDOC_OPTS = [
    "--pdf-engine=xelatex",
    "-V",
    "fontsize=10pt",
    "-V",
    "geometry:margin=2.5cm",
    "--syntax-highlighting=pygments",
    "-V",
    "linkcolor=blue",
    "-V",
    "urlcolor=blue",
    "-V",
    "block-headings",
    "-V",
    "documentclass=report",
]

# Add header file if it exists
if HEADER_TEX.exists():
    PANDOC_OPTS.extend(["-H", str(HEADER_TEX)])


def check_tool(name: str) -> bool:
    return shutil.which(name) is not None


def run_cmd(cmd, cwd: Path | None = None) -> tuple[int, str, str]:
    proc = subprocess.run(
        cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, shell=True
    )
    return proc.returncode, proc.stdout, proc.stderr


def ensure_out():
    OUT_DIR.mkdir(parents=True, exist_ok=True)


def build_main_pdf() -> bool:
    if not MAIN_MD.exists():
        print(f"[ERROR] Main markdown not found: {MAIN_MD}")
        return False

    cmd = ["pandoc", str(MAIN_MD), "-o", str(MAIN_PDF)] + PANDOC_OPTS
    print("Running pandoc for main PDF:")
    print(" ", " ".join(cmd))
    code, out, err = run_cmd(cmd, cwd=ROOT)
    if code == 0:
        print(f"[OK] {MAIN_PDF} generated")
        return True
    else:
        print(f"[ERROR] pandoc failed (exit {code})")
        print(err)
        return False


def build_mermaid_pdfs() -> bool:
    if not SOURCE.exists():
        print(f"[WARN] Source folder missing: {SOURCE}, skipping diagrams")
        return True

    mermaids = sorted(SOURCE.glob("*.mermaid"))
    if not mermaids:
        print("[INFO] No .mermaid files found in source/ to convert")
        return True

    ok_all = True

    # Choose mermaid command: prefer mmdc, fallback to npx @mermaid-js/mermaid-cli
    def make_cmd(input_path: Path, output_path: Path):
        if check_tool("mmdc"):
            return ["mmdc", "-i", str(input_path), "-o", str(output_path), "--pdfFit"]
        if check_tool("npx"):
            # use npx to run mermaid-cli (may download if missing)
            return [
                "npx",
                "-y",
                "@mermaid-js/mermaid-cli",
                "-i",
                str(input_path),
                "-o",
                str(output_path),
                "--pdfFit",
            ]
        return None

    for m in mermaids:
        out_pdf = OUT_DIR / (m.stem + ".pdf")
        cmd = make_cmd(m, out_pdf)
        if cmd is None:
            print(
                "[ERROR] mermaid CLI not found (mmdc or npx). Install 'mermaid-cli' or 'npx'. Skipping diagrams."
            )
            return False

        print(f"Converting {m} -> {out_pdf}")
        try:
            code, out, err = run_cmd(cmd, cwd=ROOT)
        except FileNotFoundError as e:
            ok_all = False
            print(f"[ERROR] Command not found when trying to run: {cmd[0]} ({e})")
            continue

        if code == 0:
            print(f"[OK] {out_pdf} generated")
        else:
            ok_all = False
            print(f"[ERROR] mermaid conversion failed for {m} (exit {code})")
            if err:
                print(err)
    return ok_all


def main():
    print("Generating PDFs into:", OUT_DIR)

    # Check tools
    missing = []
    for tool in ("pandoc", "mmdc", "xelatex"):
        if not check_tool(tool):
            missing.append(tool)
    if missing:
        print("[WARN] The following tools are not found in PATH:", ", ".join(missing))
        print(
            "The script will still try to run; ensure tools are installed if failures occur."
        )

    ensure_out()

    success_main = build_main_pdf()
    success_mermaid = build_mermaid_pdfs()
    # Fallback: if mermaid CLI wasn't available or didn't run, copy any existing diagram PDFs
    src_pdf_dir = SOURCE / "pdf"
    if src_pdf_dir.exists():
        copied = 0
        for p in src_pdf_dir.glob("*.pdf"):
            dest = OUT_DIR / p.name
            try:
                shutil.copy2(p, dest)
                print(f"[INFO] Copied existing diagram PDF {p} -> {dest}")
                copied += 1
            except Exception as e:
                print(f"[WARN] Failed to copy {p} -> {dest}: {e}")
        if copied:
            success_mermaid = True

    if success_main and success_mermaid:
        print("All PDFs generated successfully (or retrieved).")
        sys.exit(0)
    else:
        print("Some PDF generation steps failed. See logs above.")
        sys.exit(2)


if __name__ == "__main__":
    main()
