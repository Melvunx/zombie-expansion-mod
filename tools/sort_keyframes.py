#!/usr/bin/env python3
"""Trie les keyframes des fichiers .animation.json par temps croissant.

Blockbench exporte les temps entiers ("0", "1", "3") AVANT les temps décimaux ("0.25"), car un objet
JavaScript range ses clés entières en premier. GeckoLib lit les keyframes dans l'ordre du fichier et
ne les trie pas : un temps plus petit que le précédent casse l'animation (elle ne joue pas ou saute).

Usage : python3 tools/sort_keyframes.py [--check] fichier.animation.json [...]
  --check : n'écrit rien, signale seulement les fichiers à corriger (code de sortie 1 s'il y en a).
"""
import json
import re
import sys

CHANNELS = ("rotation", "position", "scale")


def sort_animation_file(path, write):
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    changed = False
    for name, anim in data.get("animations", {}).items():
        for bone, channels in anim.get("bones", {}).items():
            for channel in CHANNELS:
                frames = channels.get(channel)
                # Un seul keyframe (vecteur direct ou objet "vector") : rien à trier.
                if not isinstance(frames, dict) or "vector" in frames:
                    continue
                ordered = dict(sorted(frames.items(), key=lambda kv: float(kv[0])))
                if list(ordered) != list(frames):
                    print(f"{path}: {name} / {bone} / {channel}: {list(frames)} -> {list(ordered)}")
                    channels[channel] = ordered
                    changed = True
    if changed and write:
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            text = json.dumps(data, indent="\t", ensure_ascii=False)
            # Garde les vecteurs sur une ligne ([a, b, c]) comme l'export Blockbench, pour un diff lisible.
            text = re.sub(r"\[\s+([^\[\]{}\"]*?)\s+\]", lambda m: "[" + " ".join(m.group(1).split()) + "]", text)
            f.write(text + "\n")
    return changed


def main():
    args = sys.argv[1:]
    check = "--check" in args
    files = [a for a in args if a != "--check"]
    if not files:
        print(__doc__)
        return 2
    dirty = [sort_animation_file(p, not check) for p in files]
    return 1 if check and any(dirty) else 0


if __name__ == "__main__":
    sys.exit(main())
