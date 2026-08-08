# operator quickstart

**この repo で「動かせる」ものは 1 つだけ**（手順 4 の gate）。残りは、宣言と
現実がどれだけずれているかを **自分の端末で確かめる**ための手順である。

所要 5 分。必要なのは `git` / `jq` / `curl` / `dig` / `nbb`。
下に貼ってある出力はすべて 2026-08-09 に実際に実行した結果の写しで、手打ちではない。

---

## 手順 0 — checkout が gate を持っているか確かめる

`src/` が無ければ、west の pin が `9c3ff48` より前を指している。

```bash
ls src/oil_distribution/murakumo.cljc && git log --oneline -1
```

```
src/oil_distribution/murakumo.cljc
9c3ff48 Merge pull request #1 from etzhayyim/rescue/murakumo-wip-20260718
```

`src/` が無い場合は superproject 側で pin を進める（詳細は skill `west-pin-advance`）:

```bash
nbb scripts/gen-west-manifest.cljs --entry oil-distribution
```

---

## 手順 1 — descriptor の形を数える

README の表の数は、すべてこの 1 コマンドから出ている。

```bash
jq -r '{pipelines:(.pipelines|length), actors:(.actors|length),
        capabilities:(.capabilities|length),
        subscribeRepos:(.triggers.subscribeRepos.collections|length)}' actor-manifest.jsonld
```

```json
{
  "pipelines": 8,
  "actors": 4,
  "capabilities": 5,
  "subscribeRepos": 4
}
```

兄弟 5 本も同じ形であること（`oil-coverage` だけ違うこと）を確かめる:

```bash
cd .. && for n in oil-upstream oil-midstream oil-refining oil-trading oil-shipping oil-coverage; do
  printf "%-14s " $n
  jq -r '"actors=" + ((.actors|length)|tostring) + " pipelines=" + ((.pipelines|length)|tostring)' \
    $n/actor-manifest.jsonld
done; cd -
```

```
oil-upstream   actors=4 pipelines=8
oil-midstream  actors=4 pipelines=8
oil-refining   actors=4 pipelines=8
oil-trading    actors=4 pipelines=8
oil-shipping   actors=4 pipelines=8
oil-coverage   actors=6 pipelines=5
```

---

## 手順 2 — trigger と、実際に触るグラフラベルを見る

```bash
jq -r '.pipelines[] | .trigger.type + " " +
       (.trigger.cron // .trigger.nsid // ((.trigger.collections//[])|join(","))) +
       "  steps=" + ((.steps|length)|tostring)' actor-manifest.jsonld
```

```
cron 0 */8 * * *  steps=5
subscribeRepos com.etzhayyim.apps.oilRefining.yieldSnapshot  steps=1
xrpc com.etzhayyim.apps.oilDistribution.network.getProductTerminal  steps=1
xrpc com.etzhayyim.apps.oilDistribution.network.listProductTerminals  steps=1
xrpc com.etzhayyim.apps.oilDistribution.network.listWholesaleHubs  steps=1
xrpc com.etzhayyim.apps.oilDistribution.analytics.getProductBalance  steps=1
xrpc com.etzhayyim.apps.oilDistribution.health  steps=1
cron 0 */6 * * *  steps=3
```

読み書きするラベルは 3 つしか出てこない:

```bash
jq -r '.pipelines[].steps[] | (.args.sql // .args.template // empty)' actor-manifest.jsonld \
  | grep -oE '\([a-z]:[A-Za-z]+' | sort | uniq -c | sort -rn
```

```
   7 (t:ProductTerminal
   2 (h:WholesaleHub
   1 (c:ActorCoverageSnapshot
```

**購読先を書く者がフリート内に居ない**ことも確かめられる。7 本の `oil-*` の
`graph.write` を全部並べると、どれも自分の `coverageSnapshot` しか書いていない:

```bash
cd .. && for n in oil-upstream oil-midstream oil-refining oil-trading oil-shipping oil-distribution oil-coverage; do
  printf "%-16s " $n
  jq -r '[.pipelines[].steps[] | select(.fn=="graph.write") | (.args.params.collection // "?")] | join(", ")' \
    $n/actor-manifest.jsonld
done; cd -
```

```
oil-upstream     com.etzhayyim.apps.oilUpstream.coverageSnapshot
oil-midstream    com.etzhayyim.apps.oilMidstream.coverageSnapshot
oil-refining     com.etzhayyim.apps.oilRefining.coverageSnapshot
oil-trading      com.etzhayyim.apps.oilTrading.coverageSnapshot
oil-shipping     com.etzhayyim.apps.oilShipping.coverageSnapshot
oil-distribution com.etzhayyim.apps.oilDistribution.coverageSnapshot
oil-coverage     ?, com.etzhayyim.apps.oil.coverageSnapshot
```

`productTerminal` / `wholesaleHub` / `yieldSnapshot` / `cargo` はこの一覧に無い。

---

## 手順 3 — 2 つの DID のうち、どちらが解決するか確かめる

```bash
dig +short oil-distribution.etzhayyim.com A          # ← 何も返らない
curl -s -o /dev/null -w '%{http_code}\n' https://oil-distribution.etzhayyim.com/.well-known/did.json
curl -s -o /dev/null -w '%{http_code}\n' https://etzhayyim.com/actor/oil-distribution/did.json
```

```
000
200
```

`000` は「HTTP status が無い」＝ 接続の前段で失敗した、という意味である。
**manifest と gate が名乗る方の DID が、この解決しない側。**

repo の did.json が live の写しでないことも見ておく:

```bash
curl -s https://etzhayyim.com/actor/oil-distribution/did.json > /tmp/live-did.json
diff <(jq -S . .well-known/did.json) <(jq -S . /tmp/live-did.json) | head -20
```

`ed25519-2020` vs `jws-2020`、`alsoKnownAs` 4 件 vs 空、`verificationMethod` の
有無、PDS の宛先、2 つ目の service —— 5 か所ずれる（`diff` は exit 1）。

宛先の生死:

```bash
for u in https://pds.etzhayyim.com/xrpc/_health https://pds.aozora.app/xrpc/_health; do
  printf "%-46s -> " $u; curl -s -o /dev/null -w '%{http_code}\n' --max-time 12 $u
done
```

```
https://pds.etzhayyim.com/xrpc/_health         -> 530
https://pds.aozora.app/xrpc/_health            -> 200
```

repo の did.json が指す方（`pds.etzhayyim.com`）が落ちていて、live が指す方
（`pds.aozora.app`）が生きている。GitHub Pages 側は両 org とも 404:

```bash
for u in https://etzhayyim.github.io/com-etzhayyim-oil-distribution/.well-known/did.json \
         https://cloud-itonami.github.io/oil-distribution/.well-known/did.json; do
  printf "%s -> " $u; curl -s -o /dev/null -w '%{http_code}\n' --max-time 12 $u
done
```

---

## 手順 4 — gate を実際に走らせる（この repo で唯一動くもの）

`/tmp/probe.cljs` を作る:

```clojure
(ns probe (:require [oil_distribution.murakumo :as m]))

(def all-gates (set m/common-gates))

(defn summarise [label atts]
  (let [p (m/cell-plan :getproductterminal
                       {:attestations atts :request-id "probe-1"
                        :computed-at "2026-08-09T00:00:00Z"})]
    (println (str label "  status=" (:status p)
                  "  effects=" (count (:effects p))
                  "  missing=" (count (:missing-gates p))))))

(println "cells:" (count m/cell-specs) " gates:" (count m/common-gates))
(summarise "none      " #{})
(summarise "6-of-7    " (disj all-gates :kotoba-only-substrate-baseline))
(summarise "all 7     " all-gates)
(println "gate collection:" (m/collection "productterminal"))
(let [plans (m/all-cell-plans {:attestations #{}})]
  (println "blocked:" (count (filter #(= :blocked (:status %)) (vals plans)))
           "/" (count plans)
           " total effects:" (reduce + 0 (map #(count (:effects %)) (vals plans)))))
```

```bash
nbb --classpath "src:/tmp" /tmp/probe.cljs
```

```
cells: 15  gates: 7
none        status=:blocked  effects=0  missing=7
6-of-7      status=:blocked  effects=0  missing=1
all 7       status=:ready  effects=1  missing=0
gate collection: com.etzhayyim.oil-distribution.productterminal
blocked: 15 / 15  total effects: 0
```

**確かめるべきは「6 つ揃えても通らない」ことである** —— 部分的な attestation で
effect が 1 つでも出るなら、それは deny-by-default ではない。

最後の行の `gate collection:` に注目する。manifest 側の綴りは
`com.etzhayyim.apps.oilDistribution.productTerminal` で、**交わりが無い**:

```bash
jq -r '.triggers.subscribeRepos.collections[]' actor-manifest.jsonld
```

```
com.etzhayyim.apps.oilDistribution.productTerminal
com.etzhayyim.apps.oilDistribution.wholesaleHub
com.etzhayyim.apps.oilRefining.yieldSnapshot
com.etzhayyim.apps.oilShipping.cargo
```

cell が 15 ある理由も manifest から導ける:

```bash
jq -r '((.pipelines|map(select(.trigger.type=="xrpc"))|length) + (.requiredCollections|length)
        + (.requiredLoops|length) + (.triggers.subscribeRepos.collections|length))' actor-manifest.jsonld
```

```
15
```

---

## 手順 5 — `.ts` テストが走らないことを確かめる

```bash
ls package.json node_modules
```

```
ls: node_modules: No such file or directory
ls: package.json: No such file or directory
```

`actor-manifest.test.ts` は vitest を import しているが、その vitest がここには
無い。**この 11 の `it(` は一度も実行されていない。**

```bash
grep -c 'it(' actor-manifest.test.ts     # 11
```

---

## ここから先に進みたい場合

この repo を「動かす」には、少なくとも次の 4 つが repo の外に要る:

1. `ProductTerminal` / `WholesaleHub` を持つグラフ（**そのノードを書く者は
   フリート内に居ない**、手順 2）
2. cron を撃つ scheduler と XRPC を受ける server（`runtime: k8s-langserver`）
3. 7 つの attestation を発行する主体（無ければ gate は永久に `:blocked`）
4. `com.etzhayyim.apps.oilDistribution.*` という lexicon の登録先

いずれもこの repo は持っていないし、持っていると主張してもいない。
