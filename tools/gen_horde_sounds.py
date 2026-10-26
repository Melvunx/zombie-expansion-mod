#!/usr/bin/env python3
"""Synthétise les sons provisoires du chef de horde et des sous-fifres (voix de synthèse), puis les encode en .ogg.

Usage : python3 tools/gen_horde_sounds.py [--oggenc CHEMIN] [DOSSIER_SORTIE]
Sortie par défaut : src/main/resources/assets/zombie_expansion/sounds/entity/unearthed/

Principe : une « voix » = une source glottale (dent de scie) passée dans des résonateurs (les formants,
qui donnent la voyelle), plus du bruit (souffle, rugosité) et un peu de saturation. Rien d'enregistré :
ce sont des sons provisoires, à remplacer par de vrais enregistrements quand tu en as.
"""
import argparse, math, os, random, shutil, struct, subprocess, sys, tempfile, wave

SR = 44100

def lerp_points(points, t):
    """Interpolation linéaire d'une courbe [(temps, valeur), ...]."""
    if t <= points[0][0]: return points[0][1]
    for (t0, v0), (t1, v1) in zip(points, points[1:]):
        if t <= t1:
            return v0 + (v1 - v0) * (t - t0) / (t1 - t0) if t1 > t0 else v1
    return points[-1][1]

def voice(dur, f0, formants, env, vib=(0, 0), noise=0.0, drive=1.0, seed=1):
    rnd = random.Random(seed)
    n = int(dur * SR)
    filters = [(2 * math.pi * f / SR, math.exp(-math.pi * bw / SR), g) for f, bw, g in formants]
    state = [[0.0, 0.0] for _ in filters]
    phase = 0.0
    out = []
    for i in range(n):
        t = i / SR
        hz = lerp_points(f0, t) * (1 + vib[1] * math.sin(2 * math.pi * vib[0] * t) if vib[0] else 1)
        hz *= 1 + 0.01 * (rnd.random() - 0.5)                    # léger tremblement de hauteur
        phase = (phase + hz / SR) % 1.0
        src = (1 - 2 * phase) + noise * (rnd.random() * 2 - 1)   # dent de scie + souffle
        y = 0.0
        for k, (theta, r, g) in enumerate(filters):
            y1, y2 = state[k]
            v = (1 - r) * src + 2 * r * math.cos(theta) * y1 - r * r * y2
            state[k] = [v, y1]
            y += g * v
        out.append(math.tanh(drive * y * 2.5) * lerp_points(env, t))
    return out

def place(buf, seg, at):
    start = int(at * SR)
    if len(buf) < start + len(seg): buf.extend([0.0] * (start + len(seg) - len(buf)))
    for i, v in enumerate(seg): buf[start + i] += v

def mix(*layers):
    n = max(len(l) for l in layers)
    return [sum(l[i] if i < len(l) else 0.0 for l in layers) for i in range(n)]

def finish(buf, peak=0.85):
    m = max(abs(v) for v in buf) or 1.0
    fade = int(0.004 * SR)
    out = [v / m * peak for v in buf]
    for i in range(min(fade, len(out))):
        out[i] *= i / fade; out[-1 - i] *= i / fade
    return out

A = lambda *fs: [(f, bw, g) for f, bw, g in fs]
SOUNDS = {}

# ----- Chef : grave et puissant -----
SOUNDS["horde_chief/ambient"] = lambda: voice(1.3, [(0, 60), (.6, 50), (1.3, 42)], A((450, 90, 1), (850, 120, .7), (2400, 200, .2)),
    [(0, 0), (.15, .9), (.9, .8), (1.3, 0)], vib=(5, .04), noise=.25, drive=2.5, seed=11)
SOUNDS["horde_chief/attack"] = lambda: voice(.55, [(0, 130), (.15, 150), (.55, 70)], A((700, 110, 1), (1150, 130, .8), (2500, 200, .3)),
    [(0, 0), (.03, 1), (.3, .8), (.55, 0)], noise=.5, drive=4, seed=12)
SOUNDS["horde_chief/boost"] = lambda: mix(
    voice(1.6, [(0, 70), (.3, 150), (.8, 190), (1.6, 110)], A((750, 110, 1), (1200, 130, .9), (2700, 220, .3)),
          [(0, 0), (.05, 1), (1, .9), (1.6, 0)], vib=(7, .05), noise=.35, drive=3.5, seed=13),
    voice(1.6, [(0, 35), (.3, 75), (.8, 95), (1.6, 55)], A((500, 120, 1), (900, 140, .6)),
          [(0, 0), (.05, .8), (1, .7), (1.6, 0)], noise=.3, drive=3, seed=14))

# ----- Sous-fifre « dopey » : lent, dans la lune -----
SOUNDS["horde_minion/dopey/ambient"] = lambda: voice(1.2, [(0, 100), (.5, 88), (1.2, 70)], A((330, 80, 1), (700, 100, .7), (2300, 200, .1)),
    [(0, 0), (.2, .8), (.9, .7), (1.2, 0)], vib=(3, .08), noise=.08, drive=1.2, seed=21)
def dopey_attack():
    buf = []
    place(buf, voice(.25, [(0, 100), (.25, 110)], A((450, 90, 1), (900, 120, .6)), [(0, 0), (.05, 1), (.25, 0)], noise=.1, drive=1.5, seed=22), 0)
    place(buf, voice(.3, [(0, 150), (.3, 100)], A((700, 110, 1), (1100, 130, .8)), [(0, 0), (.04, 1), (.3, 0)], noise=.25, drive=2.5, seed=23), .3)
    return buf
SOUNDS["horde_minion/dopey/attack"] = dopey_attack
SOUNDS["horde_minion/dopey/boost"] = lambda: voice(.6, [(0, 90), (.4, 150), (.6, 140)], A((450, 90, 1), (1100, 120, .7)),
    [(0, 0), (.05, 1), (.45, .9), (.6, 0)], vib=(6, .05), noise=.1, drive=1.5, seed=24)

# ----- Sous-fifre « cross_eyed » : ricane nerveusement -----
def giggle():
    buf = []
    for k in range(5):
        place(buf, voice(.09, [(0, 260 + 10 * k), (.09, 320 + 10 * k)], A((500, 80, 1), (2000, 150, .6)),
                         [(0, 0), (.02, 1), (.09, 0)], noise=.2, drive=1.5, seed=30 + k), k * .14)
    return buf
SOUNDS["horde_minion/cross_eyed/ambient"] = giggle
SOUNDS["horde_minion/cross_eyed/attack"] = lambda: voice(.4, [(0, 300), (.12, 470), (.4, 260)], A((800, 110, 1), (1400, 130, .7)),
    [(0, 0), (.03, 1), (.25, .8), (.4, 0)], noise=.2, drive=2, seed=35)
SOUNDS["horde_minion/cross_eyed/boost"] = lambda: voice(.7, [(0, 240), (.35, 520), (.7, 260)], A((400, 80, 1), (2200, 160, .6)),
    [(0, 0), (.05, 1), (.55, .9), (.7, 0)], vib=(13, .06), noise=.05, drive=1.3, seed=36)

# ----- Sous-fifre « toothy » : « heh heh », content de lui -----
def heh():
    buf = []
    for k in range(3):
        place(buf, voice(.16, [(0, 190 - 10 * k), (.16, 170 - 10 * k)], A((650, 100, 1), (1800, 150, .6), (3000, 250, .2)),
                         [(0, 0), (.03, 1), (.16, 0)], noise=.45, drive=1.5, seed=40 + k), k * .28)
    return buf
SOUNDS["horde_minion/toothy/ambient"] = heh
SOUNDS["horde_minion/toothy/attack"] = lambda: voice(.45, [(0, 230), (.4, 170)], A((750, 110, 1), (1300, 130, .7)),
    [(0, 0), (.02, 1), (.3, .8), (.45, 0)], noise=.5, drive=3, seed=45)
SOUNDS["horde_minion/toothy/boost"] = lambda: voice(.7, [(0, 200), (.25, 330), (.7, 300)], A((500, 90, 1), (2100, 160, .6)),
    [(0, 0), (.05, 1), (.5, .9), (.7, 0)], vib=(6, .03), noise=.08, drive=1.4, seed=46)

def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    ap = argparse.ArgumentParser()
    ap.add_argument("--oggenc", default=shutil.which("oggenc"))
    ap.add_argument("out", nargs="?", default=os.path.join(root, "src/main/resources/assets/zombie_expansion/sounds/entity/unearthed"))
    args = ap.parse_args()
    if not args.oggenc:
        sys.exit("oggenc introuvable : installe vorbis-tools ou passe --oggenc")
    for name, make in SOUNDS.items():
        samples = finish(make())
        dest = os.path.join(args.out, name + ".ogg")
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with tempfile.TemporaryDirectory() as tmp:
            wav = os.path.join(tmp, "s.wav")
            with wave.open(wav, "wb") as w:
                w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
                w.writeframes(b"".join(struct.pack("<h", int(v * 32767)) for v in samples))
            subprocess.run([args.oggenc, "-q", "4", "-Q", "-o", dest, wav], check=True)
        print(f"{name}.ogg  {len(samples) / SR:.2f}s")

if __name__ == "__main__":
    main()
