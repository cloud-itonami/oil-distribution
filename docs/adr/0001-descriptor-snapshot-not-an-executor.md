# ADR-0001 — この repo は descriptor snapshot であって executor ではない

- status: accepted
- date: 2026-08-09
- 対象: `cloud-itonami/oil-distribution`
- 上位: superproject ADR-2608080000（成熟度を 1 段ずつ上げる loop）/
  ADR-2608052000（7 軸の測り方）/ ADR-2606231200（sovereign actor repo pilot）

## 文脈

superproject の成熟度 loop（skill `itonami-maturity-improve`）が、この repo を
`axis-docs`（`own = 0.041`、README 0 バイト）で名指しした。同じ形の repo が
7 本並んでおり、前周は `oil-coverage` を同じ軸で処理している。

この repo は 2026-06-24 に etzhayyim monorepo の `20-actors/oil-distribution` から
**descriptor だけを写した** snapshot として起こされた（`f6a6571`）。commit は
4 本しか無い:

| commit | 日付 | 内容 |
|---|---|---|
| `f6a6571` | 2026-06-24 | snapshot（manifest / did.json / NOTICE / test.ts） |
| `53f6a34` | 2026-07-02 | did:web を `etzhayyim.com` scheme へ移行 |
| `d1a921f` | 2026-07-18 | murakumo WIP の rescue（`src/oil_distribution/murakumo.kotoba`） |
| `9c3ff48` | 2026-07-27 | 上の rescue branch を main へ merge |

## 問題

README が 0 バイトだったため、この repo を読む者は次の 3 つを区別できなかった:

1. **宣言**（manifest が「こう動く」と書いていること）
2. **判断**（gate が実際に実行できること）
3. **実装**（どこにも無いもの）

名前（`oil-distribution`）と manifest の記述（`runtime: k8s-langserver` /
`edge: sveltekit-proxy` / cron trigger）は、素直に読むと **1 と 3 を同じもの**に
見せる。実際には 3 は 1 バイトも無い。

## 決定

### 1. README は「何が無いか」から書く

`axis-docs` は README のバイト数を測るが、**バイト数を目的にしない**。この repo に
ついて読み手が最初に知るべきことは「動くサービスは無い」であり、それを冒頭 5 行に
置く。7 軸のうち docs 軸だけを上げること自体は loop の設計どおりだが、水増しで
上げるなら上げない方がよい（skill の禁止事項）。

### 2. 主張はすべて実行可能な手順に還元する

README が出す数（pipeline 8 / sub-actor 4 / cell 15 / gate 7 / label 2）と
identity の状態は、すべて `docs/operator-quickstart.md` の 6 手順で再現できる形に
した。**書いたあとに全手順を verbatim で実行**し、貼った出力は実行結果の写しに
した（手打ちの擬似出力を置かない）。

### 3. 2 つの DID の不整合は「記録するが直さない」

- `actor-manifest.jsonld` の `@id` と `murakumo.cljc` の `actor-did` は
  `did:web:oil-distribution.etzhayyim.com` を名乗るが、**DNS レコードが無く解決しない**
- `.well-known/did.json` の `id` は `did:web:etzhayyim.com:actor:oil-distribution` で、
  **こちらは 200 で解決する**

どちらを正とするかは etzhayyim 側の identity 決定であり、descriptor snapshot が
片方に寄せてよいものではない。**現状を測って書き、直さない。**

同じ理由で、repo の `.well-known/did.json` を live（5 か所相違）に合わせることも
しない —— live 側は `_meta` に「ERC725 mirror pending」と書いており、まだ動いている
最中の文書である。

### 4. gate と manifest の collection 語彙が交わらないことを、直さずに記録する

`murakumo.cljc` の `collection` 関数は `com.etzhayyim.oil-distribution.<name>` を
組み立てるが、manifest 側は `com.etzhayyim.apps.oilDistribution.<name>` である。
**共通する綴りは 1 つも無い。** さらに gate は XRPC の *メソッド* 名
（`getproductterminal`）を書き込み先 *collection* として扱う。

どちらも scaffold 生成器の産物と読めるが、正しい語彙を決めるのは lexicon 側の
権限なので、この反復では**発見の記録に留める**。

### 5. 「購読先を書く者が居ない」ことを boundary として明記する

7 本の `oil-*` repo の `graph.write` を全部並べると、**すべてが自分の
`coverageSnapshot` だけを書く**。この repo が購読すると宣言する 4 collection
（`oilDistribution.productTerminal` / `.wholesaleHub` / `oilRefining.yieldSnapshot` /
`oilShipping.cargo`）を書く pipeline は、フリート内に 1 つも無い。

これは欠陥の指摘ではなく **境界の記述**である —— これらのノードは 7 本の外側から
入る前提になっており、その供給元はどの宣言にも書かれていない。読み手が「この repo が
ターミナルを登録する」と誤読しないために書く。

## 測ったこと（2026-08-09、すべて実測）

| 主張 | 実測 |
|---|---|
| pipeline 数 | 8（cron 2 / subscribeRepos 1 / xrpc 5） |
| sub-actor 数 | 4（うち `risk:inventory` に対応する pipeline は**無い**） |
| capability 数 | 5（うち `agent.invoke` はどの step でも未使用） |
| gate cell 数 | 15 = 5 xrpc + 2 requiredCollections + 4 requiredLoops + 4 subscribeRepos |
| gate 数 | 7、deny-by-default（6/7 でも `:blocked`、15 cell 全部 blocked で effect 0） |
| `oil-distribution.etzhayyim.com` | A/AAAA 無し、curl `000` |
| `etzhayyim.com/actor/oil-distribution/did.json` | `200` |
| repo did.json vs live | 5 か所相違（`diff` exit 1） |
| `pds.etzhayyim.com/xrpc/_health` | `530` |
| `pds.aozora.app/xrpc/_health` | `200`（`{"ok":true,"app":"aozora-pds"}`） |
| GitHub Pages（両 org） | `404` |
| `actor-manifest.test.ts` | 11 `it(` / 16 `expect(`、**走らない**（package.json も node_modules も無い） |
| 兄弟 5 本 | すべて `actors=4` / `pipelines=8`（`oil-coverage` のみ 6 / 5） |

## 結果

- `README.md` / `docs/operator-quickstart.md` / この ADR を追加した。
- superproject の west pin を `53f6a34` → `9c3ff48` に進めた。旧 pin は
  2026-07-18 の rescue より前を指しており、**`src/oil_distribution/murakumo.kotoba`
  （219 行の gate）が checkout に現れていなかった**。成熟度 scan は pin ではなく
  checkout を読むので、この repo の substrate は 0 と測られていた —— pin を正した
  副産物として、その過小評価も解消される。

## やっていないこと

- **test を書いていない。** README が主張する数と 2 つの DID を実体と突き合わせる
  ものが無く、manifest が変わっても README は赤くならない。`marine-insurance` の
  `test/marine_insurance/docs_test.cljs` が先例で、同型が要る。1 反復 1 軸なので
  次周（`axis-test`）に残した。
- identity の不整合（決定 3）、collection 語彙の不一致（決定 4）、
  `risk:inventory` の欠落 pipeline は、いずれも記録のみ。
- namespace の `oil_distribution`（アンダースコア）は直していない —— 直すと
  `require` の綴りが変わる。
- `actor-manifest.test.ts` を nbb の `test/` に書き直していない。
- **同型の兄弟 5 本**（`oil-upstream` / `oil-midstream` / `oil-refining` /
  `oil-trading` / `oil-shipping`）は README 0 バイトのまま。今回 1 本だけ。
