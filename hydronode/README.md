## Hydronode

Terminal-only Java playground for turning the Kaggle notebooks in this folder
into a properly layered object oriented program. Just `javac` and `java` (JDK 17+; built on JDK 27).

## How to run

```bash
cd hydronode
chmod +x run.sh     # first time only
./run.sh
```

> [!IMPORTANT]  
> For windows there is run.cmd and run.ps1 support anyway. If there is issue just run `Set-ExecutionPolicy -Scope Process Bypass`

Check your JDK first — anything below 17 will not compile:

## What it does

A console loop that asks what kind of **test data entry** you want, builds it,
and prints it.

Three kinds of entry:

1. **Real row** — a random row pulled from the dataset CSV.
2. **Synthetic** — values drawn from the dataset's own observed ranges and
   category options.
3. **Manual** — you answer one prompt per column, validated as you go.

A single entry prints as a **vertical** table (21 columns would wrap off
screen); the history view prints a **horizontal** summary table.

## Datasets

Three datasets, switchable from the menu (`[5]`). Each keeps its own history.

Every entry shows a **verdict** taken from the dataset's own label — leak or no
leak, potable or not. The Potability dataset can additionally train a pure-Java
Random Forest (`[4]`), and its prediction is shown next to the real label.

### 1. LeakDB / BattLeDIM — water leak  ← the leak / no-leak one

Link: https://www.kaggle.com/code/larserikrisholm/predict-pipe-burst-and-water-leakage

From `predict-pipe-burst-and-water-leakage.ipynb`. `2018_Leakages.csv` is one
row per 5-minute timestamp with a column per pipe, `;`-separated and using a
comma as decimal separator.

An entry is **one pipe reading** (timestamp + pipe + value), not a whole
timestamp. Reason: pipe `p257` carries a leak through 94% of 2018, so "any pipe
leaking" answers yes almost always. Per pipe the real leak rate is **17.3%** of
1,471,680 readings (105,120 timestamps × 14 pipes) — a question worth asking:

```
  ┌─ VERDICT ──────────────────────────────────────────────────────────────┐
  │    ⚠  LEAK                                                             │
  │    reading 16.00 against a threshold of 5.0                            │
  └────────────────────────────────────────────────────────────────────────┘
```

Threshold `5.0` follows the notebook (`leak_threshold = 5`). Values below it are
measurement noise (`0.01`–`0.04`); real events run `5`–`35`.

```bash
kaggle datasets download larserikrisholm/battledim -p data
unzip -o data/battledim.zip -d data     # → data/battledim/BattLeDIM/2018_Leakages.csv
```

### 2. Water Potability — drinking water safety

Link: https://www.kaggle.com/code/richienrg/water-potability-prediction-omdena

From `water-potability-prediction-omdena.ipynb` (the Random Forest baseline).
3,276 rows, 9 chemical features and a 0/1 `Potability` label — 1,998 not potable
vs 1,278 potable.

```
  ┌─ VERDICT ──────────────────────────────────────────────────────────────┐
  │    ⚠  NOT POTABLE                                                      │
  │    Potability = 0 (unsafe to drink)                                    │
  └────────────────────────────────────────────────────────────────────────┘
```

Missing readings in the source (`ph` 491, `Sulfate` 781, `Trihalomethanes` 162)
show as an em dash, never as a guessed value.

```bash
kaggle datasets download adityakadiwal/water-potability -p data/potability
unzip -o data/potability/water-potability.zip -d data/potability
```

### 3. Irrigation Need — playground-series-s6e4

Link: https://www.kaggle.com/code/beraterolelk/0-982-sota-oof-meta-stacking-golden-feats

From `0-982-sota-oof-meta-stacking-golden-feats.ipynb`. 21 columns, 630,000
rows, label `Irrigation_Need`. 

```bash
kaggle datasets download zia068/playground-series-s6e4 -f train.csv -p data
```



