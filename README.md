# oil-distribution

**石油サプライチェーンの *下流*（製品ターミナル・卸ハブ）を扱うと宣言した actor の
descriptor と、その書き込みを止める deny-by-default gate。ターミナルの実データも、
それを読むグラフも、ここには無い。実装ではない。**

`oil-*` は 7 本ある。うち 6 本（`upstream` / `midstream` / `refining` / `trading` /
`shipping` / **`distribution`**）が segment の実体を扱う側で、`oil-coverage` だけが
それらを上から測る meta actor。**この repo は 6 本のうち、精製後の製品が市場へ出る
最後の区間**を担当すると宣言している。

| | ここにあるか |
|---|---|
| actor が**何を名乗り、何を要求し、どの pipeline を持つと宣言しているか** | **ある**（`actor-manifest.jsonld` 8,614 B / `.well-known/did.json`） |
| **gate**（attestation が 7 つ揃わなければ effect を 1 つも出さない判断） | **ある**（`src/oil_distribution/murakumo.kotoba`、219 行） |
| ターミナルを数えるグラフ、cron を撃つ scheduler、XRPC を受ける server | **無い** |
| 製品ターミナル・卸ハブの実データ | **無い** |

**ここには動くサービスは無い。** `cell-plan` が返すのは「書くとしたら何をどこに
書くか」という**計画**であって、書き込みそのものではない。`:effects` は
`{:op :mst/put-record ...}` という data であり、それを実行する者はこの repo に居ない。

経緯は [docs/adr/0001-descriptor-snapshot-not-an-executor.md](docs/adr/0001-descriptor-snapshot-not-an-executor.md)。
手順は [docs/operator-quickstart.md](docs/operator-quickstart.md)。

## 名前が 2 つあり、解決するのは片方だけ

この repo は自分を 2 通りに名乗っている。**同じ actor の別表記ではなく、
一方は DNS に存在しない。**

| 出所 | 名乗り | 2026-08-09 実測 |
|---|---|---|
| `actor-manifest.jsonld` の `@id`<br>`src/oil_distribution/murakumo.kotoba` の `actor-did` | `did:web:oil-distribution.etzhayyim.com` | **解決しない**。`oil-distribution.etzhayyim.com` に A/AAAA レコードが無く、`curl` は `000`（接続前に失敗） |
| `.well-known/did.json` の `id` | `did:web:etzhayyim.com:actor:oil-distribution` | **解決する**。`https://etzhayyim.com/actor/oil-distribution/did.json` が `200` |

**gate が名乗るのは解決しない方**である（`murakumo.cljc:6`）。effect の
`:actor` フィールドに載るのもそちら。これは既知の不整合で、直していない —
どちらを正とするかは etzhayyim 側の identity 決定であって、この snapshot が
勝手に決めてよいことではない。

### この repo の `.well-known/did.json` は配信されていない

`.nojekyll` が置かれているので GitHub Pages 配信を意図していたと読めるが、
2026-08-09 時点で `etzhayyim.github.io/com-etzhayyim-oil-distribution` も
`cloud-itonami.github.io/oil-distribution` も **404**。

さらに、解決する方の URL が実際に返す文書は、**この repo の中の did.json とは
別物**である（`jq -S` して `diff` すると 5 か所相違）:

| | repo の `.well-known/did.json` | live（`etzhayyim.com/actor/oil-distribution/did.json`） |
|---|---|---|
| crypto suite context | `ed25519-2020` | `jws-2020` |
| `alsoKnownAs` | 4 件（at:// · github · rad: · github.io） | **空配列** |
| `verificationMethod` | **フィールド自体が無い** | 空配列（`_meta` に「ERC725 mirror pending」と注記） |
| PDS endpoint | `https://pds.etzhayyim.com` → **530**（root も `/xrpc/_health` も） | `https://pds.aozora.app` → root は 404 だが `/xrpc/_health` は **200**（`{"ok":true,"app":"aozora-pds"}`） |
| 2 つ目の service | `#aozora`（AozoraAppView、`https://aozora.app`） | `#xrpc-libp2p`（`/dnsaddr/etzhayyim.com/p2p/12D3KooW…`） |

つまり **repo の did.json は live の写しではなく、live より古い（あるいは別系統の）
文書**。ここを「配信されている DID document」として読まないこと。

## 何を扱うと宣言しているか

`actor-manifest.jsonld` は **8 pipeline** を宣言する（cron 2 / subscribeRepos 1 / xrpc 5）。

| trigger | 何を宣言しているか |
|---|---|
| cron `0 */8 * * *`（5 step） | **報告する。** 国別のターミナル数と貯蔵容量 → 卸ハブの数とスループット → 製品family別のターミナル数 を数え、`agent.chat` に要約させ、`derive:social` で社会面に 1 本流す |
| cron `0 */6 * * *`（3 step） | **自分の被覆率を書き戻す。** 自 DID のノード総数と collection 別内訳を数え、`ActorCoverageSnapshot` を MERGE する |
| subscribeRepos（1 step） | `oilRefining.yieldSnapshot` が来たら、その `productFamily` に一致する製品ターミナルを引く |
| xrpc `…network.getProductTerminal` | 1 件のターミナルを返す |
| xrpc `…network.listProductTerminals` | ターミナルを最大 50 件返す |
| xrpc `…network.listWholesaleHubs` | 卸ハブを最大 50 件返す |
| xrpc `…analytics.getProductBalance` | 国コード指定で、ターミナルの製品family と貯蔵容量を最大 50 件返す |
| xrpc `…health` | ターミナル総数を 1 行返す |

宣言された 4 つの sub-actor（`actors[]`）:
`network:product-terminal` / `network:wholesale-hub` /
`analytics:product-balance` / `risk:inventory`。

**`risk:inventory` に対応する pipeline は無い。** 「Distribution bottleneck and
inventory risk coverage」と説明されているが、8 pipeline のどれもそれを計算しない。

読むグラフラベルは 2 つだけ（`ProductTerminal` を 7 か所、`WholesaleHub` を 2 か所）。
書くのは `ActorCoverageSnapshot` 1 か所。使う `fn` は
`graph.query` 11 / `graph.write` 1 / `agent.chat` 1 / `derive:social` 1 で、
宣言された 5 capability のうち `agent.invoke` はどの step でも使われていない。

## 購読先を書く者がフリート内に居ない

`triggers.subscribeRepos` は 4 collection を購読すると宣言する。**この 4 つを
`graph.write` する pipeline は、7 本の `oil-*` repo のどこにも無い。**

| 購読先 collection | 宣言している repo | そこに write pipeline があるか |
|---|---|---|
| `…oilDistribution.productTerminal` | **この repo 自身**（subscribe 側のみ） | **無い** |
| `…oilDistribution.wholesaleHub` | **この repo 自身**（subscribe 側のみ） | **無い** |
| `…oilRefining.yieldSnapshot` | `oil-refining`（そちらも subscribe 側のみ） | **無い** |
| `…oilShipping.cargo` | `oil-shipping`（そちらも subscribe 側のみ） | **無い** |

実測: 7 本の `oil-*` はそれぞれ `graph.write` を**ちょうど 1 つ**持ち、その全部が
自分の `coverageSnapshot` を書く。つまり `ProductTerminal` / `WholesaleHub` の
ノードは、この 7 本の**外側**から入る前提になっている —— その供給元はこの repo の
どの宣言にも書かれていない。

`oil-coverage` はこの repo の `…oilDistribution.coverageSnapshot` を購読しており、
**測られる側**としての接続だけが両方向で噛み合っている。

## 兄弟 5 本との違いはどこか

`oil-upstream` / `oil-midstream` / `oil-refining` / `oil-trading` / `oil-shipping` は
いずれも `actors=4` / `pipelines=8` で、この repo と**同じ形**である（実測）。
違うのは中身の語彙と `nanoid` だけ:

| repo | nanoid |
|---|---|
| oil-upstream | `01lupstr` |
| oil-midstream | `01lm1dst` |
| oil-refining | `01lr3f1n` |
| oil-trading | `01ltrad3` |
| oil-shipping | `01l5h1p0` |
| **oil-distribution** | **`01ld1str`** |

`oil-coverage` だけが `actors=6` / `pipelines=5` で、形からして別物。

## gate は何を止めるか

`src/oil_distribution/murakumo.kotoba` は **15 cell × 7 gate** の deny-by-default。
7 つの attestation が 1 つでも欠けると `:status :blocked` で `:effects` は空になる
（実測: 6/7 揃えても `:blocked`、`all-cell-plans` は 15 cell 全部 blocked で総 effect 数 0）。

7 gate: `:council-charter-attestation` `:no-platform-held-key-baseline`
`:no-probing-baseline` `:murakumo-only-inference-baseline`
`:did-primary-baseline` `:append-only-gate-baseline`
`:kotoba-only-substrate-baseline`

15 という数は manifest から導ける: **5 xrpc + 2 `requiredCollections` +
4 `requiredLoops` + 4 `subscribeRepos` = 15**。

実行して確かめられる（[quickstart](docs/operator-quickstart.md) の手順 4）。

### ⚠ gate が書く collection は、manifest が宣言する collection と 1 つも一致しない

`murakumo.cljc` の `collection` 関数は `com.etzhayyim.oil-distribution.<name>` を
組み立てる。manifest 側の語彙は `com.etzhayyim.apps.oilDistribution.<name>` である。
**`apps.` の有無・segment のハイフン/キャメル・末尾の大文字小文字がすべて違い、
交わりは空**（実測、quickstart 手順 4）:

```
gate     com.etzhayyim.oil-distribution.productterminal
manifest com.etzhayyim.apps.oilDistribution.productTerminal
```

さらに gate は、**XRPC の *メソッド* 名を書き込み先 *collection* として扱う**。
`getproductterminal` は manifest では「読む API」だが、gate では
`com.etzhayyim.oil-distribution.getproductterminal` へ `:mst/put-record` する計画になる。

どちらも scaffold 生成器の産物と読める。**直していない** —— 正しい語彙を決めるのは
lexicon 側の権限であり、この snapshot が勝手に決めてよいことではない。

⚠ **namespace が `oil_distribution.murakumo`（アンダースコア）である。** Clojure の
慣習では `oil-distribution.murakumo` と書いてファイル側を `oil_distribution/` にする。
現状でも load はできるが、`(require '[oil-distribution.murakumo])` は**通らない** —
`oil_distribution` と綴る必要がある。

## 2026-06-24 の snapshot であること

etzhayyim monorepo の `20-actors/oil-distribution` から descriptor だけを写した
snapshot（`f6a6571`）。`actor-manifest.jsonld` が `runtime: k8s-langserver` /
`edge: sveltekit-proxy` と宣言していても、**その runtime も edge もここには無い。**
`complianceDocs` が指す 2 本（`90-docs/rules/compliance/…` /
`90-docs/platform/…`）も、この repo には存在しない**元 monorepo のパス**である。

`actor-manifest.test.ts` は **走らない** — `package.json` も vitest も無く、
`node_modules` も無い。vitest 前提で書かれた 11 の `it(` / 16 の `expect(` が、
実行されないまま置かれている。`.ts` なのでこの workspace の nbb 経路にも載らない。

commit は 4 本だけ（2026-06-24 snapshot → 2026-07-02 identity 移行 →
2026-07-18 の murakumo WIP rescue → 2026-07-27 merge）。

## 既知のギャップ（この反復で埋めていないもの）

- **test が無い。** 上の表の数（pipeline 8 / sub-actor 4 / cell 15 / gate 7 /
  label 2）と 2 つの DID は、いま**この README が主張しているだけ**で、実体が
  変わっても赤くならない。`marine-insurance` は `test/…/docs_test.cljs` でこれを
  固定している —— 同じものがここにも要る。
- **west pin が遅れていた。** superproject の pin は `53f6a34`（2026-07-02）で、
  `src/oil_distribution/murakumo.kotoba` を含む `9c3ff48` を指していなかった。この
  README を書く時点で main に合わせている。
- **identity の不整合を直していない**（上記 2 名の DID、live との 5 か所差分）。
- **collection 語彙の不一致を直していない**（gate と manifest で交わり空）。
- **`risk:inventory` に対応する pipeline が無い**ことを直していない。
